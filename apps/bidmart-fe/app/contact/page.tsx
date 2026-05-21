import Link from "next/link";

export default function ContactPage() {
  return (
    <main className="max-w-2xl mx-auto px-4 py-14">
      <div className="mb-10">
        <div className="inline-block bg-acid border-3 border-black px-4 py-1 font-black text-xs uppercase tracking-widest mb-3">Contact</div>
        <h1 className="text-5xl font-black uppercase tracking-tight">Get in Touch</h1>
        <p className="text-gray-500 mt-3 text-lg font-medium">Have a question, issue, or feedback? We're a small team and we actually read every message.</p>
      </div>

      <div className="space-y-4 mb-10">
        <div className="border-3 border-black p-6 shadow-[4px_4px_0_#0A0A0A]">
          <h3 className="font-black text-sm uppercase tracking-wider mb-1">General Support</h3>
          <p className="text-gray-500 text-sm font-medium mb-2">Bid issues, account problems, delivery disputes.</p>
          <a href="mailto:support@bidmart.store" className="font-black text-base hover:text-acid transition-colors">support@bidmart.store</a>
        </div>
        <div className="border-3 border-black p-6 shadow-[4px_4px_0_#0A0A0A]">
          <h3 className="font-black text-sm uppercase tracking-wider mb-1">Seller Inquiries</h3>
          <p className="text-gray-500 text-sm font-medium mb-2">Questions about listing, fees, or seller verification.</p>
          <a href="mailto:sellers@bidmart.store" className="font-black text-base hover:text-acid transition-colors">sellers@bidmart.store</a>
        </div>
        <div className="border-3 border-black p-6 shadow-[4px_4px_0_#0A0A0A]">
          <h3 className="font-black text-sm uppercase tracking-wider mb-1">Trust & Safety</h3>
          <p className="text-gray-500 text-sm font-medium mb-2">Report fraud, abuse, or policy violations.</p>
          <a href="mailto:safety@bidmart.store" className="font-black text-base hover:text-acid transition-colors">safety@bidmart.store</a>
        </div>
      </div>

      <p className="text-sm text-gray-500 font-medium">We respond within one business day. For urgent matters, include "URGENT" in your subject line.</p>

      <div className="mt-8">
        <Link href="/help" className="text-sm font-black uppercase underline hover:text-acid transition-colors">Check Help Center first →</Link>
      </div>
    </main>
  );
}
