import { apiFetch, ApiError } from "./api-client";
import type {
  AdminAssignUserRoleInput,
  AdminCreateRoleInput,
  AdminManagedUsersPageResponse,
  AdminRoleResponse,
  AdminSetRolePermissionsInput,
  ApiResponse,
  BidResponse,
  LoginInput,
  LoginResponse,
  LoginResult,
  MfaVerifyInput,
  OtherUserProfileResponse,
  ProfileUpdateInput,
  ProfileResponse,
  SessionReplacementConfirmationInput,
  SessionSummaryResponse,
  ResendVerificationOtpInput,
  RegisterInput,
  RegisterResponse,
  TotpSetupResponse,
  VerifyEmailInput,
} from "./types";

function unwrapOrThrow<T>(payload: ApiResponse<T>) {
  if (payload.success === false) {
    throw new Error(payload.message || "Request failed");
  }

  if (payload.data === undefined || payload.data === null) {
    throw new Error(payload.message || "Missing response data");
  }

  return payload.data;
}

export async function login(input: LoginInput): Promise<LoginResult> {
  const payload = await apiFetch<ApiResponse<LoginResponse>>(
    "/api/auth/login",
    {
      method: "POST",
      body: JSON.stringify(input),
    },
    { auth: false },
  );

  const data = unwrapOrThrow(payload);

  if (data.requiresMfa) {
    if (!data.preAuthToken) {
      throw new Error("Login requires MFA, but preAuthToken is missing");
    }

    return {
      requiresMfa: true,
      preAuthToken: data.preAuthToken,
      mfaType: data.mfaType,
    };
  }

  if (data.requiresSessionReplacement) {
    if (!data.sessionReplacementToken) {
      throw new Error("Login requires session replacement confirmation, but token is missing");
    }

    return {
      requiresMfa: false,
      requiresSessionReplacement: true,
      sessionReplacementToken: data.sessionReplacementToken,
      activeSessions: data.activeSessions || [],
    };
  }

  return {
    requiresMfa: false,
  };
}

export async function confirmSessionReplacement(
  input: SessionReplacementConfirmationInput,
): Promise<LoginResult> {
  const payload = await apiFetch<ApiResponse<LoginResponse>>(
    "/api/auth/confirm-session-replacement",
    {
      method: "POST",
      body: JSON.stringify(input),
    },
    { auth: false },
  );

  const data = unwrapOrThrow(payload);

  if (data.requiresMfa) {
    if (!data.preAuthToken) {
      throw new Error("Session replacement confirmation returned MFA without preAuthToken");
    }

    return {
      requiresMfa: true,
      preAuthToken: data.preAuthToken,
      mfaType: data.mfaType,
    };
  }

  return {
    requiresMfa: false,
  };
}

export async function register(input: RegisterInput): Promise<RegisterResponse> {
  const payload = await apiFetch<ApiResponse<RegisterResponse>>(
    "/api/auth/register",
    {
      method: "POST",
      body: JSON.stringify(input),
    },
    { auth: false },
  );

  return unwrapOrThrow(payload);
}

export async function verifyRegistrationEmail(input: VerifyEmailInput): Promise<void> {
  const payload = await apiFetch<ApiResponse<null>>(
    "/api/auth/verify-email",
    {
      method: "POST",
      body: JSON.stringify(input),
    },
    { auth: false },
  );

  if (payload.success === false) {
    throw new Error(payload.message || "Email verification failed");
  }
}

export async function resendRegistrationVerificationOtp(
  input: ResendVerificationOtpInput,
): Promise<void> {
  const payload = await apiFetch<ApiResponse<null>>(
    "/api/auth/resend-verification-otp",
    {
      method: "POST",
      body: JSON.stringify(input),
    },
    { auth: false },
  );

  if (payload.success === false) {
    throw new Error(payload.message || "Failed to resend verification OTP");
  }
}

export async function verifyMfa(input: MfaVerifyInput): Promise<void> {
  const payload = await apiFetch<ApiResponse<unknown>>(
    "/api/auth/mfa/verify",
    {
      method: "POST",
      body: JSON.stringify(input),
    },
    { auth: false },
  );

  if (payload.success === false) {
    throw new Error(payload.message || "MFA verification failed");
  }
}

export async function requestEmailOtp(preAuthToken: string): Promise<void> {
  const payload = await apiFetch<ApiResponse<null>>(
    "/api/auth/mfa/request-email-otp",
    {
      method: "POST",
      body: JSON.stringify({ preAuthToken }),
    },
    { auth: false },
  );

  if (payload.success === false) {
    throw new Error(payload.message || "Failed to send email OTP");
  }
}

export async function getProfile(): Promise<ProfileResponse> {
  const payload = await apiFetch<ApiResponse<ProfileResponse>>("/api/auth/profile", {
    method: "GET",
  });

  return unwrapOrThrow(payload);
}

export async function getOtherUserProfile(targetUserId: string): Promise<OtherUserProfileResponse> {
  const payload = await apiFetch<ApiResponse<OtherUserProfileResponse>>(
    `/api/auth/profile/users/${targetUserId}`,
    {
      method: "GET",
    },
  );

  return unwrapOrThrow(payload);
}

export async function listMyBids(): Promise<BidResponse[]> {
  const payload = await apiFetch<ApiResponse<BidResponse[]>>("/api/bidding/my-bids", {
    method: "GET",
  });

  return unwrapOrThrow(payload);
}

export async function updateProfile(input: ProfileUpdateInput): Promise<ProfileResponse> {
  const payload = await apiFetch<ApiResponse<ProfileResponse>>(
    "/api/auth/profile",
    {
      method: "PUT",
      body: JSON.stringify(input),
    },
  );

  return unwrapOrThrow(payload);
}

export async function setupTotp(): Promise<TotpSetupResponse> {
  const payload = await apiFetch<ApiResponse<TotpSetupResponse>>(
    "/api/auth/mfa/setup-totp",
    {
      method: "POST",
    },
  );

  return unwrapOrThrow(payload);
}

export async function confirmTotp(code: string): Promise<void> {
  const payload = await apiFetch<ApiResponse<null>>(
    "/api/auth/mfa/confirm-totp",
    {
      method: "POST",
      body: JSON.stringify({ code }),
    },
  );

  if (payload.success === false) {
    throw new Error(payload.message || "Failed to confirm TOTP");
  }
}

export async function enableEmailMfa(): Promise<void> {
  const payload = await apiFetch<ApiResponse<null>>(
    "/api/auth/mfa/enable-email",
    {
      method: "POST",
    },
  );

  if (payload.success === false) {
    throw new Error(payload.message || "Failed to enable email MFA");
  }
}

export async function disableMfa(): Promise<void> {
  const payload = await apiFetch<ApiResponse<null>>(
    "/api/auth/mfa/disable",
    {
      method: "POST",
    },
  );

  if (payload.success === false) {
    throw new Error(payload.message || "Failed to disable MFA");
  }
}

export async function listSessions(): Promise<SessionSummaryResponse[]> {
  const payload = await apiFetch<ApiResponse<SessionSummaryResponse[]>>("/api/auth/sessions", {
    method: "GET",
  });

  return unwrapOrThrow(payload);
}

export async function revokeSession(sessionId: string): Promise<void> {
  const payload = await apiFetch<ApiResponse<null>>(`/api/auth/sessions/${sessionId}`, {
    method: "DELETE",
  });

  if (payload.success === false) {
    throw new Error(payload.message || "Failed to revoke session");
  }
}

export async function revokeOtherSessions(): Promise<void> {
  const payload = await apiFetch<ApiResponse<null>>("/api/auth/sessions", {
    method: "DELETE",
  });

  if (payload.success === false) {
    throw new Error(payload.message || "Failed to revoke other sessions");
  }
}

export async function listAdminRoles(): Promise<AdminRoleResponse[]> {
  const payload = await apiFetch<ApiResponse<AdminRoleResponse[]>>("/api/auth/admin/rbac/roles", {
    method: "GET",
  });

  return unwrapOrThrow(payload);
}

export async function createAdminRole(input: AdminCreateRoleInput): Promise<AdminRoleResponse> {
  const payload = await apiFetch<ApiResponse<AdminRoleResponse>>(
    "/api/auth/admin/rbac/roles",
    {
      method: "POST",
      body: JSON.stringify(input),
    },
  );

  return unwrapOrThrow(payload);
}

export async function updateAdminRolePermissions(
  roleId: number,
  input: AdminSetRolePermissionsInput,
): Promise<AdminRoleResponse> {
  const payload = await apiFetch<ApiResponse<AdminRoleResponse>>(
    `/api/auth/admin/rbac/roles/${roleId}/permissions`,
    {
      method: "PUT",
      body: JSON.stringify(input),
    },
  );

  return unwrapOrThrow(payload);
}

export async function deleteAdminRole(roleId: number): Promise<void> {
  const payload = await apiFetch<ApiResponse<null>>(`/api/auth/admin/rbac/roles/${roleId}`, {
    method: "DELETE",
  });

  if (payload.success === false) {
    throw new Error(payload.message || "Failed to delete role");
  }
}

export async function listAdminPermissions(): Promise<string[]> {
  const payload = await apiFetch<ApiResponse<string[]>>("/api/auth/admin/rbac/permissions", {
    method: "GET",
  });

  return unwrapOrThrow(payload);
}

export async function listAdminManagedUsers(page = 0, size = 20): Promise<AdminManagedUsersPageResponse> {
  const payload = await apiFetch<ApiResponse<AdminManagedUsersPageResponse>>(
    `/api/auth/admin/rbac/users?page=${page}&size=${size}`,
    {
      method: "GET",
    },
  );

  return unwrapOrThrow(payload);
}

export async function assignRoleToUser(userId: string, input: AdminAssignUserRoleInput): Promise<void> {
  const payload = await apiFetch<ApiResponse<null>>(
    `/api/auth/admin/rbac/users/${userId}/role`,
    {
      method: "PUT",
      body: JSON.stringify(input),
    },
  );

  if (payload.success === false) {
    throw new Error(payload.message || "Failed to assign role");
  }
}

export async function unassignRoleFromUser(userId: string): Promise<void> {
  const payload = await apiFetch<ApiResponse<null>>(`/api/auth/admin/rbac/users/${userId}/role`, {
    method: "DELETE",
  });

  if (payload.success === false) {
    throw new Error(payload.message || "Failed to unassign role");
  }
}

export async function deactivateAccount(targetUserId: string): Promise<void> {
  const payload = await apiFetch<ApiResponse<null>>(`/api/auth/profile/deactivate/${targetUserId}`, {
    method: "POST",
  });

  if (payload.success === false) {
    throw new Error(payload.message || "Failed to deactivate account");
  }
}

export async function logout(): Promise<void> {
  try {
    await apiFetch<ApiResponse<null>>(
      "/api/auth/logout",
      {
        method: "POST",
      },
      { retryOnUnauthorized: false },
    );
  } catch (error) {
    if (!(error instanceof ApiError)) {
      throw error;
    }
  }
}
