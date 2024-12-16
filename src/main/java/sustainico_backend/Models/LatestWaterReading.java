package sustainico_backend.Models;

import com.amazonaws.services.dynamodbv2.datamodeling.*;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@DynamoDBTable(tableName = "latestWaterReading")
public class LatestWaterReading {

    @DynamoDBHashKey(attributeName = "deviceId")
    private String deviceId;

    @DynamoDBAttribute(attributeName = "latestFlowReading")
    private String latestFlowReading;

    @DynamoDBAttribute(attributeName = "firstFlowReading")
    private String firstFlowReading;

    @DynamoDBAttribute(attributeName = "firstReadingOfMonth")
    private String firstReadingOfMonth;

    @DynamoDBAttribute(attributeName = "lastReadingOfMonth")
    private String lastReadingOfMonth;

    @DynamoDBAttribute(attributeName = "timestamp")
    private String timestamp;

    @DynamoDBAttribute(attributeName = "timestampOfMonth")
    private String timestampOfMonth;
}