package cce.ruleupload;

import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.lambda.model.LambdaException;
import software.amazon.awssdk.core.exception.SdkException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.text.SimpleDateFormat;
import java.util.Date;


public class LambdaTrigger implements Runnable{
	String lambdaName; 
	String events;
	String awsRegion;
	int triggerCount=0;
	int blockLTsize=0;
	int triggerGap=0;
	Date tempTime = new Date();
	SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
	
	public LambdaTrigger(String lambdaName, String events, int triggerCount, int blockLTsize, int triggerGap, String awsRegion) {
		this.lambdaName = lambdaName;
		this.events = events;
		this.triggerCount = triggerCount;
		this.blockLTsize = blockLTsize;
		this.triggerGap = triggerGap;
		this.awsRegion = awsRegion;
	}
	
	@Override
	public void run() {
		tempTime.setTime(System.currentTimeMillis());
    	String lambdaTimeStr = df.format(tempTime);
		System.out.println(
				AnsiColors.ANSI_GREEN +
				lambdaTimeStr + ", Triggering: " + lambdaName + ", counter " + triggerCount + 
				", blockSize # / bytes: " + blockLTsize + ", " + events.getBytes().length +
				AnsiColors.ANSI_RESET);
		
        InvokeRequest invokeRequest = InvokeRequest.builder()
                .functionName(lambdaName)
                .payload(SdkBytes.fromString(events, StandardCharsets.UTF_8))
                .build();
                
        InvokeResponse invokeResult = null;

        try {
        	LambdaClient awsLambda = LambdaClient.builder()
                    .credentialsProvider(ProfileCredentialsProvider.create())
                    .region(Region.of(awsRegion))
                    .build();

        	invokeResult = awsLambda.invoke(invokeRequest);
        	String ans = invokeResult.payload().asString(StandardCharsets.UTF_8);
        	
        	tempTime.setTime(System.currentTimeMillis());
        	lambdaTimeStr = df.format(tempTime);
            System.out.println(
            		AnsiColors.ANSI_BRIGHT_GREEN + AnsiColors.ANSI_BOLD +
            		lambdaTimeStr + ", Receiving: " + lambdaName + ", counter " + triggerCount + 
            		", running.Time: " + ans +
            		AnsiColors.ANSI_RESET);
            
            ans = ans.substring(1, ans.length()-2);
            String[] ansVec = ans.split(",");
            int runningTime = Integer.parseInt(ansVec[0]);
            int epsTotal = 0;
            int insTotal = 0;
            Integer verTotal = 0;
            Path resultsPath = Paths.get("lambdaRunningResults/"+lambdaName);
            if (!Files.exists(resultsPath)) {
			    Files.createFile(resultsPath);
			}
            
            for (int i = 1; i < ansVec.length; i++) {
				String[] result = ansVec[i].split("_");
				Path filePath = Paths.get("devices/"+result[2] + "/" + result[0] + "." + result[1]+".yml");
				
				String value = new String();
				int result3 = Integer.parseInt(result[3]);
				if(result[2].equals("ins")) {
					String[] resultTemp = ansVec[i+1].split("_");
					int epsD = Integer.parseInt(resultTemp[3]);
					int insD = Integer.parseInt(result[3]);
					value = String.valueOf(100*insD / epsD);
					if (value.contains("NaN")) {
						value = "0";
					}
					insTotal+= insD;
					value += "\n";
					Files.write(filePath, value.getBytes(), StandardOpenOption.APPEND);
				}else if(result[2].equals("eps") && result3 > 0) {
					value = (result3 / (triggerGap/1000)) + "\n";
					epsTotal += result3;
					Files.write(filePath, value.getBytes(), StandardOpenOption.APPEND);
				}else if(result[2].equals("ver") && result3 > 0) {
					verTotal += result3;
					value = verTotal + "\n";
					Files.write(filePath, value.getBytes(), StandardOpenOption.APPEND);
				}
				
			}
            String lambdaResults = runningTime+","+epsTotal+","+insTotal+","+verTotal+"\n";
            Files.write(resultsPath, lambdaResults.getBytes(), StandardOpenOption.APPEND);
            
        } catch (LambdaException | IOException e) { // 'ServiceException' (v1) -> 'LambdaException' (v2)
            System.out.println(lambdaName + ": " + triggerCount + "Lambda or IO Exception: "+ e);
        } catch (SdkException e) { // Um catch-all para outras exceções do SDK
            System.out.println("Erro inesperado do AWS SDK: " + lambdaName + ": " + triggerCount);
            System.out.println(e);
        }
	}
}

