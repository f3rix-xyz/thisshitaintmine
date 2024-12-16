package sustainico_backend.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter

public class ApiResponse<T> {
    private int statusCode;
    private T payload;
    private String status;
    private Error error;
    private String errorDescription;
}


