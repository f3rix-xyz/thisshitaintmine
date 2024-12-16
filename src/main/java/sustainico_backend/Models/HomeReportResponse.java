package sustainico_backend.Models;

import java.util.Map;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class HomeReportResponse {
    private Map<String, Double> readings;
    private double averageUsage;
    private double peakUsage;
    private double continuesFlowPercentage;
    private double estimatedLeakage;

    public HomeReportResponse(Map<String, Double> readings, double averageUsage, double peakUsage, double continuesFlowPercentage, double estimatedLeakage) {
        this.readings = readings;
        this.averageUsage = averageUsage;
        this.peakUsage = peakUsage;
        this.continuesFlowPercentage = continuesFlowPercentage;
        this.estimatedLeakage = estimatedLeakage;
    }

}
