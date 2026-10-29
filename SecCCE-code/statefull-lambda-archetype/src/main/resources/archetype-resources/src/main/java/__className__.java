package ${package};

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.awt.event.ItemListener;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Iterator;

public class ${className} implements RequestHandler<List<HashMap<String, String>>, String> {
	private static final String region = "sa-east-1";
	private static final DynamoDbClient dynamoDbClient = DynamoDbClient.builder()
            .region(software.amazon.awssdk.regions.Region.of(region))
            .build();

	//private static final DynamoDbClient dynamoDbClient = DynamoDbClient.create();
    private static final String ALERT_TABLE_NAME = "AlertDB";
    private static final String ALARM_TABLE_NAME = "AlarmDB";
	

	@Override
	public String handleRequest(List<HashMap<String, String>> eventBatch, Context context) {
		Instant now = Instant.now();
		LambdaLogger logger = context.getLogger();
		ArrayList<String> alarms = new ArrayList<String>();
		int countAlarms = 0;
		
		logger.log("Instant now: " + now);
		
    	//verifVars

		//condCodes
    		
		String ret = countAlarms + " alarms created by rules: " + alarms.toString();
		countAlarms = 0;
		alarms = new ArrayList<String>();
		logger.log("Result: " + ret);
		return ret;
	}

	
	
	private ArrayList<String> getAlert(String dt_id, String timeframe, Instant now, LambdaLogger logger) {
		long amount = Long.parseLong(timeframe.substring(0, timeframe.length() - 1));
        ChronoUnit unit = ChronoUnit.MINUTES; // Padrão
        if (timeframe.endsWith("d")) unit = ChronoUnit.DAYS;
        if (timeframe.endsWith("h")) unit = ChronoUnit.HOURS;

        Instant startTime = now.minus(amount, unit);
        //logger.log("getAlert - startTime: " + startTime);
		
		Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        expressionAttributeValues.put(":v_id", AttributeValue.builder().s(dt_id).build());
        expressionAttributeValues.put(":v_start_dt", AttributeValue.builder().s(startTime.toString()).build());
        expressionAttributeValues.put(":v_end_dt", AttributeValue.builder().s(now.toString()).build());

        QueryRequest queryRequest = QueryRequest.builder()
                .tableName(ALERT_TABLE_NAME)
                .keyConditionExpression("id = :v_id and rt between :v_start_dt and :v_end_dt")
                .expressionAttributeValues(expressionAttributeValues)
                .projectionExpression("id, rt")
                .build();
        
        try {
        	ArrayList<String> itemsList = new ArrayList<String>();
        	
        	QueryResponse response = dynamoDbClient.query(queryRequest);
        	//logger.log("getAlert - query response of AlertDB: " + response.items());
        	for (Map<String, AttributeValue> item : response.items()) {
        	//for (Map<String, AttributeValue> item : dynamoDbClient.queryPaginator(queryRequest).items()) {
        		String sItem = item.get("id").s() + ";" + item.get("rt").s();
        	    itemsList.add(sItem);
        	}
            return itemsList;
        } catch (Exception e) {
            logger.log(String.format("ERROR fetching alerts for ID %s: %s", dt_id, e.getMessage()));
            return new ArrayList<String>();
        }
	}

	
	
	private void createAlarm(ArrayList<String> matches, String ruleId, Instant now, LambdaLogger logger) {
	    try {
	        Map<String, AttributeValue> alarmItem = new HashMap<>();
	        alarmItem.put("id", AttributeValue.builder().s(ruleId).build());
	        alarmItem.put("rt", AttributeValue.builder().s(now.toString()).build());
	        alarmItem.put("status", AttributeValue.builder().s("new").build());
	        List<AttributeValue> eventList = matches.stream()
	                .map(s -> AttributeValue.builder().s(s).build())
	                .collect(Collectors.toList());
	        
	        alarmItem.put("events", AttributeValue.builder().l(eventList).build());

	        PutItemRequest putRequest = PutItemRequest.builder()
	                .tableName(ALARM_TABLE_NAME)
	                .item(alarmItem)
	                .build();
	        
	        dynamoDbClient.putItem(putRequest);
	        //logger.log("createAlarm - Success: " + ruleId);
	    } catch (Exception e) {
	        logger.log(String.format("ERROR creating alarm for rule ID %s: %s", ruleId, e.getMessage()));
	    }
	}
}
