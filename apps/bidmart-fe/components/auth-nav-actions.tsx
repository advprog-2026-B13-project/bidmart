"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { useAuth } from "./auth-provider";
import { apiFetch } from "@/lib/auth/api-client";

export function AuthNavActions() {
  const router = useRouter();
  const { user, isAuthenticated, isHydrating, logout } = useAuth();
  const [isSubmitting, setIsSubmitting] = useState(false);

  const displayName = user?.displayName || user?.email || "Account";
  const isAdmin = (user?.role || "").toUpperCase().includes("ADMIN");

  const handleLogout = async () => {
    setIsSubmitting(true);

    try {
      await logout();
      router.push("/login");
      router.refresh();
    } finally {
      setIsSubmitting(false);
    }
  };

  // Dummy action
  const dummyAction = async () => {
    const data = await apiFetch("/api/bidding/my-bids");
    console.log("Dummy action response:", data);
  }

  if (isHydrating) {
    return (
      <span className="px-4 py-2 font-bold text-sm uppercase tracking-wide text-gray-500">
        Loading...
      </span>
    );
  }

  if (!isAuthenticated) {
    return (
      <>
        <Link href="/login" className="btn btn-ghost btn-sm">
          Sign In
        </Link>
        <Link href="/register" className="btn btn-acid btn-sm">
          Join Free
        </Link>
      </>
    );
  }

  return (
    <>
      <span className="hidden lg:inline px-3 py-2 border-2 border-black text-sm font-black uppercase tracking-wide bg-white">
        {displayName}
      </span>
      <button
        type="button"
        onClick={dummyAction}
        className="btn btn-ghost btn-sm"
      >
        My Bids (Dummy)
      </button>
      <button
        type="button"
        onClick={() => router.push("/sessions")}
        className="btn btn-ghost btn-sm"
      >
        Sessions
      </button>
      {isAdmin && (
        <button
          type="button"
          onClick={() => router.push("/admin")}
          className="btn btn-ghost btn-sm"
        >
          Admin
        </button>
      )}
      <button
        type="button"
        onClick={handleLogout}
        disabled={isSubmitting}
        className="btn btn-ghost btn-sm"
      >
        {isSubmitting ? "Signing Out..." : "Sign Out"}
      </button>
    </>
  );
}
