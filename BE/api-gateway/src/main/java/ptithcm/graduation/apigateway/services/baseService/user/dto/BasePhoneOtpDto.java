package ptithcm.graduation.apigateway.services.baseService.user.dto;

public class BasePhoneOtpDto {
    private String message;
    private Boolean exists;
    private String otp;
    private Boolean valid;

    // Default constructor
    public BasePhoneOtpDto() {}

    // Getters and Setters
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Boolean getExists() { return exists; }
    public void setExists(Boolean exists) { this.exists = exists; }

    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }

    public Boolean getValid() { return valid; }
    public void setValid(Boolean valid) { this.valid = valid; }
}
