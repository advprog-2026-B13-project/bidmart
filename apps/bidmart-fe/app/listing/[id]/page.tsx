"use client";

import { useState, useEffect, useRef } from "react";
import Link from "next/link";
import { Check, Star, Loader2, Send, Share2, ChevronLeft, ChevronRight, Info, ExternalLink } from "lucide-react";
import { getListingById, getListingByIdOwner, getBidsForListing, placeBid, getListings, type ParsedListing, type BidResult } from "@/lib/api/endpoints";
import { formatCurrency, formatTimeRemaining, getTimeUrgency } from "@/lib/utils";
import { useToast } from "@/components/toast";
import { useAuth } from "@/components/auth-provider";

function formatEndsAt(endTime: Date): { dateLabel: string; timeStr: string } {
  const date = new Date(endTime);
  const now = new Date();
  const isToday = date.toDateString() === now.toDateString();
  const tomorrow = new Date(now);
  tomorrow.setDate(tomorrow.getDate() + 1);
  const isTomorrow = date.toDateString() === tomorrow.toDateString();

  const timeStr = date.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });

  if (isToday) return { dateLabel: "Today", timeStr };
  if (isTomorrow) return { dateLabel: "Tomorrow", timeStr };
  return { dateLabel: date.toLocaleDateString([], { month: "short", day: "numeric" }), timeStr };
}

function formatStartsAt(startTime: Date): { dateLabel: string; timeStr: string } {
  return formatEndsAt(startTime);
}

function CountdownTimer({ endTime }: { endTime: Date }) {
  const [timeLeft, setTimeLeft] = useState(formatTimeRemaining(endTime));
  const [mounted, setMounted] = useState(false);
  const urgency = getTimeUrgency(endTime);

  useEffect(() => {
    (() => setMounted(true))();
    const update = () => setTimeLeft(formatTimeRemaining(endTime));
    update();
    const interval = setInterval(update, 1000);
    return () => clearInterval(interval);
  }, [endTime]);

  if (!mounted) return <span className="text-gray-400 font-bold">...</span>;

  return (
    <span className={`text-3xl md:text-4xl font-black uppercase tracking-tight ${
      urgency === "critical" ? "text-hot" : urgency === "soon" ? "text-electric" : "text-black"
    }`}>
      {timeLeft}
    </span>
  );
}

function StartCountdown({ startTime }: { startTime: Date }) {
  const [timeLeft, setTimeLeft] = useState(formatTimeRemaining(startTime));
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    (() => setMounted(true))();
    const update = () => setTimeLeft(formatTimeRemaining(startTime));
    update();
    const interval = setInterval(update, 1000);
    return () => clearInterval(interval);
  }, [startTime]);

  if (!mounted) return <span className="text-gray-400 font-bold">...</span>;

  return (
    <span className="text-3xl md:text-4xl font-black uppercase tracking-tight text-electric">
      {timeLeft}
    </span>
  );
}

function ShareButton() {
  const [copied, setCopied] = useState(false);

  const handleCopy = () => {
    navigator.clipboard.writeText(window.location.href);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleTwitter = () => {
    window.open(`https://twitter.com/intent/tweet?text=Check%20out%20this%20auction!&url=${encodeURIComponent(window.location.href)}`, "_blank");
  };

  return (
    <div className="flex items-center gap-2">
      <button
        onClick={handleCopy}
        className="btn btn-sm font-bold uppercase gap-2"
      >
        {copied ? (
          <>
            <Check className="w-4 h-4" />
            COPIED!
          </>
        ) : (
          <>
            <Share2 className="w-4 h-4" />
            SHARE
          </>
        )}
      </button>
      <button
        onClick={handleTwitter}
        className="btn btn-sm btn-ghost font-bold uppercase"
      >
        <ExternalLink className="w-4 h-4" />
      </button>
    </div>
  );
}

function ImageGallery({ images, title }: { images: string[]; title: string }) {
  const [active, setActive] = useState(0);
  const touchStartX = useRef(0);

  const handleTouchStart = (e: React.TouchEvent) => {
    touchStartX.current = e.touches[0].clientX;
  };

  const handleTouchEnd = (e: React.TouchEvent) => {
    const diff = touchStartX.current - e.changedTouches[0].clientX;
    if (Math.abs(diff) > 50) {
      if (diff > 0 && active < images.length - 1) setActive(active + 1);
      else if (diff < 0 && active > 0) setActive(active - 1);
    }
  };

  return (
    <div className="space-y-3">
      {/* Main Image */}
      <div
        className="relative border-3 border-black overflow-hidden shadow-[8px_8px_0_#0A0A0A]"
        onTouchStart={handleTouchStart}
        onTouchEnd={handleTouchEnd}
      >
        <div className="aspect-4/3 bg-gray-100">
          <img
            src={images[active]}
            alt={`${title} - Image ${active + 1}`}
            className="w-full h-full object-cover"
          />
        </div>

        {/* Navigation Arrows */}
        {images.length > 1 && (
          <>
            <button
              onClick={() => setActive(prev => prev > 0 ? prev - 1 : prev)}
              className="absolute left-3 top-1/2 -translate-y-1/2 w-12 h-12 bg-white border-2 border-black shadow-[3px_3px_0_#0A0A0A] flex items-center justify-center hover:bg-acid transition-colors disabled:opacity-30"
              disabled={active === 0}
            >
              <ChevronLeft className="w-6 h-6" />
            </button>
            <button
              onClick={() => setActive(prev => prev < images.length - 1 ? prev + 1 : prev)}
              className="absolute right-3 top-1/2 -translate-y-1/2 w-12 h-12 bg-white border-2 border-black shadow-[3px_3px_0_#0A0A0A] flex items-center justify-center hover:bg-acid transition-colors disabled:opacity-30"
              disabled={active === images.length - 1}
            >
              <ChevronRight className="w-6 h-6" />
            </button>
          </>
        )}

        {/* Status Badge */}
        <div className="absolute top-4 left-4">
          <span className="badge badge-electric">IMAGE {active + 1} / {images.length}</span>
        </div>
      </div>

      {/* Thumbnails */}
      {images.length > 1 && (
        <div className="flex gap-2 overflow-x-auto pb-2">
          {images.map((img, i) => (
            <button
              key={i}
              onClick={() => setActive(i)}
              className={`w-20 h-20 border-2 shrink-0 overflow-hidden transition-all ${
                i === active ? "border-electric shadow-[3px_3px_0_#0046FF]" : "border-black hover:border-gray-500"
              }`}
            >
              <img src={img} alt={`Thumbnail ${i + 1}`} className="w-full h-full object-cover" />
            </button>
          ))}
        </div>
      )}
    </div>
  );
}

function BidHistory({ bids }: { bids: BidResult[] }) {
  if (bids.length === 0) {
    return (
      <div className="text-center py-12 border-2 border-dashed border-gray-300">
        <p className="text-gray-500 font-bold uppercase text-sm tracking-wide">No bids yet. Be the first!</p>
      </div>
    );
  }

  return (
    <div className="space-y-0">
      {bids.map((bid, index) => (
        <div
          key={bid.bidId}
          className={`flex items-center justify-between py-4 ${
            index === 0 ? "bg-acid/10 border-b-3 border-black" : "border-b border-gray-200"
          }`}
        >
          <div className="flex items-center gap-4">
            <div className="relative">
              <img
                src="https://i.pravatar.cc/150?u=bidder"
                alt="Bidder"
                className="w-12 h-12 rounded-full object-cover border-2 border-black"
              />
              {index === 0 && (
                <div className="absolute -top-2 -right-2 w-6 h-6 bg-electric text-white text-[10px] font-black flex items-center justify-center">
                  1
                </div>
              )}
            </div>
            <div>
              <p className={`font-black text-sm uppercase tracking-tight ${index === 0 ? "text-black" : "text-gray-600"}`}>
                Bidder {bid.bidderId.slice(0, 8)}
              </p>
              <p className="text-xs text-gray-400 font-medium">
                {new Date(bid.createdAt).toLocaleString()}
              </p>
            </div>
          </div>
          <div className="text-right">
            <p className={`font-black text-xl ${index === 0 ? "text-electric" : "text-black"}`}>
              {formatCurrency(bid.amount)}
            </p>
            {index === 0 && <span className="text-[10px] font-black text-electric uppercase">Highest</span>}
          </div>
        </div>
      ))}
    </div>
  );
}

function BidPanel({ listing, bids }: { listing: ParsedListing; bids: BidResult[] }) {
  const [bidAmount, setBidAmount] = useState(Math.ceil(listing.currentPrice / 100) * 100 + 50);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [showSuccess, setShowSuccess] = useState(false);
  const [error, setError] = useState("");
  const { showToast } = useToast();

  const now = new Date();
  const hasStarted = now.getTime() >= listing.startTime.getTime();

  const minBid = listing.currentPrice + 1;
  const suggestedBids = [
    Math.ceil(listing.currentPrice / 100) * 100 + 50,
    Math.ceil(listing.currentPrice / 100) * 100 + 100,
    Math.ceil(listing.currentPrice / 100) * 100 + 250,
    Math.ceil(listing.currentPrice / 100) * 100 + 500,
  ];

  const handleBid = async () => {
    if (bidAmount < minBid) {
      setError(`Minimum bid is ${formatCurrency(minBid)}`);
      return;
    }

    setIsSubmitting(true);
    setError("");

    try {
      await placeBid({ listingId: listing.id, amount: bidAmount, bidType: "MANUAL" });
      setIsSubmitting(false);
      setShowSuccess(true);
      showToast(`Bid of ${formatCurrency(bidAmount)} placed successfully!`, "success");
      setTimeout(() => setShowSuccess(false), 3000);
    } catch (err) {
      setIsSubmitting(false);
      setError(err instanceof Error ? err.message : "Bid failed");
      showToast(err instanceof Error ? err.message : "Bid failed", "error");
    }
  };

  return (
    <div className="border-3 border-black bg-white shadow-[8px_8px_0_#0A0A0A] p-6 sticky top-24">
      {/* Current Bid */}
      <div className="mb-5">
        <p className="text-xs font-black text-gray-500 uppercase tracking-widest mb-1">Current Bid</p>
        <div className="flex items-baseline gap-3">
          <span className="text-5xl font-black text-black">
            {formatCurrency(listing.currentPrice)}
          </span>
          <span className="text-gray-500 font-bold uppercase text-sm">{listing.bidCount} bids</span>
        </div>
        {listing.reservePrice && (
          <p className="text-sm font-bold mt-2">
            {listing.currentPrice >= listing.reservePrice ? (
              <span className="text-electric">✓ Reserve met</span>
            ) : (
              <span className="text-gray-500">Reserve not yet met</span>
            )}
          </p>
        )}
      </div>

      {/* Time */}
      <div className="bg-gray-100 border-2 border-black p-5 mb-5">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-xs font-black text-gray-500 uppercase tracking-widest mb-2">
              {hasStarted ? "Time Remaining" : "Starting In"}
            </p>
            {hasStarted ? (
              <CountdownTimer endTime={listing.endTime} />
            ) : (
              <StartCountdown startTime={listing.startTime} />
            )}
          </div>
          <div className="text-right">
            <p className="text-xs font-black text-gray-500 uppercase tracking-widest mb-1">
              {hasStarted ? "Ends At" : "Starts At"}
            </p>
            <p className="font-black text-black">
              {hasStarted ? formatEndsAt(listing.endTime).timeStr : formatStartsAt(listing.startTime).timeStr}
            </p>
            <p className="text-xs font-black text-gray-500">
              {hasStarted ? formatEndsAt(listing.endTime).dateLabel : formatStartsAt(listing.startTime).dateLabel}
            </p>
          </div>
        </div>
      </div>

      {/* Anti-snipe */}
      <div className="bg-electric/10 border-2 border-electric p-4 mb-5">
        <div className="flex items-start gap-3">
          <Info className="w-5 h-5 text-electric shrink-0 mt-0.5" />
          <p className="text-xs font-bold text-black leading-relaxed">
            <strong>ANTI-SNIPE:</strong> Bid in the last 2 min? Auction extends 2 more minutes.
          </p>
        </div>
      </div>

      {/* Bid Input */}
      <div className="mb-4">
        <label className="text-xs font-black text-gray-500 uppercase tracking-widest mb-2 block">
          Your Bid (min. {formatCurrency(minBid)})
        </label>
        <div className="relative">
          <div className="flex gap-2 justify-center items-center">
            <span className="text-xl font-black text-black">Rp</span>
          <input
            type="number"
            value={bidAmount}
            onChange={(e) => setBidAmount(Number(e.target.value))}
            className="input pl-48 pr-12 text-2xl font-black"
            min={minBid}
            step="1"
          />
          </div>
        </div>
        {error && <p className="text-hot text-sm font-bold mt-2">{error}</p>}
      </div>

      {/* Quick Select */}
      <div className="flex flex-wrap gap-2 mb-5">
        {suggestedBids.map((amount) => (
          <button
            key={amount}
            onClick={() => setBidAmount(amount)}
            className={`px-3 py-2 text-sm font-black border-2 transition-colors ${
              bidAmount === amount
                ? "border-electric bg-electric text-white"
                : "border-black hover:border-electric hover:text-electric"
            }`}
          >
            {formatCurrency(amount)}
          </button>
        ))}
      </div>

      {/* Submit */}
      <button
        onClick={handleBid}
        disabled={isSubmitting}
        className={`btn w-full text-base py-5 font-black uppercase tracking-wide ${
          showSuccess ? "bg-electric text-white border-electric" : "btn-black"
        }`}
      >
        {isSubmitting ? (
          <>
            <Loader2 className="w-5 h-5 animate-spin" />
            Processing...
          </>
        ) : showSuccess ? (
          <>
            <Check className="w-5 h-5" />
            BID PLACED!
          </>
        ) : (
          <>
            <Send className="w-5 h-5" />
            Place Bid — {formatCurrency(bidAmount)}
          </>
        )}
      </button>

      <p className="text-xs text-center text-gray-400 mt-4 font-medium">
        By bidding, you agree to our <Link href="/terms" className="underline font-bold hover:text-black">Terms</Link>
      </p>
    </div>
  );
}

function RelatedListings({ categoryId, categoryName, currentId }: { categoryId: number; categoryName: string; currentId: string }) {
  const [related, setRelated] = useState<ParsedListing[]>([]);

  useEffect(() => {
    getListings({ categoryId, size: 4 }).then(r => {
      setRelated(r.listings.filter(l => l.id !== currentId));
    });
  }, [categoryId, currentId]);

  if (related.length === 0) return null;

  return (
    <section className="border-t-3 border-black pt-16 mt-16">
      <div className="flex items-center justify-between mb-8">
        <h2 className="text-3xl font-black uppercase tracking-tighter">
          More in {categoryName}
        </h2>
        <Link href={`/categories/${categoryName.toLowerCase().replace(/\s+/g, '-')}`} className="btn btn-ghost btn-sm font-bold uppercase">
          View All
        </Link>
      </div>
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
        {related.map((listing, i) => {
          const urgency = getTimeUrgency(listing.endTime);
          return (
            <Link
              key={listing.id}
              href={`/listing/${listing.id}`}
              className="card group block overflow-hidden animate-fade-in-up"
              style={{ animationDelay: `${i * 0.08}s`, animationFillMode: "both" }}
            >
              <div className="relative aspect-4/3 overflow-hidden bg-gray-100">
                <img
                  src={listing.imageUrl}
                  alt={listing.title}
                  className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                />
                <div className="absolute top-3 left-3">
                  <span className="badge badge-black">{listing.category}</span>
                </div>
              </div>
              <div className="p-4">
                <h3 className="font-black text-sm line-clamp-2 leading-snug mb-2 group-hover:text-electric transition-colors uppercase tracking-tight">
                  {listing.title}
                </h3>
                <div className="flex items-baseline gap-2">
                  <span className="text-xl font-black text-black">{formatCurrency(listing.currentPrice)}</span>
                  <span className={`text-xs font-bold ${urgency === "critical" ? "text-hot" : "text-gray-500"}`}>
                    {formatTimeRemaining(listing.endTime)}
                  </span>
                </div>
              </div>
            </Link>
          );
        })}
      </div>
    </section>
  );
}

export default function ListingDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const [listing, setListing] = useState<ParsedListing | null>(null);
  const [bids, setBids] = useState<BidResult[]>([]);
  const [loading, setLoading] = useState(true);
  const { isAuthenticated, isHydrating } = useAuth();

  useEffect(() => {
    let isMounted = true;

    params.then(async ({ id }) => {
      if (isHydrating) {
        return;
      }

      setLoading(true);

      try {
        const [listingData, bidsData] = await Promise.all([
          getListingById(id),
          getBidsForListing(id),
        ]);
        if (!isMounted) {
          return;
        }
        setListing(listingData);
        setBids(bidsData);
      } catch {
        if (isAuthenticated) {
          try {
            const [listingData, bidsData] = await Promise.all([
              getListingByIdOwner(id),
              getBidsForListing(id),
            ]);
            if (!isMounted) {
              return;
            }
            setListing(listingData);
            setBids(bidsData);
          } catch {
            if (isMounted) {
              setListing(null);
            }
          }
        } else if (isMounted) {
          setListing(null);
        }
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    });

    return () => {
      isMounted = false;
    };
  }, [params, isAuthenticated, isHydrating]);

  // SSE stream for real-time updates
  useEffect(() => {
    let es: EventSource | null = null;
    let listingId: string | null = null;

    params.then(({ id }) => {
      if (isHydrating) {
        return;
      }
      listingId = id;
      es = new EventSource(`/api/auctions/${id}/stream`);

      es.addEventListener("message", (e) => {
        try {
          const data = JSON.parse(e.data);
          if (data.type === "price-change") {
            setListing(prev => prev ? {
              ...prev,
              currentPrice: data.currentPrice,
              bidCount: data.bidCount,
            } : prev);
          } else if (data.type === "auction-ended") {
            setListing(prev => prev ? {
              ...prev,
              status: data.result === "WON" ? "sold" : "ended",
            } : prev);
          }
        } catch {}
      });

      es.onerror = () => {
        es?.close();
        // Reconnect after 3 seconds
        setTimeout(() => {
          if (listingId) {
            es = new EventSource(`/api/auctions/${listingId}/stream`);
          }
        }, 3000);
      };
    });

    return () => {
      es?.close();
    };
  }, [params, isHydrating]);

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="w-12 h-12 border-4 border-black border-t-transparent animate-spin"></div>
      </div>
    );
  }

  if (!listing) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center">
        <h1 className="text-4xl font-black text-black mb-6 uppercase tracking-tighter">Not Found</h1>
        <Link href="/" className="btn btn-black">Back to Home</Link>
      </div>
    );
  }

  // Generate mock gallery images (just repeat the same image for demo)
  const galleryImages = [
    listing.imageUrl,
    listing.imageUrl.replace("w=800", "w=801"),
    listing.imageUrl.replace("w=800", "w=802"),
  ];

  return (
    <div className="bg-white">
      {/* Breadcrumb */}
      <div className="max-w-7xl mx-auto px-4 py-4 border-b-2 border-black">
        <nav className="flex items-center gap-2 text-sm font-bold uppercase tracking-wide text-gray-500">
          <Link href="/" className="hover:text-black transition-colors">Home</Link>
          <span>/</span>
          <Link href="/categories" className="hover:text-black transition-colors">Categories</Link>
          <span>/</span>
          <Link href={`/categories/${listing.category.toLowerCase()}`} className="hover:text-black transition-colors">
            {listing.category}
          </Link>
        </nav>
      </div>

      {/* Main Content */}
      <div className="max-w-7xl mx-auto px-4 py-12 pb-24">
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8 lg:gap-12">
          {/* Left Column */}
          <div className="lg:col-span-2 space-y-10">
            {/* Image Gallery */}
            <ImageGallery images={galleryImages} title={listing.title} />

            {/* Title & Share */}
            <div className="flex items-start justify-between gap-4">
              <div>
                <h1 className="text-3xl md:text-4xl font-black text-black mb-3 uppercase tracking-tight leading-tight">
                  {listing.title}
                </h1>
                <div className="flex items-center gap-3">
                  <span className="badge badge-outline">{listing.condition}</span>
                  <span className="text-sm font-bold text-gray-500">Listed {new Date().toLocaleDateString()}</span>
                </div>
              </div>
              <ShareButton />
            </div>

            {/* Description */}
            <div className="border-3 border-black p-6 bg-white shadow-[5px_5px_0_#0A0A0A]">
              <h2 className="text-2xl font-black text-black mb-4 uppercase tracking-tight">
                About This Item
              </h2>
              <p className="text-gray-600 leading-relaxed mb-8">
                {listing.description}
              </p>

              <div className="grid grid-cols-2 gap-4">
                {[
                  { label: "CONDITION", value: listing.condition },
                  { label: "CATEGORY", value: listing.category },
                  { label: "STARTING PRICE", value: formatCurrency(listing.startingPrice) },
                  { label: "SELLER", value: listing.seller.name },
                ].map((item) => (
                  <div key={item.label} className="border-2 border-black p-4 bg-gray-50">
                    <p className="text-[10px] font-black text-gray-500 uppercase tracking-widest mb-1">{item.label}</p>
                    <p className="font-black text-black">{item.value}</p>
                  </div>
                ))}
              </div>
            </div>

            {/* Bid History */}
            <div className="border-3 border-black p-6 bg-white shadow-[5px_5px_0_#0A0A0A]">
              <h2 className="text-2xl font-black text-black mb-6 uppercase tracking-tight">
                Bid History
              </h2>
              <BidHistory bids={bids} />
            </div>

            {/* Related Listings */}
            <RelatedListings categoryId={listing.categoryId} categoryName={listing.category} currentId={listing.id} />
          </div>

          {/* Right Column - Bid Panel */}
          <div className="lg:col-span-1">
            <div className="lg:sticky lg:top-24">
              <BidPanel listing={listing} bids={bids} />
            </div>
          </div>

        </div>

        {/* Seller Card */}
        <div className="mt-8 max-w-7xl mx-auto">
          <div className="border-3 border-black p-6 bg-white shadow-[5px_5px_0_#0A0A0A]">
            <h3 className="text-xs font-black text-gray-500 uppercase tracking-widest mb-4">Seller</h3>
            <div className="flex items-center gap-4 mb-6">
              <img
                src={listing.seller.avatar}
                alt={listing.seller.name}
                className="w-16 h-16 rounded-full object-cover border-3 border-black"
              />
              <div>
                <p className="font-black text-lg text-black uppercase">{listing.seller.name}</p>
                <div className="flex items-center gap-1">
                  <Star className="w-4 h-4 text-electric" />
                  <span className="font-black text-sm">{listing.seller.rating}</span>
                </div>
              </div>
            </div>
            <Link href={`/seller/${listing.seller.id}`} className="btn btn-black w-full font-bold uppercase text-sm">
              View Seller
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
}
