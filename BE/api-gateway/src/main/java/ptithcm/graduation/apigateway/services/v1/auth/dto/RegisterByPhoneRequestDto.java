package ptithcm.graduation.apigateway.services.v1.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterByPhoneRequestDto {
    private String phone;
    private String password;
    private String firstName;
    private String lastName;
}
