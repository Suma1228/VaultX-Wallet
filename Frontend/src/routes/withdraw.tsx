import { createFileRoute } from "@tanstack/react-router";
import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { RequireAuth } from "@/components/RequireAuth";
import { walletApi } from "@/lib/api-client";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";

export const Route = createFileRoute("/withdraw")({
  component: () => (
    <RequireAuth>
      <Withdraw />
    </RequireAuth>
  ),
});

function Withdraw() {
  const qc = useQueryClient();
  const [amount, setAmount] = useState("");
  const [description, setDescription] = useState("");
  const [bankAccountNumber, setBank] = useState("");
  const [ifscCode, setIfsc] = useState("");
  const [loading, setLoading] = useState(false);

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const num = Number(amount);
    if (!num || num <= 0) return toast.error("Enter a valid amount");
    setLoading(true);
    try {
      await walletApi.withdraw({
        amount: num,
        description: description || undefined,
        bankAccountNumber: bankAccountNumber || undefined,
        ifscCode: ifscCode || undefined,
      });
      toast.success("Withdrawal processed");
      setAmount(""); setDescription(""); setBank(""); setIfsc("");
      qc.invalidateQueries();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-xl">
      <h1 className="text-3xl font-bold mb-6">Withdraw</h1>
      <Card>
        <CardHeader>
          <CardTitle>Withdraw to bank</CardTitle>
          <CardDescription>Transfer funds from your wallet to a bank account.</CardDescription>
        </CardHeader>
        <CardContent>
          <form className="space-y-4" onSubmit={onSubmit}>
            <div className="space-y-2">
              <Label htmlFor="amount">Amount (INR)</Label>
              <Input id="amount" type="number" min="1" step="0.01" value={amount} onChange={(e) => setAmount(e.target.value)} required />
            </div>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label htmlFor="bank">Account number (optional)</Label>
                <Input id="bank" value={bankAccountNumber} onChange={(e) => setBank(e.target.value)} />
              </div>
              <div className="space-y-2">
                <Label htmlFor="ifsc">IFSC code (optional)</Label>
                <Input id="ifsc" value={ifscCode} onChange={(e) => setIfsc(e.target.value)} />
              </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="description">Description (optional)</Label>
              <Textarea id="description" rows={2} value={description} onChange={(e) => setDescription(e.target.value)} />
            </div>
            <Button type="submit" disabled={loading}>{loading ? "Processing…" : "Withdraw"}</Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
