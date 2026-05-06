"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import { getListings, getCategories, type ParsedListing } from "@/lib/api/endpoints";
import { formatCurrency, formatTimeRemaining, getTimeUrgency } from "@/lib/utils";

function CountdownTimer({ endTime }: { endTime: Date }) {
  const [timeLeft, setTimeLeft] = useState(formatTimeRemaining(endTime));
  const urgency = getTimeUrgency(endTime);

  useEffect(() => {
    const interval = setInterval(() => {
      setTimeLeft(formatTimeRemaining(endTime));
    }, 60000);
    return () => clearInterval(interval);
  }, [endTime]);

  return (
    <span className={`font-black text-sm ${
      urgency === "critical" ? "text-hot" : urgency === "soon" ? "text-electric" : "text-gray-600"
    }`}>
      {timeLeft}
    </span>
  );
}

function ListingCard({ listing, index }: { listing: ParsedListing; index: number }) {
  const urgency = getTimeUrgency(listing.endTime);

  return (
    <Link
      href={`/listing/${listing.id}`}
      className="card group block overflow-hidden animate-fade-in-up"
      style={{ animationDelay: `${index * 0.08}s`, animationFillMode: "both" }}
    >
      {/* Image */}
      <div className="relative aspect-card overflow-hidden bg-gray-100">
        <img
          src={listing.imageUrl}
          alt={listing.title}
          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
        />
        {/* Status Badge */}
        <div className="absolute top-3 left-3">
          {listing.status === "ending-soon" ? (
            <span className="badge badge-hot">ENDING SOON</span>
          ) : (
            <span className="badge badge-black">{listing.category}</span>
          )}
        </div>
        {/* Timer */}
        <div className="absolute top-3 right-3">
          <div className="flex items-center gap-1.5 bg-white border-2 border-black px-3 py-1.5 shadow-[2px_2px_0_#0A0A0A]">
            <svg className={`w-4 h-4 ${urgency === "critical" ? "text-hot" : "text-gray-600"}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <CountdownTimer endTime={listing.endTime} />
          </div>
        </div>
      </div>

      {/* Content */}
      <div className="p-4">
        <h3 className="font-black text-base line-clamp-2 leading-snug mb-3 group-hover:text-electric transition-colors uppercase tracking-tight">
          {listing.title}
        </h3>

        {/* Price */}
        <div className="flex items-baseline gap-2 mb-3">
          <span className="text-2xl font-black text-black">
            {formatCurrency(listing.currentPrice)}
          </span>
          {listing.startingPrice !== listing.currentPrice && (
            <span className="text-xs font-bold text-electric uppercase">
              +{Math.round((listing.currentPrice / listing.startingPrice - 1) * 100)}%
            </span>
          )}
        </div>

        {/* Meta */}
        <div className="flex items-center justify-between pt-3 border-t-2 border-black">
          <div className="flex items-center gap-2">
            <img
              src={listing.seller.avatar}
              alt={listing.seller.name}
              className="w-6 h-6 rounded-full object-cover border border-black"
            />
            <span className="text-xs font-bold text-gray-500 uppercase">{listing.seller.name.split(' ')[0]}</span>
          </div>
          <div className="flex items-center gap-1">
            <svg className="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M15 15l-2 5L9 9l11 4-5 2zm0 0l5 5" />
            </svg>
            <span className="text-xs font-black text-gray-600">{listing.bidCount}</span>
          </div>
        </div>
      </div>
    </Link>
  );
}

function FeaturedAuction({ listing }: { listing: ParsedListing | null }) {
  if (!listing) return null;

  return (
    <div className="relative overflow-hidden border-3 border-black bg-black">
      <div className="absolute inset-0">
        <img
          src={listing.imageUrl}
          alt={listing.title}
          className="w-full h-full object-cover opacity-30"
        />
        <div className="absolute inset-0 bg-linear-to-r from-black via-black/90 to-transparent" />
      </div>

      <div className="relative z-10 p-8 md:p-12 lg:p-16">
        <div className="max-w-2xl">
          <span className="badge badge-acid mb-6">FEATURED AUCTION</span>

          <h2 className="text-3xl md:text-5xl lg:text-6xl font-black text-white mb-6 leading-[0.95] tracking-tighter uppercase">
            {listing.title}
          </h2>

          <p className="text-gray-400 text-lg mb-8 max-w-lg leading-relaxed">
            {listing.description.slice(0, 120)}...
          </p>

          <div className="flex flex-wrap items-center gap-6 mb-8">
            <div>
              <p className="text-xs text-gray-500 uppercase tracking-widest font-bold mb-1">Current Bid</p>
              <p className="text-5xl font-black text-acid">
                {formatCurrency(listing.currentPrice)}
              </p>
            </div>
            <div>
              <p className="text-xs text-gray-500 uppercase tracking-widest font-bold mb-1">Time Left</p>
              <div className="flex items-center gap-2">
                <svg className="w-6 h-6 text-acid" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
                </svg>
                <CountdownTimer endTime={listing.endTime} />
              </div>
            </div>
            <div>
              <p className="text-xs text-gray-500 uppercase tracking-widest font-bold mb-1">Bids</p>
              <p className="text-3xl font-black text-white">{listing.bidCount}</p>
            </div>
          </div>

          <div className="flex flex-wrap gap-3">
            <Link href={`/listing/${listing.id}`} className="btn btn-acid btn-lg font-black">
              Place Bid
            </Link>
            <Link href={`/listing/${listing.id}`} className="btn btn-lg font-bold bg-white text-black border-white hover:bg-gray-100">
              View Details
            </Link>
          </div>
        </div>
      </div>

      {/* Decorative grid */}
      <div className="absolute bottom-0 right-0 w-64 h-64 opacity-5">
        <svg viewBox="0 0 100 100" className="w-full h-full">
          <pattern id="grid" width="10" height="10" patternUnits="userSpaceOnUse">
            <path d="M 10 0 L 0 0 0 10" fill="none" stroke="white" strokeWidth="0.5"/>
          </pattern>
          <rect width="100" height="100" fill="url(#grid)" />
        </svg>
      </div>
    </div>
  );
}

function CategoryCard({ category, index }: { category: { slug: string; name: string; coverImage: string; count?: number }; index: number }) {
  return (
    <Link
      href={`/categories/${category.slug}`}
      className="card overflow-hidden group animate-fade-in-up"
      style={{ animationDelay: `${index * 0.05}s`, animationFillMode: "both" }}
    >
      <div className="relative aspect-4/3 overflow-hidden">
        <img
          src={category.coverImage}
          alt={category.name}
          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
        />
        <div className="absolute inset-0 bg-linear-to-t from-black/80 via-transparent to-transparent" />
        <div className="absolute bottom-0 left-0 right-0 p-4">
          <h3 className="font-black text-base text-white uppercase tracking-tight mb-1">
            {category.name}
          </h3>
          <p className="text-white/70 text-xs font-medium uppercase">{category.count} items</p>
        </div>
      </div>
    </Link>
  );
}

export default function HomePage() {
  const [listings, setListings] = useState<ParsedListing[]>([]);
  const [categories, setCategories] = useState<{ slug: string; name: string; coverImage: string }[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      getListings({ size: 100 }),
      getCategories(),
    ]).then(([listingsResult, cats]) => {
      setListings(listingsResult.listings);
      setCategories(cats);
      setLoading(false);
    }).catch(() => setLoading(false));
  }, []);

  const featured = listings[0] || null;
  const endingSoonListings = listings.filter(l => l.status === "active" || l.status === "ending-soon").slice(0, 4);

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="w-12 h-12 border-4 border-black border-t-transparent animate-spin"></div>
      </div>
    );
  }

  return (
    <div>
      {/* Hero */}
      <section className="relative overflow-hidden bg-white">
        <div className="absolute inset-0 grid-bg opacity-100"></div>

        <div className="relative max-w-7xl mx-auto px-4 py-20 md:py-32">
          <div className="max-w-3xl animate-fade-in-up">
            <div className="inline-block bg-electric text-white text-xs font-black uppercase tracking-widest px-4 py-2 mb-6">
              TRUSTED BY 50,000+ BIDDERS
            </div>
            <h1 className="text-5xl md:text-7xl lg:text-8xl font-black mb-8 leading-[0.9] tracking-tighter uppercase">
              Where Bold
              <br />
              <span className="text-electric">Bidders</span> Win
            </h1>
            <p className="text-xl text-gray-600 mb-10 max-w-lg leading-relaxed">
              Discover unique items from verified sellers. Every auction protected. Every bid counts.
            </p>
            <div className="flex flex-wrap gap-3">
              <Link href="#browse" className="btn btn-black btn-lg font-black">
                Start Bidding
              </Link>
              <Link href="/how-it-works" className="btn btn-lg font-bold bg-white text-black border-3 border-black hover:bg-gray-100">
                How It Works
              </Link>
            </div>
          </div>

          {/* Stats */}
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4 mt-20 animate-fade-in-up stagger-4" style={{ animationFillMode: "both" }}>
            {[
              { value: "$12M+", label: "BID VOLUME" },
              { value: "50K+", label: "ACTIVE BIDDERS" },
              { value: "99.2%", label: "SATISFACTION" },
              { value: "24H", label: "AVG DISPATCH" },
            ].map((stat, i) => (
              <div key={i} className="border-3 border-black p-6 bg-white shadow-[5px_5px_0_#0A0A0A]">
                <p className="text-3xl md:text-4xl font-black text-electric">{stat.value}</p>
                <p className="text-xs font-bold text-gray-500 uppercase tracking-widest mt-1">{stat.label}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* Featured Auction */}
      <section className="max-w-7xl mx-auto px-4 py-16">
        <FeaturedAuction listing={featured} />
      </section>

      {/* Categories */}
      <section className="bg-gray-100 py-16 border-y-3 border-black">
        <div className="max-w-7xl mx-auto px-4">
          <div className="flex items-center justify-between mb-10">
            <div>
              <h2 className="text-4xl md:text-5xl font-black uppercase tracking-tighter">
                Categories
              </h2>
              <p className="text-gray-500 font-bold mt-2 uppercase text-sm tracking-wide">Find exactly what you want</p>
            </div>
            <Link href="/categories" className="btn btn-ghost btn-sm font-bold uppercase">
              View All
            </Link>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
            {categories.slice(0, 5).map((cat, i) => (
              <CategoryCard key={cat.name} category={cat} index={i} />
            ))}
          </div>
        </div>
      </section>

      {/* Ending Soon */}
      <section id="browse" className="max-w-7xl mx-auto px-4 py-16">
        <div className="flex items-center justify-between mb-10">
          <div>
            <h2 className="text-4xl md:text-5xl font-black uppercase tracking-tighter">
              Ending Soon
            </h2>
            <p className="text-gray-500 font-bold mt-2 uppercase text-sm tracking-wide">Don&apos;t miss out</p>
          </div>
          <Link href="/ending-soon" className="btn btn-ghost btn-sm font-bold uppercase">
            View All
          </Link>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
          {endingSoonListings.map((listing, i) => (
            <ListingCard key={listing.id} listing={listing} index={i} />
          ))}
        </div>
      </section>

      {/* All Listings */}
      <section className="bg-gray-100 py-16 border-y-3 border-black">
        <div className="max-w-7xl mx-auto px-4">
          <div className="flex items-center justify-between mb-10">
            <div>
              <h2 className="text-4xl md:text-5xl font-black uppercase tracking-tighter">
                All Auctions
              </h2>
              <p className="text-gray-500 font-bold mt-2 uppercase text-sm tracking-wide">{listings.length} items waiting</p>
            </div>
            <div className="flex items-center gap-3">
              <select className="input py-2 px-3 text-sm font-bold w-auto bg-white">
                <option>ENDING SOON</option>
                <option>PRICE: LOW → HIGH</option>
                <option>PRICE: HIGH → LOW</option>
                <option>MOST BIDS</option>
              </select>
            </div>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
            {listings.map((listing, i) => (
              <ListingCard key={listing.id} listing={listing} index={i} />
            ))}
          </div>
        </div>
      </section>

      {/* Trust Banner */}
      <section className="max-w-7xl mx-auto px-4 py-16">
        <div className="bg-electric text-white p-8 md:p-12 border-3 border-black shadow-[8px_8px_0_#0A0A0A]">
          <h2 className="text-4xl md:text-5xl font-black mb-4 uppercase tracking-tighter">
            Bid with Confidence
          </h2>
          <p className="text-white/80 text-lg max-w-xl mb-10 leading-relaxed">
            Every transaction protected by our comprehensive buyer guarantee. If it&apos;s not right, we refund.
          </p>
          <div className="flex flex-wrap gap-6">
            {[
              { label: "BUYER PROTECTION" },
              { label: "VERIFIED SELLERS" },
              { label: "SECURE PAYMENTS" },
              { label: "INSURED SHIPPING" },
            ].map((item, i) => (
              <div key={i} className="flex items-center gap-2">
                <span className="w-3 h-3 bg-acid"></span>
                <span className="font-black text-sm uppercase tracking-wide">{item.label}</span>
              </div>
            ))}
          </div>
        </div>
      </section>
    </div>
  );
}
