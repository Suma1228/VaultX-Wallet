import { useAuth } from "@/contexts/AuthContext";
import { Navigate } from "@tanstack/react-router";
import type { ReactNode } from "react";
import { AppLayout } from "./AppLayout";

export function RequireAuth({ children }: { children: ReactNode }) {
  const { user, ready } = useAuth();
  if (!ready) {
    return (
      <div className="min-h-screen flex items-center justify-center text-muted-foreground">
        Loading…
      </div>
    );
  }
  if (!user) return <Navigate to="/login" />;
  return <AppLayout>{children}</AppLayout>;
}
