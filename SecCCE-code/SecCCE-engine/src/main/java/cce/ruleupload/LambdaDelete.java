package cce.ruleupload;

import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.lambda.AWSLambda;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.DeleteFunctionRequest;

public class LambdaDelete {

    public static void callDelete(String name, String awsRegion) {
        try {
	    	AWSLambda awsLambda = AWSLambdaClientBuilder.standard()
	                .withCredentials(new ProfileCredentialsProvider())
	                .withRegion(awsRegion).build();
	
	        DeleteFunctionRequest delFunc = new DeleteFunctionRequest();
	        delFunc.withFunctionName(name);
	
	        //Delete the function
	        awsLambda.deleteFunction(delFunc);
	        System.out.println("The function is deleted");
        }catch (Exception e) {
			System.out.println(e);
		}
    }
}