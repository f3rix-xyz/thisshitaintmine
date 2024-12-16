package sustainico_backend.rep;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import com.amazonaws.services.dynamodbv2.model.AttributeValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import sustainico_backend.Models.WaterReadingPerDay;
import sustainico_backend.Models.WaterReadingPerHour;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class WaterReadingPerDayRepository {

    @Autowired
    private DynamoDBMapper dynamoDBMapper;

    public void save(WaterReadingPerDay waterReadingPerDay) {
        waterReadingPerDay.generateReadingId();
        dynamoDBMapper.save(waterReadingPerDay);
    }

    public List<WaterReadingPerDay> findAll() {
        return dynamoDBMapper.scan(WaterReadingPerDay.class, new DynamoDBScanExpression());
    }

    public List<WaterReadingPerDay> findWaterReadingByDeviceId(String deviceId) {
        Map<String, AttributeValue> eav = new HashMap<>();
        eav.put(":v1", new AttributeValue().withS(deviceId));

        DynamoDBQueryExpression<WaterReadingPerDay> queryExpression = new DynamoDBQueryExpression<WaterReadingPerDay>()
                .withKeyConditionExpression("deviceId = :v1")
                .withExpressionAttributeValues(eav)
                .withScanIndexForward(false);;

        return dynamoDBMapper.query(WaterReadingPerDay.class, queryExpression);
    }

}
