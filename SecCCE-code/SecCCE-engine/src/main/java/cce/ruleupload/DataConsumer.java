package cce.ruleupload;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;
import java.time.Instant;

import java.util.ArrayList;
import java.util.concurrent.BlockingQueue;
import java.util.Base64;
import java.util.concurrent.LinkedBlockingQueue;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.json.JSONArray;
import org.json.JSONObject;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.kms.KmsClient;
import software.amazon.awssdk.services.kms.model.DataKeySpec;
import software.amazon.awssdk.services.kms.model.GenerateDataKeyRequest;
import software.amazon.awssdk.services.kms.model.GenerateDataKeyResponse;


public class DataConsumer implements Runnable{
	private ArrayList<BlockingQueue<JSONObject>> queueArray;
	private String binName;
	private double triggerGap;
	JSONArray block = new JSONArray();
	String sblock = "";
	String awsRegion;
	int runfor = 0;
	public ArrayList<JSONArray> blockArray = new ArrayList<JSONArray>();
	private final DekPoolManager dekPoolManager;
    private final Thread dekPoolManagerThread;
    private final String kmsKeyId; // A CMK usada para gerar as DEKs
	
	
	public DataConsumer(String binName, ArrayList<BlockingQueue<JSONObject>> queueArray, int triggerGap, int runfor, String awsRegion, String kmsKeyId) {
		this.queueArray = queueArray;
		this.binName = binName;
		this.triggerGap = triggerGap;
		this.runfor = runfor;
		this.awsRegion = awsRegion;
		this.kmsKeyId = kmsKeyId; // Armazena o ID da Chave Mestra
		
		// === INICIALIZA O POOL DE CHAVES ===
        // Define um pool de, no máximo, 10 chaves pré-buscadas.
        int MAX_DEK_POOL_SIZE = 10;
        this.dekPoolManager = new DekPoolManager(this.kmsKeyId, MAX_DEK_POOL_SIZE, this.awsRegion);
        this.dekPoolManagerThread = new Thread(this.dekPoolManager);
        this.dekPoolManagerThread.setName("DekPoolManagerThread-" + binName);
        this.dekPoolManagerThread.setDaemon(true); // Para a thread não bloquear o fim da JVM
        this.dekPoolManagerThread.start();
	}
	
	public void run() {
		Path eventsPath = null;
		try {
			eventsPath = Paths.get("events/"+binName+"/");
			if(Files.notExists(eventsPath)) { Files.createDirectory(eventsPath);}
		} catch (IOException e) {
			e.printStackTrace();
		}
		double lambdaTriggerStartTime = Instant.now().toEpochMilli();
		double starttime = lambdaTriggerStartTime;
		int triggerCount = 1;
		
		
		for (BlockingQueue<JSONObject> queue : queueArray) {
			blockArray.add(new JSONArray());
		}
		
		// Pensar numa solução para incluir o fechamento/geração de bloco e a trigger da 
		// lambda tbm por tamanho de bloco...
		// Verificar como fazer para que o QueueConsumer seja responsável pela trigger e não o Consumer...
		while((Instant.now().toEpochMilli() - starttime) < runfor) {
			for (int i = 0; i < queueArray.size(); i++) {
				new Thread(new QueueConsumer(queueArray.get(i), blockArray.get(i), i)).start();
			}
			try {
				Thread.sleep((long) triggerGap);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			// Here is the point to encrypt events one by one and trigger Lucene to get the IV.
			for (JSONArray jsonArray : blockArray) {
				block.putAll(jsonArray);
			}
			//blockArray.clear();
			
			// Here is the point to call for a KMS DEK and encrypt the entire block once.
			String blockLT = block.toString();
			
			int blockLTsize = block.length();
			
			// Se o bloco estiver vazio, não faz nada
			if (blockLTsize == 0 || blockLT.equals("[]")) {
			    block.clear();
			    System.out.println(binName + " - Bloco vazio, pulando trigger.");
			    continue; // Pula para a próxima iteração do loop while
			}
			
            try {
                // 1. Pega uma DEK "quente" do pool (bloqueia se o pool estiver vazio)
                DekPair dek = dekPoolManager.takeDek();

                // 2. Criptografa o bloco inteiro de uma vez
                String encryptedBlock = encryptData(blockLT, dek.getPlaintextKey());

                // 3. Cria o payload final para a Lambda
                EncryptedBatchPayload payload = new EncryptedBatchPayload(
                        dek.getCiphertextBlob(),
                        encryptedBlock
                );
                String payloadJsonString = payload.toJsonString();
                
                // 4. Limpa o bloco
                block.clear();
                
                // 5. Envia o payload CRIPTOGRAFADO para a Lambda
                // IMPORTANTE: Sua classe LambdaTrigger deve ser atualizada para
                // receber 'payloadJsonString' em vez de 'blockLT'.
                new Thread(new LambdaTrigger(binName, payloadJsonString, triggerCount, blockLTsize, (int) triggerGap, awsRegion)).start();

                for (BlockingQueue<JSONObject> queue : queueArray) {
    				System.out.println(binName+ " - Remaining events in the queue: " + queue.size());
    			}
                
                Path blockPath = Paths.get(eventsPath.toString(), Instant.now().toString());
                new Thread(new WriteToDisk(blockPath, blockLT)).start();
                triggerCount++;

            } catch (InterruptedException e) {
                System.err.println("Thread principal interrompida enquanto aguardava por DEK. Encerrando.");
                Thread.currentThread().interrupt();
                this.stop(); // Para a thread do pool
                break; // Sai do loop while
            } catch (Exception e) {
                System.err.println("ERRO FATAL ao criptografar ou enviar o bloco: " + e.getMessage());
                e.printStackTrace();
                // Aqui você pode decidir se quer tentar novamente, descartar o bloco, etc.
                block.clear(); // Descarta o bloco que falhou
            }
		}
		// Para a thread do pool quando o 'runfor' terminar
        this.stop();
        System.out.println("DataConsumer " + binName + " finalizado.");
	}
	
	/**
     * Para a thread do pool de chaves.
     */
    public void stop() {
        this.dekPoolManager.stop();
        this.dekPoolManagerThread.interrupt();
    }

    /**
     * Método auxiliar para criptografar os dados com AES-GCM.
     */
    private String encryptData(String plaintext, SecretKey key) throws Exception {
        byte[] iv = new byte[12]; // 12 bytes é o tamanho recomendado para GCM IV.
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        // 128 bit auth tag length
        GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);

        byte[] cipherTextBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

        // Empacota o IV e o ciphertext juntos, pois ambos são necessários
        JSONObject resultJson = new JSONObject();
        resultJson.put("iv", Base64.getEncoder().encodeToString(iv));
        resultJson.put("ciphertext", Base64.getEncoder().encodeToString(cipherTextBytes));

        return resultJson.toString();
    }
	
	
    
    
	
	class WriteToDisk implements Runnable{
		Path rulePath;
		JSONArray bl;
		String sblock = new String();
		
		public WriteToDisk(Path rulePath, String sblock) {
			this.rulePath = rulePath;
			this.sblock = sblock;
		}
		
		public void run() {
			byte[] dataBytes = sblock.toString().getBytes();
			
			try {
				Files.write(rulePath, dataBytes, StandardOpenOption.CREATE);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	
	
	
	class QueueConsumer implements Runnable{
		BlockingQueue<JSONObject> queue;
		JSONArray block = new JSONArray();
		String sblock = "";
		double startTime;
		int i;
		
		public QueueConsumer(BlockingQueue<JSONObject> queue, JSONArray block, int i) {
			this.queue = queue;
			this.i = i;
			startTime = Instant.now().toEpochMilli();
		}

		public void run() {
			while((Instant.now().toEpochMilli() - startTime) < triggerGap) {
				if(queue.peek() != null) {
					block.put(queue.poll());
				}
			}
			blockArray.set(i, block);
		}
	}
	
	
	
	
	
	class DekPoolManager implements Runnable {
	    private final BlockingQueue<DekPair> pool;
	    private final int maxPoolSize;
	    private final String kmsKeyId;
	    private final KmsClient kmsClient;
	    private volatile boolean running = true;

	    public DekPoolManager(String kmsKeyId, int maxPoolSize, String awsRegion) {
	        // Usamos uma BlockingQueue com capacidade definida.
	        this.pool = new LinkedBlockingQueue<>(maxPoolSize);
	        this.maxPoolSize = maxPoolSize;
	        this.kmsKeyId = kmsKeyId;
	        this.kmsClient = KmsClient.builder()
	                                  .region(Region.of(awsRegion))
	                                  .build();
	    }

	    @Override
	    public void run() {
	        System.out.println("DekPoolManager iniciado. Preenchendo pool...");
	        while (running) {
	            try {
	                // Tenta preencher o pool até a capacidade máxima.
	                // A chamada 'put' bloqueará se o pool já estiver cheio.
	                if (pool.size()<maxPoolSize) {
	                	pool.put(fetchNewDekFromKms());
					}
	            } catch (InterruptedException e) {
	                System.out.println("DekPoolManager interrompido.");
	                Thread.currentThread().interrupt();
	                running = false;
	            } catch (Exception e) {
	                // Erro ao buscar chave (ex: permissão do KMS, rede)
	                System.out.println("Waiting for taking new DEK to pool...");
	                // Aguarda um pouco antes de tentar novamente para não sobrecarregar
	                try {
	                    Thread.sleep(5000); // Dorme por 5 segundos
	                } catch (InterruptedException ie) {
	                    Thread.currentThread().interrupt();
	                    running = false;
	                }
	            }
	        }
	    }

	    /**
	     * Busca uma nova chave DEK no KMS.
	     */
	    private DekPair fetchNewDekFromKms() {
	        // System.out.println("Buscando nova DEK do KMS..."); // Descomente para debug
	        GenerateDataKeyRequest request = GenerateDataKeyRequest.builder()
	                .keyId(this.kmsKeyId)
	                .keySpec(DataKeySpec.AES_256)
	                .build();
	        
	        GenerateDataKeyResponse response = kmsClient.generateDataKey(request);

	        SecretKey plaintextKey = new SecretKeySpec(response.plaintext().asByteArray(), "AES");
	        String ciphertextBlobBase64 = Base64.getEncoder().encodeToString(response.ciphertextBlob().asByteArray());

	        // System.out.println("Nova DEK adicionada ao pool."); // Descomente para debug
	        return new DekPair(plaintextKey, ciphertextBlobBase64);
	    }

	    /**
	     * Retira uma chave do pool. Bloqueia se o pool estiver vazio.
	     */
	    public DekPair takeDek() throws InterruptedException {
	        System.out.println("DEK solicitada. Tamanho atual do pool: " + pool.size());
	        // 'take()' espera automaticamente se o pool estiver vazio
	        // até que o 'run()' adicione uma nova chave.
	        return pool.take();
	    }

	    public void stop() {
	        this.running = false;
	    }
	}
}

