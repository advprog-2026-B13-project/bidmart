import { apiFetch, ApiError } from "./api-client";
import { clearSessionTokens, setSessionTokens } from "./token-storage";
import type {
  ApiResponse,
  LoginInput,
  LoginResponse,
  LoginResult,
  MfaVerifyInput,
  ProfileResponse,
  RegisterInput,
  RegisterResponse,
  TokenPair,
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

  if (!data.accessToken || !data.refreshToken) {
    throw new Error("Login succeeded, but token pair is missing");
  }

  const tokens = {
    accessToken: data.accessToken,
    refreshToken: data.refreshToken,
  };

  setSessionTokens(tokens);

  return {
    requiresMfa: false,
    tokens,
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

export async function verifyMfa(input: MfaVerifyInput): Promise<TokenPair> {
  const payload = await apiFetch<ApiResponse<TokenPair>>(
    "/api/auth/mfa/verify",
    {
      method: "POST",
      body: JSON.stringify(input),
    },
    { auth: false },
  );

  const tokens = unwrapOrThrow(payload);

  if (!tokens.accessToken || !tokens.refreshToken) {
    throw new Error("MFA verification succeeded, but token pair is missing");
  }

  setSessionTokens({
    accessToken: tokens.accessToken,
    refreshToken: tokens.refreshToken,
  });

  return tokens;
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
  } finally {
    clearSessionTokens();
  }
}
