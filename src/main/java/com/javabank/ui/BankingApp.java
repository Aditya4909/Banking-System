package com.javabank.ui;

import com.javabank.analytics.AnalyticsService;
import com.javabank.analytics.AnalyticsServiceImpl;
import com.javabank.exception.*;
import com.javabank.model.*;
import com.javabank.repository.*;
import com.javabank.service.*;
import com.javabank.util.CurrencyFormatter;
import com.javabank.util.IdGenerator;
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Modern JavaFX Desktop Application for JavaBank.
 * Handles the complete user interface, layout screens, inputs, and events binding.
 */
public class BankingApp extends Application {
    private static BankService bankService;
    private static AnalyticsService analyticsService;
    
    // Caching pool demonstrating Garbage Collection concepts
    private static final Map<String, WeakReference<User>> userSessionCache = new ConcurrentHashMap<>();
    
    // Snapshot cache for demonstration in UI session
    private static final List<Account> snapshotCache = new ArrayList<>();

    private User currentUser;
    private BorderPane rootLayout;
    private StackPane contentArea;
    private VBox sidebar;
    private Label notificationLabel;

    // Sidebar navigation buttons references for quick actions redirect
    private Button dashBtn;
    private Button accountsBtn;
    private Button depositBtn;
    private Button withdrawBtn;
    private Button transferBtn;
    private Button historyBtn;
    private Button analyticsBtn;
    private Button snapshotBtn;
    private Button profileBtn;

    @Override
    public void init() throws Exception {
        String dataDir = "data";
        try {
            UserRepository userRepository = new UserRepositoryImpl();
            // Refactored: using clean in-memory caches coupled with FilePersistenceService
            AccountRepository accountRepository = new InMemoryAccountRepository();
            TransactionRepository transactionRepository = new InMemoryTransactionRepository();
            PersistenceService persistenceService = new FilePersistenceService(dataDir);
            
            bankService = new BankServiceImpl(userRepository, accountRepository, transactionRepository, persistenceService);
            analyticsService = new AnalyticsServiceImpl();
            
            // Seed a customer for demonstration
            User demoUser = new User("CUST-1001", "John Doe", "john.doe@example.com");
            try {
                bankService.createUser(demoUser);
                bankService.createAccount("CUST-1001", "Savings", 5000.0, 0.025); // 2.5%
                bankService.createAccount("CUST-1001", "Current", 1500.0, 800.0);  // $800 overdraft
            } catch (Exception e) {
                // If already exists, ignore seeding warning
            }
            
            userSessionCache.put(demoUser.getUserId(), new WeakReference<>(demoUser));
        } catch (IOException e) {
            System.err.println("Fatal: Failed to initialize file repositories.");
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    public void start(Stage stage) {
        rootLayout = new BorderPane();
        contentArea = new StackPane();
        contentArea.getStyleClass().add("content-area");

        // Top alert notification panel
        notificationLabel = new Label();
        notificationLabel.setVisible(false);
        notificationLabel.setManaged(false);
        notificationLabel.setMaxWidth(Double.MAX_VALUE);
        notificationLabel.setAlignment(Pos.CENTER);

        VBox centerLayout = new VBox(10, notificationLabel, contentArea);
        VBox.setVgrow(contentArea, Priority.ALWAYS);
        rootLayout.setCenter(centerLayout);

        showLoginScreen();

        Scene scene = new Scene(rootLayout, 1140, 780);
        loadStylesheet(scene);

        stage.setTitle("JavaBank Desktop Client - Core Banking");
        stage.setScene(scene);
        stage.show();
    }

    private void loadStylesheet(Scene scene) {
        try {
            scene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        } catch (Exception e) {
            try {
                scene.getStylesheets().add(new File("src/main/resources/styles.css").toURI().toURL().toExternalForm());
            } catch (Exception ex) {
                System.err.println("Style warning: Stylesheet could not be resolved.");
            }
        }
    }

    private void showNotification(String message, boolean isSuccess) {
        notificationLabel.setText(message);
        notificationLabel.getStyleClass().removeAll("notification-banner-success", "notification-banner-error");
        if (isSuccess) {
            notificationLabel.getStyleClass().add("notification-banner-success");
        } else {
            notificationLabel.getStyleClass().add("notification-banner-error");
        }
        notificationLabel.setManaged(true);
        notificationLabel.setVisible(true);

        new Thread(() -> {
            try {
                Thread.sleep(6000);
            } catch (InterruptedException e) {
                // Ignore
            }
            javafx.application.Platform.runLater(() -> {
                notificationLabel.setVisible(false);
                notificationLabel.setManaged(false);
            });
        }).start();
    }

    private void showErrorAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void showSuccessAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    /* =========================================================================
     * SCREEN 1: LOGIN & REGISTRATION
     * ========================================================================= */
    private void showLoginScreen() {
        currentUser = null;
        rootLayout.setLeft(null);

        VBox loginBox = new VBox(25);
        loginBox.getStyleClass().add("login-card");

        Label appTitle = new Label("JavaBank");
        appTitle.setStyle("-fx-font-size: 32px; -fx-text-fill: #58a6ff; -fx-font-weight: bold;");

        Label subtitle = new Label("Secure Desktop Banking System");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: #8b949e;");

        VBox fields = new VBox(15);
        fields.setAlignment(Pos.CENTER_LEFT);

        Label idLabel = new Label("User Access ID:");
        idLabel.getStyleClass().add("label-form");
        TextField idField = new TextField();
        idField.setPromptText("Enter your ID (e.g. CUST-1001)");
        idField.getStyleClass().add("text-field-custom");

        fields.getChildren().addAll(idLabel, idField);

        Button loginBtn = new Button("Secure Sign In");
        loginBtn.getStyleClass().add("button-primary");
        loginBtn.setMaxWidth(Double.MAX_VALUE);

        Button registerLink = new Button("Create New User ID");
        registerLink.getStyleClass().add("button-secondary");
        registerLink.setMaxWidth(Double.MAX_VALUE);

        loginBox.getChildren().addAll(appTitle, subtitle, fields, loginBtn, registerLink);

        StackPane container = new StackPane(loginBox);
        container.getStyleClass().add("login-bg");
        contentArea.getChildren().setAll(container);

        // Actions
        loginBtn.setOnAction(e -> {
            String uid = idField.getText().trim();
            if (uid.isEmpty()) {
                showErrorAlert("Login Input Error", "The User Access ID field cannot be blank.");
                return;
            }
            try {
                User user = bankService.getUser(uid);
                currentUser = user;
                buildSidebar();
                showDashboard();
                showNotification("Logged in securely as " + user.getName(), true);
            } catch (BankException ex) {
                showErrorAlert("Authentication Refused", "No profile matches Access ID: " + uid);
            } catch (Exception ex) {
                showErrorAlert("System Authentication Error", "Failed to retrieve user: " + ex.getMessage());
            }
        });

        registerLink.setOnAction(e -> showRegistrationScreen());
    }

    private void showRegistrationScreen() {
        VBox regBox = new VBox(20);
        regBox.getStyleClass().add("login-card");

        Label regTitle = new Label("Create User Account");
        regTitle.setStyle("-fx-font-size: 26px; -fx-text-fill: #ffffff; -fx-font-weight: bold;");

        VBox fields = new VBox(12);
        fields.setAlignment(Pos.CENTER_LEFT);

        Label nameLabel = new Label("Full Name:");
        nameLabel.getStyleClass().add("label-form");
        TextField nameField = new TextField();
        nameField.getStyleClass().add("text-field-custom");

        Label emailLabel = new Label("Email Address:");
        emailLabel.getStyleClass().add("label-form");
        TextField emailField = new TextField();
        emailField.getStyleClass().add("text-field-custom");

        fields.getChildren().addAll(nameLabel, nameField, emailLabel, emailField);

        Button signupBtn = new Button("Create User Profile");
        signupBtn.getStyleClass().add("button-primary");
        signupBtn.setMaxWidth(Double.MAX_VALUE);

        Button backBtn = new Button("Back to Sign In");
        backBtn.getStyleClass().add("button-secondary");
        backBtn.setMaxWidth(Double.MAX_VALUE);

        regBox.getChildren().addAll(regTitle, fields, signupBtn, backBtn);

        StackPane container = new StackPane(regBox);
        container.getStyleClass().add("login-bg");
        contentArea.getChildren().setAll(container);

        signupBtn.setOnAction(e -> {
            String name = nameField.getText().trim();
            String email = emailField.getText().trim();
            if (name.isEmpty() || email.isEmpty()) {
                showErrorAlert("Registration Validation Error", "All fields are required to register a profile.");
                return;
            }
            try {
                String newId = IdGenerator.generateCustomerId();
                User user = new User(newId, name, email);
                bankService.createUser(user);
                showSuccessAlert("Registration Successful", "Profile created! Your Access ID key is: " + newId + "\nPlease write this down to sign in.");
                showLoginScreen();
            } catch (Exception ex) {
                showErrorAlert("Registration Refused", ex.getMessage());
            }
        });

        backBtn.setOnAction(e -> showLoginScreen());
    }

    /* =========================================================================
     * SIDEBAR & VIEW SWAPPING NAVIGATION
     * ========================================================================= */
    private void buildSidebar() {
        sidebar = new VBox();
        sidebar.getStyleClass().add("sidebar");

        Label appHeader = new Label("JavaBank Console");
        appHeader.getStyleClass().add("sidebar-title");

        dashBtn = createSidebarBtn("Dashboard");
        accountsBtn = createSidebarBtn("Account Overview");
        depositBtn = createSidebarBtn("Deposit Funds");
        withdrawBtn = createSidebarBtn("Withdraw Funds");
        transferBtn = createSidebarBtn("Transfer Money");
        historyBtn = createSidebarBtn("Transaction History");
        analyticsBtn = createSidebarBtn("Financial Analytics");
        snapshotBtn = createSidebarBtn("Account Snapshots");
        profileBtn = createSidebarBtn("User Profiles");
        Button logoutBtn = createSidebarBtn("Sign Out");
        logoutBtn.setStyle("-fx-text-fill: #ff7b72;");

        sidebar.getChildren().addAll(appHeader, dashBtn, accountsBtn, depositBtn, 
                withdrawBtn, transferBtn, historyBtn, analyticsBtn, snapshotBtn, profileBtn, 
                new Separator(), logoutBtn);
        rootLayout.setLeft(sidebar);

        // Routing
        dashBtn.setOnAction(e -> { setActiveSidebarBtn(dashBtn); showDashboard(); });
        accountsBtn.setOnAction(e -> { setActiveSidebarBtn(accountsBtn); showAccountsOverview(); });
        depositBtn.setOnAction(e -> { setActiveSidebarBtn(depositBtn); showDepositScreen(); });
        withdrawBtn.setOnAction(e -> { setActiveSidebarBtn(withdrawBtn); showWithdrawScreen(); });
        transferBtn.setOnAction(e -> { setActiveSidebarBtn(transferBtn); showTransferScreen(); });
        historyBtn.setOnAction(e -> { setActiveSidebarBtn(historyBtn); showHistoryScreen(); });
        analyticsBtn.setOnAction(e -> { setActiveSidebarBtn(analyticsBtn); showAnalyticsScreen(); });
        snapshotBtn.setOnAction(e -> { setActiveSidebarBtn(snapshotBtn); showSnapshotScreen(); });
        profileBtn.setOnAction(e -> { setActiveSidebarBtn(profileBtn); showProfileScreen(); });
        logoutBtn.setOnAction(e -> showLoginScreen());

        setActiveSidebarBtn(dashBtn);
    }

    private Button createSidebarBtn(String text) {
        Button btn = new Button(text);
        btn.getStyleClass().add("sidebar-button");
        return btn;
    }

    private void setActiveSidebarBtn(Button activeBtn) {
        for (Node node : sidebar.getChildren()) {
            if (node instanceof Button) {
                node.getStyleClass().remove("sidebar-button-active");
            }
        }
        activeBtn.getStyleClass().add("sidebar-button-active");
    }

    private List<Account> getCurrentUserAccounts() {
        try {
            return bankService.getAllAccounts().stream()
                    .filter(a -> a.getOwner().getUserId().equals(currentUser.getUserId()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    /* =========================================================================
     * SCREEN 2: MAIN DASHBOARD DESIGN
     * ========================================================================= */
    private void showDashboard() {
        VBox dashLayout = new VBox(20);
        dashLayout.getStyleClass().add("content-area");

        Label welcomeLabel = new Label("Welcome back, " + currentUser.getName() + "!");
        welcomeLabel.setStyle("-fx-font-size: 24px; -fx-text-fill: #ffffff; -fx-font-weight: bold;");

        HBox selectorBox = new HBox(12);
        selectorBox.setAlignment(Pos.CENTER_LEFT);
        Label selLabel = new Label("Viewing Dashboard for Account:");
        selLabel.getStyleClass().add("label-form");
        
        ComboBox<String> accountSelector = new ComboBox<>();
        accountSelector.getStyleClass().add("combo-box-custom");
        
        List<Account> userAccounts = getCurrentUserAccounts();
        for (Account a : userAccounts) {
            accountSelector.getItems().add(a.getAccountNumber() + " (" + a.getAccountType() + ")");
        }

        selectorBox.getChildren().addAll(selLabel, accountSelector);

        HBox topMetrics = new HBox(15);
        topMetrics.setAlignment(Pos.CENTER_LEFT);

        VBox balanceCard = createMetricCard("CURRENT BALANCE", "$0.00");
        VBox typeCard = createMetricCard("ACCOUNT TYPE", "-");
        VBox statusCard = createMetricCard("ACCOUNT STATUS", "-");

        topMetrics.getChildren().addAll(balanceCard, typeCard, statusCard);

        HBox bottomMetrics = new HBox(15);
        bottomMetrics.setAlignment(Pos.CENTER_LEFT);

        VBox depositSumCard = createMetricCard("TOTAL DEPOSITS", "$0.00");
        VBox withdrawSumCard = createMetricCard("TOTAL WITHDRAWALS", "$0.00");
        VBox transferSumCard = createMetricCard("TOTAL TRANSFERS", "$0.00");

        bottomMetrics.getChildren().addAll(depositSumCard, withdrawSumCard, transferSumCard);

        HBox dashboardBody = new HBox(25);
        dashboardBody.setAlignment(Pos.TOP_LEFT);

        VBox recentTxsPanel = new VBox(10);
        recentTxsPanel.getStyleClass().add("card-panel");
        recentTxsPanel.setPrefWidth(550);

        Label recentTitle = new Label("Recent Activity (Last 5 Transactions)");
        recentTitle.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 16px; -fx-font-weight: bold;");

        TableView<Transaction> recentTable = new TableView<>();
        recentTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        recentTable.setPrefHeight(180);

        TableColumn<Transaction, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getType().name()));
        
        TableColumn<Transaction, String> amtCol = new TableColumn<>("Amount");
        amtCol.setCellValueFactory(cellData -> new SimpleStringProperty(CurrencyFormatter.formatUSD(cellData.getValue().getAmount())));

        TableColumn<Transaction, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDescription()));

        recentTable.getColumns().addAll(typeCol, amtCol, descCol);
        recentTxsPanel.getChildren().addAll(recentTitle, recentTable);

        VBox quickActionsPanel = new VBox(15);
        quickActionsPanel.getStyleClass().add("card-panel");
        quickActionsPanel.setPrefWidth(300);

        Label actionsTitle = new Label("Quick Access");
        actionsTitle.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 16px; -fx-font-weight: bold;");

        GridPane actionGrid = new GridPane();
        actionGrid.setHgap(10);
        actionGrid.setVgap(10);

        Button depBtn = new Button("Deposit");
        depBtn.getStyleClass().add("button-primary");
        depBtn.setPrefWidth(120);
        depBtn.setOnAction(e -> { setActiveSidebarBtn(depositBtn); showDepositScreen(); });

        Button wthBtn = new Button("Withdraw");
        wthBtn.getStyleClass().add("button-secondary");
        wthBtn.setPrefWidth(120);
        wthBtn.setOnAction(e -> { setActiveSidebarBtn(withdrawBtn); showWithdrawScreen(); });

        Button trsfBtn = new Button("Transfer");
        trsfBtn.getStyleClass().add("button-secondary");
        trsfBtn.setPrefWidth(120);
        trsfBtn.setOnAction(e -> { setActiveSidebarBtn(transferBtn); showTransferScreen(); });

        Button txsBtn = new Button("Transactions");
        txsBtn.getStyleClass().add("button-secondary");
        txsBtn.setPrefWidth(120);
        txsBtn.setOnAction(e -> { setActiveSidebarBtn(historyBtn); showHistoryScreen(); });

        Button alytBtn = new Button("Analytics");
        alytBtn.getStyleClass().add("button-secondary");
        alytBtn.setPrefWidth(120);
        alytBtn.setOnAction(e -> { setActiveSidebarBtn(analyticsBtn); showAnalyticsScreen(); });

        Button snapBtn = new Button("Snapshot");
        snapBtn.getStyleClass().add("button-secondary");
        snapBtn.setPrefWidth(120);
        snapBtn.setOnAction(e -> { setActiveSidebarBtn(snapshotBtn); showSnapshotScreen(); });

        actionGrid.add(depBtn, 0, 0);
        actionGrid.add(wthBtn, 1, 0);
        actionGrid.add(trsfBtn, 0, 1);
        actionGrid.add(txsBtn, 1, 1);
        actionGrid.add(alytBtn, 0, 2);
        actionGrid.add(snapBtn, 1, 2);

        quickActionsPanel.getChildren().addAll(actionsTitle, actionGrid);
        dashboardBody.getChildren().addAll(recentTxsPanel, quickActionsPanel);

        accountSelector.setOnAction(e -> {
            String selected = accountSelector.getValue();
            if (selected == null) return;
            String accNum = selected.split(" ")[0];
            try {
                Account acc = bankService.findAccount(accNum);
                
                updateMetricCardValue(balanceCard, CurrencyFormatter.formatUSD(acc.getBalance()));
                updateMetricCardValue(typeCard, acc.getAccountType());
                updateMetricCardValue(statusCard, acc.getAccountStatus().name());

                List<Transaction> txs = bankService.getTransactionHistory(accNum);
                
                double totalDeposited = analyticsService.calculateTotalDepositedAmount(txs);
                double totalWithdrawn = analyticsService.calculateTotalWithdrawnAmount(txs);
                double totalTransferred = analyticsService.calculateTotalTransferredAmount(txs);

                updateMetricCardValue(depositSumCard, CurrencyFormatter.formatUSD(totalDeposited));
                updateMetricCardValue(withdrawSumCard, CurrencyFormatter.formatUSD(totalWithdrawn));
                updateMetricCardValue(transferSumCard, CurrencyFormatter.formatUSD(totalTransferred));

                List<Transaction> recent = analyticsService.sortTransactionsChronologically(txs, false).stream()
                        .limit(5)
                        .collect(Collectors.toList());
                recentTable.setItems(FXCollections.observableArrayList(recent));

            } catch (AccountNotFoundException ex) {
                showErrorAlert("Account Error", "The selected account details could not be found.");
            } catch (Exception ex) {
                showErrorAlert("System Error", "Failed to refresh dashboard stats: " + ex.getMessage());
            }
        });

        if (!userAccounts.isEmpty()) {
            accountSelector.setValue(userAccounts.get(0).getAccountNumber() + " (" + userAccounts.get(0).getAccountType() + ")");
        }

        dashLayout.getChildren().addAll(welcomeLabel, selectorBox, topMetrics, bottomMetrics, dashboardBody);
        contentArea.getChildren().setAll(dashLayout);
    }

    /* =========================================================================
     * SCREEN 3: ACCOUNT OVERVIEW & REGISTRY
     * ========================================================================= */
    private void showAccountsOverview() {
        VBox view = new VBox(20);
        Label title = new Label("My Bank Accounts");
        title.getStyleClass().add("view-title");

        TableView<Account> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Account, String> numberCol = new TableColumn<>("Account Number");
        numberCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getAccountNumber()));

        TableColumn<Account, String> typeCol = new TableColumn<>("Account Type");
        typeCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getAccountType()));

        TableColumn<Account, String> balanceCol = new TableColumn<>("Balance");
        balanceCol.setCellValueFactory(cellData -> new SimpleStringProperty(CurrencyFormatter.formatUSD(cellData.getValue().getBalance())));

        TableColumn<Account, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getAccountStatus().name()));

        TableColumn<Account, String> detailCol = new TableColumn<>("Limits/Interest");
        detailCol.setCellValueFactory(cellData -> {
            Account acc = cellData.getValue();
            if (acc instanceof SavingsAccount) {
                return new SimpleStringProperty("Interest Rate: " + (((SavingsAccount) acc).getInterestRate() * 100) + "%");
            } else if (acc instanceof CurrentAccount) {
                return new SimpleStringProperty("Overdraft: " + CurrencyFormatter.formatUSD(((CurrentAccount) acc).getOverdraftLimit()));
            }
            return new SimpleStringProperty("-");
        });

        table.getColumns().addAll(numberCol, typeCol, balanceCol, statusCol, detailCol);
        table.setItems(FXCollections.observableArrayList(getCurrentUserAccounts()));
        table.setPrefHeight(250);

        VBox form = new VBox(15);
        form.getStyleClass().add("card-panel");
        Label formTitle = new Label("Open New Bank Account");
        formTitle.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 16px; -fx-font-weight: bold;");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);

        Label typeLbl = new Label("Account Class:");
        typeLbl.getStyleClass().add("label-form");
        ComboBox<String> typeBox = new ComboBox<>(FXCollections.observableArrayList("Savings", "Current"));
        typeBox.setValue("Savings");
        typeBox.getStyleClass().add("combo-box-custom");

        Label balLbl = new Label("Initial Deposit ($):");
        balLbl.getStyleClass().add("label-form");
        TextField balField = new TextField();
        balField.getStyleClass().add("text-field-custom");

        Label rateLbl = new Label("Interest / Overdraft ($):");
        rateLbl.getStyleClass().add("label-form");
        TextField rateField = new TextField();
        rateField.getStyleClass().add("text-field-custom");
        rateField.setPromptText("Default applied if empty");

        grid.add(typeLbl, 0, 0);
        grid.add(typeBox, 1, 0);
        grid.add(balLbl, 0, 1);
        grid.add(balField, 1, 1);
        grid.add(rateLbl, 0, 2);
        grid.add(rateField, 1, 2);

        Button openBtn = new Button("Open Account");
        openBtn.getStyleClass().add("button-primary");

        form.getChildren().addAll(formTitle, grid, openBtn);

        openBtn.setOnAction(e -> {
            String accType = typeBox.getValue();
            String balanceStr = balField.getText().trim();
            String rateStr = rateField.getText().trim();

            if (balanceStr.isEmpty()) {
                showErrorAlert("Input Validation Error", "Initial deposit balance field cannot be empty.");
                return;
            }
            try {
                double initialBalance = Double.parseDouble(balanceStr);
                
                if (initialBalance < 0) {
                    showErrorAlert("Input Validation Error", "Initial deposit cannot be negative.");
                    return;
                }

                Account account;
                if (!rateStr.isEmpty()) {
                    double extra = Double.parseDouble(rateStr);
                    if (extra < 0) {
                        showErrorAlert("Input Validation Error", "Interest rate / overdraft limit cannot be negative.");
                        return;
                    }
                    account = bankService.createAccount(currentUser.getUserId(), accType, initialBalance, extra);
                } else {
                    account = bankService.createAccount(currentUser.getUserId(), accType, initialBalance);
                }
                
                showSuccessAlert("Account Registered", "Success! Opened account reference: " + account.getAccountNumber());
                showAccountsOverview();
            } catch (NumberFormatException ex) {
                showErrorAlert("Numeric Input Error", "Please verify numeric quantities. Decimals must use simple dot notation.");
            } catch (DuplicateAccountException ex) {
                showErrorAlert("Conflict Detected", "Generated account number is already active. Please try again.");
            } catch (InvalidAmountException ex) {
                showErrorAlert("Invalid Deposit Amount", ex.getMessage());
            } catch (Exception ex) {
                showErrorAlert("System Error", "Failed to construct account: " + ex.getMessage());
            }
        });

        view.getChildren().addAll(title, table, form);
        contentArea.getChildren().setAll(view);
    }

    /* =========================================================================
     * SCREEN 4: DEPOSIT
     * ========================================================================= */
    private void showDepositScreen() {
        VBox view = new VBox(20);
        Label title = new Label("Deposit Cash");
        title.getStyleClass().add("view-title");

        VBox form = new VBox(15);
        form.getStyleClass().add("card-panel");
        form.setMaxWidth(500);

        Label accLbl = new Label("Select Target Account:");
        accLbl.getStyleClass().add("label-form");
        ComboBox<String> accBox = new ComboBox<>();
        accBox.getStyleClass().add("combo-box-custom");
        for (Account a : getCurrentUserAccounts()) {
            accBox.getItems().add(a.getAccountNumber() + " (Balance: " + CurrencyFormatter.formatUSD(a.getBalance()) + ")");
        }

        Label amtLbl = new Label("Deposit Amount ($):");
        amtLbl.getStyleClass().add("label-form");
        TextField amtField = new TextField();
        amtField.getStyleClass().add("text-field-custom");

        Label descLbl = new Label("Reference Message:");
        descLbl.getStyleClass().add("label-form");
        TextField descField = new TextField();
        descField.getStyleClass().add("text-field-custom");

        Button submitBtn = new Button("Process Deposit");
        submitBtn.getStyleClass().add("button-primary");

        form.getChildren().addAll(accLbl, accBox, amtLbl, amtField, descLbl, descField, submitBtn);

        submitBtn.setOnAction(e -> {
            String accSel = accBox.getValue();
            String amtStr = amtField.getText().trim();
            String desc = descField.getText().trim();

            if (accSel == null) {
                showErrorAlert("Validation Error", "You must select a target bank account.");
                return;
            }
            if (amtStr.isEmpty()) {
                showErrorAlert("Validation Error", "The deposit amount field cannot be blank.");
                return;
            }
            try {
                String accNum = accSel.split(" ")[0];
                double amount = Double.parseDouble(amtStr);
                
                if (amount <= 0) {
                    showErrorAlert("Validation Error", "Deposit amount must be greater than zero.");
                    return;
                }

                bankService.deposit(accNum, amount, desc.isEmpty() ? "Cash Deposit" : desc);
                showSuccessAlert("Deposit Accepted", "Successfully deposited " + CurrencyFormatter.formatUSD(amount) + " into " + accNum);
                showDepositScreen();
            } catch (NumberFormatException ex) {
                showErrorAlert("Format Error", "Deposit quantity must be a valid numeric amount.");
            } catch (InvalidAmountException ex) {
                showErrorAlert("Invalid Amount", ex.getMessage());
            } catch (AccountNotFoundException ex) {
                showErrorAlert("Account Error", "The target account does not exist.");
            } catch (Exception ex) {
                showErrorAlert("System Error", "Failed to process deposit: " + ex.getMessage());
            }
        });

        view.getChildren().addAll(title, form);
        contentArea.getChildren().setAll(view);
    }

    /* =========================================================================
     * SCREEN 5: WITHDRAW
     * ========================================================================= */
    private void showWithdrawScreen() {
        VBox view = new VBox(20);
        Label title = new Label("Cash Withdrawal");
        title.getStyleClass().add("view-title");

        VBox form = new VBox(15);
        form.getStyleClass().add("card-panel");
        form.setMaxWidth(500);

        Label accLbl = new Label("Select Source Account:");
        accLbl.getStyleClass().add("label-form");
        ComboBox<String> accBox = new ComboBox<>();
        accBox.getStyleClass().add("combo-box-custom");
        for (Account a : getCurrentUserAccounts()) {
            accBox.getItems().add(a.getAccountNumber() + " (Balance: " + CurrencyFormatter.formatUSD(a.getBalance()) + ")");
        }

        Label amtLbl = new Label("Withdrawal Amount ($):");
        amtLbl.getStyleClass().add("label-form");
        TextField amtField = new TextField();
        amtField.getStyleClass().add("text-field-custom");

        Label descLbl = new Label("Reference Message:");
        descLbl.getStyleClass().add("label-form");
        TextField descField = new TextField();
        descField.getStyleClass().add("text-field-custom");

        Button submitBtn = new Button("Process Withdrawal");
        submitBtn.getStyleClass().add("button-primary");

        form.getChildren().addAll(accLbl, accBox, amtLbl, amtField, descLbl, descField, submitBtn);

        submitBtn.setOnAction(e -> {
            String accSel = accBox.getValue();
            String amtStr = amtField.getText().trim();
            String desc = descField.getText().trim();

            if (accSel == null) {
                showErrorAlert("Validation Error", "You must select a source bank account.");
                return;
            }
            if (amtStr.isEmpty()) {
                showErrorAlert("Validation Error", "The withdrawal amount field cannot be blank.");
                return;
            }
            try {
                String accNum = accSel.split(" ")[0];
                double amount = Double.parseDouble(amtStr);
                
                if (amount <= 0) {
                    showErrorAlert("Validation Error", "Withdrawal amount must be greater than zero.");
                    return;
                }

                bankService.withdraw(accNum, amount, desc.isEmpty() ? "Cash withdrawal" : desc);
                showSuccessAlert("Withdrawal Completed", "Successfully withdrew " + CurrencyFormatter.formatUSD(amount) + " from " + accNum);
                showWithdrawScreen();
            } catch (NumberFormatException ex) {
                showErrorAlert("Format Error", "Withdrawal quantity must be a valid numeric amount.");
            } catch (InsufficientBalanceException ex) {
                showErrorAlert("Insufficient Funds", "Withdrawal rejected. The account balance is insufficient (exceeding overdraft limits if applicable).");
            } catch (InvalidAmountException ex) {
                showErrorAlert("Invalid Amount", ex.getMessage());
            } catch (AccountNotFoundException ex) {
                showErrorAlert("Account Error", "The source account does not exist.");
            } catch (Exception ex) {
                showErrorAlert("System Error", "Failed to process withdrawal: " + ex.getMessage());
            }
        });

        view.getChildren().addAll(title, form);
        contentArea.getChildren().setAll(view);
    }

    /* =========================================================================
     * SCREEN 6: TRANSFER MONEY
     * ========================================================================= */
    private void showTransferScreen() {
        VBox view = new VBox(20);
        Label title = new Label("Send Money (Transfer)");
        title.getStyleClass().add("view-title");

        VBox form = new VBox(15);
        form.getStyleClass().add("card-panel");
        form.setMaxWidth(500);

        Label srcLbl = new Label("From Account:");
        srcLbl.getStyleClass().add("label-form");
        ComboBox<String> srcBox = new ComboBox<>();
        srcBox.getStyleClass().add("combo-box-custom");
        for (Account a : getCurrentUserAccounts()) {
            srcBox.getItems().add(a.getAccountNumber() + " (Balance: " + CurrencyFormatter.formatUSD(a.getBalance()) + ")");
        }

        Label destLbl = new Label("Target Account Number:");
        destLbl.getStyleClass().add("label-form");
        TextField destField = new TextField();
        destField.setPromptText("Enter target account number");
        destField.getStyleClass().add("text-field-custom");

        Label amtLbl = new Label("Transfer Amount ($):");
        amtLbl.getStyleClass().add("label-form");
        TextField amtField = new TextField();
        amtField.getStyleClass().add("text-field-custom");

        Label descLbl = new Label("Reference Message:");
        descLbl.getStyleClass().add("label-form");
        TextField descField = new TextField();
        descField.getStyleClass().add("text-field-custom");

        Button submitBtn = new Button("Process Transfer");
        submitBtn.getStyleClass().add("button-primary");

        form.getChildren().addAll(srcLbl, srcBox, destLbl, destField, amtLbl, amtField, descLbl, descField, submitBtn);

        submitBtn.setOnAction(e -> {
            String srcSel = srcBox.getValue();
            String destNum = destField.getText().trim();
            String amtStr = amtField.getText().trim();
            String desc = descField.getText().trim();

            if (srcSel == null) {
                showErrorAlert("Validation Error", "Please select a source account.");
                return;
            }
            if (destNum.isEmpty()) {
                showErrorAlert("Validation Error", "Please specify the destination account number.");
                return;
            }
            if (amtStr.isEmpty()) {
                showErrorAlert("Validation Error", "The transfer amount field cannot be blank.");
                return;
            }
            try {
                String srcNum = srcSel.split(" ")[0];
                
                if (srcNum.equals(destNum)) {
                    showErrorAlert("Validation Error", "Source and destination accounts cannot be the same.");
                    return;
                }

                double amount = Double.parseDouble(amtStr);
                if (amount <= 0) {
                    showErrorAlert("Validation Error", "Transfer amount must be greater than zero.");
                    return;
                }

                bankService.transfer(srcNum, destNum, amount, desc.isEmpty() ? "Transfer" : desc);
                showSuccessAlert("Transfer Complete", "Successfully transferred " + CurrencyFormatter.formatUSD(amount) + " to account " + destNum);
                showTransferScreen();
            } catch (NumberFormatException ex) {
                showErrorAlert("Format Error", "Transfer quantity must be a valid numeric amount.");
            } catch (InvalidTransferException ex) {
                showErrorAlert("Transfer Error", ex.getMessage());
            } catch (InsufficientBalanceException ex) {
                showErrorAlert("Insufficient Funds", "Transfer rejected. The source account does not hold enough balance.");
            } catch (AccountNotFoundException ex) {
                showErrorAlert("Account Error", "Transfer rejected. Please check that the source or target account numbers are correct.");
            } catch (InvalidAmountException ex) {
                showErrorAlert("Invalid Amount", ex.getMessage());
            } catch (Exception ex) {
                showErrorAlert("System Error", "Failed to process transfer: " + ex.getMessage());
            }
        });

        view.getChildren().addAll(title, form);
        contentArea.getChildren().setAll(view);
    }

    /* =========================================================================
     * SCREEN 7: TRANSACTION HISTORY
     * ========================================================================= */
    private void showHistoryScreen() {
        VBox view = new VBox(20);
        Label title = new Label("Audit History Logs");
        title.getStyleClass().add("view-title");

        VBox filterPanel = new VBox(15);
        filterPanel.getStyleClass().add("card-panel");

        Label filterTitle = new Label("History Filters");
        filterTitle.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 16px; -fx-font-weight: bold;");

        GridPane filterGrid = new GridPane();
        filterGrid.setHgap(15);
        filterGrid.setVgap(12);

        Label selectLbl = new Label("Select Account:");
        selectLbl.getStyleClass().add("label-form");
        ComboBox<String> selectBox = new ComboBox<>();
        selectBox.getStyleClass().add("combo-box-custom");
        for (Account a : getCurrentUserAccounts()) {
            selectBox.getItems().add(a.getAccountNumber());
        }

        Label typeLbl = new Label("Tx Type:");
        typeLbl.getStyleClass().add("label-form");
        ComboBox<String> typeBox = new ComboBox<>(FXCollections.observableArrayList("ALL", "DEPOSIT", "WITHDRAWAL", "TRANSFER"));
        typeBox.setValue("ALL");
        typeBox.getStyleClass().add("combo-box-custom");

        Label minLbl = new Label("Min Value ($):");
        minLbl.getStyleClass().add("label-form");
        TextField minField = new TextField();
        minField.getStyleClass().add("text-field-custom");

        Label maxLbl = new Label("Max Value ($):");
        maxLbl.getStyleClass().add("label-form");
        TextField maxField = new TextField();
        maxField.getStyleClass().add("text-field-custom");

        Label startLbl = new Label("Start Date:");
        startLbl.getStyleClass().add("label-form");
        DatePicker startDatePicker = new DatePicker();
        startDatePicker.setStyle("-fx-font-size: 13px;");

        Label endLbl = new Label("End Date:");
        endLbl.getStyleClass().add("label-form");
        DatePicker endDatePicker = new DatePicker();
        endDatePicker.setStyle("-fx-font-size: 13px;");

        filterGrid.add(selectLbl, 0, 0);
        filterGrid.add(selectBox, 1, 0);
        filterGrid.add(typeLbl, 2, 0);
        filterGrid.add(typeBox, 3, 0);

        filterGrid.add(minLbl, 0, 1);
        filterGrid.add(minField, 1, 1);
        filterGrid.add(maxLbl, 2, 1);
        filterGrid.add(maxField, 3, 1);

        filterGrid.add(startLbl, 0, 2);
        filterGrid.add(startDatePicker, 1, 2);
        filterGrid.add(endLbl, 2, 2);
        filterGrid.add(endDatePicker, 3, 2);

        HBox btnBox = new HBox(15);
        Button applyBtn = new Button("Apply Filters");
        applyBtn.getStyleClass().add("button-primary");

        Button clearBtn = new Button("Clear Filters");
        clearBtn.getStyleClass().add("button-secondary");

        btnBox.getChildren().addAll(applyBtn, clearBtn);
        filterPanel.getChildren().addAll(filterTitle, filterGrid, btnBox);

        TableView<Transaction> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        table.setPrefHeight(300);

        TableColumn<Transaction, String> idCol = new TableColumn<>("Tx Reference");
        idCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTransactionId()));

        TableColumn<Transaction, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTimestamp().toLocalDate().toString()));

        TableColumn<Transaction, String> txTypeCol = new TableColumn<>("Type");
        txTypeCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getType().name()));

        TableColumn<Transaction, String> amountCol = new TableColumn<>("Amount");
        amountCol.setCellValueFactory(cellData -> new SimpleStringProperty(CurrencyFormatter.formatUSD(cellData.getValue().getAmount())));

        TableColumn<Transaction, String> srcCol = new TableColumn<>("Source Account");
        srcCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getSourceAccountNumber()));

        TableColumn<Transaction, String> destCol = new TableColumn<>("Destination");
        destCol.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getDestinationAccountNumber() == null ? "-" : cellData.getValue().getDestinationAccountNumber()
        ));

        TableColumn<Transaction, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getStatus().name()));

        TableColumn<Transaction, String> descriptionCol = new TableColumn<>("Description");
        descriptionCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getDescription()));

        table.getColumns().addAll(idCol, dateCol, txTypeCol, amountCol, srcCol, destCol, statusCol, descriptionCol);

        applyBtn.setOnAction(e -> {
            String acc = selectBox.getValue();
            if (acc == null) {
                showErrorAlert("Validation Error", "Select an account to load records.");
                return;
            }
            try {
                String typeStr = typeBox.getValue();
                TransactionType type = "ALL".equalsIgnoreCase(typeStr) ? null : TransactionType.valueOf(typeStr);

                String minStr = minField.getText().trim();
                Double min = minStr.isEmpty() ? null : Double.parseDouble(minStr);

                String maxStr = maxField.getText().trim();
                Double max = maxStr.isEmpty() ? null : Double.parseDouble(maxStr);

                LocalDate startLd = startDatePicker.getValue();
                LocalDateTime start = startLd == null ? null : LocalDateTime.of(startLd, LocalTime.MIN);

                LocalDate endLd = endDatePicker.getValue();
                LocalDateTime end = endLd == null ? null : LocalDateTime.of(endLd, LocalTime.MAX);

                List<Transaction> filtered = bankService.getFilteredTransactions(acc, type, min, max, start, end);
                table.setItems(FXCollections.observableArrayList(filtered));

            } catch (NumberFormatException ex) {
                showErrorAlert("Format Error", "Threshold limits must be valid numerical amounts.");
            } catch (AccountNotFoundException ex) {
                showErrorAlert("Account Error", "The selected account details could not be found.");
            } catch (Exception ex) {
                showErrorAlert("System Error", "Failed to retrieve history: " + ex.getMessage());
            }
        });

        clearBtn.setOnAction(e -> {
            typeBox.setValue("ALL");
            minField.clear();
            maxField.clear();
            startDatePicker.setValue(null);
            endDatePicker.setValue(null);
            
            String acc = selectBox.getValue();
            if (acc != null) {
                try {
                    table.setItems(FXCollections.observableArrayList(bankService.getTransactionHistory(acc)));
                } catch (Exception ex) {
                    showErrorAlert("System Error", "Error reloading records: " + ex.getMessage());
                }
            } else {
                table.getItems().clear();
            }
        });

        List<Account> accounts = getCurrentUserAccounts();
        if (!accounts.isEmpty()) {
            selectBox.setValue(accounts.get(0).getAccountNumber());
            try {
                table.setItems(FXCollections.observableArrayList(bankService.getTransactionHistory(accounts.get(0).getAccountNumber())));
            } catch (Exception ex) {
                // Ignore
            }
        }

        view.getChildren().addAll(title, filterPanel, table);
        contentArea.getChildren().setAll(view);
    }

    /* =========================================================================
     * SCREEN 8: FINANCIAL ANALYTICS DESIGN
     * ========================================================================= */
    private void showAnalyticsScreen() {
        VBox view = new VBox(20);
        view.getStyleClass().add("content-area");

        Label title = new Label("Financial Statistics & Streams");
        title.getStyleClass().add("view-title");

        List<Account> accounts = getCurrentUserAccounts();
        List<Transaction> allTxs = new ArrayList<>();
        for (Account a : accounts) {
            try {
                allTxs.addAll(bankService.getTransactionHistory(a.getAccountNumber()));
            } catch (Exception e) {
                // Ignore
            }
        }

        double totalDeposited = analyticsService.calculateTotalDepositedAmount(allTxs);
        double totalWithdrawn = analyticsService.calculateTotalWithdrawnAmount(allTxs);
        double totalTransferred = analyticsService.calculateTotalTransferredAmount(allTxs);
        double averageTx = analyticsService.calculateAverageTransactionAmount(allTxs);
        long countTxs = analyticsService.countTransactions(allTxs);
        
        Optional<Transaction> largestTxOpt = analyticsService.findLargestTransaction(allTxs);
        String largestTxStr = largestTxOpt.map(tx -> CurrencyFormatter.formatUSD(tx.getAmount())).orElse("$0.00");

        GridPane cardGrid = new GridPane();
        cardGrid.setHgap(15);
        cardGrid.setVgap(15);

        cardGrid.add(createMetricCard("TOTAL DEPOSITS", CurrencyFormatter.formatUSD(totalDeposited)), 0, 0);
        cardGrid.add(createMetricCard("TOTAL WITHDRAWALS", CurrencyFormatter.formatUSD(totalWithdrawn)), 1, 0);
        cardGrid.add(createMetricCard("TOTAL TRANSFERS", CurrencyFormatter.formatUSD(totalTransferred)), 2, 0);

        cardGrid.add(createMetricCard("AVERAGE TRANSACTION", CurrencyFormatter.formatUSD(averageTx)), 0, 1);
        cardGrid.add(createMetricCard("LARGEST TRANSACTION", largestTxStr), 1, 1);
        cardGrid.add(createMetricCard("TRANSACTIONS COUNT", String.valueOf(countTxs)), 2, 1);

        ScrollPane scrollContainer = new ScrollPane();
        scrollContainer.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scrollContainer.setFitToWidth(true);

        VBox chartsBox = new VBox(25);
        chartsBox.setPadding(new Insets(15, 0, 15, 0));

        PieChart typePieChart = new PieChart();
        typePieChart.setTitle("Transactions Volume split by Type");
        Map<TransactionType, Long> countsMap = analyticsService.getTransactionCountsByType(allTxs);
        for (Map.Entry<TransactionType, Long> entry : countsMap.entrySet()) {
            typePieChart.getData().add(new PieChart.Data(entry.getKey().name(), entry.getValue()));
        }
        typePieChart.setPrefHeight(250);

        CategoryAxis mXAxis = new CategoryAxis();
        mXAxis.setLabel("Month");
        NumberAxis mYAxis = new NumberAxis();
        mYAxis.setLabel("Total Amount ($)");
        BarChart<String, Number> monthlyBarChart = new BarChart<>(mXAxis, mYAxis);
        monthlyBarChart.setTitle("Monthly Transaction Volume Summary");
        monthlyBarChart.setPrefHeight(250);

        XYChart.Series<String, Number> mSeries = new XYChart.Series<>();
        mSeries.setName("Monthly Aggregates");
        Map<Month, Double> monthlySummary = analyticsService.calculateMonthlyTransactionSummary(allTxs);
        for (Map.Entry<Month, Double> entry : monthlySummary.entrySet()) {
            mSeries.getData().add(new XYChart.Data<>(entry.getKey().name(), entry.getValue()));
        }
        monthlyBarChart.getData().add(mSeries);

        CategoryAxis cXAxis = new CategoryAxis();
        NumberAxis cYAxis = new NumberAxis();
        BarChart<String, Number> compBarChart = new BarChart<>(cXAxis, cYAxis);
        compBarChart.setTitle("Comparative Analysis: Deposits vs. Withdrawals");
        compBarChart.setPrefHeight(250);

        XYChart.Series<String, Number> depSeries = new XYChart.Series<>();
        depSeries.setName("Deposited Sum ($)");
        depSeries.getData().add(new XYChart.Data<>("Deposits", totalDeposited));

        XYChart.Series<String, Number> wthSeries = new XYChart.Series<>();
        wthSeries.setName("Withdrawn Sum ($)");
        wthSeries.getData().add(new XYChart.Data<>("Withdrawals", totalWithdrawn));

        compBarChart.getData().addAll(depSeries, wthSeries);

        chartsBox.getChildren().addAll(typePieChart, monthlyBarChart, compBarChart);
        scrollContainer.setContent(chartsBox);

        view.getChildren().addAll(title, cardGrid, scrollContainer);
        contentArea.getChildren().setAll(view);
    }

    /* =========================================================================
     * SCREEN 9: ACCOUNT SNAPSHOTS (DEEP COPY DEMO)
     * ========================================================================= */
    private void showSnapshotScreen() {
        VBox view = new VBox(20);
        Label title = new Label("Temporal Account Snapshots");
        title.getStyleClass().add("view-title");

        Label instruction = new Label("Deep copy validation: Create a snapshot, modify the active account, then review both to confirm zero side-effects.");
        instruction.setStyle("-fx-text-fill: #8b949e;");

        HBox actionBox = new HBox(15);
        actionBox.getStyleClass().add("card-panel");
        actionBox.setAlignment(Pos.CENTER_LEFT);

        Label accLbl = new Label("Select Target:");
        accLbl.getStyleClass().add("label-form");
        ComboBox<String> accBox = new ComboBox<>();
        accBox.getStyleClass().add("combo-box-custom");
        for (Account a : getCurrentUserAccounts()) {
            accBox.getItems().add(a.getAccountNumber());
        }

        Button createBtn = new Button("Create Snapshot");
        createBtn.getStyleClass().add("button-primary");

        actionBox.getChildren().addAll(accLbl, accBox, createBtn);

        TableView<Account> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<Account, String> timeCol = new TableColumn<>("Time Generated");
        timeCol.setCellValueFactory(cellData -> new SimpleStringProperty(LocalDateTime.now().toLocalTime().toString().substring(0, 8)));

        TableColumn<Account, String> numCol = new TableColumn<>("Account Number");
        numCol.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getAccountNumber()));

        TableColumn<Account, String> balCol = new TableColumn<>("Snapshot Balance");
        balCol.setCellValueFactory(cellData -> new SimpleStringProperty(CurrencyFormatter.formatUSD(cellData.getValue().getBalance())));

        TableColumn<Account, String> txsCol = new TableColumn<>("Log Entries");
        txsCol.setCellValueFactory(cellData -> new SimpleStringProperty(String.valueOf(cellData.getValue().getTransactions().size())));

        table.getColumns().addAll(timeCol, numCol, balCol, txsCol);
        table.setItems(FXCollections.observableArrayList(snapshotCache));
        table.setPrefHeight(200);

        createBtn.setOnAction(e -> {
            String acc = accBox.getValue();
            if (acc == null) {
                showErrorAlert("Validation Error", "Please select an account to capture a snapshot.");
                return;
            }
            try {
                Account snapshot = bankService.createAccountSnapshot(acc);
                snapshotCache.add(snapshot);
                table.setItems(FXCollections.observableArrayList(snapshotCache));
                showSuccessAlert("Snapshot Created", "Captured independent historical copy of " + acc + "\nUpdates to the original account balance will not affect this snapshot.");
            } catch (AccountNotFoundException ex) {
                showErrorAlert("Account Error", "The selected account does not exist.");
            } catch (Exception ex) {
                showErrorAlert("Snapshot Failure", ex.getMessage());
            }
        });

        view.getChildren().addAll(title, instruction, actionBox, table);
        contentArea.getChildren().setAll(view);
    }

    /* =========================================================================
     * SCREEN 10: CLIENT PROFILES
     * ========================================================================= */
    private void showProfileScreen() {
        VBox view = new VBox(20);
        Label title = new Label("Client Profile");
        title.getStyleClass().add("view-title");

        VBox details = new VBox(10);
        details.getStyleClass().add("card-panel");

        Label name = new Label("Client Name: " + currentUser.getName());
        name.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 16px; -fx-font-weight: bold;");

        Label email = new Label("Primary Email: " + currentUser.getEmail());
        email.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 14px;");

        Label id = new Label("Access ID (Key): " + currentUser.getUserId());
        id.setStyle("-fx-text-fill: #8b949e; -fx-font-size: 14px;");

        details.getChildren().addAll(name, email, id);

        view.getChildren().addAll(title, details);
        contentArea.getChildren().setAll(view);
    }

    private VBox createMetricCard(String label, String value) {
        VBox card = new VBox(5);
        card.getStyleClass().add("metric-card");
        Label l = new Label(label);
        l.getStyleClass().add("metric-label");
        Label v = new Label(value);
        v.getStyleClass().add("metric-value");
        card.getChildren().addAll(l, v);
        return card;
    }

    private void updateMetricCardValue(VBox cardNode, String newValue) {
        Label valueLabel = (Label) cardNode.getChildren().get(1);
        valueLabel.setText(newValue);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
