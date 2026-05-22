import Link from "next/link";

const sections = [
  { title: "What We Collect", body: "We collect information you provide directly: name, email, password, and payment details. We also collect usage data (pages visited, bids placed, listings viewed) to improve the platform." },
  { title: "How We Use It", body: "Your data is used to operate the marketplace, process transactions, send order updates, and prevent fraud. We do not sell your personal data to third parties." },
  { title: "Cookies", body: "We use essential cookies for authentication and session management, and optional analytics cookies to understand how the platform is used. See our Cookie Policy for details." },
  { title: "Data Sharing", body: "We share data with payment processors (Midtrans) to complete transactions, and with shipping providers when an order is placed. We may disclose data if required by law." },
  { title: "Data Retention", body: "Account data is retained for as long as your account is active. Transaction records are kept for 7 years for legal compliance. You may request deletion of non-transaction data at any time." },
  { title: "Your Rights", body: "You have the right to access, correct, or delete your personal data. To exercise these rights, contact us at privacy@bidmart.store." },
  { title: "Security", body: "We use industry-standard encryption for data in transit and at rest. However, no system is completely secure — report any suspected breach immediately." },
  { title: "Changes", body: "We may update this policy at any time. Significant changes will be communicated by email or in-app notification." },
];

export default function PrivacyPage() {
  return (
    <main className="max-w-3xl mx-auto px-4 py-14">
      <div className="mb-10">
        <div className="inline-block bg-acid border-3 border-black px-4 py-1 font-black text-xs uppercase tracking-widest mb-3">Legal</div>
        <h1 className="text-5xl font-black uppercase tracking-tight">Privacy Policy</h1>
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
        <Link href="/terms" className="underline hover:text-acid transition-colors">Terms of Service</Link>
        <Link href="/cookies" className="underline hover:text-acid transition-colors">Cookie Policy</Link>
        <Link href="/contact" className="underline hover:text-acid transition-colors">Contact Us</Link>
      </div>
    </main>
  );
}
