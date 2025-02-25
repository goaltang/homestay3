export interface LoginRequest {
  username: string;
  password: string;
  remember: boolean;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password: string;
  phone?: string;
}

export interface UserInfo {
  id: number;
  username: string;
  email: string;
  phone?: string;
  realName?: string;
  idCard?: string;
  role: string;
}

export interface AuthResponse {
  token: string;
  user: UserInfo;
}
