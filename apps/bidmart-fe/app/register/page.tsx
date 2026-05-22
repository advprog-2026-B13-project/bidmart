"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { Loader2 } from "lucide-react";
import { useAuth } from "@/components/auth-provider";

function getErrorMessage(error: unknown) {
  if (error instanceof Error && error.message) {
    return error.message;
  }
  return "Failed to create account. Please try again.";
}

export default function RegisterPage() {
  const router = useRouter();
  const { register } = useAuth();
  const [formData, setFormData] = useState({
    name: "",
    email: "",
    password: "",
    confirmPassword: "",
  });
  const [agreedToTerms, setAgreedToTerms] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData(prev => ({ ...prev, [e.target.name]: e.target.value }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    if (formData.password !== formData.confirmPassword) {
      setError("Passwords do not match.");
      return;
    }
    if (formData.password.length < 8) {
      setError("Password must be at least 8 characters.");
      return;
    }
    if (!agreedToTerms) {
      setError("Please agree to the Terms of Service.");
      return;
    }

    setIsLoading(true);

    try {
      const registrationResult = await register({
        email: formData.email,
        password: formData.password,
        displayName: formData.name || undefined,
      });

      if (registrationResult.requiresEmailVerification) {
        const params = new URLSearchParams({
          email: registrationResult.email,
        });

        if (registrationResult.verificationToken) {
          params.set("verificationToken", registrationResult.verificationToken);
        }

        params.set("registered", "true");
        router.push(`/verify-email?${params.toString()}`);
        return;
      }

      router.push("/login?registered=true");
    } catch (submitError) {
      setError(getErrorMessage(submitError));
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="w-full h-screen lg:h-auto lg:min-h-screen flex flex-col-reverse lg:flex-row overflow-hidden">
      {/* Left - Visual Panel */}
      <div className="flex-1 bg-electric flex items-center justify-center p-8 lg:p-12 relative overflow-hidden" style={{ minHeight: "100vh" }}>
        {/* Grid background */}
        <div className="absolute inset-0 grid-bg opacity-10"></div>
        {/* Decorative elements */}
        <div className="absolute top-16 right-16 w-24 h-24 border-[3px] border-white/30 hidden xl:block"></div>
        <div className="absolute bottom-32 left-12 w-40 h-40 border-[3px] border-white/20 hidden xl:block"></div>
        <div className="absolute top-1/3 right-1/4 w-12 h-12 bg-acid/30 hidden xl:block"></div>
        {/* Content */}
        <div className="relative z-10 max-w-md text-center px-4">
          <div className="mb-8">
            <div className="w-16 h-16 lg:w-20 lg:h-20 bg-white flex items-center justify-center border-[3px] border-black mx-auto mb-6 shadow-[6px_6px_0_#0A0A0A] lg:shadow-[8px_8px_0_#0A0A0A]">
              <span className="text-black font-black text-3xl lg:text-4xl">B</span>
            </div>
          </div>
          <h2 className="text-4xl lg:text-5xl font-black text-white mb-4 uppercase tracking-tighter">
            START<br />BIDDING
          </h2>
          <p className="text-white/70 leading-relaxed">
            Free to join. Discover unique items from verified sellers worldwide.
          </p>
          <div className="mt-12 lg:mt-16 space-y-3 text-left">
            {[
              { icon: "🏆", text: "Access exclusive auctions" },
              { icon: "🛡️", text: "Buyer protection guarantee" },
              { icon: "⚡", text: "Real-time bid notifications" },
              { icon: "💳", text: "Secure payment processing" },
            ].map((item, i) => (
              <div key={i} className="flex items-center gap-3 bg-white/10 backdrop-blur-sm border border-white/20 px-4 py-2.5 lg:py-3">
                <span className="text-lg lg:text-xl">{item.icon}</span>
                <span className="text-white font-bold text-xs lg:text-sm uppercase tracking-wide">{item.text}</span>
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

          <div className="mb-8 lg:mb-10">
            <h1 className="text-3xl lg:text-4xl font-black text-black mb-2 uppercase tracking-tighter">
              CREATE ACCOUNT
            </h1>
            <p className="text-gray-500 font-medium">
              Join free and start bidding in minutes
            </p>
          </div>

          {error && (
            <div className="mb-5 p-4 bg-hot/10 border-2 border-hot text-hot text-sm font-bold">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-4 lg:space-y-5">
            <div>
              <label htmlFor="name" className="block text-[10px] lg:text-xs font-black text-gray-500 uppercase tracking-widest mb-2">
                Full Name
              </label>
              <input
                id="name"
                name="name"
                type="text"
                value={formData.name}
                onChange={handleChange}
                className="input"
                placeholder="John Doe"
                required
              />
            </div>

            <div>
              <label htmlFor="email" className="block text-[10px] lg:text-xs font-black text-gray-500 uppercase tracking-widest mb-2">
                Email Address
              </label>
              <input
                id="email"
                name="email"
                type="email"
                value={formData.email}
                onChange={handleChange}
                className="input"
                placeholder="you@example.com"
                required
              />
            </div>

            <div>
              <label htmlFor="password" className="block text-[10px] lg:text-xs font-black text-gray-500 uppercase tracking-widest mb-2">
                Password
              </label>
              <input
                id="password"
                name="password"
                type="password"
                value={formData.password}
                onChange={handleChange}
                className="input"
                placeholder="Min. 8 characters"
                minLength={8}
                required
              />
            </div>

            <div>
              <label htmlFor="confirmPassword" className="block text-[10px] lg:text-xs font-black text-gray-500 uppercase tracking-widest mb-2">
                Confirm Password
              </label>
              <input
                id="confirmPassword"
                name="confirmPassword"
                type="password"
                value={formData.confirmPassword}
                onChange={handleChange}
                className="input"
                placeholder="Confirm your password"
                required
              />
            </div>

            <div className="flex items-start gap-3">
              <input
                id="terms"
                type="checkbox"
                checked={agreedToTerms}
                onChange={(e) => setAgreedToTerms(e.target.checked)}
                className="w-4 h-4 lg:w-5 lg:h-5 border-2 border-black accent-electric mt-0.5"
              />
              <label htmlFor="terms" className="text-sm text-gray-600 font-medium">
                I agree to the{" "}
                <Link href="/terms" className="text-electric font-bold hover:underline">Terms</Link>
                {" "}and{" "}
                <Link href="/privacy" className="text-electric font-bold hover:underline">Privacy Policy</Link>
              </label>
            </div>

            <button
              type="submit"
              disabled={isLoading}
              className="btn btn-black w-full text-sm lg:text-base py-3 lg:py-4 font-bold uppercase tracking-wide"
            >
              {isLoading ? (
                <>
                  <Loader2 className="w-4 h-4 lg:w-5 lg:h-5 animate-spin" />
                  Creating account...
                </>
              ) : (
                "Create Account"
              )}
            </button>
          </form>

          <p className="mt-8 lg:mt-10 text-center text-sm font-medium text-gray-500">
            Already have an account?{" "}
            <Link href="/login" className="text-electric font-black hover:underline uppercase tracking-wide">
              Sign In
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
}
