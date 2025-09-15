package knp.ptithcm.datn.user_module.modules.user.dtos.responses;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * DTO cho response đăng nhập
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {
    
    private String message;
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Integer expiresIn;
    private UserResponse user;
}
