package knp.ptithcm.datn.user_module.modules.user.dtos.requests;

public class ResetPasswordWithOtpRequest {
    private String phone;
    private String otp;
    private String newPassword;

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }

    public String getNewPassword() { return newPassword; }
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
