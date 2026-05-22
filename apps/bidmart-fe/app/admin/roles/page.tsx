"use client";

import { SubmitEventHandler, useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import {
  createAdminRole,
  deleteAdminRole,
  listAdminPermissions,
  listAdminRoles,
  updateAdminRolePermissions,
} from "@/lib/auth/auth-api";
import type { AdminRoleResponse } from "@/lib/auth/types";
import { useAuth } from "@/components/auth-provider";

function getErrorMessage(error: unknown) {
  if (error instanceof Error && error.message) {
    return error.message;
  }
  return "Request failed. Please try again.";
}

export default function AdminRolesPage() {
  const router = useRouter();
  const { isAuthenticated, isHydrating } = useAuth();

  const [roles, setRoles] = useState<AdminRoleResponse[]>([]);
  const [permissions, setPermissions] = useState<string[]>([]);
  const [roleName, setRoleName] = useState("");
  const [selectedPermissions, setSelectedPermissions] = useState<string[]>([]);
  const [draftPermissions, setDraftPermissions] = useState<Record<number, string[]>>({});
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [savingRoleId, setSavingRoleId] = useState<number | null>(null);
  const [deletingRoleId, setDeletingRoleId] = useState<number | null>(null);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  useEffect(() => {
    if (!isHydrating && !isAuthenticated) {
      router.replace("/login");
    }
  }, [isAuthenticated, isHydrating, router]);

  const permissionSet = useMemo(() => new Set(permissions), [permissions]);

  const loadAdminData = async () => {
    setError("");

    try {
      const [roleData, permissionData] = await Promise.all([
        listAdminRoles(),
        listAdminPermissions(),
      ]);

      setRoles(roleData);
      setPermissions(permissionData);
      const nextDrafts: Record<number, string[]> = {};
      roleData.forEach((role) => {
        nextDrafts[role.roleId] = [...role.permissions];
      });
      setDraftPermissions(nextDrafts);
    } catch (loadError) {
      setError(getErrorMessage(loadError));
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    loadAdminData();
  }, []);

  const togglePermissionSelection = (value: string) => {
    setSelectedPermissions((current) =>
      current.includes(value)
        ? current.filter((permission) => permission !== value)
        : [...current, value],
    );
  };

  const toggleRoleDraftPermission = (roleId: number, permission: string) => {
    setDraftPermissions((current) => {
      const existing = current[roleId] || [];
      const next = existing.includes(permission)
        ? existing.filter((item) => item !== permission)
        : [...existing, permission];

      return {
        ...current,
        [roleId]: next,
      };
    });
  };

  const handleCreateRole: SubmitEventHandler<HTMLFormElement> = async (event) => {
    event.preventDefault();
    setError("");
    setSuccess("");

    const normalizedRoleName = roleName.trim();
    if (!normalizedRoleName) {
      setError("Role name is required.");
      return;
    }

    setIsSubmitting(true);

    try {
      await createAdminRole({
        roleName: normalizedRoleName,
        permissions: selectedPermissions,
      });
      setRoleName("");
      setSelectedPermissions([]);
      setSuccess("Role created successfully.");
      await loadAdminData();
    } catch (createError) {
      setError(getErrorMessage(createError));
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleSavePermissions = async (roleId: number) => {
    setError("");
    setSuccess("");
    setSavingRoleId(roleId);

    try {
      await updateAdminRolePermissions(roleId, {
        permissions: draftPermissions[roleId] || [],
      });
      setSuccess("Role permissions updated.");
      await loadAdminData();
    } catch (saveError) {
      setError(getErrorMessage(saveError));
    } finally {
      setSavingRoleId(null);
    }
  };

  const handleDeleteRole = async (roleId: number, roleLabel: string) => {
    if (!globalThis.confirm(`Delete role ${roleLabel}?`)) {
      return;
    }

    setError("");
    setSuccess("");
    setDeletingRoleId(roleId);

    try {
      await deleteAdminRole(roleId);
      setSuccess("Role deleted successfully.");
      await loadAdminData();
    } catch (deleteError) {
      setError(getErrorMessage(deleteError));
    } finally {
      setDeletingRoleId(null);
    }
  };

  if (isHydrating || !isAuthenticated) {
    return (
      <div className="min-h-screen bg-gray-100 flex items-center justify-center px-4">
        <div className="border-3 border-black bg-white px-6 py-5 shadow-[8px_8px_0_#0A0A0A]">
          <p className="text-sm font-bold uppercase tracking-wide text-gray-700">Loading role management...</p>
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
            <h1 className="text-4xl font-black uppercase tracking-tighter">Manage Roles</h1>
          </div>
          <Link href="/admin" className="btn btn-ghost text-xs font-bold uppercase">Back to Admin</Link>
        </div>

        {error && <div className="p-4 border-2 border-hot bg-hot/10 text-hot font-bold text-sm">{error}</div>}
        {success && <div className="p-4 border-2 border-black bg-acid/20 text-black font-bold text-sm">{success}</div>}

        <section className="card p-5">
          <h2 className="text-xl font-black uppercase tracking-tight mb-4">Create Role</h2>
          <form onSubmit={handleCreateRole} className="space-y-4">
            <div>
              <label htmlFor="name" className="block text-[10px] font-black uppercase tracking-widest text-gray-500 mb-2">Role Name</label>
              <input
                id="name"
                value={roleName}
                onChange={(event) => setRoleName(event.target.value)}
                className="input"
                placeholder="MODERATOR"
              />
            </div>

            <div>
              <label htmlFor="permissions" className="block text-[10px] font-black uppercase tracking-widest text-gray-500 mb-2">Permissions</label>
              <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-2 border-2 border-black p-3 bg-white">
                {permissions.map((permission) => (
                  <label key={permission} className="flex items-center gap-2 text-sm font-medium">
                    <input
                      id="permissions"
                      type="checkbox"
                      checked={selectedPermissions.includes(permission)}
                      onChange={() => togglePermissionSelection(permission)}
                      className="w-4 h-4"
                    />
                    <span>{permission}</span>
                  </label>
                ))}
              </div>
            </div>

            <button type="submit" disabled={isSubmitting} className="btn btn-black text-xs font-bold uppercase">
              {isSubmitting ? "Creating..." : "Create Role"}
            </button>
          </form>
        </section>

        <section className="space-y-4">
          <h2 className="text-xl font-black uppercase tracking-tight">Existing Roles</h2>

          {isLoading ? (
            <div className="card p-5 font-bold text-sm uppercase text-gray-600">Loading roles...</div>
          ) : (
            <RolesList
              roles={roles}
              permissions={permissions}
              draftPermissions={draftPermissions}
              permissionSet={permissionSet}
              toggleRoleDraftPermission={toggleRoleDraftPermission}
              handleDeleteRole={handleDeleteRole}
              deletingRoleId={deletingRoleId}
              handleSavePermissions={handleSavePermissions}
              savingRoleId={savingRoleId}
            />
          )}
        </section>
      </div>
    </div>
  );
}

function RolesList({ roles, permissions, draftPermissions, permissionSet, toggleRoleDraftPermission, handleDeleteRole, deletingRoleId, handleSavePermissions, savingRoleId }: {
  roles: AdminRoleResponse[];
  permissions: string[];
  draftPermissions: Record<number, string[]>;
  permissionSet: Set<string>;
  toggleRoleDraftPermission: (roleId: number, permission: string) => void;
  handleDeleteRole: (roleId: number, roleLabel: string) => void;
  deletingRoleId: number | null;
  handleSavePermissions: (roleId: number) => void;
  savingRoleId: number | null;
}) {
  return (
    roles.length === 0 ? (
      <div className="card p-5 font-bold text-sm uppercase text-gray-600">No roles found.</div>
    ) : (
      roles.map((role) => (
        <div key={role.roleId} className="card p-5 space-y-4">
          <div className="flex items-center justify-between gap-2 flex-wrap">
            <div>
              <p className="text-[10px] font-black uppercase tracking-widest text-gray-500">Role ID {role.roleId}</p>
              <h3 className="text-2xl font-black uppercase tracking-tight">{role.roleName}</h3>
            </div>
            <button
              type="button"
              onClick={() => handleDeleteRole(role.roleId, role.roleName)}
              disabled={deletingRoleId === role.roleId}
              className="btn text-xs font-bold uppercase"
            >
              {deletingRoleId === role.roleId ? "Deleting..." : "Delete Role"}
            </button>
          </div>

          <div className="grid sm:grid-cols-2 lg:grid-cols-3 gap-2 border-2 border-black p-3 bg-white">
            {permissions.map((permission) => (
              <label key={`${role.roleId}-${permission}`} className="flex items-center gap-2 text-sm font-medium">
                <input
                  type="checkbox"
                  checked={(draftPermissions[role.roleId] || []).includes(permission)}
                  onChange={() => toggleRoleDraftPermission(role.roleId, permission)}
                  disabled={!permissionSet.has(permission)}
                  className="w-4 h-4"
                />
                <span>{permission}</span>
              </label>
            ))}
          </div>

          <button
            type="button"
            onClick={() => handleSavePermissions(role.roleId)}
            disabled={savingRoleId === role.roleId}
            className="btn btn-black text-xs font-bold uppercase"
          >
            {savingRoleId === role.roleId ? "Saving..." : "Save Permissions"}
          </button>
        </div>
      ))
    )
  );
}
