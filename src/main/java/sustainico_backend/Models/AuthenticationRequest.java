package sustainico_backend.Models;


import lombok.*;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationRequest {
    private String contactNo;
    private String firebaseToken;
}
