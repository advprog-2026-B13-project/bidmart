import type { SessionTokens } from "./types";

const ACCESS_TOKEN_KEY = "bidmart.accessToken";
const REFRESH_TOKEN_KEY = "bidmart.refreshToken";

function isBrowser() {
  return typeof window !== "undefined";
}

export function getAccessToken(): string | null {
  if (!isBrowser()) {
    return null;
  }
  return localStorage.getItem(ACCESS_TOKEN_KEY);
}

export function getRefreshToken(): string | null {
  if (!isBrowser()) {
    return null;
  }
  return localStorage.getItem(REFRESH_TOKEN_KEY);
}

export function setSessionTokens(tokens: SessionTokens) {
  if (!isBrowser()) {
    return;
  }
  localStorage.setItem(ACCESS_TOKEN_KEY, tokens.accessToken);
  localStorage.setItem(REFRESH_TOKEN_KEY, tokens.refreshToken);
  window.dispatchEvent(new CustomEvent("auth:tokens-updated"));
}

export function clearSessionTokens() {
  if (!isBrowser()) {
    return;
  }
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
  window.dispatchEvent(new CustomEvent("auth:tokens-cleared"));
}

export function hasSessionToken() {
  return Boolean(getAccessToken());
}
