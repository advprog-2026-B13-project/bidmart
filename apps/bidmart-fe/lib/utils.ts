export function formatCurrency(amount: number): string {
  return new Intl.NumberFormat("id-ID", {
    style: "currency",
    currency: "IDR",
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(amount);
}

export function formatTimeRemaining(endTime: Date): string {
  const now = new Date();
  const diff = endTime.getTime() - now.getTime();

  if (diff <= 0) return "Ended";

  const seconds = Math.floor((diff % (1000 * 60)) / 1000);
  const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
  const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
  const days = Math.floor(diff / (1000 * 60 * 60 * 24));

  if (days > 0) return `${days}d ${hours % 24}h`;
  if (hours > 0) return `${hours}h ${minutes}m`;
  return `${minutes}m ${seconds}s`;
}

export function getTimeUrgency(endTime: Date): "critical" | "soon" | "normal" {
  const now = new Date();
  const hoursRemaining = (endTime.getTime() - now.getTime()) / (1000 * 60 * 60);

  if (hoursRemaining <= 1) return "critical";
  if (hoursRemaining <= 6) return "soon";
  return "normal";
}