package knp.ptithcm.datn.user_module.modules.user.dtos.requests;

public class RegisterByPhoneRequest {
    private String phone;
    private String password;
    private String firstName;
    private String lastName;

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
}
