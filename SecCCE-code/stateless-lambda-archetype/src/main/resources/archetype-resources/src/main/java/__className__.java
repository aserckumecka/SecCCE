 package ${package};


import com.amazonaws.services.lambda.runtime.Context;

import com.amazonaws.services.lambda.runtime.LambdaLogger;

import com.amazonaws.services.lambda.runtime.RequestHandler;

import com.fasterxml.jackson.core.JsonProcessingException;

import com.fasterxml.jackson.core.type.TypeReference;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.databind.node.ObjectNode;

import software.amazon.awssdk.core.SdkBytes;

import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import software.amazon.awssdk.services.kms.KmsClient;

import software.amazon.awssdk.services.kms.model.DecryptRequest;


import javax.crypto.Cipher;

import javax.crypto.SecretKey;

import javax.crypto.spec.GCMParameterSpec;

import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;

import java.security.SecureRandom;

import java.util.Base64;

import java.util.Date;

import java.util.HashMap;

import java.util.List;

import java.util.Map;

import java.util.ArrayList;

import java.time.Instant;




public class ${className} implements RequestHandler<${className}.EncryptedBatchPayload, String> {

private static final String region = "sa-east-1";

private static final KmsClient kmsClient = KmsClient.builder()

.region(software.amazon.awssdk.regions.Region.of(region))

.build();

private static final DynamoDbClient dynamoDbClient = DynamoDbClient.builder()

.region(software.amazon.awssdk.regions.Region.of(region))

.build();


private static final ObjectMapper objectMapper = new ObjectMapper();


private static final String ALERT_TABLE_NAME = "AlertDB";

private static final String ALARM_TABLE_NAME = "AlarmDB";


public static class EncryptedBatchPayload {

private String encryptedDek;

private String encryptedBlock;


// Getters e Setters são OBRIGATÓRIOS para o Jackson (usado pelo Lambda)

public String getEncryptedDek() {

return encryptedDek;

}


public void setEncryptedDek(String encryptedDek) {

this.encryptedDek = encryptedDek;

}


public String getEncryptedBlock() {

return encryptedBlock;

}


public void setEncryptedBlock(String encryptedBlock) {

this.encryptedBlock = encryptedBlock;

}

}

@Override

public String handleRequest(EncryptedBatchPayload payload, Context context) {

long lambda_ts = Instant.now().toEpochMilli();

LambdaLogger logger = context.getLogger();

try {

// LOG PARA DEBUG: Veja o payload bruto que chega

logger.log("PAYLOAD RECEBIDO: " + objectMapper.writeValueAsString(payload));

} catch (JsonProcessingException e) {

logger.log("Erro ao serializar payload recebido para log: " + e.getMessage());

}

String ret = "";


String encDek = payload.getEncryptedDek();

SecretKey requestDek;

List<HashMap<String, String>> eventBatch;

try {

// Passo 1: Descriptografar a Chave de Dados (DEK) enviada no payload

logger.log("Descriptografando DEK da requisição...");

requestDek = decryptDek(payload.getEncryptedDek(), logger);

logger.log("DEK descriptografada com sucesso.");


// Passo 2: Usar a DEK para descriptografar o bloco de eventos

logger.log("Descriptografando bloco de eventos...");

String decryptedBlockJson = decryptData(payload.getEncryptedBlock(), requestDek, logger);

// Passo 3: Converter o JSON do bloco de eventos de volta para a Lista de Mapas

eventBatch = objectMapper.readValue(decryptedBlockJson, new TypeReference<List<HashMap<String, String>>>() {});

logger.log("Bloco de eventos descriptografado. Processando " + eventBatch.size() + " eventos.");


} catch (Exception e) {

logger.log("ERRO FATAL: Falha ao descriptografar o payload de entrada. " + e.getMessage());

throw new RuntimeException("Falha na descriptografia do payload.", e);

}

// insere no lugar de verifVars as variáveis necessárias para o processamento dos IFs.

// Os nomes das variáveis são baseados no hash do código daquele IF associado e vai ser usado como ID do "alert" enviado para o DynamoDB.

//verifVars

for (HashMap<String, String> eventMap : eventBatch) {

String dvchost = eventMap.get("dvchost");

String rt = eventMap.get("rt");

String eventid = eventMap.get("eventid");

// no lugar de verifCodes são inseridos no skeleton os IFs para processamento.

//verifCodes

}

// no lugar de condCodes são inseridos os códigos de verificação se houveram matches nos IFs anteriores e chama a criação de um alerta com os eventos associados, armazenados na variável associada (Arraylist).

//condCodes

logger.log("Result: " + ret);

eventBatch = null;

//return

String className = this.getClass().getName();

long lambda_ts_fin = Instant.now().toEpochMilli();

ret = (lambda_ts_fin - lambda_ts) + "," + ret;

return ret;

}



private SecretKey decryptDek(String encryptedDekBase64, LambdaLogger logger) {

try {

SdkBytes ciphertextBlob = SdkBytes.fromByteArray(Base64.getDecoder().decode(encryptedDekBase64));

DecryptRequest decryptRequest = DecryptRequest.builder()

.ciphertextBlob(ciphertextBlob)

.build();

SdkBytes plaintextDekBytes = kmsClient.decrypt(decryptRequest).plaintext();


// Verificamos o tamanho para garantir que é AES-256

if (plaintextDekBytes.asByteArray().length != 32) {

throw new RuntimeException("A chave DEK obtida não tem 32 bytes (256 bits)!");

}

return new SecretKeySpec(plaintextDekBytes.asByteArray(), "AES");

} catch (Exception e) {

logger.log("ERRO no decryptDek: " + e.getMessage());

throw new RuntimeException("Falha ao descriptografar a chave DEK vinda do payload.", e);

}

}


private void createAlert(Map<String, String> event, String dt_id, SecretKey dek, String encDek, LambdaLogger logger) {

try {

String eventJson = objectMapper.writeValueAsString(event);

// Usa a chave 'dek' passada como parâmetro

String encryptedEvent = encryptData(eventJson, dek, encDek);

String rtValue = event.get("rt");

Map<String, AttributeValue> item = new HashMap<>();

item.put("id", AttributeValue.builder().s(dt_id).build());

item.put("rt", AttributeValue.builder().s(rtValue).build());

item.put("events", AttributeValue.builder().s(encryptedEvent).build());


PutItemRequest putItemRequest = PutItemRequest.builder()

.tableName(ALERT_TABLE_NAME)

.item(item)

.build();


dynamoDbClient.putItem(putItemRequest);

} catch (Exception e) {

logger.log("ERROR: Unable to add alert item for detection_id: " + dt_id + ". Exception: " + e.getMessage());

}

}

private void createAlarm(Map<String, String> event, String cr_id, String dt_id, SecretKey dek, LambdaLogger logger) {

String lambdaTimeStr = String.valueOf(Instant.now().toEpochMilli());

String rtValue = event.get("rt");


try {

Map<String, AttributeValue> item = new HashMap<>();

item.put("id", AttributeValue.builder().s(cr_id).build());

item.put("rt", AttributeValue.builder().s(lambdaTimeStr).build());

item.put("status", AttributeValue.builder().s("new").build());

item.put("events", AttributeValue.builder().s(dt_id + "," + rtValue).build());


PutItemRequest putItemRequest = PutItemRequest.builder()

.tableName(ALARM_TABLE_NAME)

.item(item)

.build();


dynamoDbClient.putItem(putItemRequest);


} catch (Exception e) {

logger.log("Unable to create alarm: " + cr_id + " - Generic Exception: " + e.getMessage());

}

}


private String encryptData(String plaintext, SecretKey key, String encDek) throws Exception {

byte[] iv = new byte[12]; // 12 bytes é o tamanho recomendado para GCM IV.

new SecureRandom().nextBytes(iv);


Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iv); // 128 bit auth tag length

cipher.init(Cipher.ENCRYPT_MODE, key, parameterSpec);


byte[] cipherTextBytes = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));


ObjectNode resultJson = objectMapper.createObjectNode();

resultJson.put("iv", Base64.getEncoder().encodeToString(iv));

resultJson.put("encDek", encDek);

resultJson.put("ciphertext", Base64.getEncoder().encodeToString(cipherTextBytes));


return resultJson.toString();

}



private String decryptData(String encryptedPayload, SecretKey key, LambdaLogger logger) throws Exception {

try {

// 1. Parse o JSON que contém o IV e o ciphertext

ObjectNode payloadJson = (ObjectNode) objectMapper.readTree(encryptedPayload);

byte[] iv = Base64.getDecoder().decode(payloadJson.get("iv").asText());

byte[] ciphertext = Base64.getDecoder().decode(payloadJson.get("ciphertext").asText());

// 2. Inicialize o cipher para descriptografar

Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");

GCMParameterSpec parameterSpec = new GCMParameterSpec(128, iv); // Deve ser o mesmo usado na criptografia

cipher.init(Cipher.DECRYPT_MODE, key, parameterSpec);

// 3. Descriptografe os dados

byte[] decryptedBytes = cipher.doFinal(ciphertext);

// 4. Converta de volta para String

return new String(decryptedBytes, StandardCharsets.UTF_8);

} catch (Exception e) {

logger.log("ERRO no decryptData: " + e.getMessage());

throw new RuntimeException("Falha ao descriptografar o bloco de dados.", e);

}

}

} 
