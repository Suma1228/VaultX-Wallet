import { createFileRoute } from "@tanstack/react-router";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { toast } from "sonner";
import { RequireAuth } from "@/components/RequireAuth";
import { authApi } from "@/lib/api-client";
import { useAuth } from "@/contexts/AuthContext";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

export const Route = createFileRoute("/profile")({
  component: () => (
    <RequireAuth>
      <Profile />
    </RequireAuth>
  ),
});

function Profile() {
  const { user } = useAuth();
  const qc = useQueryClient();
  const meQ = useQuery({ queryKey: ["me"], queryFn: () => authApi.me(), enabled: !!user });
  const [form, setForm] = useState<any>({});
  const [pw, setPw] = useState({ currentPassword: "", newPassword: "" });
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (meQ.data) {
      setForm({
        firstName: meQ.data.firstName || "",
        lastName: meQ.data.lastName || "",
        phoneNumber: meQ.data.phoneNumber || "",
        dateOfBirth: meQ.data.dateOfBirth || "",
        address: meQ.data.address || "",
        city: meQ.data.city || "",
        state: meQ.data.state || "",
        country: meQ.data.country || "",
        pinCode: meQ.data.pinCode || "",
      });
    }
  }, [meQ.data]);

  const set = (k: string) => (e: React.ChangeEvent<HTMLInputElement>) =>
    setForm((f: any) => ({ ...f, [k]: e.target.value }));

  const onSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) return;
    setSaving(true);
    try {
      await authApi.updateProfile(user.userId, form);
      toast.success("Profile updated");
      qc.invalidateQueries({ queryKey: ["me"] });
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Failed");
    } finally {
      setSaving(false);
    }
  };

  const onChangePw = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!pw.currentPassword || !pw.newPassword) return;
    try {
      await authApi.changePassword(pw);
      toast.success("Password changed");
      setPw({ currentPassword: "", newPassword: "" });
    } catch (err) {
      toast.error(err instanceof Error ? err.message : "Failed");
    }
  };

  return (
    <div className="space-y-6 max-w-3xl">
      <div>
        <h1 className="text-3xl font-bold">Profile</h1>
        <p className="text-muted-foreground mt-1">Manage your account details.</p>
      </div>

      <Card>
        <CardHeader>
          <CardTitle>Personal information</CardTitle>
          <CardDescription>{meQ.data?.email}</CardDescription>
        </CardHeader>
        <CardContent>
          <form className="grid grid-cols-2 gap-4" onSubmit={onSave}>
            <div className="space-y-2"><Label>First name</Label><Input value={form.firstName || ""} onChange={set("firstName")} /></div>
            <div className="space-y-2"><Label>Last name</Label><Input value={form.lastName || ""} onChange={set("lastName")} /></div>
            <div className="space-y-2"><Label>Phone</Label><Input value={form.phoneNumber || ""} onChange={set("phoneNumber")} /></div>
            <div className="space-y-2"><Label>Date of birth</Label><Input type="date" value={form.dateOfBirth || ""} onChange={set("dateOfBirth")} /></div>
            <div className="space-y-2 col-span-2"><Label>Address</Label><Input value={form.address || ""} onChange={set("address")} /></div>
            <div className="space-y-2"><Label>City</Label><Input value={form.city || ""} onChange={set("city")} /></div>
            <div className="space-y-2"><Label>State</Label><Input value={form.state || ""} onChange={set("state")} /></div>
            <div className="space-y-2"><Label>Country</Label><Input value={form.country || ""} onChange={set("country")} /></div>
            <div className="space-y-2"><Label>PIN code</Label><Input value={form.pinCode || ""} onChange={set("pinCode")} /></div>
            <div className="col-span-2">
              <Button type="submit" disabled={saving}>{saving ? "Saving…" : "Save changes"}</Button>
            </div>
          </form>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Change password</CardTitle>
        </CardHeader>
        <CardContent>
          <form className="space-y-4 max-w-sm" onSubmit={onChangePw}>
            <div className="space-y-2"><Label>Current password</Label><Input type="password" value={pw.currentPassword} onChange={(e) => setPw({ ...pw, currentPassword: e.target.value })} /></div>
            <div className="space-y-2"><Label>New password</Label><Input type="password" value={pw.newPassword} onChange={(e) => setPw({ ...pw, newPassword: e.target.value })} /></div>
            <Button type="submit">Update password</Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
