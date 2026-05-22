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
import { useAuth } from "@/components/auth-provider";
import { getProfile, updateProfile } from "@/lib/auth/auth-api";
import type { ProfileResponse, ProfileUpdateInput } from "@/lib/auth/types";

type UserProfileContextValue = {
  profile: ProfileResponse | null;
  isLoading: boolean;
  error: string | null;
  refresh: () => Promise<void>;
  update: (input: ProfileUpdateInput) => Promise<void>;
  isAdmin: boolean;
};

const UserProfileContext = createContext<UserProfileContextValue | null>(null);

export function UserProfileProvider({ children }: { children: ReactNode }) {
  const { user, isAuthenticated, isHydrating } = useAuth();
  const [profile, setProfile] = useState<ProfileResponse | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await getProfile();
      setProfile(data);
    } catch (err) {
      const msg = err instanceof Error ? err.message : "Failed to load profile";
      setError(msg);
    } finally {
      setIsLoading(false);
    }
  }, []);

  const update = useCallback(async (input: ProfileUpdateInput) => {
    setIsLoading(true);
    setError(null);
    try {
      const updated = await updateProfile(input);
      setProfile(updated);
    } catch (err) {
      const msg = err instanceof Error ? err.message : "Failed to update profile";
      setError(msg);
      throw err;
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!isHydrating && isAuthenticated) {
      refresh();
    } else if (!isAuthenticated) {
      setProfile(null);
    }
  }, [isAuthenticated, isHydrating, refresh]);

  const isAdmin = useMemo(() => {
    const role = profile?.role || user?.role || "";
    return role.toUpperCase().includes("ADMIN");
  }, [profile?.role, user?.role]);

  const value = useMemo<UserProfileContextValue>(
    () => ({ profile, isLoading, error, refresh, update, isAdmin }),
    [profile, isLoading, error, refresh, update, isAdmin],
  );

  return (
    <UserProfileContext.Provider value={value}>
      {children}
    </UserProfileContext.Provider>
  );
}

export function useUserProfile() {
  const ctx = useContext(UserProfileContext);
  if (!ctx) {
    throw new Error("useUserProfile must be used inside UserProfileProvider");
  }
  return ctx;
}
