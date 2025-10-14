export class LoginForm {
  username: string
  password: string
  rememberMe: boolean

  constructor(username: string, password: string, rememberMe: boolean) {
    this.username = username
    this.password = password
    this.rememberMe = rememberMe
  }
}

export class UserDto {
  id: string
  email: string
  username: string
  firstName: string
  lastName: string
  roles: string[]
  createdAt: string
  updatedAt: string

  constructor(
    id: string,
    email: string,
    username: string,
    firstName: string,
    lastName: string,
    roles: string[],
    createdAt: string,
    updatedAt: string,
  ) {
    this.id = id
    this.email = email
    this.username = username
    this.firstName = firstName
    this.lastName = lastName
    this.roles = roles
    this.createdAt = createdAt
    this.updatedAt = updatedAt
  }
}

export class TokenPayload {
  token: string
  tokenType: string
  iat: number
  exp: number
  user: UserDto
  roles: string[]

  constructor(
    token: string,
    tokenType: string,
    iat: number,
    exp: number,
    user: UserDto,
    roles: string[],
  ) {
    this.token = token
    this.tokenType = tokenType
    this.iat = iat
    this.exp = exp
    this.user = user
    this.roles = roles
  }
}
