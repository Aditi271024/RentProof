# RentProof – Rent Reminder Management Module Documentation

> **Project:** RentProof – Landlord Rental & Property Management System  
> **Module Lead:** Gungun (BTech CSE)  
> **Target Audience:** College Project Viva Voce, Faculty Evaluation, & Team Integration (Aditi, Shweta, Aanvi, Gungun)  
> **Theme:** Midnight Blue (`#26364A`) & Ivory (`#F5F2EB`)  

---

## 1. Executive Summary & Module Scope

The **Rent Reminder Management Module** is an integral component of the **RentProof** Landlord and Property Management SaaS. It equips landlords with automated tracking, scheduling, and dispatching of rent due notices, maintenance tasks, and lease communications.

### Core Capabilities:
- **Track Upcoming Rent Due Dates:** Automatically identifies payments due within the next 7 days.
- **Overdue Rent Alerts:** Flags unpaid reminders where the due date has elapsed.
- **Create Reminders for Tenants:** Dynamically associates reminders with registered tenants or broadcasts general maintenance announcements property-wide.
- **Reminder Status Lifecycle:** Manages states (`Pending` → `Sent` → `Completed`) with single-click status toggling.
- **Maintenance Reminders:** Facilitates scheduling and tracking of building repairs, AC servicing, and fire safety checks.
- **Activity Audit Trail:** Real-time logging of reminder creation, status updates, dispatches, and deletions.

---

## 2. Technical Stack (Strict Adherence)

| Layer | Technology | Justification |
| :--- | :--- | :--- |
| **Frontend** | HTML5, CSS3, Vanilla JavaScript | Zero third-party frontend frameworks (No React, Vue, or Angular). Lightweight, fast, and easy to explain. |
| **Backend** | Core Java (JDK 17/21) | Object-Oriented Programming (OOP) adhering strictly to standard college syllabus. |
| **Database** | MySQL (InnoDB Engine) | Shared `rentproof` database enforcing ACID compliance and foreign key integrity. |
| **Connectivity** | Java Database Connectivity (JDBC) | PreparedStatement queries preventing SQL Injection; try-with-resources for leak-free connection management. |
| **IDE** | VS Code / IntelliJ / Eclipse | Standard project setup with Maven `pom.xml` and terminal scripts (`compile-and-run.sh` / `.bat`). |

---

## 3. Architecture & File Structure

```
RentProof/
├── backend/src/main/java/com/rentproof/
│   ├── database/
│   │   └── DBConnection.java         # Shared centralized JDBC connection manager (Aditi)
│   ├── reminder/
│   │   ├── Reminder.java             # Entity model (Encapsulation, Comparable)
│   │   ├── ReminderDAO.java          # Data Access Object (PreparedStatement, CRUD)
│   │   └── ReminderService.java      # Business logic, validations, timeline calculations
│   └── ReminderDemo.java             # Standalone verification runner & viva test suite
├── frontend/
│   ├── reminders/
│   │   ├── reminders.html            # Main Reminder Dashboard (Stats, table, modals)
│   │   ├── add-reminder.html         # Form to create new reminders
│   │   ├── edit-reminder.html        # Form to edit existing reminder details
│   │   ├── reminders.css             # Midnight Blue & Ivory styles
│   │   └── reminders.js              # Vanilla JS logic & localStorage reactivity
│   ├── properties/                   # Integrated sidebar navigation to Reminders
│   └── shared/                       # common.css & sidebar.css
├── database/
│   └── rentproof.sql                 # Non-destructive MySQL schema setup
├── compile-and-run.sh                # macOS/Linux one-click compilation & test runner
└── compile-and-run.bat               # Windows batch compilation & test runner
```

---

## 4. Java Backend Implementation Details

### 4.1. `Reminder.java` (Model)
- **Private Fields:** `reminderId`, `landlordId`, `tenantId` (nullable `Integer`), `tenantName`, `title`, `message`, `dueDate`, `reminderType`, `reminderStatus`, `createdAt`.
- **Encapsulation:** Complete getters and setters.
- **OOP Concepts Applied:**
  - `Comparable<Reminder>` interface implementation: Overrides `compareTo(Reminder other)` to order reminders chronologically by `dueDate` (earliest due date first).
  - Business helper methods: `isPending()`, `isCompleted()`, `isOverdue()`, `isUpcoming(int days)`.
  - Overridden `toString()` for debugging.

### 4.2. `ReminderDAO.java` (Data Access Object)
- **`addReminder(Reminder reminder)`:** Executes parameterized `INSERT` query; returns generated `reminder_id`. Handles `Types.INTEGER` for nullable `tenant_id`.
- **`getAllReminders()`:** Joins `reminders` with `tenants` (`LEFT JOIN`) to populate tenant names.
- **`getRemindersByLandlord(int landlordId)`:** Retrieves reminders isolated to a specific landlord.
- **`getReminderById(int reminderId)`:** Finds single reminder by primary key.
- **`getRemindersByTenant(int tenantId)`:** Filters reminders for a specific tenant.
- **`getPendingReminders(int landlordId)`:** Retrieves active pending reminders.
- **`updateReminderStatus(int reminderId, String newStatus)`:** Modifies status (`Pending`, `Sent`, `Completed`).
- **`updateReminder(Reminder reminder)`:** Full update of title, message, due date, type, status, and tenant.
- **`deleteReminder(int reminderId)`:** Safely removes a reminder record.
- **`searchReminders(int landlordId, String keyword)`:** Parameterized wildcard search across title, message, type, and tenant full name.
- **`getActiveTenants(int landlordId)`:** Dynamically queries `tenants` joined with `properties` to populate tenant select dropdowns.

### 4.3. `ReminderService.java` (Business Logic Layer)
- **Decoupled Architecture:** Business rules, validations, and calculations are kept separate from SQL queries.
- **Input Validation:**
  - `title`: Non-empty, 3 to 150 characters.
  - `message`: Non-empty, at least 5 characters.
  - `dueDate`: Mandatory, validates ISO `YYYY-MM-DD` date parsing.
  - `reminderType`: Validates against allowed constants (`Rent Due`, `Maintenance`, `General`, `Agreement Expiry`).
  - `reminderStatus`: Validates against allowed statuses (`Pending`, `Completed`, `Sent`).
- **Timeline & Statistical Computing:**
  - `getUpcomingReminders(landlordId, withinDays)`: Filters pending reminders due between today and today + days.
  - `getOverdueReminders(landlordId)`: Filters reminders whose due dates have passed and are not completed.
  - `calculateReminderStats(landlordId)`: Computes counts (`total`, `pending`, `completed`, `upcoming`, `overdue`) for dashboard summary widgets.
  - `sortRemindersByDueDate(list)`: Invokes `Collections.sort(list)` leveraging `Comparable`.

---

## 5. Frontend UI/UX Design (Midnight Blue & Ivory)

The interface strictly adopts the RentProof design system:
- **Navy (`#26364A`):** Primary branding, sidebar, table headers, active buttons.
- **Steel Blue (`#526B82`):** Links, secondary action icons, focus rings.
- **Ivory (`#F5F2EB`):** Main content background, form canvas.
- **Sand (`#C6B99F`):** Card borders, subtle highlights, template chips.
- **Slate (`#344454`):** High-contrast typography and neutral dark elements.

### Dashboard Components:
1. **Collapsible Sidebar:** Links to Properties, Reminders (Active), Dashboard, Tenants, Payments.
2. **Top Navbar:** Live search bar, notifications bell, landlord profile pill.
3. **5 Metric Cards:** Total Reminders, Pending, Upcoming (7 Days), Overdue, and Completed.
4. **Toolbar:** Quick tab pills (`All`, `Pending`, `Upcoming`, `Overdue`, `Completed`, `Maintenance`), Category dropdown, and real-time search.
5. **Reminders Table:** Displays reminder ID, tenant with avatar initials, title and message preview, dynamic due date countdown badge, type badge, status badge, and action buttons (`Complete`, `Send Notice`, `View`, `Edit`, `Delete`).
6. **Activity Log Feed:** Displays recent user actions and system alerts.
7. **Modals:**
   - View Reminder Details.
   - Dispatch Notice Simulation (SMS / WhatsApp notification preview).
   - Delete Safety Confirmation Modal.

---

## 6. How to Run & Verify

### Option A: macOS / Linux Terminal
```bash
cd /Users/gungun/Desktop/RentProof
./compile-and-run.sh
```

### Option B: Windows Command Prompt
```cmd
cd RentProof
compile-and-run.bat
```

### Option C: Browser UI Demonstration
1. Open `frontend/reminders/reminders.html` directly in Google Chrome, Edge, or Safari.
2. Test dashboard interactions:
   - Click tab pills (`Pending`, `Upcoming`, `Overdue`, `Completed`) to filter rows.
   - Click the checkmark action (`✓`) to toggle status to `Completed`.
   - Click `➕ Create New Reminder` to test adding a reminder with quick templates.
   - Test search by typing "December", "Rohan", or "AC".
   - Test editing and deleting reminders with the confirmation modal.

---

## 7. Viva Voce Preparation & Faculty Questions

### Q1: Why did you separate `ReminderDAO` and `ReminderService`?
> **Answer:** This adheres to the **Single Responsibility Principle (SRP)** and **Layered Architecture**. `ReminderDAO` is solely responsible for database persistence and SQL execution (CRUD), while `ReminderService` encapsulates business logic, input validation, date computations, and status lifecycle rules. This allows business rules to be tested independently of the database.

### Q2: Why did you use `PreparedStatement` instead of `Statement`?
> **Answer:**
> 1. **Security:** `PreparedStatement` uses parameterized queries (`?`), preventing SQL Injection attacks by treating input as literal data rather than executable SQL.
> 2. **Performance:** Database engines pre-compile and cache execution plans for `PreparedStatement`, speeding up repeated queries.
> 3. **Type Safety:** Handles proper conversion of Java data types (`Date`, `int`, `null`) automatically.

### Q3: Why is `tenantId` an `Integer` wrapper class instead of primitive `int` in `Reminder.java`?
> **Answer:** In our MySQL database schema, the `tenant_id` column in the `reminders` table is nullable (`INT NULL`) because some reminders are general or property-wide (such as building fire drills or water tank maintenance) without being addressed to a specific tenant. In Java, primitive `int` cannot be `null` (it defaults to `0`), whereas the wrapper class `Integer` can hold `null`, accurately mirroring the database schema. In JDBC, we use `pstmt.setNull(2, Types.INTEGER)` when `tenantId == null`.

### Q4: How does sorting by due date work in your Java code?
> **Answer:** `Reminder` implements `java.lang.Comparable<Reminder>` and overrides `public int compareTo(Reminder other)`. The method compares `this.dueDate.compareTo(other.dueDate)`. Calling `Collections.sort(reminderList)` automatically sorts the reminders chronologically, placing the earliest due dates first.

### Q5: How did you handle resource management in JDBC?
> **Answer:** We used Java 7+'s **try-with-resources** statement. Any object implementing `java.lang.AutoCloseable` (such as `Connection`, `PreparedStatement`, and `ResultSet`) declared within the try parentheses is guaranteed to be closed automatically upon exit, even if an exception occurs. This prevents database connection leaks and cursor exhaustion.
