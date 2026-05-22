"use client";

import Link from "next/link";
import { useEffect } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/components/auth-provider";

export default function AdminPage() {
  const router = useRouter();
  const { isAuthenticated, isHydrating, user } = useAuth();

  useEffect(() => {
    if (!isHydrating && !isAuthenticated) {
      router.replace("/login");
    }
  }, [isAuthenticated, isHydrating, router]);

  if (isHydrating || !isAuthenticated) {
    return (
      <div className="min-h-screen bg-gray-100 flex items-center justify-center px-4">
        <div className="border-3 border-black bg-white px-6 py-5 shadow-[8px_8px_0_#0A0A0A]">
          <p className="text-sm font-bold uppercase tracking-wide text-gray-700">Loading admin workspace...</p>
        </div>
      </div>
    );
  }

  const roleName = user?.role || "UNKNOWN";
  const isAdmin = roleName.toUpperCase().includes("ADMIN");

  return (
    <div className="min-h-screen bg-gray-100 px-4 py-10">
      <div className="max-w-5xl mx-auto">
        <h1 className="text-4xl md:text-5xl font-black uppercase tracking-tighter mb-2">Admin Workspace</h1>
        <p className="text-gray-600 mb-6">Manage roles, assignments, and account deactivation tools.</p>

        {!isAdmin && (
          <div className="mb-6 p-4 border-2 border-hot bg-hot/10 text-hot font-bold text-sm">
            Your profile role is {roleName}. Backend permission checks still apply.
          </div>
        )}

        <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-4">
          <Link href="/admin/roles" className="card p-5 block">
            <p className="text-[10px] font-black uppercase tracking-widest text-gray-500">RBAC</p>
            <h2 className="text-2xl font-black uppercase tracking-tight mt-2">Manage Roles</h2>
            <p className="text-sm text-gray-600 mt-2">Create, update permissions, and delete roles.</p>
          </Link>

          <Link href="/admin/user-roles" className="card p-5 block">
            <p className="text-[10px] font-black uppercase tracking-widest text-gray-500">RBAC</p>
            <h2 className="text-2xl font-black uppercase tracking-tight mt-2">Assign Roles</h2>
            <p className="text-sm text-gray-600 mt-2">Assign and unassign roles for users.</p>
          </Link>

          <Link href="/admin/categories" className="card p-5 block">
            <p className="text-[10px] font-black uppercase tracking-widest text-gray-500">Catalog</p>
            <h2 className="text-2xl font-black uppercase tracking-tight mt-2">Categories</h2>
            <p className="text-sm text-gray-600 mt-2">Create, edit, and delete product categories.</p>
          </Link>

          <Link href="/admin/deactivate" className="card p-5 block">
            <p className="text-[10px] font-black uppercase tracking-widest text-gray-500">Account</p>
            <h2 className="text-2xl font-black uppercase tracking-tight mt-2">Deactivate Account</h2>
            <p className="text-sm text-gray-600 mt-2">Suspend users when required by policy.</p>
          </Link>
        </div>
      </div>
    </div>
  );
}
