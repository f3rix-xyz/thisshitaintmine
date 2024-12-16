package sustainico_backend.Models;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceArray {
    @DynamoDBAttribute(attributeName = "deviceId")
    private String deviceId;

    @DynamoDBAttribute(attributeName = "deviceName")
    private String deviceName;
}
