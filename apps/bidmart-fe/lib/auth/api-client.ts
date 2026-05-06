import type { ApiResponse } from "./types";

function resolveAuthBaseUrl() {
  const configuredBaseUrl =
    process.env.NEXT_PUBLIC_AUTH_API_URL ||
    process.env.NEXT_PUBLIC_API_BASE_URL;

  if (configuredBaseUrl) {
    return configuredBaseUrl.replace(/\/+$/, "");
  }

  if (typeof window === "undefined") {
    return "http://localhost:8080";
  }

  const host = window.location.hostname;
  const isLocalhost = host === "localhost" || host === "127.0.0.1";

  if (isLocalhost) {
    return "http://localhost:8080";
  }

  return "";
}

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

  const normalizedPath = path.startsWith("/") ? path : `/${path}`;
  const resolvedBaseUrl = resolveAuthBaseUrl();

  if (!resolvedBaseUrl) {
    return normalizedPath;
  }

  return `${resolvedBaseUrl}${normalizedPath}`;
}

async function parseJsonSafe<T>(response: Response): Promise<T | null> {
  try {
    return (await response.json()) as T;
  } catch {
    return null;
  }
}

export async function refreshSession(fallbackRefreshToken?: string): Promise<boolean> {
  const hasFallbackToken = Boolean(fallbackRefreshToken);
  const response = await fetch(joinUrl("/api/auth/refresh"), {
    method: "POST",
    credentials: "include",
    headers: {
      "Content-Type": "application/json",
    },
    body: hasFallbackToken ? JSON.stringify({ refreshToken: fallbackRefreshToken }) : undefined,
  });

  if (!response.ok) {
    return false;
  }

  const payload = await parseJsonSafe<ApiResponse<unknown>>(response);
  if (payload?.success === false) {
    return false;
  }

  return true;
}

async function refreshAccessToken(): Promise<boolean> {
  return refreshSession();
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
  onUnauthorized?: () => void;
};

export async function apiFetch<T>(
  path: string,
  init: RequestInit = {},
  options: ApiFetchOptions = {},
): Promise<T> {
  const shouldAttachAuth = options.auth ?? true;
  const shouldRetry = options.retryOnUnauthorized ?? true;

  const headers = new Headers(init.headers || {});
  const hasBody = init.body !== undefined && init.body !== null;
  if (hasBody && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }

  const response = await fetch(joinUrl(path), {
    ...init,
    credentials: "include",
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

    if (response.status === 401 && shouldAttachAuth) {
      if (options.onUnauthorized) {
        options.onUnauthorized();
      }

      if (typeof window !== "undefined") {
        window.dispatchEvent(new CustomEvent("auth:unauthorized"));
      }
    }

    throw new ApiError(message, response.status);
  }

  if (!payload) {
    throw new ApiError("Empty response payload", response.status);
  }

  return payload;
}
