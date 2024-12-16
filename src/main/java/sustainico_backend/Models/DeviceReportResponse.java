package sustainico_backend.Models;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Data
public class DeviceReportResponse {
    private List<?> readings;
    private double averageUsage;
    private double peakUsage;
    private double continuesFlowPercentage;
    private double estimatedLeakage;
}
