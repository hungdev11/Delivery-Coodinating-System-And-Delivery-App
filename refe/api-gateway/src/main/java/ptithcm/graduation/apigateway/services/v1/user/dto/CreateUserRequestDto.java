package ptithcm.graduation.apigateway.services.v1.user.dto;

import java.util.List;

public class CreateUserRequestDto {
    private String username;
    private String email;
    private String password;
    private String firstName;
    private String lastName;
    private String phone;
    private String address;
    private String identityNumber;
    private List<String> roles;

    // Default constructor
    public CreateUserRequestDto() {}

    // All args constructor
    public CreateUserRequestDto(String username, String email, String password, 
                                   String firstName, String lastName, String phone, 
                                   String address, String identityNumber, List<String> roles) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.address = address;
        this.identityNumber = identityNumber;
        this.roles = roles;
    }

    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

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
