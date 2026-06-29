import { createContext, useContext, useEffect, useState, type ReactNode } from "react";
import { authStorage, type StoredUser } from "@/lib/auth-storage";

interface AuthContextValue {
  user: StoredUser | null;
  token: string | null;
  ready: boolean;
  setSession: (token: string, user: StoredUser) => void;
  clearSession: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<StoredUser | null>(null);
  const [token, setToken] = useState<string | null>(null);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    setUser(authStorage.getUser());
    setToken(authStorage.getToken());
    setReady(true);
  }, []);

  const setSession = (t: string, u: StoredUser) => {
    authStorage.set(t, u);
    setToken(t);
    setUser(u);
  };
  const clearSession = () => {
    authStorage.clear();
    setToken(null);
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ user, token, ready, setSession, clearSession }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside AuthProvider");
  return ctx;
}
