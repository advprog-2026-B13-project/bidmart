export type ApiResponse<T> = {
  success?: boolean;
  message?: string;
  data?: T;
};

export type TokenPair = {
  accessToken: string;
  refreshToken: string;
};

export type SessionSummaryResponse = {
  sessionId: string;
  isActive?: boolean;
  createdAt?: string;
  lastLoginAt?: string;
  expiresAt?: string;
  deviceInfo?: string;
  ipAddress?: string;
  locationLabel?: string;
  isCurrent?: boolean;
};

export type LoginResponse = {
  requiresMfa?: boolean;
  accessToken?: string;
  refreshToken?: string;
  preAuthToken?: string;
  mfaType?: string;
  requiresSessionReplacement?: boolean;
  sessionReplacementToken?: string;
  activeSessions?: SessionSummaryResponse[];
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
      requiresSessionReplacement?: false;
    }
  | {
      requiresMfa: true;
      preAuthToken: string;
      mfaType?: string;
      requiresSessionReplacement?: false;
    }
  | {
      requiresMfa?: false;
      requiresSessionReplacement: true;
      sessionReplacementToken: string;
      activeSessions: SessionSummaryResponse[];
    };

export type SessionReplacementConfirmationInput = {
  sessionReplacementToken: string;
  replaceOldestSession: boolean;
};

export type AdminRoleResponse = {
  roleId: number;
  roleName: string;
  permissions: string[];
};

export type AdminCreateRoleInput = {
  roleName: string;
  permissions: string[];
};

export type AdminSetRolePermissionsInput = {
  permissions: string[];
};

export type AdminAssignUserRoleInput = {
  roleId: number;
};

export type AdminManagedUserResponse = {
  userId: string;
  email: string;
  displayName?: string;
  status?: string;
  createdAt?: string;
  roleId?: number;
  roleName?: string;
};

export type AdminManagedUsersPageResponse = {
  users: AdminManagedUserResponse[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  hasNext: boolean;
};
