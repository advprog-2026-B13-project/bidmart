"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { Loader2, Lock } from "lucide-react";
import { useAuth } from "@/components/auth-provider";
import { confirmSessionReplacement, requestEmailOtp } from "@/lib/auth/auth-api";
import type { SessionSummaryResponse } from "@/lib/auth/types";

function getErrorMessage(error: unknown) {
  if (error instanceof Error && error.message) {
    return error.message;
  }
  return "Request failed. Please try again.";
}

export default function Login() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const { login, verifyMfa, isAuthenticated, isHydrating } = useAuth();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [rememberMe, setRememberMe] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");
  const [successMessage, setSuccessMessage] = useState("");
  const [showMfa, setShowMfa] = useState(false);
  const [showSessionReplacement, setShowSessionReplacement] = useState(false);
  const [mfaCode, setMfaCode] = useState("");
  const [preAuthToken, setPreAuthToken] = useState("");
  const [mfaType, setMfaType] = useState("");
  const [sessionReplacementToken, setSessionReplacementToken] = useState("");
  const [activeSessions, setActiveSessions] = useState<SessionSummaryResponse[]>([]);
  const [isConfirmingReplacement, setIsConfirmingReplacement] = useState(false);

  useEffect(() => {
    if (!isHydrating && isAuthenticated) {
      router.replace("/");
    }
  }, [isAuthenticated, isHydrating, router]);

  useEffect(() => {
    if (searchParams.get("registered") === "true") {
      setSuccessMessage("Account created. Please sign in.");
      return;
    }

    if (searchParams.get("reset") === "true") {
      setSuccessMessage("Password updated. Please sign in.");
    }
  }, [searchParams]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setSuccessMessage("");
    setIsLoading(true);

    try {
      const result = await login({ email, password });

      if (result.requiresSessionReplacement) {
        setSessionReplacementToken(result.sessionReplacementToken);
        setActiveSessions(result.activeSessions);
        setShowSessionReplacement(true);
        return;
      }

      if (result.requiresMfa) {
        setPreAuthToken(result.preAuthToken);
        setMfaType(result.mfaType || "");
        setShowMfa(true);

        if (result.mfaType?.toUpperCase() === "EMAIL") {
          await requestEmailOtp(result.preAuthToken);
          setSuccessMessage("A verification code was sent to your email.");
        }
      } else {
        router.push("/");
      }
    } catch (submitError) {
      setError(getErrorMessage(submitError));
    } finally {
      setIsLoading(false);
    }
  };

  const handleSessionReplacementDecision = async (replaceOldestSession: boolean) => {
    if (!sessionReplacementToken) {
      setError("Session replacement token expired. Please login again.");
      setShowSessionReplacement(false);
      return;
    }

    setError("");
    setSuccessMessage("");
    setIsConfirmingReplacement(true);

    try {
      if (!replaceOldestSession) {
        setShowSessionReplacement(false);
        setError("Login cancelled. No session was replaced.");
        return;
      }

      const result = await confirmSessionReplacement({
        sessionReplacementToken,
        replaceOldestSession,
      });

      setShowSessionReplacement(false);
      setSessionReplacementToken("");
      setActiveSessions([]);

      if (result.requiresMfa) {
        setPreAuthToken(result.preAuthToken);
        setMfaType(result.mfaType || "");
        setShowMfa(true);

        if (result.mfaType?.toUpperCase() === "EMAIL") {
          await requestEmailOtp(result.preAuthToken);
          setSuccessMessage("A verification code was sent to your email.");
        }

        return;
      }

      router.push("/");
    } catch (decisionError) {
      setError(getErrorMessage(decisionError));
    } finally {
      setIsConfirmingReplacement(false);
    }
  };

  const handleMfaSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setSuccessMessage("");

    if (!preAuthToken) {
      setError("MFA session expired. Please login again.");
      setShowMfa(false);
      return;
    }

    setIsLoading(true);

    try {
      await verifyMfa({ preAuthToken, code: mfaCode });
      router.push("/");
    } catch (submitError) {
      setError(getErrorMessage(submitError));
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="w-full h-screen lg:h-auto lg:min-h-screen flex flex-col lg:flex-row overflow-hidden ">
      {/* Left - Visual Panel (desktop) */}
      <div className="hidden lg:flex flex-1 bg-black items-center justify-center p-8 lg:p-12 relative overflow-hidden" style={{ minHeight: "100vh" }}>
        {/* Grid background */}
        <div className="absolute inset-0 grid-bg opacity-10"></div>
        {/* Decorative elements */}
        <div className="absolute top-16 left-16 w-24 h-24 border-[3px] border-acid hidden xl:block"></div>
        <div className="absolute bottom-32 right-12 w-40 h-40 border-[3px] border-electric hidden xl:block"></div>
        <div className="absolute top-1/2 left-1/4 w-12 h-12 bg-electric/20 hidden xl:block"></div>
        {/* Content */}
        <div className="relative z-10 max-w-md text-center px-4">
          <div className="mb-8">
            <div className="w-16 h-16 lg:w-20 lg:h-20 bg-acid flex items-center justify-center border-[3px] border-black mx-auto mb-6 shadow-[6px_6px_0_#0046FF] lg:shadow-[8px_8px_0_#0046FF]">
              <span className="text-black font-black text-3xl lg:text-4xl">B</span>
            </div>
          </div>
          <h2 className="text-4xl lg:text-5xl font-black text-white mb-4 uppercase tracking-tighter">
            BOLD<br />BIDS WIN
          </h2>
          <p className="text-gray-400 leading-relaxed">
            Join 50,000+ bidders discovering unique items every day.
          </p>
          <div className="mt-12 lg:mt-16 grid grid-cols-3 gap-4">
            {[
              { value: "$12M+", label: "BID VOLUME" },
              { value: "50K+", label: "BIDDERS" },
              { value: "99.2%", label: "SATISFACTION" },
            ].map((stat, i) => (
              <div key={i} className="text-center">
                <p className="text-xl lg:text-2xl font-black text-acid">{stat.value}</p>
                <p className="text-[10px] font-bold text-gray-500 uppercase tracking-widest">{stat.label}</p>
              </div>
            ))}
          </div>
        </div>
      </div>

      {/* Right - Form Panel */}
      <div className="flex-1 flex items-center justify-center px-6 lg:px-8 xl:px-12 py-10 lg:py-12 bg-white min-h-screen lg:min-h-0 overflow-y-auto">
        <div className="w-full max-w-[420px]">
          {/* Logo */}
          <Link href="/" className="flex items-center gap-2 mb-8 lg:mb-10">
            <div className="w-9 h-9 lg:w-10 lg:h-10 bg-acid flex items-center justify-center border-2 border-black shadow-[3px_3px_0_#0A0A0A]">
              <span className="text-black font-black text-base lg:text-lg">B</span>
            </div>
            <span className="text-xl lg:text-2xl font-black tracking-tight">BIDMART</span>
          </Link>

          {!showMfa ? (
            <>
              <div className="mb-8 lg:mb-10">
                <h1 className="text-3xl lg:text-4xl font-black text-black mb-2 uppercase tracking-tighter">
                  SIGN IN
                </h1>
                <p className="text-gray-500 font-medium">
                  Enter your credentials to start bidding
                </p>
              </div>

              {error && (
                <div className="mb-5 p-4 bg-hot/10 border-2 border-hot text-hot text-sm font-bold">
                  {error}
                </div>
              )}

              {successMessage && (
                <div className="mb-5 p-4 bg-acid/20 border-2 border-black text-black text-sm font-bold">
                  {successMessage}
                </div>
              )}

              <form onSubmit={handleSubmit} className="space-y-4 lg:space-y-5">
                <div>
                  <label htmlFor="email" className="block text-[10px] lg:text-xs font-black text-gray-500 uppercase tracking-widest mb-2">
                    Email Address
                  </label>
                  <input
                    id="email"
                    type="email"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    className="input"
                    placeholder="you@example.com"
                    required
                  />
                </div>

                <div>
                  <div className="flex items-center justify-between mb-2">
                    <label htmlFor="password" className="block text-[10px] lg:text-xs font-black text-gray-500 uppercase tracking-widest">
                      Password
                    </label>
                    <Link href="/forgot-password" className="text-[10px] lg:text-xs font-bold text-electric hover:underline uppercase tracking-wide">
                      Forgot?
                    </Link>
                  </div>
                  <input
                    id="password"
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    className="input"
                    placeholder="Enter your password"
                    required
                  />
                </div>

                <button
                  type="submit"
                  disabled={isLoading}
                  className="btn btn-black w-full text-sm lg:text-base py-3 lg:py-4 font-bold uppercase tracking-wide"
                >
                  {isLoading ? (
                    <>
                      <Loader2 className="w-4 h-4 lg:w-5 lg:h-5 animate-spin" />
                      Signing in...
                    </>
                  ) : (
                    "Sign In"
                  )}
                </button>
              </form>

              <p className="mt-8 lg:mt-10 text-center text-sm font-medium text-gray-500">
                Don&apos;t have an account?{" "}
                <Link href="/register" className="text-electric font-black hover:underline uppercase tracking-wide">
                  Join Free
                </Link>
              </p>
            </>
          ) : (
            <>
              {/* MFA Verification */}
              <div className="mb-8 lg:mb-10 text-center">
                <div className="w-14 h-14 lg:w-16 lg:h-16 bg-electric/10 flex items-center justify-center border-2 border-electric mx-auto mb-6">
                  <Lock className="w-7 h-7 lg:w-8 lg:h-8 text-electric" />
                </div>
                <h1 className="text-2xl lg:text-3xl font-black text-black mb-2 uppercase tracking-tighter">
                  VERIFY
                </h1>
                <p className="text-gray-500 font-medium">
                  {mfaType.toUpperCase() === "TOTP"
                    ? "Enter the 6-digit authenticator code"
                    : "Enter the 6-digit code sent to your email"}
                </p>
              </div>

              {error && (
                <div className="mb-5 p-4 bg-hot/10 border-2 border-hot text-hot text-sm font-bold text-center">
                  {error}
                </div>
              )}

              {successMessage && (
                <div className="mb-5 p-4 bg-acid/20 border-2 border-black text-black text-sm font-bold text-center">
                  {successMessage}
                </div>
              )}

              <form onSubmit={handleMfaSubmit} className="space-y-5">
                <div>
                  <input
                    type="text"
                    value={mfaCode}
                    onChange={(e) => setMfaCode(e.target.value.replace(/\D/g, '').slice(0, 6))}
                    className="input text-center text-2xl lg:text-3xl tracking-[0.5em] font-black"
                    placeholder="• • • • • •"
                    maxLength={6}
                    required
                  />
                  <p className="text-xs text-center text-gray-400 mt-3 font-medium">
                    Enter your current 6-digit verification code.
                  </p>
                </div>

                <button
                  type="submit"
                  disabled={isLoading}
                  className="btn btn-black w-full text-sm lg:text-base py-3 lg:py-4 font-bold uppercase tracking-wide"
                >
                  {isLoading ? "Verifying..." : "Verify"}
                </button>
              </form>

              <button
                onClick={() => {
                  setShowMfa(false);
                  setMfaCode("");
                  setPreAuthToken("");
                  setMfaType("");
                  setError("");
                  setSuccessMessage("");
                }}
                className="mt-5 lg:mt-6 btn btn-ghost w-full font-bold uppercase text-[11px] lg:text-sm"
              >
                ← Back to login
              </button>
            </>
          )}
        </div>
      </div>

      {showSessionReplacement && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 p-4">
          <div className="w-full max-w-2xl border-3 border-black bg-white p-6 shadow-[10px_10px_0_#0A0A0A]">
            <h2 className="text-2xl lg:text-3xl font-black uppercase tracking-tighter mb-2">
              Session Limit Reached
            </h2>
            <p className="text-gray-600 text-sm lg:text-base mb-5">
              You have reached the active session limit. Replace the oldest session to continue signing in?
            </p>

            {activeSessions.length > 0 && (
              <div className="mb-5 border-2 border-black bg-gray-50 p-4">
                <p className="text-xs font-black uppercase tracking-widest text-gray-500 mb-3">
                  Current Active Sessions
                </p>
                <div className="space-y-2 max-h-56 overflow-y-auto pr-1">
                  {activeSessions.map((session) => (
                    <div
                      key={session.sessionId}
                      className="flex items-center justify-between gap-3 border border-black bg-white px-3 py-2 text-sm"
                    >
                      <div>
                        <p className="font-black text-black">
                          {session.deviceInfo || "Unknown device"}
                        </p>
                        <p className="text-xs text-gray-500">
                          {session.locationLabel || session.ipAddress || "Unknown location"}
                        </p>
                      </div>
                      <div className="text-right text-xs text-gray-500">
                        {session.isCurrent ? (
                          <span className="font-black text-electric uppercase">Current</span>
                        ) : (
                          <span>Expires {session.expiresAt || "soon"}</span>
                        )}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {error && (
              <div className="mb-4 p-3 bg-hot/10 border-2 border-hot text-hot text-sm font-bold">
                {error}
              </div>
            )}

            <div className="grid grid-cols-1 md:grid-cols-2 gap-2">
              <button
                type="button"
                onClick={() => handleSessionReplacementDecision(true)}
                disabled={isConfirmingReplacement}
                className="btn btn-black w-full text-sm font-bold uppercase tracking-wide"
              >
                {isConfirmingReplacement ? "Processing..." : "Replace Oldest Session"}
              </button>
              <button
                type="button"
                onClick={() => handleSessionReplacementDecision(false)}
                disabled={isConfirmingReplacement}
                className="btn btn-ghost w-full text-sm font-bold uppercase tracking-wide"
              >
                Cancel
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
