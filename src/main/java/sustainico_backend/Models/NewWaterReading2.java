package sustainico_backend.Models;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@DynamoDBTable(tableName = "newWaterReading2")
public class NewWaterReading2 {

    @DynamoDBHashKey(attributeName = "deviceId")
    private String deviceId;

    @DynamoDBRangeKey(attributeName = "timestamp")
    private String timestamp;

    @DynamoDBAttribute(attributeName = "status")
    private Map<String, Boolean> status;

    @DynamoDBAttribute(attributeName = "liters")
    private long liters;

    @DynamoDBAttribute(attributeName = "milliliters")
    private long milliliters;

    @DynamoDBAttribute(attributeName = "readingId")
    private String readingId;

    public void generateReadingId() {
        this.readingId = this.deviceId + "-" + this.timestamp;
    }
}
