/**
 * JavaBank SPA Controller & REST Client
 */

// Application Global State
const state = {
    currentUser: null,
    accounts: [],
    selectedAccount: null,
    charts: {
        typeSplit: null,
        monthly: null,
        comparison: null
    }
};

// ==========================================
// 1. INITIALIZATION & ROUTING
// ==========================================
document.addEventListener('DOMContentLoaded', () => {
    initAuth();
    initNavigation();
    initForms();
});

function initAuth() {
    const btnLogin = document.getElementById('btn-login');
    const btnRegister = document.getElementById('btn-register');
    const btnLogout = document.getElementById('btn-logout');
    const switchToRegister = document.getElementById('switch-to-register');
    const switchToLogin = document.getElementById('switch-to-login');

    switchToRegister.addEventListener('click', (e) => {
        e.preventDefault();
        document.getElementById('login-form-box').classList.add('hidden');
        document.getElementById('register-form-box').classList.remove('hidden');
    });

    switchToLogin.addEventListener('click', (e) => {
        e.preventDefault();
        document.getElementById('register-form-box').classList.add('hidden');
        document.getElementById('login-form-box').classList.remove('hidden');
    });

    btnLogin.addEventListener('click', async () => {
        const userId = document.getElementById('login-user-id').value.trim();
        if (!userId) {
            showToast('Please enter your Customer Access ID', 'error');
            return;
        }
        await handleLogin(userId);
    });

    btnRegister.addEventListener('click', async () => {
        const name = document.getElementById('reg-name').value.trim();
        const email = document.getElementById('reg-email').value.trim();
        if (!name || !email) {
            showToast('Name and email are required', 'error');
            return;
        }
        await handleRegister(name, email);
    });

    btnLogout.addEventListener('click', () => {
        state.currentUser = null;
        state.accounts = [];
        state.selectedAccount = null;
        document.getElementById('app-layout').classList.add('hidden');
        document.getElementById('auth-screen').classList.remove('hidden');
        showToast('You have signed out successfully.', 'info');
    });
}

function initNavigation() {
    const navItems = document.querySelectorAll('.nav-item');
    navItems.forEach(item => {
        item.addEventListener('click', (e) => {
            e.preventDefault();
            const viewName = item.getAttribute('data-view');
            navigateTo(viewName);
        });
    });
}

function navigateTo(viewName) {
    // Update nav active state
    document.querySelectorAll('.nav-item').forEach(item => {
        if (item.getAttribute('data-view') === viewName) {
            item.classList.add('active');
        } else {
            item.classList.remove('active');
        }
    });

    // Hide all views, show selected
    document.querySelectorAll('.content-view').forEach(view => {
        view.classList.add('hidden');
    });

    const targetView = document.getElementById(`view-${viewName}`);
    if (targetView) {
        targetView.classList.remove('hidden');
    }

    // Trigger view-specific loads
    if (viewName === 'dashboard') {
        loadDashboard();
    } else if (viewName === 'accounts') {
        renderAccountsTable();
    } else if (viewName === 'history') {
        loadFilteredTransactions();
    } else if (viewName === 'analytics') {
        loadAnalytics();
    } else if (viewName === 'snapshots') {
        loadSnapshots();
    } else if (viewName === 'deposit' || viewName === 'withdraw' || viewName === 'transfer') {
        populateAccountDropdowns();
    }
}

// ==========================================
// 2. AUTHENTICATION & LOGIN FLOW
// ==========================================
async function handleLogin(userId) {
    try {
        const res = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ userId })
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error || 'Authentication failed');

        state.currentUser = {
            userId: data.userId,
            name: data.name,
            email: data.email
        };
        state.accounts = data.accounts || [];
        
        setupUserInterface();
        showToast(`Welcome back, ${data.name}!`, 'success');
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function handleRegister(name, email) {
    try {
        const res = await fetch('/api/auth/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ name, email })
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error || 'Registration failed');

        showToast(`Profile created! Your ID is: ${data.userId}`, 'success');
        document.getElementById('login-user-id').value = data.userId;
        document.getElementById('register-form-box').classList.add('hidden');
        document.getElementById('login-form-box').classList.remove('hidden');
    } catch (err) {
        showToast(err.message, 'error');
    }
}

function setupUserInterface() {
    document.getElementById('auth-screen').classList.add('hidden');
    document.getElementById('app-layout').classList.remove('hidden');

    // Sidebar & Profile display
    document.getElementById('sidebar-user-name').textContent = state.currentUser.name;
    document.getElementById('sidebar-user-id').textContent = state.currentUser.userId;
    document.getElementById('sidebar-avatar').textContent = state.currentUser.name
        .split(' ').map(n => n[0]).join('').toUpperCase().substring(0, 2);

    document.getElementById('welcome-title').textContent = `Welcome back, ${state.currentUser.name.split(' ')[0]}!`;
    document.getElementById('profile-name').textContent = state.currentUser.name;
    document.getElementById('profile-email').textContent = state.currentUser.email;
    document.getElementById('profile-id').textContent = state.currentUser.userId;

    populateAccountDropdowns();
    navigateTo('dashboard');
}

// ==========================================
// 3. ACCOUNT DROPDOWNS & SELECTION
// ==========================================
function populateAccountDropdowns() {
    const dropdownIds = [
        'dash-account-select',
        'deposit-account',
        'withdraw-account',
        'transfer-source',
        'filter-account',
        'snapshot-account-picker',
        'statement-account-select'
    ];

    dropdownIds.forEach(id => {
        const select = document.getElementById(id);
        if (!select) return;
        select.innerHTML = '';

        if (state.accounts.length === 0) {
            const opt = document.createElement('option');
            opt.value = '';
            opt.textContent = 'No Accounts Available';
            select.appendChild(opt);
            return;
        }

        state.accounts.forEach(acc => {
            const opt = document.createElement('option');
            opt.value = acc.accountNumber;
            opt.textContent = `${acc.accountNumber} (${acc.accountType} - $${acc.balance.toLocaleString('en-US', { minimumFractionDigits: 2 })})`;
            select.appendChild(opt);
        });
    });

    if (state.accounts.length > 0 && !state.selectedAccount) {
        state.selectedAccount = state.accounts[0];
    }
}

// ==========================================
// 4. DASHBOARD DATA LOADER
// ==========================================
async function loadDashboard() {
    const selector = document.getElementById('dash-account-select');
    if (!selector) return;

    selector.onchange = () => {
        const accNum = selector.value;
        state.selectedAccount = state.accounts.find(a => a.accountNumber === accNum) || state.accounts[0];
        updateDashboardView();
    };

    await refreshAccounts();
    updateDashboardView();
}

async function updateDashboardView() {
    if (!state.selectedAccount) return;
    const acc = state.selectedAccount;

    document.getElementById('metric-balance').textContent = `$${acc.balance.toLocaleString('en-US', { minimumFractionDigits: 2 })}`;
    document.getElementById('metric-type').textContent = acc.accountType;
    document.getElementById('metric-status').textContent = acc.accountStatus;

    try {
        const res = await fetch(`/api/transactions?accountNumber=${acc.accountNumber}`);
        const txs = await res.json();

        // Calculate totals on the fly
        const totalDep = txs.filter(t => t.type === 'DEPOSIT').reduce((s, t) => s + t.amount, 0);
        const totalWth = txs.filter(t => t.type === 'WITHDRAWAL').reduce((s, t) => s + t.amount, 0);
        const totalTrs = txs.filter(t => t.type === 'TRANSFER').reduce((s, t) => s + t.amount, 0);

        document.getElementById('metric-total-deposits').textContent = `$${totalDep.toLocaleString('en-US', { minimumFractionDigits: 2 })}`;
        document.getElementById('metric-total-withdrawals').textContent = `$${totalWth.toLocaleString('en-US', { minimumFractionDigits: 2 })}`;
        document.getElementById('metric-total-transfers').textContent = `$${totalTrs.toLocaleString('en-US', { minimumFractionDigits: 2 })}`;

        // Render last 5
        const tbody = document.querySelector('#recent-transactions-table tbody');
        tbody.innerHTML = '';
        const recent = txs.slice(-5).reverse();

        if (recent.length === 0) {
            tbody.innerHTML = '<tr><td colspan="4" class="text-center">No recent transactions recorded.</td></tr>';
        } else {
            recent.forEach(tx => {
                const tr = document.createElement('tr');
                tr.innerHTML = `
                    <td><span class="badge badge-${tx.type.toLowerCase()}">${tx.type}</span></td>
                    <td class="font-bold ${tx.type === 'DEPOSIT' ? 'text-success' : 'text-danger'}">
                        ${tx.type === 'DEPOSIT' ? '+' : '-'}$${tx.amount.toFixed(2)}
                    </td>
                    <td>${tx.timestamp.substring(0, 10)}</td>
                    <td>${tx.description || '-'}</td>
                `;
                tbody.appendChild(tr);
            });
        }
    } catch (err) {
        console.error('Dashboard tx fetch error:', err);
    }
}

// ==========================================
// 5. ACCOUNTS & TRANSACTIONS MANAGEMENT
// ==========================================
async function refreshAccounts() {
    if (!state.currentUser) return;
    try {
        const res = await fetch(`/api/accounts?userId=${state.currentUser.userId}`);
        state.accounts = await res.json();
        populateAccountDropdowns();
    } catch (err) {
        console.error('Failed to refresh accounts:', err);
    }
}

function renderAccountsTable() {
    const tbody = document.getElementById('accounts-table-body');
    tbody.innerHTML = '';

    if (state.accounts.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="text-center">No accounts found. Open one using the button above.</td></tr>';
        return;
    }

    state.accounts.forEach(acc => {
        const tr = document.createElement('tr');
        const extra = acc.interestRate 
            ? `APY: ${(acc.interestRate * 100).toFixed(1)}%` 
            : `Overdraft: $${(acc.overdraftLimit || 0).toFixed(2)}`;

        tr.innerHTML = `
            <td class="font-mono"><strong>${acc.accountNumber}</strong></td>
            <td><span class="badge">${acc.accountType}</span></td>
            <td class="font-bold">$${acc.balance.toLocaleString('en-US', { minimumFractionDigits: 2 })}</td>
            <td><span class="badge text-success">${acc.accountStatus}</span></td>
            <td>${extra}</td>
            <td>
                <button class="btn btn-secondary btn-sm" onclick="quickSelectAccount('${acc.accountNumber}')">
                    <i class="fa-solid fa-eye"></i> View
                </button>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

function quickSelectAccount(accNum) {
    state.selectedAccount = state.accounts.find(a => a.accountNumber === accNum);
    navigateTo('dashboard');
}

// ==========================================
// 6. FORMS INITIALIZATION & SUBMIT
// ==========================================
function initForms() {
    // Deposit Form
    document.getElementById('form-deposit').addEventListener('submit', async (e) => {
        e.preventDefault();
        const accountNumber = document.getElementById('deposit-account').value;
        const amount = parseFloat(document.getElementById('deposit-amount').value);
        const description = document.getElementById('deposit-desc').value.trim();

        try {
            const res = await fetch('/api/transactions/deposit', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ accountNumber, amount, description })
            });
            const data = await res.json();
            if (!res.ok) throw new Error(data.error || 'Deposit failed');

            showToast(data.message, 'success');
            document.getElementById('deposit-amount').value = '';
            document.getElementById('deposit-desc').value = '';
            await refreshAccounts();
            navigateTo('dashboard');
        } catch (err) {
            showToast(err.message, 'error');
        }
    });

    // Withdraw Form
    document.getElementById('form-withdraw').addEventListener('submit', async (e) => {
        e.preventDefault();
        const accountNumber = document.getElementById('withdraw-account').value;
        const amount = parseFloat(document.getElementById('withdraw-amount').value);
        const description = document.getElementById('withdraw-desc').value.trim();

        try {
            const res = await fetch('/api/transactions/withdraw', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ accountNumber, amount, description })
            });
            const data = await res.json();
            if (!res.ok) throw new Error(data.error || 'Withdrawal rejected');

            showToast(data.message, 'success');
            document.getElementById('withdraw-amount').value = '';
            document.getElementById('withdraw-desc').value = '';
            await refreshAccounts();
            navigateTo('dashboard');
        } catch (err) {
            showToast(err.message, 'error');
        }
    });

    // Transfer Form
    document.getElementById('form-transfer').addEventListener('submit', async (e) => {
        e.preventDefault();
        const sourceAccountNumber = document.getElementById('transfer-source').value;
        const destinationAccountNumber = document.getElementById('transfer-dest').value.trim();
        const amount = parseFloat(document.getElementById('transfer-amount').value);
        const description = document.getElementById('transfer-desc').value.trim();

        try {
            const res = await fetch('/api/transactions/transfer', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ sourceAccountNumber, destinationAccountNumber, amount, description })
            });
            const data = await res.json();
            if (!res.ok) throw new Error(data.error || 'Transfer rejected');

            showToast(data.message, 'success');
            document.getElementById('transfer-dest').value = '';
            document.getElementById('transfer-amount').value = '';
            document.getElementById('transfer-desc').value = '';
            await refreshAccounts();
            navigateTo('dashboard');
        } catch (err) {
            showToast(err.message, 'error');
        }
    });

    // Open Account Modal Form
    document.getElementById('new-account-type').addEventListener('change', (e) => {
        const lbl = document.getElementById('lbl-new-account-extra');
        const input = document.getElementById('new-account-extra');
        if (e.target.value === 'Savings') {
            lbl.textContent = 'Interest Rate (e.g. 0.03 for 3%)';
            input.placeholder = 'Default 0.015 (1.5%)';
        } else {
            lbl.textContent = 'Overdraft Limit ($ USD)';
            input.placeholder = 'Default $500.00';
        }
    });

    document.getElementById('form-open-account').addEventListener('submit', async (e) => {
        e.preventDefault();
        const accountType = document.getElementById('new-account-type').value;
        const initialBalance = parseFloat(document.getElementById('new-account-deposit').value) || 0;
        const extraParam = document.getElementById('new-account-extra').value;

        try {
            const res = await fetch('/api/accounts', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({
                    userId: state.currentUser.userId,
                    accountType,
                    initialBalance,
                    extraParam: extraParam ? parseFloat(extraParam) : null
                })
            });
            const data = await res.json();
            if (!res.ok) throw new Error(data.error || 'Account creation failed');

            showToast(`Account opened successfully! Ref: ${data.accountNumber}`, 'success');
            closeModal('modal-open-account');
            await refreshAccounts();
            renderAccountsTable();
        } catch (err) {
            showToast(err.message, 'error');
        }
    });

    // History Filters
    document.getElementById('btn-apply-filters').addEventListener('click', loadFilteredTransactions);
    document.getElementById('btn-clear-filters').addEventListener('click', () => {
        document.getElementById('filter-type').value = 'ALL';
        document.getElementById('filter-min').value = '';
        document.getElementById('filter-max').value = '';
        document.getElementById('filter-start-date').value = '';
        document.getElementById('filter-end-date').value = '';
        loadFilteredTransactions();
    });

    // Snapshots
    document.getElementById('btn-create-snapshot').addEventListener('click', handleCreateSnapshot);

    // Profile Statements
    document.getElementById('btn-generate-statement').addEventListener('click', handleGenerateStatement);
    document.getElementById('btn-vault-summary').addEventListener('click', handleGenerateVaultSummary);
}

// ==========================================
// 7. AUDIT HISTORY WITH STREAM FILTERS
// ==========================================
async function loadFilteredTransactions() {
    const accSelect = document.getElementById('filter-account');
    if (!accSelect || !accSelect.value) return;

    const acc = accSelect.value;
    const type = document.getElementById('filter-type').value;
    const min = document.getElementById('filter-min').value;
    const max = document.getElementById('filter-max').value;
    const start = document.getElementById('filter-start-date').value;
    const end = document.getElementById('filter-end-date').value;

    let url = `/api/transactions?accountNumber=${acc}`;
    if (type && type !== 'ALL') url += `&type=${type}`;
    if (min) url += `&minAmount=${min}`;
    if (max) url += `&maxAmount=${max}`;
    if (start) url += `&startDate=${start}`;
    if (end) url += `&endDate=${end}`;

    try {
        const res = await fetch(url);
        const txs = await res.json();
        const tbody = document.getElementById('history-table-body');
        tbody.innerHTML = '';

        if (txs.length === 0) {
            tbody.innerHTML = '<tr><td colspan="8" class="text-center">No transactions match the selected criteria.</td></tr>';
            return;
        }

        txs.forEach(tx => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td class="font-mono">${tx.transactionId}</td>
                <td>${tx.timestamp.replace('T', ' ').substring(0, 19)}</td>
                <td><span class="badge badge-${tx.type.toLowerCase()}">${tx.type}</span></td>
                <td class="font-bold ${tx.type === 'DEPOSIT' ? 'text-success' : 'text-danger'}">
                    $${tx.amount.toFixed(2)}
                </td>
                <td class="font-mono">${tx.sourceAccountNumber || '-'}</td>
                <td class="font-mono">${tx.destinationAccountNumber || '-'}</td>
                <td><span class="badge text-success">${tx.status}</span></td>
                <td>${tx.description || '-'}</td>
            `;
            tbody.appendChild(tr);
        });
    } catch (err) {
        showToast('Error loading transaction history: ' + err.message, 'error');
    }
}

// ==========================================
// 8. FINANCIAL ANALYTICS & CHART.JS
// ==========================================
async function loadAnalytics() {
    if (!state.currentUser) return;
    try {
        const res = await fetch(`/api/analytics?userId=${state.currentUser.userId}`);
        const data = await res.json();
        if (!res.ok) throw new Error(data.error || 'Failed to load analytics');

        document.getElementById('analytics-avg-tx').textContent = `$${data.avgTransaction.toFixed(2)}`;
        document.getElementById('analytics-max-tx').textContent = `$${data.largestTransactionAmount.toFixed(2)}`;
        document.getElementById('analytics-count-tx').textContent = data.count;

        renderAnalyticsCharts(data);
    } catch (err) {
        showToast(err.message, 'error');
    }
}

function renderAnalyticsCharts(data) {
    // 1. Transactions Split Donut Chart
    const ctxType = document.getElementById('chart-type-split').getContext('2d');
    if (state.charts.typeSplit) state.charts.typeSplit.destroy();

    const typeLabels = Object.keys(data.typeCounts);
    const typeValues = Object.values(data.typeCounts);

    state.charts.typeSplit = new Chart(ctxType, {
        type: 'doughnut',
        data: {
            labels: typeLabels.length ? typeLabels : ['No Data'],
            datasets: [{
                data: typeValues.length ? typeValues : [1],
                backgroundColor: ['#2ea043', '#f85149', '#58a6ff', '#a371f7'],
                borderWidth: 0
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { position: 'bottom', labels: { color: '#8b949e' } }
            }
        }
    });

    // 2. Monthly Summary Bar Chart
    const ctxMonthly = document.getElementById('chart-monthly').getContext('2d');
    if (state.charts.monthly) state.charts.monthly.destroy();

    const monthLabels = Object.keys(data.monthlySummary);
    const monthValues = Object.values(data.monthlySummary);

    state.charts.monthly = new Chart(ctxMonthly, {
        type: 'bar',
        data: {
            labels: monthLabels.length ? monthLabels : ['Current Month'],
            datasets: [{
                label: 'Monthly Volume ($)',
                data: monthValues.length ? monthValues : [0],
                backgroundColor: '#58a6ff',
                borderRadius: 6
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                x: { ticks: { color: '#8b949e' }, grid: { color: 'rgba(240, 246, 252, 0.05)' } },
                y: { ticks: { color: '#8b949e' }, grid: { color: 'rgba(240, 246, 252, 0.05)' } }
            },
            plugins: {
                legend: { display: false }
            }
        }
    });

    // 3. Cashflow Comparison Chart
    const ctxComp = document.getElementById('chart-comparison').getContext('2d');
    if (state.charts.comparison) state.charts.comparison.destroy();

    state.charts.comparison = new Chart(ctxComp, {
        type: 'bar',
        data: {
            labels: ['Total Inflow (Deposits)', 'Total Outflow (Withdrawals)', 'Transfers'],
            datasets: [{
                label: 'Cashflow Sum ($)',
                data: [data.totalDeposits, data.totalWithdrawals, data.totalTransfers],
                backgroundColor: ['#2ea043', '#f85149', '#58a6ff'],
                borderRadius: 8
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            scales: {
                x: { ticks: { color: '#8b949e' }, grid: { color: 'rgba(240, 246, 252, 0.05)' } },
                y: { ticks: { color: '#8b949e' }, grid: { color: 'rgba(240, 246, 252, 0.05)' } }
            },
            plugins: {
                legend: { display: false }
            }
        }
    });
}

// ==========================================
// 9. SNAPSHOTS (DEEP COPY DEMO)
// ==========================================
async function handleCreateSnapshot() {
    const picker = document.getElementById('snapshot-account-picker');
    if (!picker || !picker.value) {
        showToast('Please select an account to snapshot', 'warning');
        return;
    }
    try {
        const res = await fetch('/api/snapshots', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ accountNumber: picker.value })
        });
        const data = await res.json();
        if (!res.ok) throw new Error(data.error || 'Snapshot generation failed');

        showToast(`Created independent snapshot for ${data.accountNumber}!`, 'success');
        loadSnapshots();
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function loadSnapshots() {
    try {
        const res = await fetch('/api/snapshots');
        const snapshots = await res.json();
        const tbody = document.getElementById('snapshots-table-body');
        tbody.innerHTML = '';

        if (snapshots.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" class="text-center">No snapshots captured yet.</td></tr>';
            return;
        }

        snapshots.forEach((snap, idx) => {
            const tr = document.createElement('tr');
            tr.innerHTML = `
                <td class="font-mono">#SNAP-00${idx + 1}</td>
                <td class="font-mono">${snap.accountNumber}</td>
                <td class="font-bold text-success">$${snap.balance.toFixed(2)}</td>
                <td><span class="badge">${snap.accountType}</span></td>
                <td><span class="badge text-success">${snap.accountStatus}</span></td>
                <td>${(snap.transactions || []).length} records</td>
            `;
            tbody.appendChild(tr);
        });
    } catch (err) {
        console.error('Failed to load snapshots:', err);
    }
}

// ==========================================
// 10. STATEMENTS & SYSTEM VAULT
// ==========================================
async function handleGenerateStatement() {
    const acc = document.getElementById('statement-account-select').value;
    if (!acc) {
        showToast('Please select an account', 'warning');
        return;
    }
    try {
        const res = await fetch(`/api/reports/statement?accountNumber=${acc}`);
        const data = await res.json();
        document.getElementById('statement-text').textContent = data.statement;
    } catch (err) {
        showToast(err.message, 'error');
    }
}

async function handleGenerateVaultSummary() {
    try {
        const res = await fetch('/api/reports/vault');
        const data = await res.json();
        document.getElementById('statement-text').textContent = data.summary;
    } catch (err) {
        showToast(err.message, 'error');
    }
}

// ==========================================
// 11. UI MODALS & TOASTS
// ==========================
function openModal(modalId) {
    document.getElementById(modalId).classList.remove('hidden');
}

function closeModal(modalId) {
    document.getElementById(modalId).classList.add('hidden');
}

function showToast(message, type = 'info') {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;

    let icon = 'fa-circle-info';
    if (type === 'success') icon = 'fa-circle-check';
    if (type === 'error') icon = 'fa-triangle-exclamation';

    toast.innerHTML = `<i class="fa-solid ${icon}"></i> <span>${message}</span>`;
    container.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateX(100%)';
        setTimeout(() => toast.remove(), 300);
    }, 4500);
}
