# Car Rental System

> **Student Assessment Prototype** — Software Engineering Project  
> Built with Java 17, Spring Boot 3.2, MySQL, Thymeleaf

---

## Problem Statement

Manual vehicle rental processes are prone to errors, double bookings, and lack of transparency. This web-based Car Rental System automates booking, availability tracking, status lifecycle management, and audit logging.

---

## Objective

Build a simple but complete web application where:

- **Users** can search vehicles, make reservations, view their bookings, and cancel eligible ones.
- **Admins** can view all bookings and progress them through a strict sequential status lifecycle.

---

## Features

| Feature | Description |
|---------|-------------|
| Role-based access | USER and ADMIN roles |
| Vehicle browsing & search | List vehicles by name/category |
| Availability enforcement | BR1-BR2: quantity & status checks |
| Rate snapshot | BR3: rate locked at booking time |
| Fare calculation | BR4: base amount, booking charge, grand total |
| Input validation | BR5: name, 10-digit phone, date, identity ref |
| Atomic transaction | BR6-BR7: all-or-nothing booking creation |
| Status lifecycle | BR8: CONFIRMED → CHECKED_IN → COMPLETED |
| Cancellation | BR9: only CONFIRMED; availability restored once |
| Access control | BR10: users see own bookings only |
| Status audit trail | Full history in BookingStatusHistory |

---

## User Roles

| Role | Actions |
|------|---------|
| USER | Search vehicles, book, view own bookings, cancel |
| ADMIN | View all bookings, update status sequentially |

---

## Technology Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 17 |
| Framework | Spring Boot 3.2, Spring MVC, Spring Data JPA |
| ORM | Hibernate |
| Database | MySQL 8.0 |
| Templating | Thymeleaf |
| Frontend | HTML, CSS, JavaScript |
| Build | Maven |

---

## Project Structure

```
car-rental-system/
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/example/carrental/
    │   │   ├── CarRentalApplication.java       # Main entry point
    │   │   ├── DataInitializer.java             # Seeds demo users and vehicles
    │   │   ├── controller/
    │   │   │   ├── AuthController.java          # Login / Logout
    │   │   │   ├── DashboardController.java     # User dashboard
    │   │   │   ├── VehicleController.java       # Vehicle listing + search
    │   │   │   ├── BookingController.java       # Booking CRUD + cancel
    │   │   │   └── AdminController.java         # Admin status management
    │   │   ├── model/
    │   │   │   ├── User.java
    │   │   │   ├── Resource.java                # Vehicle entity
    │   │   │   ├── Reservation.java
    │   │   │   ├── ReservationItem.java          # Rate snapshot line item
    │   │   │   ├── BookingStatusHistory.java
    │   │   │   └── Schedule.java
    │   │   ├── repository/
    │   │   │   ├── UserRepository.java
    │   │   │   ├── ResourceRepository.java
    │   │   │   ├── ReservationRepository.java
    │   │   │   ├── ReservationItemRepository.java
    │   │   │   ├── BookingStatusHistoryRepository.java
    │   │   │   └── ScheduleRepository.java
    │   │   ├── service/
    │   │   │   ├── AvailabilityService.java     # BR1, BR2, availability I/O
    │   │   │   └── BookingService.java          # BR3-BR9, transactions
    │   │   └── exception/
    │   │       ├── CarRentalException.java      # Base
    │   │       ├── BookingException.java
    │   │       ├── InvalidBookingException.java
    │   │       ├── ResourceNotFoundException.java
    │   │       ├── UnauthorizedException.java
    │   │       └── GlobalExceptionHandler.java
    │   └── resources/
    │       ├── application.properties
    │       ├── templates/
    │       │   ├── login.html
    │       │   ├── dashboard.html
    │       │   ├── vehicles.html
    │       │   ├── booking.html
    │       │   ├── bookings.html
    │       │   ├── admin.html
    │       │   └── error.html
    │       └── static/
    │           ├── css/style.css
    │           └── js/script.js
    └── test/
        ├── java/com/example/carrental/
        │   └── CarRentalSystemTests.java        # T1-T12 test scenarios
        └── resources/application.properties    # H2 in-memory for tests
```

---

## Database Design

**Database name:** `carrentaldb`

| Table | Key Fields |
|-------|-----------|
| `users` | id, name, username, password, role, status |
| `resources` | id (VARCHAR), name, category, rate, availability, status |
| `reservations` | id, reference, date, status, baseAmount, bookingCharge, totalAmount, customerName, customerPhone, identityReference, user_id |
| `reservation_items` | id, quantity, rate (snapshot), reservation_id, resource_id |
| `booking_status_history` | id, referenceId, actorId, oldStatus, newStatus, eventAt |
| `schedules` | id, resourceId, startDate, endDate |

---

## Business Rules Summary

| Rule | Description |
|------|-------------|
| BR1 | Quantity: 1 to 5 units per booking |
| BR2 | Vehicle must be ACTIVE with sufficient availability |
| BR3 | Rate snapshot saved at booking time |
| BR4 | Base = Rate × Qty; Fee = ₹50 if Base < ₹1000 else ₹0; Grand = Base + Fee |
| BR5 | Name not empty, phone 10 digits, date valid, identity ref required |
| BR6 | All validations must pass; no partial records created |
| BR7 | Booking creation is atomic: reservation + item + availability − history |
| BR8 | Admin: CONFIRMED → CHECKED_IN → COMPLETED (no skipping) |
| BR9 | Only CONFIRMED can be cancelled; availability restored exactly once |
| BR10 | User sees own bookings only; admin sees all |

---

## UML Diagrams

### Use Case Diagram

```
           ┌─────────────────────────────────────────────┐
           │               Car Rental System              │
           │                                              │
  USER ───►│  Search Vehicles                             │
       ───►│  Select Vehicle + Quantity                   │
       ───►│  Make Booking (Enter Details, Calculate Fare)│
       ───►│  View My Bookings                            │
       ───►│  Cancel Booking                              │
           │                                              │
  ADMIN───►│  View All Bookings                           │
       ───►│  Update Booking Status                       │
           └─────────────────────────────────────────────┘
```

### Class Diagram (simplified)

```
User 1──────* Reservation
Reservation 1──* ReservationItem
Reservation 1──* BookingStatusHistory
ReservationItem *──1 Resource
Resource 1──* Schedule

BookingService ──► ReservationRepository
BookingService ──► AvailabilityService
AvailabilityService ──► ResourceRepository
```

---

## How to Run

### Prerequisites
- Java 17+
- Maven 3.8+
- MySQL 8.0 (running on port 3306)

### Step 1: Configure MySQL Password

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/carrentaldb?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=YOUR_MYSQL_PASSWORD
```

### Step 2: Run the Application

```bash
mvn spring-boot:run
```

The database `carrentaldb` is created automatically. Sample data is seeded on first startup.

### Step 3: Open in Browser

```
http://localhost:8080
```

---

## Demo Credentials

| Role | Username | Password |
|------|----------|----------|
| User | user1 | user123 |
| User | user2 | user123 |
| Admin | admin | admin123 |

---

## Sample Vehicle Data

| ID | Name | Category | Rate | Availability |
|----|------|----------|------|--------------|
| R101 | Vehicle A | Sedan | ₹499 | 10 |
| R102 | Vehicle B | SUV | ₹799 | 5 |
| R103 | Vehicle C | Hatchback | ₹199 | 20 |
| R104 | Vehicle D | SUV | ₹1299 | 0 (unavailable) |
| R105 | Vehicle E | Sedan | ₹1000 | 3 |

---

## Required Test Scenarios (T1-T12)

| Test | Scenario | Expected |
|------|----------|----------|
| T1 | Book same resource incorrectly | Rejected |
| T2 | Quantity 0, 1, 5, 6, 1.5 | Only 1–5 integers accepted |
| T3 | Book unavailable vehicle (R104) | Rejected, state unchanged |
| T4 | Amount: ₹999 / ₹1000 / ₹1001 | ₹50 / ₹0 / ₹0 booking charge |
| T5 | Complete valid reservation | Created, availability reduced |
| T6 | Confirm with missing/invalid details | Rejected, no partial update |
| T7 | Availability changes before confirm | Rejected |
| T8 | Access another user's booking | Access denied |
| T9 | Admin skip CONFIRMED→COMPLETED | Rejected |
| T10 | Cancel confirmed booking twice | First works, second rejected |
| T11 | Restart app after booking | Booking persists |
| T12 | Change vehicle rate after booking | Old booking retains original rate |

---

## Functional Requirements

| ID | Requirement |
|----|-------------|
| FR1 | Display/search active vehicles |
| FR2 | Select vehicle and quantity (BR1, BR2) |
| FR3 | Display base amount, booking charge, grand total |
| FR4 | Validate details and availability before confirm |
| FR5 | Create confirmed booking atomically |
| FR6 | Display logged-in user's own bookings |
| FR7 | Admin moves booking to next valid status only |
| FR8 | Eligible cancellation restores availability exactly once |
| FR9 | Clear errors for invalid input / unauthorized access |
| FR10 | Persist all data in MySQL |

---

## Limitations

- No email notifications or SMS
- No payment gateway
- No JWT/OAuth authentication (session-based simulation)
- No real-time availability sync across concurrent users
- Single-item bookings per reservation

---

## Future Enhancements

- Spring Security with proper authentication
- Multi-vehicle booking (cart feature)
- Date-range availability calendar
- Email confirmation on booking
- PDF invoice generation
- Admin vehicle management CRUD

---

*This is a student assessment prototype built for educational purposes.*

