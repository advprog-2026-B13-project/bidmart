"use client";

import { useState, useEffect } from "react";
import { useAuth } from "@/components/auth-provider";
import { getNotifications, markNotificationAsRead, markAllNotificationsAsRead, type NotificationItem } from "@/lib/api/endpoints";
import Link from "next/link";
import { Bell, ArrowLeft } from "lucide-react";

export default function NotificationsPage() {
  const { user, isAuthenticated, isHydrating } = useAuth();
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [visibleCount, setVisibleCount] = useState(10);

  useEffect(() => {
    const handleReadAll = () => {
      setNotifications((prev) => prev.map((n) => ({ ...n, isRead: true })));
    };

    const handleReadOne = (e: Event) => {
      const customEvent = e as CustomEvent<{ id: string }>;
      const id = customEvent.detail?.id;
      if (id) {
        setNotifications((prev) =>
            prev.map((n) => (n.id === id ? { ...n, isRead: true } : n))
        );
      }
    };

    window.addEventListener("notifications-read-all", handleReadAll);
    window.addEventListener("notification-marked-read", handleReadOne);

    return () => {
      window.removeEventListener("notifications-read-all", handleReadAll);
      window.removeEventListener("notification-marked-read", handleReadOne);
    };
  }, []);

  useEffect(() => {
    if (isHydrating || !isAuthenticated || !user?.userId) return;

    getNotifications(user.userId)
        .then((data) => {
          setNotifications(data);
          setLoading(false);
        })
        .catch(() => setLoading(false));
  }, [isAuthenticated, isHydrating, user?.userId]);

  useEffect(() => {
    if (!isAuthenticated || !user?.userId) return;

    let es: EventSource | null = null;
    let cancelled = false;

    const connect = () => {
      if (cancelled) return;
      const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";
      es = new EventSource(`${apiBaseUrl}/api/notifications/user/${user.userId}/stream`);

      es.addEventListener("notification", (event) => {
        try {
          const newNotif = JSON.parse(event.data);
          const mappedNotif: NotificationItem = {
            id: newNotif.id,
            userId: newNotif.userId,
            type: newNotif.type,
            message: newNotif.message,
            isRead: newNotif.read !== undefined ? newNotif.read : newNotif.isRead,
            referenceId: newNotif.referenceId,
            createdAt: newNotif.createdAt,
          };
          setNotifications((prev) => [mappedNotif, ...prev]);
        } catch (err) {
          console.error("Failed to parse SSE notification:", err);
        }
      });

      es.onerror = () => {
        es?.close();
        if (!cancelled) {
          setTimeout(connect, 3000);
        }
      };
    };

    connect();

    return () => {
      cancelled = true;
      es?.close();
    };
  }, [isAuthenticated, user?.userId]);

  const handleMarkAllAsRead = async () => {
    if (!user?.userId) return;
    try {
      await markAllNotificationsAsRead(user.userId);
      setNotifications((prev) =>
          prev.map((n) => ({ ...n, isRead: true }))
      );
      window.dispatchEvent(new CustomEvent("notifications-read-all"));
    } catch (err) {
      console.error("Failed to mark all notifications as read:", err);
    }
  };

  if (isHydrating || loading) {
    return (
        <div className="min-h-screen flex items-center justify-center bg-white">
          <div className="w-12 h-12 border-4 border-black border-t-transparent animate-spin"></div>
        </div>
    );
  }

  if (!isAuthenticated) {
    return (
        <div className="min-h-screen flex flex-col items-center justify-center bg-white p-4">
          <h1 className="text-3xl font-black uppercase mb-4">Access Denied</h1>
          <p className="text-gray-500 font-bold mb-6">Please sign in to view your notifications.</p>
          <Link href="/login" className="btn btn-black">Sign In</Link>
        </div>
    );
  }

  const hasUnread = notifications.some((n) => !n.isRead);

  return (
      <div className="bg-white min-h-screen pb-20">
        <div className="max-w-3xl mx-auto px-4 py-8">
          {/* Header */}
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-8">
            <div className="flex items-center gap-4">
              <Link href="/" className="btn btn-sm btn-ghost">
                <ArrowLeft className="w-5 h-5" />
              </Link>
              <h1 className="text-4xl font-black uppercase tracking-tight flex items-center gap-3">
                <Bell className="w-8 h-8" />
                Your Notifications
              </h1>
            </div>
            {hasUnread && (
                <button
                    onClick={handleMarkAllAsRead}
                    className="btn btn-black btn-sm uppercase font-black tracking-wider self-start sm:self-auto"
                >
                  Mark All As Read
                </button>
            )}
          </div>

          {/* Notifications List */}
          <div className="border-3 border-black bg-white shadow-[8px_8px_0_#0A0A0A] divide-y-2 divide-black">
            {notifications.length === 0 ? (
                <div className="p-12 text-center">
                  <p className="text-gray-500 font-black uppercase tracking-wide">No notifications yet.</p>
                </div>
            ) : (
                notifications.slice(0, visibleCount).map((notification) => {
                  const handleMarkAsRead = async () => {
                    if (!notification.isRead) {
                      try {
                        await markNotificationAsRead(notification.id);
                        setNotifications((prev) =>
                            prev.map((n) =>
                                n.id === notification.id ? { ...n, isRead: true } : n
                            )
                        );
                        window.dispatchEvent(
                            new CustomEvent("notification-marked-read", {
                              detail: { id: notification.id },
                            })
                        );
                      } catch (err) {
                        console.error("Failed to mark notification as read:", err);
                      }
                    }
                  };

                  const content = (
                      <>
                        <div className={`w-3.5 h-3.5 mt-1 shrink-0 rounded-full ${
                            notification.type === "OUTBID" ? "bg-hot" : "bg-electric"
                        }`} />
                        <div className="flex-1">
                          <p className="font-bold text-black text-base leading-tight mb-2">
                            {notification.message}
                          </p>
                          <span className="text-xs text-gray-400 font-bold">
                      {new Date(notification.createdAt).toLocaleString()}
                    </span>
                        </div>
                      </>
                  );

                  if (notification.referenceId) {
                    return (
                        <Link
                            key={notification.id}
                            href={`/listing/${notification.referenceId}`}
                            onClick={handleMarkAsRead}
                            className={`p-6 transition-colors flex items-start gap-4 hover:bg-gray-50/50 block ${
                                !notification.isRead ? "bg-acid/10" : "bg-white"
                            }`}
                        >
                          {content}
                        </Link>
                    );
                  }

                  return (
                      <button
                          key={notification.id}
                          onClick={handleMarkAsRead}
                          className={`p-6 transition-colors flex items-start gap-4 cursor-pointer hover:bg-gray-50/50 ${
                              !notification.isRead ? "bg-acid/10" : "bg-white"
                          }`}
                      >
                        {content}
                      </button>
                  );
                })
            )}
          </div>

          {/* Show More Button */}
          {notifications.length > visibleCount && (
              <div className="flex justify-center mt-8">
                <button
                    onClick={() => setVisibleCount((prev) => prev + 10)}
                    className="btn btn-black font-black uppercase tracking-wider px-8 py-3"
                >
                  Show More
                </button>
              </div>
          )}
        </div>
      </div>
  );
}