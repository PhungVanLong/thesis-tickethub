export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresInSeconds: number;
}

export interface RegisterRequest {
  fullName: string;
  phone: string;
  email: string;
  password: string;
}
