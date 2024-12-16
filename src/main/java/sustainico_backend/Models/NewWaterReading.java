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
@DynamoDBTable(tableName = "newWaterReading")
public class NewWaterReading {

    @DynamoDBHashKey(attributeName = "deviceId")
    private String deviceId;

    @DynamoDBRangeKey(attributeName = "timestamp")
    private String timestamp;

    @DynamoDBAttribute(attributeName = "index")
    private int index;

    @DynamoDBAttribute(attributeName = "alerts")
    private Map<String, Boolean> alerts;

    @DynamoDBAttribute(attributeName = "status")
    private Map<String, Boolean> status;

    @DynamoDBAttribute(attributeName = "totalizer")
    private int totalizer;

    @DynamoDBAttribute(attributeName = "intraDay")
    private List<Integer> intraDay;

    @DynamoDBAttribute(attributeName = "readingId")
    private String readingId;

    @DynamoDBAttribute(attributeName = "key")
    private int key;

    public void generateReadingId() {
        this.readingId = this.deviceId + "-" + this.timestamp;
    }
}
