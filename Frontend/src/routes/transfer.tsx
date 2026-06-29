import { createFileRoute } from "@tanstack/react-router";
import { useState } from "react";
import { useQueryClient } from "@tanstack/react-query";
import { toast } from "sonner";
import { RequireAuth } from "@/components/RequireAuth";
import { authApi, walletApi, ApiError } from "@/lib/api-client";
import { useAuth } from "@/contexts/AuthContext";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";

export const Route = createFileRoute("/transfer")({
  component: () => (
    <RequireAuth>
      <Transfer />
    </RequireAuth>
  ),
});

interface Recipient {
  userId: string;
  firstName: string;
  lastName: string;
}

function Transfer() {
  const qc = useQueryClient();
  const { user } = useAuth();

  const [phone, setPhone] = useState("");
  const [recipient, setRecipient] = useState<Recipient | null>(null);
  const [lookupLoading, setLookupLoading] = useState(false);

  const [amount, setAmount] = useState("");
  const [description, setDescription] = useState("");
  const [loading, setLoading] = useState(false);

  const onPhoneChange = (value: string) => {
    setPhone(value);
    if (recipient) setRecipient(null); // stale resolved recipient — force re-lookup
  };

  const onFindRecipient = async () => {
    const trimmed = phone.trim();
    if (!trimmed) return toast.error("Enter a phone number");
    setLookupLoading(true);
    try {
      const found = await authApi.lookupByPhone(trimmed);
      if (user && found.userId === user.userId) {
        toast.error("You can't send money to yourself");
        return;
      }
      setRecipient(found);
    } catch (err) {
      if (err instanceof ApiError && err.status === 404) {
        toast.error("No user found with this phone number");
      } else {
        toast.error(err instanceof Error ? err.message : "Couldn't look up that number");
      }
    } finally {
      setLookupLoading(false);
    }
  };

  const onChangeRecipient = () => {
    setRecipient(null);
    setPhone("");
  };

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!recipient) return toast.error("Find a recipient first");
    const num = Number(amount);
    if (!num || num <= 0) return toast.error("Enter a valid amount");
    setLoading(true);
    try {
   await walletApi.transfer({
    toUserId: recipient.userId,
    amount: num,
    description: description || undefined,
    senderName: user ? `${user.firstName} ${user.lastName}`.trim() : undefined,
    recipientName: `${recipient.firstName} ${recipient.lastName}`.trim(),
  });
      toast.success("Transfer sent");
      setPhone(""); setRecipient(null); setAmount(""); setDescription("");
      qc.invalidateQueries();
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-xl">
      <h1 className="text-3xl font-bold mb-6">Send money</h1>
      <Card>
        <CardHeader>
          <CardTitle>Transfer to another user</CardTitle>
          <CardDescription>Enter the recipient's phone number to find them, then send.</CardDescription>
        </CardHeader>
        <CardContent>
          <div className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="phone">Recipient's phone number</Label>
              <div className="flex gap-2">
                <Input
                  id="phone"
                  value={phone}
                  onChange={(e) => onPhoneChange(e.target.value)}
                  disabled={lookupLoading}
                  placeholder="9876543210"
                />
                <Button
                  type="button"
                  variant="outline"
                  onClick={onFindRecipient}
                  disabled={lookupLoading || !phone.trim()}
                >
                  {lookupLoading ? "Finding…" : "Find"}
                </Button>
              </div>
            </div>

            {recipient && (
              <div className="flex items-center justify-between rounded-md border p-3 bg-muted/50">
                <div>
                  <p className="text-sm text-muted-foreground">Sending to</p>
                  <p className="font-medium">{recipient.firstName} {recipient.lastName}</p>
                </div>
                <Button type="button" variant="ghost" size="sm" onClick={onChangeRecipient}>
                  Change
                </Button>
              </div>
            )}

            <form className="space-y-4" onSubmit={onSubmit}>
              <div className="space-y-2">
                <Label htmlFor="amount">Amount (INR)</Label>
                <Input
                  id="amount"
                  type="number"
                  min="1"
                  step="0.01"
                  value={amount}
                  onChange={(e) => setAmount(e.target.value)}
                  disabled={!recipient}
                  required
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="description">Description (optional)</Label>
                <Textarea
                  id="description"
                  rows={2}
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  disabled={!recipient}
                />
              </div>
              <Button type="submit" disabled={loading || !recipient}>
                {loading ? "Sending…" : "Send"}
              </Button>
            </form>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
