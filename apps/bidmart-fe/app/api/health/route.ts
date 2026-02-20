import { NextResponse } from "next/server";

export async function GET() {
  const auctionUrl = process.env.AUCTION_SERVICE_URL || "http://localhost:8081";

  try {
    const res = await fetch(`${auctionUrl}/api/health/status`, {
      cache: "no-store",
    });
    const data = await res.json();

    return NextResponse.json({
      service: "bidmart-fe",
      status: "UP",
      auction: data,
    });
  } catch (e: unknown) {
    const message = e instanceof Error ? e.message : String(e);
    return NextResponse.json({
      service: "bidmart-fe",
      status: "UP",
      auction: { status: "UNREACHABLE", error: message },
    });
  }
}
