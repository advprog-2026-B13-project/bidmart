"use client";

import { useEffect } from "react";
import { useRouter, useSearchParams } from "next/navigation";

export default function VerifyEmailRedirectPage() {
  const router = useRouter();
  const searchParams = useSearchParams();

  useEffect(() => {
    const query = searchParams.toString();
    const destination = query ? `/verify-email?${query}` : "/verify-email";
    router.replace(destination);
  }, [router, searchParams]);

  return (
    <div className="min-h-screen flex items-center justify-center px-4 bg-gray-100">
      <div className="border-3 border-black bg-white px-6 py-5 shadow-[6px_6px_0_#0A0A0A]">
        <p className="text-sm font-bold uppercase tracking-wide text-gray-700">Redirecting to verification page...</p>
      </div>
    </div>
  );
}
