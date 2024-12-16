package sustainico_backend.Models;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WaterReadingHomeRequest {
    private String userId;
    private String homeId;
    private String startTimestamp;
    private String endTimestamp;
    private String resolution;

}
