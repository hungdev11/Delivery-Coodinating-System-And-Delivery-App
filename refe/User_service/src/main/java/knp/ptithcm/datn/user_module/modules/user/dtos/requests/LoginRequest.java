package knp.ptithcm.datn.user_module.modules.user.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO cho request đăng nhập
 */
@Data
public class LoginRequest {
    
    @NotBlank(message = "Username không được để trống")
    private String username;
    
    @NotBlank(message = "Password không được để trống")
    private String password;
}
