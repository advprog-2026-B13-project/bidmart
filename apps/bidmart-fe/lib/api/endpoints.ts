import { apiFetch } from "../auth/api-client";

export interface AuctionStatus {
  listingId: string;
  currentPrice: number;
  currentWinnerId: string | null;
  endTime: string;
  status: string;
}

export interface BidResult {
  bidId: string;
  listingId: string;
  bidderId: string;
  amount: number;
  status: string;
  createdAt: string;
}

export interface BidRequest {
  listingId: string;
  amount: number;
  bidType: "MANUAL" | "PROXY";
}

export interface CategoryItem {
  id: number;
  name: string;
  parentId: number | null;
  slug: string;
  coverImage: string;
}

export interface ParsedListing {
  id: string;
  title: string;
  description: string;
  imageUrl: string;
  categoryId: number;
  category: string;
  condition: "New" | "Like New" | "Excellent" | "Good" | "Fair";
  startingPrice: number;
  currentPrice: number;
  reservePrice: number | null;
  bidCount: number;
  endTime: Date;
  status: "active" | "ending-soon" | "ended" | "sold";
  seller: { id: string; name: string; avatar: string; rating: number };
  topBidder: { id: string; name: string } | null;
}

function toSlug(name: string) {
  return name.toLowerCase().replace(/\s+/g, "-").replace(/[^a-z0-9-]/g, "");
}

function mapStatus(status: string): "active" | "ending-soon" | "ended" | "sold" {
  if (status === "ACTIVE" || status === "EXTENDED") return "active";
  if (status === "CLOSED") return "ended";
  if (status === "WON") return "sold";
  return "active";
}

// Spring Page response — no ApiResponse envelope
interface PageResponse {
  content: Record<string, unknown>[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

function parseListing(listing: Record<string, unknown>): ParsedListing {
  const cat = listing.category as Record<string, unknown> | undefined;
  return {
    id: listing.id as string,
    title: listing.title as string,
    description: listing.description as string,
    imageUrl: (listing.imageUrl as string) || "",
    categoryId: (listing.category as { id: number })?.id ?? 0,
    category: (cat?.name as string) || "",
    condition: "Good",
    startingPrice: Number(listing.startingPrice),
    currentPrice: Number(listing.currentPrice),
    reservePrice: listing.reservePrice != null ? Number(listing.reservePrice) : null,
    bidCount: (listing.bidCount as number) ?? 0,
    endTime: new Date(listing.endTime as string),
    status: mapStatus(listing.status as string),
    seller: {
      id: listing.sellerId as string,
      name: "Seller",
      avatar: "https://i.pravatar.cc/150?u=seller",
      rating: 4.5,
    },
    topBidder: listing.winnerId ? { id: listing.winnerId as string, name: "Bidder" } : null,
  };
}

export async function getListings(params?: {
  keyword?: string;
  minPrice?: number;
  maxPrice?: number;
  categoryId?: number;
  page?: number;
  size?: number;
}) {
  const searchParams = new URLSearchParams();
  if (params?.keyword) searchParams.set("keyword", params.keyword);
  if (params?.minPrice) searchParams.set("minPrice", String(params.minPrice));
  if (params?.maxPrice) searchParams.set("maxPrice", String(params.maxPrice));
  if (params?.categoryId) searchParams.set("categoryId", String(params.categoryId));
  if (params?.page !== undefined) searchParams.set("page", String(params.page));
  if (params?.size !== undefined) searchParams.set("size", String(params.size));

  const qs = searchParams.toString();
  const path = `/api/catalog/listings/search${qs ? `?${qs}` : ""}`;

  const raw = await apiFetch(path, { method: "GET" }, { auth: false }) as PageResponse;

  return {
    listings: (raw.content || []).map(parseListing),
    total: raw.totalElements || 0,
  };
}

export async function getListingById(id: string) {
  const raw = await apiFetch(`/api/catalog/listings/detail/${id}`, { method: "GET" }, { auth: false }) as Record<string, unknown>;
  return parseListing(raw);
}

export async function getAuctionStatus(listingId: string) {
  const raw = await apiFetch(`/api/bidding/listings/${listingId}/status`, { method: "GET" }, { auth: false }) as { data: AuctionStatus };
  return raw.data;
}

export async function getBidsForListing(listingId: string) {
  const raw = await apiFetch(`/api/bidding/listings/${listingId}/bids`, { method: "GET" }, { auth: false }) as { data: BidResult[] };
  return raw.data || [];
}

export async function placeBid(request: BidRequest) {
  const raw = await apiFetch("/api/bidding/bids", { method: "POST", body: JSON.stringify(request) }, { auth: true }) as { data: BidResult };
  return raw.data;
}

export async function getCategories() {
  const raw = await apiFetch("/api/catalog/categories/main", { method: "GET" }, { auth: false }) as { id: number; name: string; parentId: number | null }[];
  const cats = Array.isArray(raw) ? raw : [];
  return cats.map((c) => ({
    id: c.id,
    name: c.name,
    parentId: c.parentId,
    slug: toSlug(c.name),
    coverImage: `https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=800&q=80`,
  }));
}

export async function getSubCategories(parentId: number) {
  const raw = await apiFetch(`/api/catalog/categories/sub/${parentId}`, { method: "GET" }, { auth: false }) as { id: number; name: string; parentId: number | null }[];
  const cats = Array.isArray(raw) ? raw : [];
  return cats.map((c) => ({
    id: c.id,
    name: c.name,
    parentId: c.parentId,
    slug: toSlug(c.name),
    coverImage: `https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=800&q=80`,
  }));
}

export interface NotificationItem {
  id: string;
  userId: string;
  type: string;
  message: string;
  isRead: boolean;
  createdAt: string;
}

export async function getNotifications(userId: string) {
  const raw = await apiFetch(`/api/notifications/user/${userId}`, { method: "GET" }, { auth: true }) as NotificationItem[];
  return Array.isArray(raw) ? raw : [];
}

export async function getCategoryById(id: number) {
  const raw = await apiFetch(`/api/catalog/categories/${id}`, { method: "GET" }, { auth: false }) as { id: number; name: string; parentId: number | null } | null;
  if (!raw || Array.isArray(raw)) return null;
  return {
    id: raw.id,
    name: raw.name,
    parentId: raw.parentId,
    slug: toSlug(raw.name),
    coverImage: "",
  };
}