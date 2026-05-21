import Link from "next/link";

const pillars = [
  { title: "Verified Sellers", body: "Every seller completes identity verification before listing their first item. We review listings for accuracy and remove anything that violates our standards." },
  { title: "Wallet Protection", body: "Your funds are held securely in your BidMart wallet. Payments are only released to sellers after you confirm delivery — never before." },
  { title: "Binding Bids", body: "All bids are binding and backed by your wallet balance. This keeps auctions honest and protects sellers from bad-faith bidding." },
  { title: "Dispute Resolution", body: "If something goes wrong — wrong item, damaged goods, non-delivery — open a dispute and our team steps in to mediate and protect your funds." },
  { title: "Anti-Snipe Protection", body: "Bids placed in the last 2 minutes automatically extend the auction by 2 minutes. No last-second stealing — every bidder gets a fair shot." },
];

export default function TrustPage() {
  return (
    <main className="max-w-3xl mx-auto px-4 py-14">
      <div className="mb-10">
        <div className="inline-block bg-acid border-3 border-black px-4 py-1 font-black text-xs uppercase tracking-widest mb-3">Safety</div>
        <h1 className="text-5xl font-black uppercase tracking-tight">Trust & Safety</h1>
        <p className="text-gray-500 mt-3 text-lg font-medium">BidMart is built on the principle that every transaction should be safe, transparent, and fair — for buyers and sellers alike.</p>
      </div>

      <div className="space-y-4 mb-12">
        {pillars.map((p, i) => (
          <div key={i} className="border-3 border-black p-5 shadow-[3px_3px_0_#0A0A0A]">
            <h3 className="font-black text-base uppercase mb-2">{p.title}</h3>
            <p className="text-gray-600 font-medium text-sm leading-relaxed">{p.body}</p>
          </div>
        ))}
      </div>

      <div className="border-3 border-black p-6 bg-black text-white text-center">
        <p className="font-black text-lg uppercase mb-1">Questions about safety?</p>
        <p className="text-gray-400 text-sm font-medium mb-4">Our team is here to help.</p>
        <Link href="/contact" className="btn btn-acid font-bold uppercase text-sm text-black">Get in Touch</Link>
      </div>
    </main>
  );
}
