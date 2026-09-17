# 📝 Leave Management System — Technical Interview Guide

Use this guide to explain the project architecture, design choices, and request lifecycle under interview pressure.

---

## 🏗️ 1. Architecture Overview (The 10-Second Pitch)
"This is a three-tier, decoupled **Employee Leave Management System**. It uses a **React.js** single-page application frontend, a **Spring Boot** REST API backend, and a **MySQL** relational database. Authentication is completely stateless using **JWT (JSON Web Tokens)**, and authorization is enforced via **Spring Security** (on the backend) and **React Router DOM** (on the frontend)."

---

## 🔄 2. End-to-End Request Lifecycles

### A. The Login Flow
```
[React Login Page] ──(Credentials Post)──► [Spring Controller] ──► [AuthService] 
                                                                     │
  ◄──(HTTP 200 + JWT Token Response)─────────────────────────────────┘
```
1. **User Submits Credentials:** The user submits their `username` and `password` on the React UI.
2. **API Request:** Axios POSTs a JSON payload to `/api/auth/login`.
3. **Authentication Manager Check:** The backend `AuthController` delegates authentication to `AuthService`. The `AuthenticationManager` hashes the password using `BCrypt` and matches it against the stored hash in the DB.
4. **Token Generation (`JwtUtil`):** Upon success, the system calls `JwtUtil.generateToken()`, embedding the user's username (subject), user ID, first name, and role into the JWT payload as custom claims.
5. **HS256 Signing:** The token is signed using a symmetric **HMAC SHA-256 (`HS256`)** key, ensuring integrity (clients cannot alter roles/IDs).
6. **Token Delivery:** The server returns HTTP 200 with the JWT string. The React client saves the token and user metadata in `localStorage`.

---

### B. Calling Protected APIs
```
[React View] ──(Axios Interceptor Adds Token)──► [JwtAuthFilter Middleware] ──► [Controller]
```
1. **Axios Request Interceptor:** Whenever React initiates an API call (e.g. fetching leave history), an Axios interceptor in `api.js` automatically reads the token from `localStorage` and appends it to the header: `Authorization: Bearer <token>`.
2. **Filter Interception (`JwtAuthFilter`):** In the backend, the request passes through the `JwtAuthFilter` (which extends `OncePerRequestFilter` so it runs exactly once per request thread).
3. **Parsing & Expiration Verification:**
   - Extracts the token from the header.
   - Decodes claims via Jwts parser (which automatically checks signature validity using the secret key).
   - Extracts the username and verifies the token is not expired (`isTokenExpired()`).
4. **Context Injection:** If valid, the filter loads the user details and puts a `UsernamePasswordAuthenticationToken` into the `SecurityContextHolder`. Spring Security now considers the current thread authenticated.
5. **Session Expiry Handling (Frontend):** If the server returns `401 Unauthorized` (e.g., token expired), an Axios response interceptor intercepts the error, clears `localStorage`, and redirects the user to `/login`.

---

### C. Leave Request & Approval Transaction
```
[Employee: Apply] ──► PENDING request saved (Balance NOT deducted yet)
[Manager: Approve] ──► Balance Deducted & Request APPROVED (Atomic Transaction)
```
1. **Application:** An employee submits a leave request. The `LeaveService.applyLeave()` method calculates the inclusive duration (`endDate - startDate + 1`), checks if the employee has enough remaining days, and creates a record in `leave_requests` with status `PENDING`.
   - *Design Choice:* The balance is **not** deducted yet, so the days are not locked if the manager rejects the request.
2. **Manager Approval:** The manager views the pending requests of their direct reports. They click **Approve** and can add feedback.
3. **Atomic Balance Update (`@Transactional`):** `ManagerService.approveRequest()` is annotated with `@Transactional`. It performs two database writes:
   - Updates the employee's `LeaveBalance` (adds days to `used_days`, subtracts from `remaining_days`).
   - Updates the `LeaveRequest` status to `APPROVED`.
   - *Why Transactional?* If either database write fails, the entire transaction rolls back, preventing data mismatch (e.g. approved request but no balance deduction).

---

## 💡 3. Key Design Decisions (and How to Defend Them)

### Q: Why use stateless JWT over stateful Sessions?
> "JWT makes the backend completely stateless. The server doesn't need to store session states in memory or shared caches (like Redis). Any instance of our Spring Boot app can validate any token because the verification relies entirely on validating the token's cryptographic signature."

### Q: What is the "Self-Healing" balance pattern in your service?
> "When fetching employee balances in `LeaveService.getMyBalances()`, if the size of the user's balance list is less than the count of active leave types in the database, the backend automatically seeds the missing balances on the fly. This prevents application crashes when new leave policies are added by admins."

### Q: Why use "Soft Delete" (`isActive`) for employees?
> "If we hard-deleted an employee record, it would cascade-delete or break foreign key references on historical leave records (which are required for analytics/auditing). Instead, we set `isActive = false`, retaining historical data integrity while preventing the user from logging in."

### Q: Why replace Java Streams with simple for-loops?
> "Imperative loops are much easier to read, debug, and explain on a whiteboard during technical discussions than complex lambda streams, reducing cognitive load without affecting performance."
