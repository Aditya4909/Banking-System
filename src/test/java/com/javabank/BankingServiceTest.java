package com.javabank;

import com.javabank.analytics.AnalyticsService;
import com.javabank.analytics.AnalyticsServiceImpl;
import com.javabank.analytics.ReportGenerator;
import com.javabank.exception.*;
import com.javabank.model.*;
import com.javabank.repository.*;
import com.javabank.service.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class BankingServiceTest {

    private UserRepository userRepository;
    private AccountRepository accountRepository;
    private TransactionRepository transactionRepository;
    private BankService bankService;
    private AnalyticsService analyticsService;
    private String testDataDir;

    @BeforeEach
    public void setUp(@TempDir Path tempDir) throws Exception {
        testDataDir = tempDir.toString();
        userRepository = new UserRepositoryImpl();
        accountRepository = new InMemoryAccountRepository();
        transactionRepository = new InMemoryTransactionRepository();
        PersistenceService persistenceService = new FilePersistenceService(testDataDir);
        
        bankService = new BankServiceImpl(userRepository, accountRepository, transactionRepository, persistenceService);
        analyticsService = new AnalyticsServiceImpl();
    }

    // 1. Account Creation Test
    @Test
    public void testAccountCreation() throws Exception {
        User user = new User("TEST-01", "Alice", "alice@test.com");
        bankService.createUser(user);

        Account savings = bankService.createAccount("TEST-01", "Savings", 1000.0, 0.03);
        assertNotNull(savings);
        assertEquals(1000.0, savings.getBalance());
        assertEquals("Savings", savings.getAccountType());
        assertEquals(0.03, ((SavingsAccount) savings).getInterestRate());

        Account current = bankService.createAccount("TEST-01", "Current", 500.0);
        assertNotNull(current);
        assertEquals(500.0, current.getBalance());
        assertEquals("Current", current.getAccountType());
        assertEquals(500.0, ((CurrentAccount) current).getOverdraftLimit());
    }

    // 2. Deposit Test
    @Test
    public void testDeposit() throws Exception {
        User user = new User("TEST-02", "Bob", "bob@test.com");
        bankService.createUser(user);
        Account account = bankService.createAccount("TEST-02", "Savings", 200.0);

        bankService.deposit(account.getAccountNumber(), 150.0, "Cash deposit");
        assertEquals(350.0, bankService.getAccountBalance(account.getAccountNumber()));
    }

    // 3. Withdrawal Test
    @Test
    public void testWithdrawal() throws Exception {
        User user = new User("TEST-03", "Charlie", "charlie@test.com");
        bankService.createUser(user);
        
        Account savings = bankService.createAccount("TEST-03", "Savings", 500.0);
        bankService.withdraw(savings.getAccountNumber(), 100.0, "ATM withdrawal");
        assertEquals(400.0, savings.getBalance());

        Account current = bankService.createAccount("TEST-03", "Current", 200.0, 300.0);
        bankService.withdraw(current.getAccountNumber(), 400.0, "Rent check");
        assertEquals(-200.0, current.getBalance());
    }

    // 4. Transfer Test
    @Test
    public void testTransfer() throws Exception {
        User u1 = new User("TEST-04A", "David", "david@test.com");
        User u2 = new User("TEST-04B", "Emily", "emily@test.com");
        bankService.createUser(u1);
        bankService.createUser(u2);

        Account acc1 = bankService.createAccount("TEST-04A", "Savings", 1000.0);
        Account acc2 = bankService.createAccount("TEST-04B", "Current", 200.0);

        bankService.transfer(acc1.getAccountNumber(), acc2.getAccountNumber(), 300.0, "Payment for dinner");
        assertEquals(700.0, acc1.getBalance());
        assertEquals(500.0, acc2.getBalance());
    }

    // 5. Insufficient Balance Test
    @Test
    public void testInsufficientBalance() throws Exception {
        User user = new User("TEST-05", "Frank", "frank@test.com");
        bankService.createUser(user);

        Account savings = bankService.createAccount("TEST-05", "Savings", 100.0);
        assertThrows(InsufficientBalanceException.class, () -> {
            bankService.withdraw(savings.getAccountNumber(), 150.0, "Over-withdrawal");
        });

        Account current = bankService.createAccount("TEST-05", "Current", 50.0, 100.0);
        assertThrows(InsufficientBalanceException.class, () -> {
            bankService.withdraw(current.getAccountNumber(), 200.0, "Exceed overdraft limit");
        });
    }

    // 6. Invalid Amount Test
    @Test
    public void testInvalidAmount() throws Exception {
        User user = new User("TEST-06", "Grace", "grace@test.com");
        bankService.createUser(user);
        Account account = bankService.createAccount("TEST-06", "Savings", 100.0);

        assertThrows(InvalidAmountException.class, () -> {
            bankService.deposit(account.getAccountNumber(), -50.0, "Invalid deposit");
        });

        assertThrows(InvalidAmountException.class, () -> {
            bankService.deposit(account.getAccountNumber(), 0.0, "Zero deposit");
        });

        assertThrows(InvalidAmountException.class, () -> {
            bankService.withdraw(account.getAccountNumber(), -20.0, "Invalid withdrawal");
        });
    }

    // 7. Duplicate Account Test
    @Test
    public void testDuplicateAccount() throws Exception {
        User user = new User("TEST-07", "Hannah", "hannah@test.com");
        bankService.createUser(user);

        assertThrows(BankException.class, () -> {
            bankService.createUser(user);
        });
    }

    // 8. Account Lookup Test
    @Test
    public void testAccountLookup() throws Exception {
        User user = new User("TEST-08", "Ivan", "ivan@test.com");
        bankService.createUser(user);
        Account account = bankService.createAccount("TEST-08", "Savings", 300.0);

        Account found = bankService.findAccount(account.getAccountNumber());
        assertNotNull(found);
        assertEquals(account.getAccountNumber(), found.getAccountNumber());

        assertThrows(AccountNotFoundException.class, () -> {
            bankService.findAccount("NON-EXISTENT");
        });
    }

    // 9. Transaction Creation Test
    @Test
    public void testTransactionCreation() throws Exception {
        User user = new User("TEST-09", "Jack", "jack@test.com");
        bankService.createUser(user);
        Account account = bankService.createAccount("TEST-09", "Current", 500.0);

        bankService.deposit(account.getAccountNumber(), 100.0, "Deposit log entry");
        bankService.withdraw(account.getAccountNumber(), 50.0, "Withdrawal log entry");

        List<Transaction> history = bankService.getTransactionHistory(account.getAccountNumber());
        assertEquals(2, history.size());
        
        assertEquals(TransactionType.DEPOSIT, history.get(0).getType());
        assertEquals(100.0, history.get(0).getAmount());

        assertEquals(TransactionType.WITHDRAWAL, history.get(1).getType());
        assertEquals(50.0, history.get(1).getAmount());
    }

    // 10. Analytics Calculations Test
    @Test
    public void testAnalyticsCalculations() throws Exception {
        User user = new User("TEST-10", "Katie", "katie@test.com");
        bankService.createUser(user);
        Account current = bankService.createAccount("TEST-10", "Current", 1000.0);
        Account savings = bankService.createAccount("TEST-10", "Savings", 2000.0);

        bankService.deposit(current.getAccountNumber(), 500.0, "Deposit 1");
        bankService.withdraw(current.getAccountNumber(), 200.0, "ATM withdrawal");
        bankService.transfer(savings.getAccountNumber(), current.getAccountNumber(), 1500.0, "Rent contribution");

        List<Transaction> txs = bankService.getTransactionHistory(current.getAccountNumber());

        assertEquals(500.0, analyticsService.calculateTotalDepositedAmount(txs));
        assertEquals(200.0, analyticsService.calculateTotalWithdrawnAmount(txs));
        assertEquals(1500.0, analyticsService.calculateTotalTransferredAmount(txs));

        Optional<Transaction> largest = analyticsService.findLargestTransaction(txs);
        assertTrue(largest.isPresent());
        assertEquals(1500.0, largest.get().getAmount());

        Optional<Transaction> smallest = analyticsService.findSmallestTransaction(txs);
        assertTrue(smallest.isPresent());
        assertEquals(200.0, smallest.get().getAmount());

        assertEquals(733.33, analyticsService.calculateAverageTransactionAmount(txs), 0.01);
        assertEquals(3, analyticsService.countTransactions(txs));

        Map<TransactionType, Long> counts = analyticsService.getTransactionCountsByType(txs);
        assertEquals(1L, counts.get(TransactionType.DEPOSIT));
        assertEquals(1L, counts.get(TransactionType.WITHDRAWAL));
        assertEquals(1L, counts.get(TransactionType.TRANSFER));
    }

    // 11. Deep Copy / Snapshot Independence Test
    @Test
    public void testDeepCopyAndSnapshotIndependence() throws Exception {
        User user = new User("TEST-11", "Luke", "luke@test.com");
        bankService.createUser(user);
        Account original = bankService.createAccount("TEST-11", "Current", 500.0);
        bankService.deposit(original.getAccountNumber(), 100.0, "Initial transaction");

        // Take snapshot
        Account snapshot = bankService.createAccountSnapshot(original.getAccountNumber());

        assertEquals(original.getAccountNumber(), snapshot.getAccountNumber());
        assertEquals(original.getBalance(), snapshot.getBalance());
        assertEquals(original.getTransactions().size(), snapshot.getTransactions().size());

        bankService.deposit(original.getAccountNumber(), 250.0, "Subsequent deposit");
        bankService.withdraw(original.getAccountNumber(), 850.0, "Clean out account balance");
        bankService.closeAccount(original.getAccountNumber());

        assertEquals(0.0, original.getBalance());
        assertEquals(AccountStatus.CLOSED, original.getAccountStatus());
        assertEquals(3, original.getTransactions().size());

        assertEquals(600.0, snapshot.getBalance());
        assertEquals(AccountStatus.ACTIVE, snapshot.getAccountStatus());
        assertEquals(1, snapshot.getTransactions().size());

        assertNotSame(original.getOwner(), snapshot.getOwner());
        assertEquals("Luke", snapshot.getOwner().getName());
    }

    // 12. Report Statement Verification
    @Test
    public void testReportGeneration() throws Exception {
        User user = new User("TEST-12", "Report User", "report@test.com");
        bankService.createUser(user);
        Account current = bankService.createAccount("TEST-12", "Current", 1500.0);

        List<Account> list = List.of(current);

        // Verification of report formatting statements
        String statement = analyticsService.generateAccountStatement(current);
        String summary = analyticsService.generateVaultSummary(list);

        assertNotNull(statement);
        assertTrue(statement.contains("ACCOUNT STATEMENT"));
        assertTrue(statement.contains(current.getAccountNumber()));

        assertNotNull(summary);
        assertTrue(summary.contains("SYSTEM VAULT SUMMARY"));
        assertTrue(summary.contains("$1,500.00"));
    }
}
