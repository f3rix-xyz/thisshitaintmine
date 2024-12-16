package sustainico_backend.Models;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.*;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@DynamoDBTable(tableName = "waterReadingPerHour")
public class WaterReadingPerHour {

    @DynamoDBHashKey(attributeName = "deviceId")
    private String deviceId;

    @DynamoDBAttribute(attributeName = "flowReading")
    private String flowReading;

    @DynamoDBAttribute(attributeName = "timestamp")
    private String timestamp;

    @DynamoDBRangeKey(attributeName = "fetchTimestamp")
    private String fetchTimestamp;

    @DynamoDBAttribute(attributeName = "readingId")
    private String readingId;

    public void generateReadingId() {
        this.readingId = this.deviceId + "-" + this.fetchTimestamp;
    }
}
