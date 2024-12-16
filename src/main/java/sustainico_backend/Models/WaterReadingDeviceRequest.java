package sustainico_backend.Models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WaterReadingDeviceRequest {

    private String userId;
    private String deviceId;
    private String startTimestamp;
    private String endTimestamp;
    private String resolution;
}
