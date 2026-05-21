import Link from "next/link";

const faqs = [
  { q: "How do I place a bid?", a: "Open any active listing, enter your bid amount (must exceed the current price by the minimum increment), and click Bid. Your bid is held from your wallet balance immediately." },
  { q: "What is a proxy bid?", a: "A proxy bid lets you set your maximum price. BidMart automatically bids on your behalf up to that limit — you only pay as much as needed to stay in the lead." },
  { q: "Can I cancel a bid?", a: "Bids are binding once placed. Please bid only what you're willing to pay. Contact support if you believe there was a technical error." },
  { q: "How does payment work?", a: "Your bid amount is held from your wallet when you win. Funds are released to the seller after you confirm delivery. Top up your wallet anytime under Settings." },
  { q: "What if an item doesn't arrive?", a: "Open a dispute within 7 days of the estimated delivery date. BidMart holds the funds until the issue is resolved." },
  { q: "How do I sell on BidMart?", a: "Create a listing from the + button in the navigation bar. Set your starting price, reserve price, and auction duration. Your item goes live immediately after submission." },
];

export default function HelpPage() {
  return (
    <main className="max-w-3xl mx-auto px-4 py-14">
      <div className="mb-10">
        <div className="inline-block bg-acid border-3 border-black px-4 py-1 font-black text-xs uppercase tracking-widest mb-3">Support</div>
        <h1 className="text-5xl font-black uppercase tracking-tight">Help Center</h1>
        <p className="text-gray-500 mt-3 text-lg font-medium">Answers to the most common questions about buying and selling on BidMart.</p>
      </div>

      <div className="space-y-4 mb-12">
        {faqs.map((faq, i) => (
          <div key={i} className="border-3 border-black p-5 shadow-[3px_3px_0_#0A0A0A]">
            <h3 className="font-black text-base uppercase mb-2">{faq.q}</h3>
            <p className="text-gray-600 font-medium text-sm leading-relaxed">{faq.a}</p>
          </div>
        ))}
      </div>

      <div className="border-3 border-black p-6 text-center">
        <p className="font-black text-lg uppercase mb-1">Still need help?</p>
        <p className="text-gray-500 text-sm font-medium mb-4">Reach out and we&apos;ll get back to you within one business day.</p>
        <Link href="/contact" className="btn btn-black font-bold uppercase text-sm">Contact Us</Link>
      </div>
    </main>
  );
}
