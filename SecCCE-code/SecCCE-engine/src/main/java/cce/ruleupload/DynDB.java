package cce.ruleupload;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ResourceNotFoundException;
import com.amazonaws.services.dynamodbv2.model.StreamSpecification;
import com.amazonaws.services.dynamodbv2.model.StreamViewType;
import com.amazonaws.services.dynamodbv2.model.UpdateTableRequest;

public class DynDB {
	static AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
	static DynamoDB dynamoDB = new DynamoDB(client);
	
	
	public static void createCCETables(String tableName) {
		try {
            List<AttributeDefinition> attributeDefinitions = new ArrayList<AttributeDefinition>();
            attributeDefinitions.add(new AttributeDefinition().withAttributeName("id").withAttributeType("S"));
            attributeDefinitions.add(new AttributeDefinition().withAttributeName("rt").withAttributeType("S"));

            List<KeySchemaElement> keySchema = new ArrayList<>();
            keySchema.add(new KeySchemaElement().withAttributeName("id").withKeyType(KeyType.HASH)); // Partition
            keySchema.add(new KeySchemaElement().withAttributeName("rt").withKeyType(KeyType.RANGE)); // Sort
            
            CreateTableRequest request = new CreateTableRequest().withTableName(tableName).withKeySchema(keySchema)
                .withAttributeDefinitions(attributeDefinitions).withBillingMode("PAY_PER_REQUEST");

            System.out.println("Issuing CreateTable request for " + tableName);
            Table table = dynamoDB.createTable(request);

            System.out.println("Waiting for " + tableName + " to be created...this may take a while...");
            table.waitForActive();
            
            // --- INÍCIO: Habilitar DynamoDB Streams ---
            System.out.println("Habilitando DynamoDB Streams para a tabela " + tableName);

            // 1. Crie a especificação do Stream.
            StreamSpecification streamSpecification = new StreamSpecification();
            streamSpecification.setStreamEnabled(true);
            // 2. Defina quais dados serão enviados para o Stream.
            // NEW_AND_OLD_IMAGES é a opção mais completa.
            streamSpecification.setStreamViewType(StreamViewType.NEW_AND_OLD_IMAGES);

            // 3. Crie a requisição de atualização da tabela.
            UpdateTableRequest updateTableRequest = new UpdateTableRequest()
                .withTableName(tableName)
                .withStreamSpecification(streamSpecification);

            // 4. Execute a atualização.
            client.updateTable(updateTableRequest);
            System.out.println("Streams habilitados com sucesso para " + tableName + " ✅");
            // --- FIM: Habilitar DynamoDB Streams ---
        }
        catch (Exception e) {
            System.err.println("Request failed for " + tableName);
            System.err.println(e.getMessage());
        }
	}
	
	
	public static boolean createRulesTable(String tableName) {
		try {
			if (!tableExist(tableName)) {
	            List<AttributeDefinition> attributeDefinitions = new ArrayList<AttributeDefinition>();
	            attributeDefinitions.add(new AttributeDefinition().withAttributeName("id").withAttributeType("S"));
	
	            List<KeySchemaElement> keySchema = new ArrayList<>();
	            keySchema.add(new KeySchemaElement().withAttributeName("id").withKeyType(KeyType.HASH)); // Partition
	            
	            CreateTableRequest request = new CreateTableRequest().withTableName(tableName).withKeySchema(keySchema)
	                .withAttributeDefinitions(attributeDefinitions).withBillingMode("PAY_PER_REQUEST");
	
	            System.out.println("Issuing CreateTable request for " + tableName);
	            Table table = dynamoDB.createTable(request);
	
	            System.out.println("Waiting for " + tableName + " to be created...this may take a while...");
	            table.waitForActive();
	            return true;
			} else return false;
        }
        catch (Exception e) {
            System.err.println("CreateTable request failed for " + tableName);
            System.err.println(e.getMessage());
            return false;
        }
	}
	
	
	public static void deleteTable(String tableName) {
		AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();
		DynamoDB dynamoDB = new DynamoDB(client);

		Table table = dynamoDB.getTable(tableName);
		if (tableExist(tableName)) {
			table.delete();
			try {
				table.waitForDelete();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	
	public static boolean tableExist(String tableName) {
		AmazonDynamoDB client = AmazonDynamoDBClientBuilder.standard().build();

        try {
            DescribeTableRequest request = new DescribeTableRequest().withTableName(tableName);
            client.describeTable(request);

            // If no exception is thrown, the table exists
            System.out.println("Table '" + tableName + "' exists.");
            return true;
        } catch (ResourceNotFoundException e) {
            // If ResourceNotFoundException is caught, the table does not exist
            System.out.println("Table '" + tableName + "' does not exist.");
            return false;
        } catch (Exception e) {
            System.err.println("An error occurred while checking table existence: " + e.getMessage());
            return false;
        } 
    }
	
	
	public static void execShellCommand(String command, String path) {
		try {
			Process process = Runtime.getRuntime().exec(
					command, 
					null, 
					new File(path));

			StringBuilder output = new StringBuilder();
			BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

			String line;
			while ((line = reader.readLine()) != null) {
				output.append(line + "\n");
			}

			int exitVal = process.waitFor();
			if (exitVal == 0) {
				System.out.println(output);
				//System.exit(0);
			} else {
				// abnormal...
				System.out.println(output);
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public static void rebuildTables() {
		deleteTable("AlertDB");
		createCCETables("AlertDB");
		deleteTable("AlarmDB");
		createCCETables("AlarmDB");
	}
	
	
	public static void main(String args[]) {
		String tableName = "AlertDB";
		String command = "aws dynamodb create-table "
				+ "  --table-name " + tableName
				+ "  --attribute-definitions '["
				+ "    {"
				+ "      \"AttributeName\": \"id\","
				+ "      \"AttributeType\": \"S\""
				+ "    },"
				+ "    {"
				+ "      \"AttributeName\": \"rt\","
				+ "      \"AttributeType\": \"S\""
				+ "    },"
				+ "  ]'"
				+ "  --key-schema '["
				+ "    {"
				+ "      \"AttributeName\": \"id\","
				+ "      \"KeyType\": \"HASH\""
				+ "    },"
				+ "    {"
				+ "      \"AttributeName\": \"rt\","
				+ "      \"KeyType\": \"RANGE\""
				+ "    },"
				+ "  ]' "
				+ "  --billing-mode: \"PAY_PER_REQUEST\"";
		deleteTable("AlertDB");
		createCCETables("AlertDB");
		deleteTable("AlarmDB");
		createCCETables("AlarmDB");
	}
}
