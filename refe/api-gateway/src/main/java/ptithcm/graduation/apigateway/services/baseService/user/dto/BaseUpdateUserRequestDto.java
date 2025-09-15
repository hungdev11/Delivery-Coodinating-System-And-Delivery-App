package ptithcm.graduation.apigateway.services.baseService.user.dto;

import java.util.List;

public class BaseUpdateUserRequestDto {
    private String id;
    private String email;
    private String username;
    private String firstName;
    private String lastName;
    private String phone;
    private String address;
    private String identityNumber;
    private List<String> roles;

    // Default constructor
    public BaseUpdateUserRequestDto() {}

    // All args constructor
    public BaseUpdateUserRequestDto(String id, String email, String username, String firstName, 
                                   String lastName, String phone, String address, 
                                   String identityNumber, List<String> roles) {
        this.id = id;
        this.email = email;
        this.username = username;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.address = address;
        this.identityNumber = identityNumber;
        this.roles = roles;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

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

    public List<String> getRoles() { return roles; }
    public void setRoles(List<String> roles) { this.roles = roles; }
}
