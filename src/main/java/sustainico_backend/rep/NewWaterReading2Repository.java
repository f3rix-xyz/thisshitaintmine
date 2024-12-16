package sustainico_backend.rep;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import sustainico_backend.Models.NewWaterReading2;

@Repository
public class NewWaterReading2Repository {
    @Autowired
    private DynamoDBMapper dynamoDBMapper;

    public NewWaterReading2 save(NewWaterReading2 newWaterReading) {
        dynamoDBMapper.save(newWaterReading);
        return newWaterReading;
    }

    public NewWaterReading2 findLatestByDeviceId(String deviceId) {
        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":deviceId", new AttributeValue().withS(deviceId));
        
        DynamoDBQueryExpression<NewWaterReading2> queryExpression = new DynamoDBQueryExpression<NewWaterReading2>()
                .withKeyConditionExpression("deviceId = :deviceId")
                .withExpressionAttributeValues(eav)
                .withScanIndexForward(false)
                .withLimit(1);
                
        List<NewWaterReading2> readings = dynamoDBMapper.query(NewWaterReading2.class, queryExpression);
        return readings.isEmpty() ? null : readings.get(0);
    }
public List<NewWaterReading2> findReadingsBetweenTimestamps(String deviceId, String startTimestamp, String endTimestamp) {
    Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":deviceId", new AttributeValue().withS(deviceId));
        eav.put(":startTime", new AttributeValue().withS(startTimestamp));
        eav.put(":endTime", new AttributeValue().withS(endTimestamp));

        Map<String, String> expressionAttrNames = new HashMap<>();
        expressionAttrNames.put("#ts", "timestamp");

        DynamoDBQueryExpression<NewWaterReading2> queryExpression = new DynamoDBQueryExpression<NewWaterReading2>()
                .withKeyConditionExpression("deviceId = :deviceId and #ts between :startTime and :endTime")
                .withExpressionAttributeValues(eav)
                .withExpressionAttributeNames(expressionAttrNames)
                .withScanIndexForward(true);

        return dynamoDBMapper.query(NewWaterReading2.class, queryExpression);
    }
}
