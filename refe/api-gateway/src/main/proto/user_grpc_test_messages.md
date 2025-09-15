# Sample gRPC Messages for UserService Testing

## Authentication Methods

### Login
```json
{
  "username": "dev",
  "password": "dev"
}
```

### Refresh Token
```json
{
  "refresh_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

### Logout
```json
{
  "refresh_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

## User Management Methods

### CreateUser
```json
{
  "username": "testuser1",
  "email": "testuser1@example.com",
  "first_name": "Test",
  "last_name": "User",
  "phone": "1234567890",
  "address": "123 Main St",
  "identity_number": "ID123456",
  "password": "StrongPassword123",
  "roles": ["admin", "staff"]
}
```

### UpdateUser
```json
{
  "id": "<user-uuid>",
  "username": "updateduser1",
  "email": "updateduser1@example.com",
  "first_name": "Updated",
  "last_name": "User",
  "phone": "0987654321",
  "address": "456 New St",
  "identity_number": "ID654321",
  "roles": ["client"]
}
```

### UpdateUserPassword
```json
{
  "id": "<user-uuid>",
  "new_password": "NewStrongPassword456"
}
```

### DeleteUser
```json
{
  "id": "<user-uuid>"
}
```

### GetUserById
```json
{
  "id": "<user-uuid>"
}
```

### GetUserByUsername
```json
{
  "username": "testuser1"
}
```

### GetUserByEmail
```json
{
  "email": "testuser1@example.com"
}
```

### ListUsers
```json
{
  "page": 1,
  "size": 10
}
```

### UpdateUserStatus
```json
{
  "id": "<user-uuid>",
  "status": 1
}
```

### UpdateUserRole
```json
{
  "id": "<user-uuid>",
  "role": 2
}
```

## Phone & OTP Methods

### RegisterByPhone
```json
{
  "phone": "+84123456789",
  "password": "Password123!",
  "first_name": "Khoa",
  "last_name": "Nguyen"
}
```

### PhoneExists
```json
{
  "phone": "+84123456789"
}
```

### SendOtp
```json
{
  "phone": "+84123456789"
}
```

### VerifyOtp
```json
{
  "phone": "+84123456789",
  "otp": "000000"
}
```

### ResetPasswordWithOtp
```json
{
  "phone": "+84123456789",
  "otp": "000000",
  "new_password": "NewPassword456!"
}
```

## Profile Management

### UpdateProfile
```json
{
  "id": "<user-uuid>",
  "first_name": "Khoa",
  "last_name": "Nguyen",
  "phone": "+84123456789",
  "address": "123 Main St",
  "identity_number": "ID123456"
}
```

## Status Values
- **UserStatus**: 0 = ACTIVE, 1 = INACTIVE, 2 = BLOCKED
- **UserRole**: 0 = USER, 1 = ADMIN, 2 = STAFF

> Replace `<user-uuid>` with the actual user UUID you want to test. 
