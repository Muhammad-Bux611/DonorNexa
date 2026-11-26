# DonorNexa — Smart Emergency Blood-Donor Coordination Portal

> **Tagline:** Connecting Donors. Bridging Lives. Saving Hope.  
> **Course:** Web Engineering (23SW) — Complex Engineering Problem (CEP)  
> **Architecture:** Modular Monolithic Architecture with Feature-Based Domain Packages  
> **UI Theme:** Red and White Healthcare Theme (`#c8102e`, `#ffffff`)  
> **Test Status:** **389 / 389 Tests Passing (100% Pass Rate, 0 Failures, 0 Errors, 0 Skipped)**  
> **Java Version:** Java 21 LTS | **Spring Boot:** 3.5.7  

---

## 1. Project Introduction

**DonorNexa** is an enterprise-grade emergency blood-donor coordination platform designed to bridge the life-critical gap between healthcare institutions facing urgent blood shortages and medically eligible voluntary blood donors. By replacing slow, fragmented manual phone trees and unverified social media requests with an automated, geo-aware, real-time matching engine, DonorNexa accelerates emergency blood procurement from hours to minutes.

---

## 2. Modular Monolithic Architecture & Package Layout

DonorNexa organizes source code by **functional business modules**, ensuring that each domain feature encapsulates its own entities, repositories, services, controllers, DTOs, and validation logic:

```text
com.donornexa/
├── DonorNexaApplication.java       # Application Bootstrap & Asia/Karachi Timezone Init
│
├── config/                         # Application Configuration
│   ├── SecurityConfig.java         # Spring Security 6, BCrypt, CSRF, Session Policy
│   ├── WebConfig.java              # WebMvcConfigurer, Interceptors, Upload Resource Handlers
│   ├── FileStorageConfig.java      # Dynamic Cross-PC Upload Directory Initializer
│   ├── SchedulingConfig.java       # @EnableScheduling for Background Sweeps
│   ├── RoleBasedSuccessHandler.java# Post-Login Redirection (Admin, Hospital, Donor)
│   └── AdminPasswordChangeInterceptor.java # Mandatory Admin First-Login Password Change
│
├── common/                         # Cross-Cutting Infrastructure
│   ├── enums/                      # Standardized Enums (BloodGroup, RequestStatus, Role, etc.)
│   ├── exception/                  # Centralized GlobalExceptionHandler & Domain Exceptions
│   ├── service/                    # FileUploadService (Deep Binary Magic Bytes Validation)
│   ├── util/                       # PhoneMaskingUtil (Donor Privacy + Audited Reveal)
│   ├── controller/                 # HomeController, PublicMetricsController
│   └── advice/                     # GlobalDashboardControllerAdvice
│
├── auth/                           # Authentication & Account Lifecycle
│   ├── controller/                 # AuthController, LoginController
│   ├── service/                    # AuthService, CustomUserDetailsService
│   └── dto/                        # DonorRegistrationRequest, HospitalRegistrationRequest
│
├── user/                           # Shared User Accounts
│   ├── entity/                     # User JPA Entity
│   └── repository/                 # UserRepository
│
├── donor/                          # Voluntary Donor Management
│   ├── controller/                 # DonorController, DonorMatchController, DonorMessageController
│   ├── service/                    # DonorService, DonorEligibilityService
│   ├── entity/                     # Donor JPA Entity
│   ├── repository/                 # DonorRepository
│   └── dto/                        # Profile, Availability, Eligibility DTOs
│
├── hospital/                       # Hospital Accreditation & Operations
│   ├── controller/                 # HospitalController, HospitalMatchCoordinationController, HospitalReportController
│   ├── service/                    # HospitalService, HospitalVerificationService, HospitalReportService
│   ├── entity/                     # Hospital JPA Entity
│   ├── repository/                 # HospitalRepository
│   └── dto/                        # HospitalProfileDTO, Verification DTOs
│
├── bloodrequest/                   # Emergency Blood Request Lifecycle
│   ├── controller/                 # HospitalBloodRequestController
│   ├── service/                    # HospitalBloodRequestService, BloodRequestAutoExpiryScheduler
│   ├── entity/                     # BloodRequest JPA Entity
│   ├── repository/                 # BloodRequestRepository (Pessimistic Write Locking)
│   └── dto/                        # CreateBloodRequestDTO
│
├── matching/                       # Compatibility & Candidate Ranking Engine
│   ├── service/                    # BloodCompatibilityService, DonorMatchingService
│   ├── entity/                     # RequestMatch JPA Entity
│   ├── repository/                 # RequestMatchRepository
│   └── dto/                        # DonorMatchDTO, MatchingEvaluationSummaryDTO
│
├── donation/                       # Completed Donation Coordination
│   ├── controller/                 # HospitalDonationController
│   ├── service/                    # HospitalDonationService
│   ├── entity/                     # Donation JPA Entity
│   └── repository/                 # DonationRepository
│
├── inventory/                      # Hospital Blood Bank Stock & FEFO
│   ├── controller/                 # HospitalInventoryController
│   ├── service/                    # HospitalInventoryService
│   ├── entity/                     # BloodInventory, BloodStockEntry JPA Entities
│   └── repository/                 # BloodInventoryRepository, BloodStockEntryRepository
│
├── badge/                          # Gamification Milestone Recognition
│   ├── entity/                     # DonationBadge (Bronze 1, Silver 5, Gold 10)
│   └── service/                    # DonationBadgeService
│
├── notification/                   # Stakeholder Alerts & Messages
│   ├── controller/                 # NotificationController
│   ├── service/                    # NotificationService
│   ├── entity/                     # Notification JPA Entity
│   └── repository/                 # NotificationRepository
│
├── location/                       # Pakistan Geographic Hierarchy
│   ├── controller/                 # LocationController (Cascading Dropdowns)
│   ├── service/                    # LocationService, LocationDataSeeder
│   ├── entity/                     # Province, City JPA Entities
│   └── repository/                 # ProvinceRepository, CityRepository
│
├── messaging/                      # Two-Way Coordination Chat
│   ├── service/                    # MessagingService (IDOR Defense)
│   ├── entity/                     # Conversation, Message JPA Entities
│   └── repository/                 # ConversationRepository, MessageRepository
│
└── audit/                          # Immutable Audit Trail
    ├── service/                    # AdminAuditLogService
    ├── entity/                     # AuditLog JPA Entity
    └── repository/                 # AuditLogRepository
```

---

## 3. Prerequisites for Running on Any PC

To run DonorNexa on another Windows or Linux PC, ensure the following are installed:
1. **Java Development Kit (JDK) 21 LTS**
   - Verify: `java -version` (Must show version 21+)
2. **MySQL Server 8.0+**
   - Verify: `mysql --version` (Ensure MySQL service is running)
3. **Git** (if cloning the repository)
   - Note: Apache Maven is **not** required to be pre-installed; the included Maven Wrapper (`mvnw` / `mvnw.cmd`) handles Maven automatically.

---

## 4. Database Setup & Initialization

### Step 1: Create Database
Connect to your local MySQL instance using MySQL Workbench or terminal:

```sql
CREATE DATABASE IF NOT EXISTS donornexa_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE donornexa_db;
```

### Step 2: Execute SQL Scripts in Correct Sequence
Run the scripts in `sql/` in the following strict order:

1. `sql/schema.sql` — Creates 14 normalized 3NF tables, constraints, indexes.
2. `sql/views.sql` — Creates `v_eligible_donors` view.
3. `sql/triggers.sql` — Creates triggers updating last donation date.
4. `sql/seed.sql` — Seeds 32 voluntary donors, 6 hospitals, 22 requests, and initial admin.
5. `sql/reports.sql` — 5 analytical queries for executive reporting.

Command Line execution:
```bash
mysql -u root -p donornexa_db < sql/schema.sql
mysql -u root -p donornexa_db < sql/views.sql
mysql -u root -p donornexa_db < sql/triggers.sql
mysql -u root -p donornexa_db < sql/seed.sql
```

---

## 5. Database Connection Configuration

DonorNexa is cross-PC portable and contains **no hardcoded credentials**. Configure connection properties via environment variables:

### Setting Environment Variables:

#### On Windows (PowerShell):
```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/donornexa_db?useSSL=false&serverTimezone=Asia/Karachi"
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "your_actual_mysql_password"
$env:SERVER_PORT = "8080"
$env:UPLOAD_DIR = "uploads"
```

#### On Windows (Command Prompt - CMD):
```cmd
set DB_URL=jdbc:mysql://localhost:3306/donornexa_db?useSSL=false&serverTimezone=Asia/Karachi
set DB_USERNAME=root
set DB_PASSWORD=your_actual_mysql_password
set SERVER_PORT=8080
set UPLOAD_DIR=uploads
```

#### On Linux / macOS (Bash / Zsh):
```bash
export DB_URL="jdbc:mysql://localhost:3306/donornexa_db?useSSL=false&serverTimezone=Asia/Karachi"
export DB_USERNAME="root"
export DB_PASSWORD="your_actual_mysql_password"
export SERVER_PORT="8080"
export UPLOAD_DIR="uploads"
```

*An example file `.env.example` and `src/main/resources/application-example.properties` are provided for reference.*

---

## 6. Running & Building the Application

DonorNexa supports two execution modes:

### Mode A: Zero-Config Standalone / Viva Demo Mode (No MySQL Setup Required!)
Uses an embedded in-memory database with MySQL syntax compatibility. Starts instantly on ANY machine without installing or configuring MySQL:

#### Windows (Command Prompt or PowerShell):
```cmd
mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

#### Linux / macOS:
```bash
chmod +x ./mvnw
./mvnw spring-boot:run "-Dspring-boot.run.profiles=dev"
```

---

### Mode B: Full MySQL Database Mode (Standard Submission Setup)
Requires MySQL 8.0 running on port 3306 with `donornexa_db` created and `sql/*.sql` executed.

#### Passing MySQL password directly:
```powershell
mvnw.cmd spring-boot:run -Dspring-boot.run.arguments="--spring.datasource.password=YOUR_MYSQL_PASSWORD"
```

#### Or with environment variable:
```powershell
$env:DB_PASSWORD = "YOUR_MYSQL_PASSWORD"
mvnw.cmd spring-boot:run
```

---

### Running All 389 Automated Tests:
```cmd
mvnw.cmd clean test
```

### Building the Production JAR:
```cmd
mvnw.cmd clean package
```
The executable archive is generated at `target/donornexa-0.0.1-SNAPSHOT.jar`. Run with:
```bash
java -jar target/donornexa-0.0.1-SNAPSHOT.jar
```

---

## 7. Pre-Seeded Test Credentials

All pre-seeded test accounts use the password: `ValidPass123!` (BCrypt hash `$2a$10$Ono6ojcTlI.QoHb3flTIcOWjilUu0zbb0l6XVkgLA6H2Om1nDmrvK`).

| Portal | Role | Email Address | Password | Pre-configured Details |
| :--- | :--- | :--- | :--- | :--- |
| **Admin Portal** | `ROLE_ADMIN` | `admin@donornexa.com` | `ValidPass123!` | System Administrator, Platform Operations Command |
| **Hospital Portal** | `ROLE_HOSPITAL`| `hospital.akuh@donornexa.com` | `ValidPass123!` | Aga Khan University Hospital (Karachi), Trust Score 100 |
| **Hospital Portal** | `ROLE_HOSPITAL`| `hospital.indus@donornexa.com`| `ValidPass123!` | Indus Hospital & Health Network (Karachi), Trust Score 95 |
| **Hospital Portal** | `ROLE_HOSPITAL`| `hospital.lnh@donornexa.com`  | `ValidPass123!` | Liaquat National Hospital (Karachi), Trust Score 90 |
| **Hospital Portal** | `ROLE_HOSPITAL`| `hospital.skmch@donornexa.com`| `ValidPass123!` | Shaukat Khanum Memorial Hospital (Lahore), Trust Score 100 |
| **Donor Portal** | `ROLE_DONOR` | `tariq.jamil@gmail.com` | `ValidPass123!` | Tariq Jamil, Blood Group: **O-** (Universal Donor), Karachi |
| **Donor Portal** | `ROLE_DONOR` | `ahmed.khan@gmail.com` | `ValidPass123!` | Ahmed Khan, Blood Group: **A+**, Karachi, Bronze Lifesaver |
| **Donor Portal** | `ROLE_DONOR` | `fatima.tariq@gmail.com`| `ValidPass123!` | Fatima Tariq, Blood Group: **A+**, Karachi |
| **Donor Portal** | `ROLE_DONOR` | `kashif.mehmood@gmail.com`| `ValidPass123!`| Kashif Mehmood, Blood Group: **AB+**, Karachi |
| **Donor Portal** | `ROLE_DONOR` | `saad.iqbal@gmail.com` | `ValidPass123!` | Saad Iqbal, Blood Group: **AB-** (Rare), Karachi |

---

## 8. Common Troubleshooting & Solutions

1. **Java Version Mismatch (`release version 21 not supported`):**
   - Ensure `JAVA_HOME` points to a valid JDK 21 installation.
   - Windows PowerShell: `$env:JAVA_HOME = "C:\Path\To\jdk-21"`
2. **Access Denied for user 'root'@'localhost' (using password: NO / YES):**
   - Your local MySQL server has a root password set. Either provide your password:
     `mvnw.cmd spring-boot:run -Dspring-boot.run.arguments="--spring.datasource.password=YOUR_PASSWORD"`
   - Or run the zero-setup demo mode without MySQL:
     `mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"`
3. **MySQL Connection Refused (`Communications link failure`):**
   - Check if MySQL service is running (`services.msc` on Windows or `systemctl status mysql` on Linux).
   - Alternatively, run with `-Dspring-boot.run.profiles=dev`.
4. **Port 8080 already in use:**
   - Override the port using `$env:SERVER_PORT="8081"` or pass `--server.port=8081`.

---

## 9. Viva Examination & Viva Defense Quick Reference

When asked questions by examiners during your project defense, locate the files immediately using this cheat sheet:

| Examiner Question / Topic | Module Package | Primary Class / File | Key Method / Function |
| :--- | :--- | :--- | :--- |
| **User Login & Session Security** | `com.donornexa.config` | `SecurityConfig.java` | `securityFilterChain()`, BCrypt, session-fixation protection |
| **Role-Based Redirect after Login** | `com.donornexa.config` | `RoleBasedSuccessHandler.java` | Redirects ADMIN to `/admin/dashboard`, HOSPITAL to `/hospital/dashboard`, DONOR to `/donor/dashboard` |
| **Blood Compatibility Rules** | `com.donornexa.matching` | `BloodCompatibilityService.java` | `isCompatible(donorGroup, recipientGroup)` — O- universal donor, AB+ universal recipient |
| **Donor Ranking & Scoring Algorithm** | `com.donornexa.matching` | `DonorMatchingService.java` | Calculates distance/city match, availability, days since last donation, responsiveness score |
| **Emergency Request & Pessimistic Lock**| `com.donornexa.bloodrequest` | `BloodRequestRepository.java` | `@Lock(LockModeType.PESSIMISTIC_WRITE)` prevents race conditions when claiming requests |
| **Auto-Expiry Background Scheduler** | `com.donornexa.bloodrequest` | `BloodRequestAutoExpiryScheduler.java`| `@Scheduled(fixedRate = 60000)` sweeps expired requests after urgency timeout (CRITICAL 2h, URGENT 6h, ROUTINE 24h) |
| **Hospital Blood Stock & FEFO Inventory**| `com.donornexa.inventory` | `HospitalInventoryService.java` | First-Expired-First-Out (FEFO) automated stock deduction and expiry tracking |
| **Donor 90-Day Eligibility Rule** | `com.donornexa.donor` | `DonorEligibilityService.java` | Checks 90 days between whole blood donations, weight ≥ 50kg, age 18-65 |
| **Gamification Badges (Bronze/Silver/Gold)**| `com.donornexa.badge` | `DonationBadgeService.java` | Bronze (1 donation), Silver (5 donations), Gold (10 donations) |
| **Two-Way Chat Messaging & IDOR Guard** | `com.donornexa.messaging` | `MessagingService.java` | Validates session user belongs to conversation before returning chat messages |
| **Audit Trail Logging (Immutable)** | `com.donornexa.audit` | `AdminAuditLogService.java` | Logs action, actor IP, timestamp, role, target entity |
| **File Upload Magic Bytes Validation** | `com.donornexa.common` | `FileUploadService.java` | Inspects binary file headers (JPEG `FF D8 FF`, PNG `89 50 4E 47`, PDF `25 50 44 46`) to prevent disguised malicious files |
| **Donor Phone Privacy Masking** | `com.donornexa.common` | `PhoneMaskingUtil.java` | Masks voluntary donor numbers (`0300-***-1234`) until accepted match, with audited reveal |
| **Cascading Province-City Dropdowns** | `com.donornexa.location` | `LocationController.java` | `GET /api/provinces`, `GET /api/cities?provinceId={id}` REST endpoints for AJAX |
| **Thymeleaf UI Templates** | `src/main/resources/templates`| `templates/{admin,hospital,donor,auth,common}/` | HTML5 + Thymeleaf layout fragments with CSRF tokens on every POST form |
| **Database Schema, Views & Triggers** | `sql/` | `schema.sql`, `views.sql`, `triggers.sql`, `seed.sql`, `reports.sql` | 14 normalized 3NF tables with foreign keys, triggers, and analytical views |
