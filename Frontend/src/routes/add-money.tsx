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
import { Loader2, CheckCircle2 } from "lucide-react";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

export const Route = createFileRoute("/add-money")({
  component: () => (
    <RequireAuth>
      <AddMoney />
    </RequireAuth>
  ),
});

const PAYMENT_METHODS = [
  { value: "UPI", label: "UPI" },
  { value: "DEBIT_CARD", label: "Debit Card" },
  { value: "CREDIT_CARD", label: "Credit Card" },
  { value: "NET_BANKING", label: "Net Banking" },
  { value: "WALLET", label: "Other Wallet (Paytm / PhonePe / etc.)" },
];

function generateMockReferenceId() {
  return `MOCK-${Date.now()}-${Math.floor(Math.random() * 10000)}`;
}

function AddMoney() {
  const qc = useQueryClient();
const [amount, setAmount] = useState("");
  const [description, setDescription] = useState("");
  const [paymentMethod, setPaymentMethod] = useState("");
  const [step, setStep] = useState<"form" | "processing" | "success">("form");

  const methodLabel = PAYMENT_METHODS.find((m) => m.value === paymentMethod)?.label ?? "";

const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const num = Number(amount);
    if (!num || num <= 0) return toast.error("Enter a valid amount");
    if (!paymentMethod) return toast.error("Choose a payment method");

    setStep("processing");
    await new Promise((r) => setTimeout(r, 1400));

    try {
      await walletApi.addMoney({
        amount: num,
        description: description || undefined,
        paymentMethod,
        referenceId: generateMockReferenceId(),
      });
      setStep("success");
      await new Promise((r) => setTimeout(r, 900));
      toast.success("Money added to your wallet");
      setAmount(""); setDescription(""); setPaymentMethod("");
      qc.invalidateQueries();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Payment failed");
    } finally {
      setStep("form");
    }
  };

  return (
    <div className="max-w-xl">
      <h1 className="text-3xl font-bold mb-6">Add money</h1>
      <Card>
        <CardHeader>
          <CardTitle>Top up your wallet</CardTitle>
          <CardDescription>
            {step === "form" && "Choose a payment method to add money to your wallet."}
            {step === "processing" && `Processing your payment via ${methodLabel}…`}
            {step === "success" && "Payment successful."}
          </CardDescription>
        </CardHeader>
        <CardContent>
          {step === "processing" && (
            <div className="flex flex-col items-center justify-center gap-3 py-10">
              <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
              <p className="text-sm text-muted-foreground">Connecting to {methodLabel}…</p>
            </div>
          )}

          {step === "success" && (
            <div className="flex flex-col items-center justify-center gap-3 py-10">
              <CheckCircle2 className="h-10 w-10 text-green-500" />
              <p className="text-sm font-medium">Payment confirmed</p>
            </div>
          )}

          {step === "form" && (
            <form className="space-y-4" onSubmit={onSubmit}>
              <div className="space-y-2">
                <Label htmlFor="amount">Amount (INR)</Label>
                <Input id="amount" type="number" min="1" step="0.01" value={amount} onChange={(e) => setAmount(e.target.value)} required />
              </div>
              <div className="space-y-2">
                <Label htmlFor="paymentMethod">Payment method</Label>
                <Select value={paymentMethod} onValueChange={setPaymentMethod}>
                  <SelectTrigger id="paymentMethod">
                    <SelectValue placeholder="Select a payment method" />
                  </SelectTrigger>
                  <SelectContent>
                    {PAYMENT_METHODS.map((m) => (
                      <SelectItem key={m.value} value={m.value}>{m.label}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label htmlFor="description">Description (optional)</Label>
                <Textarea id="description" rows={2} value={description} onChange={(e) => setDescription(e.target.value)} />
              </div>
              <Button type="submit" disabled={!amount || !paymentMethod}>Proceed to pay</Button>
            </form>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
