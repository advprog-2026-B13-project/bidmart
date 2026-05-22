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
  maxAmount?: number;
  source?: "MANUAL" | "PROXY";
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
  imageUrl: string | null;
  categoryId: number;
  category: string;
  condition: "New" | "Like New" | "Excellent" | "Good" | "Fair";
  startingPrice: number;
  currentPrice: number;
  reservePrice: number | null;
  minBidIncrement: number;
  bidCount: number;
  startTime: Date;
  endTime: Date;
  status: "active" | "ending-soon" | "ended" | "sold";
  canEdit: boolean;
  seller: { id: string; name: string; avatar: string; rating: number };
  topBidder: { id: string; name: string } | null;
}

export interface SellerListing {
  id: string;
  title: string;
  imageUrl: string | null;
  status: string;
  startingPrice: number;
  reservePrice: number | null;
  currentPrice: number;
  bidCount: number;
  startTime: Date;
  endTime: Date;
  categoryName: string;
}

export interface ListingDetail {
  id: string;
  title: string;
  description: string;
  imageUrl: string | null;
  status: string;
  startingPrice: number;
  reservePrice: number | null;
  minBidIncrement: number;
  startTime: string;
  endTime: string;
  categoryName: string;
}

function toSlug(name: string) {
  return name.toLowerCase().replace(/\s+/g, "-").replace(/[^a-z0-9-]/g, "");
}

function mapStatus(status: string): "active" | "ending-soon" | "ended" | "sold" {
  if (status === "ACTIVE" || status === "EXTENDED") return "active";
  if (status === "CLOSED" || status === "UNSOLD") return "ended";
  if (status === "WON") return "sold";
  return "active";
}

function normalizeImageUrl(value: unknown): string | null {
  if (typeof value !== "string") return null;
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : null;
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
    imageUrl: normalizeImageUrl(listing.imageUrl),
    categoryId: (listing.category as { id: number })?.id ?? 0,
    category: (cat?.name as string) || "",
    condition: "Good",
    startingPrice: Number(listing.startingPrice),
    currentPrice: Number(listing.currentPrice),
    reservePrice: listing.reservePrice != null ? Number(listing.reservePrice) : null,
    minBidIncrement: Number(listing.minBidIncrement),
    bidCount: (listing.bidCount as number) ?? 0,
    startTime: new Date(listing.startTime as string),
    endTime: new Date(listing.endTime as string),
    status: mapStatus(listing.status as string),
    canEdit: Boolean(listing.canEdit),
    seller: {
      id: listing.sellerId as string,
      name: "Seller",
      avatar: `https://api.dicebear.com/9.x/open-peeps/svg?seed=${listing.sellerId as string}`,
      rating: 4.5,
    },
    topBidder: listing.winnerId ? { id: listing.winnerId as string, name: "Bidder" } : null,
  };
}

function parseSellerListing(listing: Record<string, unknown>): SellerListing {
  const cat = listing.category as Record<string, unknown> | undefined;
  return {
    id: listing.id as string,
    title: listing.title as string,
    imageUrl: normalizeImageUrl(listing.imageUrl),
    status: (listing.status as string) || "UNKNOWN",
    startingPrice: Number(listing.startingPrice),
    reservePrice: listing.reservePrice != null ? Number(listing.reservePrice) : null,
    currentPrice: Number(listing.currentPrice),
    bidCount: (listing.bidCount as number) ?? 0,
    startTime: new Date(listing.startTime as string),
    endTime: new Date(listing.endTime as string),
    categoryName: (cat?.name as string) || "",
  };
}

function parseListingDetail(listing: Record<string, unknown>): ListingDetail {
  const cat = listing.category as Record<string, unknown> | undefined;
  return {
    id: listing.id as string,
    title: listing.title as string,
    description: listing.description as string,
    imageUrl: normalizeImageUrl(listing.imageUrl),
    status: (listing.status as string) || "UNKNOWN",
    startingPrice: Number(listing.startingPrice),
    reservePrice: listing.reservePrice != null ? Number(listing.reservePrice) : null,
    minBidIncrement: Number(listing.minBidIncrement),
    startTime: listing.startTime as string,
    endTime: listing.endTime as string,
    categoryName: (cat?.name as string) || "",
  };
}

export async function getListings(params?: {
  keyword?: string;
  minPrice?: number;
  maxPrice?: number;
  categoryId?: number;
  status?: string;
  page?: number;
  size?: number;
}) {
  const searchParams = new URLSearchParams();
  if (params?.keyword) searchParams.set("keyword", params.keyword);
  if (params?.minPrice) searchParams.set("minPrice", String(params.minPrice));
  if (params?.maxPrice) searchParams.set("maxPrice", String(params.maxPrice));
  if (params?.categoryId) searchParams.set("categoryId", String(params.categoryId));
  if (params?.status) searchParams.set("status", params.status);
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

export async function getListingByIdOwner(id: string) {
  const raw = await apiFetch(`/api/catalog/listings/detail/${id}/owner`, { method: "GET" }, { auth: true }) as Record<string, unknown>;
  return parseListing(raw);
}

export async function getListingDetail(id: string) {
  const raw = await apiFetch(`/api/catalog/listings/detail/${id}/owner`, { method: "GET" }, { auth: true }) as Record<string, unknown>;
  return parseListingDetail(raw);
}

export async function getAuctionStatus(listingId: string) {
  const raw = await apiFetch(`/api/bidding/listings/${listingId}/status`, { method: "GET" }, { auth: false }) as { data: AuctionStatus };
  return raw.data;
}

export async function getBidsForListing(listingId: string) {
  const raw = await apiFetch(`/api/bidding/listings/${listingId}/bids`, { method: "GET" }) as { data: BidResult[] };
  return raw.data || [];
}

export async function placeBid(request: BidRequest) {
  const raw = await apiFetch("/api/bidding/bids", { method: "POST", body: JSON.stringify(request) }, { auth: true }) as { data: BidResult };
  return raw.data;
}

export async function getCategories() {
  const raw = await apiFetch("/api/catalog/categories/main", { method: "GET" }, { auth: false }) as { id: number; name: string; parentId: number | null; imageUrl: string }[];
  const cats = Array.isArray(raw) ? raw : [];
  return cats.map((c) => ({
    id: c.id,
    name: c.name,
    parentId: c.parentId,
    imageUrl: c.imageUrl,
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
  referenceId?: string;
  createdAt: string;
}

interface RawNotification {
  id: string;
  userId: string;
  type: string;
  message: string;
  read?: boolean;
  isRead?: boolean;
  referenceId: string;
  createdAt: string;
}
export async function getNotifications(userId: string): Promise<NotificationItem[]> {
  const raw = await apiFetch(`/api/notifications/user/${userId}`, { method: "GET" }, { auth: true }) as RawNotification[];
  if (!Array.isArray(raw)) return [];
  return raw.map(n => ({
    id: n.id,
    userId: n.userId,
    type: n.type,
    message: n.message,
    isRead: n.read !== undefined ? n.read : (n.isRead ?? false),
    referenceId: n.referenceId,
    createdAt: n.createdAt,
  }));
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

export interface CreateListingInput {
  title: string;
  description: string;
  imageUrl?: string;
  categoryId: number;
  startingPrice: number;
  reservePrice: number;
  minBidIncrement: number;
  startTime: string; // ISO datetime string
  endTime: string;   // ISO datetime string
}

export interface UpdateListingInput {
  description: string;
  imageUrl?: string;
  startingPrice: number;
  reservePrice: number;
  minBidIncrement: number;
  startTime: string;
  endTime: string;
}

export async function createListing(input: CreateListingInput) {
  const raw = await apiFetch("/api/catalog/listings/create", {
    method: "POST",
    body: JSON.stringify(input),
  }, { auth: true });
  return parseListing(raw as Record<string, unknown>);
}

export async function updateListing(id: string, input: UpdateListingInput) {
  const raw = await apiFetch(`/api/catalog/listings/update/${id}`, {
    method: "PUT",
    body: JSON.stringify(input),
  }, { auth: true });
  return parseListingDetail(raw as Record<string, unknown>);
}

export async function activateListing(id: string) {
  const raw = await apiFetch(`/api/catalog/listings/${id}/activate`, { method: "PUT" }, { auth: true });
  return parseListingDetail(raw as Record<string, unknown>);
}

export async function closeListing(id: string) {
  const raw = await apiFetch(`/api/catalog/listings/${id}/close`, { method: "PUT" }, { auth: true });
  return parseListingDetail(raw as Record<string, unknown>);
}

export async function deleteListing(id: string) {
  await apiFetch(`/api/catalog/listings/delete/${id}`, { method: "DELETE" }, { auth: true });
}

export async function getMyListings() {
  const raw = await apiFetch("/api/catalog/listings/mine", { method: "GET" }, { auth: true }) as Record<string, unknown>[];
  const listings = Array.isArray(raw) ? raw : [];
  return listings.map(parseSellerListing);
}

export interface NotificationPreference {
  id: string;
  userId: string;
  emailEnabled: boolean;
  pushEnabled: boolean;
}

export async function getNotificationPreferences(userId: string): Promise<NotificationPreference> {
  return await apiFetch(`/api/notifications/preferences/${userId}`, { method: "GET" }, { auth: true });
}

export async function updateNotificationPreferences(
    userId: string,
    data: { emailEnabled: boolean; pushEnabled: boolean }
): Promise<NotificationPreference> {
  return await apiFetch(`/api/notifications/preferences/${userId}`, {
    method: "PUT",
    body: JSON.stringify(data),
  }, { auth: true });
}

export async function markNotificationAsRead(id: string): Promise<void> {
  await apiFetch(`/api/notifications/${id}/read`, { method: "POST" }, { auth: true });
}
