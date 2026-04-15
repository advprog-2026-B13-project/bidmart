import { clearSessionTokens, getAccessToken, getRefreshToken, setSessionTokens } from "./token-storage";
import type { ApiResponse, TokenPair } from "./types";

const AUTH_BASE_URL =
  process.env.NEXT_PUBLIC_AUTH_API_URL ||
  process.env.NEXT_PUBLIC_API_BASE_URL ||
  "http://localhost:8080";

export class ApiError extends Error {
  status: number;

  constructor(message: string, status: number) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

let refreshPromise: Promise<boolean> | null = null;

function joinUrl(path: string) {
  if (path.startsWith("http://") || path.startsWith("https://")) {
    return path;
  }

  const normalizedBase = AUTH_BASE_URL.replace(/\/+$/, "");
  const normalizedPath = path.startsWith("/") ? path : `/${path}`;
  return `${normalizedBase}${normalizedPath}`;
}

async function parseJsonSafe<T>(response: Response): Promise<T | null> {
  try {
    return (await response.json()) as T;
  } catch {
    return null;
  }
}

async function refreshAccessToken(): Promise<boolean> {
  const refreshToken = getRefreshToken();

  if (!refreshToken) {
    clearSessionTokens();
    return false;
  }

  const response = await fetch(joinUrl("/api/auth/refresh"), {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ refreshToken }),
  });

  if (!response.ok) {
    clearSessionTokens();
    return false;
  }

  const payload = await parseJsonSafe<ApiResponse<TokenPair>>(response);
  const tokens = payload?.data;

  if (!tokens?.accessToken || !tokens?.refreshToken) {
    clearSessionTokens();
    return false;
  }

  setSessionTokens({ accessToken: tokens.accessToken, refreshToken: tokens.refreshToken });
  return true;
}

async function ensureFreshToken() {
  if (!refreshPromise) {
    refreshPromise = refreshAccessToken().finally(() => {
      refreshPromise = null;
    });
  }

  return refreshPromise;
}

type ApiFetchOptions = {
  auth?: boolean;
  retryOnUnauthorized?: boolean;
};

export async function apiFetch<T>(
  path: string,
  init: RequestInit = {},
  options: ApiFetchOptions = {},
): Promise<T> {
  const shouldAttachAuth = options.auth ?? true;
  const shouldRetry = options.retryOnUnauthorized ?? true;

  const headers = new Headers(init.headers || {});
  headers.set("Content-Type", "application/json");

  if (shouldAttachAuth) {
    const accessToken = getAccessToken();
    if (accessToken) {
      headers.set("Authorization", `Bearer ${accessToken}`);
    }
  }

  const response = await fetch(joinUrl(path), {
    ...init,
    headers,
  });

  if (response.status === 401 && shouldAttachAuth && shouldRetry) {
    const refreshed = await ensureFreshToken();

    if (refreshed) {
      return apiFetch<T>(path, init, {
        auth: shouldAttachAuth,
        retryOnUnauthorized: false,
      });
    }
  }

  const payload = await parseJsonSafe<T & { message?: string }>(response);

  if (!response.ok) {
    const message = payload?.message || "Request failed";
    throw new ApiError(message, response.status);
  }

  if (!payload) {
    throw new ApiError("Empty response payload", response.status);
  }

  return payload;
}
