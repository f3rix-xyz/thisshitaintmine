package sustainico_backend.rep;


import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import sustainico_backend.Models.WaterReadingPerDay;
import sustainico_backend.Models.WaterReadingPerMonth;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class WaterReadingPerMonthRepository {

    @Autowired
    private DynamoDBMapper dynamoDBMapper;

    public void save(WaterReadingPerMonth waterReadingPerMonth) {
        waterReadingPerMonth.generateReadingId();
        dynamoDBMapper.save(waterReadingPerMonth);
    }

    public List<WaterReadingPerMonth> findAll() {
        return dynamoDBMapper.scan(WaterReadingPerMonth.class, new DynamoDBScanExpression());
    }

    public List<WaterReadingPerMonth> findWaterReadingByDeviceId(String deviceId) {
        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":v1", new AttributeValue().withS(deviceId));

        DynamoDBQueryExpression<WaterReadingPerMonth> queryExpression = new DynamoDBQueryExpression<WaterReadingPerMonth>()
                .withKeyConditionExpression("deviceId = :v1")
                .withExpressionAttributeValues(eav)
                .withScanIndexForward(false);;

        return dynamoDBMapper.query(WaterReadingPerMonth.class, queryExpression);
    }

}
