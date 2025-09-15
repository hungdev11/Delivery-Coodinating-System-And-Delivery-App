package knp.ptithcm.datn.user_module.modules.user.dtos.requests;

public class UpdateProfileRequest {
    private String id; // UUID as string (temporary until security context is wired)
    private String firstName;
    private String lastName;
    private String phone;
    private String address;
    private String identityNumber;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getIdentityNumber() { return identityNumber; }
    public void setIdentityNumber(String identityNumber) { this.identityNumber = identityNumber; }
}
