package cce.ruleupload;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.BlockingQueue;

import org.json.JSONObject;

import binpack.FFD;

public class CCE{

	public static void main(String[] args) {
		try {
			//-------------------------------------------------------------------------------
			// Creating new AlarmDB and AlertDB tables or deleting them before recreating.
			// Otherwise, the existing tables will be kept.
			Scanner inputScanner = new Scanner(System.in);
	        System.out.print("If you want to delete AlarmDB and AlertDB and re-create them, type (y):");
	        String rebuildTables = inputScanner.nextLine(); // Read the entire line of input
	        if (rebuildTables.equals("Y") || rebuildTables.equals("y")) {
				DynDB.rebuildTables();
			}
	        DynDB.createRulesTable("CCErules");
	        inputScanner.close();
			
	        //-------------------------------------------------------------------------------
			// Setting variables defined by the user.
			File cceConf = new File("CCE.conf");
			List<String> cceConfLines = Files.readAllLines(cceConf.toPath());
			
			// Getting parameters set by user in CCE.conf
			
			String[] temp = cceConfLines.get(0).split(":");
			int runfor = Integer.parseInt(temp[1].trim());
			
			temp = cceConfLines.get(1).split(":");
			String lambda_memsize = temp[1].trim();
			
			temp = cceConfLines.get(2).split(":");
			int bincap = Integer.parseInt(temp[1].trim());
			//temp = cceConfLines.get(3).split(":");
			int triggerGap = Integer.parseInt(temp[1].trim());
			//by default, triggerGap = RTL/bincap but could be different...
			
			temp = cceConfLines.get(3).split(":");
			String s3Bucket = temp[1].trim();
			
			temp = cceConfLines.get(4).split(":");
			String awsRegion = temp[1].trim();
			
			temp = cceConfLines.get(5).split(":");
			String awsAccountId = temp[1].trim();
	        
			temp = cceConfLines.get(6).split(":");
			String awsMasterKey = temp[1].trim();
			
	        //-------------------------------------------------------------------------------
			// Reading and loading all YML rules and uploading them to CCErules of DynamoDB
			ArrayList<SigmaMapping> sigmaMappingArray = Load_YML_Rules.loadRules();
			ArrayList<String> rulesYml = Load_YML_Rules.loadRulesYml();
			
			if (!rulesYml.isEmpty()) {
				DynamoDBUploadRules uploader = new DynamoDBUploadRules();
				for (String ruleYml : rulesYml) {
					uploader.uploadRawRule(ruleYml, awsRegion);
				}
	            
	        } else {
	            System.out.println("Nenhuma regra para enviar.");
	        }
			
			/*//Sending rules to CCErules table of DynamoDB.
			if (!sigmaMappingArray.isEmpty()) {
				DynamoDBUploadRules uploader = new DynamoDBUploadRules();
	            uploader.uploadRules(sigmaMappingArray, awsRegion);
	        } else {
	            System.out.println("Nenhuma regra para enviar.");
	        } */
			
			// Deleting current Lambdas, based on the lambda ID of existing devices.
			File configFolder = new File("devices/config/"); // YML config files
			File[] configDevices = configFolder.listFiles();
			if (configDevices.length > 0) {
				for (File configDevice : configDevices) {
					List<String> configDeviceLines = Files.readAllLines(configDevice.toPath());
					for (String line : configDeviceLines) {
						if (line.startsWith("bin:")) {
							String binName = line.substring(4);
							binName = binName.trim();
							LambdaDelete.callDelete(binName, awsRegion);
						}
					}
				}
			}
			LambdaDelete.callDelete("correlation", awsRegion);
			
			
			//verifying and creating eps, ins, and var folders, if they not exist.
			if(Files.notExists(Paths.get("devices/eps/"))) { Files.createDirectory(Paths.get("devices/eps/"));}
			if(Files.notExists(Paths.get("devices/ins/"))) { Files.createDirectory(Paths.get("devices/ins/"));}
			if(Files.notExists(Paths.get("devices/ver/"))) { Files.createDirectory(Paths.get("devices/ver/"));}
			
			for (int i = 0; i < configDevices.length; i++) {
				new File("devices/eps/"+configDevices[i].getName()).createNewFile();
				new File("devices/ins/"+configDevices[i].getName()).createNewFile();
				new File("devices/ver/"+configDevices[i].getName()).createNewFile();
			}
			
			
			//-------------------------------------------------------------------------------
	        // Setting user AWS data to archetypes (statefull and stateless...)
	        ArrayList<Path> pomPaths = new ArrayList<Path>();
	        pomPaths.add(Paths.get("..", "/statefull-lambda-archetype/src/main/resources/archetype-resources/pom.xml"));
	        pomPaths.add(Paths.get("..", "/stateless-lambda-archetype/src/main/resources/archetype-resources/pom.xml"));
	        pomPaths.add(Paths.get("..", "/statefull-lambda-archetype/src/main/resources/archetype-resources/template.yaml"));
	        pomPaths.add(Paths.get("..", "/stateless-lambda-archetype/src/main/resources/archetype-resources/template.yaml"));
	        pomPaths.add(Paths.get("..", "/stateless-lambda-archetype/src/main/resources/archetype-resources/src/main/java/__className__.java"));
	        pomPaths.add(Paths.get("..", "/statefull-lambda-archetype/src/main/resources/archetype-resources/src/main/java/__className__.java"));
	        
	        for (Path path : pomPaths) {
				Bins_DeployCode.findAndReplace(path, "yourRegion", awsRegion);
				Bins_DeployCode.findAndReplace(path, "yourBucket", s3Bucket);
				Bins_DeployCode.findAndReplace(path, "awsAccountId", awsAccountId);
			}
	        Bins_DeployCode.execShellCommand("mvn clean install", Paths.get("..", "/statefull-lambda-archetype/").toString());
	        Bins_DeployCode.execShellCommand("mvn clean install", Paths.get("..", "/stateless-lambda-archetype/").toString());
			
	        //-------------------------------------------------------------------------------
			// Separating and organizing the rule conditions by device, as well as the correlation rules.
			LinkedHashMap<String, Rule_Condition_Data> ruleCondProductArray = 
					Rule_Condition_Config.setCondRules(sigmaMappingArray, triggerGap);
			
			// Generating the code for the rule conditions by product and the correlation rules.
			Rule_Condition_GenCode.generateCondCode(ruleCondProductArray);
			LinkedHashMap<String, ArrayList<String>> verifVars = Rule_Condition_GenCode.getVerifVars();
			LinkedHashMap<String, ArrayList<String>> verifCodes = Rule_Condition_GenCode.getVerifCodes();
			LinkedHashMap<String, ArrayList<String>> condCodes = Rule_Condition_GenCode.getCondCodes();
						
			//-------------------------------------------------------------------------------
			// Estimating processing time cost for each source to run the bin packing algorithm.
			Integer binItems[] = new Integer[ruleCondProductArray.size()-1];
			ArrayList<BPdata> bpData = new ArrayList<BPdata>();
			
			int count = 0;
			for (Map.Entry<String, Rule_Condition_Data> prodCond : ruleCondProductArray.entrySet()) {
				if (!prodCond.getKey().equals("correlationRules")) {
					binItems[count] = prodCond.getValue().cost_bincap;
					bpData.add(new BPdata(prodCond.getValue().cost_bincap, prodCond.getValue().productName));
					count++;
				}
			}
			ArrayList<ArrayList<String>> bins = FFD.firstFitDec(binItems, bpData, bincap);
			
			//-------------------------------------------------------------------------------
			// Deleting old code from correlation lambda.
			Bins_DeployCode.deleteFiles(new File("rule_template_code/correlation"));
			
			//-------------------------------------------------------------------------------
			// Generating the code for the correlation rules (stateful lambda)
			Bins_DeployCode.generateRuleCodeFromTemplate("correlation", "statefull-lambda-archetype");
			Bins_DeployCode.injectRuleCode(
					verifVars.get("correlationRules"), 
					verifCodes.get("correlationRules"), 
					condCodes.get("correlationRules"), 
					"correlation");
			Bins_DeployCode.deployLambdaRule("rule_template_code/correlation", "correlation", lambda_memsize, s3Bucket, awsRegion, awsAccountId);
			
			//-------------------------------------------------------------------------------
			// Deleting events stored.
			// If you want to keep older events from previous running of CCE, comment the following two lines.
			Bins_DeployCode.deleteFiles(new File("events/"));
			Files.createDirectory(Paths.get("events"));
			
			//-------------------------------------------------------------------------------
			// Creating a hash name for each bin (stateless condition rules)
			for (ArrayList<String> bin : bins) {
				ArrayList<String> verifVar = new ArrayList<String>();
				ArrayList<String> verifCode = new ArrayList<String>();
				ArrayList<String> condCode = new ArrayList<String>();
				
				String hash = Rule_Condition_Config.getHash(bin.toString().getBytes());
				hash = "_"+hash;
				
				// Deleting old version of this lambda.
				Bins_DeployCode.deleteFiles(new File("rule_template_code/"+hash));

				// Generating code for each rule (stateless lambda)
				Bins_DeployCode.generateRuleCodeFromTemplate(hash, "stateless-lambda-archetype");
				for (String binItem : bin) {
					verifVar.addAll(verifVars.get(binItem));
					verifCode.addAll(verifCodes.get(binItem));
					condCode.addAll(condCodes.get(binItem));
					Rule_Condition_Config.setDeviceBin(binItem, hash);
					ruleCondProductArray.get(binItem).bin = hash;
				}
				Bins_DeployCode.injectRuleCode(verifVar, verifCode, condCode, hash);
				Bins_DeployCode.deployLambdaRule("rule_template_code/"+hash, hash, lambda_memsize, s3Bucket, awsRegion, awsAccountId);
			}
			Thread.sleep(10000); // Wait a moment to finish the Lambdas deployment and turn them available.
			
			//-------------------------------------------------------------------------------
			for (ArrayList<String> bin : bins) {
				String hash = Rule_Condition_Config.getHash(bin.toString().getBytes());
				hash = "_"+hash;
				Bins_DeployCode.updateLambdaConfig(hash);
				System.out.println("Updating environment variable of Lambda "+hash);
			}
			Bins_DeployCode.updateLambdaConfig("correlation");
			System.out.println("Updating environment variable of Lambda correlation");
			
			//Writing to devices' config file the current lambda ID it belongs.
			Rule_Condition_Config.writeConfigDevicesToFile(ruleCondProductArray);
			
			//-------------------------------------------------------------------------------
			// Starting the producers and consumers...
			// This CCE version incorporate an event producer for each device, according to the parameters of config files.
			// Each one has also a consumer, which organizes and triggers the respective lambdas for processing events.
			HashMap<String, ArrayList<BlockingQueue<JSONObject>>> binsBlockingQueue = new HashMap<String, ArrayList<BlockingQueue<JSONObject>>>();
			
			String evtEmpty = "{\"eventid\": "+9999+", \"src_ip\": \"10.0.0.1\", \"dst_ip\": \"10.0.0.1\", \"dvchost\": "
					+ "\"10.0.0.1\", \"dvc\": \"10.0.0.1\", \"dhost\": \"10.0.0.1\", \"msg\": \"Warming Lambda\", "
					+ "\"duser\": \"any\", \"rt\": \"whatever\"}";
			
			for (Map.Entry<String, Rule_Condition_Data> ruleCondProductEntry : ruleCondProductArray.entrySet()) {
				String key = ruleCondProductEntry.getKey();
				Rule_Condition_Data value = ruleCondProductEntry.getValue();
				// Setting blocking queues...
				if (!key.equals("correlationRules")) {
					if (binsBlockingQueue.get(value.bin) == null) {
						binsBlockingQueue.put(value.bin, new ArrayList<BlockingQueue<JSONObject>>());
					}
					binsBlockingQueue.get(value.bin).add(value.queue);
				}
			}
			
			//-------------------------------------------------------------------------------
			// Starting consumers...
			/*
			for (Map.Entry<String, ArrayList<BlockingQueue<JSONObject>>> binBlockingQueue : binsBlockingQueue.entrySet()) {
				String key = binBlockingQueue.getKey();
				Thread lambdaWarming = new Thread(new LambdaTrigger(key, evtEmpty, 0, 0, 0, awsRegion));
				lambdaWarming.start();
				System.out.println("Triggering a warming lambda for: "+ key);
			}*/
			
			for (Map.Entry<String, ArrayList<BlockingQueue<JSONObject>>> binBlockingQueue : binsBlockingQueue.entrySet()) {
				String key = binBlockingQueue.getKey();
				ArrayList<BlockingQueue<JSONObject>> value = binBlockingQueue.getValue();
				Thread thread = new Thread(new DataConsumer(key, value, triggerGap, runfor, awsRegion, awsMasterKey));
				System.out.println("Starting bin consumer: " + key);
				thread.start();
			}
			Thread.sleep(bincap*2); // Wait a moment for warming up all Detection Lambdas.
			
			//-------------------------------------------------------------------------------
			// Starting the producers...
			for (Map.Entry<String, Rule_Condition_Data> ruleCondProductEntry : ruleCondProductArray.entrySet()) {
				String key = ruleCondProductEntry.getKey();
				Rule_Condition_Data value = ruleCondProductEntry.getValue();
				
				if (!key.equals("correlationRules")) {
					Thread thread = new Thread(new DataProducer(key, value.queue, value.eps, runfor));
					System.out.println("Starting dev producer: " + key + " to bin " + value.bin);
					thread.start();
				}
			}
			
			//-------------------------------------------------------------------------------
			// Running CCE according to the user time, set on CCE.conf
			long starttime = System.currentTimeMillis();
			int triggerCount=0;
			while(System.currentTimeMillis() - starttime < (runfor)) {
				triggerCount++;
				Thread thread = new Thread(new LambdaTriggerCorrelation("correlation", triggerGap, triggerCount, awsRegion));
				thread.start();
				Thread.sleep(triggerGap);
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}
}
