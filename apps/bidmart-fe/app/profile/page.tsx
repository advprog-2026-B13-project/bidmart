"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import { useAuth } from "@/components/auth-provider";
import { getProfile, listMyBids } from "@/lib/auth/auth-api";
import type { BidResponse, ProfileResponse } from "@/lib/auth/types";

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

function formatCurrency(value: number) {
  return new Intl.NumberFormat("id-ID", {
    style: "currency",
    currency: "IDR",
    maximumFractionDigits: 0,
  }).format(value);
}

function BidTable({ bids }: { bids: BidResponse[] }) {
  if (bids.length === 0) {
    return (
      <div className="border-2 border-dashed border-gray-300 bg-gray-50 p-6">
        <p className="text-sm font-bold uppercase tracking-wide text-gray-600">No bids found for this account.</p>
      </div>
    );
  }

  return (
    <div className="overflow-x-auto border-2 border-black">
      <table className="w-full min-w-170 text-sm bg-white">
        <thead>
          <tr className="border-b-2 border-black bg-gray-100">
            <th className="py-3 px-3 text-left font-black uppercase tracking-wide text-xs">Listing</th>
            <th className="py-3 px-3 text-left font-black uppercase tracking-wide text-xs">Amount</th>
            <th className="py-3 px-3 text-left font-black uppercase tracking-wide text-xs">Status</th>
            <th className="py-3 px-3 text-left font-black uppercase tracking-wide text-xs">Created At</th>
          </tr>
        </thead>
        <tbody>
          {bids.map((bid) => (
            <tr key={bid.bidId} className="border-b border-gray-200">
              <td className="py-3 px-3 font-semibold text-electric">
                <Link href={`/listing/${bid.listingId}`} className="hover:underline">
                  {bid.listingId}
                </Link>
              </td>
              <td className="py-3 px-3 font-black">{formatCurrency(bid.amount)}</td>
              <td className="py-3 px-3 font-semibold">{bid.status || "UNKNOWN"}</td>
              <td className="py-3 px-3 text-gray-600">{formatDate(bid.createdAt)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export default function ProfilePage() {
  const router = useRouter();
  const { isAuthenticated, isHydrating, user } = useAuth();

  const [profile, setProfile] = useState<ProfileResponse | null>(null);
  const [myBids, setMyBids] = useState<BidResponse[]>([]);
  const [targetUserId, setTargetUserId] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!isHydrating && !isAuthenticated) {
      router.replace("/login");
    }
  }, [isAuthenticated, isHydrating, router]);

  useEffect(() => {
    let isMounted = true;

    async function load() {
      if (isHydrating || !isAuthenticated) {
        return;
      }

      setIsLoading(true);
      setError("");

      try {
        const [profileResponse, bidsResponse] = await Promise.all([
          getProfile(),
          listMyBids(),
        ]);

        if (!isMounted) {
          return;
        }

        setProfile(profileResponse);
        setMyBids(bidsResponse);
      } catch (loadError) {
        if (isMounted) {
          setError(getErrorMessage(loadError));
        }
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    load();

    return () => {
      isMounted = false;
    };
  }, [isAuthenticated, isHydrating]);

  const displayName = useMemo(() => {
    return profile?.displayName || profile?.email || user?.email || "Account";
  }, [profile?.displayName, profile?.email, user?.email]);

  const openOtherProfile = () => {
    const value = targetUserId.trim();
    if (!value) {
      return;
    }

    router.push(`/profile/${value}`);
  };

  if (isHydrating || !isAuthenticated) {
    return (
      <div className="min-h-screen bg-gray-100 flex items-center justify-center px-4">
        <div className="border-3 border-black bg-white px-6 py-5 shadow-[8px_8px_0_#0A0A0A]">
          <p className="text-sm font-bold uppercase tracking-wide text-gray-700">Loading profile...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-100">
      <div className="max-w-6xl mx-auto px-4 py-10 md:py-14 space-y-6">
        <div className="flex flex-col md:flex-row md:items-end md:justify-between gap-4">
          <div>
            <p className="text-[10px] font-black uppercase tracking-widest text-gray-500">Account</p>
            <h1 className="text-3xl md:text-5xl font-black uppercase tracking-tighter text-black">
              My Profile
            </h1>
          </div>
        </div>

        {error && (
          <div className="p-4 bg-hot/10 border-2 border-hot text-hot text-sm font-bold">
            {error}
          </div>
        )}

        {isLoading ? (
          <div className="border-3 border-black bg-white p-8 shadow-[8px_8px_0_#0A0A0A]">
            <p className="text-sm font-bold uppercase tracking-wide text-gray-600">Loading profile data...</p>
          </div>
        ) : (
          <>
            <section className="border-2 border-black bg-white p-6 shadow-[6px_6px_0_#0A0A0A]">
              <div className="flex flex-col md:flex-row md:items-start gap-5">
                <div className="w-20 h-20 border-2 border-black bg-acid flex items-center justify-center text-3xl font-black">
                  {displayName.charAt(0).toUpperCase()}
                </div>

                <div className="grid gap-2 text-sm w-full">
                  <h2 className="text-2xl md:text-3xl font-black uppercase tracking-tight">{displayName}</h2>
                  <p><span className="font-black uppercase tracking-wide text-xs text-gray-500">Email</span><br />{profile?.email || "Unknown"}</p>
                  <p><span className="font-black uppercase tracking-wide text-xs text-gray-500">Role</span><br />{profile?.role || "UNKNOWN"}</p>
                  <p><span className="font-black uppercase tracking-wide text-xs text-gray-500">Status</span><br />{profile?.status || "UNKNOWN"}</p>
                  <p><span className="font-black uppercase tracking-wide text-xs text-gray-500">Created At</span><br />{formatDate(profile?.createdAt)}</p>
                  {profile?.shippingAddress && (
                    <p><span className="font-black uppercase tracking-wide text-xs text-gray-500">Shipping Address</span><br />{profile.shippingAddress}</p>
                  )}
                </div>
              </div>
            </section>

            <section className="border-2 border-black bg-white p-6 shadow-[6px_6px_0_#0A0A0A]">
              <div className="flex items-center justify-between gap-3 mb-4">
                <div>
                  <p className="text-[10px] font-black uppercase tracking-widest text-gray-500">Bidding</p>
                  <h2 className="text-2xl md:text-3xl font-black uppercase tracking-tight">My Bidding History</h2>
                </div>
                <span className="px-3 py-1 border-2 border-black bg-acid text-xs font-black uppercase tracking-widest">
                  {myBids.length} Bids
                </span>
              </div>

              <BidTable bids={myBids} />
            </section>
          </>
        )}
      </div>
    </div>
  );
}
