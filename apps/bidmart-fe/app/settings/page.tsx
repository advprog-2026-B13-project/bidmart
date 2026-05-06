"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useMemo, useState, type FormEvent } from "react";
import { useAuth } from "@/components/auth-provider";
import {
  confirmTotp,
  disableMfa,
  enableEmailMfa,
  getProfile,
  setupTotp,
  updateProfile,
} from "@/lib/auth/auth-api";
import type { ProfileResponse, TotpSetupResponse } from "@/lib/auth/types";

function getErrorMessage(error: unknown) {
  if (error instanceof Error && error.message) {
    return error.message;
  }

  return "Request failed. Please try again.";
}

function formatDate(value?: string) {
  if (!value) {
    return "Unknown";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return date.toLocaleString();
}

function normalizeMfaMethod(value?: string) {
  const normalized = value?.trim().toUpperCase();

  if (!normalized || normalized === "DISABLED" || normalized === "NONE") {
    return "DISABLED";
  }

  if (normalized === "TOTP") {
    return "TOTP";
  }

  if (normalized === "EMAIL") {
    return "EMAIL";
  }

  return normalized;
}

function formatMfaLabel(value?: string) {
  const method = normalizeMfaMethod(value);

  if (method === "DISABLED") {
    return "Disabled";
  }

  if (method === "TOTP") {
    return "TOTP (Authenticator App)";
  }

  if (method === "EMAIL") {
    return "Email OTP";
  }

  return method;
}

function buildQrImageUrl(otpAuthUrl: string) {
  return `https://api.qrserver.com/v1/create-qr-code/?size=240x240&data=${encodeURIComponent(otpAuthUrl)}`;
}

export default function SettingsPage() {
  const router = useRouter();
  const { isAuthenticated, isHydrating, refreshProfile, user } = useAuth();

  const [profile, setProfile] = useState<ProfileResponse | null>(null);
  const [displayName, setDisplayName] = useState("");
  const [photoUrl, setPhotoUrl] = useState("");
  const [shippingAddress, setShippingAddress] = useState("");

  const [totpSetupData, setTotpSetupData] = useState<TotpSetupResponse | null>(null);
  const [totpCode, setTotpCode] = useState("");

  const [isLoadingProfile, setIsLoadingProfile] = useState(true);
  const [isSavingProfile, setIsSavingProfile] = useState(false);
  const [isMfaSubmitting, setIsMfaSubmitting] = useState(false);

  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const currentMfaMethod = useMemo(
    () => normalizeMfaMethod(profile?.default2FAMethod || user?.default2FAMethod),
    [profile?.default2FAMethod, user?.default2FAMethod],
  );

  const loadProfile = useCallback(async () => {
    const latest = await getProfile();
    setProfile(latest);
    setDisplayName(latest.displayName || "");
    setPhotoUrl(latest.photoUrl || "");
    setShippingAddress(latest.shippingAddress || "");
  }, []);

  useEffect(() => {
    if (!isHydrating && !isAuthenticated) {
      router.replace("/login");
    }
  }, [isAuthenticated, isHydrating, router]);

  useEffect(() => {
    let isMounted = true;

    async function init() {
      if (isHydrating || !isAuthenticated) {
        return;
      }

      setIsLoadingProfile(true);
      setError("");

      try {
        const latest = await getProfile();

        if (!isMounted) {
          return;
        }

        setProfile(latest);
        setDisplayName(latest.displayName || "");
        setPhotoUrl(latest.photoUrl || "");
        setShippingAddress(latest.shippingAddress || "");
      } catch (loadError) {
        if (isMounted) {
          setError(getErrorMessage(loadError));
        }
      } finally {
        if (isMounted) {
          setIsLoadingProfile(false);
        }
      }
    }

    init();

    return () => {
      isMounted = false;
    };
  }, [isAuthenticated, isHydrating]);

  const handleSaveProfile = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError("");
    setSuccess("");
    setIsSavingProfile(true);

    try {
      const updated = await updateProfile({
        displayName: displayName.trim() || null,
        photoUrl: photoUrl.trim() || null,
        shippingAddress: shippingAddress.trim() || null,
      });

      setProfile(updated);
      setDisplayName(updated.displayName || "");
      setPhotoUrl(updated.photoUrl || "");
      setShippingAddress(updated.shippingAddress || "");

      await refreshProfile();
      setSuccess("Profile updated successfully.");
    } catch (saveError) {
      setError(getErrorMessage(saveError));
    } finally {
      setIsSavingProfile(false);
    }
  };

  const handleStartTotpSetup = async () => {
    setError("");
    setSuccess("");
    setIsMfaSubmitting(true);

    try {
      const setupResponse = await setupTotp();
      setTotpSetupData(setupResponse);
      setTotpCode("");
      setSuccess("TOTP setup started. Scan the QR code, then enter the 6-digit code to confirm.");
    } catch (setupError) {
      setError(getErrorMessage(setupError));
    } finally {
      setIsMfaSubmitting(false);
    }
  };

  const handleConfirmTotp = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (!totpSetupData) {
      setError("Start TOTP setup first before confirming.");
      return;
    }

    if (totpCode.length !== 6) {
      setError("TOTP code must be 6 digits.");
      return;
    }

    setError("");
    setSuccess("");
    setIsMfaSubmitting(true);

    try {
      await confirmTotp(totpCode);
      setTotpSetupData(null);
      setTotpCode("");
      await loadProfile();
      await refreshProfile();
      setSuccess("TOTP MFA enabled successfully.");
    } catch (confirmError) {
      setError(getErrorMessage(confirmError));
    } finally {
      setIsMfaSubmitting(false);
    }
  };

  const handleEnableEmailMfa = async () => {
    setError("");
    setSuccess("");
    setIsMfaSubmitting(true);

    try {
      await enableEmailMfa();
      setTotpSetupData(null);
      setTotpCode("");
      await loadProfile();
      await refreshProfile();
      setSuccess("Email MFA enabled successfully.");
    } catch (enableError) {
      setError(getErrorMessage(enableError));
    } finally {
      setIsMfaSubmitting(false);
    }
  };

  const handleDisableMfa = async () => {
    setError("");
    setSuccess("");
    setIsMfaSubmitting(true);

    try {
      await disableMfa();
      setTotpSetupData(null);
      setTotpCode("");
      await loadProfile();
      await refreshProfile();
      setSuccess("MFA has been disabled.");
    } catch (disableError) {
      setError(getErrorMessage(disableError));
    } finally {
      setIsMfaSubmitting(false);
    }
  };

  if (isHydrating || !isAuthenticated) {
    return (
      <div className="min-h-screen bg-gray-100 flex items-center justify-center px-4">
        <div className="border-3 border-black bg-white px-6 py-5 shadow-[8px_8px_0_#0A0A0A]">
          <p className="text-sm font-bold uppercase tracking-wide text-gray-700">Loading account settings...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-100">
      <div className="max-w-5xl mx-auto px-4 py-10 md:py-14">
        <div className="flex flex-col md:flex-row md:items-end md:justify-between gap-4 mb-8">
          <div>
            <Link href="/" className="inline-flex items-center gap-2 mb-4">
              <div className="w-9 h-9 bg-acid flex items-center justify-center border-2 border-black shadow-[3px_3px_0_#0A0A0A]">
                <span className="text-black font-black text-base">B</span>
              </div>
              <span className="text-xl font-black tracking-tight">BIDMART</span>
            </Link>
            <h1 className="text-3xl md:text-5xl font-black uppercase tracking-tighter text-black">
              Profile & MFA Settings
            </h1>
            <p className="text-gray-600 mt-2 max-w-2xl">
              Manage your account profile and security preferences. TOTP setup requires two steps: setup, then confirmation.
            </p>
          </div>

          <div className="flex flex-col items-start md:items-end gap-2">
            <div className="border-2 border-black bg-white px-4 py-2 text-sm font-bold uppercase tracking-wide">
              {profile?.displayName || profile?.email || user?.email || "Account"}
            </div>
            <button
              type="button"
              onClick={() => router.push("/sessions")}
              className="btn btn-ghost text-xs font-bold uppercase tracking-wide"
            >
              Manage Sessions
            </button>
          </div>
        </div>

        {error && (
          <div className="mb-4 p-4 bg-hot/10 border-2 border-hot text-hot text-sm font-bold">
            {error}
          </div>
        )}

        {success && (
          <div className="mb-4 p-4 bg-acid/20 border-2 border-black text-black text-sm font-bold">
            {success}
          </div>
        )}

        {isLoadingProfile ? (
          <div className="border-3 border-black bg-white p-8 shadow-[8px_8px_0_#0A0A0A]">
            <p className="text-sm font-bold uppercase tracking-wide text-gray-600">Loading profile...</p>
          </div>
        ) : (
          <div className="grid gap-6">
            <section className="border-2 border-black bg-white p-6 shadow-[6px_6px_0_#0A0A0A]">
              <p className="text-[10px] font-black uppercase tracking-widest text-gray-500">Profile</p>
              <h2 className="text-2xl md:text-3xl font-black uppercase tracking-tight mt-2 mb-5">Public Information</h2>

              <form onSubmit={handleSaveProfile} className="grid gap-4">
                <div>
                  <label htmlFor="displayName" className="block text-[10px] font-black uppercase tracking-widest text-gray-500 mb-2">
                    Display Name
                  </label>
                  <input
                    id="displayName"
                    type="text"
                    value={displayName}
                    onChange={(event) => setDisplayName(event.target.value)}
                    className="input"
                    placeholder="Your display name"
                  />
                </div>

                <div>
                  <label htmlFor="photoUrl" className="block text-[10px] font-black uppercase tracking-widest text-gray-500 mb-2">
                    Photo URL
                  </label>
                  <input
                    id="photoUrl"
                    type="url"
                    value={photoUrl}
                    onChange={(event) => setPhotoUrl(event.target.value)}
                    className="input"
                    placeholder="https://example.com/avatar.jpg"
                  />
                </div>

                <div>
                  <label htmlFor="shippingAddress" className="block text-[10px] font-black uppercase tracking-widest text-gray-500 mb-2">
                    Shipping Address
                  </label>
                  <textarea
                    id="shippingAddress"
                    value={shippingAddress}
                    onChange={(event) => setShippingAddress(event.target.value)}
                    className="input min-h-24"
                    placeholder="Street, city, postal code"
                  />
                </div>

                <div className="flex justify-end">
                  <button
                    type="submit"
                    disabled={isSavingProfile}
                    className="btn btn-black text-xs font-bold uppercase tracking-wide"
                  >
                    {isSavingProfile ? "Saving..." : "Save Profile"}
                  </button>
                </div>
              </form>
            </section>

            <section className="border-2 border-black bg-white p-6 shadow-[6px_6px_0_#0A0A0A]">
              <p className="text-[10px] font-black uppercase tracking-widest text-gray-500">Security</p>
              <h2 className="text-2xl md:text-3xl font-black uppercase tracking-tight mt-2 mb-2">Multi-Factor Authentication</h2>
              <p className="text-sm text-gray-600 mb-5">
                Current method: <span className="font-black text-black">{formatMfaLabel(profile?.default2FAMethod || user?.default2FAMethod)}</span>
              </p>

              <div className="flex flex-wrap gap-2 mb-5">
                <button
                  type="button"
                  onClick={handleStartTotpSetup}
                  disabled={isMfaSubmitting || (currentMfaMethod === "TOTP" && !totpSetupData)}
                  className="btn btn-electric text-xs font-bold uppercase tracking-wide disabled:opacity-50"
                >
                  {isMfaSubmitting ? "Processing..." : "Setup TOTP"}
                </button>
                <button
                  type="button"
                  onClick={handleEnableEmailMfa}
                  disabled={isMfaSubmitting || currentMfaMethod === "EMAIL"}
                  className="btn btn-ghost text-xs font-bold uppercase tracking-wide disabled:opacity-50"
                >
                  Enable Email MFA
                </button>
                <button
                  type="button"
                  onClick={handleDisableMfa}
                  disabled={isMfaSubmitting || currentMfaMethod === "DISABLED"}
                  className="btn btn-ghost text-xs font-bold uppercase tracking-wide disabled:opacity-50"
                >
                  Disable MFA
                </button>
              </div>

              {totpSetupData && (
                <div className="border-2 border-black bg-gray-50 p-4">
                  <h3 className="text-lg font-black uppercase tracking-tight mb-2">Step 1: Scan QR / Save Secret</h3>
                  <p className="text-sm text-gray-600 mb-4">
                    Use your authenticator app to scan the QR code below. After that, continue with step 2 to confirm and activate TOTP.
                  </p>

                  <div className="grid gap-4 md:grid-cols-[240px_1fr] items-start">
                    <img
                      src={buildQrImageUrl(totpSetupData.otpAuthUrl)}
                      alt="TOTP setup QR code"
                      width={240}
                      height={240}
                      className="border-2 border-black bg-white p-2"
                    />

                    <div className="space-y-3">
                      <div>
                        <p className="text-[10px] font-black uppercase tracking-widest text-gray-500 mb-1">Secret</p>
                        <input
                          type="text"
                          readOnly
                          value={totpSetupData.secret}
                          className="input font-mono text-xs"
                        />
                      </div>
                      <div>
                        <p className="text-[10px] font-black uppercase tracking-widest text-gray-500 mb-1">OTP Auth URL</p>
                        <textarea
                          readOnly
                          value={totpSetupData.otpAuthUrl}
                          className="input min-h-21 font-mono text-xs"
                        />
                      </div>
                    </div>
                  </div>

                  <form onSubmit={handleConfirmTotp} className="mt-5 border-t-2 border-black pt-4">
                    <h3 className="text-lg font-black uppercase tracking-tight mb-2">Step 2: Confirm TOTP</h3>
                    <p className="text-sm text-gray-600 mb-3">
                      Enter the current 6-digit code from your authenticator app to activate TOTP MFA.
                    </p>

                    <div className="flex flex-col sm:flex-row gap-2">
                      <input
                        type="text"
                        value={totpCode}
                        onChange={(event) => setTotpCode(event.target.value.replace(/\D/g, "").slice(0, 6))}
                        className="input sm:max-w-45 text-center text-xl font-black tracking-[0.3em]"
                        placeholder="123456"
                        maxLength={6}
                        required
                      />
                      <button
                        type="submit"
                        disabled={isMfaSubmitting}
                        className="btn btn-black text-xs font-bold uppercase tracking-wide disabled:opacity-50"
                      >
                        {isMfaSubmitting ? "Confirming..." : "Confirm TOTP"}
                      </button>
                    </div>
                  </form>
                </div>
              )}
            </section>
          </div>
        )}
      </div>
    </div>
  );
}
