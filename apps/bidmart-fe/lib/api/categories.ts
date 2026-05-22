import { apiFetch } from "../auth/api-client";

export interface CategoryResponse {
  id: number;
  name: string;
  imageUrl: string | null;
  parentId: number | null;
}

export interface CategoryCreateRequest {
  name: string;
  imageUrl?: string;
  parentId?: number | null;
}

export async function getAllCategories(): Promise<CategoryResponse[]> {
  const raw = await apiFetch<CategoryResponse[]>(
    "/api/catalog/categories/main",
    { method: "GET" },
    { auth: false },
  );
  return Array.isArray(raw) ? raw : [];
}

export async function getSubCategories(parentId: number): Promise<CategoryResponse[]> {
  const raw = await apiFetch<CategoryResponse[]>(
    `/api/catalog/categories/sub/${parentId}`,
    { method: "GET" },
    { auth: false },
  );
  return Array.isArray(raw) ? raw : [];
}

export async function getCategoryById(id: number): Promise<CategoryResponse> {
  return await apiFetch<CategoryResponse>(
    `/api/catalog/categories/${id}`,
    { method: "GET" },
    { auth: false },
  );
}

export async function createCategory(input: CategoryCreateRequest): Promise<CategoryResponse> {
  return await apiFetch<CategoryResponse>(
    "/api/catalog/categories",
    {
      method: "POST",
      body: JSON.stringify(input),
    },
    { auth: true },
  );
}

export async function updateCategory(
  id: number,
  input: CategoryCreateRequest,
): Promise<CategoryResponse> {
  return await apiFetch<CategoryResponse>(
    `/api/catalog/categories/${id}`,
    {
      method: "PUT",
      body: JSON.stringify(input),
    },
    { auth: true },
  );
}

export async function deleteCategory(id: number): Promise<void> {
  await apiFetch(
    `/api/catalog/categories/${id}`,
    { method: "DELETE" },
    { auth: true },
  );
}
