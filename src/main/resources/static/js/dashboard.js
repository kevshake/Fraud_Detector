document.addEventListener('DOMContentLoaded', function () {
    // --- Profile Dropdown ---
    const profileTrigger = document.getElementById('userProfileTrigger');
    const profileDropdown = document.getElementById('profileDropdown');

    if (profileTrigger) {
        profileTrigger.addEventListener('click', (e) => {
            e.stopPropagation();
            profileDropdown.classList.toggle('show');
        });
    }

    window.addEventListener('click', () => {
        if (profileDropdown) profileDropdown.classList.remove('show');
    });

    // Navigation
    const navItems = document.querySelectorAll('.nav-item');
    const dashboardView = document.getElementById('dashboard-view');
    const userView = document.getElementById('user-management-view');
    const roleView = document.getElementById('role-management-view');

    // Unified routing logic
    const allViews = [
        'dashboard-view', 'user-management-view', 'role-management-view',
        'cases-view', 'case-detail-view', 'case-timeline-view', 'case-network-view',
        'sar-view', 'alerts-view', 'merchants-view',
        'transactions-view', 'screening-view', 'profile-view',
        'messages-view', 'settings-view', 'reports-view', 'audit-view',
        'risk-analytics-view', 'compliance-calendar-view', 'regulatory-reports-view',
        'limits-aml-view', 'transaction-monitoring-view'
    ];

    function showView(viewId) {
        allViews.forEach(v => {
            const el = document.getElementById(v);
            if (el) el.style.display = 'none';
        });

        const targetEl = document.getElementById(viewId);
        if (targetEl) {
            targetEl.style.display = 'block';
            // Specific load logic for each view
            if (viewId === 'user-management-view') fetchUsers();
            if (viewId === 'role-management-view') fetchRoles();
            if (viewId === 'cases-view') fetchCases();
            if (viewId === 'merchants-view') fetchMerchants();
            if (viewId === 'sar-view') fetchSarReports();
            if (viewId === 'audit-view') fetchAuditLogs();
            if (viewId === 'transactions-view') fetchTransactions();
            if (viewId === 'reports-view') fetchReports();
            if (viewId === 'alerts-view') fetchAlerts();
            if (viewId === 'screening-view') initScreeningView();
            if (viewId === 'profile-view') fetchUserProfile();
            if (viewId === 'messages-view') fetchMessages();
            if (viewId === 'settings-view') fetchSettings();
            if (viewId === 'risk-analytics-view') loadRiskAnalytics();
            if (viewId === 'compliance-calendar-view') loadComplianceCalendar();
            if (viewId === 'case-network-view') loadNetworkGraph();
            if (viewId === 'limits-aml-view') initLimitsAmlManagement();
            if (viewId === 'transaction-monitoring-view') initTransactionMonitoring();
        }
    }

    // Attach listeners to all nav items and subitems
    document.querySelectorAll('.nav-item, .nav-subitem').forEach(item => {
        item.addEventListener('click', (e) => {
            const viewName = item.getAttribute('data-view');
            if (viewName) {
                e.preventDefault();

                // Construct view ID correctly
                let viewId = viewName + '-view';
                if (viewName === 'users') viewId = 'user-management-view';
                if (viewName === 'roles') viewId = 'role-management-view';
                if (viewName === 'sar') viewId = 'sar-view';
                if (viewName === 'audit') viewId = 'audit-view';
                if (viewName === 'case-timeline') viewId = 'case-timeline-view';
                if (viewName === 'case-network') viewId = 'case-network-view';
                if (viewName === 'case-queues') viewId = 'cases-view'; // TODO: Create queue view
                if (viewName === 'risk-analytics') viewId = 'risk-analytics-view';
                if (viewName === 'compliance-calendar') viewId = 'compliance-calendar-view';
                if (viewName === 'regulatory-reports') viewId = 'regulatory-reports-view';

                showView(viewId);

                // Update active state
                document.querySelectorAll('.nav-item, .nav-subitem').forEach(n => n.classList.remove('active'));
                item.classList.add('active');

                // If it's a subitem, also highlight the parent
                const parentNav = item.closest('.nav-section')?.querySelector('.nav-item');
                if (parentNav) parentNav.classList.add('active');
            }
        });
    });

    // --- User Management ---

    window.openAddUserModal = function () {
        fetchRolesForSelect();
        fetchPspsForSelect();
        document.getElementById('addUserModal').style.display = 'block';
    }

    window.closeAddUserModal = function () {
        document.getElementById('addUserModal').style.display = 'none';
    }


    // --- Role Management ---

    window.openAddRoleModal = function () {
        fetchPermissions();
        fetchPspsForSelect();
        document.getElementById('addRoleModal').style.display = 'block';
    }
    window.closeAddRoleModal = function () {
        document.getElementById('addRoleModal').style.display = 'none';
    }



    // Initial load
    if (window.location.hash === '#users') {
        showView('user-management-view');
        fetchUsers();
    }

    // --- Case Management ---

    function getRiskClass(score) {
        if (score > 80) return 'high';
        if (score > 40) return 'medium';
        return 'low';
    }

    function getStatusClass(status) {
        if (!status) return 'pending';
        const s = status.toString().toUpperCase();
        if (s === 'INVESTIGATING' || s === 'IN_PROGRESS') return 'investigating';
        if (s === 'RESOLVED' || s === 'COMPLETED') return 'resolved';
        if (s === 'NEW' || s === 'OPEN') return 'pending';
        return 'escalated';
    }

    function demoCases() {
        const tbody = document.querySelector('#cases-table tbody');
        if (!tbody) return;
        tbody.innerHTML = `
            <tr>
                <td><span class="case-id">CASE-2024-1247</span></td>
                <td>TechFlow Inc.</td>
                <td><span class="risk-badge high">92</span></td>
                <td><span class="status-badge investigating">Investigating</span></td>
                <td>John Smith</td>
                <td>2 hours ago</td>
                <td>
                    <button class="action-btn"><i class="fas fa-eye"></i></button>
                </td>
            </tr>
            <tr>
                <td><span class="case-id">CASE-2024-1248</span></td>
                <td>Global Pay Ltd.</td>
                <td><span class="risk-badge medium">45</span></td>
                <td><span class="status-badge pending">New</span></td>
                <td>Unassigned</td>
                <td>5 hours ago</td>
                <td>
                    <button class="action-btn"><i class="fas fa-eye"></i></button>
                </td>
            </tr>
        `;
    }

    // --- Merchant Management ---

    // --- Sanctions & Fraud Metrics ---
    window.fetchSanctionsStatus = function () {
        // Assuming an endpoint exists or showing mock for now
        fetch('/api/v1/dashboard/sanctions/status')
            .then(res => res.json())
            .then(data => {
                document.getElementById('lastSanctionsRun').innerText = data.lastRun || 'Today, 02:00';
                document.getElementById('sanctionsRunStatus').innerText = data.status || 'Success';
            })
            .catch(() => {
                document.getElementById('lastSanctionsRun').innerText = 'Today, 02:00';
                document.getElementById('sanctionsRunStatus').innerText = 'Success';
            });
    };

    window.fetchFraudMetrics = function () {
        // Assuming an endpoint exists or showing mock for now
        fetch('/api/v1/dashboard/fraud-metrics')
            .then(res => res.json())
            .then(data => {
                if (data.precision) document.getElementById('metricPrecision').innerText = data.precision;
                if (data.recall) document.getElementById('metricRecall').innerText = data.recall;
                if (data.f1) document.getElementById('metricF1').innerText = data.f1;
            })
            .catch(() => {
                // Keep default values if fetch fails
            });
    };

    // --- Chart Initialization ---
    let riskPieChart = null;
    let transactionChart = null;

    function initCharts() {
        // Risk Pie Chart
        const riskCtx = document.getElementById('riskPieChart');
        if (riskCtx && typeof Chart !== 'undefined') {
            if (riskPieChart) riskPieChart.destroy();
            riskPieChart = new Chart(riskCtx, {
                type: 'pie',
                data: {
                    labels: ['High', 'Medium', 'Low'],
                    datasets: [{
                        data: [15, 35, 50],
                        backgroundColor: ['#e74c3c', '#f39c12', '#27ae60'],
                        borderWidth: 0
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: { display: false }
                    }
                }
            });
        }

        // Transaction Volume Chart
        const txCtx = document.getElementById('transactionChart');
        if (txCtx && typeof Chart !== 'undefined') {
            if (transactionChart) transactionChart.destroy();
            const labels = [];
            const data = [];
            for (let i = 6; i >= 0; i--) {
                const date = new Date();
                date.setDate(date.getDate() - i);
                labels.push(date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' }));
                data.push(Math.floor(Math.random() * 50000) + 10000);
            }

            transactionChart = new Chart(txCtx, {
                type: 'line',
                data: {
                    labels: labels,
                    datasets: [{
                        label: 'Transactions',
                        data: data,
                        borderColor: '#6c5ce7',
                        backgroundColor: 'rgba(108, 92, 231, 0.1)',
                        tension: 0.4,
                        fill: true
                    }]
                },
                options: {
                    responsive: true,
                    maintainAspectRatio: false,
                    plugins: {
                        legend: { display: false }
                    },
                    scales: {
                        y: {
                            beginAtZero: true,
                            ticks: { color: 'rgba(255, 255, 255, 0.7)' },
                            grid: { color: 'rgba(255, 255, 255, 0.1)' }
                        },
                        x: {
                            ticks: { color: 'rgba(255, 255, 255, 0.7)' },
                            grid: { color: 'rgba(255, 255, 255, 0.1)' }
                        }
                    }
                }
            });
        }
    }

    // --- CSRF Token Helper ---
    function getCsrfToken() {
        const cookies = document.cookie.split(';');
        for (let cookie of cookies) {
            const [name, value] = cookie.trim().split('=');
            if (name === 'XSRF-TOKEN') {
                return decodeURIComponent(value);
            }
        }
        return null;
    }

    function getFetchOptions(method = 'GET', body = null) {
        const options = {
            method: method,
            headers: {
                'Content-Type': 'application/json',
                'X-Requested-With': 'XMLHttpRequest'
            },
            credentials: 'include'
        };

        if (method !== 'GET' && method !== 'HEAD') {
            const csrfToken = getCsrfToken();
            if (csrfToken) {
                options.headers['X-XSRF-TOKEN'] = csrfToken;
            }
        }

        if (body) {
            options.body = typeof body === 'string' ? body : JSON.stringify(body);
        }

        return options;
    }

    // --- Enhanced Error Handling ---
    function handleApiError(err, context) {
        console.error(`Error in ${context}:`, err);
        
        // Check if error is due to session expiration
        if (err && (err.status === 401 || err.status === 403)) {
            if (window.sessionManager) {
                window.sessionManager.handleSessionExpired();
            } else {
                window.location.href = '/login.html?expired=true';
            }
            return;
        }
        
        // Could show a toast notification here for other errors
    }

    // Update all fetch calls to use getFetchOptions
    window.fetchUsers = function () {
        fetch('/api/v1/users', getFetchOptions())
            .then(res => {
                if (!res.ok) throw new Error(`HTTP ${res.status}`);
                return res.json();
            })
            .then(users => {
                const tbody = document.querySelector('#users-table tbody');
                if (!tbody) return;
                tbody.innerHTML = '';
                users.forEach(user => {
                    const tr = document.createElement('tr');
                    tr.innerHTML = `
                        <td>${user.userId || user.id || 'N/A'}</td>
                        <td>${user.username || 'N/A'}</td>
                        <td>${user.email || 'N/A'}</td>
                        <td><span class="badge badge-info">${user.role ? user.role.name : 'N/A'}</span></td>
                        <td>${user.psp ? (user.psp.tradingName || user.psp.legalName) : 'System'}</td>
                        <td><span class="status-badge ${user.enabled ? 'resolved' : 'escalated'}">${user.enabled ? 'Active' : 'Disabled'}</span></td>
                        <td>
                            <button class="action-btn"><i class="fas fa-edit"></i></button>
                            <button class="action-btn"><i class="fas fa-trash"></i></button>
                        </td>
                    `;
                    tbody.appendChild(tr);
                });
            })
            .catch(err => handleApiError(err, 'fetchUsers'));
    };

    window.fetchRoles = function () {
        fetch('/api/v1/roles', getFetchOptions())
            .then(res => {
                if (!res.ok) throw new Error(`HTTP ${res.status}`);
                return res.json();
            })
            .then(roles => {
                const tbody = document.querySelector('#roles-table tbody');
                if (!tbody) return;
                tbody.innerHTML = '';
                roles.forEach(role => {
                    const tr = document.createElement('tr');
                    tr.innerHTML = `
                        <td>${role.name || 'N/A'}</td>
                        <td>${role.description || ''}</td>
                        <td>${role.psp ? (role.psp.tradingName || role.psp.legalName) : 'Global (System)'}</td>
                        <td>${role.permissions ? role.permissions.length + ' permissions' : '0 permissions'}</td>
                        <td>
                            <button class="action-btn"><i class="fas fa-edit"></i></button>
                        </td>
                    `;
                    tbody.appendChild(tr);
                });
            })
            .catch(err => handleApiError(err, 'fetchRoles'));
    };

    // Update form submissions to use CSRF
    const addUserForm = document.getElementById('addUserForm');
    if (addUserForm) {
        addUserForm.addEventListener('submit', function (e) {
            e.preventDefault();
            const formData = new FormData(e.target);
            const data = Object.fromEntries(formData.entries());

            fetch('/api/v1/users', getFetchOptions('POST', data))
                .then(res => {
                    if (res.ok) {
                        closeAddUserModal();
                        fetchUsers();
                        e.target.reset();
                    } else {
                        return res.text().then(text => {
                            throw new Error(text || 'Failed to create user');
                        });
                    }
                })
                .catch(err => {
                    handleApiError(err, 'createUser');
                    alert('Failed to create user: ' + err.message);
                });
        });
    }

    const addRoleForm = document.getElementById('addRoleForm');
    if (addRoleForm) {
        addRoleForm.addEventListener('submit', function (e) {
            e.preventDefault();
            const formData = new FormData(e.target);
            const data = Object.fromEntries(formData.entries());

            // Handle permissions checkbox array
            const permissions = [];
            document.querySelectorAll('input[name="permissions"]:checked').forEach(cb => {
                permissions.push(cb.value);
            });
            data.permissions = permissions;

            fetch('/api/v1/roles', getFetchOptions('POST', data))
                .then(res => {
                    if (res.ok) {
                        closeAddRoleModal();
                        fetchRoles();
                        e.target.reset();
                    } else {
                        return res.text().then(text => {
                            throw new Error(text || 'Failed to create role');
                        });
                    }
                })
                .catch(err => {
                    handleApiError(err, 'createRole');
                    alert('Failed to create role: ' + err.message);
                });
        });
    }

    // Modal close handlers
    window.addEventListener('click', function (e) {
        const addUserModal = document.getElementById('addUserModal');
        const addRoleModal = document.getElementById('addRoleModal');
        if (e.target === addUserModal) closeAddUserModal();
        if (e.target === addRoleModal) closeAddRoleModal();
    });

    // Update fetch calls to use getFetchOptions
    function fetchRolesForSelect() {
        fetch('/api/v1/roles', getFetchOptions())
            .then(res => res.json())
            .then(roles => {
                const select = document.getElementById('userRoleSelect');
                if (!select) return;
                select.innerHTML = '';
                roles.forEach(role => {
                    const opt = document.createElement('option');
                    opt.value = role.id;
                    opt.innerText = role.name;
                    select.appendChild(opt);
                });
            })
            .catch(err => handleApiError(err, 'fetchRolesForSelect'));
    }

    function fetchPspsForSelect() {
        fetch('/api/v1/admin/psp', getFetchOptions())
            .then(res => res.json())
            .then(psps => {
                const userSelect = document.getElementById('userPspSelect');
                const roleSelect = document.getElementById('rolePspSelect');

                if (userSelect) {
                    while (userSelect.options.length > 1) userSelect.remove(1);
                    psps.forEach(psp => {
                        const opt = document.createElement('option');
                        opt.value = psp.id || psp.pspId;
                        opt.innerText = psp.tradingName || psp.legalName;
                        userSelect.appendChild(opt);
                    });
                }

                if (roleSelect) {
                    while (roleSelect.options.length > 1) roleSelect.remove(1);
                    psps.forEach(psp => {
                        const opt = document.createElement('option');
                        opt.value = psp.id || psp.pspId;
                        opt.innerText = psp.tradingName || psp.legalName;
                        roleSelect.appendChild(opt);
                    });
                }
            })
            .catch(err => console.log('Could not fetch PSPs:', err));
    }

    function fetchPermissions() {
        fetch('/api/v1/auth/permissions', getFetchOptions())
            .then(res => res.json())
            .then(perms => {
                const container = document.getElementById('permissionsCheckboxes');
                if (!container) return;
                container.innerHTML = '';
                perms.forEach(perm => {
                    const div = document.createElement('div');
                    div.className = 'checkbox-item';
                    div.innerHTML = `
                        <input type="checkbox" name="permissions" value="${perm}" id="perm_${perm}">
                        <label for="perm_${perm}">${perm}</label>
                    `;
                    container.appendChild(div);
                });
            })
            .catch(err => handleApiError(err, 'fetchPermissions'));
    }

    window.fetchCases = function () {
        fetch('/api/v1/compliance/cases', getFetchOptions())
            .then(res => {
                if (!res.ok) throw new Error(`HTTP ${res.status}`);
                return res.json();
            })
            .then(cases => {
                const tbody = document.querySelector('#cases-table tbody');
                if (!tbody) return;
                tbody.innerHTML = '';
                if (!cases || cases.length === 0) {
                    demoCases();
                    return;
                }
                cases.forEach(c => {
                    const tr = document.createElement('tr');
                    tr.innerHTML = `
                        <td><span class="case-id">${c.caseReference || ('CASE-' + c.id)}</span></td>
                        <td>${c.merchant ? (c.merchant.legalName || c.merchant.tradingName) : 'N/A'}</td>
                        <td><span class="risk-badge ${getRiskClass(c.riskScore)}">${c.riskScore || 0}</span></td>
                        <td><span class="status-badge ${getStatusClass(c.status)}">${c.status || 'N/A'}</span></td>
                        <td>${c.assignedTo ? (c.assignedTo.username || c.assignedTo.firstName) : 'Unassigned'}</td>
                        <td>${c.createdAt ? new Date(c.createdAt).toLocaleDateString() : 'N/A'}</td>
                        <td>
                            <button class="action-btn"><i class="fas fa-eye"></i></button>
                        </td>
                    `;
                    tbody.appendChild(tr);
                });
            })
            .catch(err => {
                console.error('Error fetching cases:', err);
                demoCases();
            });
    };

    window.fetchMerchants = function () {
        fetch('/api/v1/merchants', getFetchOptions())
            .then(res => {
                if (!res.ok) throw new Error(`HTTP ${res.status}`);
                return res.json();
            })
            .then(merchants => {
                const tbody = document.querySelector('#merchants-table tbody');
                if (!tbody) return;
                tbody.innerHTML = '';
                if (!merchants || merchants.length === 0) {
                    tbody.innerHTML = '<tr><td colspan="9" style="text-align:center;">No merchants found.</td></tr>';
                    return;
                }
                merchants.forEach(m => {
                    const usagePercent = m.dailyLimit > 0 ? (m.currentUsage / m.dailyLimit) * 100 : 0;
                    const usageColor = usagePercent > 90 ? '#e74c3c' : (usagePercent > 70 ? '#f39c12' : '#27ae60');

                    const tr = document.createElement('tr');
                    tr.innerHTML = `
                        <td>
                            <div class="merchant-info">
                                <div class="merchant-name">${m.tradingName || m.legalName || 'N/A'}</div>
                                <div class="merchant-email">${m.contactEmail || ''}</div>
                                <div class="merchant-category">${m.businessType || ''}</div>
                            </div>
                        </td>
                        <td>
                            <div class="mcc-info">
                                <div class="mcc-code">${m.mcc || 'N/A'}</div>
                                <div class="mcc-desc">${m.mccDescription || 'Unknown Category'}</div>
                            </div>
                        </td>
                        <td><span class="status-badge ${getRiskClassForLevel(m.riskLevel)}">${m.riskLevel || 'UNKNOWN'}</span></td>
                        <td><span class="status-badge ${getKycStatusClass(m.kycStatus)}">${m.kycStatus || 'PENDING'}</span></td>
                        <td><span class="status-badge ${getContractStatusClass(m.contractStatus)}">${m.contractStatus || 'NO_CONTRACT'}</span></td>
                        <td>${formatCurrency(m.dailyLimit || 0)}</td>
                        <td>
                            <div class="usage-container">
                                <div class="usage-text">${formatCurrency(m.currentUsage || 0)} (${usagePercent.toFixed(1)}%)</div>
                                <div class="usage-bar-bg">
                                    <div class="usage-bar-fill" style="width: ${Math.min(usagePercent, 100)}%; background-color: ${usageColor}"></div>
                                </div>
                            </div>
                        </td>
                        <td><span class="status-badge resolved">${m.status || 'ACTIVE'}</span></td>
                        <td>
                            <div class="action-btns">
                                <button class="action-btn" title="View Details"><i class="fas fa-eye"></i></button>
                                <button class="action-btn" title="Edit"><i class="fas fa-edit"></i></button>
                                <button class="action-btn" title="Settings"><i class="fas fa-cog"></i></button>
                                <button class="action-btn accent" title="Force Manual Settlement"><i class="fas fa-hand-holding-usd"></i></button>
                            </div>
                        </td>
                    `;
                    tbody.appendChild(tr);
                });
            })
            .catch(err => {
                const tbody = document.querySelector('#merchants-table tbody');
                if (tbody) tbody.innerHTML = '<tr><td colspan="9" style="text-align:center;">Error loading merchants.</td></tr>';
                handleApiError(err, 'fetchMerchants');
            });
    };

    // --- Dashboard Statistics ---
    window.fetchDashboardStats = function () {
        // Fetch reporting summary for dashboard
        fetch('/api/v1/reporting/summary-detailed/summary', getFetchOptions())
            .then(res => {
                if (!res.ok) throw new Error(`HTTP ${res.status}`);
                return res.json();
            })
            .then(summary => {
                // Update cases today count
                if (summary.casesLast7d) {
                    const today = new Date().toISOString().split('T')[0];
                    const todayCount = summary.casesLast7d[today] || 0;
                    const casesTodayEl = document.getElementById('casesToday'); // Use ID now
                    if (casesTodayEl) casesTodayEl.innerText = todayCount;
                }

                // Update New Merchant Stats
                if (summary.totalMerchants !== undefined) document.getElementById('totalMerchants').innerText = summary.totalMerchants;
                if (summary.activeMerchants !== undefined) {
                    document.getElementById('activeMerchants').innerText = summary.activeMerchants;
                    const percent = summary.totalMerchants > 0 ? Math.round((summary.activeMerchants / summary.totalMerchants) * 100) : 0;
                    document.getElementById('activeMerchantsPercent').innerText = `${percent}% of total`;
                }
                if (summary.highRiskMerchants !== undefined) document.getElementById('highRiskMerchants').innerText = summary.highRiskMerchants;
                if (summary.totalVolume !== undefined) document.getElementById('totalVolume').innerText = formatCurrency(summary.totalVolume);

                // Update risk breakdown chart if data available
                if (summary.casesByStatus) {
                    // Could update pie chart here with real data
                }
            })
            .catch(err => {
                // Silently fail - keep default values
                console.log('Could not fetch dashboard stats:', err);
            });
    };

    // Initialize charts when dashboard view is shown
    const originalShowView = showView;
    showView = function (viewId) {
        originalShowView(viewId);
        if (viewId === 'dashboard-view') {
            setTimeout(() => {
                initCharts();
                fetchDashboardStats();
            }, 100); // Small delay to ensure DOM is ready
        }
    };

    // --- SAR Reports Management ---
    window.fetchSarReports = function () {
        fetch('/api/v1/compliance/sar', getFetchOptions())
            .then(res => {
                if (!res.ok) throw new Error(`HTTP ${res.status}`);
                return res.json();
            })
            .then(sars => {
                const container = document.querySelector('#sar-view .table-card');
                if (!container) return;
                if (!sars || sars.length === 0) {
                    container.innerHTML = '<p>No SAR reports available.</p>';
                    return;
                }
                let html = '<table class="data-table"><thead><tr><th>SAR Reference</th><th>Status</th><th>Type</th><th>Jurisdiction</th><th>Created</th><th>Actions</th></tr></thead><tbody>';
                sars.forEach(sar => {
                    html += `<tr>
                        <td><span class="case-id">${sar.sarReference || 'SAR-' + sar.id}</span></td>
                        <td><span class="status-badge ${getStatusClass(sar.status)}">${sar.status || 'DRAFT'}</span></td>
                        <td>${sar.sarType || 'N/A'}</td>
                        <td>${sar.jurisdiction || 'N/A'}</td>
                        <td>${sar.createdAt ? new Date(sar.createdAt).toLocaleDateString() : 'N/A'}</td>
                        <td><button class="action-btn"><i class="fas fa-eye"></i></button></td>
                    </tr>`;
                });
                html += '</tbody></table>';
                container.innerHTML = html;
            })
            .catch(err => {
                const container = document.querySelector('#sar-view .table-card');
                if (container) container.innerHTML = '<p>Error loading SAR reports. Please try again later.</p>';
                handleApiError(err, 'fetchSarReports');
            });
    };

    // --- Audit Logs Management ---
    window.fetchAuditLogs = function () {
        fetch('/api/v1/audit/logs?limit=100', getFetchOptions())
            .then(res => {
                if (!res.ok) throw new Error(`HTTP ${res.status}`);
                return res.json();
            })
            .then(logs => {
                const container = document.querySelector('#audit-view .table-card');
                if (!container) return;
                if (!logs || logs.length === 0) {
                    container.innerHTML = '<p>No audit logs found.</p>';
                    return;
                }
                let html = '<table class="data-table"><thead><tr><th>Timestamp</th><th>User</th><th>Action</th><th>Entity Type</th><th>Entity ID</th><th>Details</th></tr></thead><tbody>';
                logs.forEach(log => {
                    html += `<tr>
                        <td>${log.timestamp ? new Date(log.timestamp).toLocaleString() : 'N/A'}</td>
                        <td>${log.username || 'System'}</td>
                        <td><span class="badge badge-info">${log.actionType || 'N/A'}</span></td>
                        <td>${log.entityType || 'N/A'}</td>
                        <td>${log.entityId || 'N/A'}</td>
                        <td>${log.details || ''}</td>
                    </tr>`;
                });
                html += '</tbody></table>';
                container.innerHTML = html;
            })
            .catch(err => {
                const container = document.querySelector('#audit-view .table-card');
                if (container) container.innerHTML = '<p>Error loading audit logs. Please try again later.</p>';
                handleApiError(err, 'fetchAuditLogs');
            });
    };

    // --- Transactions Management ---
    window.fetchTransactions = function () {
        fetch('/api/v1/transactions?limit=100', getFetchOptions())
            .then(res => {
                if (!res.ok) throw new Error(`HTTP ${res.status}`);
                return res.json();
            })
            .then(transactions => {
                const container = document.querySelector('#transactions-view .table-card');
                if (!container) return;
                if (!transactions || transactions.length === 0) {
                    container.innerHTML = '<p>No recent transactions found.</p>';
                    return;
                }
                let html = '<table class="data-table"><thead><tr><th>Transaction ID</th><th>Merchant</th><th>Amount</th><th>Currency</th><th>Timestamp</th><th>Status</th></tr></thead><tbody>';
                transactions.forEach(txn => {
                    const amount = txn.amountCents ? (txn.amountCents / 100).toFixed(2) : '0.00';
                    html += `<tr>
                        <td>${txn.txnId || 'N/A'}</td>
                        <td>${txn.merchantId || 'N/A'}</td>
                        <td>${amount}</td>
                        <td>${txn.currency || 'USD'}</td>
                        <td>${txn.txnTs ? new Date(txn.txnTs).toLocaleString() : 'N/A'}</td>
                        <td><span class="status-badge resolved">Processed</span></td>
                    </tr>`;
                });
                html += '</tbody></table>';
                container.innerHTML = html;
            })
            .catch(err => {
                const container = document.querySelector('#transactions-view .table-card');
                if (container) container.innerHTML = '<p>Error loading transactions. Please try again later.</p>';
                handleApiError(err, 'fetchTransactions');
            });
    };

    // --- Reports Management ---
    window.fetchReports = function () {
        fetch('/api/v1/reporting/summary', getFetchOptions())
            .then(res => {
                if (!res.ok) throw new Error(`HTTP ${res.status}`);
                return res.json();
            })
            .then(summary => {
                const container = document.querySelector('#reports-view .table-card');
                if (!container) return;
                let html = '<div class="report-summary">';
                html += '<h3>Summary Statistics</h3>';
                if (summary.casesByStatus) {
                    html += '<h4>Cases by Status:</h4><ul>';
                    for (const [status, count] of Object.entries(summary.casesByStatus)) {
                        html += `<li>${status}: ${count}</li>`;
                    }
                    html += '</ul>';
                }
                if (summary.sarsByStatus) {
                    html += '<h4>SARs by Status:</h4><ul>';
                    for (const [status, count] of Object.entries(summary.sarsByStatus)) {
                        html += `<li>${status}: ${count}</li>`;
                    }
                    html += '</ul>';
                }
                if (summary.auditLast24h !== undefined) {
                    html += `<p><strong>Audit Events (Last 24h):</strong> ${summary.auditLast24h}</p>`;
                }
                html += '</div>';
                container.innerHTML = html;
            })
            .catch(err => {
                const container = document.querySelector('#reports-view .table-card');
                if (container) container.innerHTML = '<p>Error loading reports. Please try again later.</p>';
                handleApiError(err, 'fetchReports');
            });
    };

    // --- Alerts Management ---
    window.fetchAlerts = function () {
        fetch('/api/v1/alerts', getFetchOptions())
            .then(res => {
                if (!res.ok) throw new Error(`HTTP ${res.status}`);
                return res.json();
            })
            .then(alerts => {
                const container = document.querySelector('#alerts-view .alerts-grid');
                if (!container) return;
                if (!alerts || alerts.length === 0) {
                    container.innerHTML = '<div class="alert-card"><p>No active alerts.</p></div>';
                    return;
                }
                let html = '<div class="alert-card"><div class="card-header"><h3><i class="fas fa-bell"></i> Live Alerts</h3></div><div class="alerts-list">';
                alerts.forEach(alert => {
                    const severity = alert.severity || 'INFO';
                    const severityClass = severity.toLowerCase();
                    const timeAgo = alert.createdAt ? getTimeAgo(new Date(alert.createdAt)) : 'Unknown';
                    html += `
                        <div class="alert-item ${severityClass}">
                            <div class="alert-icon"><i class="fas fa-exclamation-triangle"></i></div>
                            <div class="alert-content">
                                <strong>${alert.reason || 'Alert'}</strong>
                                <p>Transaction ID: ${alert.txnId || 'N/A'} | Score: ${alert.score || 'N/A'}</p>
                                <span class="alert-time">${timeAgo}</span>
                            </div>
                        </div>
                    `;
                });
                html += '</div></div>';
                container.innerHTML = html;
            })
            .catch(err => {
                // Fallback to demo alerts if API fails
                const container = document.querySelector('#alerts-view .alerts-grid');
                if (container) {
                    container.innerHTML = `
                        <div class="alert-card">
                            <div class="card-header">
                                <h3><i class="fas fa-bell"></i> Live Alerts</h3>
                            </div>
                            <div class="alerts-list">
                                <div class="alert-item high">
                                    <div class="alert-icon"><i class="fas fa-exclamation-triangle"></i></div>
                                    <div class="alert-content">
                                        <strong>High-Risk Transaction Detected</strong>
                                        <p>Unusual pattern detected - requires review</p>
                                        <span class="alert-time">Just now</span>
                                    </div>
                                </div>
                            </div>
                        </div>
                    `;
                }
                handleApiError(err, 'fetchAlerts');
            });
    };

    // --- User Profile ---
    window.fetchUserProfile = function () {
        fetch('/api/v1/users/me', getFetchOptions())
            .then(res => res.json())
            .then(user => {
                const container = document.getElementById('profile-content');
                if (!container) return;
                container.innerHTML = `
                    <div class="profile-card">
                        <div class="profile-header">
                            <div class="profile-avatar">${user.firstName?.charAt(0) || 'A'}</div>
                            <div class="profile-info">
                                <h2>${user.firstName} ${user.lastName}</h2>
                                <p class="profile-email">${user.email}</p>
                            </div>
                        </div>
                        <div class="profile-details">
                            <div class="detail-item"><strong>Username:</strong> ${user.username}</div>
                            <div class="detail-item"><strong>Role:</strong> ${user.role?.name || 'N/A'}</div>
                            <div class="detail-item"><strong>PSP:</strong> ${user.psp?.legalName || 'System Global'}</div>
                        </div>
                    </div>
                `;
            })
            .catch(err => {
                const container = document.getElementById('profile-content');
                if (container) container.innerHTML = '<p>Error loading profile details.</p>';
            });
    };

    // --- Messages Management ---
    window.fetchMessages = function (unreadOnly = false) {
        fetch(`/api/v1/messages?unreadOnly=${unreadOnly}`, getFetchOptions())
            .then(res => res.json())
            .then(messages => {
                const container = document.getElementById('messages-content');
                if (!container) return;

                if (!messages || messages.length === 0) {
                    container.innerHTML = '<p>No internal messages.</p>';
                    return;
                }

                let html = '<div class="messages-list">';
                messages.forEach(msg => {
                    const timeAgo = getTimeAgo(new Date(msg.timestamp));
                    html += `
                        <div class="message-item ${msg.read ? '' : 'unread'}">
                            <div class="message-meta">
                                <strong>From: ${msg.from}</strong>
                                <span class="message-time">${timeAgo}</span>
                            </div>
                            <div class="message-subject">${msg.subject}</div>
                            <div class="message-body">${msg.body}</div>
                        </div>
                    `;
                });
                html += '</div>';
                container.innerHTML = html;
            })
            .catch(err => {
                const container = document.getElementById('messages-content');
                if (container) container.innerHTML = '<p>Error loading messages.</p>';
            });
    };

    // --- Settings Management ---
    window.fetchSettings = function () {
        fetch('/api/v1/settings', getFetchOptions())
            .then(res => res.json())
            .then(settings => {
                const container = document.getElementById('settings-content');
                if (!container) return;

                let html = '<div class="settings-grid">';
                for (const [key, value] of Object.entries(settings)) {
                    html += `
                        <div class="setting-item">
                            <label>${key.charAt(0).toUpperCase() + key.slice(1)}</label>
                            <input type="${typeof value === 'boolean' ? 'checkbox' : 'text'}" 
                                   value="${value}" ${value === true ? 'checked' : ''} readonly>
                        </div>
                    `;
                }
                html += '</div>';
                container.innerHTML = html;
            })
            .catch(err => {
                const container = document.getElementById('settings-content');
                if (container) container.innerHTML = '<p>Error loading settings.</p>';
            });
    };

    // Helper function to calculate time ago
    function getTimeAgo(date) {
        const now = new Date();
        const diffMs = now - date;
        const diffMins = Math.floor(diffMs / 60000);
        const diffHours = Math.floor(diffMs / 3600000);
        const diffDays = Math.floor(diffMs / 86400000);

        if (diffMins < 1) return 'Just now';
        if (diffMins < 60) return `${diffMins} minute${diffMins > 1 ? 's' : ''} ago`;
        if (diffHours < 24) return `${diffHours} hour${diffHours > 1 ? 's' : ''} ago`;
        return `${diffDays} day${diffDays > 1 ? 's' : ''} ago`;
    }

    // --- Screening View Initialization ---
    window.initScreeningView = function () {
        const form = document.querySelector('#screening-view form');
        if (form && !form.getAttribute('data-init')) {
            form.setAttribute('data-init', 'true');
            form.addEventListener('submit', (e) => {
                e.preventDefault();
                const formData = new FormData(form);
                const data = {
                    name: formData.get('name'),
                    entityType: formData.get('entityType')
                };

                const resultsDiv = document.getElementById('screening-results');
                const contentDiv = document.getElementById('screening-results-content');

                resultsDiv.style.display = 'block';
                contentDiv.innerHTML = '<p>Processing screening request...</p>';

                fetch('/api/v1/sanctions/screen', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(data)
                })
                    .then(res => res.json())
                    .then(result => {
                        if (result.hits && result.hits.length > 0) {
                            let html = `<div class="screening-hit high"><strong>HIT FOUND: ${result.hits.length} matches</strong></div>`;
                            result.hits.forEach(hit => {
                                html += `
                                <div class="hit-detail">
                                    <p><strong>Name:</strong> ${hit.matchedName}</p>
                                    <p><strong>List:</strong> ${hit.sourceList}</p>
                                    <p><strong>Reason:</strong> ${hit.reason || 'N/A'}</p>
                                </div>
                            `;
                            });
                            contentDiv.innerHTML = html;
                        } else {
                            contentDiv.innerHTML = '<div class="screening-hit low"><strong>NO HITS FOUND</strong> - Clear</div>';
                        }
                    })
                    .catch(err => {
                        contentDiv.innerHTML = '<p class="error">Error performing screening. Please try again.</p>';
                    });
            });
        }
    };

    window.performScreening = function (event) {
        event.preventDefault();
        const form = event.target;
        const formData = new FormData(form);
        const entityType = formData.get('entityType');
        const name = formData.get('name');

        if (!name || !name.trim()) {
            alert('Please enter a name to screen');
            return;
        }

        const requestBody = {
            name: name.trim(),
            entityType: entityType
        };

        fetch('/api/v1/sanctions/screen', getFetchOptions('POST', requestBody))
            .then(res => {
                if (!res.ok) throw new Error(`HTTP ${res.status}`);
                return res.json();
            })
            .then(result => {
                const resultsDiv = document.getElementById('screening-results');
                const contentDiv = document.getElementById('screening-results-content');
                if (!resultsDiv || !contentDiv) return;

                let html = '<div class="screening-result">';
                html += `<h4>Results for: ${name}</h4>`;
                html += `<p><strong>Status:</strong> <span class="status-badge ${result.match ? 'high' : 'resolved'}">${result.match ? 'MATCH FOUND' : 'NO MATCH'}</span></p>`;

                if (result.match && result.matches && result.matches.length > 0) {
                    html += '<h5>Matches:</h5><ul>';
                    result.matches.forEach(match => {
                        html += `<li><strong>${match.name || 'N/A'}</strong> - ${match.list || 'Unknown List'}</li>`;
                    });
                    html += '</ul>';
                } else {
                    html += '<p>No matches found in sanctions lists.</p>';
                }

                if (result.confidence) {
                    html += `<p><strong>Confidence:</strong> ${(result.confidence * 100).toFixed(2)}%</p>`;
                }

                html += '</div>';
                contentDiv.innerHTML = html;
                resultsDiv.style.display = 'block';
            })
            .catch(err => {
                alert('Error performing screening. Please try again.');
                handleApiError(err, 'performScreening');
            });
    };

    // --- Profile View ---
    window.fetchUserProfile = function () {
        fetch('/api/v1/users/me', getFetchOptions())
            .then(res => {
                if (!res.ok) throw new Error(`HTTP ${res.status}`);
                return res.json();
            })
            .then(user => {
                const container = document.getElementById('profile-content');
                if (!container) return;

                let html = '<div class="profile-info">';
                html += '<h3>User Information</h3>';
                html += `<p><strong>Username:</strong> ${user.username || 'N/A'}</p>`;
                html += `<p><strong>Email:</strong> ${user.email || 'N/A'}</p>`;
                html += `<p><strong>Name:</strong> ${(user.firstName || '') + ' ' + (user.lastName || '') || 'N/A'}</p>`;
                html += `<p><strong>Role:</strong> ${user.role ? (user.role.name || 'N/A') : 'N/A'}</p>`;
                if (user.psp) {
                    html += `<p><strong>PSP:</strong> ${user.psp.legalName || user.psp.tradingName || 'N/A'}</p>`;
                }
                html += `<p><strong>Status:</strong> <span class="status-badge ${user.enabled ? 'resolved' : 'high'}">${user.enabled ? 'Active' : 'Inactive'}</span></p>`;
                html += `<p><strong>Created:</strong> ${user.createdAt ? new Date(user.createdAt).toLocaleDateString() : 'N/A'}</p>`;
                html += '</div>';
                container.innerHTML = html;
            })
            .catch(err => {
                const container = document.getElementById('profile-content');
                if (container) {
                    container.innerHTML = '<p>Error loading profile. Please try again later.</p>';
                }
                handleApiError(err, 'fetchUserProfile');
            });
    };

    // --- Messages View ---
    window.fetchMessages = function (unreadOnly = false) {
        const url = unreadOnly ? '/api/v1/messages?unreadOnly=true' : '/api/v1/messages';
        fetch(url, getFetchOptions())
            .then(res => {
                if (!res.ok) throw new Error(`HTTP ${res.status}`);
                return res.json();
            })
            .then(messages => {
                const container = document.getElementById('messages-content');
                if (!container) return;

                if (!messages || messages.length === 0) {
                    container.innerHTML = '<p>No messages found.</p>';
                    return;
                }

                let html = '<table class="data-table"><thead><tr><th>From</th><th>Subject</th><th>Time</th><th>Status</th><th>Actions</th></tr></thead><tbody>';
                messages.forEach(msg => {
                    const timestamp = msg.timestamp ? new Date(msg.timestamp).toLocaleString() : 'N/A';
                    const readClass = msg.read ? 'read' : 'unread';
                    html += `<tr class="${readClass}">
                        <td>${msg.from || 'System'}</td>
                        <td><strong>${msg.subject || 'No Subject'}</strong></td>
                        <td>${timestamp}</td>
                        <td><span class="status-badge ${msg.read ? 'resolved' : 'high'}">${msg.read ? 'Read' : 'Unread'}</span></td>
                        <td><button class="action-btn" onclick="viewMessage(${msg.id})"><i class="fas fa-eye"></i></button></td>
                    </tr>`;
                });
                html += '</tbody></table>';
                container.innerHTML = html;
            })
            .catch(err => {
                const container = document.getElementById('messages-content');
                if (container) {
                    container.innerHTML = '<p>Error loading messages. Please try again later.</p>';
                }
                handleApiError(err, 'fetchMessages');
            });
    };

    window.viewMessage = function (messageId) {
        // In a real implementation, this would show message details
        alert('Message details view - ID: ' + messageId);
    };

    // --- Settings View ---
    window.fetchSettings = function () {
        fetch('/api/v1/settings', getFetchOptions())
            .then(res => {
                if (!res.ok) throw new Error(`HTTP ${res.status}`);
                return res.json();
            })
            .then(settings => {
                const container = document.getElementById('settings-content');
                if (!container) return;

                let html = '<div class="settings-form">';
                html += '<h3>Application Settings</h3>';
                html += '<form id="settings-form" onsubmit="saveSettings(event)">';
                html += `<div class="form-group">
                    <label for="theme">Theme:</label>
                    <select id="theme" name="theme">
                        <option value="light" ${settings.theme === 'light' ? 'selected' : ''}>Light</option>
                        <option value="dark" ${settings.theme === 'dark' ? 'selected' : ''}>Dark</option>
                    </select>
                </div>`;
                html += `<div class="form-group">
                    <label><input type="checkbox" name="notifications" ${settings.notifications ? 'checked' : ''}> Enable Notifications</label>
                </div>`;
                html += `<div class="form-group">
                    <label><input type="checkbox" name="autoRefresh" ${settings.autoRefresh ? 'checked' : ''}> Auto Refresh</label>
                </div>`;
                html += `<div class="form-group">
                    <label for="refreshInterval">Refresh Interval (seconds):</label>
                    <input type="number" id="refreshInterval" name="refreshInterval" value="${settings.refreshInterval || 30}" min="10" max="300">
                </div>`;
                html += `<div class="form-group">
                    <label for="itemsPerPage">Items Per Page:</label>
                    <input type="number" id="itemsPerPage" name="itemsPerPage" value="${settings.itemsPerPage || 50}" min="10" max="200">
                </div>`;
                html += '<button type="submit" class="btn-primary"><i class="fas fa-save"></i> Save Settings</button>';
                html += '</form>';
                html += '</div>';
                container.innerHTML = html;
            })
            .catch(err => {
                const container = document.getElementById('settings-content');
                if (container) {
                    container.innerHTML = '<p>Error loading settings. Please try again later.</p>';
                }
                handleApiError(err, 'fetchSettings');
            });
    };

    window.saveSettings = function (event) {
        event.preventDefault();
        const form = event.target;
        const formData = new FormData(form);
        const settings = {
            theme: formData.get('theme'),
            notifications: formData.has('notifications'),
            autoRefresh: formData.has('autoRefresh'),
            refreshInterval: parseInt(formData.get('refreshInterval')),
            itemsPerPage: parseInt(formData.get('itemsPerPage'))
        };

        fetch('/api/v1/settings', getFetchOptions('PUT', settings))
            .then(res => {
                if (!res.ok) throw new Error(`HTTP ${res.status}`);
                return res.json();
            })
            .then(() => {
                alert('Settings saved successfully!');
            })
            .catch(err => {
                alert('Error saving settings. Please try again.');
                handleApiError(err, 'saveSettings');
            });
    };

    // --- Helper Functions ---
    function formatCurrency(amount) {
        return new Intl.NumberFormat('en-US', { style: 'currency', currency: 'USD' }).format(amount);
    }

    function getRiskClassForLevel(level) {
        if (!level) return 'pending';
        switch (level.toUpperCase()) {
            case 'CRITICAL':
            case 'HIGH': return 'high';
            case 'MEDIUM': return 'medium';
            case 'LOW': return 'resolved';
            default: return 'pending';
        }
    }

    function getKycStatusClass(status) {
        if (!status) return 'pending';
        switch (status.toUpperCase()) {
            case 'APPROVED': return 'resolved';
            case 'REJECTED': return 'escalated';
            case 'PENDING':
            case 'UNDER_REVIEW': return 'investigating';
            default: return 'pending';
        }
    }

    function getContractStatusClass(status) {
        if (!status) return 'pending';
        switch (status.toUpperCase()) {
            case 'ACTIVE': return 'resolved';
            case 'EXPIRED': return 'escalated';
            case 'NO_CONTRACT': return 'pending';
            default: return 'pending';
        }
    }

    // Logout handler
    window.handleLogout = function(event) {
        event.preventDefault();
        if (window.sessionManager) {
            window.sessionManager.destroy();
        }
        // Invalidate session on backend
        fetch('/api/v1/auth/session/invalidate', {
            method: 'POST',
            credentials: 'include'
        }).finally(() => {
            window.location.href = '/logout';
        });
    };

    // Load initial data and initialize charts
    fetchSanctionsStatus();
    initCharts();
});
