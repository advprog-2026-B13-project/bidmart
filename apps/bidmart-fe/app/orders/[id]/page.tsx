"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { 
  CheckCircle2, 
  Clock, 
  Package, 
  Truck, 
  AlertTriangle, 
  ChevronLeft, 
  ShieldAlert,
  Loader2,
  Copy,
  Check
} from "lucide-react";
import { useAuth } from "@/components/auth-provider";
import { useToast } from "@/components/toast";
import { 
  getOrderById, 
  getListingById, 
  updateShipmentStatus, 
  confirmDelivery, 
  disputeOrder, 
  type Order, 
  type OrderStatus,
  type ParsedListing
} from "@/lib/api/endpoints";

function formatCurrency(value: number) {
  return new Intl.NumberFormat("id-ID", {
    style: "currency",
    currency: "IDR",
    maximumFractionDigits: 0,
  }).format(value);
}

function formatDate(value?: string) {
  if (!value) return "Unknown";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString();
}

function CopyableId({ id, label }: { id: string; label: string }) {
  const [copied, setCopied] = useState(false);
  const { showToast } = useToast();

  const handleCopy = () => {
    navigator.clipboard.writeText(id);
    setCopied(true);
    showToast(`${label} copied to clipboard!`, "success");
    setTimeout(() => setCopied(false), 2000);
  };

  return (
    <div className="flex items-center justify-between border border-black bg-gray-50 px-2 py-1 gap-2 mt-1">
      <span className="font-mono text-xs text-gray-700 truncate">{id}</span>
      <button 
        type="button" 
        onClick={handleCopy}
        className="hover:text-electric transition-colors"
        title="Copy ID"
      >
        {copied ? <Check className="w-3.5 h-3.5 text-success" /> : <Copy className="w-3.5 h-3.5" />}
      </button>
    </div>
  );
}

function StatusStepper({ status }: { status: OrderStatus }) {
  const steps = [
    { label: "Pending", desc: "Awaiting packaging", icon: Clock },
    { label: "Packed", desc: "Ready to ship", icon: Package },
    { label: "Shipped", desc: "In transit", icon: Truck },
    { label: "Completed", desc: "Delivered & Closed", icon: CheckCircle2 },
  ];

  const getStepIndex = (s: OrderStatus): number => {
    switch (s) {
      case "PENDING": return 0;
      case "PACKED": return 1;
      case "SHIPPED": return 2;
      case "COMPLETED": return 3;
      default: return -1;
    }
  };

  const currentIndex = getStepIndex(status);

  return (
    <div className="border-2 border-black bg-white p-6 shadow-[4px_4px_0_#000] space-y-6">
      <h3 className="text-lg font-black uppercase tracking-tight">Order Progress</h3>
      
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-6 md:gap-4">
        {steps.map((step, idx) => {
          const Icon = step.icon;
          const isDone = currentIndex >= idx;
          const isCurrent = currentIndex === idx;

          return (
            <div key={step.label} className="flex flex-row md:flex-col items-center md:text-center gap-3 md:flex-1">
              <div className={`w-12 h-12 flex items-center justify-center border-2 border-black transition-colors ${
                isDone 
                  ? "bg-acid text-black" 
                  : isCurrent 
                  ? "bg-electric text-white" 
                  : "bg-gray-100 text-gray-400"
              }`}>
                <Icon className="w-6 h-6" />
              </div>
              <div className="text-left md:text-center">
                <p className={`font-black text-sm uppercase tracking-tight ${isDone || isCurrent ? "text-black" : "text-gray-400"}`}>
                  {step.label}
                </p>
                <p className="text-xs text-gray-500">{step.desc}</p>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

function OrderDetailPageContent({ orderId }: { orderId: string }) {
  const router = useRouter();
  const { isAuthenticated, isHydrating, user } = useAuth();
  const { showToast } = useToast();

  const [order, setOrder] = useState<Order | null>(null);
  const [listing, setListing] = useState<ParsedListing | null>(null);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [trackingInput, setTrackingInput] = useState("");
  const [error, setError] = useState("");

  const loadData = async () => {
    setLoading(true);
    setError("");
    try {
      const orderData = await getOrderById(orderId);
      setOrder(orderData);
      if (orderData.trackingNumber) {
        setTrackingInput(orderData.trackingNumber);
      }
      try {
        const listingData = await getListingById(orderData.listingId);
        setListing(listingData);
      } catch (err) {
        console.error("Failed to load listing details", err);
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load order details.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!isHydrating && !isAuthenticated) {
      router.replace("/login");
      return;
    }
    if (isAuthenticated) {
      loadData();
    }
  }, [orderId, isAuthenticated, isHydrating]);

  if (loading) {
    return (
      <div className="min-h-screen bg-gray-100 flex items-center justify-center px-4">
        <div className="border-3 border-black bg-white px-6 py-5 shadow-[8px_8px_0_#0A0A0A]">
          <p className="text-sm font-bold uppercase tracking-wide text-gray-700">Loading order details...</p>
        </div>
      </div>
    );
  }

  if (error || !order) {
    return (
      <div className="min-h-screen bg-gray-100 flex items-center justify-center px-4">
        <div className="max-w-md w-full border-3 border-black bg-white p-6 shadow-[8px_8px_0_#0A0A0A] space-y-4">
          <div className="flex items-center gap-3 text-hot">
            <AlertTriangle className="w-8 h-8" />
            <h2 className="text-xl font-black uppercase tracking-tight">Error Loading Order</h2>
          </div>
          <p className="text-sm text-gray-600 font-medium">{error || "Order not found."}</p>
          <Link href="/profile" className="btn btn-black w-full text-xs font-bold uppercase">
            Back to Profile
          </Link>
        </div>
      </div>
    );
  }

  const isBuyer = user?.userId === order.buyerId;
  const isSeller = user?.userId === order.sellerId;

  if (!isBuyer && !isSeller) {
    return (
      <div className="min-h-screen bg-gray-100 flex items-center justify-center px-4">
        <div className="max-w-md w-full border-3 border-black bg-white p-6 shadow-[8px_8px_0_#0A0A0A] space-y-4">
          <div className="flex items-center gap-3 text-hot">
            <ShieldAlert className="w-8 h-8" />
            <h2 className="text-xl font-black uppercase tracking-tight">Access Denied</h2>
          </div>
          <p className="text-sm text-gray-600 font-medium">You are not authorized to view this order details.</p>
          <Link href="/profile" className="btn btn-black w-full text-xs font-bold uppercase">
            Back to Profile
          </Link>
        </div>
      </div>
    );
  }

  const handlePack = async () => {
    setActionLoading(true);
    try {
      const updated = await updateShipmentStatus(orderId, "PACKED");
      setOrder(updated);
      showToast("Order status updated to Packed!", "success");
    } catch (err) {
      showToast(err instanceof Error ? err.message : "Failed to update status", "error");
    } finally {
      setActionLoading(false);
    }
  };

  const handleShip = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!trackingInput.trim()) {
      showToast("Please enter a tracking number (resi).", "error");
      return;
    }
    setActionLoading(true);
    try {
      const updated = await updateShipmentStatus(orderId, "SHIPPED", trackingInput.trim());
      setOrder(updated);
      showToast("Order status updated to Shipped!", "success");
    } catch (err) {
      showToast(err instanceof Error ? err.message : "Failed to ship order", "error");
    } finally {
      setActionLoading(false);
    }
  };

  const handleConfirmDelivery = async () => {
    const confirmed = window.confirm("Are you sure you have received the package and want to confirm delivery? This will release the funds to the seller.");
    if (!confirmed) return;

    setActionLoading(true);
    try {
      const updated = await confirmDelivery(orderId);
      setOrder(updated);
      showToast("Delivery confirmed. Transaction completed!", "success");
    } catch (err) {
      showToast(err instanceof Error ? err.message : "Failed to confirm delivery", "error");
    } finally {
      setActionLoading(false);
    }
  };

  const handleDispute = async () => {
    const confirmed = window.confirm("Are you sure you want to dispute this order? This will temporarily hold funds and alert the administrators.");
    if (!confirmed) return;

    setActionLoading(true);
    try {
      const updated = await disputeOrder(orderId);
      setOrder(updated);
      showToast("Order disputed. Admin review has been requested.", "info");
    } catch (err) {
      showToast(err instanceof Error ? err.message : "Failed to flag dispute", "error");
    } finally {
      setActionLoading(false);
    }
  };

  const getStatusBadgeStyle = (status: OrderStatus) => {
    switch (status) {
      case "PENDING": return "bg-yellow-200 border-black text-black";
      case "PACKED": return "bg-blue-200 border-black text-black";
      case "SHIPPED": return "bg-purple-200 border-black text-black";
      case "COMPLETED": return "bg-acid border-black text-black";
      case "DISPUTED": return "bg-hot border-black text-white";
      default: return "bg-gray-100 border-gray-300 text-gray-600";
    }
  };

  return (
    <div className="min-h-screen bg-gray-100 pb-16">
      <div className="max-w-6xl mx-auto px-4 py-8 md:py-12 space-y-6">
        {/* Back Link */}
        <div className="flex items-center gap-2">
          <Link href="/profile" className="flex items-center gap-1.5 text-xs font-black uppercase tracking-wide text-gray-500 hover:text-black transition-colors">
            <ChevronLeft className="w-4 h-4" />
            Back to Profile
          </Link>
        </div>

        {/* Header Block */}
        <div className="flex flex-col md:flex-row md:items-end justify-between gap-4 border-b-2 border-black pb-4">
          <div>
            <p className="text-[10px] font-black uppercase tracking-widest text-gray-500">Order Management</p>
            <h1 className="text-3xl md:text-4xl font-black uppercase tracking-tighter text-black mt-1">
              Order #{order.id.slice(0, 8).toUpperCase()}
            </h1>
            <p className="text-xs text-gray-400 mt-0.5">Created on {formatDate(order.createdAt)}</p>
          </div>
          <div>
            <span className={`inline-block px-3 py-1 border-2 text-xs font-black uppercase tracking-widest shadow-[2px_2px_0_#000] ${getStatusBadgeStyle(order.status)}`}>
              {order.status}
            </span>
          </div>
        </div>

        {/* Dispute Banner if disputed */}
        {order.status === "DISPUTED" && (
          <div className="border-2 border-black bg-hot text-white p-4 shadow-[4px_4px_0_#000] flex items-start gap-3">
            <AlertTriangle className="w-6 h-6 shrink-0 mt-0.5" />
            <div>
              <h3 className="font-black text-base uppercase tracking-wide">ORDER UNDER DISPUTE</h3>
              <p className="text-xs mt-1 text-white/90">
                A dispute has been raised for this order. Payment holds are active and our team is currently investigating.
              </p>
            </div>
          </div>
        )}

        {/* Stepper (Only show if not DISPUTED) */}
        {order.status !== "DISPUTED" && (
          <StatusStepper status={order.status} />
        )}

        {/* Dynamic Grid Layout */}
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          
          {/* Main Actions Panel (Left & Center Columns) */}
          <div className="lg:col-span-2 space-y-6">
            
            {/* Context Actions Card */}
            <div className="border-2 border-black bg-white p-6 shadow-[6px_6px_0_#000] space-y-4">
              <h3 className="text-xl font-black uppercase tracking-tight border-b-2 border-black pb-2">
                Actions
              </h3>

              {isSeller && (
                <div className="space-y-4">
                  <p className="text-xs font-bold uppercase tracking-wider text-electric">Seller Panel</p>
                  
                  {order.status === "PENDING" && (
                    <div className="space-y-3">
                      <p className="text-sm text-gray-600">
                        You have received a purchase order. Confirm that the item is packed and ready for shipping.
                      </p>
                      <button
                        type="button"
                        onClick={handlePack}
                        disabled={actionLoading}
                        className="btn btn-acid text-xs font-bold uppercase w-full sm:w-auto"
                      >
                        {actionLoading ? <Loader2 className="w-4 h-4 animate-spin" /> : "Mark as Packed"}
                      </button>
                    </div>
                  )}

                  {order.status === "PACKED" && (
                    <form onSubmit={handleShip} className="space-y-3">
                      <p className="text-sm text-gray-600">
                        Please enter the tracking number (resi) to ship the item to the buyer.
                      </p>
                      <div className="max-w-md space-y-2">
                        <label htmlFor="trackingInput" className="text-[10px] font-black uppercase tracking-widest text-gray-500">Tracking Number / Resi</label>
                        <input
                          type="text"
                          id="trackingInput"
                          value={trackingInput}
                          onChange={(e) => setTrackingInput(e.target.value)}
                          placeholder="e.g. JNE123456789"
                          required
                          className="input"
                        />
                      </div>
                      <button
                        type="submit"
                        disabled={actionLoading}
                        className="btn btn-electric text-xs font-bold uppercase w-full sm:w-auto"
                      >
                        {actionLoading ? <Loader2 className="w-4 h-4 animate-spin" /> : "Confirm Shipment"}
                      </button>
                    </form>
                  )}

                  {order.status === "SHIPPED" && (
                    <div className="space-y-2">
                      <p className="text-sm text-gray-600">
                        The item has been shipped. Awaiting buyer confirmation of receipt.
                      </p>
                      {order.trackingNumber && (
                        <p className="text-sm font-bold text-gray-700">
                          Resi: <span className="font-mono bg-gray-100 border border-black px-1.5 py-0.5">{order.trackingNumber}</span>
                        </p>
                      )}
                    </div>
                  )}

                  {order.status === "COMPLETED" && (
                    <div className="space-y-2">
                      <p className="text-sm text-gray-600">
                        Transaction complete. The funds have been successfully released to your wallet.
                      </p>
                    </div>
                  )}

                  {order.status === "DISPUTED" && (
                    <div className="space-y-2 text-sm text-gray-600">
                      We will reach out to you via email for details regarding this transaction.
                    </div>
                  )}
                </div>
              )}

              {isBuyer && (
                <div className="space-y-4">
                  <p className="text-xs font-bold uppercase tracking-wider text-electric">Buyer Panel</p>

                  {order.status === "PENDING" && (
                    <p className="text-sm text-gray-600">
                      Payment is held securely in escrow. Awaiting the seller to pack the item.
                    </p>
                  )}

                  {order.status === "PACKED" && (
                    <p className="text-sm text-gray-600">
                      Seller has packed the item. It will be shipped shortly.
                    </p>
                  )}

                  {order.status === "SHIPPED" && (
                    <div className="space-y-4">
                      <p className="text-sm text-gray-600">
                        The seller has shipped your order! Verify your package contents once it arrives.
                      </p>
                      {order.trackingNumber && (
                        <p className="text-sm font-bold text-gray-700">
                          Resi / Tracking: <span className="font-mono bg-gray-100 border border-black px-1.5 py-0.5">{order.trackingNumber}</span>
                        </p>
                      )}
                      <div className="flex flex-wrap items-center gap-3">
                        <button
                          type="button"
                          onClick={handleConfirmDelivery}
                          disabled={actionLoading}
                          className="btn btn-acid text-xs font-bold uppercase"
                        >
                          {actionLoading ? <Loader2 className="w-4 h-4 animate-spin" /> : "Confirm Receipt & Release Funds"}
                        </button>
                        <button
                          type="button"
                          onClick={handleDispute}
                          disabled={actionLoading}
                          className="btn btn-ghost text-xs font-bold uppercase text-hot"
                        >
                          {actionLoading ? <Loader2 className="w-4 h-4 animate-spin" /> : "Dispute Order"}
                        </button>
                      </div>
                    </div>
                  )}

                  {order.status === "COMPLETED" && (
                    <p className="text-sm text-gray-600">
                      Delivery confirmed. Thank you for purchasing on BidMart!
                    </p>
                  )}

                  {order.status === "DISPUTED" && (
                    <p className="text-sm text-gray-600">
                      Dispute opened. Please check your email for messages from our Support agents.
                    </p>
                  )}
                </div>
              )}
            </div>

            {/* Listing details */}
            {listing && (
              <div className="border-2 border-black bg-white p-6 shadow-[6px_6px_0_#000] space-y-4">
                <h3 className="text-xl font-black uppercase tracking-tight border-b-2 border-black pb-2">
                  Item Information
                </h3>
                <div className="flex flex-col sm:flex-row gap-4">
                  <div className="w-full sm:w-28 sm:h-28 border-2 border-black bg-gray-100 overflow-hidden shrink-0">
                    {listing.imageUrl ? (
                      <img src={listing.imageUrl} alt={listing.title} className="w-full h-full object-cover" />
                    ) : (
                      <div className="w-full h-full flex items-center justify-center text-xs font-black text-gray-400">
                        NO IMAGE
                      </div>
                    )}
                  </div>
                  <div className="space-y-1">
                    <span className="inline-block px-1.5 py-0.5 border border-black text-[9px] font-black uppercase tracking-wide bg-gray-100 text-gray-600">
                      {listing.category || "Uncategorized"}
                    </span>
                    <h4 className="font-black text-base text-black uppercase tracking-tight">
                      {listing.title}
                    </h4>
                    <p className="text-xs text-gray-500 line-clamp-2">
                      {listing.description}
                    </p>
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* Sidebar Summary Panel (Right Column) */}
          <div className="space-y-6">
            
            {/* Financial Summary */}
            <div className="border-2 border-black bg-white p-6 shadow-[6px_6px_0_#000] space-y-4">
              <h3 className="text-xl font-black uppercase tracking-tight border-b-2 border-black pb-2">
                Order Summary
              </h3>
              
              <div className="space-y-2 text-sm">
                <div className="flex justify-between font-medium">
                  <span className="text-gray-500">Subtotal</span>
                  <span className="font-bold text-black">{formatCurrency(order.totalAmount)}</span>
                </div>
                <div className="flex justify-between font-medium border-b border-gray-200 pb-2">
                  <span className="text-gray-500">Shipping</span>
                  <span className="font-bold text-success">Free</span>
                </div>
                <div className="flex justify-between pt-2">
                  <span className="font-black text-base uppercase">Total</span>
                  <span className="font-black text-lg text-black">{formatCurrency(order.totalAmount)}</span>
                </div>
              </div>
            </div>

            {/* Stakeholder Details */}
            <div className="border-2 border-black bg-white p-6 shadow-[6px_6px_0_#000] space-y-4">
              <h3 className="text-xl font-black uppercase tracking-tight border-b-2 border-black pb-2">
                Transaction Parties
              </h3>
              <div className="space-y-3">
                <div>
                  <p className="text-[10px] font-black uppercase tracking-widest text-gray-500">Seller</p>
                  {isSeller ? (
                    <span className="text-xs font-black text-electric uppercase mt-0.5 block">You (Seller)</span>
                  ) : (
                    <CopyableId id={order.sellerId} label="Seller ID" />
                  )}
                </div>
                <div>
                  <p className="text-[10px] font-black uppercase tracking-widest text-gray-500">Buyer</p>
                  {isBuyer ? (
                    <span className="text-xs font-black text-electric uppercase mt-0.5 block">You (Buyer)</span>
                  ) : (
                    <CopyableId id={order.buyerId} label="Buyer ID" />
                  )}
                </div>
              </div>
            </div>
            
          </div>
        </div>
      </div>
    </div>
  );
}

export default function OrderDetailPage({ params }: { params: Promise<{ id: string }> }) {
  const [resolvedId, setResolvedId] = useState<string | null>(null);

  useEffect(() => {
    params.then(({ id }) => {
      setResolvedId(id);
    });
  }, [params]);

  if (!resolvedId) {
    return (
      <div className="min-h-screen bg-gray-100 flex items-center justify-center px-4">
        <div className="border-3 border-black bg-white px-6 py-5 shadow-[8px_8px_0_#0A0A0A]">
          <p className="text-sm font-bold uppercase tracking-wide text-gray-700">Loading order routing...</p>
        </div>
      </div>
    );
  }

  return <OrderDetailPageContent orderId={resolvedId} />;
}
