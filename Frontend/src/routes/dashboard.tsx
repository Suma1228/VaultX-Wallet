import { createFileRoute, Link } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { RequireAuth } from "@/components/RequireAuth";
import { walletApi } from "@/lib/api-client";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Progress } from "@/components/ui/progress";
import { Badge } from "@/components/ui/badge";
import { formatINR, formatDateTime } from "@/lib/format";
import { useAuth } from "@/contexts/AuthContext";
import { ArrowDownToLine, ArrowUpFromLine, Send } from "lucide-react";
import { Button } from "@/components/ui/button";

export const Route = createFileRoute("/dashboard")({
  component: () => (
    <RequireAuth>
      <Dashboard />
    </RequireAuth>
  ),
});

function Dashboard() {
  const { user } = useAuth();
  const balanceQ = useQuery({
    queryKey: ["balance"],
    queryFn: () => walletApi.balance(),
    enabled: !!user,
  });
  const walletQ = useQuery({
    queryKey: ["wallet", user?.userId],
    queryFn: () => walletApi.getByUser(user!.userId),
    enabled: !!user,
  });
  const txQ = useQuery({
    queryKey: ["tx", "recent"],
    queryFn: () => walletApi.transactions(0, 5),
    enabled: !!user,
  });

  const wallet = walletQ.data;
  const dailyPct = wallet && wallet.dailyLimit > 0 ? (wallet.dailySpent / wallet.dailyLimit) * 100 : 0;
  const monthlyPct = wallet && wallet.monthlyLimit > 0 ? (wallet.monthlySpent / wallet.monthlyLimit) * 100 : 0;

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold">Welcome, {user?.firstName}</h1>
        <p className="text-muted-foreground mt-1">Here's a snapshot of your wallet.</p>
      </div>

      <div className="grid md:grid-cols-3 gap-4">
        <Card className="md:col-span-2 bg-primary text-primary-foreground border-0">
          <CardHeader>
            <CardTitle className="text-sm font-normal opacity-80">Available Balance</CardTitle>
          </CardHeader>
          <CardContent>
            <div className="text-4xl font-bold">
              {balanceQ.isLoading ? "…" : formatINR(balanceQ.data?.balance ?? 0)}
            </div>
            {wallet && (
              <div className="mt-3 flex items-center gap-2 text-sm opacity-80">
                <span>Status:</span>
                <Badge variant="secondary">{wallet.status}</Badge>
              </div>
            )}
          </CardContent>
        </Card>
        <Card>
          <CardHeader>
            <CardTitle className="text-base">Quick actions</CardTitle>
          </CardHeader>
          <CardContent className="space-y-2">
            <Button asChild className="w-full justify-start" variant="outline">
              <Link to="/add-money"><ArrowDownToLine className="h-4 w-4 mr-2" />Add money</Link>
            </Button>
            <Button asChild className="w-full justify-start" variant="outline">
              <Link to="/withdraw"><ArrowUpFromLine className="h-4 w-4 mr-2" />Withdraw</Link>
            </Button>
            <Button asChild className="w-full justify-start" variant="outline">
              <Link to="/transfer"><Send className="h-4 w-4 mr-2" />Transfer</Link>
            </Button>
          </CardContent>
        </Card>
      </div>

      <div className="grid md:grid-cols-2 gap-4">
        <Card>
          <CardHeader><CardTitle className="text-base">Daily spend</CardTitle></CardHeader>
          <CardContent>
            <Progress value={Math.min(dailyPct, 100)} />
            <div className="mt-2 text-sm text-muted-foreground">
              {formatINR(wallet?.dailySpent ?? 0)} of {formatINR(wallet?.dailyLimit ?? 0)}
            </div>
          </CardContent>
        </Card>
        <Card>
          <CardHeader><CardTitle className="text-base">Monthly spend</CardTitle></CardHeader>
          <CardContent>
            <Progress value={Math.min(monthlyPct, 100)} />
            <div className="mt-2 text-sm text-muted-foreground">
              {formatINR(wallet?.monthlySpent ?? 0)} of {formatINR(wallet?.monthlyLimit ?? 0)}
            </div>
          </CardContent>
        </Card>
      </div>

      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <CardTitle className="text-base">Recent transactions</CardTitle>
          <Link to="/transactions" className="text-sm text-primary hover:underline">View all</Link>
        </CardHeader>
        <CardContent>
          {txQ.isLoading ? (
            <div className="text-muted-foreground text-sm">Loading…</div>
          ) : !txQ.data?.content?.length ? (
            <div className="text-muted-foreground text-sm">No transactions yet.</div>
          ) : (
            <div className="divide-y">
              {txQ.data.content.map((t) => {
                const credit = t.type.includes("CREDIT");
                return (
                  <div key={t.transactionId} className="py-3 flex items-center justify-between">
                    <div>
                      <div className="font-medium">{t.description || t.type}</div>
                      <div className="text-xs text-muted-foreground">
                        {formatDateTime(t.createdAt)} · {t.status}
                      </div>
                    </div>
                    <div className={`font-semibold ${credit ? "text-green-600" : "text-foreground"}`}>
                      {credit ? "+" : "−"}{formatINR(t.amount)}
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
