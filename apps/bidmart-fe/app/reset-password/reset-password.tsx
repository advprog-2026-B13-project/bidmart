"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { confirmPasswordReset, verifyPasswordResetToken } from "@/lib/auth/auth-api";

const MIN_PASSWORD_LENGTH = 8;

function getErrorMessage(error: unknown) {
  if (error instanceof Error && error.message) {
    return error.message;
  }

  return "Password reset failed. Please try again.";
}

export default function ResetPassword() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const autoVerifyRef = useRef<string | null>(null);

  const tokenFromQuery = useMemo(
    () => searchParams.get("token")?.trim() || searchParams.get("resetToken")?.trim() || "",
    [searchParams],
  );

  const [token, setToken] = useState(tokenFromQuery);
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [isVerifying, setIsVerifying] = useState(false);
  const [isTokenValid, setIsTokenValid] = useState<boolean | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  useEffect(() => {
    setToken(tokenFromQuery);
  }, [tokenFromQuery]);

  const verifyToken = useCallback(async (value: string) => {
    setIsVerifying(true);
    setError("");
    setSuccess("");

    try {
      const result = await verifyPasswordResetToken({ token: value });
      setIsTokenValid(Boolean(result.valid));

      if (!result.valid) {
        setError("Reset link is invalid or has expired.");
      }
    } catch (verifyError) {
      setIsTokenValid(false);
      setError(getErrorMessage(verifyError));
    } finally {
      setIsVerifying(false);
    }
  }, []);

  useEffect(() => {
    if (!token) {
      setIsTokenValid(null);
      return;
    }

    if (autoVerifyRef.current === token) {
      return;
    }

    autoVerifyRef.current = token;
    void verifyToken(token);
  }, [token, verifyToken]);

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError("");
    setSuccess("");

    if (!token) {
      setError("Missing reset token. Please use the link from your email.");
      return;
    }

    if (newPassword.length < MIN_PASSWORD_LENGTH) {
      setError(`Password must be at least ${MIN_PASSWORD_LENGTH} characters.`);
      return;
    }

    if (newPassword !== confirmPassword) {
      setError("Passwords do not match.");
      return;
    }

    setIsSubmitting(true);

    try {
      await confirmPasswordReset({ token, newPassword });
      setSuccess("Password updated. Redirecting to sign in...");
      setTimeout(() => {
        router.push("/login?reset=true");
      }, 900);
    } catch (submitError) {
      setError(getErrorMessage(submitError));
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-100 px-4 py-12">
      <div className="max-w-xl mx-auto border-3 border-black bg-white shadow-[8px_8px_0_#0A0A0A] p-6 md:p-8">
        <Link href="/" className="inline-flex items-center gap-2 mb-8">
          <div className="w-9 h-9 bg-acid flex items-center justify-center border-2 border-black shadow-[3px_3px_0_#0A0A0A]">
            <span className="text-black font-black text-base">B</span>
          </div>
          <span className="text-xl font-black tracking-tight">BIDMART</span>
        </Link>

        <h1 className="text-3xl md:text-4xl font-black uppercase tracking-tighter mb-2">
          Set New Password
        </h1>
        <p className="text-gray-600 text-sm md:text-base mb-6">
          Verify your reset link, then choose a new password.
        </p>

        {error && (
          <div className="mb-4 p-3 bg-hot/10 border-2 border-hot text-hot text-sm font-bold">
            {error}
          </div>
        )}

        {success && (
          <div className="mb-4 p-3 bg-acid/20 border-2 border-black text-black text-sm font-bold">
            {success}
          </div>
        )}

        {!token && (
          <div className="space-y-4">
            <p className="text-sm text-gray-600">
              Missing reset token. Please open the link from your email or request a new one.
            </p>
            <Link href="/forgot-password" className="btn btn-black w-full text-sm font-bold uppercase tracking-wide">
              Request New Link
            </Link>
          </div>
        )}

        {token && isVerifying && (
          <div className="text-sm font-bold text-gray-600">
            Validating reset link...
          </div>
        )}

        {token && !isVerifying && isTokenValid && (
          <form onSubmit={handleSubmit} className="space-y-4">
            <div>
              <label htmlFor="new-password" className="block text-[10px] md:text-xs font-black text-gray-500 uppercase tracking-widest mb-2">
                New Password
              </label>
              <input
                id="new-password"
                type="password"
                value={newPassword}
                onChange={(e) => setNewPassword(e.target.value)}
                className="input"
                placeholder="Min. 8 characters"
                minLength={MIN_PASSWORD_LENGTH}
                required
              />
            </div>

            <div>
              <label htmlFor="confirm-password" className="block text-[10px] md:text-xs font-black text-gray-500 uppercase tracking-widest mb-2">
                Confirm Password
              </label>
              <input
                id="confirm-password"
                type="password"
                value={confirmPassword}
                onChange={(e) => setConfirmPassword(e.target.value)}
                className="input"
                placeholder="Re-enter your password"
                minLength={MIN_PASSWORD_LENGTH}
                required
              />
            </div>

            <button
              type="submit"
              disabled={isSubmitting}
              className="btn btn-black w-full text-sm md:text-base font-bold uppercase tracking-wide"
            >
              {isSubmitting ? "Updating..." : "Update Password"}
            </button>
          </form>
        )}

        {token && !isVerifying && isTokenValid === false && (
          <div className="space-y-4">
            <p className="text-sm text-gray-600">
              This reset link is no longer valid. Request a new reset email to continue.
            </p>
            <Link href="/forgot-password" className="btn btn-black w-full text-sm font-bold uppercase tracking-wide">
              Request New Link
            </Link>
          </div>
        )}

        <div className="mt-4">
          <Link href="/login" className="btn btn-ghost text-xs md:text-sm font-bold uppercase justify-center">
            Back to Login
          </Link>
        </div>
      </div>
    </div>
  );
}
