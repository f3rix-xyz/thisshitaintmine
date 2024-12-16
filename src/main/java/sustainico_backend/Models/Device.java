package sustainico_backend.Models;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import lombok.*;

import java.time.Instant;
import java.util.List;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@DynamoDBTable(tableName = "device")
public class Device {

    @DynamoDBHashKey(attributeName = "deviceId")
    private String deviceId;

    @DynamoDBAttribute(attributeName = "deviceName")
    private String deviceName;

    @DynamoDBAttribute(attributeName = "status")
    private String status;

    @DynamoDBAttribute(attributeName = "deviceType")
    private String deviceType = "WaterMeter1";

    @DynamoDBAttribute(attributeName = "billAccNo")
    private String billAccNo;

    @DynamoDBAttribute(attributeName = "ownerId")
    private String ownerId;

    @DynamoDBAttribute(attributeName = "viewerId")
    private List<String> viewerId;

    @DynamoDBAttribute(attributeName = "initialPin")
    private String initialPin;

    @DynamoDBAttribute(attributeName = "digitalPin")
    private String digitalPin;

    @DynamoDBAttribute(attributeName = "pinExpiryTimestamp")
    private Long pinExpiryTimestamp;

    @DynamoDBAttribute(attributeName = "homeIds")
    private List<String> homeIds;

    @DynamoDBAttribute(attributeName = "installmentDate")
    private String installmentDate = String.valueOf(Instant.now().getEpochSecond());

}
