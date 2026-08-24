# JavaBank

[![Java Version](https://img.shields.io/badge/Java-17%2B-orange.svg)](https://www.oracle.com/java/)
[![JavaFX](https://img.shields.io/badge/JavaFX-17-blue.svg)](https://openjfx.io/)
[![JUnit 5](https://img.shields.io/badge/JUnit-5-red.svg)](https://junit.org/junit5/)
[![Build Tool](https://img.shields.io/badge/Maven-3.8%2B-green.svg)](https://maven.apache.org/)

JavaBank is a professional, secure desktop retail banking application built from the ground up using **Core Java 17** and **JavaFX**. It features a robust, multi-layered architecture completely decoupled from external frameworks or database engines, demonstrating textbook implementations of advanced object-oriented programming, generic repositories, custom checked exceptions, functional interfaces, primitive mapping streams, and transactional commands.

---

## 🏛️ Architectural Overview

JavaBank is built around a strict separation of concerns, ensuring high maintainability and testability:

```
[ Presentation Layer (com.javabank.ui) ]
  ├── BankingApp.java (JavaFX Stage, VBox sidebar, GridPanes, charts)
  └── styles.css (Modern dark-slate interface elements, layout cards)
        │
        ▼ (Calls facade APIs, intercepts & displays exceptions)
[ Business Service Layer (com.javabank.service) ]
  ├── BankService.java / BankServiceImpl.java (Coordinates core actions)
  └── Exception Boundaries (Converts exceptions to UI alerts)
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

1. **User Authentication & Profiles**: Register new customers with customized identity keys or login securely using existing credentials.
2. **Interactive Financial Dashboard**: Features summary cards, a quick-action grid, a list of the 5 most recent transactions, and an account drop-down selector that refreshes all calculations on the fly.
3. **Transaction Forms**: Perform real-time deposits, cash withdrawals, and inter-account transfers. Rejects transactions matching invalid bounds (e.g. transfers to oneself, negative deposits).
4. **Audit History Logs**: Custom-filterable ledger records table supporting transaction type filters, minimum/maximum amount thresholds, and DatePicker ranges.
5. **Stream-based Analytics**: Interactive charts (assets split PieChart, monthly summaries BarChart, and comparative cashflow BarChart) populated via optimized Stream pipelines.
6. **Temporal Account Snapshots**: Generates true deep-copied, detached historical records of bank accounts at a specific point in time to verify state independence.
7. **Write-Through File Persistence**: Serializes data to local CSV databases. Features corruption recovery (skips corrupted rows and logs warnings) and registers a JVM shutdown hook to commit caches upon program exit.

---

## 🛠️ Tech Stack & Dependencies

* **Core Platform**: Java 17+ (JDK 26 support included)
* **GUI Toolkit**: JavaFX 17 (Controls, Graphics, FXML)
* **Testing Library**: JUnit 5 (Jupyter)
* **Build System**: Apache Maven 3.8+

---

## 💎 Core Java & OOP Concepts Demonstrated

### 1. Object-Oriented Design (OOP)
* **Abstraction**: Enforced via abstract class `Account`, which isolates core state variables and defines abstract operations (like `withdraw()`) overridden by concrete implementations.
* **Encapsulation**: Fields are private and final where applicable. State changes are directed through strict validation methods (e.g. balance adjustments).
* **Inheritance & Polymorphism**: `SavingsAccount` (prevents negative balances) and `CurrentAccount` (enforces configurable overdraft limits) extend `Account` to override withdrawals polymorphically.
* **Method Overloading**: Overloaded account factories (custom limits vs default parameters) and transaction methods.

### 2. Generics & Collections
* **Generic Repository**: Declares `Repository<T, ID>` defining generic CRUD contracts, implemented by the base `InMemoryRepository<T, ID>` class.
* **Collections Mapping**: Uses `ConcurrentHashMap` in generic memory repositories for thread-safe $O(1)$ key-based lookups, and `ArrayList` for transaction ledgers where order of insertion is critical.

### 3. Exception Handling
* **Checked Exceptions**: Custom checked boundary classes (`BankException`, `InsufficientBalanceException`, `AccountNotFoundException`, etc.) propagate from core layers. The presentation layer catches them to display error alerts rather than crashing.
* **Resource Management**: Employs **try-with-resources** blocks for all file readers and writers to prevent memory leaks and locked file descriptors.

### 4. Functional Programming & Streams
* **Lambda Expressions**: Implements custom predicates like `TransactionFilter` and operations like `TransactionAction` via clean anonymous closures.
* **Stream API & Primitives**: Optimizes mathematical sum aggregates (deposits, withdrawals, transfers) using primitive mapping streams (`mapToDouble().sum()`) to eliminate autoboxing overhead.
* **Comparators**: Chronologically sorts and limits transaction arrays (`sorted()`, `limit()`) for tables.

### 5. Advanced Cloning & Cyclic Graphs
* **Deep Copying**: Implements custom **Copy Constructors** rather than relying on flawed `Cloneable` / `Object.clone()` setups.
* **Cyclic Re-binding**: Avoids copy loop cycles in `User.deepCopy()` by passing the new user reference (`account.copy(newOwner)`) to re-bind the final owner reference securely.

---

## 📂 Project Structure

```
java-banking/
├── pom.xml
├── data/
│   ├── users.csv
│   ├── accounts.csv
│   └── transactions.csv
└── src/
    ├── main/
    │   ├── java/
    │   │   └── com/
    │   │       └── javabank/
    │   │           ├── Main.java
    │   │           ├── model/
    │   │           │   ├── Account.java
    │   │           │   ├── SavingsAccount.java
    │   │           │   ├── CurrentAccount.java
    │   │           │   ├── User.java
    │   │           │   ├── Transaction.java
    │   │           │   └── ... (Enums, Status)
    │   │           ├── repository/
    │   │           │   ├── Repository.java
    │   │           │   ├── InMemoryRepository.java
    │   │           │   ├── FilePersistenceService.java
    │   │           │   └── ... (Sub-interfaces)
    │   │           ├── service/
    │   │           │   ├── BankService.java
    │   │           │   └── BankServiceImpl.java
    │   │           ├── transaction/
    │   │           │   ├── TransactionProcessor.java
    │   │           │   ├── DepositProcessor.java
    │   │           │   └── ... (Withdrawal, Transfer)
    │   │           ├── analytics/
    │   │           │   ├── AnalyticsService.java
    │   │           │   └── AnalyticsServiceImpl.java
    │   │           ├── exception/
    │   │           │   └── ... (Custom exceptions)
    │   │           ├── util/
    │   │           │   └── ... (Formatters, ID generators)
    │   │           └── ui/
    │   │               └── BankingApp.java
    │   └── resources/
    │       └── styles.css
    └── test/
        └── java/
            └── com/
                └── javabank/
                    └── BankingServiceTest.java
```

---

## 🖥️ Screenshots (Placeholders)

*Coming soon - visual assets of the client dashboard:*

| Login Screen | Dashboard & Charts |
|:---:|:---:|
| `[Screenshot Placeholder: Login Panel]` | `[Screenshot Placeholder: Dashboard view]` |

| Audit Logs & Filters | Account Snapshots |
|:---:|:---:|
| `[Screenshot Placeholder: Transaction History Table]` | `[Screenshot Placeholder: Copyable Snapshot comparison]` |

---

## ⚙️ How to Build and Run

### Prerequisites
1. Install **Java Development Kit (JDK) 17** or higher.
2. Install **Apache Maven 3.8+**.

### Compilation & Unit Testing
To compile source code and execute all JUnit 5 test cases, run:
```bash
mvn clean test
```

### Launching the JavaFX Application
To boot the desktop application client, run the Maven exec goal:
```bash
mvn javafx:run
```

---

## 📈 Learning Outcomes & Portfolio Highlights

Developing JavaBank yielded several critical backend design insights:
* **Decoupling presentation from logic**: Designing the application as a strict service-facade pattern ensures that the business layer remains testable and reusable, even if migrating to a web client (e.g. Spring Boot or JAX-RS).
* **Avoiding Cloneable pitfalls**: Learned why `Cloneable` fails to deep-copy graph structures when `final` fields are present, and how to safely implement custom copy constructors.
* **Stream Optimization**: Discovered the heap performance benefits of primitive streams (`DoubleStream` / `mapToDouble`) over boxed wrapper object reductions.
* **Concurrency Safety**: Gained experience protecting in-memory collection nodes (`ArrayList`) in multithreaded platforms by synchronizing critical registration methods.
