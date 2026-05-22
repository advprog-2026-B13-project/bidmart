"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { deactivateAccount, listAdminManagedUsers } from "@/lib/auth/auth-api";
import type { AdminManagedUserResponse } from "@/lib/auth/types";
import { useAuth } from "@/components/auth-provider";

function getErrorMessage(error: unknown) {
  if (error instanceof Error && error.message) {
    return error.message;
  }
  return "Request failed. Please try again.";
}

export default function AdminDeactivatePage() {
  const router = useRouter();
  const { isAuthenticated, isHydrating } = useAuth();

  const [users, setUsers] = useState<AdminManagedUserResponse[]>([]);
  const [page, setPage] = useState(0);
  const [size] = useState(20);
  const [totalPages, setTotalPages] = useState(1);
  const [hasNext, setHasNext] = useState(false);
  const [isLoading, setIsLoading] = useState(true);
  const [deactivatingUserId, setDeactivatingUserId] = useState<string | null>(null);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  useEffect(() => {
    if (!isHydrating && !isAuthenticated) {
      router.replace("/login");
    }
  }, [isAuthenticated, isHydrating, router]);

  const loadUsers = useCallback(async (targetPage: number) => {
    setError("");
    setIsLoading(true);

    try {
      const response = await listAdminManagedUsers(targetPage, size);
      setUsers(response.users || []);
      setTotalPages(response.totalPages || 1);
      setHasNext(Boolean(response.hasNext));
    } catch (loadError) {
      setError(getErrorMessage(loadError));
    } finally {
      setIsLoading(false);
    }
  }, [size]);

  useEffect(() => {
    loadUsers(page);
  }, [loadUsers, page]);

  const handleDeactivate = async (user: AdminManagedUserResponse) => {
    if (!user.userId) {
      setError("Invalid user id.");
      return;
    }

    if (!window.confirm(`Deactivate account ${user.email}? This revokes all active sessions.`)) {
      return;
    }

    setError("");
    setSuccess("");
    setDeactivatingUserId(user.userId);

    try {
      await deactivateAccount(user.userId);
      setSuccess(`Account ${user.email} has been deactivated.`);
      await loadUsers(page);
    } catch (deactivateError) {
      setError(getErrorMessage(deactivateError));
    } finally {
      setDeactivatingUserId(null);
    }
  };

  if (isHydrating || !isAuthenticated) {
    return (
      <div className="min-h-screen bg-gray-100 flex items-center justify-center px-4">
        <div className="border-3 border-black bg-white px-6 py-5 shadow-[8px_8px_0_#0A0A0A]">
          <p className="text-sm font-bold uppercase tracking-wide text-gray-700">Loading account deactivation...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-100 px-4 py-10">
      <div className="max-w-6xl mx-auto space-y-6">
        <div className="flex items-center justify-between gap-3 flex-wrap">
          <div>
            <p className="text-[10px] font-black uppercase tracking-widest text-gray-500">Admin</p>
            <h1 className="text-4xl font-black uppercase tracking-tighter">Deactivate Accounts</h1>
          </div>
          <Link href="/admin" className="btn btn-ghost text-xs font-bold uppercase">Back to Admin</Link>
        </div>

        <div className="p-4 border-2 border-black bg-acid/20 text-black font-bold text-sm">
          Use this tool carefully. Deactivation suspends the account and revokes all active sessions.
        </div>

        {error && <div className="p-4 border-2 border-hot bg-hot/10 text-hot font-bold text-sm">{error}</div>}
        {success && <div className="p-4 border-2 border-black bg-white text-black font-bold text-sm">{success}</div>}

        <div className="card p-5 overflow-x-auto">
          {isLoading ? (
            <p className="font-bold text-sm uppercase text-gray-600">Loading users...</p>
          ) : users.length === 0 ? (
            <p className="font-bold text-sm uppercase text-gray-600">No users found.</p>
          ) : (
            <table className="w-full min-w-[760px] text-sm">
              <thead>
                <tr className="text-left border-b-2 border-black">
                  <th className="py-2 pr-3 font-black uppercase tracking-wide">Email</th>
                  <th className="py-2 pr-3 font-black uppercase tracking-wide">Display Name</th>
                  <th className="py-2 pr-3 font-black uppercase tracking-wide">Status</th>
                  <th className="py-2 pr-3 font-black uppercase tracking-wide">Role</th>
                  <th className="py-2 font-black uppercase tracking-wide">Action</th>
                </tr>
              </thead>
              <tbody>
                {users.map((user) => {
                  const isSuspended = (user.status || "").toUpperCase().includes("SUSPEND");
                  return (
                    <tr key={user.userId} className="border-b border-gray-200">
                      <td className="py-3 pr-3 font-semibold">{user.email}</td>
                      <td className="py-3 pr-3">{user.displayName || "-"}</td>
                      <td className="py-3 pr-3">{user.status || "UNKNOWN"}</td>
                      <td className="py-3 pr-3">{user.roleName || "Unassigned"}</td>
                      <td className="py-3">
                        <button
                          type="button"
                          disabled={isSuspended || deactivatingUserId === user.userId}
                          onClick={() => handleDeactivate(user)}
                          className="btn btn-black text-xs font-bold uppercase disabled:opacity-50"
                        >
                          {isSuspended
                            ? "Already Deactivated"
                            : deactivatingUserId === user.userId
                              ? "Deactivating..."
                              : "Deactivate"}
                        </button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </div>

        <div className="flex items-center justify-between gap-3">
          <button
            type="button"
            disabled={page <= 0}
            onClick={() => setPage((current) => Math.max(0, current - 1))}
            className="btn btn-ghost text-xs font-bold uppercase"
          >
            Previous
          </button>
          <p className="text-xs font-black uppercase tracking-widest text-gray-500">
            Page {page + 1} of {Math.max(totalPages, 1)}
          </p>
          <button
            type="button"
            disabled={!hasNext || page + 1 >= totalPages}
            onClick={() => setPage((current) => current + 1)}
            className="btn btn-ghost text-xs font-bold uppercase"
          >
            Next
          </button>
        </div>
      </div>
    </div>
  );
}
