package sustainico_backend.Models;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class StatusChangeResponse {
    private String timestamp;
    private Map<String, Boolean> status;
}