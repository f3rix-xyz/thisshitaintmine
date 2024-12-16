package sustainico_backend.rep;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.PaginatedQueryList;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.BatchGetItemRequest;
import com.amazonaws.services.dynamodbv2.model.BatchGetItemResult;
import com.amazonaws.services.dynamodbv2.model.KeysAndAttributes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import sustainico_backend.Models.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

//import static java.util.stream.Nodes.collect;

@Repository
public class WaterReadingRepository {

    @Autowired
    private final DynamoDBMapper dynamoDBMapper;

    @Autowired
    private AmazonDynamoDB amazonDynamoDB;

    @Autowired
    public WaterReadingRepository(DynamoDBMapper dynamoDBMapper) {
        this.dynamoDBMapper = dynamoDBMapper;
    }

    @Autowired
    public LatestWaterReadingRepository latestWaterReadingRepository;

    //    @Autowired
    public Home home;

    public WaterReading save(WaterReading waterReading) {
        dynamoDBMapper.save(waterReading);
        return waterReading;
    }

    public WaterReading findById(String id) {
        return dynamoDBMapper.load(WaterReading.class, id);
    }

    public void delete(WaterReading waterReading) {
        dynamoDBMapper.delete(waterReading);
    }

    public List<WaterReading> findAll() {
        return dynamoDBMapper.scan(WaterReading.class, new DynamoDBScanExpression());
    }

    public List<WaterReading> findWaterReadingByDeviceId(String deviceId) {
        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":v1", new AttributeValue().withS(deviceId));

        DynamoDBQueryExpression<WaterReading> queryExpression = new DynamoDBQueryExpression<WaterReading>()
                .withKeyConditionExpression("deviceId = :v1")
                .withExpressionAttributeValues(eav)
                .withScanIndexForward(false);

        return dynamoDBMapper.query(WaterReading.class, queryExpression);
    }

    public String updateTodaysUsage(Home home) {
        if (home == null) {
            return null;
        }

        List<DeviceArray> devices = home.getDevices();
        if (devices == null || devices.isEmpty()) {
            return null;
        }

        // Create keys for batch get request
        Map<String, KeysAndAttributes> requestItems = new HashMap<>();
        KeysAndAttributes keysAndAttributes = new KeysAndAttributes();

        for (DeviceArray device : devices) {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("deviceId", new AttributeValue(device.getDeviceId()));
            keysAndAttributes.withKeys(key);
        }

        requestItems.put("latestWaterReading", keysAndAttributes);

        // Perform batch get request
        BatchGetItemRequest batchGetItemRequest = new BatchGetItemRequest().withRequestItems(requestItems);
        BatchGetItemResult result = amazonDynamoDB.batchGetItem(batchGetItemRequest);

        // Process the results
        double totalTodaysUsage = 0.0;
        List<Map<String, AttributeValue>> responses = result.getResponses().get("latestWaterReading");

        for (Map<String, AttributeValue> item : responses) {
            double latestFlow = Double.parseDouble(item.get("latestFlowReading").getS());
            double firstFlow = Double.parseDouble(item.get("firstFlowReading").getS());
            double todaysUsage = latestFlow - firstFlow;
            todaysUsage = Math.max(todaysUsage, 0);

            totalTodaysUsage += todaysUsage;
        }
        totalTodaysUsage = Math.max(totalTodaysUsage, 0);

        home.setTodaysUsage(String.valueOf(totalTodaysUsage));

        return String.valueOf(totalTodaysUsage);
    }


    // FOR CURRENT MONTH USAGE

    private String getStartOfMonthEpochTimestamp() {
        // Get the start of the month in India timezone
        LocalDateTime startOfMonth = LocalDate.now(ZoneId.of("Asia/Kolkata")).withDayOfMonth(1).atStartOfDay();
        // Convert to epoch milliseconds and then to string
        long epochMillis = startOfMonth.atZone(ZoneId.of("Asia/Kolkata")).toInstant().toEpochMilli();
        return Long.toString(epochMillis);
    }

    public WaterReadingPerHour findFirstReadingOfMonthByDeviceId(String deviceId, String startOfMonthEpochTimestamp) {
        // Map for expression attribute values
        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":v1", new AttributeValue().withS(deviceId));
        eav.put(":v2", new AttributeValue().withS(startOfMonthEpochTimestamp));

        // Map for expression attribute names
        Map<String, String> expressionAttributeNames = new HashMap<>();
        expressionAttributeNames.put("#ts", "fetchTimestamp");

        // DynamoDB query expression
        DynamoDBQueryExpression<WaterReadingPerHour> queryExpression = new DynamoDBQueryExpression<WaterReadingPerHour>()
                .withKeyConditionExpression("deviceId = :v1 and #ts >= :v2")
                .withExpressionAttributeValues(eav)
                .withExpressionAttributeNames(expressionAttributeNames)
                .withScanIndexForward(true) // Ensures the earliest readings come first
                .withLimit(1); // Limits the result to the first one

        // Execute the query
        PaginatedQueryList<WaterReadingPerHour> result = dynamoDBMapper.query(WaterReadingPerHour.class, queryExpression);

        // Return the first result or null
        if (result != null && !result.isEmpty()) {
            return result.get(0);
        } else {
            return null;
        }
    }

    public String updateCurrentMonthUsage(Home home) {
        if (home == null) {
            return null;
        }

        List<DeviceArray> devices = home.getDevices();
        if (devices == null || devices.isEmpty()) {
            return null;
        }

        // Get the start of the month epoch timestamp in IST
        String startOfMonthEpochTimestamp = getStartOfMonthEpochTimestamp();

        // Create keys for batch get request
        Map<String, KeysAndAttributes> requestItems = new HashMap<>();
        List<Map<String, AttributeValue>> keys = new ArrayList<>();

        for (DeviceArray device : devices) {
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("deviceId", new AttributeValue(device.getDeviceId()));
            keys.add(key);
        }

        KeysAndAttributes keysAndAttributes = new KeysAndAttributes().withKeys(keys);
        requestItems.put("latestWaterReading", keysAndAttributes);

        // Perform batch get request
        BatchGetItemRequest batchGetItemRequest = new BatchGetItemRequest().withRequestItems(requestItems);
        BatchGetItemResult result = amazonDynamoDB.batchGetItem(batchGetItemRequest);

        // Process the results
        double totalCurrentMonthUsage = 0.0;
        List<Map<String, AttributeValue>> responses = result.getResponses().get("latestWaterReading");

        for (Map<String, AttributeValue> item : responses) {
            String deviceId = item.get("deviceId").getS();
            LatestWaterReading latestReading = new LatestWaterReading();
            latestReading.setDeviceId(deviceId);
            latestReading.setLatestFlowReading(item.get("latestFlowReading").getS());
            latestReading.setFirstFlowReading(item.get("firstFlowReading").getS());
            latestReading.setTimestamp(item.get("timestamp").getS());

            WaterReadingPerHour firstReadingOfMonth = findFirstReadingOfMonthByDeviceId(deviceId, startOfMonthEpochTimestamp);

            if (latestReading != null && firstReadingOfMonth != null) {
                double latestFlow = Double.parseDouble(latestReading.getLatestFlowReading());
                double firstFlow = Double.parseDouble(firstReadingOfMonth.getFlowReading());
                double currentMonthUsage = latestFlow - firstFlow;
                currentMonthUsage = Math.max(currentMonthUsage, 0);

                totalCurrentMonthUsage += currentMonthUsage;
            }
        }

        totalCurrentMonthUsage = Math.max(totalCurrentMonthUsage, 0);

        home.setCurrentMonthUsage(String.valueOf(totalCurrentMonthUsage));

        return String.valueOf(totalCurrentMonthUsage);
    }


    // FOR LAST MONTH USAGE

    private String getStartOfPreviousMonthEpochTimestamp() {
        // Get the start of the previous month in India timezone
        LocalDateTime startOfPreviousMonth = LocalDate.now(ZoneId.of("Asia/Kolkata")).minusMonths(1).withDayOfMonth(1).atStartOfDay();
        // Convert to epoch milliseconds and then to string
        long epochMillis = startOfPreviousMonth.atZone(ZoneId.of("Asia/Kolkata")).toInstant().toEpochMilli();
        return Long.toString(epochMillis);
    }

    private String getEndOfPreviousMonthEpochTimestamp() {
        // Get the end of the previous month in India timezone
        LocalDateTime endOfPreviousMonth = LocalDate.now(ZoneId.of("Asia/Kolkata")).withDayOfMonth(1).minusDays(1).atTime(23, 59, 59);
        // Convert to epoch milliseconds and then to string
        long epochMillis = endOfPreviousMonth.atZone(ZoneId.of("Asia/Kolkata")).toInstant().toEpochMilli();
        return Long.toString(epochMillis);
    }

    public String updateLastMonthUsage(Home home) {
        if (home == null) {
            return null;
        }

        List<DeviceArray> devices = home.getDevices();
        if (devices == null || devices.isEmpty()) {
            return null;
        }

        String startOfPreviousMonthEpochTimestamp = getStartOfPreviousMonthEpochTimestamp(); // Get the start of the previous month epoch timestamp in IST
        String endOfPreviousMonthEpochTimestamp = getEndOfPreviousMonthEpochTimestamp(); // Get the end of the previous month epoch timestamp in IST

        // Create keys for batch get request
        Map<String, KeysAndAttributes> requestItems = new HashMap<>();
        List<Map<String, AttributeValue>> keys = new ArrayList<>();

        for (DeviceArray device : devices) {
            // Add first reading of the previous month key
            Map<String, AttributeValue> firstKey = new HashMap<>();
            firstKey.put("deviceId", new AttributeValue(device.getDeviceId()));
            firstKey.put("fetchTimestamp", new AttributeValue(startOfPreviousMonthEpochTimestamp));
            keys.add(firstKey);

            // Add last reading of the previous month key
            Map<String, AttributeValue> lastKey = new HashMap<>();
            lastKey.put("deviceId", new AttributeValue(device.getDeviceId()));
            lastKey.put("fetchTimestamp", new AttributeValue(endOfPreviousMonthEpochTimestamp));
            keys.add(lastKey);
        }

        KeysAndAttributes keysAndAttributes = new KeysAndAttributes().withKeys(keys);
        requestItems.put("waterReadingPerMonth", keysAndAttributes);

        // Perform batch get request
        BatchGetItemRequest batchGetItemRequest = new BatchGetItemRequest().withRequestItems(requestItems);
        BatchGetItemResult result = amazonDynamoDB.batchGetItem(batchGetItemRequest);

        // Process the results
        double totalLastMonthUsage = 0.0;
        List<Map<String, AttributeValue>> responses = result.getResponses().get("waterReadingPerMonth");

        Map<String, WaterReadingPerMonth> firstReadings = new HashMap<>();
        Map<String, WaterReadingPerMonth> lastReadings = new HashMap<>();

        for (Map<String, AttributeValue> item : responses) {
            WaterReadingPerMonth reading = new WaterReadingPerMonth();
            reading.setDeviceId(item.get("deviceId").getS());
            reading.setFlowReading(item.get("flowReading").getS());
            reading.setFetchTimestamp(item.get("fetchTimestamp").getS());
            reading.setTimestamp(item.get("timestamp").getS());

            String deviceId = reading.getDeviceId();
            if (reading.getFetchTimestamp().equals(startOfPreviousMonthEpochTimestamp)) {
                firstReadings.put(deviceId, reading);
            } else if (reading.getFetchTimestamp().equals(endOfPreviousMonthEpochTimestamp)) {
                lastReadings.put(deviceId, reading);
            }
        }

        for (DeviceArray device : devices) {
            String deviceId = device.getDeviceId();
            WaterReadingPerMonth firstReading = firstReadings.get(deviceId);
            WaterReadingPerMonth lastReading = lastReadings.get(deviceId);

            if (firstReading != null && lastReading != null) {
                double firstFlow = Double.parseDouble(firstReading.getFlowReading());
                double lastFlow = Double.parseDouble(lastReading.getFlowReading());
                double lastMonthUsage = lastFlow - firstFlow;
                lastMonthUsage = Math.max(lastMonthUsage, 0);

                totalLastMonthUsage += lastMonthUsage;
            }
        }

        totalLastMonthUsage = Math.max(totalLastMonthUsage, 0);

        home.setLastMonthUsage(String.valueOf(totalLastMonthUsage));

        return String.valueOf(totalLastMonthUsage);
    }


}
