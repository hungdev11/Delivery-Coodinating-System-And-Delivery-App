package ptithcm.graduation.apigateway.services.v1.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequestDto {
    private String userId;
    private String oldPassword;
    private String newPassword;
}
