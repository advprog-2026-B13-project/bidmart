"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { SubmitEventHandler, useEffect, useMemo, useState } from "react";
import { ChevronDown } from "lucide-react";
import { useAuth } from "@/components/auth-provider";
import {
  activateListing,
  closeListing,
  deleteListing,
  getListingById,
  getMyListings,
  type SellerListing,
  type Order,
  getBuyerOrders,
  getSellerOrders
} from "@/lib/api/endpoints";
import {
  createTopUpTransaction,
  getMyWallet,
  getMyWalletTransactions,
  type TopUpResponse,
  type WalletSummary,
  type WalletTransaction,
} from "@/lib/api/wallet";
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

function formatTransactionType(type?: string) {
  switch ((type || "").toUpperCase()) {
    case "TOP_UP":
      return "Top Up";
    case "WITHDRAW":
      return "Withdraw";
    case "HOLD":
      return "Hold";
    case "RELEASE":
      return "Release";
    case "PAYMENT":
      return "Payment";
    default:
      return type || "Unknown";
  }
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

function BuyerOrderList({ orders }: { orders: Order[] }) {
  const [listingNames, setListingNames] = useState<Record<string, string>>({});

  useEffect(() => {
    const uniqueIds = [...new Set(orders.map(o => o.listingId))];
    Promise.all(uniqueIds.map(id => getListingById(id).catch(() => null)))
        .then(listings => {
          const map: Record<string, string> = {};
          listings.forEach((listing, i) => {
            if (listing) map[uniqueIds[i]] = listing.title;
          });
          setListingNames(map);
        });
  }, [orders]);

  if (orders.length === 0) {
    return (
        <div className="border-2 border-dashed border-gray-300 bg-gray-50 p-6">
          <p className="text-sm font-bold uppercase tracking-wide text-gray-600">No purchases found.</p>
        </div>
    );
  }

  const getStatusStyle = (status: string) => {
    switch (status.toUpperCase()) {
      case "PENDING": return "bg-yellow-200 text-black border border-black shadow-[2px_2px_0_#000]";
      case "PACKED": return "bg-blue-200 text-black border border-black shadow-[2px_2px_0_#000]";
      case "SHIPPED": return "bg-purple-200 text-black border border-black shadow-[2px_2px_0_#000]";
      case "COMPLETED": return "bg-acid text-black border border-black shadow-[2px_2px_0_#000]";
      case "DISPUTED": return "bg-hot text-white border border-black shadow-[2px_2px_0_#000]";
      default: return "bg-gray-100 text-gray-600 border border-gray-300";
    }
  };

  return (
      <div className="space-y-3">
        {orders.map((order) => {
          const listingName = listingNames[order.listingId] || order.listingId;
          return (
              <div key={order.id} className="border-2 border-black bg-white p-4 flex flex-col md:flex-row md:items-center justify-between gap-4 shadow-[4px_4px_0_#000] hover:translate-x-[2px] hover:translate-y-[2px] hover:shadow-[2px_2px_0_#000] transition-all">
                <div>
                  <p className="font-black text-xs text-electric uppercase tracking-wide">
                    Order #{order.id.slice(0, 8).toUpperCase()}
                  </p>
                  <h3 className="font-bold text-base mt-1 text-black">{listingName}</h3>
                  <p className="text-xs text-gray-400 mt-1">
                    Created: {formatDate(order.createdAt)}
                  </p>
                  {order.trackingNumber && (
                      <p className="text-xs font-bold text-gray-600 mt-1">
                        Resi: <span className="font-mono text-black bg-gray-100 px-1.5 py-0.5 border border-black">{order.trackingNumber}</span>
                      </p>
                  )}
                </div>
                <div className="flex flex-row md:flex-col items-center md:items-end justify-between md:justify-center gap-3">
                  <div className="md:text-right">
                    <p className="font-black text-sm">{formatCurrency(order.totalAmount)}</p>
                    <span className={`text-[9px] font-black uppercase px-2 py-0.5 mt-1 inline-block ${getStatusStyle(order.status)}`}>
                  {order.status}
                </span>
                  </div>
                  <Link href={`/orders/${order.id}`} className="btn btn-ghost btn-sm text-xs font-bold uppercase border-2 border-black shadow-[2px_2px_0_#000]">
                    Track / Manage
                  </Link>
                </div>
              </div>
          );
        })}
      </div>
  );
}

function SellerOrderList({ orders }: { orders: Order[] }) {
  const [listingNames, setListingNames] = useState<Record<string, string>>({});

  useEffect(() => {
    const uniqueIds = [...new Set(orders.map(o => o.listingId))];
    Promise.all(uniqueIds.map(id => getListingById(id).catch(() => null)))
        .then(listings => {
          const map: Record<string, string> = {};
          listings.forEach((listing, i) => {
            if (listing) map[uniqueIds[i]] = listing.title;
          });
          setListingNames(map);
        });
  }, [orders]);

  if (orders.length === 0) {
    return (
        <div className="border-2 border-dashed border-gray-300 bg-gray-50 p-6">
          <p className="text-sm font-bold uppercase tracking-wide text-gray-600">No sales found.</p>
        </div>
    );
  }

  const getStatusStyle = (status: string) => {
    switch (status.toUpperCase()) {
      case "PENDING": return "bg-yellow-200 text-black border border-black shadow-[2px_2px_0_#000]";
      case "PACKED": return "bg-blue-200 text-black border border-black shadow-[2px_2px_0_#000]";
      case "SHIPPED": return "bg-purple-200 text-black border border-black shadow-[2px_2px_0_#000]";
      case "COMPLETED": return "bg-acid text-black border border-black shadow-[2px_2px_0_#000]";
      case "DISPUTED": return "bg-hot text-white border border-black shadow-[2px_2px_0_#000]";
      default: return "bg-gray-100 text-gray-600 border border-gray-300";
    }
  };

  return (
      <div className="space-y-3">
        {orders.map((order) => {
          const listingName = listingNames[order.listingId] || order.listingId;
          return (
              <div key={order.id} className="border-2 border-black bg-white p-4 flex flex-col md:flex-row md:items-center justify-between gap-4 shadow-[4px_4px_0_#000] hover:translate-x-[2px] hover:translate-y-[2px] hover:shadow-[2px_2px_0_#000] transition-all">
                <div>
                  <p className="font-black text-xs text-electric uppercase tracking-wide">
                    Order #{order.id.slice(0, 8).toUpperCase()}
                  </p>
                  <h3 className="font-bold text-base mt-1 text-black">{listingName}</h3>
                  <p className="text-xs text-gray-400 mt-1">
                    Created: {formatDate(order.createdAt)}
                  </p>
                  {order.trackingNumber && (
                      <p className="text-xs font-bold text-gray-600 mt-1">
                        Resi: <span className="font-mono text-black bg-gray-100 px-1.5 py-0.5 border border-black">{order.trackingNumber}</span>
                      </p>
                  )}
                </div>
                <div className="flex flex-row md:flex-col items-center md:items-end justify-between md:justify-center gap-3">
                  <div className="md:text-right">
                    <p className="font-black text-sm">{formatCurrency(order.totalAmount)}</p>
                    <span className={`text-[9px] font-black uppercase px-2 py-0.5 mt-1 inline-block ${getStatusStyle(order.status)}`}>
                  {order.status}
                </span>
                  </div>
                  <Link href={`/orders/${order.id}`} className="btn btn-ghost btn-sm text-xs font-bold uppercase border-2 border-black shadow-[2px_2px_0_#000]">
                    Track / Manage
                  </Link>
                </div>
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
  const [buyerOrders, setBuyerOrders] = useState<Order[]>([]);
  const [sellerOrders, setSellerOrders] = useState<Order[]>([]);
  const [wallet, setWallet] = useState<WalletSummary | null>(null);
  const [walletTransactions, setWalletTransactions] = useState<WalletTransaction[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isWalletLoading, setIsWalletLoading] = useState(true);
  const [isDeleting, setIsDeleting] = useState<string | null>(null);
  const [isUpdatingStatus, setIsUpdatingStatus] = useState<string | null>(null);
  const [isTopUpSubmitting, setIsTopUpSubmitting] = useState(false);
  const [activateTarget, setActivateTarget] = useState<SellerListing | null>(null);
  const [error, setError] = useState("");
  const [walletError, setWalletError] = useState("");
  const [topUpMethod, setTopUpMethod] = useState("bank_transfer");
  const [topUpAmount, setTopUpAmount] = useState("");
  const [topUpBank, setTopUpBank] = useState("bca");
  const [topUpResult, setTopUpResult] = useState<TopUpResponse | null>(null);

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

        if (profileResponse?.userId) {
          const [buyerOrdersRes, sellerOrdersRes] = await Promise.all([
            getBuyerOrders(profileResponse.userId),
            getSellerOrders(profileResponse.userId),
          ]);
          if (isMounted) {
            setBuyerOrders(buyerOrdersRes);
            setSellerOrders(sellerOrdersRes);
          }
        }

        await loadWallet(() => isMounted);
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

  const loadWallet = async (isActive?: () => boolean) => {
    setIsWalletLoading(true);
    setWalletError("");

    try {
      const [walletResponse, transactionsResponse] = await Promise.all([
        getMyWallet(),
        getMyWalletTransactions(),
      ]);

      if (isActive && !isActive()) {
        return;
      }

      setWallet(walletResponse);
      setWalletTransactions(transactionsResponse);
    } catch (loadError) {
      if (!isActive || isActive()) {
        setWalletError(getErrorMessage(loadError));
      }
    } finally {
      if (!isActive || isActive()) {
        setIsWalletLoading(false);
      }
    }
  };

  const handleTopUp: SubmitEventHandler<HTMLFormElement> = async (event) => {
    event.preventDefault();

    if (!profile?.userId) {
      setWalletError("Unable to resolve user ID for top up.");
      return;
    }

    const amountNumber = Number(topUpAmount);
    if (!Number.isFinite(amountNumber) || amountNumber <= 0) {
      setWalletError("Top up amount must be greater than zero.");
      return;
    }

    if (!Number.isInteger(amountNumber)) {
      setWalletError("Top up amount must be a whole number.");
      return;
    }

    setWalletError("");
    setIsTopUpSubmitting(true);
    setTopUpResult(null);

    try {
      const shouldSendBank = topUpMethod === "bank_transfer" || topUpMethod === "qris";
      const response = await createTopUpTransaction({
        userId: profile.userId,
        amount: amountNumber,
        paymentType: topUpMethod,
        bank: shouldSendBank ? (topUpBank || undefined) : undefined,
      });

      setTopUpResult(response);
    } catch (topUpError) {
      setWalletError(getErrorMessage(topUpError));
    } finally {
      setIsTopUpSubmitting(false);
    }
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

  const totalBalance = wallet
    ? wallet.availableBalance + wallet.heldBalance
    : 0;

  const recentTransactions = [...walletTransactions]
    .sort((a, b) => {
      const aTime = a.createdAt ? new Date(a.createdAt).getTime() : 0;
      const bTime = b.createdAt ? new Date(b.createdAt).getTime() : 0;
      return bTime - aTime;
    })
    .slice(0, 8);

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
              <div className="flex flex-col md:flex-row md:items-start md:justify-between gap-4 mb-4">
                <div>
                  <p className="text-[10px] font-black uppercase tracking-widest text-gray-500">Wallet</p>
                  <h2 className="text-2xl md:text-3xl font-black uppercase tracking-tight">Balance & Payments</h2>
                  <p className="text-sm text-gray-600 mt-2 max-w-2xl">
                    Available balance can be used for new bids. Held balance is reserved for active bids and will be
                    released automatically if you get outbid.
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => loadWallet()}
                  disabled={isWalletLoading}
                  className="btn btn-ghost btn-sm text-xs font-bold uppercase"
                >
                  {isWalletLoading ? "Refreshing..." : "Refresh"}
                </button>
              </div>

              {walletError && (
                <div className="mb-4 p-3 bg-hot/10 border-2 border-hot text-hot text-sm font-bold">
                  {walletError}
                </div>
              )}

              {isWalletLoading ? (
                <div className="border-2 border-dashed border-gray-300 bg-gray-50 p-6">
                  <p className="text-sm font-bold uppercase tracking-wide text-gray-600">Loading wallet...</p>
                </div>
              ) : (
                <div className="grid gap-3 md:grid-cols-3">
                  <div className="border-2 border-black bg-gray-50 p-4">
                    <p className="text-[10px] font-black uppercase tracking-widest text-gray-500">Available</p>
                    <p className="text-2xl font-black text-black mt-2">
                      {formatCurrency(wallet?.availableBalance ?? 0)}
                    </p>
                  </div>
                  <div className="border-2 border-black bg-gray-50 p-4">
                    <p className="text-[10px] font-black uppercase tracking-widest text-gray-500">Held</p>
                    <p className="text-2xl font-black text-black mt-2">
                      {formatCurrency(wallet?.heldBalance ?? 0)}
                    </p>
                  </div>
                  <div className="border-2 border-black bg-gray-50 p-4">
                    <p className="text-[10px] font-black uppercase tracking-widest text-gray-500">Total</p>
                    <p className="text-2xl font-black text-black mt-2">
                      {formatCurrency(totalBalance)}
                    </p>
                  </div>
                </div>
              )}

              <div className="grid gap-4 md:grid-cols-2 mt-6">
                <div className="border-2 border-black bg-white p-4">
                  <p className="text-[10px] font-black uppercase tracking-widest text-gray-500">Top Up</p>
                  <form onSubmit={(event) => { handleTopUp(event); }} className="grid gap-3 mt-3">
                    <div>
                      <label htmlFor="topUpAmount" className="text-[10px] font-black uppercase tracking-widest text-gray-500">Amount (IDR)</label>
                      <input
                        id="topUpAmount"
                        type="number"
                        min={1}
                        step={1}
                        value={topUpAmount}
                        onChange={(event) => setTopUpAmount(event.target.value)}
                        className="input mt-2"
                        placeholder="10000"
                        required
                      />
                    </div>
                    <div>
                      <label htmlFor="topUpBank" className="text-[10px] font-black uppercase tracking-widest text-gray-500">Bank</label>
                      <select
                        id="topUpBank"
                        value={topUpBank}
                        onChange={(event) => setTopUpBank(event.target.value)}
                        className="input mt-2"
                      >
                        <option value="bca">BCA</option>
                        <option value="bni">BNI</option>
                        <option value="bri">BRI</option>
                        <option value="permata">Permata</option>
                      </select>
                    </div>
                    <button
                      type="submit"
                      disabled={isTopUpSubmitting}
                      className="btn btn-electric text-xs font-bold uppercase tracking-wide"
                    >
                      {isTopUpSubmitting ? "Creating..." : "Create Top Up"}
                    </button>
                  </form>
                </div>

                <div className="border-2 border-black bg-gray-50 p-4">
                  <p className="text-[10px] font-black uppercase tracking-widest text-gray-500">Latest Top Up</p>
                  {topUpResult ? (
                    <div className="mt-3 text-sm">
                      <p className="font-black uppercase tracking-wide">Order ID</p>
                      <p className="font-mono text-xs break-all mb-3">{topUpResult.orderId}</p>
                      <p className="font-black uppercase tracking-wide">Bank Transfer</p>
                      <p className="font-bold">{topUpResult.bank.toUpperCase()}</p>
                      <p className="font-black uppercase tracking-wide mt-3">VA Number</p>
                      <p className="font-mono text-lg font-black">
                        {topUpResult.vaNumber || "Pending"}
                      </p>
                      <p className="font-black uppercase tracking-wide mt-3">Status</p>
                      <p className="font-bold">{topUpResult.transactionStatus}</p>
                      <p className="text-xs text-gray-600 mt-3">
                        Complete the transfer, then refresh your wallet balance.
                      </p>
                    </div>
                  ) : (
                    <p className="text-sm text-gray-600 mt-3">
                      Create a top up to receive a virtual account number for bank transfer.
                    </p>
                  )}
                </div>
              </div>

              <div className="mt-6">
                <div className="flex items-center justify-between mb-3">
                  <p className="text-[10px] font-black uppercase tracking-widest text-gray-500">Recent Transactions</p>
                  <span className="text-xs font-bold uppercase text-gray-400">
                    {walletTransactions.length} total
                  </span>
                </div>

                {recentTransactions.length === 0 ? (
                  <div className="border-2 border-dashed border-gray-300 bg-gray-50 p-6">
                    <p className="text-sm font-bold uppercase tracking-wide text-gray-600">No wallet activity yet.</p>
                  </div>
                ) : (
                  <div className="divide-y divide-gray-200 border-2 border-black">
                    {recentTransactions.map((tx) => (
                      <div key={tx.id} className="flex items-center justify-between p-4 bg-white">
                        <div>
                          <p className="font-black text-sm uppercase tracking-wide">{formatTransactionType(tx.type)}</p>
                          <p className="text-xs text-gray-500">{formatDate(tx.createdAt ?? "")}</p>
                        </div>
                        <p className="font-black text-sm">
                          {formatCurrency(Number(tx.amount))}
                        </p>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </section>

            <section className="border-2 border-black bg-white p-6 shadow-[6px_6px_0_#0A0A0A]">
              <div className="flex items-center justify-between gap-3 mb-4">
                <div>
                  <p className="text-[10px] font-black uppercase tracking-widest text-gray-500">Purchases</p>
                  <h2 className="text-2xl md:text-3xl font-black uppercase tracking-tight">My Purchases</h2>
                </div>
                <span className="px-3 py-1 border-2 border-black bg-blue-200 text-xs font-black uppercase tracking-widest shadow-[2px_2px_0_#000]">
                  {buyerOrders.length} Orders
                </span>
              </div>

              <BuyerOrderList orders={buyerOrders} />
            </section>

            <section className="border-2 border-black bg-white p-6 shadow-[6px_6px_0_#0A0A0A]">
              <div className="flex items-center justify-between gap-3 mb-4">
                <div>
                  <p className="text-[10px] font-black uppercase tracking-widest text-gray-500">Sales</p>
                  <h2 className="text-2xl md:text-3xl font-black uppercase tracking-tight">My Sales</h2>
                </div>
                <span className="px-3 py-1 border-2 border-black bg-blue-200 text-xs font-black uppercase tracking-widest shadow-[2px_2px_0_#000]">
                  {sellerOrders.length} Sales
                </span>
              </div>

              <SellerOrderList orders={sellerOrders} />
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
