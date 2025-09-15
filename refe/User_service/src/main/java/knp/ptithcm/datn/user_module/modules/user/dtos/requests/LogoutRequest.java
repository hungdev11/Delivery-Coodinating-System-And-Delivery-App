package knp.ptithcm.datn.user_module.modules.user.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO cho request logout
 */
@Data
public class LogoutRequest {
    
    @NotBlank(message = "Refresh token không được để trống")
    private String refreshToken;
}
