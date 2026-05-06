"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect, useMemo, useState } from "react";
import { ChevronDown } from "lucide-react";
import { useAuth } from "@/components/auth-provider";
import { activateListing, closeListing, deleteListing, getListingById, getMyListings, type SellerListing } from "@/lib/api/endpoints";
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
  const [expandedListing, setExpandedListing] = useState<string | null>(null);
  const [listingNames, setListingNames] = useState<Record<string, string>>({});

  useEffect(() => {
    const uniqueIds = [...new Set(bids.map(b => b.listingId))];
    Promise.all(uniqueIds.map(id => getListingById(id).catch(() => null)))
      .then(listings => {
        const map: Record<string, string> = {};
        listings.forEach((listing, i) => {
          if (listing) map[uniqueIds[i]] = listing.title;
        });
        setListingNames(map);
      });
  }, [bids]);

  if (bids.length === 0) {
    return (
      <div className="border-2 border-dashed border-gray-300 bg-gray-50 p-6">
        <p className="text-sm font-bold uppercase tracking-wide text-gray-600">No bids found for this account.</p>
      </div>
    );
  }

  // Group bids by listing
  const grouped = bids.reduce<Record<string, BidResponse[]>>((acc, bid) => {
    if (!acc[bid.listingId]) acc[bid.listingId] = [];
    acc[bid.listingId].push(bid);
    return acc;
  }, {});

  const sortedListings = Object.entries(grouped).sort(
    (a, b) => new Date(b[1][0].createdAt!).getTime() - new Date(a[1][0].createdAt!).getTime()
  );

  const getStatusStyle = (status?: string) => {
    switch (status?.toUpperCase()) {
      case "ACCEPTED": return "bg-electric/10 text-electric border border-electric";
      case "OUTBID": return "bg-hot/10 text-hot border border-hot";
      case "WON": return "bg-acid/20 text-black border border-acid";
      default: return "bg-gray-100 text-gray-600 border border-gray-300";
    }
  };

  return (
    <div className="space-y-3">
      {sortedListings.map(([listingId, listingBids]) => {
        const isOpen = expandedListing === listingId;
        const latestBid = listingBids[0];
        const listingName = listingNames[listingId] || listingId;

        return (
          <div key={listingId} className="border-2 border-black bg-white">
            {/* Group header — always visible */}
            <button
              onClick={() => setExpandedListing(isOpen ? null : listingId)}
              className="w-full flex items-center justify-between p-4 hover:bg-gray-50 transition-colors text-left"
            >
              <div className="flex-1 min-w-0">
                <p className="font-black text-sm text-electric truncate">
                  {listingName}
                </p>
                <p className="text-xs text-gray-400 mt-0.5">
                  {listingBids.length} bid{listingBids.length > 1 ? "s" : ""} ·{" "}
                  {formatDate(latestBid.createdAt)}
                </p>
              </div>
              <div className="flex items-center gap-3 ml-4 shrink-0">
                <div className="text-right">
                  <p className="font-black text-sm">
                    {formatCurrency(latestBid.amount)}
                  </p>
                  <span className={`text-[10px] font-black uppercase px-2 py-0.5 ${getStatusStyle(latestBid.status)}`}>
                    {latestBid.status || "UNKNOWN"}
                  </span>
                </div>
                <ChevronDown className={`w-5 h-5 text-gray-400 transition-transform ${isOpen ? "rotate-180" : ""}`} />
              </div>
            </button>

            {/* Expanded bid details */}
            {isOpen && (
              <div className="border-t-2 border-black">
                <div className="divide-y divide-gray-200">
                  {listingBids
                    .sort((a, b) => new Date(b.createdAt!).getTime() - new Date(a.createdAt!).getTime())
                    .map(bid => (
                      <div key={bid.bidId} className="flex items-center justify-between p-4">
                        <div className="flex items-center gap-4">
                          <div className="w-8 h-8 bg-gray-100 border border-gray-300 flex items-center justify-center">
                            <span className="text-xs font-black text-gray-500">{bid.bidId.slice(0, 2).toUpperCase()}</span>
                          </div>
                          <div>
                            <p className="font-black text-sm">{formatCurrency(bid.amount)}</p>
                            <p className="text-xs text-gray-400">{formatDate(bid.createdAt)}</p>
                          </div>
                        </div>
                        <div className="flex items-center gap-3">
                          <span className={`text-[10px] font-black uppercase px-2 py-0.5 ${getStatusStyle(bid.status)}`}>
                            {bid.status || "UNKNOWN"}
                          </span>
                          <Link href={`/listing/${listingId}`} className="text-xs font-black text-electric hover:underline uppercase">
                            View
                          </Link>
                        </div>
                      </div>
                    ))}
                </div>
              </div>
            )}
          </div>
        );
      })}
    </div>
  );
}

export default function ProfilePage() {
  const router = useRouter();
  const { isAuthenticated, isHydrating, user } = useAuth();

  const [profile, setProfile] = useState<ProfileResponse | null>(null);
  const [myBids, setMyBids] = useState<BidResponse[]>([]);
  const [myListings, setMyListings] = useState<SellerListing[]>([]);
  const [targetUserId, setTargetUserId] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isDeleting, setIsDeleting] = useState<string | null>(null);
  const [isUpdatingStatus, setIsUpdatingStatus] = useState<string | null>(null);
  const [activateTarget, setActivateTarget] = useState<SellerListing | null>(null);
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
        const [profileResponse, bidsResponse, listingsResponse] = await Promise.all([
          getProfile(),
          listMyBids(),
          getMyListings(),
        ]);

        if (!isMounted) {
          return;
        }

        setProfile(profileResponse);
        setMyBids(bidsResponse);
        setMyListings(listingsResponse);
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

  const handleDeleteListing = async (listingId: string, title: string) => {
    if (!listingId) {
      return;
    }

    const confirmed = window.confirm(`Delete draft listing "${title}"? This cannot be undone.`);
    if (!confirmed) {
      return;
    }

    setIsDeleting(listingId);
    setError("");

    try {
      await deleteListing(listingId);
      setMyListings((current) => current.filter((listing) => listing.id !== listingId));
    } catch (deleteError) {
      setError(getErrorMessage(deleteError));
    } finally {
      setIsDeleting(null);
    }
  };

  const handleActivateListing = async () => {
    if (!activateTarget) {
      return;
    }

    setIsUpdatingStatus(activateTarget.id);
    setError("");

    try {
      const updated = await activateListing(activateTarget.id);
      setMyListings((current) => current.map((listing) => (
        listing.id === activateTarget.id
          ? { ...listing, status: updated.status }
          : listing
      )));
      setActivateTarget(null);
    } catch (activateError) {
      setError(getErrorMessage(activateError));
    } finally {
      setIsUpdatingStatus(null);
    }
  };

  const handleCloseListing = async (listing: SellerListing) => {
    const confirmed = window.confirm(`Close listing "${listing.title}"? This cannot be undone.`);
    if (!confirmed) {
      return;
    }

    setIsUpdatingStatus(listing.id);
    setError("");

    try {
      const updated = await closeListing(listing.id);
      setMyListings((current) => current.map((item) => (
        item.id === listing.id
          ? { ...item, status: updated.status }
          : item
      )));
    } catch (closeError) {
      setError(getErrorMessage(closeError));
    } finally {
      setIsUpdatingStatus(null);
    }
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

            <section className="border-2 border-black bg-white p-6 shadow-[6px_6px_0_#0A0A0A]">
              <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4 mb-4">
                <div>
                  <p className="text-[10px] font-black uppercase tracking-widest text-gray-500">Seller</p>
                  <h2 className="text-2xl md:text-3xl font-black uppercase tracking-tight">My Listings</h2>
                </div>
                <Link href="/listing/new" className="btn btn-acid text-xs font-bold uppercase tracking-wide">
                  Create Listing
                </Link>
              </div>

              {myListings.length === 0 ? (
                <div className="border-2 border-dashed border-gray-300 bg-gray-50 p-6">
                  <p className="text-sm font-bold uppercase tracking-wide text-gray-600">No listings yet.</p>
                </div>
              ) : (
                <div className="space-y-4">
                  {myListings.map((listing) => {
                    const isDraft = listing.status === "DRAFT";
                    const isActive = listing.status === "ACTIVE";
                    const isBeforeStart = listing.startTime.getTime() > Date.now();
                    return (
                      <div key={listing.id} className="border-2 border-black bg-gray-50 p-4">
                        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
                          <div className="flex items-center gap-4">
                            <div className="w-16 h-16 border-2 border-black bg-white overflow-hidden">
                              {listing.imageUrl ? (
                                <img src={listing.imageUrl} alt={listing.title} className="w-full h-full object-cover" />
                              ) : (
                                <div className="w-full h-full bg-gray-100 flex items-center justify-center text-xs font-black text-gray-400">
                                  NO IMG
                                </div>
                              )}
                            </div>
                            <div>
                              <p className="font-black text-sm uppercase tracking-wide">{listing.title}</p>
                              <p className="text-xs text-gray-500">{listing.categoryName || "Uncategorized"}</p>
                              <p className="text-xs text-gray-500">Ends: {formatDate(listing.endTime.toISOString())}</p>
                              {isBeforeStart && (
                                <p className="text-xs text-gray-500">Starts: {formatDate(listing.startTime.toISOString())}</p>
                              )}
                            </div>
                          </div>

                          <div className="flex flex-col items-start md:items-end gap-3">
                            <span className="px-2 py-1 border-2 border-black text-[10px] font-black uppercase tracking-widest bg-white">
                              {listing.status}
                            </span>
                            <div className="flex flex-wrap gap-2">
                              <Link href={`/listing/${listing.id}`} className="btn btn-ghost btn-sm text-xs font-bold uppercase">
                                View
                              </Link>
                              {isDraft && (
                                <>
                                  <Link href={`/listing/${listing.id}/edit`} className="btn btn-ghost btn-sm text-xs font-bold uppercase">
                                    Edit
                                  </Link>
                                  <button
                                    type="button"
                                    onClick={() => setActivateTarget(listing)}
                                    disabled={isUpdatingStatus === listing.id}
                                    className="btn btn-ghost btn-sm text-xs font-bold uppercase text-electric"
                                  >
                                    {isUpdatingStatus === listing.id ? "Activating..." : "Activate"}
                                  </button>
                                  <button
                                    type="button"
                                    onClick={() => handleDeleteListing(listing.id, listing.title)}
                                    disabled={isDeleting === listing.id}
                                    className="btn btn-ghost btn-sm text-xs font-bold uppercase text-hot"
                                  >
                                    {isDeleting === listing.id ? "Deleting..." : "Delete"}
                                  </button>
                                </>
                              )}
                              {isActive && isBeforeStart && (
                                <button
                                  type="button"
                                  onClick={() => handleCloseListing(listing)}
                                  disabled={isUpdatingStatus === listing.id}
                                  className="btn btn-ghost btn-sm text-xs font-bold uppercase text-hot"
                                >
                                  {isUpdatingStatus === listing.id ? "Closing..." : "Close"}
                                </button>
                              )}
                            </div>
                          </div>
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </section>
          </>
        )}
      </div>

      {activateTarget && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4">
          <div className="max-w-md w-full border-3 border-black bg-white p-6 shadow-[8px_8px_0_#0A0A0A]">
            <p className="text-xs font-black uppercase tracking-widest text-gray-500">Activate Listing</p>
            <h3 className="text-2xl font-black uppercase tracking-tight mt-2">{activateTarget.title}</h3>
            <p className="text-sm text-gray-600 mt-3">
              This action is irreversible. Once activated, the listing can no longer be edited as a draft.
            </p>
            <div className="flex flex-wrap gap-3 mt-6">
              <button
                type="button"
                onClick={handleActivateListing}
                disabled={isUpdatingStatus === activateTarget.id}
                className="btn btn-acid text-xs font-bold uppercase tracking-wide"
              >
                {isUpdatingStatus === activateTarget.id ? "Activating..." : "Activate"}
              </button>
              <button
                type="button"
                onClick={() => setActivateTarget(null)}
                className="btn btn-ghost text-xs font-bold uppercase tracking-wide"
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
