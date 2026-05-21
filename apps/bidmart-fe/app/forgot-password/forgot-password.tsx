"use client";

import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import { requestPasswordReset } from "@/lib/auth/auth-api";

function getErrorMessage(error: unknown) {
  if (error instanceof Error && error.message) {
    return error.message;
  }

  return "Password reset request failed. Please try again.";
}

export default function ForgotPassword() {
  const searchParams = useSearchParams();
  const initialEmail = useMemo(
    () => searchParams.get("email")?.trim() || "",
    [searchParams],
  );

  const [email, setEmail] = useState(initialEmail);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    setEmail(initialEmail);
  }, [initialEmail]);

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError("");
    setSuccess("");
    setIsSubmitting(true);

    try {
      await requestPasswordReset({ email: email.trim() });
      setSuccess("If an account exists for this email, a reset link has been sent. Check your inbox and spam folder.");
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
          Reset Password
        </h1>
        <p className="text-gray-600 text-sm md:text-base mb-6">
          Enter your email address and we will send a reset link.
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

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label htmlFor="reset-email" className="block text-[10px] md:text-xs font-black text-gray-500 uppercase tracking-widest mb-2">
              Email Address
            </label>
            <input
              id="reset-email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="input"
              placeholder="you@example.com"
              required
            />
          </div>

          <button
            type="submit"
            disabled={isSubmitting}
            className="btn btn-black w-full text-sm md:text-base font-bold uppercase tracking-wide"
          >
            {isSubmitting ? "Sending..." : "Send Reset Link"}
          </button>
        </form>

        <div className="mt-4 grid grid-cols-1 md:grid-cols-2 gap-2">
          <Link href="/login" className="btn btn-ghost text-xs md:text-sm font-bold uppercase justify-center">
            Back to Login
          </Link>
          <Link href="/register" className="btn text-xs md:text-sm font-bold uppercase justify-center">
            Create Account
          </Link>
        </div>
      </div>
    </div>
  );
}
