"use client";

import { useEffect, useState, useCallback } from "react";
import { useRouter } from "next/navigation";
import { useAuth } from "@/components/auth-provider";
import {
  getAllCategories,
  getSubCategories,
  createCategory,
  updateCategory,
  deleteCategory,
  type CategoryResponse,
  type CategoryCreateRequest,
} from "@/lib/api/categories";
import Link from "next/link";
import { ArrowLeft, Plus, Pencil, Trash2, ChevronDown, ChevronRight, X } from "lucide-react";

function getErrorMessage(err: unknown) {
  if (err instanceof Error && err.message) return err.message;
  return "Request failed. Please try again.";
}

type FormData = {
  name: string;
  imageUrl: string;
  parentId: number | null;
};

const emptyForm: FormData = { name: "", imageUrl: "", parentId: null };

export default function AdminCategoriesPage() {
  const router = useRouter();
  const { isAuthenticated, isHydrating, user } = useAuth();

  const [mainCategories, setMainCategories] = useState<CategoryResponse[]>([]);
  const [subCategories, setSubCategories] = useState<Record<number, CategoryResponse[]>>({});
  const [expandedIds, setExpandedIds] = useState<Set<number>>(new Set());
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [showForm, setShowForm] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form, setForm] = useState<FormData>(emptyForm);
  const [submitting, setSubmitting] = useState(false);
  const [deletingId, setDeletingId] = useState<number | null>(null);

  const isAdmin = (user?.role || "").toUpperCase().includes("ADMIN");

  useEffect(() => {
    if (!isHydrating && !isAuthenticated) {
      router.replace("/login");
    }
  }, [isAuthenticated, isHydrating, router]);

  const loadCategories = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const cats = await getAllCategories();
      setMainCategories(cats);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    if (!isHydrating && isAuthenticated) {
      loadCategories();
    }
  }, [isAuthenticated, isHydrating, loadCategories]);

  const toggleExpand = async (id: number) => {
    setExpandedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });

    if (!subCategories[id]) {
      try {
        const subs = await getSubCategories(id);
        setSubCategories((prev) => ({ ...prev, [id]: subs }));
      } catch {
        // ignore subcategory load errors
      }
    }
  };

  const openCreateForm = (parentId: number | null = null) => {
    setEditingId(null);
    setForm({ ...emptyForm, parentId });
    setShowForm(true);
  };

  const openEditForm = (cat: CategoryResponse) => {
    setEditingId(cat.id);
    setForm({
      name: cat.name,
      imageUrl: cat.imageUrl || "",
      parentId: cat.parentId,
    });
    setShowForm(true);
  };

  const closeForm = () => {
    setShowForm(false);
    setEditingId(null);
    setForm(emptyForm);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setError("");

    const payload: CategoryCreateRequest = {
      name: form.name.trim(),
    };
    if (form.imageUrl.trim()) {
      payload.imageUrl = form.imageUrl.trim();
    }
    if (form.parentId !== null) {
      payload.parentId = form.parentId;
    }

    try {
      if (editingId !== null) {
        await updateCategory(editingId, payload);
      } else {
        await createCategory(payload);
      }
      closeForm();
      await loadCategories();
      setSubCategories({});
      setExpandedIds(new Set());
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (id: number, name: string) => {
    const confirmed = window.confirm(
      `Delete category "${name}"? This cannot be undone.`,
    );
    if (!confirmed) return;

    setDeletingId(id);
    setError("");
    try {
      await deleteCategory(id);
      await loadCategories();
      setSubCategories({});
      setExpandedIds(new Set());
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setDeletingId(null);
    }
  };

  if (isHydrating || !isAuthenticated) {
    return (
      <div className="min-h-screen bg-gray-100 flex items-center justify-center px-4">
        <div className="border-3 border-black bg-white px-6 py-5 shadow-[8px_8px_0_#0A0A0A]">
          <p className="text-sm font-bold uppercase tracking-wide text-gray-700">
            Loading...
          </p>
        </div>
      </div>
    );
  }

  if (!isAdmin) {
    return (
      <div className="min-h-screen bg-gray-100 flex items-center justify-center px-4">
        <div className="border-3 border-hot bg-white px-6 py-5 shadow-[8px_8px_0_#0A0A0A]">
          <p className="text-sm font-bold uppercase tracking-wide text-hot">
            Access denied. Admin role required.
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-100 px-4 py-10">
      <div className="max-w-5xl mx-auto">
        <div className="flex items-center gap-3 mb-2">
          <Link
            href="/admin"
            className="w-10 h-10 flex items-center justify-center border-2 border-black shadow-[3px_3px_0_#0A0A0A] hover:shadow-[5px_5px_0_#0A0A0A] hover:translate-x-[-2px] hover:translate-y-[-2px] transition-all bg-white"
          >
            <ArrowLeft className="w-5 h-5" />
          </Link>
          <p className="text-[10px] font-black uppercase tracking-widest text-gray-500">
            Admin
          </p>
        </div>
        <div className="flex flex-col md:flex-row md:items-end md:justify-between gap-4 mb-6">
          <div>
            <h1 className="text-4xl md:text-5xl font-black uppercase tracking-tighter">
              Category Management
            </h1>
            <p className="text-gray-600 mt-1">
              Create, edit, and delete product categories.
            </p>
          </div>
          <button
            type="button"
            onClick={() => openCreateForm(null)}
            className="btn btn-acid text-xs font-bold uppercase tracking-wide"
          >
            <Plus className="w-4 h-4" />
            Add Category
          </button>
        </div>

        {error && (
          <div className="p-4 bg-hot/10 border-2 border-hot text-hot text-sm font-bold mb-6">
            {error}
          </div>
        )}

        {loading ? (
          <div className="border-3 border-black bg-white p-8 shadow-[8px_8px_0_#0A0A0A]">
            <p className="text-sm font-bold uppercase tracking-wide text-gray-600">
              Loading categories...
            </p>
          </div>
        ) : mainCategories.length === 0 ? (
          <div className="border-2 border-dashed border-gray-300 bg-gray-50 p-8">
            <p className="text-sm font-bold uppercase tracking-wide text-gray-600">
              No categories found. Create your first category above.
            </p>
          </div>
        ) : (
          <div className="space-y-3">
            {mainCategories.map((cat) => {
              const isExpanded = expandedIds.has(cat.id);
              const subs = subCategories[cat.id] || [];

              return (
                <div key={cat.id} className="border-2 border-black bg-white">
                  <div className="flex items-center justify-between p-4">
                    <div className="flex items-center gap-3 min-w-0">
                      <button
                        type="button"
                        onClick={() => toggleExpand(cat.id)}
                        className="w-8 h-8 flex items-center justify-center border border-gray-300 hover:border-black hover:bg-gray-50 transition-colors"
                      >
                        {isExpanded ? (
                          <ChevronDown className="w-4 h-4" />
                        ) : (
                          <ChevronRight className="w-4 h-4" />
                        )}
                      </button>
                      <div className="min-w-0">
                        <p className="font-black text-sm uppercase tracking-wide truncate">
                          {cat.name}
                        </p>
                        <p className="text-[10px] text-gray-500 font-bold uppercase">
                          ID: {cat.id}
                          {cat.imageUrl && " · Has image"}
                        </p>
                      </div>
                    </div>
                    <div className="flex items-center gap-2 shrink-0 ml-4">
                      <button
                        type="button"
                        onClick={() => openCreateForm(cat.id)}
                        className="btn btn-ghost btn-sm text-xs font-bold uppercase"
                        title="Add subcategory"
                      >
                        <Plus className="w-3 h-3" />
                        Sub
                      </button>
                      <button
                        type="button"
                        onClick={() => openEditForm(cat)}
                        className="btn btn-ghost btn-sm text-xs font-bold uppercase"
                      >
                        <Pencil className="w-3 h-3" />
                        Edit
                      </button>
                      <button
                        type="button"
                        onClick={() => handleDelete(cat.id, cat.name)}
                        disabled={deletingId === cat.id}
                        className="btn btn-ghost btn-sm text-xs font-bold uppercase text-hot"
                      >
                        {deletingId === cat.id ? (
                          "Deleting..."
                        ) : (
                          <>
                            <Trash2 className="w-3 h-3" />
                            Delete
                          </>
                        )}
                      </button>
                    </div>
                  </div>

                  {isExpanded && (
                    <div className="border-t-2 border-black bg-gray-50">
                      {subs.length === 0 ? (
                        <p className="p-4 text-xs font-bold uppercase text-gray-500">
                          No subcategories.
                        </p>
                      ) : (
                        <div className="divide-y divide-gray-200">
                          {subs.map((sub) => (
                            <div
                              key={sub.id}
                              className="flex items-center justify-between p-3 pl-12"
                            >
                              <div className="min-w-0">
                                <p className="font-bold text-sm uppercase tracking-wide truncate">
                                  {sub.name}
                                </p>
                                <p className="text-[10px] text-gray-500 font-bold uppercase">
                                  ID: {sub.id}
                                  {sub.imageUrl && " · Has image"}
                                </p>
                              </div>
                              <div className="flex items-center gap-2 shrink-0 ml-4">
                                <button
                                  type="button"
                                  onClick={() => openEditForm(sub)}
                                  className="btn btn-ghost btn-sm text-xs font-bold uppercase"
                                >
                                  <Pencil className="w-3 h-3" />
                                  Edit
                                </button>
                                <button
                                  type="button"
                                  onClick={() => handleDelete(sub.id, sub.name)}
                                  disabled={deletingId === sub.id}
                                  className="btn btn-ghost btn-sm text-xs font-bold uppercase text-hot"
                                >
                                  {deletingId === sub.id ? (
                                    "Deleting..."
                                  ) : (
                                    <>
                                      <Trash2 className="w-3 h-3" />
                                      Delete
                                    </>
                                  )}
                                </button>
                              </div>
                            </div>
                          ))}
                        </div>
                      )}
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}

        {showForm && (
          <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4">
            <div className="max-w-md w-full border-3 border-black bg-white p-6 shadow-[8px_8px_0_#0A0A0A]">
              <div className="flex items-center justify-between mb-4">
                <div>
                  <p className="text-[10px] font-black uppercase tracking-widest text-gray-500">
                    {editingId !== null ? "Edit" : "New"} Category
                    {form.parentId !== null && " · Subcategory"}
                  </p>
                  <h3 className="text-2xl font-black uppercase tracking-tight">
                    {editingId !== null ? "Edit Category" : "Create Category"}
                  </h3>
                </div>
                <button
                  type="button"
                  onClick={closeForm}
                  className="w-8 h-8 flex items-center justify-center border border-gray-300 hover:border-black"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>

              <form onSubmit={handleSubmit} className="grid gap-4">
                <div>
                  <label className="text-[10px] font-black uppercase tracking-widest text-gray-500">
                    Name *
                  </label>
                  <input
                    type="text"
                    value={form.name}
                    onChange={(e) =>
                      setForm((f) => ({ ...f, name: e.target.value }))
                    }
                    className="input mt-1"
                    placeholder="e.g. Electronics"
                    required
                    autoFocus
                  />
                </div>

                <div>
                  <label className="text-[10px] font-black uppercase tracking-widest text-gray-500">
                    Image URL
                  </label>
                  <input
                    type="text"
                    value={form.imageUrl}
                    onChange={(e) =>
                      setForm((f) => ({ ...f, imageUrl: e.target.value }))
                    }
                    className="input mt-1"
                    placeholder="https://..."
                  />
                </div>

                {editingId === null && form.parentId === null && (
                  <div>
                    <label className="text-[10px] font-black uppercase tracking-widest text-gray-500">
                      Parent Category (optional — leave empty for main)
                    </label>
                    <select
                      value={form.parentId ?? ""}
                      onChange={(e) =>
                        setForm((f) => ({
                          ...f,
                          parentId: e.target.value
                            ? Number(e.target.value)
                            : null,
                        }))
                      }
                      className="input mt-1"
                    >
                      <option value="">None (Main Category)</option>
                      {mainCategories.map((c) => (
                        <option key={c.id} value={c.id}>
                          {c.name}
                        </option>
                      ))}
                    </select>
                  </div>
                )}

                <div className="flex flex-wrap gap-3 mt-2">
                  <button
                    type="submit"
                    disabled={submitting}
                    className="btn btn-acid text-xs font-bold uppercase tracking-wide"
                  >
                    {submitting
                      ? "Saving..."
                      : editingId !== null
                        ? "Update"
                        : "Create"}
                  </button>
                  <button
                    type="button"
                    onClick={closeForm}
                    className="btn btn-ghost text-xs font-bold uppercase tracking-wide"
                  >
                    Cancel
                  </button>
                </div>
              </form>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
