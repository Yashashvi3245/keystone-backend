# KEYSTONE

KEYSTONE is a Java full-stack field-service management platform built for managing customers, sites, work orders, technicians, parts, time logs, SLA tracking, notifications, dashboards, and customer self-service.

The platform supports four roles:

- MANAGER
- DISPATCHER
- TECHNICIAN
- CUSTOMER

---

## 1. Live Application

### Frontend

https://keystone-frontend-9wlj.onrender.com

### Backend API

https://keystone-backend-id49.onrender.com

### Swagger / OpenAPI

https://keystone-backend-id49.onrender.com/swagger-ui/index.html

### API Documentation

https://keystone-backend-id49.onrender.com/api-docs

---

## 2. Features

### Authentication & Authorization

- JWT-based authentication
- BCrypt password hashing
- Role-based access control
- Server-side authorization
- JWT token expiration
- Protected API endpoints

### Customer & Site Management

- Create and manage customers
- Create and manage customer sites
- Customer-to-site ownership
- Customer organization isolation
- Search and pagination support

### Work Orders

- Create and manage work orders
- Unique human-readable work-order codes
- Priority management
- Customer and site association
- Technician assignment
- Kanban board
- Search, filtering, and pagination
- SLA due dates
- Work-order status history

### Work-Order Lifecycle

Supported statuses:

- NEW
- ASSIGNED
- IN_PROGRESS
- ON_HOLD
- COMPLETED
- CLOSED
- CANCELLED

The lifecycle is enforced server-side.

Invalid state transitions are rejected.

Closed and cancelled work orders are immutable.

Every status change is recorded in the work-order history.

### Dispatch

Managers and dispatchers can:

- Assign technicians
- Reassign technicians while work is open
- View open work orders
- Monitor the work-order board

Technicians cannot assign or reassign work orders.

### Technician Field View

Technicians can:

- View assigned work orders
- Start work
- Put work on hold
- Resume work
- Complete work
- Log time
- Log parts

Technicians cannot access another technician's work orders.

The technician interface is responsive and designed for phone/browser use.

### Parts & Inventory

- Parts inventory management
- Parts usage against work orders
- Transactional stock decrement
- Negative stock prevention
- Parts cost tracking

### Time Logging

- Log time against work orders
- Record minutes
- Optional notes
- Calculate total labour time

### SLA

SLA due dates are calculated from work-order priority.

The system includes:

- SLA due-date calculation
- Scheduled SLA breach checking
- Overdue work-order detection
- Manager notifications
- SLA status in dashboard/work-order views

### Notifications

The application supports in-app notifications for important events including:

- Technician assignments
- SLA breaches

Users can:

- View notifications
- See unread notification count
- Refresh notifications
- Mark notifications as read

### Dashboard

Managers and dispatchers can view:

- Total work orders
- Completed work orders
- In-progress work orders
- Overdue work orders
- SLA compliance
- Status breakdown
- Priority breakdown
- Technician breakdown
- Site breakdown

### Customer Portal

Customers can:

- View their organization
- View their work orders
- Track work-order status
- View SLA information
- View relevant notifications

Customer data is isolated server-side so customers cannot access another organization's data by changing IDs in API requests.

---

# 3. Technology Stack

## Backend

- Java 21
- Spring Boot
- Spring Security
- Spring Data JPA
- PostgreSQL
- Flyway
- JWT
- BCrypt
- Bean Validation
- Maven
- OpenAPI / Swagger

## Frontend

- React
- TypeScript
- Vite
- CSS

## Database

- PostgreSQL
- Flyway versioned migrations

## Testing

- JUnit
- Spring Boot Test
- H2 for automated tests

---

# 4. Project Structure

```text
keystone-backend/
│
├── src/
│   ├── main/
│   │   ├── java/com/keystone/
│   │   │   ├── config/
│   │   │   ├── controller/
│   │   │   ├── dto/
│   │   │   ├── exception/
│   │   │   ├── model/
│   │   │   ├── repository/
│   │   │   ├── security/
│   │   │   └── service/
│   │   │
│   │   └── resources/
│   │       ├── db/migration/
│   │       ├── application.properties
│   │       └── application.properties.example
│   │
│   └── test/
│
├── frontend/
│   ├── src/
│   ├── public/
│   ├── package.json
│   ├── Dockerfile
│   └── nginx.conf
│
├── Dockerfile
├── docker-compose.yml
├── pom.xml
├── mvnw
├── mvnw.cmd
└── README.md