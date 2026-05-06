"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import {
  getProfile as getProfileRequest,
  login as loginRequest,
  logout as logoutRequest,
  register as registerRequest,
  verifyMfa as verifyMfaRequest,
} from "@/lib/auth/auth-api";
import { usePathname, useRouter } from "next/navigation";
import { ApiError } from "@/lib/auth/api-client";
import type {
  LoginInput,
  LoginResult,
  MfaVerifyInput,
  ProfileResponse,
  RegisterInput,
  RegisterResponse,
} from "@/lib/auth/types";

type AuthContextValue = {
  user: ProfileResponse | null;
  isAuthenticated: boolean;
  isHydrating: boolean;
  login: (input: LoginInput) => Promise<LoginResult>;
  register: (input: RegisterInput) => Promise<RegisterResponse>;
  verifyMfa: (input: MfaVerifyInput) => Promise<void>;
  logout: () => Promise<void>;
  refreshProfile: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const router = useRouter();
  const pathname = usePathname();
  const [user, setUser] = useState<ProfileResponse | null>(null);
  const [isHydrating, setIsHydrating] = useState(true);

  const refreshProfile = useCallback(async () => {
    const profile = await getProfileRequest();
    setUser(profile);
  }, []);

  const login = useCallback(async (input: LoginInput) => {
    const result = await loginRequest(input);

    if (!result.requiresMfa) {
      await refreshProfile();
    }

    return result;
  }, [refreshProfile]);

  const verifyMfa = useCallback(async (input: MfaVerifyInput) => {
    await verifyMfaRequest(input);
    await refreshProfile();
  }, [refreshProfile]);

  const register = useCallback(async (input: RegisterInput) => {
    return registerRequest(input);
  }, []);

  const logout = useCallback(async () => {
    await logoutRequest();
    setUser(null);
  }, []);

  useEffect(() => {
    let isMounted = true;

    async function initAuth() {
      try {
        const profile = await getProfileRequest();
        if (isMounted) {
          setUser(profile);
        }
      } catch (error) {
        if (isMounted) {
          setUser(null);
        }

        if (!(error instanceof ApiError) || error.status !== 401) {
          console.error("Failed to restore auth session", error);
        }
      } finally {
        if (isMounted) {
          setIsHydrating(false);
        }
      }
    }

    initAuth();

    const onUnauthorized = () => {
      if (isMounted) {
        setUser(null);
      }

      const publicPaths = ["/", "/login", "/register", "/verify-email"];
      const isPublicPath = publicPaths.some((publicPath) => pathname?.startsWith(publicPath));

      if (!isPublicPath) {
        router.replace("/login");
      }
    };

    window.addEventListener("auth:unauthorized", onUnauthorized);

    return () => {
      isMounted = false;
      window.removeEventListener("auth:unauthorized", onUnauthorized);
    };
  }, [pathname, router]);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      isAuthenticated: Boolean(user),
      isHydrating,
      login,
      register,
      verifyMfa,
      logout,
      refreshProfile,
    }),
    [user, isHydrating, login, register, verifyMfa, logout, refreshProfile],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error("useAuth must be used inside AuthProvider");
  }

  return context;
}
