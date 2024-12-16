package sustainico_backend.rep;


import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import sustainico_backend.Models.WaterReadingPerDay;
import sustainico_backend.Models.WaterReadingPerWeek;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class WaterReadingPerWeekRepository {

    @Autowired
    private DynamoDBMapper dynamoDBMapper;

    public void save(WaterReadingPerWeek waterReadingPerWeek) {
        waterReadingPerWeek.generateReadingId();
        dynamoDBMapper.save(waterReadingPerWeek);
    }

    public List<WaterReadingPerWeek> findAll() {
        return dynamoDBMapper.scan(WaterReadingPerWeek.class, new DynamoDBScanExpression());
    }

    public List<WaterReadingPerWeek> findWaterReadingByDeviceId(String deviceId) {
        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":v1", new AttributeValue().withS(deviceId));

        DynamoDBQueryExpression<WaterReadingPerWeek> queryExpression = new DynamoDBQueryExpression<WaterReadingPerWeek>()
                .withKeyConditionExpression("deviceId = :v1")
                .withExpressionAttributeValues(eav);

        return dynamoDBMapper.query(WaterReadingPerWeek.class, queryExpression);
    }

}
