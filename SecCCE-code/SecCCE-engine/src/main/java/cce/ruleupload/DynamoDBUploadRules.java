package cce.ruleupload;

import com.google.gson.Gson;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.time.Instant; // Usado para gerar o timestamp
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DynamoDBUploadRules {
    
    private static final String TABLE_NAME = "CCErules";
    
    private String extractIdFromYaml(String yamlContent) {
        for (String line : yamlContent.split("\n")) {
            if (line.trim().startsWith("id:")|| line.trim().startsWith("id :")) {
                return line.substring(line.indexOf(":") + 1).trim();
            }
        }
        return null;
    }
    
    public void uploadRawRule(String yamlContent, String awsRegion) {
        Region region = Region.of(awsRegion);
        try (DynamoDbClient ddb = DynamoDbClient.builder().region(region).build()) {
        	String ruleId = extractIdFromYaml(yamlContent);
        	Map<String, AttributeValue> item = new HashMap<>();
            item.put("id", AttributeValue.builder().s(ruleId).build());
            item.put("LastModified", AttributeValue.builder().s(Instant.now().toString()).build());
            item.put("Rule", AttributeValue.builder().s(yamlContent).build()); 

            PutItemRequest request = PutItemRequest.builder().tableName("CCErules").item(item).build();
            ddb.putItem(request);
            System.out.println("Enviada regra (bruta) com ID: " + ruleId);
        } catch (DynamoDbException e) {
        	e.printStackTrace();
        }
    }

    public void uploadRules(ArrayList<SigmaMapping> sigmaMappingArray, String awsRegion) {
        // Inicializa o cliente DynamoDB e a biblioteca Gson
        Region region = Region.of(awsRegion);
        try (DynamoDbClient ddb = DynamoDbClient.builder().region(region).build()) {
            Gson gson = new Gson();

            System.out.println("Iniciando upload de " + sigmaMappingArray.size() + " regras para a tabela " + TABLE_NAME);

            // Itera sobre a lista de regras para enviar
            for (SigmaMapping rule : sigmaMappingArray) {
                // O ID continua sendo obrigatório
                if (rule.getId() == null || rule.getId().trim().isEmpty()) {
                    System.err.println("Regra ignorada por não possuir um ID: " + rule.getTitle());
                    continue;
                }

                // --- Construção do novo item simplificado ---

                // 1. Serializa o objeto 'rule' inteiro para uma string JSON.
                String ruleAsJson = gson.toJson(rule);
                
                // 2. Cria o mapa para o item do DynamoDB.
                Map<String, AttributeValue> item = new HashMap<>();
                
                // 3. Adiciona os três atributos: id, LastModified e a regra em JSON.
                item.put("id", AttributeValue.builder().s(rule.getId()).build());
                item.put("LastModified", AttributeValue.builder().s(Instant.now().toString()).build());
                item.put("Rule", AttributeValue.builder().s(ruleAsJson).build());

                // Constrói a requisição para inserir o item
                PutItemRequest request = PutItemRequest.builder()
                        .tableName(TABLE_NAME)
                        .item(item)
                        .build();

                // Envia a requisição
                ddb.putItem(request);
                System.out.println("Enviada regra com ID: " + rule.getId());
            }

            System.out.println("Upload concluído com sucesso! ✅");

        } catch (DynamoDbException e) {
            System.err.println("Erro durante a comunicação com o DynamoDB: " + e.getMessage());
            e.printStackTrace();
        }
    }
}