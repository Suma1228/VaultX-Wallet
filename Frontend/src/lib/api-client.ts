import { AUTH_BASE_URL, NOTIFICATION_BASE_URL, WALLET_BASE_URL } from "@/config/api";
import { authStorage } from "./auth-storage";

export class ApiError extends Error {
  status: number;
  errorCode?: string;
  isNetwork: boolean;
  constructor(message: string, status: number, errorCode?: string, isNetwork = false) {
    super(message);
    this.status = status;
    this.errorCode = errorCode;
    this.isNetwork = isNetwork;
  }
}

type Envelope<T> = { success: boolean; message: string; data: T; timestamp: number };

interface RequestOpts {
  method?: string;
  body?: unknown;
  auth?: boolean; // attach Authorization + X-User-Id
  unwrap?: boolean; // unwrap data envelope
}

async function request<T>(baseUrl: string, path: string, opts: RequestOpts = {}): Promise<T> {
  const { method = "GET", body, auth = false, unwrap = true } = opts;
  const headers: Record<string, string> = { "Content-Type": "application/json" };

  if (auth) {
    const token = authStorage.getToken();
    const user = authStorage.getUser();
    if (!token || !user) {
      throw new ApiError("Not authenticated", 401);
    }
    headers["Authorization"] = `Bearer ${token}`;
    headers["X-User-Id"] = user.userId;
  }

  let res: Response;
  try {
    res = await fetch(`${baseUrl}${path}`, {
      method,
      headers,
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch (e) {
    throw new ApiError(
      "Couldn't reach the server. Please check your connection or backend services.",
      0,
      undefined,
      true,
    );
  }

  const text = await res.text();
  const json = text ? safeJson(text) : null;

  if (!res.ok) {
    if (auth && (res.status === 401 || res.status === 403)) {
      authStorage.clear();
      if (typeof window !== "undefined" && !window.location.pathname.startsWith("/login")) {
        window.location.href = "/login";
      }
    }
    const msg =
      (json && (json.message || json.error)) || `Request failed with status ${res.status}`;
    throw new ApiError(msg, res.status, json?.errorCode);
  }

  if (!unwrap) return json as T;
  if (json && typeof json === "object" && "data" in json) {
    return (json as Envelope<T>).data;
  }
  return json as T;
}

function safeJson(t: string): any {
  try {
    return JSON.parse(t);
  } catch {
    return null;
  }
}

// ============== AUTH ==============
export const authApi = {
  register: (body: any) => request<any>(AUTH_BASE_URL, "/api/users/register", { method: "POST", body }),
  login: (body: { email: string; password: string }) =>
    request<{
      token: string;
      tokenType: string;
      userId: string;
      email: string;
      firstName: string;
      lastName: string;
      accountStatus: string;
    }>(AUTH_BASE_URL, "/api/users/login", { method: "POST", body }),
  logout: () => request(AUTH_BASE_URL, "/api/users/logout", { method: "POST", auth: true }),
  me: () => request<any>(AUTH_BASE_URL, "/api/users/me", { auth: true }),
  updateProfile: (userId: string, body: any) =>
    request<any>(AUTH_BASE_URL, `/api/users/profile/${userId}`, { method: "PUT", body, auth: true }),
  changePassword: (body: { currentPassword: string; newPassword: string }) =>
    request(AUTH_BASE_URL, "/api/users/change-password", { method: "POST", body, auth: true }),
  forgotPassword: (email: string) =>
    request(AUTH_BASE_URL, "/api/users/forgot-password", { method: "POST", body: { email } }),
  lookupByPhone: (phoneNumber: string) =>
    request<{ userId: string; firstName: string; lastName: string }>(
      AUTH_BASE_URL,
      `/api/users/lookup-by-phone?phoneNumber=${encodeURIComponent(phoneNumber)}`,
      { auth: true },
    ),
};

// ============== WALLET ==============
export interface WalletResponse {
  walletId: string;
  userId: string;
  balance: number;
  currency: string;
  status: string;
  dailyLimit: number;
  monthlyLimit: number;
  dailySpent: number;
  monthlySpent: number;
  createdAt: string;
  updatedAt: string;
}
export interface BalanceResponse {
  walletId: string;
  userId: string;
  balance: number;
  currency: string;
  status: string;
  lastUpdated: string;
}
export interface TransactionResponse {
  transactionId: string;
  walletId: string;
  userId: string;
  amount: number;
  type: string;
  status: string;
  description: string | null;
  balanceAfter: number | null;
  transferId: string | null;
  senderUserId: string | null;
  recipientUserId: string | null;
  paymentMethod: string | null;
  referenceId: string | null;
  failureReason: string | null;
  createdAt: string;
  completedAt: string | null;
}
export interface PageResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
}

export const walletApi = {
  create: () => request<WalletResponse>(WALLET_BASE_URL, "/api/wallet/create", { method: "POST", auth: true }),
  getByUser: (userId: string) =>
    request<WalletResponse>(WALLET_BASE_URL, `/api/wallet/user/${userId}`, { auth: true }),
  balance: () => request<BalanceResponse>(WALLET_BASE_URL, "/api/wallet/balance", { auth: true }),
  addMoney: (body: { amount: number; description?: string; paymentMethod?: string; referenceId?: string }) =>
    request<TransactionResponse>(WALLET_BASE_URL, "/api/wallet/add-money", {
      method: "POST",
      body,
      auth: true,
    }),
  withdraw: (body: { amount: number; description?: string; bankAccountNumber?: string; ifscCode?: string }) =>
    request<TransactionResponse>(WALLET_BASE_URL, "/api/wallet/withdraw-money", {
      method: "POST",
      body,
      auth: true,
    }),
transfer: (body: { toUserId: string; amount: number; description?: string; senderName?: string; recipientName?: string }) =>
    request<any>(WALLET_BASE_URL, "/api/wallet/transfer", { method: "POST", body, auth: true }),
  transactions: (page = 0, size = 20) =>
    request<PageResponse<TransactionResponse>>(
      WALLET_BASE_URL,
      `/api/wallet/transactions?page=${page}&size=${size}`,
      { auth: true },
    ),
  transactionsByDate: (startDate: string, endDate: string) =>
    request<TransactionResponse[]>(
      WALLET_BASE_URL,
      `/api/wallet/transactions/date-range?startDate=${encodeURIComponent(startDate)}&endDate=${encodeURIComponent(endDate)}`,
      { auth: true },
    ),
};

// ============== NOTIFICATIONS ==============
// No auth headers; envelope shape unknown — try unwrap, fall back to raw.
async function notifRequest<T>(path: string, opts: RequestOpts = {}): Promise<T> {
  const { method = "GET", body } = opts;
  let res: Response;
  try {
    res = await fetch(`${NOTIFICATION_BASE_URL}${path}`, {
      method,
      headers: { "Content-Type": "application/json" },
      body: body !== undefined ? JSON.stringify(body) : undefined,
    });
  } catch {
    throw new ApiError("Couldn't reach the notification server.", 0, undefined, true);
  }
  const text = await res.text();
  const json = text ? safeJson(text) : null;
  if (!res.ok) {
    const msg = (json && (json.message || json.error)) || `Request failed (${res.status})`;
    throw new ApiError(msg, res.status, json?.errorCode);
  }
  if (json && typeof json === "object" && "data" in json && "success" in json) {
    return (json as any).data as T;
  }
  return json as T;
}

export interface NotificationPreferences {
  userId: string;
  email: string;
  phoneNumber: string;
  emailEnabled: boolean;
  smsEnabled: boolean;
  creditAlerts: boolean;
  debitAlerts: boolean;
  transferAlerts: boolean;
  securityAlerts: boolean;
  spendAlerts: boolean;
}

export interface NotificationLog {
  notificationId: string;
  userId: string;
  eventType: string;
  notificationType: string;
  status: string;
  recipient: string | null;
  subject: string | null;
  transactionId: string | null;
  walletId: string | null;
  amount: number | null;
  balanceAfter: number | null;
  failureReason: string | null;
  retryCount: number;
  createdAt: string;
  sentAt: string | null;
}

export const notificationApi = {
  createPrefs: (body: NotificationPreferences) =>
    notifRequest<NotificationPreferences>("/api/notifications/preferences", { method: "POST", body }),
  getPrefs: (userId: string) =>
    notifRequest<NotificationPreferences>(`/api/notifications/preferences/${userId}`),
  updatePrefs: (userId: string, body: NotificationPreferences) =>
    notifRequest<NotificationPreferences>(`/api/notifications/preferences/${userId}`, {
      method: "PUT",
      body,
    }),
  history: (userId: string, page = 0, size = 20) =>
    notifRequest<PageResponse<NotificationLog> | NotificationLog[]>(
      `/api/notifications/history/${userId}?page=${page}&size=${size}`,
    ),
};
