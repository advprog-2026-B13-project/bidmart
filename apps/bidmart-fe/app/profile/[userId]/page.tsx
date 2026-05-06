"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import { useAuth } from "@/components/auth-provider";
import { getOtherUserProfile } from "@/lib/auth/auth-api";
import type { OtherUserBidResponse, OtherUserProfileResponse } from "@/lib/auth/types";

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

function BidCards({ title, bids }: { title: string; bids: OtherUserBidResponse[] }) {
  return (
    <section className="border-2 border-black bg-white p-5 shadow-[5px_5px_0_#0A0A0A]">
      <div className="flex items-center justify-between gap-3 mb-4">
        <h3 className="text-xl font-black uppercase tracking-tight">{title}</h3>
        <span className="px-2 py-1 border border-black bg-acid text-[10px] font-black uppercase tracking-widest">
          {bids.length}
        </span>
      </div>

      {bids.length === 0 ? (
        <div className="border-2 border-dashed border-gray-300 bg-gray-50 p-5">
          <p className="text-sm font-bold uppercase tracking-wide text-gray-600">No bids available.</p>
        </div>
      ) : (
        <div className="space-y-3">
          {bids.map((bid) => (
            <article key={bid.bidId} className="border-2 border-black bg-gray-50 p-4">
              <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-3">
                <div>
                  <p className="text-[10px] font-black uppercase tracking-widest text-gray-500">Listing</p>
                  <Link href={`/listing/${bid.listingId}`} className="font-black text-electric hover:underline break-all">
                    {bid.listingId}
                  </Link>
                </div>
                <div className="text-left md:text-right">
                  <p className="text-[10px] font-black uppercase tracking-widest text-gray-500">Amount</p>
                  <p className="text-xl font-black">{formatCurrency(bid.amount)}</p>
                </div>
              </div>
              <div className="mt-3 pt-3 border-t border-gray-300 flex flex-wrap gap-x-5 gap-y-1 text-sm">
                <p><span className="font-black text-xs uppercase tracking-wide text-gray-500">Status</span> {bid.status || "UNKNOWN"}</p>
                <p><span className="font-black text-xs uppercase tracking-wide text-gray-500">Created</span> {formatDate(bid.createdAt)}</p>
              </div>
            </article>
          ))}
        </div>
      )}
    </section>
  );
}

export default function OtherUserProfilePage({
  params,
}: {
  params: Promise<{ userId: string }>;
}) {
  const router = useRouter();
  const { isAuthenticated, isHydrating, user } = useAuth();

  const [targetUserId, setTargetUserId] = useState("");
  const [profile, setProfile] = useState<OtherUserProfileResponse | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    params.then((value) => {
      setTargetUserId(value.userId || "");
    });
  }, [params]);

  useEffect(() => {
    if (!isHydrating && !isAuthenticated) {
      router.replace("/login");
    }
  }, [isAuthenticated, isHydrating, router]);

  useEffect(() => {
    let isMounted = true;

    async function load() {
      if (isHydrating || !isAuthenticated || !targetUserId) {
        return;
      }

      setIsLoading(true);
      setError("");

      try {
        const response = await getOtherUserProfile(targetUserId);

        if (!isMounted) {
          return;
        }

        setProfile(response);
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
  }, [isAuthenticated, isHydrating, targetUserId]);

  const canSeeBiddingHistory = useMemo(() => {
    return Boolean(profile?.biddingHistoryVisible);
  }, [profile?.biddingHistoryVisible]);

  const previousBids = profile?.previousBids || [];
  const ongoingBids = profile?.ongoingBids || [];
  const isOwnProfile = Boolean(user?.userId && targetUserId && user.userId === targetUserId);

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
            <p className="text-[10px] font-black uppercase tracking-widest text-gray-500">Profile</p>
            <h1 className="text-3xl md:text-5xl font-black uppercase tracking-tighter text-black">
              User Profile
            </h1>
          </div>
          <div className="flex flex-wrap gap-2">
            <Link href="/profile" className="btn btn-ghost text-xs font-bold uppercase tracking-wide">My Profile</Link>
            <Link href="/" className="btn btn-ghost text-xs font-bold uppercase tracking-wide">Home</Link>
          </div>
        </div>

        {isOwnProfile && (
          <div className="p-4 border-2 border-black bg-acid/20 text-sm font-bold">
            This is your own account ID. For full personal profile details, use the My Profile page.
          </div>
        )}

        {error && (
          <div className="p-4 bg-hot/10 border-2 border-hot text-hot text-sm font-bold">
            {error}
          </div>
        )}

        {isLoading ? (
          <div className="border-3 border-black bg-white p-8 shadow-[8px_8px_0_#0A0A0A]">
            <p className="text-sm font-bold uppercase tracking-wide text-gray-600">Loading user profile...</p>
          </div>
        ) : profile ? (
          <>
            <section className="border-2 border-black bg-white p-6 shadow-[6px_6px_0_#0A0A0A]">
              <div className="grid gap-2 text-sm">
                <h2 className="text-2xl md:text-3xl font-black uppercase tracking-tight mb-2">
                  {profile.displayName || profile.email || "Unknown User"}
                </h2>
                <p><span className="font-black uppercase tracking-wide text-xs text-gray-500">User ID</span><br />{profile.userId}</p>
                <p><span className="font-black uppercase tracking-wide text-xs text-gray-500">Email</span><br />{profile.email || "Unknown"}</p>
                <p><span className="font-black uppercase tracking-wide text-xs text-gray-500">Status</span><br />{profile.status || "UNKNOWN"}</p>
                {profile.shippingAddress && (
                  <p><span className="font-black uppercase tracking-wide text-xs text-gray-500">Shipping Address</span><br />{profile.shippingAddress}</p>
                )}
              </div>
            </section>

            <section className="border-2 border-black bg-white p-6 shadow-[6px_6px_0_#0A0A0A]">
              <p className="text-[10px] font-black uppercase tracking-widest text-gray-500 mb-2">Bidding Visibility</p>
              {canSeeBiddingHistory ? (
                <p className="text-sm font-bold text-electric">
                  You can view this user&apos;s bidding history (admin access).
                </p>
              ) : (
                <p className="text-sm font-bold text-gray-600">
                  Bidding history is hidden for your current permissions.
                </p>
              )}
            </section>

            {canSeeBiddingHistory && (
              <div className="grid gap-4 lg:grid-cols-2">
                <BidCards title="Ongoing Bids" bids={ongoingBids} />
                <BidCards title="Previous Bids" bids={previousBids} />
              </div>
            )}
          </>
        ) : null}
      </div>
    </div>
  );
}
