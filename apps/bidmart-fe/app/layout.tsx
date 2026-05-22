import type { Metadata } from "next";
import { DM_Sans } from "next/font/google";
import "./globals.css";
import Link from "next/link";
import Script from "next/script";
import { AuthProvider } from "@/components/auth-provider";
import { AuthNavActions } from "@/components/auth-nav-actions";
import { NotificationBell } from "@/components/notification-bell";
import { Search } from "lucide-react";
import { ToastProvider } from "@/components/toast";

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
              <Search className="w-5 h-5" />
            </button>
            <Link href="/listing/new" className="w-10 h-10 flex items-center justify-center border-2 border-black shadow-[3px_3px_0_#0A0A0A] hover:shadow-[5px_5px_0_#0A0A0A] hover:translate-x-[-2px] hover:translate-y-[-2px] transition-all bg-white">
              <span className="text-lg font-black leading-none">+</span>
            </Link>
            <NotificationBell />
            <AuthNavActions />
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
  const clarityId = process.env.NEXT_PUBLIC_CLARITY_ID;

  return (
    <html lang="en">
      <body className={`${dmSans.variable} antialiased`}>
        {clarityId && (
          <Script id="clarity" strategy="afterInteractive">{`
            (function(c,l,a,r,i,t,y){
              c[a]=c[a]||function(){(c[a].q=c[a].q||[]).push(arguments)};
              t=l.createElement(r);t.async=1;t.src="https://www.clarity.ms/tag/"+i;
              y=l.getElementsByTagName(r)[0];y.parentNode.insertBefore(t,y);
            })(window,document,"clarity","script","${clarityId}");
          `}</Script>
        )}
        <AuthProvider>

          <ToastProvider>
            <div className="min-h-screen flex flex-col bg-white">
              <NavBar />
              <MarqueeStrip />
              <main className="flex-1">{children}</main>
              <Footer />
            </div>
          </ToastProvider>
        </AuthProvider>
      </body>
    </html>
  );
}
