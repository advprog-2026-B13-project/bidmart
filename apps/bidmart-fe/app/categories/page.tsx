"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import { getCategories, getSubCategories } from "@/lib/api/endpoints";

export default function CategoriesPage() {
  const [categories, setCategories] = useState<{ slug: string; name: string; id: number }[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    getCategories().then(cats => {
      setCategories(cats);
      setLoading(false);
    }).catch(() => setLoading(false));
  }, []);

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="w-12 h-12 border-4 border-black border-t-transparent animate-spin"></div>
      </div>
    );
  }

  const featured = categories.slice(0, 3);

  return (
    <div>
      {/* Hero */}
      <section className="relative overflow-hidden border-b-3 border-black bg-white">
        <div className="absolute inset-0 grid-bg opacity-100"></div>
        <div className="relative max-w-7xl mx-auto px-4 py-16 md:py-24">
          <div className="max-w-2xl animate-fade-in-up">
            <div className="inline-block bg-electric text-white text-xs font-black uppercase tracking-widest px-4 py-2 mb-6">
              BROWSE BY CATEGORY
            </div>
            <h1 className="text-5xl md:text-7xl font-black mb-6 leading-[0.9] tracking-tighter uppercase">
              Find Your
              <br />
              <span className="text-electric">Next Obsession</span>
            </h1>
            <p className="text-xl text-gray-600 max-w-lg leading-relaxed">
              Ten curated categories. Thousands of verified listings. New items added daily.
            </p>
          </div>
        </div>
      </section>

      {/* Featured Categories - Asymmetric hero grid */}
      <section className="max-w-7xl mx-auto px-4 py-12">
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {featured.map((cat, i) => (
            <Link
              key={cat.slug}
              href={`/categories/${cat.slug}`}
              className={`group relative overflow-hidden border-3 border-black animate-fade-in-up ${
                i === 0 ? "md:row-span-2 md:aspect-auto aspect-4/3" : "aspect-4/3"
              }`}
              style={{ animationDelay: `${i * 0.1}s`, animationFillMode: "both" }}
            >
              {/* Cover Image Background */}
              <div className="absolute inset-0">
                <img
                  src="https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=800&q=80"
                  alt={cat.name}
                  className="w-full h-full object-cover"
                />
                <div className="absolute inset-0 bg-linear-to-t from-black/90 via-black/50 to-transparent" />
              </div>

              {/* Content */}
              <div className="relative z-10 h-full flex flex-col justify-end p-6 md:p-8">
                <div>
                  <h2 className={`font-black text-white uppercase tracking-tight mb-2 ${
                    i === 0 ? "text-4xl md:text-5xl" : "text-2xl"
                  }`}>
                    {cat.name}
                  </h2>
                  <p className="text-white/70 text-sm font-medium mb-4 max-w-xs">Browse {cat.name} listings</p>
                  <div className="flex items-center gap-2">
                    <span className="bg-white text-black text-xs font-black px-3 py-1">
                      EXPLORE
                    </span>
                    <svg className="w-5 h-5 text-white transform group-hover:translate-x-2 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M9 5l7 7-7 7" />
                    </svg>
                  </div>
                </div>
              </div>

              {/* Hover overlay */}
              <div className="absolute inset-0 bg-black/0 group-hover:bg-black/10 transition-colors" />
            </Link>
          ))}
        </div>
      </section>

      {/* All Categories - Grid */}
      <section className="bg-gray-100 border-y-3 border-black py-16">
        <div className="max-w-7xl mx-auto px-4">
          <div className="flex items-center justify-between mb-10">
            <h2 className="text-4xl md:text-5xl font-black uppercase tracking-tighter">
              All Categories
            </h2>
            <span className="text-sm font-bold text-gray-500 uppercase tracking-wide">{categories.length} Total</span>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
            {categories.map((cat, i) => (
              <Link
                key={cat.slug}
                href={`/categories/${cat.slug}`}
                className="card group overflow-hidden animate-fade-in-up"
                style={{ animationDelay: `${i * 0.05}s`, animationFillMode: "both" }}
              >
                <div className="relative aspect-4/3 overflow-hidden">
                  <img
                    src="https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=800&q=80"
                    alt={cat.name}
                    className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                  />
                  <div className="absolute inset-0 bg-linear-to-t from-black/80 via-transparent to-transparent" />
                  <div className="absolute bottom-0 left-0 right-0 p-4">
                    <h3 className="font-black text-lg text-white uppercase tracking-tight mb-1">
                      {cat.name}
                    </h3>
                    <p className="text-white/70 text-xs font-medium uppercase">Browse listings</p>
                  </div>
                </div>
              </Link>
            ))}
          </div>
        </div>
      </section>

      {/* Popular Listings Across Categories */}
      <section className="max-w-7xl mx-auto px-4 py-16">
        <div className="flex items-center justify-between mb-10">
          <h2 className="text-4xl md:text-5xl font-black uppercase tracking-tighter">
            Trending Now
          </h2>
          <Link href="/" className="btn btn-ghost btn-sm font-bold uppercase">
            View All
          </Link>
        </div>

        {/* Ticker strip */}
        <div className="marquee-strip mb-10">
          <div className="marquee-strip-inner">
            {["HOTTEST BIDS", "ENDING SOON", "NEW LISTINGS", "VERIFIED SELLERS", "FREE SHIPPING", "BUYER PROTECTED", "ANTI-SNIPE"].map((item, i) => (
              <span key={i} className="marquee-strip-item">{item}</span>
            ))}
            {[...["HOTTEST BIDS", "ENDING SOON", "NEW LISTINGS", "VERIFIED SELLERS", "FREE SHIPPING", "BUYER PROTECTED", "ANTI-SNIPE"]].map((item, i) => (
              <span key={`dup-${i}`} className="marquee-strip-item">{item}</span>
            ))}
          </div>
        </div>
      </section>
    </div>
  );
}
