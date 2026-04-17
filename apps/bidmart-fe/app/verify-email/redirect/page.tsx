import { redirect } from "next/navigation";

type SearchParams = Record<string, string | string[] | undefined>;

function toQueryString(searchParams: SearchParams) {
  const query = new URLSearchParams();

  for (const [key, value] of Object.entries(searchParams)) {
    if (Array.isArray(value)) {
      value.forEach((entry) => query.append(key, entry));
      continue;
    }

    if (typeof value === "string" && value.length > 0) {
      query.set(key, value);
    }
  }

  return query.toString();
}

export default function VerifyEmailRedirectPage({ searchParams }: { searchParams: SearchParams }) {
  const query = toQueryString(searchParams);
  redirect(query ? `/verify-email?${query}` : "/verify-email");
}
