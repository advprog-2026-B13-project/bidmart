import Link from "next/link";

const cookieTypes = [
  { name: "Essential Cookies", required: true, desc: "Required for the platform to function. These handle authentication, session management, and security. You cannot opt out of these." },
  { name: "Analytics Cookies", required: false, desc: "Help us understand how users interact with BidMart — which pages are visited, how long sessions last, and where users drop off. Used to improve the platform." },
  { name: "Preference Cookies", required: false, desc: "Remember your settings such as language, timezone, and display preferences across sessions." },
];

export default function CookiesPage() {
  return (
    <main className="max-w-3xl mx-auto px-4 py-14">
      <div className="mb-10">
        <div className="inline-block bg-acid border-3 border-black px-4 py-1 font-black text-xs uppercase tracking-widest mb-3">Legal</div>
        <h1 className="text-5xl font-black uppercase tracking-tight">Cookie Policy</h1>
        <p className="text-gray-500 mt-3 font-medium">Last updated: January 2026</p>
      </div>

      <div className="border-3 border-black p-5 mb-8 shadow-[4px_4px_0_#0A0A0A]">
        <p className="font-medium text-sm leading-relaxed text-gray-700">
          Cookies are small text files stored in your browser. BidMart uses cookies to keep you logged in, remember your preferences, and understand how the platform is used. By continuing to use BidMart, you accept our use of essential cookies.
        </p>
      </div>

      <div className="space-y-4 mb-12">
        {cookieTypes.map((c, i) => (
          <div key={i} className="border-3 border-black p-5 shadow-[3px_3px_0_#0A0A0A]">
            <div className="flex items-center gap-3 mb-2">
              <h3 className="font-black text-sm uppercase">{c.name}</h3>
              <span className={`text-xs font-black uppercase px-2 py-0.5 border-2 border-black ${c.required ? "bg-black text-white" : "bg-gray-100"}`}>
                {c.required ? "Required" : "Optional"}
              </span>
            </div>
            <p className="text-gray-600 font-medium text-sm leading-relaxed">{c.desc}</p>
          </div>
        ))}
      </div>

      <p className="text-sm text-gray-500 font-medium mb-6">
        To manage or delete cookies, use your browser settings. Note that disabling essential cookies will prevent you from logging in.
      </p>

      <div className="flex gap-4 text-sm font-black uppercase">
        <Link href="/privacy" className="underline hover:text-acid transition-colors">Privacy Policy</Link>
        <Link href="/terms" className="underline hover:text-acid transition-colors">Terms of Service</Link>
      </div>
    </main>
  );
}
