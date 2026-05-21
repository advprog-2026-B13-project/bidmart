"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import { getListings, type ParsedListing } from "@/lib/api/endpoints";
import { formatCurrency, formatTimeRemaining, getTimeUrgency } from "@/lib/utils";
import { Clock } from "lucide-react";

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

export default function EndingSoonPage() {
  const [listings, setListings] = useState<ParsedListing[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getListings().then(({ listings: all }) => {
      const sorted = [...all]
        .filter(l => l.status === "active" || l.status === "ending-soon")
        .sort((a, b) => new Date(a.endTime).getTime() - new Date(b.endTime).getTime());
      setListings(sorted);
      setLoading(false);
    }).catch(() => setLoading(false));
  }, []);

  return (
    <main className="max-w-7xl mx-auto px-4 py-10">
      <div className="mb-8">
        <div className="inline-block bg-hot border-3 border-black px-4 py-1 font-black text-xs uppercase tracking-widest mb-3 text-white">Ending Soon</div>
        <h1 className="text-4xl font-black uppercase tracking-tight">Last Chance Bids</h1>
        <p className="text-gray-500 mt-2 font-medium">These auctions are closing fast — place your bid before time runs out.</p>
      </div>

      {loading ? (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {Array.from({ length: 8 }).map((_, i) => (
            <div key={i} className="border-3 border-black h-72 bg-gray-100 animate-pulse" />
          ))}
        </div>
      ) : listings.length === 0 ? (
        <div className="border-3 border-black p-12 text-center">
          <p className="font-black text-xl">No auctions ending soon.</p>
          <Link href="/" className="mt-4 inline-block btn btn-black font-bold uppercase text-sm">Browse All</Link>
        </div>
      ) : (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {listings.map((listing, i) => {
            const imageUrl = typeof listing.imageUrl === "string" && listing.imageUrl.trim().length > 0 ? listing.imageUrl : null;
            const urgency = getTimeUrgency(listing.endTime);
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
                  {urgency === "critical" && (
                    <div className="absolute top-2 left-2 bg-hot border-2 border-black px-2 py-0.5 text-xs font-black text-white uppercase">Ending Now</div>
                  )}
                </div>
                <div className="p-3 border-t-3 border-black">
                  <p className="font-black text-sm uppercase truncate">{listing.title}</p>
                  <div className="flex justify-between items-center mt-2">
                    <span className="font-black text-base">{formatCurrency(listing.currentPrice)}</span>
                    <span className="flex items-center gap-1 text-xs"><Clock className="w-3 h-3" /><CountdownTimer endTime={listing.endTime} /></span>
                  </div>
                </div>
              </Link>
            );
          })}
        </div>
      )}
    </main>
  );
}
