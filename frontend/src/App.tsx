import { useEffect, useState, useCallback } from "react";
import "./App.css";

// ─────────────────────────────────────────────
// Types
// ─────────────────────────────────────────────

type Role = "MANAGER" | "DISPATCHER" | "TECHNICIAN" | "CUSTOMER";

type Status =
  | "NEW" | "ASSIGNED" | "IN_PROGRESS" | "ON_HOLD"
  | "COMPLETED" | "CLOSED" | "CANCELLED";

type Priority = "LOW" | "MEDIUM" | "HIGH" | "URGENT";

interface WorkOrder {
  id: number; code: string; title: string; description?: string;
  priority: Priority; status: Status; slaDueDate?: string;
  customerId?: number; customerName?: string;
  siteId?: number; siteName?: string;
  assigneeId?: number; assigneeEmail?: string;
}

interface Page<T> {
  content: T[]; number: number; size: number;
  totalElements: number; totalPages: number;
  first: boolean; last: boolean;
}

interface User {
  id: number; name: string; email: string; role: Role;
  customer?: { id: number; companyName: string; contactEmail: string };
}

interface LoginResult {
  token: string; role: Role; userId: number; name: string; customerId?: number;
}

interface Dashboard {
  totalWorkOrders: number; overdueWorkOrders: number;
  completedWorkOrders: number; inProgressWorkOrders: number;
  slaCompliancePercentage: number;
  statusCounts: Record<string, number>;
  priorityCounts: Record<string, number>;
  technicianCounts: Record<string, number>;
  siteCounts: Record<string, number>;
}

interface Notification {
  id: number; message: string; read: boolean; createdAt: string;
}

interface Part { id: number; name: string; stockQuantity: number; unitPrice: number; }

// ─────────────────────────────────────────────
// API helpers
// ─────────────────────────────────────────────

// In dev (npm run dev): Vite proxy forwards /api → localhost:8080, so BASE = ""
// In Docker (nginx):   nginx proxy_pass forwards /api → backend:8080, so BASE = ""
// Override via VITE_API_URL env var if needed (e.g. cloud deployment)
const BASE = (import.meta.env.VITE_API_URL ?? "").replace(/\/$/, "") + "/api";

async function api<T>(
  path: string,
  token: string,
  options: RequestInit = {}
): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
      ...(options.headers || {}),
    },
  });
  if (!res.ok) {
    const body = await res.text();
    let msg = `HTTP ${res.status}`;
    try { msg = JSON.parse(body).message || msg; } catch { /* ignore */ }
    throw new Error(msg);
  }
  const text = await res.text();
  return text ? JSON.parse(text) : (undefined as T);
}

// ─────────────────────────────────────────────
// Utility
// ─────────────────────────────────────────────

function fmt(d?: string) {
  if (!d) return "—";
  const dt = new Date(d);
  return isNaN(dt.getTime()) ? d : dt.toLocaleString("en-IN", { day: "2-digit", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit" });
}

function slaClass(d?: string, status?: Status): string {
  if (!d || status === "COMPLETED" || status === "CLOSED" || status === "CANCELLED") return "";
  const ms = new Date(d).getTime() - Date.now();
  if (ms < 0) return "sla-breached";
  if (ms < 4 * 3600 * 1000) return "sla-warn";
  return "sla-ok";
}

function StatusBadge({ s }: { s: Status }) {
  return <span className={`badge badge-${s}`}>{s.replace("_", " ")}</span>;
}
function PriorityBadge({ p }: { p: Priority }) {
  return <span className={`badge badge-${p}`}>{p}</span>;
}

// ─────────────────────────────────────────────
// Main App
// ─────────────────────────────────────────────

export default function App() {
  const [token, setToken] = useState(() => localStorage.getItem("token") || "");
  const [me, setMe] = useState<LoginResult | null>(() => {
    const s = localStorage.getItem("me");
    return s ? JSON.parse(s) : null;
  });

  function logout() {
    localStorage.removeItem("token");
    localStorage.removeItem("me");
    setToken(""); setMe(null);
  }

  if (!token || !me) return <LoginPage onLogin={(t, m) => { setToken(t); setMe(m); }} />;

  return (
    <div className="app">
      <AppHeader me={me} onLogout={logout} />
      <AppBody token={token} me={me} onUnauth={logout} />
    </div>
  );
}

// ─────────────────────────────────────────────
// Login
// ─────────────────────────────────────────────

function LoginPage({ onLogin }: { onLogin: (t: string, m: LoginResult) => void }) {
  const [email, setEmail] = useState("");
  const [pass, setPass] = useState("");
  const [err, setErr] = useState("");
  const [loading, setLoading] = useState(false);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setErr(""); setLoading(true);
    try {
      const res = await fetch(`${BASE}/auth/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ email, password: pass }),
      });
      if (!res.ok) { setErr("Invalid email or password."); return; }
      const data: LoginResult = await res.json();
      localStorage.setItem("token", data.token);
      localStorage.setItem("me", JSON.stringify(data));
      onLogin(data.token, data);
    } catch {
      setErr("Could not connect to server. Is the backend running?");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="login-page">
      <div className="login-card">
        <h1>KEYSTONE</h1>
        <h2>Field Service Management</h2>
        <form onSubmit={submit}>
          <div className="form-group">
            <label htmlFor="email">Email</label>
            <input id="email" type="email" value={email}
              onChange={e => setEmail(e.target.value)} placeholder="manager@keystone.example.com" required />
          </div>
          <div className="form-group">
            <label htmlFor="pass">Password</label>
            <input id="pass" type="password" value={pass}
              onChange={e => setPass(e.target.value)} placeholder="Password123!" required />
          </div>
          {err && <div className="alert alert-error">{err}</div>}
          <button type="submit" className="login-btn" disabled={loading}>
            {loading ? "Signing in…" : "Sign In"}
          </button>
        </form>
        <p className="text-muted text-sm mt-16" style={{ textAlign: "center" }}>
          Seed accounts use password: <strong>Password123!</strong>
        </p>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────
// Header
// ─────────────────────────────────────────────

function AppHeader({ me, onLogout }: { me: LoginResult; onLogout: () => void }) {
  return (
    <header className="header">
      <div className="flex items-center gap-8">
        <h1>KEYSTONE</h1>
        <span className="header-meta">{me.name} · {me.role}</span>
      </div>
      <div className="header-actions">
        <button className="btn btn-ghost btn-sm" onClick={onLogout}>Sign Out</button>
      </div>
    </header>
  );
}

// ─────────────────────────────────────────────
// Router — role-based view dispatch
// ─────────────────────────────────────────────

function AppBody({ token, me, onUnauth }: { token: string; me: LoginResult; onUnauth: () => void }) {
  const role = me.role;

  if (role === "CUSTOMER") return <CustomerPortal token={token} me={me} onUnauth={onUnauth} />;
  if (role === "TECHNICIAN") return <TechnicianView token={token} me={me} onUnauth={onUnauth} />;
  return <ManagerDispatcherView token={token} me={me} onUnauth={onUnauth} />;
}

// ─────────────────────────────────────────────
// MANAGER / DISPATCHER
// ─────────────────────────────────────────────

type ManagerTab = "board" | "list" | "dashboard" | "customers" | "users" | "parts" | "overdue";

function ManagerDispatcherView({ token, me, onUnauth }: { token: string; me: LoginResult; onUnauth: () => void }) {
  const [tab, setTab] = useState<ManagerTab>("board");

  const tabs: { key: ManagerTab; label: string }[] = [
    { key: "board",     label: "Board" },
    { key: "list",      label: "Work Orders" },
    { key: "overdue",   label: "Overdue / SLA" },
    { key: "dashboard", label: "Dashboard" },
    { key: "customers", label: "Customers & Sites" },
    { key: "users",     label: "Users" },
    { key: "parts",     label: "Parts" },
  ];

  return (
    <main>
      <div className="tab-bar">
        {tabs.map(t => (
          <button key={t.key} className={`tab-btn ${tab === t.key ? "active" : ""}`}
            onClick={() => setTab(t.key)}>{t.label}</button>
        ))}
      </div>
      {tab === "board"     && <WorkOrderBoard token={token} me={me} onUnauth={onUnauth} />}
      {tab === "list"      && <WorkOrderList  token={token} me={me} onUnauth={onUnauth} />}
      {tab === "overdue"   && <OverdueView    token={token} onUnauth={onUnauth} />}
      {tab === "dashboard" && <DashboardView  token={token} onUnauth={onUnauth} />}
      {tab === "customers" && <CustomersView  token={token} me={me} />}
      {tab === "users"     && <UsersView      token={token} me={me} />}
      {tab === "parts"     && <PartsView      token={token} me={me} />}
    </main>
  );
}

// ─────────────────────────────────────────────
// Kanban Board
// ─────────────────────────────────────────────

const BOARD_COLS: { key: Status; label: string }[] = [
  { key: "NEW",         label: "New" },
  { key: "ASSIGNED",    label: "Assigned" },
  { key: "IN_PROGRESS", label: "In Progress" },
  { key: "ON_HOLD",     label: "On Hold" },
  { key: "COMPLETED",   label: "Completed" },
  { key: "CLOSED",      label: "Closed" },
  { key: "CANCELLED",   label: "Cancelled" },
];

function WorkOrderBoard({ token, me, onUnauth }: { token: string; me: LoginResult; onUnauth: () => void }) {
  const [orders, setOrders] = useState<WorkOrder[]>([]);
  const [technicians, setTechnicians] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");
  const [assigning, setAssigning] = useState<number | null>(null);
  const [transitioning, setTransitioning] = useState<number | null>(null);

  const load = useCallback(async () => {
    try {
      setLoading(true); setErr("");
      const p: Page<WorkOrder> = await api("/work-orders?page=0&size=200", token);
      setOrders(p.content || []);
      if (me.role === "MANAGER" || me.role === "DISPATCHER") {
        const users: User[] = await api("/users", token);
        setTechnicians(users.filter(u => u.role === "TECHNICIAN"));
      }
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : String(e);
      if (msg.includes("401") || msg.includes("403")) onUnauth();
      else setErr("Failed to load work orders.");
    } finally { setLoading(false); }
  }, [token, me.role, onUnauth]);

  useEffect(() => { void load(); }, [load]);

  async function assign(orderId: number, techId: number) {
    try {
      setAssigning(orderId);
      const updated: WorkOrder = await api(`/work-orders/${orderId}/assign?assigneeId=${techId}`, token, { method: "PATCH" });
      setOrders(o => o.map(x => x.id === orderId ? updated : x));
    } catch (e: unknown) {
      setErr(e instanceof Error ? e.message : "Assignment failed.");
    } finally { setAssigning(null); }
  }

  async function transition(orderId: number, status: Status) {
    try {
      setTransitioning(orderId);
      const updated: WorkOrder = await api(`/work-orders/${orderId}/status?status=${status}`, token, { method: "PATCH" });
      setOrders(o => o.map(x => x.id === orderId ? updated : x));
    } catch (e: unknown) {
      setErr(e instanceof Error ? e.message : "Status update failed.");
    } finally { setTransitioning(null); }
  }

  if (loading) return <div className="empty-state">Loading board…</div>;

  return (
    <div>
      {err && <div className="alert alert-error mb-16">{err}</div>}
      <div className="flex justify-between items-center mb-16">
        <h2 className="page-title" style={{ margin: 0 }}>Work Order Board</h2>
        <button className="btn btn-outline btn-sm" onClick={load}>Refresh</button>
      </div>
      <div className="board">
        {BOARD_COLS.map(col => {
          const colOrders = orders.filter(o => o.status === col.key);
          return (
            <div className="board-column" key={col.key}>
              <div className="board-column-header">
                <StatusBadge s={col.key} />
                <span className="board-column-count">{colOrders.length}</span>
              </div>
              {colOrders.length === 0
                ? <div className="board-empty">No orders</div>
                : colOrders.map(o => (
                  <div className="board-card" key={o.id}>
                    <div className="board-card-top">
                      <span className="board-card-code">{o.code}</span>
                      <PriorityBadge p={o.priority} />
                    </div>
                    <h4 className="board-card">{o.title}</h4>
                    <div className="board-card-meta">
                      <div>📍 {o.siteName || "—"}</div>
                      <div>👤 {o.assigneeEmail || "Unassigned"}</div>
                      <div className={slaClass(o.slaDueDate, o.status)}>
                        ⏱ {fmt(o.slaDueDate)}
                      </div>
                    </div>
                    {/* Quick status transitions */}
                    {o.status !== "CLOSED" && o.status !== "CANCELLED" && (
                      <div style={{ marginTop: 8 }}>
                        <select
                          style={{ fontSize: 12 }}
                          value={o.status}
                          disabled={transitioning === o.id}
                          onChange={e => transition(o.id, e.target.value as Status)}
                        >
                          {BOARD_COLS.filter(c => c.key !== "CLOSED" && c.key !== "CANCELLED")
                            .map(c => <option key={c.key} value={c.key}>{c.label}</option>)}
                          <option value="CLOSED">Closed</option>
                          <option value="CANCELLED">Cancelled</option>
                        </select>
                      </div>
                    )}
                    {/* Assign technician */}
                    {(me.role === "MANAGER" || me.role === "DISPATCHER")
                      && o.status !== "CLOSED" && o.status !== "CANCELLED" && (
                      <select
                        style={{ marginTop: 6, fontSize: 12 }}
                        value={o.assigneeId || ""}
                        disabled={assigning === o.id}
                        onChange={e => e.target.value && assign(o.id, +e.target.value)}
                      >
                        <option value="">Assign technician…</option>
                        {technicians.map(t => (
                          <option key={t.id} value={t.id}>{t.name}</option>
                        ))}
                      </select>
                    )}
                  </div>
                ))}
            </div>
          );
        })}
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────
// Work Order List (paginated, filterable)
// ─────────────────────────────────────────────

function WorkOrderList({ token, me: _me, onUnauth }: { token: string; me: LoginResult; onUnauth: () => void }) {
  const [page, setPage] = useState<Page<WorkOrder> | null>(null);
  const [pageNum, setPageNum] = useState(0);
  const [search, setSearch] = useState("");
  const [status, setStatus] = useState("");
  const [priority, setPriority] = useState("");
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");

  const load = useCallback(async (p: number) => {
    try {
      setLoading(true); setErr("");
      const qs = new URLSearchParams({ page: String(p), size: "15" });
      if (search)   qs.set("search", search);
      if (status)   qs.set("status", status);
      if (priority) qs.set("priority", priority);
      const data: Page<WorkOrder> = await api(`/work-orders?${qs}`, token);
      setPage(data); setPageNum(p);
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : String(e);
      if (msg.includes("401") || msg.includes("403")) onUnauth();
      else setErr("Failed to load work orders.");
    } finally { setLoading(false); }
  }, [token, search, status, priority, onUnauth]);

  useEffect(() => { void load(0); }, [load]);

  return (
    <div>
      <h2 className="page-title">Work Orders</h2>
      {err && <div className="alert alert-error mb-16">{err}</div>}
      {/* Filters */}
      <div className="card mb-16">
        <div className="flex gap-12 flex-wrap">
          <div className="form-group" style={{ flex: "1 1 180px", marginBottom: 0 }}>
            <label>Search</label>
            <input value={search} onChange={e => setSearch(e.target.value)} placeholder="Title…" />
          </div>
          <div className="form-group" style={{ flex: "0 0 150px", marginBottom: 0 }}>
            <label>Status</label>
            <select value={status} onChange={e => setStatus(e.target.value)}>
              <option value="">All</option>
              {BOARD_COLS.map(c => <option key={c.key} value={c.key}>{c.label}</option>)}
            </select>
          </div>
          <div className="form-group" style={{ flex: "0 0 140px", marginBottom: 0 }}>
            <label>Priority</label>
            <select value={priority} onChange={e => setPriority(e.target.value)}>
              <option value="">All</option>
              {(["LOW","MEDIUM","HIGH","URGENT"] as Priority[]).map(p => (
                <option key={p} value={p}>{p}</option>
              ))}
            </select>
          </div>
          <div style={{ display: "flex", alignItems: "flex-end" }}>
            <button className="btn btn-primary btn-sm" onClick={() => load(0)}>Filter</button>
          </div>
        </div>
      </div>

      {loading ? <div className="empty-state">Loading…</div> : !page || page.content.length === 0
        ? <div className="empty-state">No work orders found.</div>
        : (
          <>
            <div className="card table-wrap">
              <table>
                <thead>
                  <tr>
                    <th>Code</th><th>Title</th><th>Priority</th><th>Status</th>
                    <th>Customer</th><th>Site</th><th>Assignee</th><th>SLA Due</th>
                  </tr>
                </thead>
                <tbody>
                  {page.content.map(o => (
                    <tr key={o.id}>
                      <td><code>{o.code}</code></td>
                      <td>{o.title}</td>
                      <td><PriorityBadge p={o.priority} /></td>
                      <td><StatusBadge s={o.status} /></td>
                      <td>{o.customerName || "—"}</td>
                      <td>{o.siteName || "—"}</td>
                      <td className="text-sm text-muted">{o.assigneeEmail || "Unassigned"}</td>
                      <td><span className={`${slaClass(o.slaDueDate, o.status)} text-sm`}>{fmt(o.slaDueDate)}</span></td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            {page.totalPages > 1 && (
              <div className="pagination">
                <button className="btn btn-outline btn-sm" disabled={page.first}
                  onClick={() => load(pageNum - 1)}>← Prev</button>
                <span className="text-muted text-sm">Page {pageNum + 1} of {page.totalPages} ({page.totalElements} total)</span>
                <button className="btn btn-outline btn-sm" disabled={page.last}
                  onClick={() => load(pageNum + 1)}>Next →</button>
              </div>
            )}
          </>
        )}
    </div>
  );
}

// ─────────────────────────────────────────────
// Overdue / SLA view
// ─────────────────────────────────────────────

function OverdueView({ token, onUnauth }: { token: string; onUnauth: () => void }) {
  const [orders, setOrders] = useState<WorkOrder[]>([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");

  useEffect(() => {
    api<WorkOrder[]>("/work-orders/overdue", token)
      .then(setOrders)
      .catch((e: unknown) => {
        const msg = e instanceof Error ? e.message : String(e);
        if (msg.includes("401") || msg.includes("403")) onUnauth();
        else setErr("Failed to load overdue work orders.");
      })
      .finally(() => setLoading(false));
  }, [token, onUnauth]);

  if (loading) return <div className="empty-state">Loading…</div>;

  return (
    <div>
      <h2 className="page-title">Overdue / SLA Breached Work Orders</h2>
      {err && <div className="alert alert-error mb-16">{err}</div>}
      {orders.length === 0
        ? <div className="empty-state">✅ No overdue work orders — SLA is on track.</div>
        : (
          <div className="card table-wrap">
            <div className="alert alert-warn mb-16">{orders.length} work order(s) have breached SLA.</div>
            <table>
              <thead><tr><th>Code</th><th>Title</th><th>Priority</th><th>Status</th><th>Assignee</th><th>SLA Due</th></tr></thead>
              <tbody>
                {orders.map(o => (
                  <tr key={o.id}>
                    <td><code>{o.code}</code></td>
                    <td>{o.title}</td>
                    <td><PriorityBadge p={o.priority} /></td>
                    <td><StatusBadge s={o.status} /></td>
                    <td className="text-sm text-muted">{o.assigneeEmail || "Unassigned"}</td>
                    <td className="sla-breached text-sm">{fmt(o.slaDueDate)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
    </div>
  );
}

// ─────────────────────────────────────────────
// Dashboard
// ─────────────────────────────────────────────

function DashboardView({ token, onUnauth }: { token: string; onUnauth: () => void }) {
  const [data, setData] = useState<Dashboard | null>(null);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");

  useEffect(() => {
    api<Dashboard>("/dashboard", token)
      .then(setData)
      .catch((e: unknown) => {
        const msg = e instanceof Error ? e.message : String(e);
        if (msg.includes("401") || msg.includes("403")) onUnauth();
        else setErr("Failed to load dashboard.");
      })
      .finally(() => setLoading(false));
  }, [token, onUnauth]);

  if (loading) return <div className="empty-state">Loading dashboard…</div>;
  if (err) return <div className="alert alert-error">{err}</div>;
  if (!data) return <div className="empty-state">No data.</div>;

  return (
    <div>
      <h2 className="page-title">Dashboard</h2>
      <div className="stat-grid">
        <div className="card stat-card">
          <h3>Total</h3>
          <div className="stat-value">{data.totalWorkOrders}</div>
        </div>
        <div className="card stat-card">
          <h3>In Progress</h3>
          <div className="stat-value">{data.inProgressWorkOrders}</div>
        </div>
        <div className="card stat-card">
          <h3>Completed</h3>
          <div className="stat-value success">{data.completedWorkOrders}</div>
        </div>
        <div className="card stat-card">
          <h3>SLA Breached</h3>
          <div className={`stat-value ${data.overdueWorkOrders > 0 ? "danger" : "success"}`}>
            {data.overdueWorkOrders}
          </div>
        </div>
        <div className="card stat-card">
          <h3>SLA Compliance</h3>
          <div className={`stat-value ${data.slaCompliancePercentage >= 90 ? "success" : data.slaCompliancePercentage >= 70 ? "warn" : "danger"}`}>
            {data.slaCompliancePercentage.toFixed(1)}%
          </div>
        </div>
      </div>

      <div className="grid-2">
        <div className="card">
          <h3 className="section-title">By Status</h3>
          {Object.entries(data.statusCounts).length === 0
            ? <div className="text-muted text-sm">No data</div>
            : Object.entries(data.statusCounts).map(([s, n]) => (
              <div key={s} className="flex justify-between items-center mb-8">
                <StatusBadge s={s as Status} /><strong>{n}</strong>
              </div>
            ))}
        </div>
        <div className="card">
          <h3 className="section-title">By Priority</h3>
          {Object.entries(data.priorityCounts).length === 0
            ? <div className="text-muted text-sm">No data</div>
            : Object.entries(data.priorityCounts).map(([p, n]) => (
              <div key={p} className="flex justify-between items-center mb-8">
                <PriorityBadge p={p as Priority} /><strong>{n}</strong>
              </div>
            ))}
        </div>
        <div className="card">
          <h3 className="section-title">By Technician</h3>
          {Object.entries(data.technicianCounts).length === 0
            ? <div className="text-muted text-sm">No assignments yet</div>
            : Object.entries(data.technicianCounts).map(([t, n]) => (
              <div key={t} className="flex justify-between items-center mb-8">
                <span className="text-sm">{t}</span><strong>{n}</strong>
              </div>
            ))}
        </div>
        <div className="card">
          <h3 className="section-title">By Site</h3>
          {Object.entries(data.siteCounts).length === 0
            ? <div className="text-muted text-sm">No data</div>
            : Object.entries(data.siteCounts).map(([s, n]) => (
              <div key={s} className="flex justify-between items-center mb-8">
                <span className="text-sm">{s}</span><strong>{n}</strong>
              </div>
            ))}
        </div>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────
// Customers & Sites
// ─────────────────────────────────────────────

interface Customer { id: number; companyName: string; contactEmail: string; }
interface Site { id: number; name: string; address: string; city: string; state: string; postalCode: string; customer?: Customer; }

function CustomersView({ token, me }: { token: string; me: LoginResult }) {
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");

  const load = useCallback(() => {
    api<Customer[]>("/customers", token).then(setCustomers).catch(e => setErr(String(e))).finally(() => setLoading(false));
  }, [token]);

  useEffect(() => { load(); }, [load]);

  if (loading) return <div className="empty-state">Loading…</div>;

  return (
    <div>
      <h2 className="page-title">Customers &amp; Sites</h2>
      {err && <div className="alert alert-error mb-16">{err}</div>}
      {customers.length === 0
        ? <div className="empty-state">No customers found.</div>
        : (
          <div className="grid-2">
            {customers.map(c => (
              <CustomerCard key={c.id} customer={c} token={token} me={me} />
            ))}
          </div>
        )}
    </div>
  );
}

function CustomerCard({ customer, token, me: _me }: { customer: Customer; token: string; me: LoginResult }) {
  const [sites, setSites] = useState<Site[]>([]);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    if (!open) return;
    api<Site[]>(`/customers/${customer.id}/sites`, token).then(setSites).catch(() => {});
  }, [open, customer.id, token]);

  return (
    <div className="card">
      <div className="flex justify-between items-center">
        <div>
          <strong>{customer.companyName}</strong>
          <div className="text-sm text-muted">{customer.contactEmail}</div>
        </div>
        <button className="btn btn-outline btn-sm" onClick={() => setOpen(o => !o)}>
          {open ? "Hide Sites" : "View Sites"}
        </button>
      </div>
      {open && (
        <div className="mt-8">
          <div className="text-sm text-muted mb-8" style={{ fontWeight: 600, textTransform: "uppercase", letterSpacing: ".4px" }}>Sites</div>
          {sites.length === 0
            ? <div className="text-sm text-muted">No sites</div>
            : sites.map(s => (
              <div key={s.id} className="text-sm mb-8" style={{ paddingLeft: 8, borderLeft: "2px solid #e0e4ea" }}>
                <strong>{s.name}</strong> — {s.address}, {s.city}, {s.state} {s.postalCode}
              </div>
            ))}
        </div>
      )}
    </div>
  );
}

// ─────────────────────────────────────────────
// Users (MANAGER only)
// ─────────────────────────────────────────────

function UsersView({ token, me: _me }: { token: string; me: LoginResult }) {
  const [users, setUsers] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");

  useEffect(() => {
    api<User[]>("/users", token).then(setUsers).catch(e => setErr(String(e))).finally(() => setLoading(false));
  }, [token]);

  if (loading) return <div className="empty-state">Loading…</div>;

  return (
    <div>
      <h2 className="page-title">Users</h2>
      {err && <div className="alert alert-error mb-16">{err}</div>}
      <div className="card table-wrap">
        <table>
          <thead><tr><th>Name</th><th>Email</th><th>Role</th></tr></thead>
          <tbody>
            {users.map(u => (
              <tr key={u.id}>
                <td>{u.name}</td>
                <td className="text-sm text-muted">{u.email}</td>
                <td><span className="badge">{u.role}</span></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────
// Parts inventory
// ─────────────────────────────────────────────

function PartsView({ token, me: _me }: { token: string; me: LoginResult }) {
  const [parts, setParts] = useState<Part[]>([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");

  const load = () => {
    api<Part[]>("/parts", token).then(setParts).catch(e => setErr(String(e))).finally(() => setLoading(false));
  };

  useEffect(() => { load(); }, []);

  if (loading) return <div className="empty-state">Loading…</div>;

  return (
    <div>
      <h2 className="page-title">Parts Inventory</h2>
      {err && <div className="alert alert-error mb-16">{err}</div>}
      <div className="card table-wrap">
        <table>
          <thead><tr><th>Name</th><th>Stock</th><th>Unit Price (₹)</th></tr></thead>
          <tbody>
            {parts.map(p => (
              <tr key={p.id}>
                <td>{p.name}</td>
                <td>
                  <span className={p.stockQuantity === 0 ? "sla-breached" : p.stockQuantity < 5 ? "sla-warn" : "sla-ok"}>
                    {p.stockQuantity}
                  </span>
                </td>
                <td>{(p.unitPrice / 100).toFixed(2)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────
// TECHNICIAN view (mobile-first, responsive)
// ─────────────────────────────────────────────

type TechTab = "jobs" | "timelog" | "parts";

function TechnicianView({ token, me, onUnauth }: { token: string; me: LoginResult; onUnauth: () => void }) {
  const [tab, setTab] = useState<TechTab>("jobs");
  const [orders, setOrders] = useState<WorkOrder[]>([]);
  const [parts, setParts] = useState<Part[]>([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");

  const load = useCallback(async () => {
    try {
      setLoading(true); setErr("");
      const p: Page<WorkOrder> = await api("/work-orders?page=0&size=100", token);
      setOrders(p.content || []);
      const ps: Part[] = await api("/parts", token);
      setParts(ps);
    } catch (e: unknown) {
      const msg = e instanceof Error ? e.message : String(e);
      if (msg.includes("401") || msg.includes("403")) onUnauth();
      else setErr("Failed to load your jobs.");
    } finally { setLoading(false); }
  }, [token, onUnauth]);

  useEffect(() => { void load(); }, [load]);

  const activeOrders = orders.filter(o =>
    o.status !== "CLOSED" && o.status !== "CANCELLED" && o.status !== "COMPLETED");

  return (
    <main>
      <div className="tab-bar">
        <button className={`tab-btn ${tab === "jobs" ? "active" : ""}`} onClick={() => setTab("jobs")}>My Jobs ({activeOrders.length})</button>
        <button className={`tab-btn ${tab === "timelog" ? "active" : ""}`} onClick={() => setTab("timelog")}>Log Time</button>
        <button className={`tab-btn ${tab === "parts" ? "active" : ""}`} onClick={() => setTab("parts")}>Log Parts</button>
      </div>
      {err && <div className="alert alert-error mb-16">{err}</div>}
      {loading
        ? <div className="empty-state">Loading your jobs…</div>
        : tab === "jobs"
          ? <TechJobList orders={orders} token={token} onRefresh={load} />
          : tab === "timelog"
            ? <TechTimeLog orders={activeOrders} token={token} me={me} />
            : <TechPartsLog orders={activeOrders} parts={parts} token={token} me={me} />
      }
    </main>
  );
}

// Allowed status transitions a technician can perform
const TECH_TRANSITIONS: Partial<Record<Status, Status[]>> = {
  ASSIGNED:    ["IN_PROGRESS"],
  IN_PROGRESS: ["ON_HOLD", "COMPLETED"],
  ON_HOLD:     ["IN_PROGRESS"],
};

function TechJobList({ orders, token, onRefresh }: { orders: WorkOrder[]; token: string; onRefresh: () => void }) {
  const [transitioning, setTransitioning] = useState<number | null>(null);
  const [err, setErr] = useState("");

  async function doTransition(orderId: number, status: Status) {
    try {
      setTransitioning(orderId); setErr("");
      await api(`/work-orders/${orderId}/status?status=${status}`, token, { method: "PATCH" });
      onRefresh();
    } catch (e: unknown) {
      setErr(e instanceof Error ? e.message : "Status update failed.");
    } finally { setTransitioning(null); }
  }

  const activeOrders = orders.filter(o =>
    o.status !== "CLOSED" && o.status !== "CANCELLED");

  if (activeOrders.length === 0) return <div className="empty-state">No active jobs assigned to you.</div>;

  return (
    <div>
      <h2 className="page-title">My Jobs</h2>
      {err && <div className="alert alert-error mb-16">{err}</div>}
      <div className="grid-2">
        {activeOrders.map(o => {
          const transitions = TECH_TRANSITIONS[o.status] || [];
          return (
            <div className="card" key={o.id}>
              <div className="flex justify-between items-center mb-8">
                <span className="board-card-code">{o.code}</span>
                <PriorityBadge p={o.priority} />
              </div>
              <h3 style={{ marginBottom: 8 }}>{o.title}</h3>
              {o.description && <p className="text-sm text-muted mb-8">{o.description}</p>}
              <div className="text-sm mb-8">
                <div>📍 {o.siteName || "—"} ({o.customerName || "—"})</div>
                <div className={slaClass(o.slaDueDate, o.status)}>⏱ SLA: {fmt(o.slaDueDate)}</div>
              </div>
              <StatusBadge s={o.status} />
              {transitions.length > 0 && (
                <div className="flex gap-8 mt-8 flex-wrap">
                  {transitions.map(t => (
                    <button key={t} className="btn btn-primary btn-sm"
                      disabled={transitioning === o.id}
                      onClick={() => doTransition(o.id, t)}>
                      {t === "IN_PROGRESS" ? (o.status === "ON_HOLD" ? "▶ Resume" : "▶ Start") :
                       t === "ON_HOLD" ? "⏸ Hold" :
                       t === "COMPLETED" ? "✅ Complete" : t}
                    </button>
                  ))}
                </div>
              )}
            </div>
          );
        })}
      </div>
    </div>
  );
}

function TechTimeLog({ orders, token, me }: { orders: WorkOrder[]; token: string; me: LoginResult }) {
  const [orderId, setOrderId] = useState("");
  const [minutes, setMinutes] = useState("");
  const [note, setNote] = useState("");
  const [msg, setMsg] = useState({ text: "", type: "" });
  const [saving, setSaving] = useState(false);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    if (!orderId || !minutes) { setMsg({ text: "Select a job and enter minutes.", type: "error" }); return; }
    try {
      setSaving(true); setMsg({ text: "", type: "" });
      const qs = new URLSearchParams({ technicianId: String(me.userId), minutes, ...(note ? { note } : {}) });
      await api(`/work-orders/${orderId}/time-logs?${qs}`, token, { method: "POST" });
      setMsg({ text: "Time logged successfully!", type: "success" });
      setMinutes(""); setNote("");
    } catch (e: unknown) {
      setMsg({ text: e instanceof Error ? e.message : "Failed to log time.", type: "error" });
    } finally { setSaving(false); }
  }

  return (
    <div style={{ maxWidth: 480 }}>
      <h2 className="page-title">Log Time</h2>
      {msg.text && <div className={`alert alert-${msg.type === "error" ? "error" : "success"} mb-16`}>{msg.text}</div>}
      <div className="card">
        <form onSubmit={submit}>
          <div className="form-group">
            <label>Work Order</label>
            <select value={orderId} onChange={e => setOrderId(e.target.value)} required>
              <option value="">Select a job…</option>
              {orders.map(o => <option key={o.id} value={o.id}>{o.code} — {o.title}</option>)}
            </select>
          </div>
          <div className="form-group">
            <label>Minutes Worked</label>
            <input type="number" min="1" value={minutes} onChange={e => setMinutes(e.target.value)} placeholder="e.g. 90" required />
          </div>
          <div className="form-group">
            <label>Note (optional)</label>
            <textarea value={note} onChange={e => setNote(e.target.value)} placeholder="What did you do?" />
          </div>
          <button type="submit" className="btn btn-primary" disabled={saving}>
            {saving ? "Saving…" : "Log Time"}
          </button>
        </form>
      </div>
    </div>
  );
}

function TechPartsLog({ orders, parts, token, me: _me }: { orders: WorkOrder[]; parts: Part[]; token: string; me: LoginResult }) {
  const [orderId, setOrderId] = useState("");
  const [partId, setPartId] = useState("");
  const [quantity, setQuantity] = useState("");
  const [msg, setMsg] = useState({ text: "", type: "" });
  const [saving, setSaving] = useState(false);

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    if (!orderId || !partId || !quantity) { setMsg({ text: "All fields are required.", type: "error" }); return; }
    try {
      setSaving(true); setMsg({ text: "", type: "" });
      const qs = new URLSearchParams({ partId, quantity });
      await api(`/work-orders/${orderId}/parts?${qs}`, token, { method: "POST" });
      setMsg({ text: "Part logged successfully!", type: "success" });
      setQuantity("");
    } catch (e: unknown) {
      setMsg({ text: e instanceof Error ? e.message : "Failed to log part.", type: "error" });
    } finally { setSaving(false); }
  }

  return (
    <div style={{ maxWidth: 480 }}>
      <h2 className="page-title">Log Parts Used</h2>
      {msg.text && <div className={`alert alert-${msg.type === "error" ? "error" : "success"} mb-16`}>{msg.text}</div>}
      <div className="card">
        <form onSubmit={submit}>
          <div className="form-group">
            <label>Work Order</label>
            <select value={orderId} onChange={e => setOrderId(e.target.value)} required>
              <option value="">Select a job…</option>
              {orders.map(o => <option key={o.id} value={o.id}>{o.code} — {o.title}</option>)}
            </select>
          </div>
          <div className="form-group">
            <label>Part</label>
            <select value={partId} onChange={e => setPartId(e.target.value)} required>
              <option value="">Select a part…</option>
              {parts.map(p => <option key={p.id} value={p.id}>{p.name} (stock: {p.stockQuantity})</option>)}
            </select>
          </div>
          <div className="form-group">
            <label>Quantity</label>
            <input type="number" min="1" value={quantity} onChange={e => setQuantity(e.target.value)} placeholder="e.g. 2" required />
          </div>
          <button type="submit" className="btn btn-primary" disabled={saving}>
            {saving ? "Saving…" : "Log Part"}
          </button>
        </form>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────
// CUSTOMER portal
// ─────────────────────────────────────────────

function CustomerPortal({ token, me: _me, onUnauth }: { token: string; me: LoginResult; onUnauth: () => void }) {
  const [orders, setOrders] = useState<WorkOrder[]>([]);
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");

  useEffect(() => {
    Promise.all([
      api<Page<WorkOrder>>("/work-orders?page=0&size=100", token),
      api<Notification[]>("/notifications", token),
    ])
      .then(([p, n]) => { setOrders(p.content || []); setNotifications(n); })
      .catch((e: unknown) => {
        const msg = e instanceof Error ? e.message : String(e);
        if (msg.includes("401") || msg.includes("403")) onUnauth();
        else setErr("Failed to load your data.");
      })
      .finally(() => setLoading(false));
  }, [token, onUnauth]);

  const active = orders.filter(o => o.status !== "CLOSED" && o.status !== "CANCELLED");
  const closed = orders.filter(o => o.status === "CLOSED" || o.status === "COMPLETED");
  const unread = notifications.filter(n => !n.read);

  return (
    <main>
      <h2 className="page-title">Customer Portal</h2>
      {err && <div className="alert alert-error mb-16">{err}</div>}
      {loading
        ? <div className="empty-state">Loading your work orders…</div>
        : (
          <>
            {/* Summary */}
            <div className="stat-grid" style={{ marginBottom: 24 }}>
              <div className="card stat-card"><h3>Total Orders</h3><div className="stat-value">{orders.length}</div></div>
              <div className="card stat-card"><h3>Active</h3><div className="stat-value">{active.length}</div></div>
              <div className="card stat-card"><h3>Completed</h3><div className="stat-value success">{closed.length}</div></div>
              <div className="card stat-card"><h3>Notifications</h3><div className={`stat-value ${unread.length > 0 ? "warn" : ""}`}>{unread.length}</div></div>
            </div>

            {/* Notifications */}
            {unread.length > 0 && (
              <div className="alert alert-info mb-16">
                <strong>Notifications:</strong>
                {unread.slice(0, 3).map(n => <div key={n.id} className="text-sm mt-8">• {n.message}</div>)}
              </div>
            )}

            {/* Work Orders */}
            <h3 className="section-title">Your Work Orders</h3>
            {orders.length === 0
              ? <div className="empty-state">No work orders yet. Contact your service provider to raise a request.</div>
              : (
                <div className="grid-2">
                  {orders.map(o => (
                    <div className="card" key={o.id}>
                      <div className="flex justify-between items-center mb-8">
                        <span className="board-card-code">{o.code}</span>
                        <PriorityBadge p={o.priority} />
                      </div>
                      <h3 style={{ marginBottom: 8 }}>{o.title}</h3>
                      {o.description && <p className="text-sm text-muted mb-8">{o.description}</p>}
                      <div className="flex gap-8 items-center mb-8 flex-wrap">
                        <StatusBadge s={o.status} />
                        <span className={`text-sm ${slaClass(o.slaDueDate, o.status)}`}>⏱ {fmt(o.slaDueDate)}</span>
                      </div>
                      <div className="text-sm text-muted">
                        <div>📍 Site: {o.siteName || "—"}</div>
                        <div>👤 Technician: {o.assigneeEmail || "Not yet assigned"}</div>
                      </div>
                    </div>
                  ))}
                </div>
              )}
          </>
        )}
    </main>
  );
}
