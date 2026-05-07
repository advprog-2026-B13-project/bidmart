"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useAuth } from "@/components/auth-provider";
import { createListing, getCategories, type CreateListingInput } from "@/lib/api/endpoints";
import { useToast } from "@/components/toast";
import type { CategoryItem } from "@/lib/api/endpoints";

function formatCurrency(value: number) {
  return new Intl.NumberFormat("id-ID", {
    style: "currency",
    currency: "IDR",
    maximumFractionDigits: 0,
  }).format(value);
}

export default function NewListingPage() {
  const router = useRouter();
  const { isAuthenticated, isHydrating } = useAuth();
  const toast = useToast();

  const [categories, setCategories] = useState<CategoryItem[]>([]);
  const [isLoadingCategories, setIsLoadingCategories] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errors, setErrors] = useState<Partial<Record<keyof CreateListingInput, string>>>({});

  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [categoryId, setCategoryId] = useState<number | "">("");
  const [startingPrice, setStartingPrice] = useState("");
  const [reservePrice, setReservePrice] = useState("");
  const [minBidIncrement, setMinBidIncrement] = useState("");
  const [startTime, setStartTime] = useState("");
  const [endTime, setEndTime] = useState("");
  const [imageUrl, setImageUrl] = useState("");

  useEffect(() => {
    if (!isHydrating && !isAuthenticated) {
      router.replace("/login");
      return;
    }

    getCategories()
      .then(setCategories)
      .catch(() => setCategories([]))
      .finally(() => setIsLoadingCategories(false));
  }, [isAuthenticated, isHydrating, router]);

  function validate(): boolean {
    const newErrors: typeof errors = {};
    if (!title.trim()) newErrors.title = "Title is required";
    if (!description.trim()) newErrors.description = "Description is required";
    if (!categoryId) newErrors.categoryId = "Category is required";
    if (!startingPrice || Number(startingPrice) <= 0) newErrors.startingPrice = "Starting price must be greater than 0";
    if (!reservePrice || Number(reservePrice) < 0) newErrors.reservePrice = "Reserve price is required";
    if (!minBidIncrement || Number(minBidIncrement) <= 0) newErrors.minBidIncrement = "Min bid increment must be greater than 0";
    if (!startTime) newErrors.startTime = "Start time is required";
    if (!endTime) newErrors.endTime = "End time is required";
    if (startTime && endTime && new Date(startTime) >= new Date(endTime)) {
      newErrors.endTime = "End time must be after start time";
    }
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    if (!validate()) return;

    setIsSubmitting(true);
    try {
      const input: CreateListingInput = {
        title: title.trim(),
        description: description.trim(),
        categoryId: categoryId as number,
        startingPrice: Number(startingPrice),
        reservePrice: Number(reservePrice),
        minBidIncrement: Number(minBidIncrement),
        startTime,
        endTime,
        imageUrl: imageUrl.trim() || undefined,
      };
      const listing = await createListing(input);
      toast.showToast("Listing created successfully!", "success");
      router.push(`/listing/${listing.id}`);
    } catch (err) {
      toast.showToast(err instanceof Error ? err.message : "Failed to create listing", "error");
    } finally {
      setIsSubmitting(false);
    }
  }

  if (isHydrating || !isAuthenticated) {
    return (
      <div className="min-h-screen bg-gray-100 flex items-center justify-center px-4">
        <div className="border-3 border-black bg-white px-6 py-5 shadow-[8px_8px_0_#0A0A0A]">
          <p className="text-sm font-bold uppercase tracking-wide text-gray-700">Loading...</p>
        </div>
      </div>
    );
  }

  const inputClass = (field: keyof CreateListingInput) =>
    `w-full border-2 border-black px-4 py-3 text-sm font-bold uppercase tracking-wide focus:outline-none focus:ring-2 focus:ring-acid ${errors[field] ? "border-hot" : ""}`;

  return (
    <div className="min-h-screen bg-gray-100">
      <div className="max-w-3xl mx-auto px-4 py-10 md:py-14">
        <div className="mb-6">
          <p className="text-[10px] font-black uppercase tracking-widest text-gray-500">Sell</p>
          <h1 className="text-3xl md:text-5xl font-black uppercase tracking-tighter text-black">
            Create Listing
          </h1>
        </div>

        <form onSubmit={handleSubmit} className="space-y-6">
          {/* Title */}
          <div className="border-2 border-black bg-white p-6 shadow-[4px_4px_0_#0A0A0A]">
            <label className="block text-[10px] font-black uppercase tracking-widest text-gray-500 mb-2">
              Title <span className="text-hot">*</span>
            </label>
            <input
              type="text"
              value={title}
              onChange={e => setTitle(e.target.value)}
              placeholder="e.g. Vintage Rolex Submariner"
              className={inputClass("title")}
            />
            {errors.title && <p className="mt-1 text-xs font-bold text-hot">{errors.title}</p>}
          </div>

          {/* Category */}
          <div className="border-2 border-black bg-white p-6 shadow-[4px_4px_0_#0A0A0A]">
            <label className="block text-[10px] font-black uppercase tracking-widest text-gray-500 mb-2">
              Category <span className="text-hot">*</span>
            </label>
            {isLoadingCategories ? (
              <p className="text-sm font-bold uppercase tracking-wide text-gray-400">Loading categories...</p>
            ) : (
              <>
                <select
                  value={categoryId}
                  onChange={e => setCategoryId(e.target.value ? Number(e.target.value) : "")}
                  className={inputClass("categoryId")}
                >
                  <option value="">Select a category</option>
                  {categories.map(cat => (
                    <option key={cat.id} value={cat.id}>{cat.name}</option>
                  ))}
                </select>
                {errors.categoryId && <p className="mt-1 text-xs font-bold text-hot">{errors.categoryId}</p>}
              </>
            )}
          </div>

          {/* Description */}
          <div className="border-2 border-black bg-white p-6 shadow-[4px_4px_0_#0A0A0A]">
            <label className="block text-[10px] font-black uppercase tracking-widest text-gray-500 mb-2">
              Description <span className="text-hot">*</span>
            </label>
            <textarea
              value={description}
              onChange={e => setDescription(e.target.value)}
              rows={5}
              placeholder="Describe your item in detail..."
              className={inputClass("description")}
            />
            {errors.description && <p className="mt-1 text-xs font-bold text-hot">{errors.description}</p>}
          </div>

          {/* Pricing */}
          <div className="border-2 border-black bg-white p-6 shadow-[4px_4px_0_#0A0A0A]">
            <p className="text-[10px] font-black uppercase tracking-widest text-gray-500 mb-4">Pricing</p>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
              <div>
                <label className="block text-[10px] font-black uppercase tracking-widest text-gray-500 mb-2">
                  Starting Price (IDR) <span className="text-hot">*</span>
                </label>
                <input
                  type="number"
                  min="0"
                  value={startingPrice}
                  onChange={e => setStartingPrice(e.target.value)}
                  placeholder="100000"
                  className={inputClass("startingPrice")}
                />
                {errors.startingPrice && <p className="mt-1 text-xs font-bold text-hot">{errors.startingPrice}</p>}
              </div>
              <div>
                <label className="block text-[10px] font-black uppercase tracking-widest text-gray-500 mb-2">
                  Reserve Price (IDR) <span className="text-hot">*</span>
                </label>
                <input
                  type="number"
                  min="0"
                  value={reservePrice}
                  onChange={e => setReservePrice(e.target.value)}
                  placeholder="500000"
                  className={inputClass("reservePrice")}
                />
                {errors.reservePrice && <p className="mt-1 text-xs font-bold text-hot">{errors.reservePrice}</p>}
              </div>
              <div>
                <label className="block text-[10px] font-black uppercase tracking-widest text-gray-500 mb-2">
                  Min Bid Increment (IDR) <span className="text-hot">*</span>
                </label>
                <input
                  type="number"
                  min="1"
                  value={minBidIncrement}
                  onChange={e => setMinBidIncrement(e.target.value)}
                  placeholder="10000"
                  className={inputClass("minBidIncrement")}
                />
                {errors.minBidIncrement && <p className="mt-1 text-xs font-bold text-hot">{errors.minBidIncrement}</p>}
              </div>
            </div>
            {(startingPrice || reservePrice) && (
              <p className="mt-3 text-xs text-gray-500 font-medium">
                Reserve price is the minimum price you&apos;ll accept. Items below reserve won&apos;t sell.
              </p>
            )}
          </div>

          {/* Timing */}
          <div className="border-2 border-black bg-white p-6 shadow-[4px_4px_0_#0A0A0A]">
            <p className="text-[10px] font-black uppercase tracking-widest text-gray-500 mb-4">Auction Timing</p>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <label className="block text-[10px] font-black uppercase tracking-widest text-gray-500 mb-2">
                  Start Time <span className="text-hot">*</span>
                </label>
                <input
                  type="datetime-local"
                  value={startTime}
                  onChange={e => setStartTime(e.target.value)}
                  className={inputClass("startTime")}
                />
                {errors.startTime && <p className="mt-1 text-xs font-bold text-hot">{errors.startTime}</p>}
              </div>
              <div>
                <label className="block text-[10px] font-black uppercase tracking-widest text-gray-500 mb-2">
                  End Time <span className="text-hot">*</span>
                </label>
                <input
                  type="datetime-local"
                  value={endTime}
                  onChange={e => setEndTime(e.target.value)}
                  className={inputClass("endTime")}
                />
                {errors.endTime && <p className="mt-1 text-xs font-bold text-hot">{errors.endTime}</p>}
              </div>
            </div>
          </div>

          {/* Image URL */}
          <div className="border-2 border-black bg-white p-6 shadow-[4px_4px_0_#0A0A0A]">
            <label className="block text-[10px] font-black uppercase tracking-widest text-gray-500 mb-2">
              Image URL <span className="text-gray-400 font-normal">(optional)</span>
            </label>
            <input
              type="url"
              value={imageUrl}
              onChange={e => setImageUrl(e.target.value)}
              placeholder="https://example.com/image.jpg"
              className={inputClass("imageUrl")}
            />
          </div>

          {/* Summary */}
          {startingPrice && reservePrice && (
            <div className="border-2 border-acid bg-acid/10 p-6">
              <p className="text-[10px] font-black uppercase tracking-widest text-gray-500 mb-3">Listing Summary</p>
              <div className="grid grid-cols-2 md:grid-cols-4 gap-4 text-sm">
                <div>
                  <p className="font-black uppercase text-xs text-gray-500">Start Price</p>
                  <p className="font-black">{formatCurrency(Number(startingPrice))}</p>
                </div>
                <div>
                  <p className="font-black uppercase text-xs text-gray-500">Reserve</p>
                  <p className="font-black">{formatCurrency(Number(reservePrice))}</p>
                </div>
                <div>
                  <p className="font-black uppercase text-xs text-gray-500">Min Increment</p>
                  <p className="font-black">{formatCurrency(Number(minBidIncrement))}</p>
                </div>
                <div>
                  <p className="font-black uppercase text-xs text-gray-500">First Bid</p>
                  <p className="font-black">{(Number(startingPrice) + Number(minBidIncrement)).toLocaleString("id-ID")}</p>
                </div>
              </div>
            </div>
          )}

          {/* Actions */}
          <div className="flex items-center justify-between gap-4">
            <Link
              href="/"
              className="px-6 py-3 border-2 border-black font-black text-sm uppercase tracking-wide hover:bg-gray-100 transition-colors"
            >
              Cancel
            </Link>
            <button
              type="submit"
              disabled={isSubmitting}
              className="px-8 py-3 bg-electric border-2 border-black font-black text-sm uppercase tracking-wide text-white disabled:opacity-50 disabled:cursor-not-allowed hover:bg-electric/90 transition-colors shadow-[4px_4px_0_#0A0A0A] hover:shadow-[6px_6px_0_#0A0A0A] hover:translate-x-[-2px] hover:translate-y-[-2px]"
            >
              {isSubmitting ? "Creating..." : "Create Listing"}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
