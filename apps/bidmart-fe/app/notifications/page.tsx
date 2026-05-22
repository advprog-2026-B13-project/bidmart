"use client";

import { useState, useEffect } from "react";
import { useAuth } from "@/components/auth-provider";
import { getNotifications, markNotificationAsRead, type NotificationItem } from "@/lib/api/endpoints";
import Link from "next/link";
import { Bell, ArrowLeft } from "lucide-react";
import { Client } from "@stomp/stompjs";

export default function NotificationsPage() {
  const { user, isAuthenticated, isHydrating } = useAuth();
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [loading, setLoading] = useState(true);

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

    const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";
    const wsUrl = apiBaseUrl.replace(/^http/, "ws") + "/ws";

    const client = new Client({
      brokerURL: wsUrl,
      reconnectDelay: 5000,
      onConnect: () => {
        console.log("WebSocket connected for notifications!");
        client.subscribe(`/topic/notifications/${user.userId}`, (message) => {
          const newNotif = JSON.parse(message.body);

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
        });
      },
      onWebSocketError: (error) => {
        console.error("WebSocket error:", error);
      },
    });

    client.activate();

    return () => {
      client.deactivate();
    };
  }, [isAuthenticated, user?.userId]);

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

  return (
      <div className="bg-white min-h-screen pb-20">
        <div className="max-w-3xl mx-auto px-4 py-8">
          {/* Header */}
          <div className="flex items-center gap-4 mb-8">
            <Link href="/" className="btn btn-sm btn-ghost">
              <ArrowLeft className="w-5 h-5" />
            </Link>
            <h1 className="text-4xl font-black uppercase tracking-tight flex items-center gap-3">
              <Bell className="w-8 h-8" />
              Your Notifications
            </h1>
          </div>

          {/* Notifications List */}
          <div className="border-3 border-black bg-white shadow-[8px_8px_0_#0A0A0A] divide-y-2 divide-black">
            {notifications.length === 0 ? (
                <div className="p-12 text-center">
                  <p className="text-gray-500 font-black uppercase tracking-wide">No notifications yet.</p>
                </div>
            ) : (
                notifications.map((notification) => {
                  const handleMarkAsRead = async () => {
                    if (!notification.isRead) {
                      try {
                        await markNotificationAsRead(notification.id);
                        setNotifications((prev) =>
                            prev.map((n) =>
                                n.id === notification.id ? { ...n, isRead: true } : n
                            )
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
                      <div
                          key={notification.id}
                          onClick={handleMarkAsRead}
                          className={`p-6 transition-colors flex items-start gap-4 cursor-pointer hover:bg-gray-50/50 ${
                              !notification.isRead ? "bg-acid/10" : "bg-white"
                          }`}
                      >
                        {content}
                      </div>
                  );
                })
            )}
          </div>
        </div>
      </div>
  );
}