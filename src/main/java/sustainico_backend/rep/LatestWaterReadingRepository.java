package sustainico_backend.rep;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sustainico_backend.Models.LatestWaterReading;

import java.util.List;

@Repository
public class LatestWaterReadingRepository {

    private final DynamoDBMapper dynamoDBMapper;

    @Autowired
    public LatestWaterReadingRepository(DynamoDBMapper dynamoDBMapper) {
        this.dynamoDBMapper = dynamoDBMapper;
    }

    public void save(LatestWaterReading latestWaterReading) {
        dynamoDBMapper.save(latestWaterReading);
    }

    public LatestWaterReading findByDeviceId(String deviceId) {
        LatestWaterReading latestWaterReading = new LatestWaterReading();
        latestWaterReading.setDeviceId(deviceId);

        DynamoDBQueryExpression<LatestWaterReading> queryExpression = new DynamoDBQueryExpression<LatestWaterReading>()
                .withHashKeyValues(latestWaterReading)
                .withConsistentRead(false);

        List<LatestWaterReading> result = dynamoDBMapper.query(LatestWaterReading.class, queryExpression);
        return result.isEmpty() ? null : result.get(0);
    }

    // Custom query to find all readings by a list of device IDs
    @Query("SELECT l FROM LatestWaterReading l WHERE l.deviceId IN :deviceIds")
    List<LatestWaterReading> findAllByDeviceIds(@Param("deviceIds") List<String> deviceIds) {
        return null;
    }
}