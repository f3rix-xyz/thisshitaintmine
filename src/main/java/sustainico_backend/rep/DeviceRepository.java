package sustainico_backend.rep;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBScanExpression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import sustainico_backend.Models.Device;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class DeviceRepository {

    private final DynamoDBMapper dynamoDBMapper;

    @Autowired
    public DeviceRepository(DynamoDBMapper dynamoDBMapper) {
        this.dynamoDBMapper = dynamoDBMapper;
    }

    public Device save(Device device) {
        dynamoDBMapper.save(device);
        return device;
    }

    public Optional<Device> findByDeviceId(String deviceId) {
        return Optional.ofNullable(dynamoDBMapper.load(Device.class, deviceId));
    }

    public List<Device> findAll(){
        return dynamoDBMapper.scan(Device.class, new DynamoDBScanExpression())
                .stream()
                .collect(Collectors.toList());
    }

    public void delete(Device device) {
        dynamoDBMapper.delete(device);
    }

}
