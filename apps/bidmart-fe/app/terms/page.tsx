import Link from "next/link";

const sections = [
  { title: "Acceptance", body: "By accessing or using BidMart, you agree to these Terms. If you do not agree, do not use the platform." },
  { title: "Eligibility", body: "You must be at least 18 years old and legally capable of entering a binding contract to use BidMart." },
  { title: "Binding Bids", body: "All bids placed on BidMart are legally binding offers to purchase. By placing a bid, you commit to completing the transaction if you win. Withdrawal of a winning bid without cause may result in account suspension." },
  { title: "Seller Obligations", body: "Sellers must accurately describe items, ship within the agreed timeframe, and honor the winning bid price. Misrepresentation may result in account termination and liability." },
  { title: "Fees", body: "BidMart charges a platform fee on completed transactions. Fee rates are displayed at the time of listing. BidMart reserves the right to update fees with 30 days notice." },
  { title: "Prohibited Items", body: "You may not list counterfeit goods, stolen property, hazardous materials, or any items prohibited by applicable law. Violations result in immediate listing removal and possible account ban." },
  { title: "Dispute Resolution", body: "Disputes between buyers and sellers are handled by BidMart's Trust & Safety team. BidMart's decision in disputes is final for transactions processed through the platform wallet." },
  { title: "Limitation of Liability", body: "BidMart is a marketplace platform and is not responsible for the quality, safety, legality, or delivery of listed items. Use the platform at your own risk." },
  { title: "Changes to Terms", body: "We may update these Terms at any time. Continued use of BidMart after changes constitutes acceptance of the new Terms." },
];

export default function TermsPage() {
  return (
    <main className="max-w-3xl mx-auto px-4 py-14">
      <div className="mb-10">
        <div className="inline-block bg-acid border-3 border-black px-4 py-1 font-black text-xs uppercase tracking-widest mb-3">Legal</div>
        <h1 className="text-5xl font-black uppercase tracking-tight">Terms of Service</h1>
        <p className="text-gray-500 mt-3 font-medium">Last updated: January 2026</p>
      </div>

      <div className="space-y-6 mb-12">
        {sections.map((s, i) => (
          <div key={i}>
            <h2 className="font-black text-base uppercase mb-2">{s.title}</h2>
            <p className="text-gray-600 font-medium text-sm leading-relaxed">{s.body}</p>
            <div className="mt-4 h-px bg-gray-200" />
          </div>
        ))}
      </div>

      <div className="flex gap-4 text-sm font-black uppercase">
        <Link href="/privacy" className="underline hover:text-acid transition-colors">Privacy Policy</Link>
        <Link href="/cookies" className="underline hover:text-acid transition-colors">Cookie Policy</Link>
        <Link href="/contact" className="underline hover:text-acid transition-colors">Contact Us</Link>
      </div>
    </main>
  );
}
