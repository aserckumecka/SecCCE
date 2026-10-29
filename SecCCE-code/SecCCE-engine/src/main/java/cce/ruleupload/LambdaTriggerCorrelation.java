package cce.ruleupload;

import java.text.SimpleDateFormat;
import java.util.Date;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.amazonaws.services.lambda.model.ServiceException;


public class LambdaTriggerCorrelation implements Runnable{
	String lambdaName; 
	String awsRegion;
	long triggerGap;
	int triggerCount;
	Date tempTime = new Date();
	SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
	
	public LambdaTriggerCorrelation(String lambdaName, long triggerGap, int triggerCount, String awsRegion) {
		this.lambdaName = lambdaName;
		this.triggerGap = (long) triggerGap;
		this.triggerCount = triggerCount;
		this.awsRegion = awsRegion;
	}
	
	@Override
	public void run() {
		InvokeRequest invokeRequest = new InvokeRequest()
                .withFunctionName(lambdaName)
                .withPayload("{\"test\":\"test\"}");
        InvokeResult invokeResult = null;
        
        try {
            AWSLambda awsLambda = AWSLambdaClientBuilder.standard()
                    .withCredentials(new ProfileCredentialsProvider())
                    .withRegion(awsRegion).build();
			
	            tempTime.setTime(System.currentTimeMillis());
	        	String lambdaTimeStr = df.format(tempTime);
	            System.out.println("\n" 
	            		+AnsiColors.ANSI_YELLOW 
	            		+lambdaTimeStr + ", " + triggerCount + ", Triggering Correlation"
	            		+ AnsiColors.ANSI_RESET
	            		+ "\n");
            	
	            invokeResult = awsLambda.invoke(invokeRequest);
	            String ans = new String(invokeResult.getPayload().array());

	            long timeSpent = System.currentTimeMillis() - tempTime.getTime();
	            tempTime.setTime(System.currentTimeMillis());
	        	lambdaTimeStr = df.format(tempTime);
	            System.out.println("\n"
	            		+AnsiColors.ANSI_BRIGHT_YELLOW + AnsiColors.ANSI_BOLD
	            		+lambdaTimeStr + ", " + triggerCount + ", Correlation running.Time: "+ timeSpent + ", " + ans + "\n"
	            		+ AnsiColors.ANSI_RESET
	            		);
        } catch (ServiceException e) {
        	System.out.println(lambdaName + ": " + triggerCount);
        	System.out.println(e);
        }
	}
}