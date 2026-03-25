import type { Metadata } from "next";
import { DM_Sans } from "next/font/google";
import "./globals.css";
import Link from "next/link";

const dmSans = DM_Sans({
  variable: "--font-dm-sans",
  subsets: ["latin"],
  weight: ["400", "500", "600", "700", "800"],
});

export const metadata: Metadata = {
  title: "Bidmart — Where Bidders Win",
  description: "The premium auction marketplace. Discover unique items, bid with confidence.",
  icons: {
    icon: "/favicon.ico",
  },
};

function MarqueeStrip() {
  const items = [
    "FREE SHIPPING OVER $500",
    "BUYER PROTECTION GUARANTEE",
    "VERIFIED SELLERS ONLY",
    "24/7 SUPPORT",
    "ANTI-SNIPE PROTECTION",
  ];

  return (
    <div className="marquee-strip">
      <div className="marquee-strip-inner">
        {[...items, ...items].map((item, i) => (
          <span key={i} className="marquee-strip-item">{item}</span>
        ))}
      </div>
    </div>
  );
}

function NavBar() {
  return (
    <nav className="sticky top-0 z-50 bg-white border-b-3 border-black">
      <div className="max-w-7xl mx-auto px-4">
        <div className="flex items-center justify-between h-16">
          <Link href="/" className="flex items-center gap-2 group">
            <div className="w-10 h-10 bg-acid flex items-center justify-center border-2 border-black shadow-[3px_3px_0_#0A0A0A] group-hover:shadow-[5px_5px_0_#0A0A0A] group-hover:translate-x-[-2px] group-hover:translate-y-[-2px] transition-all">
              <span className="text-black font-black text-lg">B</span>
            </div>
            <span className="text-2xl font-black tracking-tight">BIDMART</span>
          </Link>

          <div className="hidden md:flex items-center gap-1">
            <Link href="/" className="px-4 py-2 font-bold text-sm uppercase tracking-wide hover:bg-gray-100 transition-colors">
              Browse
            </Link>
            <Link href="/categories" className="px-4 py-2 font-bold text-sm uppercase tracking-wide text-gray-500 hover:text-black hover:bg-gray-100 transition-colors">
              Categories
            </Link>
            <Link href="/how-it-works" className="px-4 py-2 font-bold text-sm uppercase tracking-wide text-gray-500 hover:text-black hover:bg-gray-100 transition-colors">
              How It Works
            </Link>
          </div>

          <div className="flex items-center gap-2">
            <button className="w-10 h-10 flex items-center justify-center border-2 border-black shadow-[3px_3px_0_#0A0A0A] hover:shadow-[5px_5px_0_#0A0A0A] hover:translate-x-[-2px] hover:translate-y-[-2px] transition-all bg-white">
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
              </svg>
            </button>
            <button className="relative w-10 h-10 flex items-center justify-center border-2 border-black shadow-[3px_3px_0_#0A0A0A] hover:shadow-[5px_5px_0_#0A0A0A] hover:translate-x-[-2px] hover:translate-y-[-2px] transition-all bg-white">
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
              </svg>
              <span className="absolute -top-1 -right-1 w-4 h-4 bg-hot text-white text-[10px] font-black flex items-center justify-center">3</span>
            </button>
            <Link href="/login" className="btn btn-ghost btn-sm">
              Sign In
            </Link>
            <Link href="/register" className="btn btn-acid btn-sm">
              Join Free
            </Link>
          </div>
        </div>
      </div>
    </nav>
  );
}

function Footer() {
  return (
    <footer className="border-t-3 border-black  bg-black text-white">
      <div className="max-w-7xl mx-auto px-4 py-16">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-8">
          <div className="col-span-2 md:col-span-1">
            <div className="flex items-center gap-2 mb-6">
              <div className="w-10 h-10 bg-acid flex items-center justify-center border-2 border-black">
                <span className="text-black font-black text-lg">B</span>
              </div>
              <span className="text-2xl font-black tracking-tight text-white">BIDMART</span>
            </div>
            <p className="text-gray-400 text-sm leading-relaxed">
              The auction marketplace for bold bidders. Unique items, verified sellers, guaranteed protection.
            </p>
          </div>
          <div>
            <h4 className="font-black text-sm uppercase tracking-wider mb-4 text-white">Marketplace</h4>
            <ul className="space-y-2">
              <li><Link href="/" className="text-gray-400 hover:text-acid transition-colors text-sm font-medium">Browse All</Link></li>
              <li><Link href="/categories" className="text-gray-400 hover:text-acid transition-colors text-sm font-medium">Categories</Link></li>
              <li><Link href="/new" className="text-gray-400 hover:text-acid transition-colors text-sm font-medium">New Listings</Link></li>
              <li><Link href="/ending-soon" className="text-gray-400 hover:text-acid transition-colors text-sm font-medium">Ending Soon</Link></li>
            </ul>
          </div>
          <div>
            <h4 className="font-black text-sm uppercase tracking-wider mb-4 text-white">Support</h4>
            <ul className="space-y-2">
              <li><Link href="/help" className="text-gray-400 hover:text-acid transition-colors text-sm font-medium">Help Center</Link></li>
              <li><Link href="/how-it-works" className="text-gray-400 hover:text-acid transition-colors text-sm font-medium">How It Works</Link></li>
              <li><Link href="/trust" className="text-gray-400 hover:text-acid transition-colors text-sm font-medium">Trust & Safety</Link></li>
              <li><Link href="/contact" className="text-gray-400 hover:text-acid transition-colors text-sm font-medium">Contact Us</Link></li>
            </ul>
          </div>
          <div>
            <h4 className="font-black text-sm uppercase tracking-wider mb-4 text-white">Legal</h4>
            <ul className="space-y-2">
              <li><Link href="/terms" className="text-gray-400 hover:text-acid transition-colors text-sm font-medium">Terms</Link></li>
              <li><Link href="/privacy" className="text-gray-400 hover:text-acid transition-colors text-sm font-medium">Privacy</Link></li>
              <li><Link href="/cookies" className="text-gray-400 hover:text-acid transition-colors text-sm font-medium">Cookies</Link></li>
            </ul>
          </div>
        </div>
        <div className="h-3 bg-acid mt-12"></div>
        <div className="flex flex-col md:flex-row justify-between items-center gap-4 mt-8 text-sm text-gray-400">
          <p>© 2026 Bidmart. All rights reserved.</p>
          <p className="text-acid font-bold">BOLD BIDS WIN</p>
        </div>
      </div>
    </footer>
  );
}

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en">
      <body className={`${dmSans.variable} antialiased`}>
        <div className="min-h-screen flex flex-col bg-white">
          <NavBar />
          <MarqueeStrip />
          <main className="flex-1">{children}</main>
          <Footer />
        </div>
      </body>
    </html>
  );
}
