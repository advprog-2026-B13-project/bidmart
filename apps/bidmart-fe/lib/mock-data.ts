// ─── Types ──────────────────────────────────────────────────────────
export interface Listing {
  id: string;
  title: string;
  description: string;
  imageUrl: string;
  category: string;
  condition: "New" | "Like New" | "Excellent" | "Good" | "Fair";
  startingPrice: number;
  currentPrice: number;
  reservePrice: number | null;
  bidCount: number;
  endTime: Date;
  status: "active" | "ending-soon" | "ended" | "sold";
  seller: {
    id: string;
    name: string;
    avatar: string;
    rating: number;
  };
  topBidder: {
    id: string;
    name: string;
  } | null;
}

export interface Bid {
  id: string;
  listingId: string;
  bidder: {
    id: string;
    name: string;
    avatar: string;
  };
  amount: number;
  timestamp: Date;
}

export interface User {
  id: string;
  email: string;
  name: string;
  avatar: string;
  rating: number;
  joinedAt: Date;
  walletBalance: number;
}

// ─── Mock Users ─────────────────────────────────────────────────────
export const mockUsers: User[] = [
  {
    id: "u1",
    email: "sarah.chen@email.com",
    name: "Sarah Chen",
    avatar: "https://i.pravatar.cc/150?u=sarah",
    rating: 4.9,
    joinedAt: new Date("2024-03-15"),
    walletBalance: 2450.00,
  },
  {
    id: "u2",
    email: "marcus.wong@email.com",
    name: "Marcus Wong",
    avatar: "https://i.pravatar.cc/150?u=marcus",
    rating: 4.7,
    joinedAt: new Date("2024-06-22"),
    walletBalance: 1890.50,
  },
  {
    id: "u3",
    email: "elena.rossi@email.com",
    name: "Elena Rossi",
    avatar: "https://i.pravatar.cc/150?u=elena",
    rating: 4.8,
    joinedAt: new Date("2023-11-08"),
    walletBalance: 3200.00,
  },
];

// ─── Mock Listings ─────────────────────────────────────────────────
const now = new Date();
const hoursFromNow = (h: number) => new Date(now.getTime() + h * 60 * 60 * 1000);
const daysFromNow = (d: number) => new Date(now.getTime() + d * 24 * 60 * 60 * 1000);

export const mockListings: Listing[] = [
  {
    id: "lst-001",
    title: "Vintage Leica M3 Camera — 1954 First Edition",
    description: "Authentic 1954 Leica M3 double-stroke rangefinder camera. Serial number 700xxx. Includes original leather case and 50mm Summicron f/2 lens. Professionally serviced in 2025.",
    imageUrl: "https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=800&q=80",
    category: "Electronics",
    condition: "Excellent",
    startingPrice: 2800,
    currentPrice: 3850,
    reservePrice: 3500,
    bidCount: 23,
    endTime: hoursFromNow(2.5),
    status: "ending-soon",
    seller: mockUsers[2],
    topBidder: { id: "u1", name: "Sarah Chen" },
  },
  {
    id: "lst-002",
    title: "Herman Miller Eames Lounge Chair — Walnut",
    description: "Iconic Eames Lounge Chair and Ottoman in genuine walnut veneer with black leather upholstery. Authentic Herman Miller piece from authorized dealer. Purchased 2022.",
    imageUrl: "https://images.unsplash.com/photo-1567538096630-e0c55bd6374c?w=800&q=80",
    category: "Furniture",
    condition: "Like New",
    startingPrice: 4500,
    currentPrice: 5200,
    reservePrice: null,
    bidCount: 15,
    endTime: hoursFromNow(18),
    status: "active",
    seller: mockUsers[0],
    topBidder: { id: "u3", name: "Elena Rossi" },
  },
  {
    id: "lst-003",
    title: "First Edition Hemingway — The Sun Also Rises",
    description: "True first edition, first printing of Ernest Hemingway's masterpiece published by Charles Scribner's Sons, 1926. Dust jacket intact with minor wear. Authenticated.",
    imageUrl: "https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=800&q=80",
    category: "Books",
    condition: "Good",
    startingPrice: 15000,
    currentPrice: 18750,
    reservePrice: 17500,
    bidCount: 8,
    endTime: daysFromNow(3),
    status: "active",
    seller: mockUsers[1],
    topBidder: { id: "u2", name: "Marcus Wong" },
  },
  {
    id: "lst-004",
    title: "Rolex Submariner Date — 41mm Stainless",
    description: "2021 Rolex Submariner Date ref. 126610LN. Complete set with box, papers, and warranty card. Worn only 6 months. No scratches on crystal or clasp.",
    imageUrl: "https://images.unsplash.com/photo-1587836374828-4dbafa94cf0e?w=800&q=80",
    category: "Watches",
    condition: "Like New",
    startingPrice: 12000,
    currentPrice: 14200,
    reservePrice: 13500,
    bidCount: 31,
    endTime: hoursFromNow(0.5),
    status: "ending-soon",
    seller: mockUsers[2],
    topBidder: { id: "u1", name: "Sarah Chen" },
  },
  {
    id: "lst-005",
    title: "Original Banksy Print — Girl with Balloon",
    description: "Authenticated Banksy screenprint. 50x70cm on cotton rag paper. Signed in pencil. Pest Control verified. Comes with certificate of authenticity.",
    imageUrl: "https://images.unsplash.com/photo-1578662996442-48f60103fc96?w=800&q=80",
    category: "Art",
    condition: "New",
    startingPrice: 250000,
    currentPrice: 285000,
    reservePrice: 280000,
    bidCount: 12,
    endTime: daysFromNow(5),
    status: "active",
    seller: mockUsers[0],
    topBidder: { id: "u3", name: "Elena Rossi" },
  },
  {
    id: "lst-006",
    title: "Gibson Les Paul Standard '59 Reissue",
    description: "Gibson Custom Shop 60th Anniversary 1959 Les Paul Standard. Historic Teambuilt model with accurate '59 specs. tobacco burst finish. Hardshell case included.",
    imageUrl: "https://images.unsplash.com/photo-1564186763535-ebb21ef5277f?w=800&q=80",
    category: "Musical Instruments",
    condition: "Excellent",
    startingPrice: 3500,
    currentPrice: 4100,
    reservePrice: null,
    bidCount: 19,
    endTime: hoursFromNow(8),
    status: "active",
    seller: mockUsers[1],
    topBidder: { id: "u2", name: "Marcus Wong" },
  },
  {
    id: "lst-007",
    title: "Nintendo Sealed DS Lite — Arctic White",
    description: "Brand new, factory sealed Nintendo DS Lite in Arctic White. Japanese region free. Original sticker on back has been preserved. Collector grade.",
    imageUrl: "https://images.unsplash.com/photo-1531525645387-7f14be1bdbbd?w=800&q=80",
    category: "Gaming",
    condition: "New",
    startingPrice: 800,
    currentPrice: 1150,
    reservePrice: 900,
    bidCount: 27,
    endTime: hoursFromNow(4),
    status: "active",
    seller: mockUsers[0],
    topBidder: { id: "u1", name: "Sarah Chen" },
  },
  {
    id: "lst-008",
    title: "Omega Speedmaster Moonwatch — Hesalite",
    description: "Omega Speedmaster Professional Moonwatch with hesalite crystal. Caliber 3861 manual movement. Full kit: box, papers, NATO strap, bracelet. Worn twice.",
    imageUrl: "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=800&q=80",
    category: "Watches",
    condition: "Like New",
    startingPrice: 5500,
    currentPrice: 6200,
    reservePrice: null,
    bidCount: 14,
    endTime: daysFromNow(2),
    status: "active",
    seller: mockUsers[2],
    topBidder: { id: "u2", name: "Marcus Wong" },
  },
];

// ─── Mock Bids ─────────────────────────────────────────────────────
export const mockBids: Record<string, Bid[]> = {
  "lst-001": [
    { id: "b1", listingId: "lst-001", bidder: mockUsers[2], amount: 3850, timestamp: hoursFromNow(-0.5) },
    { id: "b2", listingId: "lst-001", bidder: mockUsers[0], amount: 3700, timestamp: hoursFromNow(-1) },
    { id: "b3", listingId: "lst-001", bidder: mockUsers[1], amount: 3550, timestamp: hoursFromNow(-2) },
    { id: "b4", listingId: "lst-001", bidder: mockUsers[2], amount: 3400, timestamp: hoursFromNow(-3) },
    { id: "b5", listingId: "lst-001", bidder: mockUsers[0], amount: 3200, timestamp: hoursFromNow(-5) },
  ],
  "lst-004": [
    { id: "b10", listingId: "lst-004", bidder: mockUsers[0], amount: 14200, timestamp: hoursFromNow(-0.1) },
    { id: "b11", listingId: "lst-004", bidder: mockUsers[1], amount: 14000, timestamp: hoursFromNow(-0.3) },
    { id: "b12", listingId: "lst-004", bidder: mockUsers[2], amount: 13800, timestamp: hoursFromNow(-0.5) },
  ],
};

// ─── Categories ────────────────────────────────────────────────────
// ─── Category Types ───────────────────────────────────────────────────
export interface Category {
  name: string;
  slug: string;
  coverImage: string;
  description: string;
  count: number;
  children?: Category[];
}

// ─── Nested Categories ────────────────────────────────────────────────
export const categoryTree: Category[] = [
  {
    name: "Electronics",
    slug: "electronics",
    coverImage: "https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=1200&q=80",
    description: "Cameras, audio gear, vintage tech",
    count: 234,
    children: [
      { name: "Cameras", slug: "cameras", coverImage: "https://images.unsplash.com/photo-1516035069371-29a1b244cc32?w=800&q=80", description: "DSLR, mirrorless, film cameras", count: 89 },
      { name: "Audio", slug: "audio", coverImage: "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=800&q=80", description: "Headphones, speakers, amplifiers", count: 67 },
      { name: "Vintage Tech", slug: "vintage-tech", coverImage: "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=800&q=80", description: "Retro consoles, classic gadgets", count: 45 },
      { name: "Computers", slug: "computers", coverImage: "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=800&q=80", description: "Vintage computers, peripherals", count: 33 },
    ],
  },
  {
    name: "Watches",
    slug: "watches",
    coverImage: "https://images.unsplash.com/photo-1587836374828-4dbafa94cf0e?w=1200&q=80",
    description: "Luxury timepieces, rare chronographs",
    count: 156,
    children: [
      { name: "Luxury", slug: "luxury", coverImage: "https://images.unsplash.com/photo-1587836374828-4dbafa94cf0e?w=800&q=80", description: "Rolex, Omega, Patek Philippe", count: 67 },
      { name: "Vintage", slug: "vintage", coverImage: "https://images.unsplash.com/photo-1524592094714-0f0654e20314?w=800&q=80", description: "1960s-1990s timepieces", count: 42 },
      { name: "Smartwatches", slug: "smartwatches", coverImage: "https://images.unsplash.com/photo-1546868871-7041f2a55e12?w=800&q=80", description: "Apple Watch, Garmin, etc", count: 28 },
      { name: "Accessories", slug: "accessories", coverImage: "https://images.unsplash.com/photo-1594534475808-b18fc33b045e?w=800&q=80", description: "Straps, boxes, tools", count: 19 },
    ],
  },
  {
    name: "Art",
    slug: "art",
    coverImage: "https://images.unsplash.com/photo-1578662996442-48f60103fc96?w=1200&q=80",
    description: "Prints, street art, contemporary pieces",
    count: 89,
    children: [
      { name: "Prints", slug: "prints", coverImage: "https://images.unsplash.com/photo-1578662996442-48f60103fc96?w=800&q=80", description: "Limited editions, posters", count: 34 },
      { name: "Street Art", slug: "street-art", coverImage: "https://images.unsplash.com/photo-1561214115-f2f134cc4912?w=800&q=80", description: "Banksy, Shepard Fairey, murals", count: 23 },
      { name: "Contemporary", slug: "contemporary", coverImage: "https://images.unsplash.com/photo-1541961017774-22349e4a1262?w=800&q=80", description: "Modern artists, emerging work", count: 19 },
      { name: "Photography", slug: "photography", coverImage: "https://images.unsplash.com/photo-1452587925148-ce544e77e70d?w=800&q=80", description: "Fine art prints, editions", count: 13 },
    ],
  },
  {
    name: "Furniture",
    slug: "furniture",
    coverImage: "https://images.unsplash.com/photo-1567538096630-e0c55bd6374c?w=1200&q=80",
    description: "Designer chairs, vintage finds",
    count: 178,
    children: [
      { name: "Chairs", slug: "chairs", coverImage: "https://images.unsplash.com/photo-1506439773649-6e0eb8cfb237?w=800&q=80", description: "Eames, Barcelona chairs", count: 56 },
      { name: "Tables", slug: "tables", coverImage: "https://images.unsplash.com/photo-1555041469-a586c61ea9bc?w=800&q=80", description: "Dining, coffee, side tables", count: 45 },
      { name: "Storage", slug: "storage", coverImage: "https://images.unsplash.com/photo-1595428774223-ef52624120d2?w=800&q=80", description: "Cabinets, shelving, credenzas", count: 38 },
      { name: "Lighting", slug: "lighting", coverImage: "https://images.unsplash.com/photo-1507473885765-e6ed057f782c?w=800&q=80", description: "Floor lamps, pendants, sconces", count: 39 },
    ],
  },
  {
    name: "Books",
    slug: "books",
    coverImage: "https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=1200&q=80",
    description: "First editions, rare manuscripts",
    count: 92,
    children: [
      { name: "First Editions", slug: "first-editions", coverImage: "https://images.unsplash.com/photo-1544947950-fa07a98d237f?w=800&q=80", description: "First printings, signed copies", count: 38 },
      { name: "Rare Books", slug: "rare-books", coverImage: "https://images.unsplash.com/photo-1481627834876-b7833e8f5570?w=800&q=80", description: "Antiquarian, manuscripts", count: 24 },
      { name: "Comics", slug: "comics", coverImage: "https://images.unsplash.com/photo-1612036782180-6f0b6cd846fe?w=800&q=80", description: "Marvel, DC, indie comics", count: 18 },
      { name: "Magazines", slug: "magazines", coverImage: "https://images.unsplash.com/photo-1512820790803-83ca734da794?w=800&q=80", description: "Vintage issues, collectible", count: 12 },
    ],
  },
  {
    name: "Musical Instruments",
    slug: "musical-instruments",
    coverImage: "https://images.unsplash.com/photo-1564186763535-ebb21ef5277f?w=1200&q=80",
    description: "Guitars, synthesizers, vintage gear",
    count: 67,
    children: [
      { name: "Guitars", slug: "guitars", coverImage: "https://images.unsplash.com/photo-1564186763535-ebb21ef5277f?w=800&q=80", description: "Electric, acoustic, bass", count: 28 },
      { name: "Synthesizers", slug: "synthesizers", coverImage: "https://images.unsplash.com/photo-1598653222000-6b7b9a48efb3?w=800&q=80", description: "Analog, digital, modular", count: 15 },
      { name: "Drums", slug: "drums", coverImage: "https://images.unsplash.com/photo-1519892300165-cb5542fb47c7?w=800&q=80", description: "Kits, cymbals, percussion", count: 12 },
      { name: "Studio Gear", slug: "studio-gear", coverImage: "https://images.unsplash.com/photo-1598488035139-bdbb2231ce04?w=800&q=80", description: "Preamps, compressors, interfaces", count: 12 },
    ],
  },
  {
    name: "Gaming",
    slug: "gaming",
    coverImage: "https://images.unsplash.com/photo-1531525645387-7f14be1bdbbd?w=1200&q=80",
    description: "Consoles, cartridges, collector editions",
    count: 203,
    children: [
      { name: "Consoles", slug: "consoles", coverImage: "https://images.unsplash.com/photo-1531525645387-7f14be1bdbbd?w=800&q=80", description: "PlayStation, Nintendo, Xbox", count: 67 },
      { name: "Games", slug: "games", coverImage: "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=800&q=80", description: "Cartridges, discs, digital", count: 89 },
      { name: "Collectibles", slug: "collectibles", coverImage: "https://images.unsplash.com/photo-1606144042614-b2417e99c4e3?w=800&q=80", description: "Figurines, boards, merchandise", count: 28 },
      { name: "Arcade", slug: "arcade", coverImage: "https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=800&q=80", description: "Cabinets, boards, peripherals", count: 19 },
    ],
  },
  {
    name: "Jewelry",
    slug: "jewelry",
    coverImage: "https://images.unsplash.com/photo-1515562141207-7a88fb7ce338?w=1200&q=80",
    description: "Fine jewelry, statement pieces",
    count: 145,
    children: [
      { name: "Rings", slug: "rings", coverImage: "https://images.unsplash.com/photo-1605100804763-247f67b3557e?w=800&q=80", description: "Engagement, fashion, signets", count: 42 },
      { name: "Necklaces", slug: "necklaces", coverImage: "https://images.unsplash.com/photo-1599643478518-a784e5dc4c8f?w=800&q=80", description: "Chains, pendants, pearls", count: 38 },
      { name: "Watches", slug: "watches", coverImage: "https://images.unsplash.com/photo-1587836374828-4dbafa94cf0e?w=800&q=80", description: "Jewelry watches, dress watches", count: 35 },
      { name: "Earrings", slug: "earrings", coverImage: "https://images.unsplash.com/photo-1535632066927-ab7c9ab60908?w=800&q=80", description: "Studs, hoops, drops", count: 30 },
    ],
  },
  {
    name: "Collectibles",
    slug: "collectibles",
    coverImage: "https://images.unsplash.com/photo-1567427017947-545c5f8d16ad?w=1200&q=80",
    description: "Sports memo, trading cards, rare toys",
    count: 312,
    children: [
      { name: "Sports Memorabilia", slug: "sports-memorabilia", coverImage: "https://images.unsplash.com/photo-1567427017947-545c5f8d16ad?w=800&q=80", description: "Jerseys, balls, autographs", count: 89 },
      { name: "Trading Cards", slug: "trading-cards", coverImage: "https://images.unsplash.com/photo-1613771404784-3a5686aa2be3?w=800&q=80", description: "Pokemon, Sports, Magic", count: 112 },
      { name: "Toys", slug: "toys", coverImage: "https://images.unsplash.com/photo-1558060370-d644479cb6f7?w=800&q=80", description: "Action figures, diecast, dolls", count: 67 },
      { name: "Coins & Stamps", slug: "coins-stamps", coverImage: "https://images.unsplash.com/photo-1605792657660-596af9009e82?w=800&q=80", description: "Numismatic, philatelic", count: 44 },
    ],
  },
  {
    name: "Fashion",
    slug: "fashion",
    coverImage: "https://images.unsplash.com/photo-1445205170230-053b83016050?w=1200&q=80",
    description: "Designer pieces, vintage couture",
    count: 189,
    children: [
      { name: "Designer Bags", slug: "designer-bags", coverImage: "https://images.unsplash.com/photo-1548036328-c9fa89d128fa?w=800&q=80", description: "Louis Vuitton, Chanel, Hermes", count: 56 },
      { name: "Vintage", slug: "vintage", coverImage: "https://images.unsplash.com/photo-1558618666-fcd25c85cd64?w=800&q=80", description: "60s-90s pieces", count: 45 },
      { name: "Sneakers", slug: "sneakers", coverImage: "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=800&q=80", description: "Jordan, Nike, Adidas collabs", count: 52 },
      { name: "Accessories", slug: "accessories", coverImage: "https://images.unsplash.com/photo-1553062407-98eeb64c6a62?w=800&q=80", description: "Scarves, belts, sunglasses", count: 36 },
    ],
  },
];

// ─── Flat list for convenience ────────────────────────────────────────
export const categories = categoryTree;

// ─── Helpers ────────────────────────────────────────────────────────

/**
 * Find a category by a path of slugs (e.g., ['electronics', 'cameras'])
 * Returns the category and its parent tree
 */
export function findCategoryByPath(slugs: string[]): {
  category: Category | null;
  path: Category[];
  breadcrumb: { name: string; slug: string }[];
} {
  if (slugs.length === 0) {
    return { category: null, path: [], breadcrumb: [] };
  }

  let currentLevel: Category[] = categoryTree;
  const breadcrumb: { name: string; slug: string }[] = [];
  const path: Category[] = [];

  for (const slug of slugs) {
    const found = currentLevel.find(c => c.slug === slug);
    if (!found) {
      return { category: null, path, breadcrumb };
    }
    breadcrumb.push({ name: found.name, slug: found.slug });
    path.push(found);
    currentLevel = found.children || [];
  }

  return { category: path[path.length - 1], path, breadcrumb };
}

/**
 * Flatten category tree to all leaf categories with their full path
 */
export function flattenCategories(cats: Category[] = categoryTree, basePath: string[] = []): Array<{ category: Category; path: string[] }> {
  const result: Array<{ category: Category; path: string[] }> = [];

  for (const cat of cats) {
    const fullPath = [...basePath, cat.slug];
    if (cat.children && cat.children.length > 0) {
      result.push(...flattenCategories(cat.children, fullPath));
    } else {
      result.push({ category: cat, path: fullPath });
    }
  }

  return result;
}

// ─── Helpers ────────────────────────────────────────────────────────
export function formatCurrency(amount: number): string {
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: "USD",
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(amount);
}

export function formatTimeRemaining(endTime: Date): string {
  const now = new Date();
  const diff = endTime.getTime() - now.getTime();

  if (diff <= 0) return "Ended";

  const hours = Math.floor(diff / (1000 * 60 * 60));
  const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
  const days = Math.floor(hours / 24);

  if (days > 0) return `${days}d ${hours % 24}h`;
  if (hours > 0) return `${hours}h ${minutes}m`;
  return `${minutes}m`;
}

export function getTimeUrgency(endTime: Date): "critical" | "soon" | "normal" {
  const now = new Date();
  const hoursRemaining = (endTime.getTime() - now.getTime()) / (1000 * 60 * 60);

  if (hoursRemaining <= 1) return "critical";
  if (hoursRemaining <= 6) return "soon";
  return "normal";
}
