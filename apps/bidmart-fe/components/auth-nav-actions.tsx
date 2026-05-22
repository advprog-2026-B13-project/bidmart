"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useState } from "react";
import { useAuth } from "./auth-provider";
import { UserRoundCog } from "lucide-react";

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
      {isAdmin && (
        <Link href="/admin" className="w-10 h-10 flex items-center justify-center border-2 border-black shadow-[3px_3px_0_#0A0A0A] hover:shadow-[5px_5px_0_#0A0A0A] hover:translate-x-[-2px] hover:translate-y-[-2px] transition-all bg-white">
          <UserRoundCog className="w-5 h-5" />
        </Link>
      )}
      <Link href="/profile" className="px-3 py-2 flex items-center justify-center border-2 border-black shadow-[3px_3px_0_#0A0A0A] hover:shadow-[5px_5px_0_#0A0A0A] hover:-translate-x-0.5 hover:-translate-y-0.5 transition-all bg-white">
        <span className="text-lg font-black leading-none">{displayName}</span>
      </Link>
      <button
        type="button"
        onClick={() => router.push("/settings")}
        className="btn btn-ghost btn-sm"
      >
        Settings
      </button>
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
