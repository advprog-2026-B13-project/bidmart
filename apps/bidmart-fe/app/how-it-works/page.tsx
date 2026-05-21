import Link from "next/link";

const steps = [
  { n: "01", title: "Browse Listings", body: "Explore hundreds of unique items across every category. Filter by price, time remaining, or what's trending." },
  { n: "02", title: "Place Your Bid", body: "Enter your maximum bid and let BidMart's proxy system work for you — it bids automatically up to your limit." },
  { n: "03", title: "Win & Pay", body: "If you're the highest bidder when the clock hits zero, the item is yours. Pay securely through our platform." },
  { n: "04", title: "Get Delivered", body: "Sellers ship directly to you. Track your order in real time and confirm delivery to release payment." },
];

export default function HowItWorksPage() {
  return (
    <main className="max-w-4xl mx-auto px-4 py-14">
      <div className="mb-10">
        <div className="inline-block bg-acid border-3 border-black px-4 py-1 font-black text-xs uppercase tracking-widest mb-3">Guide</div>
        <h1 className="text-5xl font-black uppercase tracking-tight">How It Works</h1>
        <p className="text-gray-500 mt-3 text-lg font-medium">BidMart auctions are simple, transparent, and built for bold bidders.</p>
      </div>

      <div className="grid gap-6 mb-12">
        {steps.map(s => (
          <div key={s.n} className="flex gap-6 border-3 border-black p-6 shadow-[4px_4px_0_#0A0A0A]">
            <span className="text-5xl font-black text-acid leading-none shrink-0">{s.n}</span>
            <div>
              <h2 className="font-black text-xl uppercase mb-1">{s.title}</h2>
              <p className="text-gray-600 font-medium">{s.body}</p>
            </div>
          </div>
        ))}
      </div>

      <div className="border-3 border-black p-8 bg-acid">
        <h3 className="font-black text-2xl uppercase mb-2">Ready to bid?</h3>
        <p className="font-medium mb-4">Thousands of items are live right now. Your next great find is one bid away.</p>
        <Link href="/" className="btn btn-black font-bold uppercase">Browse Auctions</Link>
      </div>
    </main>
  );
}
