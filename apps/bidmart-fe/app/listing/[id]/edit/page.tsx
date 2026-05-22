"use client";

import { SubmitEventHandler, useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/components/auth-provider";
import { useToast } from "@/components/toast";
import { activateListing, getListingDetail, updateListing, type ListingDetail } from "@/lib/api/endpoints";

function getErrorMessage(error: unknown) {
  if (error instanceof Error && error.message) {
    return error.message;
  }

  return "Request failed. Please try again.";
}

function toLocalInputValue(value: string) {
  if (!value) {
    return "";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value.slice(0, 16);
  }

  const pad = (num: number) => String(num).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}T${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

export default function EditListingPage({
  params,
}: {
  readonly params: Promise<{ id: string }>;
}) {
  const router = useRouter();
  const { isAuthenticated, isHydrating } = useAuth();
  const toast = useToast();

  const [listingId, setListingId] = useState("");
  const [listing, setListing] = useState<ListingDetail | null>(null);
  const [description, setDescription] = useState("");
  const [imageUrl, setImageUrl] = useState("");
  const [startingPrice, setStartingPrice] = useState("");
  const [reservePrice, setReservePrice] = useState("");
  const [minBidIncrement, setMinBidIncrement] = useState("");
  const [startTime, setStartTime] = useState("");
  const [endTime, setEndTime] = useState("");
  const [isLoading, setIsLoading] = useState(true);
  const [isSaving, setIsSaving] = useState(false);
  const [isActivating, setIsActivating] = useState(false);
  const [showActivateModal, setShowActivateModal] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    params.then((value) => {
      setListingId(value.id || "");
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
      if (isHydrating || !isAuthenticated || !listingId) {
        return;
      }

      setIsLoading(true);
      setError("");

      try {
        const response = await getListingDetail(listingId);
        if (!isMounted) {
          return;
        }

        setListing(response);
        setDescription(response.description || "");
        setImageUrl(response.imageUrl || "");
        setStartingPrice(String(response.startingPrice ?? ""));
        setReservePrice(String(response.reservePrice ?? ""));
        setMinBidIncrement(String(response.minBidIncrement ?? ""));
        setStartTime(toLocalInputValue(response.startTime));
        setEndTime(toLocalInputValue(response.endTime));
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
  }, [isAuthenticated, isHydrating, listingId]);

  const canEdit = listing?.status === "DRAFT";

  const handleSubmit: SubmitEventHandler<HTMLFormElement> = async (event) => {
    event.preventDefault();

    if (!listing || !canEdit) {
      return;
    }

    if (!description.trim()) {
      setError("Description is required.");
      return;
    }

    if (!startingPrice || Number(startingPrice) < 0) {
      setError("Starting price is required.");
      return;
    }

    if (!reservePrice || Number(reservePrice) < 0) {
      setError("Reserve price is required.");
      return;
    }

    if (!minBidIncrement || Number(minBidIncrement) <= 0) {
      setError("Min bid increment is required.");
      return;
    }

    if (!startTime || !endTime) {
      setError("Start and end time are required.");
      return;
    }

    setIsSaving(true);
    setError("");

    try {
      await updateListing(listing.id, {
        description: description.trim(),
        imageUrl: imageUrl.trim() || undefined,
        startingPrice: Number(startingPrice),
        reservePrice: Number(reservePrice),
        minBidIncrement: Number(minBidIncrement),
        startTime,
        endTime,
      });
      toast.showToast("Listing updated.", "success");
      router.push(`/listing/${listing.id}`);
    } catch (saveError) {
      setError(getErrorMessage(saveError));
    } finally {
      setIsSaving(false);
    }
  };

  const handleActivate = async () => {
    if (listing?.status !== "DRAFT") {
      return;
    }

    setIsActivating(true);
    setError("");

    try {
      const updated = await activateListing(listing.id);
      setListing((current) => current ? { ...current, status: updated.status } : current);
      setShowActivateModal(false);
      toast.showToast("Listing activated.", "success");
      router.push(`/listing/${listing.id}`);
    } catch (activateError) {
      setError(getErrorMessage(activateError));
    } finally {
      setIsActivating(false);
    }
  };

  if (isHydrating || !isAuthenticated) {
    return (
      <div className="min-h-screen bg-gray-100 flex items-center justify-center px-4">
        <div className="border-3 border-black bg-white px-6 py-5 shadow-[8px_8px_0_#0A0A0A]">
          <p className="text-sm font-bold uppercase tracking-wide text-gray-700">Loading...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-100">
      <div className="max-w-3xl mx-auto px-4 py-10 md:py-14">
        <div className="flex flex-col md:flex-row md:items-end md:justify-between gap-4 mb-8">
          <div>
            <p className="text-[10px] font-black uppercase tracking-widest text-gray-500">Seller</p>
            <h1 className="text-3xl md:text-5xl font-black uppercase tracking-tighter text-black">
              Edit Listing
            </h1>
          </div>
          <div className="flex flex-wrap gap-2">
            <Link href="/profile" className="btn btn-ghost text-xs font-bold uppercase tracking-wide">My Profile</Link>
            {listingId && (
              <Link href={`/listing/${listingId}`} className="btn btn-ghost text-xs font-bold uppercase tracking-wide">View Listing</Link>
            )}
          </div>
        </div>

        {error && (
          <div className="p-4 bg-hot/10 border-2 border-hot text-hot text-sm font-bold mb-6">
            {error}
          </div>
        )}

        {isLoading ? (
          <div className="border-3 border-black bg-white p-8 shadow-[8px_8px_0_#0A0A0A]">
            <p className="text-sm font-bold uppercase tracking-wide text-gray-600">Loading listing...</p>
          </div>
        ) : (
          <ListingEditForm
            listing={listing}
            canEdit={canEdit}
            handleSubmit={handleSubmit}
            description={description}
            setDescription={setDescription}
            imageUrl={imageUrl}
            setImageUrl={setImageUrl}
            startingPrice={startingPrice}
            setStartingPrice={setStartingPrice}
            reservePrice={reservePrice}
            setReservePrice={setReservePrice}
            minBidIncrement={minBidIncrement}
            setMinBidIncrement={setMinBidIncrement}
            startTime={startTime}
            setStartTime={setStartTime}
            endTime={endTime}
            setEndTime={setEndTime}
            isSaving={isSaving}
            isActivating={isActivating}
            setShowActivateModal={setShowActivateModal}
          />
        )}
      </div>

      {showActivateModal && listing && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4">
          <div className="max-w-md w-full border-3 border-black bg-white p-6 shadow-[8px_8px_0_#0A0A0A]">
            <p className="text-xs font-black uppercase tracking-widest text-gray-500">Activate Listing</p>
            <h3 className="text-2xl font-black uppercase tracking-tight mt-2">{listing.title}</h3>
            <p className="text-sm text-gray-600 mt-3">
              This action is irreversible. Once activated, the listing can no longer be edited as a draft.
            </p>
            <div className="flex flex-wrap gap-3 mt-6">
              <button
                type="button"
                onClick={handleActivate}
                disabled={isActivating}
                className="btn btn-acid text-xs font-bold uppercase tracking-wide"
              >
                {isActivating ? "Activating..." : "Activate"}
              </button>
              <button
                type="button"
                onClick={() => setShowActivateModal(false)}
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

function ListingEditForm({ listing, canEdit, handleSubmit, description, setDescription, imageUrl, setImageUrl, startingPrice, setStartingPrice, reservePrice, setReservePrice, minBidIncrement, setMinBidIncrement, startTime, setStartTime, endTime, setEndTime, isSaving, isActivating, setShowActivateModal }: {
  listing: ListingDetail | null;
  canEdit: boolean;
  handleSubmit: SubmitEventHandler<HTMLFormElement>;
  description: string;
  setDescription: (value: string) => void;
  imageUrl: string;
  setImageUrl: (value: string) => void;
  startingPrice: string;
  setStartingPrice: (value: string) => void;
  reservePrice: string;
  setReservePrice: (value: string) => void;
  minBidIncrement: string;
  setMinBidIncrement: (value: string) => void;
  startTime: string;
  setStartTime: (value: string) => void;
  endTime: string;
  setEndTime: (value: string) => void;
  isSaving: boolean;
  isActivating: boolean;
  setShowActivateModal: (value: boolean) => void;
}) {
  return (
    listing ? (
          <div className="space-y-6">
            {!canEdit && (
              <div className="p-4 border-2 border-black bg-gray-50 text-sm font-bold">
                Only DRAFT listings can be edited.
              </div>
            )}

            <div className="border-2 border-black bg-white p-6 shadow-[4px_4px_0_#0A0A0A]">
              <p className="text-[10px] font-black uppercase tracking-widest text-gray-500 mb-2">Title</p>
              <p className="text-xl font-black uppercase tracking-tight">{listing.title}</p>
              <p className="text-xs text-gray-500 mt-1">Status: {listing.status}</p>
            </div>

            <form onSubmit={handleSubmit} className="space-y-6">
              <div className="border-2 border-black bg-white p-6 shadow-[4px_4px_0_#0A0A0A]">
                <p className="text-[10px] font-black uppercase tracking-widest text-gray-500 mb-4">Pricing</p>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                  <div>
                    <label htmlFor="startingPrice" className="block text-[10px] font-black uppercase tracking-widest text-gray-500 mb-2">
                      Starting Price (IDR) <span className="text-hot">*</span>
                    </label>
                    <input
                      id="startingPrice"
                      type="number"
                      min="0"
                      value={startingPrice}
                      onChange={(e) => setStartingPrice(e.target.value)}
                      className="w-full border-2 border-black px-4 py-3 text-sm font-bold uppercase tracking-wide focus:outline-none focus:ring-2 focus:ring-acid"
                      disabled={!canEdit}
                    />
                  </div>
                  <div>
                    <label htmlFor="reservePrice" className="block text-[10px] font-black uppercase tracking-widest text-gray-500 mb-2">
                      Reserve Price (IDR) <span className="text-hot">*</span>
                    </label>
                    <input
                      id="reservePrice"
                      type="number"
                      min="0"
                      value={reservePrice}
                      onChange={(e) => setReservePrice(e.target.value)}
                      className="w-full border-2 border-black px-4 py-3 text-sm font-bold uppercase tracking-wide focus:outline-none focus:ring-2 focus:ring-acid"
                      disabled={!canEdit}
                    />
                  </div>
                  <div>
                    <label htmlFor="minBidIncrement" className="block text-[10px] font-black uppercase tracking-widest text-gray-500 mb-2">
                      Min Bid Increment (IDR) <span className="text-hot">*</span>
                    </label>
                    <input
                      id="minBidIncrement"
                      type="number"
                      min="1"
                      value={minBidIncrement}
                      onChange={(e) => setMinBidIncrement(e.target.value)}
                      className="w-full border-2 border-black px-4 py-3 text-sm font-bold uppercase tracking-wide focus:outline-none focus:ring-2 focus:ring-acid"
                      disabled={!canEdit}
                    />
                  </div>
                </div>
              </div>

              <div className="border-2 border-black bg-white p-6 shadow-[4px_4px_0_#0A0A0A]">
                <p className="text-[10px] font-black uppercase tracking-widest text-gray-500 mb-4">Schedule</p>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label htmlFor="startTime" className="block text-[10px] font-black uppercase tracking-widest text-gray-500 mb-2">
                      Start Time <span className="text-hot">*</span>
                    </label>
                    <input
                      id="startTime"
                      type="datetime-local"
                      value={startTime}
                      onChange={(e) => setStartTime(e.target.value)}
                      className="w-full border-2 border-black px-4 py-3 text-sm font-bold uppercase tracking-wide focus:outline-none focus:ring-2 focus:ring-acid"
                      disabled={!canEdit}
                    />
                  </div>
                  <div>
                    <label htmlFor="endTime" className="block text-[10px] font-black uppercase tracking-widest text-gray-500 mb-2">
                      End Time <span className="text-hot">*</span>
                    </label>
                    <input
                      id="endTime"
                      type="datetime-local"
                      value={endTime}
                      onChange={(e) => setEndTime(e.target.value)}
                      className="w-full border-2 border-black px-4 py-3 text-sm font-bold uppercase tracking-wide focus:outline-none focus:ring-2 focus:ring-acid"
                      disabled={!canEdit}
                    />
                  </div>
                </div>
              </div>

              <div className="border-2 border-black bg-white p-6 shadow-[4px_4px_0_#0A0A0A]">
                <label htmlFor="description" className="block text-[10px] font-black uppercase tracking-widest text-gray-500 mb-2">
                  Description <span className="text-hot">*</span>
                </label>
                <textarea
                  id="description"
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  rows={6}
                  className="w-full border-2 border-black px-4 py-3 text-sm font-bold uppercase tracking-wide focus:outline-none focus:ring-2 focus:ring-acid"
                  disabled={!canEdit}
                />
              </div>

              <div className="border-2 border-black bg-white p-6 shadow-[4px_4px_0_#0A0A0A]">
                <label htmlFor="imageUrl" className="block text-[10px] font-black uppercase tracking-widest text-gray-500 mb-2">
                  Image URL
                </label>
                <input
                  id="imageUrl"
                  type="url"
                  value={imageUrl}
                  onChange={(e) => setImageUrl(e.target.value)}
                  placeholder="https://..."
                  className="w-full border-2 border-black px-4 py-3 text-sm font-bold uppercase tracking-wide focus:outline-none focus:ring-2 focus:ring-acid"
                  disabled={!canEdit}
                />
              </div>

              <div className="flex flex-wrap gap-3">
                <button
                  type="submit"
                  disabled={!canEdit || isSaving}
                  className="btn btn-acid text-xs font-bold uppercase tracking-wide"
                >
                  {isSaving ? "Saving..." : "Save Changes"}
                </button>
                {canEdit && (
                  <button
                    type="button"
                    onClick={() => setShowActivateModal(true)}
                    disabled={isActivating}
                    className="btn btn-ghost text-xs font-bold uppercase tracking-wide text-electric"
                  >
                    {isActivating ? "Activating..." : "Activate"}
                  </button>
                )}
                <Link href="/profile" className="btn btn-ghost text-xs font-bold uppercase tracking-wide">
                  Cancel
                </Link>
              </div>
            </form>
          </div>
        ) : (
          <div className="border-3 border-black bg-white p-8 shadow-[8px_8px_0_#0A0A0A]">
            <p className="text-sm font-bold uppercase tracking-wide text-gray-600">Listing not found.</p>
          </div>
        )
  )
}
