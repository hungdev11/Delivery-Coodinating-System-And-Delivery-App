package knp.ptithcm.datn.user_module.modules.user.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO cho request refresh token
 */
@Data
public class RefreshTokenRequest {
    
    @NotBlank(message = "Refresh token không được để trống")
    private String refreshToken;
}
