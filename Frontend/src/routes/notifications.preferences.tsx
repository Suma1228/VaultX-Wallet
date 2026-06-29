import { createFileRoute } from "@tanstack/react-router";
import { useEffect, useState } from "react";
import { toast } from "sonner";
import { RequireAuth } from "@/components/RequireAuth";
import { notificationApi, ApiError, type NotificationPreferences } from "@/lib/api-client";
import { useAuth } from "@/contexts/AuthContext";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Switch } from "@/components/ui/switch";

export const Route = createFileRoute("/notifications/preferences")({
  component: () => (
    <RequireAuth>
      <Prefs />
    </RequireAuth>
  ),
});

const toggleFields = [
  { key: "emailEnabled", label: "Email notifications" },
  { key: "smsEnabled", label: "SMS notifications" },
  { key: "creditAlerts", label: "Credit alerts" },
  { key: "debitAlerts", label: "Debit alerts" },
  { key: "transferAlerts", label: "Transfer alerts" },
  { key: "securityAlerts", label: "Security alerts" },
  { key: "spendAlerts", label: "Spend alerts" },
] as const;

function Prefs() {
  const { user } = useAuth();
  const [prefs, setPrefs] = useState<NotificationPreferences | null>(null);
  const [exists, setExists] = useState(false);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!user) return;
    (async () => {
      try {
        const data = await notificationApi.getPrefs(user.userId);
        setPrefs(data);
        setExists(true);
      } catch (err) {
        if (err instanceof ApiError && err.status === 404) {
          setPrefs({
            userId: user.userId,
            email: user.email,
            phoneNumber: "",
            emailEnabled: true,
            smsEnabled: false,
            creditAlerts: true,
            debitAlerts: true,
            transferAlerts: true,
            securityAlerts: true,
            spendAlerts: true,
          });
          setExists(false);
        } else {
          toast.error(err instanceof Error ? err.message : "Failed to load preferences");
        }
      } finally {
        setLoading(false);
      }
    })();
  }, [user]);

  if (loading || !prefs) {
    return <div className="text-muted-foreground">Loading…</div>;
  }

  const onSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setSaving(true);
    try {
      if (exists) {
        const updated = await notificationApi.updatePrefs(prefs.userId, prefs);
        setPrefs(updated);
      } else {
        const created = await notificationApi.createPrefs(prefs);
        setPrefs(created);
        setExists(true);
      }
      toast.success("Preferences saved");
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Failed");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="max-w-2xl space-y-6">
      <div>
        <h1 className="text-3xl font-bold">Notification preferences</h1>
        <p className="text-muted-foreground mt-1">Choose what alerts you receive.</p>
      </div>
      <Card>
        <CardHeader>
          <CardTitle>Contact</CardTitle>
          <CardDescription>Where we send your alerts.</CardDescription>
        </CardHeader>
        <CardContent>
          <form className="space-y-6" onSubmit={onSave}>
            <div className="grid grid-cols-2 gap-4">
              <div className="space-y-2">
                <Label>Email</Label>
                <Input type="email" value={prefs.email} onChange={(e) => setPrefs({ ...prefs, email: e.target.value })} />
              </div>
              <div className="space-y-2">
                <Label>Phone number</Label>
                <Input value={prefs.phoneNumber} onChange={(e) => setPrefs({ ...prefs, phoneNumber: e.target.value })} />
              </div>
            </div>
            <div className="space-y-3 pt-2 border-t">
              {toggleFields.map((f) => (
                <div key={f.key} className="flex items-center justify-between py-1">
                  <Label htmlFor={f.key} className="font-normal">{f.label}</Label>
                  <Switch
                    id={f.key}
                    checked={(prefs as any)[f.key]}
                    onCheckedChange={(v) => setPrefs({ ...prefs, [f.key]: v } as NotificationPreferences)}
                  />
                </div>
              ))}
            </div>
            <Button type="submit" disabled={saving}>{saving ? "Saving…" : "Save preferences"}</Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
