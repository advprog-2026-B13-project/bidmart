export type ApiResponse<T> = {
  success?: boolean;
  message?: string;
  data?: T;
};

export type TokenPair = {
  accessToken: string;
  refreshToken: string;
};

export type LoginResponse = {
  requiresMfa?: boolean;
  accessToken?: string;
  refreshToken?: string;
  preAuthToken?: string;
  mfaType?: string;
};

export type RegisterResponse = {
  userId: string;
  email: string;
  displayName?: string;
  requiresEmailVerification?: boolean;
  verificationToken?: string;
};

export type ProfileResponse = {
  userId: string;
  email: string;
  displayName?: string;
  photoUrl?: string;
  shippingAddress?: string;
  status?: string;
  default2FAMethod?: string;
  createdAt?: string;
  role?: string;
};

export type LoginInput = {
  email: string;
  password: string;
};

export type RegisterInput = {
  email: string;
  password: string;
  displayName?: string;
};

export type VerifyEmailInput = {
  email: string;
  code: string;
};

export type ResendVerificationOtpInput = {
  email: string;
  verificationToken: string;
};

export type MfaVerifyInput = {
  preAuthToken: string;
  code: string;
};

export type LoginResult =
  | {
      requiresMfa: false;
    }
  | {
      requiresMfa: true;
      preAuthToken: string;
      mfaType?: string;
    };
