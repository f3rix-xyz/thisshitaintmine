package sustainico_backend.Models;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DeviceDetailsResponse {
    private String deviceId;
    private String deviceName;
    private String deviceType;
    private String billAccNo;
    private String ownerName;
    private String ownerId;
}