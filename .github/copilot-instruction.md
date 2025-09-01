### **AI Prompt: Blueprint for a Personal Website with Spring Boot & Thymeleaf**

**You are an expert Senior Java Developer specializing in the Spring ecosystem. Your task is to act as my pair programmer and generate code for a personal website based on the following detailed blueprint.**

---

### **1. Project Overview & Goals**

We are building a backend-driven personal website for a professional (e.g., a coach, consultant, or creator). The site will serve as a portfolio, a content platform, and a client management tool.

*   **Public-Facing Features:** A blog and information pages.
*   **Client-Facing Features:** A secure personal account area to book sessions, view past bookings, and access exclusive content.
*   **Admin-Facing Features:** The ability to manage blog posts, available booking slots, and user content.

### **2. Technology Stack**

*   **Language:** Java 21
*   **Framework:** Spring Boot 
*   **Build Tool:** Maven
*   **Database:** PostgreSQL
*   **View Layer:** Thymeleaf (with Layout Dialect for templating)
*   **Core Dependencies:**
    *   `spring-boot-starter-web`
    *   `spring-boot-starter-data-jpa`
    *   `spring-boot-starter-thymeleaf`
    *   `spring-boot-starter-security`
    *   `spring-boot-starter-mail`
    *   `postgresql` (JDBC driver)
    *   `lombok` (to reduce boilerplate)
    *   `nz.net.ultraq.thymeleaf:thymeleaf-layout-dialect`

### **3. Database Schema Design (PostgreSQL)**

Generate the JPA Entity classes (`@Entity`) for the following tables. Include standard annotations like `@Id`, `@GeneratedValue`, relationships (`@ManyToOne`, `@OneToMany`), and basic constraints (`@Column(nullable = false)`).

1.  **`users`**
    *   `id` (BIGINT, Primary Key, Auto-increment)
    *   `email` (VARCHAR(255), Not Null, Unique)
    *   `password` (VARCHAR(255), Not Null) - *Will be BCrypt hashed*
    *   `full_name` (VARCHAR(255), Not Null)
    *   `role` (VARCHAR(50), Not Null) - *e.g., 'ROLE_USER', 'ROLE_ADMIN'*
    *   `created_at` (TIMESTAMP)

2.  **`blog_posts`**
    *   `id` (BIGINT, PK, Auto-increment)
    *   `title` (VARCHAR(255), Not Null)
    *   `slug` (VARCHAR(255), Not Null, Unique) - *A URL-friendly version of the title*
    *   `content` (TEXT, Not Null)
    *   `author_id` (BIGINT, Foreign Key to `users.id`)
    *   `created_at` (TIMESTAMP)
    *   `published_at` (TIMESTAMP, Nullable) - *If null, it's a draft*
    *   **Relationship:** `ManyToOne` with `User` (author)

3.  **`booking_slots`**
    *   `id` (BIGINT, PK, Auto-increment)
    *   `start_time` (TIMESTAMP, Not Null)
    *   `end_time` (TIMESTAMP, Not Null)
    *   `is_booked` (BOOLEAN, Not Null, Default: false)

4.  **`bookings`**
    *   `id` (BIGINT, PK, Auto-increment)
    *   `client_id` (BIGINT, FK to `users.id`)
    *   `slot_id` (BIGINT, FK to `booking_slots.id`, Unique) - *One booking per slot*
    *   `status` (VARCHAR(50), Not Null) - *e.g., 'CONFIRMED', 'PENDING_PAYMENT', 'CANCELLED'*
    *   `created_at` (TIMESTAMP)
    *   **Relationships:** `ManyToOne` with `User` (client), `OneToOne` with `BookingSlot`.

5.  **`personal_content`**
    *   `id` (BIGINT, PK, Auto-increment)
    *   `title` (VARCHAR(255), Not Null)
    *   `description` (TEXT)
    *   `content_url` (VARCHAR(255)) - *Link to a video, PDF, etc.*
    *   `content_type` (VARCHAR(50)) - *e.g., 'VIDEO', 'ARTICLE', 'PDF'*
    *   `created_at` (TIMESTAMP)

### **4. Application Architecture (Layered)**

Structure the application code into the following packages:
*   `com.myapp.config`: Spring Security, Mail configuration.
*   `com.myapp.controller`: Spring MVC Controllers handling HTTP requests.
*   `com.myapp.model`: JPA Entities.
*   `com.myapp.repository`: Spring Data JPA Repositories (interfaces extending `JpaRepository`).
*   `com.myapp.service`: Business logic, transaction management.
*   `com.myapp.dto`: Data Transfer Objects for forms and API responses.
*   `com.myapp.util`: Helper classes.

### **5. Core Feature Breakdown**

#### **A. User Management & Security**

*   **Flow:** Registration -> Login -> Access Personal Account.
*   **Backend:**
    *   `WebSecurityConfig`: Configure form-based login, password hashing (BCrypt), and role-based URL authorization (`/admin/**` requires `ROLE_ADMIN`, `/account/**` requires `ROLE_USER`).
    *   `UserService`: Handle user registration, find user by email.
    *   `AuthController`: Show login/registration pages (`@GetMapping`). Handle user registration (`@PostMapping`).
    *   `AccountController`: Show the user's personal dashboard (`/account/dashboard`).
*   **Frontend (Thymeleaf):**
    *   `templates/login.html`: Login form.
    *   `templates/register.html`: Registration form.
    *   `templates/account/dashboard.html`: Main page for logged-in users.

#### **B. Booking System**

*   **Flow:** User sees available slots -> Clicks to book -> Confirms booking -> Receives email.
*   **Backend:**
    *   `BookingService`:
        *   `getAvailableSlots()`: Fetch all `BookingSlot` where `is_booked = false`.
        *   `createBooking(User client, Long slotId)`: Core logic. Checks if slot is available, creates a `Booking` record, marks the `BookingSlot` as booked.
    *   `BookingController`:
        *   `@GetMapping("/booking")`: Display the page with available slots.
        *   `@PostMapping("/booking/create")`: Handle the booking form submission.
    *   `AdminBookingController` (`/admin/bookings`):
        *   Methods to view all bookings, create/delete available `BookingSlot`s.
*   **Frontend (Thymeleaf):**
    *   `templates/booking.html`: A page displaying available time slots (e.g., in a list or simple calendar view).
    *   `templates/account/my_bookings.html`: A list of the current user's past and upcoming bookings.
    *   `templates/admin/slots.html`: A form for the admin to add new available slots.

#### **C. Email Notifications**

*   **Flow:** System events trigger emails.
*   **Backend:**
    *   `application.properties`: Configure SMTP server settings (host, port, username, password).
    *   `EmailService`: An `@Service` with methods:
        *   `sendWelcomeEmail(User user)`: Called after successful registration.
        *   `sendBookingConfirmation(Booking booking)`: Called after a successful booking.
    *   This service should be called from the `UserService` and `BookingService`.
*   **Frontend (Thymeleaf):**
    *   `templates/email/welcome.html`: Thymeleaf template for the welcome email.
    *   `templates/email/booking-confirmation.html`: Template for the booking confirmation.

#### **D. Blog & Personal Content**

*   **Flow (Blog):** Visitor views list of posts -> Clicks to read a full post. Admin logs in to create/edit posts.
*   **Backend:**
    *   `BlogPostService`: Logic to find all published posts, find a post by its slug, save/update posts.
    *   `BlogPostController`: Public-facing endpoints (`/blog`, `/blog/{slug}`).
    *   `AdminBlogController` (`/admin/blog`): CRUD operations for blog posts.
*   **Flow (Personal Content):** Logged-in user navigates to their account to view exclusive content.
*   **Backend:**
    *   `PersonalContentService`: Logic to retrieve content.
    *   `AccountController`: Add an endpoint like `@GetMapping("/account/content")` to display content.
*   **Frontend (Thymeleaf):**
    *   `templates/blog/list.html`: Displays a list of all blog posts with titles and summaries.
    *   `templates/blog/post.html`: Displays a single full blog post.
    *   `templates/admin/blog-form.html`: A form for creating/editing blog posts.
    *   `templates/account/content.html`: A page listing available personal content for the user.

### **6. UI Layout (Thymeleaf Layout Dialect)**

Create a main layout template that other pages will decorate.

*   `templates/layout/main-layout.html`:
    *   Contains the `<html>`, `<head>`, and `<body>` tags.
    *   Includes a common header/navigation bar.
    *   Includes a common footer.
    *   Has a main content section: `<div layout:fragment="content"></div>`.
*   All other view templates (e.g., `booking.html`) will start with `<html layout:decorate="~{layout/main-layout}">` and define their content inside `<div layout:fragment="content">`.