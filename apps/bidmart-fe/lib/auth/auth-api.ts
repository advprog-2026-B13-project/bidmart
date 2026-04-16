import { apiFetch, ApiError } from "./api-client";
import type {
  ApiResponse,
  LoginInput,
  LoginResponse,
  LoginResult,
  MfaVerifyInput,
  ProfileResponse,
  ResendVerificationOtpInput,
  RegisterInput,
  RegisterResponse,
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
