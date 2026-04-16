import Link from "next/link";

export default function NotFound() {
  return (
    <div className="min-h-screen flex items-center justify-center bg-white relative overflow-hidden py-24">
      <div className="absolute inset-0 grid-bg opacity-30"></div>

      <div className="absolute top-20 left-20 w-32 h-32 border-[3px] border-acid rotate-12 opacity-50"></div>
      <div className="absolute bottom-40 right-20 w-48 h-48 border-[3px] border-electric -rotate-12 opacity-50"></div>
      <div className="absolute top-1/4 right-1/4 w-16 h-16 bg-hot/10 rotate-45"></div>
      <div className="absolute bottom-1/3 left-1/4 w-8 h-8 bg-electric/20 rotate-12"></div>

      <div className="relative z-10 text-center px-6">
        <div className="mb-8">
          <h1 className="text-[12rem] md:text-[16rem] lg:text-[20rem] font-black text-black leading-none tracking-tighter select-none">
            404
          </h1>
          <div className="h-3 bg-hot mx-auto -mt-8 md:-mt-12 lg:-mt-16"></div>
        </div>

        <div className="mb-10">
          <h2 className="text-3xl md:text-4xl lg:text-5xl font-black text-black uppercase tracking-tight mb-4">
            PAGE NOT FOUND
          </h2>
          <p className="text-lg md:text-xl text-gray-500 max-w-md mx-auto leading-relaxed">
            Looks like this auction ended before you could bid. The page you&apos;re looking for doesn&apos;t exist.
          </p>
        </div>

        <div className="flex flex-wrap items-center justify-center gap-4">
          <Link href="/" className="btn btn-black text-base md:text-lg py-4 px-8 font-bold uppercase tracking-wide">
            Back to Home
          </Link>
          <Link href="/categories" className="btn bg-white text-black border-3 border-black hover:bg-gray-100 text-base md:text-lg py-4 px-8 font-bold uppercase tracking-wide">
            Browse Auctions
          </Link>
        </div>

        <div className="mt-12 p-6 border-3 border-black bg-gray-50 shadow-[5px_5px_0_#0A0A0A] max-w-md mx-auto">
          <p className="text-sm font-bold text-black uppercase tracking-wide mb-2">
            Try These Instead
          </p>
          <div className="flex flex-wrap justify-center gap-2">
            {["Electronics", "Watches", "Art", "Gaming", "Furniture"].map((cat) => (
              <Link
                key={cat}
                href={`/categories/${cat.toLowerCase()}`}
                className="tag hover:bg-acid transition-colors"
              >
                {cat}
              </Link>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}
