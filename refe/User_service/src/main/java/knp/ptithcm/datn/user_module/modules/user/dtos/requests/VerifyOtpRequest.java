package knp.ptithcm.datn.user_module.modules.user.dtos.requests;

public class VerifyOtpRequest {
    private String phone;
    private String otp;

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }
}
