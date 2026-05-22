"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { useAuth } from "@/components/auth-provider";
import {
  listSessions,
  revokeOtherSessions,
  revokeSession,
} from "@/lib/auth/auth-api";
import type { SessionSummaryResponse } from "@/lib/auth/types";

function formatDate(value?: string) {
  if (!value) {
    return "Unknown";
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return date.toLocaleString();
}

function SessionCard({
  session,
  onRevoke,
  isRevoking,
}: {
  session: SessionSummaryResponse;
  onRevoke: (sessionId: string) => void;
  isRevoking: boolean;
}) {
  return (
    <div className={`border-2 border-black bg-white p-4 ${session.isCurrent ? "shadow-[6px_6px_0_#0046FF]" : "shadow-[4px_4px_0_#0A0A0A]"}`}>
      <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between gap-4">
        <div className="space-y-2">
          <div className="flex flex-wrap items-center gap-2">
            <h3 className="font-black text-lg uppercase tracking-tight">
              {session.deviceInfo || "Unknown device"}
            </h3>
            {session.isCurrent && (
              <span className="px-2 py-1 text-[10px] font-black uppercase tracking-widest bg-acid text-black border border-black">
                Current Session
              </span>
            )}
            {!session.isActive && (
              <span className="px-2 py-1 text-[10px] font-black uppercase tracking-widest bg-hot text-white border border-black">
                Inactive
              </span>
            )}
          </div>
          <p className="text-sm text-gray-600">
            {session.locationLabel || session.ipAddress || "Unknown location"}
          </p>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-2 text-xs text-gray-500">
            <span>Created: {formatDate(session.createdAt)}</span>
            <span>Last login: {formatDate(session.lastLoginAt)}</span>
            <span>Expires: {formatDate(session.expiresAt)}</span>
          </div>
        </div>

        <div className="flex flex-col items-start lg:items-end gap-2">
          <button
            type="button"
            onClick={() => onRevoke(session.sessionId)}
            disabled={isRevoking || session.isCurrent}
            className="btn btn-black text-xs font-bold uppercase tracking-wide disabled:opacity-50"
          >
            {session.isCurrent ? "Current Session" : isRevoking ? "Revoking..." : "Revoke"}
          </button>
          <p className="text-[10px] text-gray-500 uppercase tracking-widest">
            ID: {session.sessionId}
          </p>
        </div>
      </div>
    </div>
  );
}

export default function SessionsPage() {
  const router = useRouter();
  const { user, isAuthenticated, isHydrating } = useAuth();
  const [sessions, setSessions] = useState<SessionSummaryResponse[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [revokingSessionId, setRevokingSessionId] = useState<string | null>(null);
  const [isRevokingOthers, setIsRevokingOthers] = useState(false);

  const currentSessionCount = useMemo(
    () => sessions.filter((session) => session.isActive).length,
    [sessions],
  );

  useEffect(() => {
    if (!isHydrating && !isAuthenticated) {
      router.replace("/login");
    }
  }, [isAuthenticated, isHydrating, router]);

  useEffect(() => {
    let isMounted = true;

    async function loadSessions() {
      setError("");
      setIsLoading(true);

      try {
        const response = await listSessions();
        if (isMounted) {
          setSessions(response);
        }
      } catch (loadError) {
        if (isMounted) {
          setError(loadError instanceof Error ? loadError.message : "Failed to load sessions.");
        }
      } finally {
        if (isMounted) {
          setIsLoading(false);
        }
      }
    }

    loadSessions();

    return () => {
      isMounted = false;
    };
  }, []);

  const refreshSessions = async () => {
    setIsRefreshing(true);
    setError("");
    setSuccess("");

    try {
      const response = await listSessions();
      setSessions(response);
      setSuccess("Session list refreshed.");
    } catch (refreshError) {
      setError(refreshError instanceof Error ? refreshError.message : "Failed to refresh sessions.");
    } finally {
      setIsRefreshing(false);
    }
  };

  const handleRevoke = async (sessionId: string) => {
    if (!sessionId) {
      return;
    }

    setRevokingSessionId(sessionId);
    setError("");
    setSuccess("");

    try {
      await revokeSession(sessionId);
      setSessions((currentSessions) => currentSessions.filter((session) => session.sessionId !== sessionId));
      setSuccess("Session revoked successfully.");
    } catch (revokeError) {
      setError(revokeError instanceof Error ? revokeError.message : "Failed to revoke session.");
    } finally {
      setRevokingSessionId(null);
    }
  };

  const handleRevokeOthers = async () => {
    setIsRevokingOthers(true);
    setError("");
    setSuccess("");

    try {
      await revokeOtherSessions();
      setSessions((currentSessions) => currentSessions.filter((session) => session.isCurrent));
      setSuccess("All other sessions have been revoked.");
    } catch (revokeError) {
      setError(revokeError instanceof Error ? revokeError.message : "Failed to revoke other sessions.");
    } finally {
      setIsRevokingOthers(false);
    }
  };

  if (isHydrating || !isAuthenticated) {
    return (
      <div className="min-h-screen bg-gray-100 flex items-center justify-center px-4">
        <div className="border-3 border-black bg-white px-6 py-5 shadow-[8px_8px_0_#0A0A0A]">
          <p className="text-sm font-bold uppercase tracking-wide text-gray-700">Loading session dashboard...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-100">
      <div className="max-w-5xl mx-auto px-4 py-10 md:py-14">
        <div className="flex flex-col md:flex-row md:items-end md:justify-between gap-4 mb-8">
          <div>
            <Link href="/" className="inline-flex items-center gap-2 mb-4">
              <div className="w-9 h-9 bg-acid flex items-center justify-center border-2 border-black shadow-[3px_3px_0_#0A0A0A]">
                <span className="text-black font-black text-base">B</span>
              </div>
              <span className="text-xl font-black tracking-tight">BIDMART</span>
            </Link>
            <h1 className="text-3xl md:text-5xl font-black uppercase tracking-tighter text-black">
              Session Management
            </h1>
            <p className="text-gray-600 mt-2 max-w-2xl">
              Review active sessions, revoke a specific session, or sign out everywhere else.
            </p>
          </div>

          <div className="flex flex-col items-start md:items-end gap-2">
            <div className="border-2 border-black bg-white px-4 py-2 text-sm font-bold uppercase tracking-wide">
              {user?.displayName || user?.email || "Account"}
            </div>
            <div className="flex flex-wrap gap-2">
              <button
                type="button"
                onClick={refreshSessions}
                disabled={isRefreshing}
                className="btn btn-ghost text-xs font-bold uppercase tracking-wide"
              >
                {isRefreshing ? "Refreshing..." : "Refresh"}
              </button>
              <button
                type="button"
                onClick={handleRevokeOthers}
                disabled={isRevokingOthers}
                className="btn btn-black text-xs font-bold uppercase tracking-wide"
              >
                {isRevokingOthers ? "Revoking..." : "Sign Out Everywhere Else"}
              </button>
            </div>
          </div>
        </div>

        <div className="grid gap-3 mb-8 md:grid-cols-3">
          <div className="border-2 border-black bg-white p-4 shadow-[4px_4px_0_#0A0A0A]">
            <p className="text-[10px] font-black uppercase tracking-widest text-gray-500">Active Sessions</p>
            <p className="text-3xl font-black text-black">{currentSessionCount}</p>
          </div>
          <div className="border-2 border-black bg-white p-4 shadow-[4px_4px_0_#0A0A0A]">
            <p className="text-[10px] font-black uppercase tracking-widest text-gray-500">Current Session</p>
            <p className="text-3xl font-black text-electric">{sessions.some((session) => session.isCurrent) ? "Yes" : "No"}</p>
          </div>
          <div className="border-2 border-black bg-white p-4 shadow-[4px_4px_0_#0A0A0A]">
            <p className="text-[10px] font-black uppercase tracking-widest text-gray-500">Loaded Sessions</p>
            <p className="text-3xl font-black text-hot">{sessions.length}</p>
          </div>
        </div>

        {error && (
          <div className="mb-4 p-4 bg-hot/10 border-2 border-hot text-hot text-sm font-bold">
            {error}
          </div>
        )}

        {success && (
          <div className="mb-4 p-4 bg-acid/20 border-2 border-black text-black text-sm font-bold">
            {success}
          </div>
        )}

        {isLoading ? (
          <div className="border-3 border-black bg-white p-8 shadow-[8px_8px_0_#0A0A0A]">
            <p className="text-sm font-bold uppercase tracking-wide text-gray-600">Loading sessions...</p>
          </div>
        ) : sessions.length === 0 ? (
          <div className="border-3 border-black bg-white p-8 shadow-[8px_8px_0_#0A0A0A]">
            <h2 className="text-2xl font-black uppercase tracking-tighter mb-2">No Sessions Found</h2>
            <p className="text-gray-600 text-sm md:text-base">There are no active sessions available for this account.</p>
          </div>
        ) : (
          <div className="space-y-4">
            {sessions.map((session) => (
              <SessionCard
                key={session.sessionId}
                session={session}
                onRevoke={handleRevoke}
                isRevoking={revokingSessionId === session.sessionId}
              />
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
