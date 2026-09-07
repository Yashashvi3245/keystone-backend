import { useEffect, useState } from "react";
import "./App.css";

type Status =
  | "NEW"
  | "ASSIGNED"
  | "IN_PROGRESS"
  | "ON_HOLD"
  | "COMPLETED"
  | "CLOSED"
  | "CANCELLED";

type Priority =
  | "LOW"
  | "MEDIUM"
  | "HIGH"
  | "CRITICAL";

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

interface WorkOrderPage {
  content: WorkOrder[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

interface DashboardResponse {
  totalWorkOrders: number;
  overdueWorkOrders: number;
  statusCounts: Record<string, number>;
  priorityCounts: Record<string, number>;
}

const API_BASE_URL = "http://localhost:8080/api";

const LOGIN_URL = `${API_BASE_URL}/auth/login`;
const WORK_ORDERS_URL = `${API_BASE_URL}/work-orders`;
const USERS_URL = `${API_BASE_URL}/users`;
const DASHBOARD_URL = `${API_BASE_URL}/dashboard`;

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

  const [email, setEmail] = useState(
    "testuser@example.com"
  );

  const [password, setPassword] = useState("");

  const [loginLoading, setLoginLoading] =
    useState(false);

  const [loginError, setLoginError] =
    useState("");

  // =========================
  // WORK ORDERS
  // =========================

  const [workOrders, setWorkOrders] =
    useState<WorkOrder[]>([]);

  const [loading, setLoading] =
    useState(false);

  const [error, setError] =
    useState("");

  const [currentPage, setCurrentPage] =
    useState(0);

  const [totalPages, setTotalPages] =
    useState(0);

  // =========================
  // TECHNICIANS
  // =========================

  const [technicians, setTechnicians] =
    useState<User[]>([]);

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
  // DASHBOARD
  // =========================

  const [dashboard, setDashboard] =
    useState<DashboardResponse | null>(null);

  const [dashboardLoading, setDashboardLoading] =
    useState(false);

  const [dashboardVisible, setDashboardVisible] =
    useState(false);

  // =========================
  // INITIAL LOAD
  // =========================

  useEffect(() => {
    if (token) {
      loadWorkOrders(token, 0);
      loadTechnicians(token);
    }
  }, [token]);

  // =========================
  // LOAD WORK ORDERS
  // =========================

  async function loadWorkOrders(
    authToken: string = token || "",
    page: number = currentPage
  ) {
    if (!authToken) {
      return;
    }

    try {
      setLoading(true);
      setError("");

      const response = await fetch(
        `${WORK_ORDERS_URL}?page=${page}&size=100`,
        {
          method: "GET",
          headers: {
            Authorization: `Bearer ${authToken}`,
            "Content-Type": "application/json",
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
          `Failed to load work orders (${response.status})`
        );
      }

      const data: WorkOrderPage =
        await response.json();

      setWorkOrders(data.content || []);
      setCurrentPage(data.number || 0);
      setTotalPages(data.totalPages || 0);
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

      const response = await fetch(
        USERS_URL,
        {
          method: "GET",
          headers: {
            Authorization: `Bearer ${authToken}`,
            "Content-Type": "application/json",
          },
        }
      );

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

      const users: User[] =
        await response.json();

      const technicianUsers =
        users.filter(
          (user) =>
            user.role?.toUpperCase() ===
            "TECHNICIAN"
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
  // LOAD DASHBOARD
  // =========================

  async function loadDashboard() {
    const authToken =
      localStorage.getItem("token");

    if (!authToken) {
      setToken(null);
      return;
    }

    try {
      setDashboardLoading(true);
      setError("");

      const response = await fetch(
        DASHBOARD_URL,
        {
          method: "GET",
          headers: {
            Authorization: `Bearer ${authToken}`,
            "Content-Type": "application/json",
          },
        }
      );

      if (
        response.status === 401 ||
        response.status === 403
      ) {
        setError(
          "Dashboard sirf Manager aur Dispatcher ke liye available hai."
        );
        return;
      }

      if (!response.ok) {
        throw new Error(
          `Dashboard failed (${response.status})`
        );
      }

      const data: DashboardResponse =
        await response.json();

      setDashboard(data);
      setDashboardVisible(true);
    } catch (err) {
      console.error(err);

      setError(
        "Dashboard load nahi ho raha. Backend check karo."
      );
    } finally {
      setDashboardLoading(false);
    }
  }

  // =========================
  // LOGIN
  // =========================

  async function login() {
    try {
      setLoginLoading(true);
      setLoginError("");

      const response = await fetch(
        LOGIN_URL,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            email,
            password,
          }),
        }
      );

      if (!response.ok) {
        throw new Error(
          `Login failed (${response.status})`
        );
      }

      const data: LoginResponse =
        await response.json();

      if (!data.token) {
        throw new Error(
          "Token not received"
        );
      }

      localStorage.setItem(
        "token",
        data.token
      );

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
    setDashboard(null);
    setDashboardVisible(false);
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
    const authToken =
      localStorage.getItem("token");

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

      setWorkOrders(
        (currentOrders) =>
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
    const authToken =
      localStorage.getItem("token");

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

      setWorkOrders(
        (currentOrders) =>
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
  // PRIORITY CLASS
  // =========================

  function getPriorityClass(
    priority: Priority
  ) {
    return `priority ${priority.toLowerCase()}`;
  }

  // =========================
  // DATE
  // =========================

  function formatDate(
    date?: string
  ) {
    if (!date) {
      return "No SLA";
    }

    const parsedDate =
      new Date(date);

    if (
      Number.isNaN(
        parsedDate.getTime()
      )
    ) {
      return date;
    }

    return parsedDate.toLocaleDateString(
      "en-IN",
      {
        day: "2-digit",
        month: "short",
        year: "numeric",
      }
    );
  }

  // =========================
  // LOGIN SCREEN
  // =========================

  if (!token) {
    return (
      <div className="app">

        <div className="login-page">

          <div className="login-card">

            <h1>
              Keystone
            </h1>

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
                    setEmail(
                      event.target.value
                    )
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
                    setPassword(
                      event.target.value
                    )
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

  // =========================
  // MAIN APPLICATION
  // =========================

  return (
    <div className="app">

      <header className="header">

        <div>

          <h1>
            Keystone
          </h1>

          <p>
            Work Order Management System
          </p>

        </div>

        <div className="header-actions">

          <button
            className="refresh-btn"
            onClick={() => {
              loadWorkOrders(
                undefined,
                currentPage
              );

              loadTechnicians();
            }}
            disabled={loading}
          >
            {loading
              ? "Loading..."
              : "Refresh"}
          </button>

          <button
            className="refresh-btn"
            onClick={() => {
              if (dashboardVisible) {
                setDashboardVisible(false);
              } else {
                loadDashboard();
              }
            }}
            disabled={dashboardLoading}
          >
            {dashboardLoading
              ? "Loading..."
              : dashboardVisible
                ? "Work Orders"
                : "Dashboard"}
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

      {/* =========================
          DASHBOARD
      ========================= */}

      {dashboardVisible &&
      dashboard ? (

        <main
          style={{
            padding: "24px",
          }}
        >

          <h2>
            Dashboard
          </h2>

          <p>
            Work order overview and SLA status
          </p>

          {/* SUMMARY CARDS */}

          <div
            style={{
              display: "grid",
              gridTemplateColumns:
                "repeat(auto-fit, minmax(200px, 1fr))",
              gap: "16px",
              marginTop: "24px",
              marginBottom: "32px",
            }}
          >

            <div className="card">

              <h3>
                Total Work Orders
              </h3>

              <strong
                style={{
                  fontSize: "32px",
                }}
              >
                {dashboard.totalWorkOrders}
              </strong>

            </div>

            <div className="card">

              <h3>
                SLA Breached
              </h3>

              <strong
                style={{
                  fontSize: "32px",
                }}
              >
                {dashboard.overdueWorkOrders}
              </strong>

            </div>

          </div>

          {/* STATUS BREAKDOWN */}

          <section
            className="card"
            style={{
              marginBottom: "24px",
            }}
          >

            <h2>
              Status Breakdown
            </h2>

            <div
              style={{
                display: "grid",
                gridTemplateColumns:
                  "repeat(auto-fit, minmax(150px, 1fr))",
                gap: "12px",
                marginTop: "16px",
              }}
            >

              {Object.entries(
                dashboard.statusCounts
              ).map(
                ([status, count]) => (

                  <div
                    key={status}
                    style={{
                      padding: "16px",
                      border: "1px solid #ddd",
                      borderRadius: "8px",
                    }}
                  >

                    <strong>
                      {status.replace(
                        "_",
                        " "
                      )}
                    </strong>

                    <div
                      style={{
                        fontSize: "24px",
                        marginTop: "8px",
                      }}
                    >
                      {count}
                    </div>

                  </div>

                )
              )}

            </div>

          </section>

          {/* PRIORITY BREAKDOWN */}

          <section className="card">

            <h2>
              Priority Breakdown
            </h2>

            <div
              style={{
                display: "grid",
                gridTemplateColumns:
                  "repeat(auto-fit, minmax(150px, 1fr))",
                gap: "12px",
                marginTop: "16px",
              }}
            >

              {Object.entries(
                dashboard.priorityCounts
              ).map(
                ([priority, count]) => (

                  <div
                    key={priority}
                    style={{
                      padding: "16px",
                      border: "1px solid #ddd",
                      borderRadius: "8px",
                    }}
                  >

                    <strong>
                      {priority}
                    </strong>

                    <div
                      style={{
                        fontSize: "24px",
                        marginTop: "8px",
                      }}
                    >
                      {count}
                    </div>

                  </div>

                )
              )}

            </div>

          </section>

        </main>

      ) : (

        /* =========================
           WORK ORDER BOARD
        ========================= */

        <>

          {loading &&
          workOrders.length === 0 && (
            <div className="message">
              Loading work orders...
            </div>
          )}

          <main className="board">

            {columns.map(
              (column) => {

                const orders =
                  workOrders.filter(
                    (workOrder) =>
                      workOrder.status ===
                      column.key
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

                        orders.map(
                          (workOrder) => (

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
                                  {
                                    workOrder.description
                                  }
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

                              {/* STATUS */}

                              <div className="status-control">

                                <label
                                  htmlFor={`status-${workOrder.id}`}
                                >
                                  Status
                                </label>

                                <select
                                  id={`status-${workOrder.id}`}
                                  value={
                                    workOrder.status
                                  }
                                  disabled={
                                    updatingStatusId ===
                                    workOrder.id
                                  }
                                  onChange={(
                                    event
                                  ) =>
                                    updateStatus(
                                      workOrder.id,
                                      event.target
                                        .value as Status
                                    )
                                  }
                                >

                                  {statuses.map(
                                    (status) => (

                                      <option
                                        key={status}
                                        value={status}
                                      >
                                        {status.replace(
                                          "_",
                                          " "
                                        )}
                                      </option>

                                    )
                                  )}

                                </select>

                                {updatingStatusId ===
                                  workOrder.id && (
                                  <small>
                                    Updating...
                                  </small>
                                )}

                              </div>

                              {/* ASSIGN TECHNICIAN */}

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
                                  onChange={(
                                    event
                                  ) => {

                                    const selectedId =
                                      Number(
                                        event.target
                                          .value
                                      );

                                    if (
                                      selectedId >
                                      0
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
                                        key={
                                          technician.id
                                        }
                                        value={
                                          technician.id
                                        }
                                      >
                                        {
                                          technician.name
                                        }{" "}
                                        (
                                        {
                                          technician.email
                                        }
                                        )
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

                          )
                        )

                      )}

                    </div>

                  </section>
                );
              }
            )}

          </main>

          {/* PAGINATION */}

          {totalPages > 1 && (

            <div
              style={{
                display: "flex",
                justifyContent: "center",
                gap: "12px",
                padding: "20px",
              }}
            >

              <button
                className="refresh-btn"
                disabled={currentPage === 0}
                onClick={() =>
                  loadWorkOrders(
                    undefined,
                    currentPage - 1
                  )
                }
              >
                Previous
              </button>

              <span
                style={{
                  padding: "10px",
                }}
              >
                Page {currentPage + 1} of{" "}
                {totalPages}
              </span>

              <button
                className="refresh-btn"
                disabled={
                  currentPage >=
                  totalPages - 1
                }
                onClick={() =>
                  loadWorkOrders(
                    undefined,
                    currentPage + 1
                  )
                }
              >
                Next
              </button>

            </div>

          )}

        </>

      )}

    </div>
  );
}

export default App;