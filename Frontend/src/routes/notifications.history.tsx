import { createFileRoute } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { RequireAuth } from "@/components/RequireAuth";
import { notificationApi, type NotificationLog } from "@/lib/api-client";
import { useAuth } from "@/contexts/AuthContext";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { formatDateTime } from "@/lib/format";

export const Route = createFileRoute("/notifications/history")({
  component: () => (
    <RequireAuth>
      <History />
    </RequireAuth>
  ),
});

function History() {
  const { user } = useAuth();
  const [page, setPage] = useState(0);

  const q = useQuery({
    queryKey: ["notif-history", user?.userId, page],
    queryFn: () => notificationApi.history(user!.userId, page, 20),
    enabled: !!user,
  });

  const data = q.data;
  const items: NotificationLog[] = Array.isArray(data)
    ? data
    : (data as any)?.content ?? [];
  const totalPages = !Array.isArray(data) ? (data as any)?.totalPages ?? 0 : 0;

  const statusBadge = (s: string) => {
    if (s === "SENT") return <Badge className="bg-green-600 hover:bg-green-600 text-white">SENT</Badge>;
    if (s === "FAILED") return <Badge variant="destructive">FAILED</Badge>;
    if (s === "SKIPPED") return <Badge variant="secondary">SKIPPED</Badge>;
    return <Badge variant="outline">{s}</Badge>;
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold">Notification history</h1>
        <p className="text-muted-foreground mt-1">All alerts we've tried to send you.</p>
      </div>

      {q.isLoading ? (
        <div className="text-muted-foreground">Loading…</div>
      ) : items.length === 0 ? (
        <Card><CardContent className="py-12 text-center text-muted-foreground">No notifications.</CardContent></Card>
      ) : (
        <div className="space-y-3">
          {items.map((n) => (
            <Card key={n.notificationId}>
              <CardContent className="p-4">
                <div className="flex items-start justify-between gap-4">
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2 flex-wrap">
                      <span className="font-medium">{n.subject || n.eventType}</span>
                      {statusBadge(n.status)}
                      <Badge variant="outline">{n.notificationType}</Badge>
                    </div>
                    <div className="text-sm text-muted-foreground mt-1">
                      To: {n.recipient || "—"} · {formatDateTime(n.createdAt)}
                    </div>
                    {n.status === "FAILED" && n.failureReason && (
                      <div className="mt-2 text-sm text-destructive bg-destructive/10 rounded px-3 py-2">
                        {n.failureReason}
                      </div>
                    )}
                  </div>
                  {n.amount != null && (
                    <div className="text-right font-medium whitespace-nowrap">
                      ₹{n.amount.toFixed(2)}
                    </div>
                  )}
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {totalPages > 1 && (
        <div className="flex items-center justify-between">
          <div className="text-sm text-muted-foreground">Page {page + 1} of {totalPages}</div>
          <div className="flex gap-2">
            <Button variant="outline" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>Previous</Button>
            <Button variant="outline" disabled={page >= totalPages - 1} onClick={() => setPage((p) => p + 1)}>Next</Button>
          </div>
        </div>
      )}
    </div>
  );
}
