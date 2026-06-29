import { createFileRoute } from "@tanstack/react-router";
import { useQuery } from "@tanstack/react-query";
import { useState } from "react";
import { RequireAuth } from "@/components/RequireAuth";
import { walletApi, type TransactionResponse } from "@/lib/api-client";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table";
import { formatDateTime, formatINR } from "@/lib/format";

export const Route = createFileRoute("/transactions")({
  component: () => (
    <RequireAuth>
      <Transactions />
    </RequireAuth>
  ),
});

function toBackendDateTime(localValue: string): string {
  // localValue from <input type="datetime-local"> is "YYYY-MM-DDTHH:mm"
  // Backend needs the exact literal value, no timezone conversion: "YYYY-MM-DDTHH:mm:ss.SSS"
  return `${localValue}:00.000`;
}

function Transactions() {
  const [page, setPage] = useState(0);
  const size = 20;
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [filterActive, setFilterActive] = useState(false);

  const listQ = useQuery({
    queryKey: ["tx", page],
    queryFn: () => walletApi.transactions(page, size),
    enabled: !filterActive,
  });

  const rangeQ = useQuery({
    queryKey: ["tx-range", startDate, endDate],
    queryFn: () =>
      walletApi.transactionsByDate(
        toBackendDateTime(startDate),
        toBackendDateTime(endDate),
      ),
    enabled: filterActive && !!startDate && !!endDate,
  });

  const txs: TransactionResponse[] = filterActive
    ? rangeQ.data ?? []
    : listQ.data?.content ?? [];
  const rangeError = filterActive && rangeQ.isError;
  const totalPages = listQ.data?.totalPages ?? 0;

  const statusColor = (s: string) =>
    s === "SUCCESS" ? "default" : s === "PENDING" ? "secondary" : "destructive";

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-3xl font-bold">Transactions</h1>
        <p className="text-muted-foreground mt-1">All wallet activity.</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Filter by date</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex flex-wrap items-end gap-3">
            <div className="space-y-1">
              <Label htmlFor="start">From</Label>
              <Input id="start" type="datetime-local" value={startDate} onChange={(e) => setStartDate(e.target.value)} />
            </div>
            <div className="space-y-1">
              <Label htmlFor="end">To</Label>
              <Input id="end" type="datetime-local" value={endDate} onChange={(e) => setEndDate(e.target.value)} />
            </div>
            <Button
              onClick={() => {
                if (startDate && endDate) setFilterActive(true);
              }}
            >
              Apply
            </Button>
            {filterActive && (
              <Button variant="outline" onClick={() => { setFilterActive(false); setStartDate(""); setEndDate(""); }}>
                Clear
              </Button>
            )}
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardContent className="p-0">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Date</TableHead>
                <TableHead>Type</TableHead>
                <TableHead>Description</TableHead>
                <TableHead>Status</TableHead>
                <TableHead className="text-right">Amount</TableHead>
                <TableHead className="text-right">Balance after</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
{rangeError ? (
                <TableRow>
                  <TableCell colSpan={6} className="text-center text-destructive py-12">
                    Couldn't load transactions for that range.
                  </TableCell>
                </TableRow>
              ) : txs.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={6} className="text-center text-muted-foreground py-12">
                    No transactions.
                  </TableCell>
                </TableRow>
              ) : (
                txs.map((t) => {
                  const credit = t.type.includes("CREDIT");
                  return (
                    <TableRow key={t.transactionId}>
                      <TableCell className="text-sm">{formatDateTime(t.createdAt)}</TableCell>
                      <TableCell><Badge variant="outline">{t.type}</Badge></TableCell>
                      <TableCell className="max-w-xs truncate">{t.description || "—"}</TableCell>
                      <TableCell><Badge variant={statusColor(t.status)}>{t.status}</Badge></TableCell>
                      <TableCell className={`text-right font-medium ${credit ? "text-green-600" : ""}`}>
                        {credit ? "+" : "−"}{formatINR(t.amount)}
                      </TableCell>
                      <TableCell className="text-right text-sm text-muted-foreground">
                        {formatINR(t.balanceAfter ?? undefined as any)}
                      </TableCell>
                    </TableRow>
                  );
                })
              )}
            </TableBody>
          </Table>
        </CardContent>
      </Card>

      {!filterActive && totalPages > 1 && (
        <div className="flex items-center justify-between">
          <div className="text-sm text-muted-foreground">
            Page {page + 1} of {totalPages}
          </div>
          <div className="flex gap-2">
            <Button variant="outline" disabled={page === 0} onClick={() => setPage((p) => p - 1)}>Previous</Button>
            <Button variant="outline" disabled={page >= totalPages - 1} onClick={() => setPage((p) => p + 1)}>Next</Button>
          </div>
        </div>
      )}
    </div>
  );
}
