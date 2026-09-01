import { useEffect, useState } from "react";
import "./App.css";

type Status =
  | "NEW"
  | "ASSIGNED"
  | "IN_PROGRESS"
  | "ON_HOLD"
  | "COMPLETED";

type Priority = "LOW" | "MEDIUM" | "HIGH" | "URGENT";

interface WorkOrder {
  id: number;
  code: string;
  title: string;
  description?: string;
  priority: Priority;
  status: Status;
  slaDueDate?: string;
  customerName?: string;
  siteName?: string;
  assigneeEmail?: string;
  assigneeId?: number;
}

interface User {
  id: number;
  name: string;
  email: string;
  role: string;
}

interface LoginResponse {
  token: string;
}

const API_BASE_URL = "http://localhost:8080/api";

const LOGIN_URL = `${API_BASE_URL}/auth/login`;
const WORK_ORDERS_URL = `${API_BASE_URL}/work-orders`;
const USERS_URL = `${API_BASE_URL}/users`;

const columns: { key: Status; label: string }[] = [
  { key: "NEW", label: "New" },
  { key: "ASSIGNED", label: "Assigned" },
  { key: "IN_PROGRESS", label: "In Progress" },
  { key: "ON_HOLD", label: "On Hold" },
  { key: "COMPLETED", label: "Completed" },
];

const statuses: Status[] = [
  "NEW",
  "ASSIGNED",
  "IN_PROGRESS",
  "ON_HOLD",
  "COMPLETED",
];

function App() {
  // =========================
  // AUTH
  // =========================

  const [token, setToken] = useState<string | null>(() =>
    localStorage.getItem("token")
  );

  const [email, setEmail] = useState("testuser@example.com");
  const [password, setPassword] = useState("");

  const [loginLoading, setLoginLoading] = useState(false);
  const [loginError, setLoginError] = useState("");

  // =========================
  // WORK ORDERS
  // =========================

  const [workOrders, setWorkOrders] = useState<WorkOrder[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  // =========================
  // TECHNICIANS
  // =========================

  const [technicians, setTechnicians] = useState<User[]>([]);
  const [techniciansLoading, setTechniciansLoading] =
    useState(false);

  // =========================
  // UPDATE STATES
  // =========================

  const [updatingStatusId, setUpdatingStatusId] =
    useState<number | null>(null);

  const [assigningId, setAssigningId] =
    useState<number | null>(null);

  // =========================
  // LOAD WORK ORDERS
  // =========================

  useEffect(() => {
    if (token) {
      loadWorkOrders(token);
      loadTechnicians(token);
    }
  }, [token]);

  async function loadWorkOrders(
    authToken: string = token || ""
  ) {
    if (!authToken) {
      return;
    }

    try {
      setLoading(true);
      setError("");

      const response = await fetch(WORK_ORDERS_URL, {
        method: "GET",
        headers: {
          Authorization: `Bearer ${authToken}`,
          "Content-Type": "application/json",
        },
      });

      if (
        response.status === 401 ||
        response.status === 403
      ) {
        localStorage.removeItem("token");
        setToken(null);
        setWorkOrders([]);
        return;
      }

      if (!response.ok) {
        throw new Error(
          `Failed to load work orders (${response.status})`
        );
      }

      const data: WorkOrder[] = await response.json();

      setWorkOrders(data);
    } catch (err) {
      console.error(err);

      setError(
        "Work orders load nahi ho rahe. Backend check karo."
      );
    } finally {
      setLoading(false);
    }
  }

  // =========================
  // LOAD TECHNICIANS
  // =========================

  async function loadTechnicians(
    authToken: string = token || ""
  ) {
    if (!authToken) {
      return;
    }

    try {
      setTechniciansLoading(true);

      const response = await fetch(USERS_URL, {
        method: "GET",
        headers: {
          Authorization: `Bearer ${authToken}`,
          "Content-Type": "application/json",
        },
      });

      if (
        response.status === 401 ||
        response.status === 403
      ) {
        localStorage.removeItem("token");
        setToken(null);
        setTechnicians([]);
        return;
      }

      if (!response.ok) {
        throw new Error(
          `Failed to load users (${response.status})`
        );
      }

      const users: User[] = await response.json();

      const technicianUsers = users.filter(
        (user) =>
          user.role?.toUpperCase() === "TECHNICIAN"
      );

      setTechnicians(technicianUsers);
    } catch (err) {
      console.error(err);

      setError(
        "Technicians load nahi ho rahe. Users API check karo."
      );
    } finally {
      setTechniciansLoading(false);
    }
  }

  // =========================
  // LOGIN
  // =========================

  async function login() {
    try {
      setLoginLoading(true);
      setLoginError("");

      const response = await fetch(LOGIN_URL, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          email,
          password,
        }),
      });

      if (!response.ok) {
        throw new Error(
          `Login failed (${response.status})`
        );
      }

      const data: LoginResponse = await response.json();

      if (!data.token) {
        throw new Error("Token not received");
      }

      localStorage.setItem("token", data.token);

      setToken(data.token);
      setPassword("");
    } catch (err) {
      console.error(err);

      setLoginError(
        "Login failed. Email/password check karo."
      );
    } finally {
      setLoginLoading(false);
    }
  }

  // =========================
  // LOGOUT
  // =========================

  function logout() {
    localStorage.removeItem("token");

    setToken(null);
    setWorkOrders([]);
    setTechnicians([]);
    setError("");
    setLoginError("");
    setPassword("");
  }

  // =========================
  // UPDATE STATUS
  // =========================

  async function updateStatus(
    workOrderId: number,
    newStatus: Status
  ) {
    const authToken = localStorage.getItem("token");

    if (!authToken) {
      setToken(null);
      return;
    }

    try {
      setUpdatingStatusId(workOrderId);
      setError("");

      const response = await fetch(
        `${WORK_ORDERS_URL}/${workOrderId}/status?status=${encodeURIComponent(
          newStatus
        )}`,
        {
          method: "PATCH",
          headers: {
            Authorization: `Bearer ${authToken}`,
          },
        }
      );

      if (
        response.status === 401 ||
        response.status === 403
      ) {
        localStorage.removeItem("token");
        setToken(null);
        setWorkOrders([]);
        return;
      }

      if (!response.ok) {
        throw new Error(
          `Status update failed (${response.status})`
        );
      }

      const updatedWorkOrder: WorkOrder =
        await response.json();

      setWorkOrders((currentOrders) =>
        currentOrders.map((order) =>
          order.id === updatedWorkOrder.id
            ? updatedWorkOrder
            : order
        )
      );
    } catch (err) {
      console.error(err);

      setError(
        "Status update nahi hua. Backend check karo."
      );
    } finally {
      setUpdatingStatusId(null);
    }
  }

  // =========================
  // ASSIGN TECHNICIAN
  // =========================

  async function assignTechnician(
    workOrderId: number,
    technicianId: number
  ) {
    const authToken = localStorage.getItem("token");

    if (!authToken) {
      setToken(null);
      return;
    }

    try {
      setAssigningId(workOrderId);
      setError("");

      const response = await fetch(
        `${WORK_ORDERS_URL}/${workOrderId}/assign?assigneeId=${technicianId}`,
        {
          method: "PATCH",
          headers: {
            Authorization: `Bearer ${authToken}`,
          },
        }
      );

      if (
        response.status === 401 ||
        response.status === 403
      ) {
        localStorage.removeItem("token");
        setToken(null);
        setWorkOrders([]);
        return;
      }

      if (!response.ok) {
        throw new Error(
          `Assignment failed (${response.status})`
        );
      }

      const updatedWorkOrder: WorkOrder =
        await response.json();

      setWorkOrders((currentOrders) =>
        currentOrders.map((order) =>
          order.id === updatedWorkOrder.id
            ? updatedWorkOrder
            : order
        )
      );
    } catch (err) {
      console.error(err);

      setError(
        "Technician assign nahi hua. Backend check karo."
      );
    } finally {
      setAssigningId(null);
    }
  }

  // =========================
  // PRIORITY
  // =========================

  function getPriorityClass(priority: Priority) {
    return `priority ${priority.toLowerCase()}`;
  }

  // =========================
  // DATE
  // =========================

  function formatDate(date?: string) {
    if (!date) {
      return "No SLA";
    }

    const parsedDate = new Date(date);

    if (Number.isNaN(parsedDate.getTime())) {
      return date;
    }

    return parsedDate.toLocaleDateString("en-IN", {
      day: "2-digit",
      month: "short",
      year: "numeric",
    });
  }

  // ==================================================
  // LOGIN SCREEN
  // ==================================================

  if (!token) {
    return (
      <div className="app">
        <div className="login-page">
          <div className="login-card">
            <h1>Keystone</h1>

            <h2>
              Login to Work Order Board
            </h2>

            <form
              onSubmit={(event) => {
                event.preventDefault();
                login();
              }}
            >
              <div className="form-group">
                <label htmlFor="email">
                  Email
                </label>

                <input
                  id="email"
                  type="email"
                  value={email}
                  onChange={(event) =>
                    setEmail(event.target.value)
                  }
                  placeholder="Enter email"
                  required
                />
              </div>

              <div className="form-group">
                <label htmlFor="password">
                  Password
                </label>

                <input
                  id="password"
                  type="password"
                  value={password}
                  onChange={(event) =>
                    setPassword(event.target.value)
                  }
                  placeholder="Enter password"
                  required
                />
              </div>

              {loginError && (
                <div className="error">
                  {loginError}
                </div>
              )}

              <button
                type="submit"
                className="login-btn"
                disabled={loginLoading}
              >
                {loginLoading
                  ? "Logging in..."
                  : "Login"}
              </button>
            </form>
          </div>
        </div>
      </div>
    );
  }

  // ==================================================
  // WORK ORDER BOARD
  // ==================================================

  return (
    <div className="app">

      <header className="header">
        <div>
          <h1>
            Work Order Board
          </h1>

          <p>
            Manage and track work orders by status
          </p>
        </div>

        <div className="header-actions">

          <button
            className="refresh-btn"
            onClick={() => {
              loadWorkOrders();
              loadTechnicians();
            }}
            disabled={loading}
          >
            {loading
              ? "Loading..."
              : "Refresh"}
          </button>

          <button
            className="logout-btn"
            onClick={logout}
          >
            Logout
          </button>

        </div>
      </header>

      {error && (
        <div className="error">
          {error}
        </div>
      )}

      {loading && workOrders.length === 0 && (
        <div className="message">
          Loading work orders...
        </div>
      )}

      <main className="board">

        {columns.map((column) => {

          const orders = workOrders.filter(
            (workOrder) =>
              workOrder.status === column.key
          );

          return (
            <section
              className="column"
              key={column.key}
            >

              <div className="column-header">
                <h2>
                  {column.label}
                </h2>

                <span>
                  {orders.length}
                </span>
              </div>

              <div className="cards">

                {orders.length === 0 ? (

                  <div className="empty">
                    No work orders
                  </div>

                ) : (

                  orders.map((workOrder) => (

                    <article
                      className="card"
                      key={workOrder.id}
                    >

                      <div className="card-top">

                        <strong>
                          {workOrder.code}
                        </strong>

                        <span
                          className={getPriorityClass(
                            workOrder.priority
                          )}
                        >
                          {workOrder.priority}
                        </span>

                      </div>

                      <h3>
                        {workOrder.title}
                      </h3>

                      {workOrder.description && (
                        <p className="description">
                          {workOrder.description}
                        </p>
                      )}

                      <div className="details">

                        <div>
                          <span>
                            Customer
                          </span>

                          <strong>
                            {workOrder.customerName ||
                              "—"}
                          </strong>
                        </div>

                        <div>
                          <span>
                            Site
                          </span>

                          <strong>
                            {workOrder.siteName ||
                              "—"}
                          </strong>
                        </div>

                        <div>
                          <span>
                            Assignee
                          </span>

                          <strong>
                            {workOrder.assigneeEmail ||
                              "Unassigned"}
                          </strong>
                        </div>

                        <div>
                          <span>
                            SLA Due
                          </span>

                          <strong>
                            {formatDate(
                              workOrder.slaDueDate
                            )}
                          </strong>
                        </div>

                      </div>

                      {/* =========================
                          STATUS
                      ========================= */}

                      <div className="status-control">

                        <label
                          htmlFor={`status-${workOrder.id}`}
                        >
                          Status
                        </label>

                        <select
                          id={`status-${workOrder.id}`}
                          value={workOrder.status}
                          disabled={
                            updatingStatusId ===
                            workOrder.id
                          }
                          onChange={(event) =>
                            updateStatus(
                              workOrder.id,
                              event.target
                                .value as Status
                            )
                          }
                        >
                          {statuses.map((status) => (
                            <option
                              key={status}
                              value={status}
                            >
                              {status.replace(
                                "_",
                                " "
                              )}
                            </option>
                          ))}
                        </select>

                        {updatingStatusId ===
                          workOrder.id && (
                          <small>
                            Updating...
                          </small>
                        )}

                      </div>

                      {/* =========================
                          ASSIGN TECHNICIAN
                      ========================= */}

                      <div className="assign-control">

                        <label
                          htmlFor={`technician-${workOrder.id}`}
                        >
                          Assign Technician
                        </label>

                        <select
                          id={`technician-${workOrder.id}`}
                          value={
                            workOrder.assigneeId ||
                            ""
                          }
                          disabled={
                            assigningId ===
                              workOrder.id ||
                            techniciansLoading
                          }
                          onChange={(event) => {

                            const selectedId =
                              Number(
                                event.target.value
                              );

                            if (
                              selectedId > 0
                            ) {
                              assignTechnician(
                                workOrder.id,
                                selectedId
                              );
                            }

                          }}
                        >

                          <option value="">
                            {techniciansLoading
                              ? "Loading technicians..."
                              : "Select technician"}
                          </option>

                          {technicians.map(
                            (technician) => (
                              <option
                                key={technician.id}
                                value={technician.id}
                              >
                                {technician.name} (
                                {technician.email})
                              </option>
                            )
                          )}

                        </select>

                        {assigningId ===
                          workOrder.id && (
                          <small>
                            Assigning...
                          </small>
                        )}

                      </div>

                    </article>

                  ))

                )}

              </div>

            </section>
          );
        })}

      </main>
    </div>
  );
}

export default App;

