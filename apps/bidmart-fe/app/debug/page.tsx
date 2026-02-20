"use client";

import { useEffect, useState } from "react";

type Status =
  | "UP"
  | "UNREACHABLE"
  | "CONNECTED"
  | "DISCONNECTED"
  | "ERROR"
  | "LOADING";

interface HealthData {
  service: string;
  status: string;
  auction?: {
    service?: string;
    status?: string;
    core?: {
      service?: string;
      status?: string;
      db?: string;
      error?: string;
    };
    error?: string;
  };
}

function Badge({ status }: { status: Status }) {
  const color: Record<Status, string> = {
    UP: "bg-green-500",
    CONNECTED: "bg-green-500",
    LOADING: "bg-yellow-500 animate-pulse",
    UNREACHABLE: "bg-red-500",
    DISCONNECTED: "bg-red-500",
    ERROR: "bg-red-500",
  };
  return (
    <span
      className={`inline-block w-3 h-3 rounded-full ${color[status] ?? "bg-gray-500"}`}
    />
  );
}

export default function DebugPage() {
  const [data, setData] = useState<HealthData | null>(null);
  const [loading, setLoading] = useState(true);

  const fetchHealth = async () => {
    setLoading(true);
    try {
      const res = await fetch("/api/health", { cache: "no-store" });
      setData(await res.json());
    } catch {
      setData(null);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchHealth();
  }, []);

  const feStatus: Status = loading ? "LOADING" : "UP";
  const auctionStatus: Status = loading
    ? "LOADING"
    : ((data?.auction?.status as Status) ?? "UNREACHABLE");
  const coreStatus: Status = loading
    ? "LOADING"
    : ((data?.auction?.core?.status as Status) ?? "UNREACHABLE");
  const dbStatus: Status = loading
    ? "LOADING"
    : ((data?.auction?.core?.db as Status) ?? "DISCONNECTED");

  return (
    <div className="min-h-screen bg-gray-950 text-white p-8 font-mono">
      <h1 className="text-2xl font-bold mb-6">Debug — Chained Health Check</h1>
      <p className="text-gray-400 mb-8 text-sm">FE → Auction → Core → DB</p>

      <div className="space-y-4 max-w-md">
        <Row label="Frontend (Next.js)" status={feStatus} />
        <Arrow />
        <Row label="Auction (Rust/Axum)" status={auctionStatus} />
        <Arrow />
        <Row label="Core (Spring Boot)" status={coreStatus} />
        <Arrow />
        <Row label="PostgreSQL" status={dbStatus} />
      </div>

      {!loading && data?.auction?.core?.error && (
        <p className="mt-6 text-red-400 text-sm">
          Error: {data.auction.core.error}
        </p>
      )}
      {!loading && data?.auction?.error && (
        <p className="mt-2 text-red-400 text-sm">Error: {data.auction.error}</p>
      )}

      <button
        onClick={fetchHealth}
        className="mt-8 px-4 py-2 bg-blue-600 rounded hover:bg-blue-500 text-sm"
      >
        Refresh
      </button>

      {!loading && data && (
        <pre className="mt-8 bg-gray-900 p-4 rounded text-xs overflow-auto max-w-lg">
          {JSON.stringify(data, null, 2)}
        </pre>
      )}
    </div>
  );
}

function Row({ label, status }: { label: string; status: Status }) {
  return (
    <div className="flex items-center gap-3 bg-gray-900 px-4 py-3 rounded">
      <Badge status={status} />
      <span>{label}</span>
      <span className="ml-auto text-xs text-gray-400">{status}</span>
    </div>
  );
}

function Arrow() {
  return <div className="text-center text-gray-600">↓</div>;
}
