"use client";

import { useState, useEffect, useRef } from "react";
import Link from "next/link";
import { Bell } from "lucide-react";
import { useAuth } from "./auth-provider";
import { getNotifications, markNotificationAsRead, type NotificationItem } from "@/lib/api/endpoints";
import { Client } from "@stomp/stompjs";

export function NotificationBell() {
  const { user, isAuthenticated, isHydrating } = useAuth();
  const [notifications, setNotifications] = useState<NotificationItem[]>([]);
  const [isOpen, setIsOpen] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!isAuthenticated || !user?.userId) return;

    const ignore = { current: false };
    getNotifications(user.userId)
      .then(n => { if (!ignore.current) setNotifications(n); })
      .catch(() => {});

    return () => { ignore.current = true; };
  }, [isAuthenticated, user?.userId]);

  useEffect(() => {
    if (!isAuthenticated || !user?.userId) return;

    const apiBaseUrl = process.env.NEXT_PUBLIC_API_BASE_URL || "http://localhost:8080";
    const wsUrl = apiBaseUrl.replace(/^http/, "ws") + "/ws";

    const client = new Client({
      brokerURL: wsUrl,
      reconnectDelay: 5000,
      onConnect: () => {
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
    });

    client.activate();

    return () => {
      client.deactivate();
    };
  }, [isAuthenticated, user?.userId]);

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setIsOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  if (isHydrating || !isAuthenticated) return null;

  const unreadCount = notifications.filter(n => !n.isRead).length;

  return (
    <div className="relative" ref={dropdownRef}>
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="relative w-10 h-10 flex items-center justify-center border-2 border-black shadow-[3px_3px_0_#0A0A0A] hover:shadow-[5px_5px_0_#0A0A0A] hover:translate-x-[-2px] hover:translate-y-[-2px] transition-all bg-white"
      >
        <Bell className="w-5 h-5" />
        {unreadCount > 0 && (
          <span className="absolute -top-1 -right-1 w-4 h-4 bg-hot text-white text-[10px] font-black flex items-center justify-center">
            {unreadCount > 9 ? "9+" : unreadCount}
          </span>
        )}
      </button>

      {isOpen && (
        <div className="absolute right-0 top-12 w-80 bg-white border-3 border-black shadow-[8px_8px_0_#0A0A0A] z-50">
          <div className="flex items-center justify-between p-4 border-b-2 border-black">
            <h3 className="font-black text-sm uppercase tracking-tight">Notifications</h3>
            {unreadCount > 0 && (
              <span className="text-xs font-bold text-hot uppercase">{unreadCount} unread</span>
            )}
          </div>

          <div className="max-h-80 overflow-y-auto">
            {notifications.length === 0 ? (
              <div className="p-8 text-center">
                <p className="text-gray-500 font-bold text-sm uppercase">No notifications yet</p>
              </div>
            ) : (
              notifications.slice(0, 5).map((notification) => {
                  const handleClick = async () => {
                    try {
                      if (!notification.isRead) {
                        await markNotificationAsRead(notification.id);
                        setNotifications(prev =>
                            prev.map(n => n.id === notification.id ? { ...n, isRead: true } : n)
                        );
                      }
                    } catch (err) {
                      console.error("Failed to mark notification as read:", err);
                    }
                    setIsOpen(false);
                  };
                  const content = (
                      <div className="flex items-start gap-3 text-left">
                        <div className={`w-2 h-2 mt-2 shrink-0 rounded-full ${getNotificationColor(notification.type)}`} />
                        <div className="flex-1 min-w-0">
                          <p className="text-sm font-bold text-black leading-tight">{notification.message}</p>
                          <p className="text-xs text-gray-400 mt-1">
                            {formatNotifTime(notification.createdAt)}
                          </p>
                        </div>
                      </div>
                  );
                  if (notification.referenceId) {
                    return (
                        <Link
                            key={notification.id}
                            href={`/listing/${notification.referenceId}`}
                            onClick={handleClick}
                            className={`block p-4 border-b border-gray-200 hover:bg-gray-50 transition-colors ${
                                !notification.isRead ? "bg-acid/5" : ""
                            }`}
                        >
                          {content}
                        </Link>
                    );
                  }
                  return (
                      <div
                          key={notification.id}
                          onClick={handleClick}
                          className={`p-4 border-b border-gray-200 hover:bg-gray-50 cursor-pointer transition-colors ${
                              !notification.isRead ? "bg-acid/5" : ""
                          }`}
                      >
                        {content}
                      </div>
                  );
                })
            )}
          </div>

          <div className="p-3 border-t-2 border-black">
            <Link
              href="/notifications"
              onClick={() => setIsOpen(false)}
              className="block w-full text-center text-xs font-black uppercase text-electric hover:underline"
            >
              View all {notifications.length} notifications
            </Link>
          </div>
        </div>
      )}
    </div>
  );
}

function getNotificationColor(type: string): string {
  switch (type.toUpperCase()) {
    case "OUTBID":
    case "BID_REJECTED":
      return "bg-hot";
    case "BID_PLACED":
    case "AUCTION_WON":
      return "bg-electric";
    case "AUCTION_ENDED":
      return "bg-acid";
    default:
      return "bg-gray-400";
  }
}

function formatNotifTime(isoString: string): string {
  const date = new Date(isoString);
  const now = new Date();
  const diffMs = now.getTime() - date.getTime();
  const diffMins = Math.floor(diffMs / 60000);
  const diffHours = Math.floor(diffMins / 60);
  const diffDays = Math.floor(diffHours / 24);

  if (diffMins < 1) return "Just now";
  if (diffMins < 60) return `${diffMins}m ago`;
  if (diffHours < 24) return `${diffHours}h ago`;
  if (diffDays < 7) return `${diffDays}d ago`;
  return date.toLocaleDateString();
}