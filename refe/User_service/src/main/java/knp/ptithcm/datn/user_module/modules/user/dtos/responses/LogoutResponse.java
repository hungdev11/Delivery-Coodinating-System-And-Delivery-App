package knp.ptithcm.datn.user_module.modules.user.dtos.responses;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * DTO cho response logout
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LogoutResponse {
    
    private String message;
}
