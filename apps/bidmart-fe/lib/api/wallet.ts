import { apiFetch } from "../auth/api-client";
import type { ApiResponse } from "../auth/types";

export type WalletSummary = {
  id: string;
  userId: string;
  availableBalance: number;
  heldBalance: number;
};

export type WalletTransaction = {
  id: string;
  walletId: string;
  type: "TOP_UP" | "WITHDRAW" | "HOLD" | "RELEASE" | "PAYMENT";
  amount: number;
  referenceId?: string | null;
  createdAt?: string | null;
};

export type PaymentAction = {
  name: string;
  method: string;
  url: string;
};

export type TopUpResponse = {
  orderId: string;
  paymentType: string;
  bank: string;
  vaNumber: string | null;
  transactionStatus: string;
  actions?: PaymentAction[];
};

function unwrapOrThrow<T>(payload: ApiResponse<T>) {
  if (payload.success === false) {
    throw new Error(payload.message || "Request failed");
  }

  if (payload.data === undefined || payload.data === null) {
    throw new Error(payload.message || "Missing response data");
  }

  return payload.data;
}

export async function getMyWallet(): Promise<WalletSummary> {
  const payload = await apiFetch<ApiResponse<WalletSummary>>(
    "/api/wallet/me",
    { method: "GET" },
  );

  return unwrapOrThrow(payload);
}

export async function getMyWalletTransactions(): Promise<WalletTransaction[]> {
  const payload = await apiFetch<ApiResponse<WalletTransaction[]>>(
    "/api/wallet/me/transactions",
    { method: "GET" },
  );

  return unwrapOrThrow(payload);
}

export async function createTopUpTransaction(input: {
  userId: string;
  amount: number;
  paymentType?: string;
  bank?: string;
}): Promise<TopUpResponse> {
  const payload = await apiFetch<ApiResponse<TopUpResponse>>(
    "/api/payment/topup",
    {
      method: "POST",
      body: JSON.stringify(input),
    },
  );

  return unwrapOrThrow(payload);
}
