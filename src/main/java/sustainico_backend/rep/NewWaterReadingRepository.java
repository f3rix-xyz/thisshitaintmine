package sustainico_backend.rep;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import sustainico_backend.Models.NewWaterReading;

import java.util.Optional;

@Repository
public class NewWaterReadingRepository {

    private final DynamoDBMapper dynamoDBMapper;

    @Autowired
    public NewWaterReadingRepository(DynamoDBMapper dynamoDBMapper) {
        this.dynamoDBMapper = dynamoDBMapper;
    }

    public void save(NewWaterReading newWaterReading) {
        dynamoDBMapper.save(newWaterReading);
    }

    public Optional<NewWaterReading> findById(String deviceId, String timestamp) {
        NewWaterReading newWaterReading = dynamoDBMapper.load(NewWaterReading.class, deviceId, timestamp);
        return Optional.ofNullable(newWaterReading);
    }

    public void delete(String deviceId, String timestamp) {
        NewWaterReading newWaterReading = dynamoDBMapper.load(NewWaterReading.class, deviceId, timestamp);
        if (newWaterReading != null) {
            dynamoDBMapper.delete(newWaterReading);
        }
    }
}
