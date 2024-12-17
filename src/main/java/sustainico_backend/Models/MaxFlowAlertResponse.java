package sustainico_backend.Models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MaxFlowAlertResponse {
    private double percentage;
    private int totalReadings;
    private int maxFlowAlerts;
    private String date;
}
