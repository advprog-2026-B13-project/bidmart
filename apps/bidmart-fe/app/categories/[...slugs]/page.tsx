"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import { mockListings, categoryTree, findCategoryByPath, formatCurrency, formatTimeRemaining, getTimeUrgency, type Category, type Listing } from "@/lib/mock-data";

function ListingCard({ listing, index }: { listing: Listing; index: number }) {
  const urgency = getTimeUrgency(listing.endTime);

  return (
    <Link
      href={`/listing/${listing.id}`}
      className="card group block overflow-hidden animate-fade-in-up"
      style={{ animationDelay: `${index * 0.06}s`, animationFillMode: "both" }}
    >
      <div className="relative aspect-4/3 overflow-hidden bg-gray-100">
        <img
          src={listing.imageUrl}
          alt={listing.title}
          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
        />
        <div className="absolute top-3 left-3">
          {listing.status === "ending-soon" ? (
            <span className="badge badge-hot">ENDING SOON</span>
          ) : (
            <span className="badge badge-black">{listing.category}</span>
          )}
        </div>
        <div className="absolute top-3 right-3">
          <div className="flex items-center gap-1.5 bg-white border-2 border-black px-3 py-1.5 shadow-[2px_2px_0_#0A0A0A]">
            <svg className={`w-4 h-4 ${urgency === "critical" ? "text-hot" : "text-gray-600"}`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            <span className={`font-black text-sm ${urgency === "critical" ? "text-hot" : urgency === "soon" ? "text-electric" : "text-gray-600"}`}>
              {formatTimeRemaining(listing.endTime)}
            </span>
          </div>
        </div>
      </div>

      <div className="p-4">
        <h3 className="font-black text-sm line-clamp-2 leading-snug mb-3 group-hover:text-electric transition-colors uppercase tracking-tight">
          {listing.title}
        </h3>

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

export default function NestedCategoryPage({ params }: { params: Promise<{ slugs: string[] }> }) {
  const [slugs, setSlugs] = useState<string[]>([]);
  const [sortBy, setSortBy] = useState("ending-soon");
  const [priceMin, setPriceMin] = useState("");
  const [priceMax, setPriceMax] = useState("");
  const [status, setStatus] = useState<string>("all");
  const [showFilters, setShowFilters] = useState(false);

  useEffect(() => {
    params.then(({ slugs }) => setSlugs(slugs || []));
  }, [params]);

  const { category, breadcrumb } = findCategoryByPath(slugs);

  if (slugs.length === 0) {
    // Root categories page
    return <RootCategoriesPage />;
  }

  if (!category) {
    return (
      <div className="min-h-screen flex flex-col items-center justify-center">
        <h1 className="text-5xl font-black text-black mb-4 uppercase tracking-tighter">404</h1>
        <p className="text-gray-500 font-bold mb-8">Category not found</p>
        <Link href="/categories" className="btn btn-black">Browse Categories</Link>
      </div>
    );
  }

  // Get listings for this category (including subcategory listings)
  let listings = mockListings.filter(l => {
    const catPath = categoryTree.find(c => c.slug === slugs[0]);
    if (!catPath) return false;

    // Check if listing matches this category or any subcategory
    const checkCategory = (cat: Category): boolean => {
      if (cat.slug === category.slug) return true;
      if (cat.children) {
        return cat.children.some(child => checkCategory(child));
      }
      return false;
    };

    return checkCategory(catPath);
  });

  // Apply filters
  if (status === "ending-soon") {
    listings = listings.filter(l => l.status === "ending-soon");
  } else if (status === "active") {
    listings = listings.filter(l => l.status === "active");
  }

  if (priceMin) {
    listings = listings.filter(l => l.currentPrice >= Number(priceMin));
  }
  if (priceMax) {
    listings = listings.filter(l => l.currentPrice <= Number(priceMax));
  }

  // Sort
  if (sortBy === "ending-soon") {
    listings.sort((a, b) => new Date(a.endTime).getTime() - new Date(b.endTime).getTime());
  } else if (sortBy === "price-low") {
    listings.sort((a, b) => a.currentPrice - b.currentPrice);
  } else if (sortBy === "price-high") {
    listings.sort((a, b) => b.currentPrice - a.currentPrice);
  } else if (sortBy === "most-bids") {
    listings.sort((a, b) => b.bidCount - a.bidCount);
  }

  const hasChildren = category.children && category.children.length > 0;

  return (
    <div>
      {/* Hero Banner */}
      <section className="relative overflow-hidden bg-black border-b-3 border-black">
        <div className="absolute inset-0">
          <img
            src={category.coverImage}
            alt={category.name}
            className="w-full h-full object-cover opacity-40"
          />
          <div className="absolute inset-0 bg-gradient-to-r from-black via-black/90 to-transparent" />
        </div>
        <div className="relative max-w-7xl mx-auto px-4 py-12">
          {/* Breadcrumb */}
          <nav className="flex items-center gap-2 text-sm font-bold uppercase tracking-wide text-gray-400 mb-6">
            <Link href="/categories" className="hover:text-white transition-colors">
              All Categories
            </Link>
            {breadcrumb.map((crumb, i) => (
              <span key={crumb.slug} className="flex items-center gap-2">
                <span>/</span>
                {i === breadcrumb.length - 1 ? (
                  <span className="text-white">{crumb.name}</span>
                ) : (
                  <Link href={`/categories/${breadcrumb.slice(0, i + 1).map(c => c.slug).join('/')}`} className="hover:text-white transition-colors">
                    {crumb.name}
                  </Link>
                )}
              </span>
            ))}
          </nav>

          <div>
            <h1 className="text-5xl md:text-6xl font-black text-white mb-3 uppercase tracking-tighter">
              {category.name}
            </h1>
            <p className="text-gray-400 text-lg mb-4">{category.description}</p>
            <span className="bg-acid text-black text-xs font-black px-4 py-2 uppercase tracking-widest">
              {category.count} LISTINGS
            </span>
          </div>
        </div>
      </section>

      {/* Subcategories Grid (if has children) */}
      {hasChildren && (
        <section className="max-w-7xl mx-auto px-4 py-12">
          <h2 className="text-2xl font-black uppercase tracking-tight mb-6">
            Subcategories
          </h2>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            {category.children!.map((child) => (
              <Link
                key={child.slug}
                href={`/categories/${[...slugs, child.slug].join('/')}`}
                className="card group overflow-hidden"
              >
                <div className="relative aspect-4/3 overflow-hidden">
                  <img
                    src={child.coverImage}
                    alt={child.name}
                    className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                  />
                  <div className="absolute inset-0 bg-gradient-to-t from-black/80 via-transparent to-transparent" />
                  <div className="absolute bottom-0 left-0 right-0 p-4">
                    <h3 className="font-black text-base text-white uppercase tracking-tight mb-1">
                      {child.name}
                    </h3>
                    <p className="text-white/70 text-xs font-medium uppercase">{child.count} items</p>
                  </div>
                </div>
              </Link>
            ))}
          </div>
        </section>
      )}

      {/* Filters Bar */}
      <section className="sticky top-16 z-40 bg-white border-b-3 border-black">
        <div className="max-w-7xl mx-auto px-4">
          <div className="flex items-center justify-between py-4 gap-4">
            <div className="flex items-center gap-3 flex-1">
              {/* Mobile filter toggle */}
              <button
                onClick={() => setShowFilters(!showFilters)}
                className="lg:hidden btn btn-sm font-bold uppercase gap-2"
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M3 4a1 1 0 011-1h16a1 1 0 011 1v2.586a1 1 0 01-.293.707l-6.414 6.414a1 1 0 00-.293.707V17l-4 4v-6.586a1 1 0 00-.293-.707L3.293 7.293A1 1 0 013 6.586V4z" />
                </svg>
                Filters
              </button>

              {/* Sort */}
              <div className="flex items-center gap-2">
                <span className="text-xs font-bold text-gray-500 uppercase tracking-wide hidden md:block">Sort:</span>
                <select
                  value={sortBy}
                  onChange={(e) => setSortBy(e.target.value)}
                  className="input py-2 px-4 text-sm font-bold w-auto min-w-[180px]"
                >
                  <option value="ending-soon">ENDING SOON</option>
                  <option value="price-low">PRICE: LOW → HIGH</option>
                  <option value="price-high">PRICE: HIGH → LOW</option>
                  <option value="most-bids">MOST BIDS</option>
                </select>
              </div>

              {/* Status filter */}
              <select
                value={status}
                onChange={(e) => setStatus(e.target.value)}
                className="input py-2 px-4 text-sm font-bold w-auto min-w-[160px] hidden md:block"
              >
                <option value="all">ALL STATUS</option>
                <option value="active">ACTIVE ONLY</option>
                <option value="ending-soon">ENDING SOON</option>
              </select>

              {/* Results count */}
              <div className="hidden lg:block ml-4">
                <span className="text-sm font-bold text-gray-500">
                  <span className="text-black font-black">{listings.length}</span> items
                </span>
              </div>
            </div>
          </div>

          {/* Expanded Filters */}
          {showFilters && (
            <div className="lg:hidden border-t-2 border-black py-4 space-y-4 animate-fade-in">
              {/* Price Range */}
              <div>
                <label className="text-xs font-black text-gray-500 uppercase tracking-widest block mb-2">Price Range</label>
                <div className="flex items-center gap-2">
                  <input
                    type="number"
                    placeholder="Min"
                    value={priceMin}
                    onChange={(e) => setPriceMin(e.target.value)}
                    className="input py-2 px-3 text-sm w-28"
                  />
                  <span className="font-bold text-gray-400">—</span>
                  <input
                    type="number"
                    placeholder="Max"
                    value={priceMax}
                    onChange={(e) => setPriceMax(e.target.value)}
                    className="input py-2 px-3 text-sm w-28"
                  />
                </div>
              </div>

              {/* Apply */}
              <div className="flex gap-2">
                <button
                  onClick={() => { setPriceMin(""); setPriceMax(""); }}
                  className="btn btn-sm font-bold uppercase"
                >
                  Clear
                </button>
                <button
                  onClick={() => setShowFilters(false)}
                  className="btn btn-sm btn-black font-bold uppercase"
                >
                  Apply
                </button>
              </div>
            </div>
          )}
        </div>
      </section>

      {/* Listings Grid */}
      <section className="max-w-7xl mx-auto px-4 py-12">
        {listings.length === 0 ? (
          <div className="text-center py-24 border-3 border-dashed border-gray-300">
            <div className="text-6xl mb-6">📦</div>
            <h2 className="text-3xl font-black text-black mb-3 uppercase tracking-tight">No Listings Found</h2>
            <p className="text-gray-500 font-medium mb-8">Try adjusting your filters or check back later</p>
            <button
              onClick={() => { setPriceMin(""); setPriceMax(""); setStatus("all"); }}
              className="btn btn-black font-bold uppercase"
            >
              Clear Filters
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
            {listings.map((listing, i) => (
              <ListingCard key={listing.id} listing={listing} index={i} />
            ))}
          </div>
        )}
      </section>

      {/* Related Categories */}
      {listings.length > 0 && (
        <section className="bg-gray-100 border-t-3 border-black py-16">
          <div className="max-w-7xl mx-auto px-4">
            <h2 className="text-3xl font-black uppercase tracking-tighter mb-8">
              Explore Other Categories
            </h2>
            <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-5 gap-3">
              {categoryTree.filter(c => c.slug !== slugs[0]).slice(0, 5).map(cat => (
                <Link
                  key={cat.slug}
                  href={`/categories/${cat.slug}`}
                  className="card p-4 flex items-center gap-3 group"
                >
                  <img
                    src={cat.coverImage}
                    alt={cat.name}
                    className="w-12 h-12 object-cover border-2 border-black shrink-0"
                  />
                  <span className="font-black text-sm uppercase tracking-tight group-hover:text-electric transition-colors">
                    {cat.name}
                  </span>
                </Link>
              ))}
            </div>
          </div>
        </section>
      )}
    </div>
  );
}

function RootCategoriesPage() {
  const featured = categoryTree.slice(0, 3);

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
              <div className="absolute inset-0">
                <img
                  src={cat.coverImage}
                  alt={cat.name}
                  className="w-full h-full object-cover"
                />
                <div className="absolute inset-0 bg-linear-to-t from-black/90 via-black/50 to-transparent" />
              </div>

              <div className="relative z-10 h-full flex flex-col justify-end p-6 md:p-8">
                <div>
                  <h2 className={`font-black text-white uppercase tracking-tight mb-2 ${
                    i === 0 ? "text-4xl md:text-5xl" : "text-2xl"
                  }`}>
                    {cat.name}
                  </h2>
                  <p className="text-white/70 text-sm font-medium mb-4 max-w-xs">{cat.description}</p>
                  <div className="flex items-center gap-2">
                    <span className="bg-white text-black text-xs font-black px-3 py-1">
                      {cat.count} LISTINGS
                    </span>
                    <svg className="w-5 h-5 text-white transform group-hover:translate-x-2 transition-transform" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M9 5l7 7-7 7" />
                    </svg>
                  </div>
                </div>
              </div>
            </Link>
          ))}
        </div>
      </section>

      {/* All Categories - Grid with subcategories preview */}
      <section className="bg-gray-100 border-y-3 border-black py-16">
        <div className="max-w-7xl mx-auto px-4">
          <div className="flex items-center justify-between mb-10">
            <h2 className="text-4xl md:text-5xl font-black uppercase tracking-tighter">
              All Categories
            </h2>
            <span className="text-sm font-bold text-gray-500 uppercase tracking-wide">{categoryTree.length} Categories</span>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
            {categoryTree.map((cat, i) => (
              <Link
                key={cat.slug}
                href={`/categories/${cat.slug}`}
                className="card group overflow-hidden animate-fade-in-up"
                style={{ animationDelay: `${i * 0.05}s`, animationFillMode: "both" }}
              >
                <div className="relative aspect-4/3 overflow-hidden">
                  <img
                    src={cat.coverImage}
                    alt={cat.name}
                    className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300"
                  />
                  <div className="absolute inset-0 bg-linear-to-t from-black/80 via-transparent to-transparent" />
                  <div className="absolute bottom-0 left-0 right-0 p-4">
                    <h3 className="font-black text-lg text-white uppercase tracking-tight mb-1">
                      {cat.name}
                    </h3>
                    <p className="text-white/70 text-xs font-medium uppercase">{cat.count} items</p>
                  </div>
                </div>
              </Link>
            ))}
          </div>
        </div>
      </section>

      {/* Ticker strip */}
      <section className="max-w-7xl mx-auto px-4 py-16">
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
