export default function Home() {
  return (
    <div className="min-h-screen bg-black flex flex-col items-center justify-center px-4 relative overflow-hidden">
      <div className="absolute top-0 right-0 w-96 h-96 bg-blue-600 rounded-full mix-blend-multiply filter blur-3xl opacity-20 animate-pulse"></div>
      <div className="absolute bottom-0 left-0 w-96 h-96 bg-blue-500 rounded-full mix-blend-multiply filter blur-3xl opacity-20 animate-pulse"></div>

      <main className="relative z-10 text-center space-y-8">
        <div className="mb-8">
          <h1
            className="text-7xl md:text-8xl font-bold mb-4 bg-gradient-to-r from-blue-400 via-blue-500 to-blue-600 bg-clip-text text-transparent drop-shadow-lg"
            style={{
              textShadow:
                "0 0 20px rgba(59, 130, 246, 0.5), 0 0 40px rgba(59, 130, 246, 0.3)",
            }}
          >
            BIDMART
          </h1>
          <div className="h-1 w-24 bg-gradient-to-r from-blue-400 to-blue-600 mx-auto"></div>
        </div>

        <div>
          <p className="text-3xl md:text-4xl font-light text-blue-300 mb-4 tracking-widest">
            COMING SOON
          </p>
          <p className="text-xl md:text-2xl text-gray-300 max-w-2xl mx-auto leading-relaxed">
            Something extraordinary is being crafted. Get ready for the next
            revolution in marketplace innovation.
          </p>
        </div>

        <div className="pt-16 text-gray-500 text-sm">
          <p>© 2026 B13. All rights reserved.</p>
        </div>
      </main>
    </div>
  );
}
