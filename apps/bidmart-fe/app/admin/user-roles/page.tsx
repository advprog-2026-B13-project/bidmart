"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  assignRoleToUser,
  listAdminManagedUsers,
  listAdminRoles,
  unassignRoleFromUser,
} from "@/lib/auth/auth-api";
import type { AdminManagedUserResponse, AdminRoleResponse } from "@/lib/auth/types";
import { useAuth } from "@/components/auth-provider";

function getErrorMessage(error: unknown) {
  if (error instanceof Error && error.message) {
    return error.message;
  }
  return "Request failed. Please try again.";
}

export default function AdminUserRolesPage() {
  const router = useRouter();
  const { isAuthenticated, isHydrating } = useAuth();

  const [roles, setRoles] = useState<AdminRoleResponse[]>([]);
  const [users, setUsers] = useState<AdminManagedUserResponse[]>([]);
  const [page, setPage] = useState(0);
  const [size] = useState(20);
  const [totalPages, setTotalPages] = useState(1);
  const [hasNext, setHasNext] = useState(false);
  const [selectedRoleByUserId, setSelectedRoleByUserId] = useState<Record<string, number | "">>({});
  const [isLoading, setIsLoading] = useState(true);
  const [updatingUserId, setUpdatingUserId] = useState<string | null>(null);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  useEffect(() => {
    if (!isHydrating && !isAuthenticated) {
      router.replace("/login");
    }
  }, [isAuthenticated, isHydrating, router]);

  const loadRolesAndUsers = useCallback(async (targetPage: number) => {
    setError("");
    setIsLoading(true);

    try {
      const [roleData, userData] = await Promise.all([
        listAdminRoles(),
        listAdminManagedUsers(targetPage, size),
      ]);

      setRoles(roleData);
      setUsers(userData.users || []);
      setTotalPages(userData.totalPages || 1);
      setHasNext(Boolean(userData.hasNext));

      const nextMap: Record<string, number | ""> = {};
      (userData.users || []).forEach((user) => {
        nextMap[user.userId] = user.roleId ?? "";
      });
      setSelectedRoleByUserId(nextMap);
    } catch (loadError) {
      setError(getErrorMessage(loadError));
    } finally {
      setIsLoading(false);
    }
  }, [size]);

  useEffect(() => {
    loadRolesAndUsers(page);
  }, [loadRolesAndUsers, page]);

  const handleAssignRole = async (userId: string) => {
    const roleId = selectedRoleByUserId[userId];
    if (roleId === "" || roleId === undefined) {
      setError("Please select a role before assigning.");
      return;
    }

    setError("");
    setSuccess("");
    setUpdatingUserId(userId);

    try {
      await assignRoleToUser(userId, { roleId: Number(roleId) });
      setSuccess("Role assigned successfully.");
      await loadRolesAndUsers(page);
    } catch (assignError) {
      setError(getErrorMessage(assignError));
    } finally {
      setUpdatingUserId(null);
    }
  };

  const handleUnassignRole = async (userId: string) => {
    setError("");
    setSuccess("");
    setUpdatingUserId(userId);

    try {
      await unassignRoleFromUser(userId);
      setSuccess("Role unassigned successfully.");
      await loadRolesAndUsers(page);
    } catch (unassignError) {
      setError(getErrorMessage(unassignError));
    } finally {
      setUpdatingUserId(null);
    }
  };

  if (isHydrating || !isAuthenticated) {
    return (
      <div className="min-h-screen bg-gray-100 flex items-center justify-center px-4">
        <div className="border-3 border-black bg-white px-6 py-5 shadow-[8px_8px_0_#0A0A0A]">
          <p className="text-sm font-bold uppercase tracking-wide text-gray-700">Loading role assignment...</p>
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
            <h1 className="text-4xl font-black uppercase tracking-tighter">Assign Roles to Users</h1>
          </div>
          <Link href="/admin" className="btn btn-ghost text-xs font-bold uppercase">Back to Admin</Link>
        </div>

        {error && <div className="p-4 border-2 border-hot bg-hot/10 text-hot font-bold text-sm">{error}</div>}
        {success && <div className="p-4 border-2 border-black bg-acid/20 text-black font-bold text-sm">{success}</div>}

        <div className="card p-5 overflow-x-auto">
          {isLoading ? (
            <p className="font-bold text-sm uppercase text-gray-600">Loading users...</p>
          ) : users.length === 0 ? (
            <p className="font-bold text-sm uppercase text-gray-600">No users found.</p>
          ) : (
            <table className="w-full min-w-205 text-sm">
              <thead>
                <tr className="text-left border-b-2 border-black">
                  <th className="py-2 pr-3 font-black uppercase tracking-wide">Email</th>
                  <th className="py-2 pr-3 font-black uppercase tracking-wide">Display Name</th>
                  <th className="py-2 pr-3 font-black uppercase tracking-wide">Current Role</th>
                  <th className="py-2 pr-3 font-black uppercase tracking-wide">Status</th>
                  <th className="py-2 pr-3 font-black uppercase tracking-wide">Assign Role</th>
                  <th className="py-2 font-black uppercase tracking-wide">Actions</th>
                </tr>
              </thead>
              <tbody>
                {users.map((user) => (
                  <tr key={user.userId} className="border-b border-gray-200 align-top">
                    <td className="py-3 pr-3 font-semibold">{user.email}</td>
                    <td className="py-3 pr-3">
                      <div className="flex flex-col gap-1">
                        <span>{user.displayName || "-"}</span>
                        <Link
                          href={`/profile/${user.userId}`}
                          className="text-[10px] font-black uppercase tracking-widest text-electric hover:underline"
                        >
                          View Profile
                        </Link>
                      </div>
                    </td>
                    <td className="py-3 pr-3">{user.roleName || "Unassigned"}</td>
                    <td className="py-3 pr-3">{user.status || "UNKNOWN"}</td>
                    <td className="py-3 pr-3">
                      <select
                        className="input"
                        value={selectedRoleByUserId[user.userId] ?? ""}
                        onChange={(event) => {
                          const nextValue = event.target.value;
                          setSelectedRoleByUserId((current) => ({
                            ...current,
                            [user.userId]: nextValue ? Number(nextValue) : "",
                          }));
                        }}
                      >
                        <option value="">Select role</option>
                        {roles.map((role) => (
                          <option key={role.roleId} value={role.roleId}>
                            {role.roleName}
                          </option>
                        ))}
                      </select>
                    </td>
                    <td className="py-3 space-x-2 whitespace-nowrap">
                      <button
                        type="button"
                        disabled={updatingUserId === user.userId}
                        onClick={() => handleAssignRole(user.userId)}
                        className="btn btn-black text-xs font-bold uppercase"
                      >
                        Assign
                      </button>
                      <button
                        type="button"
                        disabled={updatingUserId === user.userId}
                        onClick={() => handleUnassignRole(user.userId)}
                        className="btn text-xs font-bold uppercase"
                      >
                        Unassign
                      </button>
                    </td>
                  </tr>
                ))}
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
