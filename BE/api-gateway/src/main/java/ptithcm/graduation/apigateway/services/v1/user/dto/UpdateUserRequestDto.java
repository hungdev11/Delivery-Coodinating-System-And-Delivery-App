package ptithcm.graduation.apigateway.services.v1.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserRequestDto {
    private String id;
    private String firstName;
    private String lastName;
    private String phone;
    private String address;
    private String identityNumber;
}
