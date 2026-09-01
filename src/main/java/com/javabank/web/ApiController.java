package com.javabank.web;

import com.javabank.analytics.AnalyticsService;
import com.javabank.exception.*;
import com.javabank.model.*;
import com.javabank.service.BankService;
import com.javabank.util.IdGenerator;
import com.javabank.web.dto.AccountDTO;
import com.javabank.web.dto.TransactionDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Main REST Controller for JavaBank Web Application.
 * Uses immutable DTO records to guarantee lightweight, recursion-free JSON payloads.
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ApiController {

    private final BankService bankService;
    private final AnalyticsService analyticsService;
    private final List<AccountDTO> snapshotRegistry = new CopyOnWriteArrayList<>();

    public ApiController(BankService bankService, AnalyticsService analyticsService) {
        this.bankService = bankService;
        this.analyticsService = analyticsService;
    }

    // ==========================================
    // 1. AUTHENTICATION & USER PROFILE
    // ==========================================
    @PostMapping("/auth/login")
    public ResponseEntity<Map<String, Object>> login(@RequestBody Map<String, String> payload) throws Exception {
        String userId = payload.get("userId");
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User ID is required.");
        }
        User user = bankService.getUser(userId.trim());
        List<AccountDTO> accounts = getUserAccounts(user.getUserId());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("userId", user.getUserId());
        response.put("name", user.getName());
        response.put("email", user.getEmail());
        response.put("accounts", accounts);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/auth/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, String> payload) throws Exception {
        String name = payload.get("name");
        String email = payload.get("email");
        if (name == null || name.isBlank() || email == null || email.isBlank()) {
            throw new IllegalArgumentException("Name and email are required to create a profile.");
        }
        String newId = IdGenerator.generateCustomerId();
        User user = new User(newId, name.trim(), email.trim());
        bankService.createUser(user);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("userId", newId);
        response.put("name", user.getName());
        response.put("email", user.getEmail());
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // 2. ACCOUNT MANAGEMENT
    // ==========================================
    @GetMapping("/accounts")
    public ResponseEntity<List<AccountDTO>> getAccounts(@RequestParam String userId) throws Exception {
        return ResponseEntity.ok(getUserAccounts(userId));
    }

    @GetMapping("/accounts/{accountNumber}")
    public ResponseEntity<AccountDTO> getAccount(@PathVariable String accountNumber) throws Exception {
        Account account = bankService.findAccount(accountNumber);
        return ResponseEntity.ok(AccountDTO.from(account));
    }

    @PostMapping("/accounts")
    public ResponseEntity<AccountDTO> createAccount(@RequestBody Map<String, Object> payload) throws Exception {
        String userId = (String) payload.get("userId");
        String accountType = (String) payload.get("accountType");
        double initialBalance = payload.containsKey("initialBalance") 
                ? Double.parseDouble(payload.get("initialBalance").toString()) : 0.0;

        Account account;
        if (payload.containsKey("extraParam") && payload.get("extraParam") != null && !payload.get("extraParam").toString().isBlank()) {
            double extraParam = Double.parseDouble(payload.get("extraParam").toString());
            account = bankService.createAccount(userId, accountType, initialBalance, extraParam);
        } else {
            account = bankService.createAccount(userId, accountType, initialBalance);
        }
        return ResponseEntity.ok(AccountDTO.from(account));
    }

    // ==========================================
    // 3. TRANSACTIONS (DEPOSIT, WITHDRAW, TRANSFER)
    // ==========================================
    @PostMapping("/transactions/deposit")
    public ResponseEntity<Map<String, Object>> deposit(@RequestBody Map<String, Object> payload) throws Exception {
        String accountNumber = (String) payload.get("accountNumber");
        double amount = Double.parseDouble(payload.get("amount").toString());
        String description = (String) payload.getOrDefault("description", "Cash Deposit");

        bankService.deposit(accountNumber, amount, description);
        double newBalance = bankService.getAccountBalance(accountNumber);

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Successfully deposited $" + String.format("%.2f", amount));
        res.put("newBalance", newBalance);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/transactions/withdraw")
    public ResponseEntity<Map<String, Object>> withdraw(@RequestBody Map<String, Object> payload) throws Exception {
        String accountNumber = (String) payload.get("accountNumber");
        double amount = Double.parseDouble(payload.get("amount").toString());
        String description = (String) payload.getOrDefault("description", "Cash Withdrawal");

        bankService.withdraw(accountNumber, amount, description);
        double newBalance = bankService.getAccountBalance(accountNumber);

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Successfully withdrew $" + String.format("%.2f", amount));
        res.put("newBalance", newBalance);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/transactions/transfer")
    public ResponseEntity<Map<String, Object>> transfer(@RequestBody Map<String, Object> payload) throws Exception {
        String source = (String) payload.get("sourceAccountNumber");
        String destination = (String) payload.get("destinationAccountNumber");
        double amount = Double.parseDouble(payload.get("amount").toString());
        String description = (String) payload.getOrDefault("description", "Fund Transfer");

        bankService.transfer(source, destination, amount, description);
        double newBalance = bankService.getAccountBalance(source);

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", "Successfully transferred $" + String.format("%.2f", amount) + " to " + destination);
        res.put("newBalance", newBalance);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/transactions")
    public ResponseEntity<List<TransactionDTO>> getTransactions(
            @RequestParam String accountNumber,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Double minAmount,
            @RequestParam(required = false) Double maxAmount,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) throws Exception {
        TransactionType txType = (type != null && !type.isBlank() && !"ALL".equalsIgnoreCase(type))
                ? TransactionType.valueOf(type.toUpperCase()) : null;

        LocalDateTime start = (startDate != null && !startDate.isBlank()) 
                ? LocalDate.parse(startDate).atStartOfDay() : null;

        LocalDateTime end = (endDate != null && !endDate.isBlank()) 
                ? LocalDate.parse(endDate).atTime(LocalTime.MAX) : null;

        List<Transaction> transactions = bankService.getFilteredTransactions(
                accountNumber, txType, minAmount, maxAmount, start, end
        );

        List<TransactionDTO> dtos = transactions.stream()
                .map(TransactionDTO::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // ==========================================
    // 4. FINANCIAL ANALYTICS & CHARTS DATA
    // ==========================================
    @GetMapping("/analytics")
    public ResponseEntity<Map<String, Object>> getAnalytics(@RequestParam String userId) throws Exception {
        List<Account> accounts = bankService.getAllAccounts().stream()
                .filter(acc -> acc.getOwner().getUserId().equals(userId))
                .collect(Collectors.toList());

        List<Transaction> allTransactions = new ArrayList<>();
        for (Account a : accounts) {
            allTransactions.addAll(bankService.getTransactionHistory(a.getAccountNumber()));
        }

        double totalDeposits = analyticsService.calculateTotalDepositedAmount(allTransactions);
        double totalWithdrawals = analyticsService.calculateTotalWithdrawnAmount(allTransactions);
        double totalTransfers = analyticsService.calculateTotalTransferredAmount(allTransactions);
        double avgTransaction = analyticsService.calculateAverageTransactionAmount(allTransactions);
        long count = analyticsService.countTransactions(allTransactions);
        Optional<Transaction> largest = analyticsService.findLargestTransaction(allTransactions);

        Map<TransactionType, Long> typeCountsEnum = analyticsService.getTransactionCountsByType(allTransactions);
        Map<String, Long> typeCounts = new LinkedHashMap<>();
        for (Map.Entry<TransactionType, Long> e : typeCountsEnum.entrySet()) {
            typeCounts.put(e.getKey().name(), e.getValue());
        }

        Map<Month, Double> monthlySummaryEnum = analyticsService.calculateMonthlyTransactionSummary(allTransactions);
        Map<String, Double> monthlySummary = new LinkedHashMap<>();
        for (Map.Entry<Month, Double> e : monthlySummaryEnum.entrySet()) {
            monthlySummary.put(e.getKey().name(), e.getValue());
        }

        Map<String, Object> analytics = new HashMap<>();
        analytics.put("totalDeposits", totalDeposits);
        analytics.put("totalWithdrawals", totalWithdrawals);
        analytics.put("totalTransfers", totalTransfers);
        analytics.put("avgTransaction", avgTransaction);
        analytics.put("count", count);
        analytics.put("largestTransactionAmount", largest.map(Transaction::getAmount).orElse(0.0));
        analytics.put("typeCounts", typeCounts);
        analytics.put("monthlySummary", monthlySummary);

        return ResponseEntity.ok(analytics);
    }

    // ==========================================
    // 5. ACCOUNT SNAPSHOTS (DEEP COPY DEMO)
    // ==========================================
    @PostMapping("/snapshots")
    public ResponseEntity<AccountDTO> createSnapshot(@RequestBody Map<String, String> payload) throws Exception {
        String accountNumber = payload.get("accountNumber");
        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("Account number is required.");
        }
        Account snapshot = bankService.createAccountSnapshot(accountNumber);
        AccountDTO dto = AccountDTO.from(snapshot);
        snapshotRegistry.add(dto);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/snapshots")
    public ResponseEntity<List<AccountDTO>> getSnapshots() {
        return ResponseEntity.ok(snapshotRegistry);
    }

    // ==========================================
    // 6. STATEMENT & VAULT SUMMARY REPORTS
    // ==========================================
    @GetMapping("/reports/statement")
    public ResponseEntity<Map<String, String>> getStatement(@RequestParam String accountNumber) throws Exception {
        Account account = bankService.findAccount(accountNumber);
        String statement = analyticsService.generateAccountStatement(account);
        return ResponseEntity.ok(Map.of("statement", statement));
    }

    @GetMapping("/reports/vault")
    public ResponseEntity<Map<String, String>> getVaultSummary() throws Exception {
        List<Account> allAccounts = bankService.getAllAccounts();
        String summary = analyticsService.generateVaultSummary(allAccounts);
        return ResponseEntity.ok(Map.of("summary", summary));
    }

    // Helper
    private List<AccountDTO> getUserAccounts(String userId) throws Exception {
        return bankService.getAllAccounts().stream()
                .filter(acc -> acc.getOwner().getUserId().equals(userId))
                .map(AccountDTO::from)
                .collect(Collectors.toList());
    }
}
