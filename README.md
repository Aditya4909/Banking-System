# JavaBank - Enterprise Core Banking System

[![Java Version](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.1-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![JavaFX](https://img.shields.io/badge/JavaFX-17-blue.svg)](https://openjfx.io/)
[![JUnit 5](https://img.shields.io/badge/JUnit-5-red.svg)](https://junit.org/junit5/)
[![Docker](https://img.shields.io/badge/Docker-Enabled-2496ED.svg)](https://www.docker.com/)
[![Render](https://img.shields.io/badge/Deploy_to-Render-46E3B7.svg)](https://render.com/)

JavaBank is an enterprise-grade core retail banking system built with **Core Java 17**, **Spring Boot 3 REST APIs**, a **Dark Glassmorphism Single-Page Web Dashboard**, and a **JavaFX Desktop GUI**. It features a robust multi-layered architecture decoupled from external database engines, demonstrating textbook implementations of advanced object-oriented design, generic repositories, custom checked exception boundaries, functional interfaces, primitive mapping streams, deep-copy cyclic snapshots, and cloud containerization.

---

## 🌐 Live Cloud Web Deployment (Render / Railway)

JavaBank is pre-configured with a multi-stage **`Dockerfile`** and a **`render.yaml`** blueprint for instant, free 1-click cloud deployment on [Render.com](https://render.com).

### 🚀 Deploying to Render in 3 Steps:
1. **Create an account** on [Render.com](https://render.com) and log in.
2. Click **New +** ➔ **Web Service** ➔ **Connect your GitHub repository** (`Aditya4909/Banking-System`).
3. Set the following build options:
   * **Environment**: `Docker` (or select `Java`)
   * **Branch**: `main`
   * **Plan**: `Free`
4. Click **Deploy Web Service**! Render will build the container and provide you with a live HTTPS link (e.g. `https://javabank-cloud.onrender.com`).

---

## 🏛️ Architectural Overview

JavaBank is built around strict separation of concerns, supporting dual presentation layers (Web Single-Page Application & Desktop JavaFX Client) powered by the same underlying service engine:

```
[ Web Browser Client (Mobile/Desktop) ]       [ JavaFX Desktop Client ]
  ├── Single-Page Application (SPA)             └── BankingApp.java (Stage / Controls)
  ├── Dark Glassmorphism Theme (styles.css)
  └── Interactive Chart.js Visualizations
        │                                             │
        ▼ (JSON REST Requests over HTTPS)             │
[ Spring Boot REST API Layer (com.javabank.web) ]     │
  ├── ApiController.java (REST Endpoints)             │
  ├── GlobalExceptionHandler.java (Domain errors)     │
  └── WebConfig (Static Asset Routing)                │
        │                                             │
        ▼                                             ▼
[ Business Service Layer (com.javabank.service) ]
  ├── BankService.java / BankServiceImpl.java (Coordinates core actions)
  └── Exception Boundaries (Enforces domain invariants)
        │
        ├───────────────────────────────┐
        ▼                               ▼
[ Repository Layer (com.javabank.repository) ]   [ Analytics Engine (com.javabank.analytics) ]
  ├── Repository.java (Generic CRUD)              ├── AnalyticsService.java (Calculations)
  ├── InMemoryRepository.java (Map cache)         └── ReportGenerator (Statement printer)
  └── FilePersistenceService (Disk CSVs)
```

---

## 🚀 Core Features

1. **Dual Client Architecture**: Accessible either as a **Cloud Web Application** in any browser (mobile or desktop) or as a native **JavaFX Desktop Client**.
2. **User Authentication & Profiles**: Register new customers with customized identity keys or login securely using existing credentials (`CUST-1001`).
3. **Interactive Financial Dashboard**: Summary cards, quick-action grid, recent transactions ledger, and account selector dropdown refreshing calculations in real time.
4. **Transaction Processing**: Real-time cash deposits, cash withdrawals (enforcing positive bounds and overdraft ceilings), and inter-account transfers.
5. **Stream-based Audit History**: Ledger records supporting transaction type filters, minimum/maximum amount thresholds, and date ranges.
6. **Financial Analytics & Interactive Charts**: Donut charts for transaction types, monthly volume bar charts, and cashflow comparison charts powered by Java Stream pipelines and Chart.js.
7. **Temporal Account Snapshots**: Generates true deep-copied, detached historical records of bank accounts at a specific point in time to verify state independence.
8. **Write-Through File Persistence**: Serializes data to local CSV databases with corruption recovery and JVM shutdown hooks.

---

## 🔌 REST API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/auth/login` | Authenticate customer via User ID (e.g. `CUST-1001`) |
| `POST` | `/api/auth/register` | Create a new user profile |
| `GET` | `/api/accounts?userId={id}` | Retrieve all accounts owned by user |
| `POST` | `/api/accounts` | Open a new Savings or Current account |
| `POST` | `/api/transactions/deposit` | Credit cash to an account |
| `POST` | `/api/transactions/withdraw` | Debit cash with overdraft validation |
| `POST` | `/api/transactions/transfer` | Transfer funds between two accounts |
| `GET` | `/api/transactions` | Query transactions with filters (`type`, `minAmount`, `maxAmount`, `startDate`, `endDate`) |
| `GET` | `/api/analytics?userId={id}` | Aggregate stream metrics and monthly chart datasets |
| `POST` | `/api/snapshots` | Capture independent deep-copy account snapshot |
| `GET` | `/api/snapshots` | Retrieve all historical snapshots |
| `GET` | `/api/reports/statement` | Format and render text account statement |
| `GET` | `/api/reports/vault` | System vault reserve summary |

---

## ⚙️ How to Build and Run Locally

### Prerequisites
* **Java Development Kit (JDK) 17** or higher.
* **Apache Maven 3.8+** (or use the included `./mvnw.cmd` wrapper).

### 1. Running the Web Application (Recommended)
To run the Spring Boot Web application and access the web dashboard:
```powershell
cmd /c mvnw.cmd spring-boot:run
```
Once booted, open your browser and visit:
👉 **`http://localhost:8080`**

### 2. Running the JavaFX Desktop Client
To run the standalone desktop GUI window:
```powershell
cmd /c mvnw.cmd javafx:run
```
*(Or double-click the included `run.bat` file in Windows Explorer)*

### 3. Running Unit Tests
To execute all 12 JUnit 5 test cases:
```powershell
cmd /c mvnw.cmd test
```

### 4. Running via Docker Container
```bash
docker build -t javabank:latest .
docker run -p 8080:8080 javabank:latest
```

---

## 💎 Core Java & OOP Concepts Demonstrated

* **Abstraction & Inheritance**: Polymorphic account behaviors in `SavingsAccount` (non-negative balances) and `CurrentAccount` (overdraft protection).
* **Generic Repository Pattern**: Generic CRUD interface `Repository<T, ID>` implemented with `ConcurrentHashMap` for thread-safe $O(1)$ operations.
* **Domain Exception Boundaries**: Custom checked exceptions (`BankException`, `InsufficientBalanceException`, `AccountNotFoundException`) mapped to REST status codes.
* **Functional Programming & Stream API**: Primitive mapping streams (`mapToDouble().sum()`) avoiding autoboxing overhead during financial aggregations.
* **Deep Copying & Cyclic Graphs**: Custom copy constructors in `User` and `Account` preventing reference cycle memory leaks.
* **Cloud Containerization**: Multi-stage Docker packaging with minimal JRE runtime and dynamic `$PORT` binding.
