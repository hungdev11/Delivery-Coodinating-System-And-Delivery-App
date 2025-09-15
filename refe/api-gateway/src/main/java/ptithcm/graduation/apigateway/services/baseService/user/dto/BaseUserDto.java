package ptithcm.graduation.apigateway.services.baseService.user.dto;

import java.util.List;
import java.time.LocalDateTime;

public class BaseUserDto {
    private String id;
    private String keycloakId;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phone;
    private String address;
    private String identityNumber;
    private List<String> roles;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Default constructor
    public BaseUserDto() {}

    // All args constructor
    public BaseUserDto(String id, String keycloakId, String username, String email, 
                    String firstName, String lastName, String phone, String address, 
                    String identityNumber, List<String> roles, Integer status, 
                    LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.keycloakId = keycloakId;
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.phone = phone;
        this.address = address;
        this.identityNumber = identityNumber;
        this.roles = roles;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getKeycloakId() { return keycloakId; }
    public void setKeycloakId(String keycloakId) { this.keycloakId = keycloakId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

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

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
