"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import { use } from "react";
import { getListings, type ParsedListing } from "@/lib/api/endpoints";
import { formatCurrency, formatTimeRemaining, getTimeUrgency } from "@/lib/utils";
import { Clock, Package } from "lucide-react";

function CountdownTimer({ endTime }: { endTime: Date }) {
  const [timeLeft, setTimeLeft] = useState(formatTimeRemaining(endTime));
  useEffect(() => {
    const update = () => setTimeLeft(formatTimeRemaining(endTime));
    update();
    const interval = setInterval(update, 1000);
    return () => clearInterval(interval);
  }, [endTime]);
  const urgency = getTimeUrgency(endTime);
  return (
    <span className={`font-black text-sm ${urgency === "critical" ? "text-hot" : urgency === "soon" ? "text-electric" : "text-gray-600"}`}>
      {timeLeft}
    </span>
  );
}

export default function SellerPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const [listings, setListings] = useState<ParsedListing[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getListings().then(({ listings: all }) => {
      setListings(all.filter((l: ParsedListing) => l.seller?.id === id));
      setLoading(false);
    }).catch(() => setLoading(false));
  }, [id]);

  const active = listings.filter(l => l.status === "active" || l.status === "ending-soon");

  return (
    <main className="max-w-5xl mx-auto px-4 py-10">
      <div className="border-3 border-black p-6 shadow-[4px_4px_0_#0A0A0A] mb-8 flex items-center gap-5">
        <div className="w-16 h-16 bg-acid border-3 border-black flex items-center justify-center shrink-0">
          <span className="text-3xl font-black">S</span>
        </div>
        <div>
          <p className="text-xs font-black uppercase tracking-widest text-gray-500 mb-1">Seller</p>
          <h1 className="text-2xl font-black uppercase tracking-tight">Seller Profile</h1>
          <p className="text-xs text-gray-400 font-mono mt-1 break-all">{id}</p>
        </div>
      </div>

      <div className="mb-6">
        <h2 className="font-black text-lg uppercase tracking-wide mb-4 flex items-center gap-2">
          <Package className="w-5 h-5" /> Active Listings
          {!loading && <span className="text-sm font-bold text-gray-500">({active.length})</span>}
        </h2>

        {loading ? (
          <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
            {Array.from({ length: 3 }).map((_, i) => <div key={i} className="border-3 border-black h-60 bg-gray-100 animate-pulse" />)}
          </div>
        ) : active.length === 0 ? (
          <div className="border-3 border-black p-8 text-center">
            <p className="font-black text-base uppercase">No active listings from this seller.</p>
            <Link href="/" className="mt-3 inline-block btn btn-black font-bold uppercase text-sm">Browse All</Link>
          </div>
        ) : (
          <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
            {active.map((listing, i) => {
              const imageUrl = typeof listing.imageUrl === "string" && listing.imageUrl.trim().length > 0 ? listing.imageUrl : null;
              return (
                <Link key={listing.id} href={`/listing/${listing.id}`}
                  className="card group block overflow-hidden animate-fade-in-up"
                  style={{ animationDelay: `${i * 0.05}s`, animationFillMode: "both" }}>
                  <div className="relative aspect-card overflow-hidden bg-gray-100">
                    {imageUrl ? (
                      <img src={imageUrl} alt={listing.title} className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300" />
                    ) : (
                      <div className="w-full h-full bg-gray-100 flex items-center justify-center text-xs font-black text-gray-400">NO IMAGE</div>
                    )}
                  </div>
                  <div className="p-3 border-t-3 border-black">
                    <p className="font-black text-sm uppercase truncate">{listing.title}</p>
                    <div className="flex justify-between items-center mt-2">
                      <span className="font-black text-base">{formatCurrency(listing.currentPrice)}</span>
                      <span className="flex items-center gap-1 text-xs text-gray-500"><Clock className="w-3 h-3" /><CountdownTimer endTime={listing.endTime} /></span>
                    </div>
                  </div>
                </Link>
              );
            })}
          </div>
        )}
      </div>
    </main>
  );
}
