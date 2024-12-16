package sustainico_backend.rep;


import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.model.ComparisonOperator;
import com.amazonaws.services.dynamodbv2.model.Condition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import sustainico_backend.Models.WaterReading;
import sustainico_backend.Models.WaterReadingPerHour;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class WaterReadingPerHourRepository {

    @Autowired
    private DynamoDBMapper dynamoDBMapper;

    public void save(WaterReadingPerHour waterReadingPerHour) {
        waterReadingPerHour.generateReadingId();
        dynamoDBMapper.save(waterReadingPerHour);
    }

    public List<WaterReadingPerHour> findAll() {
        return dynamoDBMapper.scan(WaterReadingPerHour.class, new DynamoDBScanExpression());
    }

    public List<WaterReadingPerHour> findWaterReadingByDeviceId(String deviceId) {
        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":v1", new AttributeValue().withS(deviceId));

        DynamoDBQueryExpression<WaterReadingPerHour> queryExpression = new DynamoDBQueryExpression<WaterReadingPerHour>()
                .withKeyConditionExpression("deviceId = :v1")
                .withExpressionAttributeValues(eav)
                .withScanIndexForward(false);;

        return dynamoDBMapper.query(WaterReadingPerHour.class, queryExpression);
    }

    public List<WaterReadingPerHour> findByDeviceIdAndFetchTimestamp(String deviceId, String fetchTimestamp) {
        WaterReadingPerHour hashKey = new WaterReadingPerHour();
        hashKey.setDeviceId(deviceId);
        hashKey.setFetchTimestamp(fetchTimestamp);

        DynamoDBQueryExpression<WaterReadingPerHour> queryExpression = new DynamoDBQueryExpression<WaterReadingPerHour>()
                .withHashKeyValues(hashKey);

        return dynamoDBMapper.query(WaterReadingPerHour.class, queryExpression);
    }







    public List<WaterReadingPerHour> findByDeviceIdAndTimestampBetween(String deviceId, String start, String end) {
        WaterReadingPerHour partitionKey = new WaterReadingPerHour();
        partitionKey.setDeviceId(deviceId);

        DynamoDBQueryExpression<WaterReadingPerHour> queryExpression = new DynamoDBQueryExpression<WaterReadingPerHour>()
                .withHashKeyValues(partitionKey)
                .withRangeKeyCondition("timestamp",
                        new Condition()
                                .withComparisonOperator(ComparisonOperator.BETWEEN.toString())
                                .withAttributeValueList(new AttributeValue().withS(start), new AttributeValue().withS(end))
                );

        return dynamoDBMapper.query(WaterReadingPerHour.class, queryExpression);
    }

    public WaterReadingPerHour getLastReadingBefore(String deviceId, String timestamp) {
        WaterReadingPerHour partitionKey = new WaterReadingPerHour();
        partitionKey.setDeviceId(deviceId);

        DynamoDBQueryExpression<WaterReadingPerHour> queryExpression = new DynamoDBQueryExpression<WaterReadingPerHour>()
                .withHashKeyValues(partitionKey)
                .withRangeKeyCondition("timestamp",
                        new Condition()
                                .withComparisonOperator(ComparisonOperator.LE.toString())
                                .withAttributeValueList(new AttributeValue().withS(timestamp))
                )
                .withScanIndexForward(false) // Descending order
                .withLimit(1);

        List<WaterReadingPerHour> result = dynamoDBMapper.query(WaterReadingPerHour.class, queryExpression);
        return result.isEmpty() ? null : result.get(0);
    }

}
