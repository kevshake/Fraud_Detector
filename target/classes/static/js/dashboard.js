// #region agent log - Global error handler
window.addEventListener('error', function (e) {
    fetch('http://127.0.0.1:7243/ingest/328b5c76-f5f4-4b08-a869-a5ae08fcc3e6', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ location: 'dashboard.js:1', message: 'Global JavaScript error', data: { message: e.message, filename: e.filename, lineno: e.lineno, error: e.error ? e.error.toString() : null }, timestamp: Date.now(), sessionId: 'debug-session', runId: 'run1', hypothesisId: 'H' }) }).catch(() => { });
});
// #endregion

document.addEventListener('DOMContentLoaded', function () {
    // #region agent log - Check function availability after DOM load
    setTimeout(function () {
        fetch('http://127.0.0.1:7243/ingest/328b5c76-f5f4-4b08-a869-a5ae08fcc3e6', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ location: 'dashboard.js:2', message: 'Function availability check', data: { editCase: typeof window.editCase, deleteCase: typeof window.deleteCase, editMerchant: typeof window.editMerchant, deleteMerchant: typeof window.deleteMerchant, getFetchOptions: typeof getFetchOptions }, timestamp: Date.now(), sessionId: 'debug-session', runId: 'run1', hypothesisId: 'I' }) }).catch(() => { });
    }, 1000);
    // #endregion
    // --- Currency Formatting (global, shared across pages) ---
    (function initCurrencyFormatter() {
        if (window.currencyFormatter) return;

        const DEFAULT_CURRENCY = 'USD';
        const DEFAULT_LOCALE = (typeof navigator !== 'undefined' && navigator.language) ? navigator.language : 'en-US';

        function normalizeCurrency(code) {
            if (code == null) return null;
            const raw = String(code).trim().toUpperCase();
            if (!raw) return null;
            if (raw.length === 3) return raw;
            const match = raw.match(/[A-Z]{3}/);
            return match ? match[0] : null;
        }

        function setDefaultCurrency(code) {
            const normalized = normalizeCurrency(code) || DEFAULT_CURRENCY;
            window.appDefaultCurrency = normalized;
            return normalized;
        }

        function getDefaultCurrency() {
            return normalizeCurrency(window.appDefaultCurrency) || DEFAULT_CURRENCY;
        }

        function parseAmount(amount) {
            if (amount == null || amount === '') return 0;
            if (typeof amount === 'number') return Number.isFinite(amount) ? amount : 0;
            if (typeof amount === 'bigint') return Number(amount);
            if (typeof amount === 'string') {
                const cleaned = amount.replace(/,/g, '').trim();
                const num = parseFloat(cleaned);
                return Number.isFinite(num) ? num : 0;
            }
            const num = Number(amount);
            return Number.isFinite(num) ? num : 0;
        }

        function format(amount, currencyCode, opts) {
            const options = opts || {};
            const locale = options.locale || DEFAULT_LOCALE;
            const currency = normalizeCurrency(currencyCode) || getDefaultCurrency();
            const num = parseAmount(amount);

            const minimumFractionDigits = typeof options.minimumFractionDigits === 'number' ? options.minimumFractionDigits : 2;
            const maximumFractionDigits = typeof options.maximumFractionDigits === 'number' ? options.maximumFractionDigits : 2;
            const currencyDisplay = options.currencyDisplay || 'narrowSymbol';

            try {
                return new Intl.NumberFormat(locale, {
                    style: 'currency',
                    currency,
                    currencyDisplay,
                    minimumFractionDigits,
                    maximumFractionDigits
                }).format(num);
            } catch (e) {
                try {
                    return new Intl.NumberFormat(locale, {
                        style: 'currency',
                        currency: DEFAULT_CURRENCY,
                        minimumFractionDigits,
                        maximumFractionDigits
                    }).format(num);
                } catch (e2) {
                    return `${currency || DEFAULT_CURRENCY} ${num.toFixed(maximumFractionDigits)}`;
                }
            }
        }

        function formatCompact(amount, currencyCode, opts) {
            const options = opts || {};
            const currency = normalizeCurrency(currencyCode) || getDefaultCurrency();
            const num = parseAmount(amount);

            if (num === 0) {
                return format(0, currency, { ...options, minimumFractionDigits: 0, maximumFractionDigits: 0 });
            }

            const abs = Math.abs(num);
            if (abs >= 1000000) {
                return format(num / 1000000, currency, { ...options, minimumFractionDigits: 1, maximumFractionDigits: 1 }) + 'M';
            }
            if (abs >= 1000) {
                return format(num / 1000, currency, { ...options, minimumFractionDigits: 0, maximumFractionDigits: 0 }) + 'K';
            }
            return format(num, currency, { ...options, minimumFractionDigits: 2, maximumFractionDigits: 2 });
        }

        window.currencyFormatter = {
            normalizeCurrency,
            setDefaultCurrency,
            getDefaultCurrency,
            parseAmount,
            format,
            formatCompact
        };

        // Ensure a default exists immediately
        setDefaultCurrency(window.appDefaultCurrency);
    })();

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
        'cases-view', 'case-detail-view', 'case-timeline-view', 'case-network-view', 'case-queues-view',
        'sar-view', 'alerts-view', 'merchants-view',
        'transactions-view', 'screening-view', 'profile-view',
        'messages-view', 'settings-view', 'reports-view', 'audit-view',
        'risk-analytics-view', 'compliance-calendar-view', 'regulatory-reports-view',
        'limits-aml-view', 'transaction-monitoring-live-view', 'transaction-monitoring-analytics-view',
        'transaction-monitoring-sars-view', 'transaction-monitoring-reports-view'
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
            if (viewId === 'dashboard-view') {
                fetchLiveAlerts();
                fetchRecentActivity();
                fetchSanctionsStatus();
            }
            if (viewId === 'user-management-view') fetchUsers();
            if (viewId === 'role-management-view') fetchRoles();
            if (viewId === 'cases-view') fetchCases();
            if (viewId === 'merchants-view') fetchMerchants();
            if (viewId === 'sar-view') fetchSarReports();
            if (viewId === 'audit-view') fetchAuditLogs();
            if (viewId === 'reports-view') fetchReports();
            if (viewId === 'alerts-view') fetchAlerts();
            if (viewId === 'screening-view') initScreeningView();
            if (viewId === 'profile-view') fetchUserProfile();
            if (viewId === 'messages-view') fetchMessages();
            if (viewId === 'settings-view') fetchSettings();
            if (viewId === 'risk-analytics-view') loadRiskAnalytics();
            if (viewId === 'compliance-calendar-view') loadComplianceCalendar();
            if (viewId === 'case-network-view') {
                initializeNetworkGraphView();
            }
            if (viewId === 'case-timeline-view') {
                if (typeof initializeTimelineView === 'function') {
                    initializeTimelineView();
                }
                if (typeof loadTimelineCases === 'function') {
                    loadTimelineCases();
                }
            }
            if (viewId === 'case-queues-view') {
                if (typeof loadCaseQueuesView === 'function') {
                    loadCaseQueuesView();
                }
            }
            if (viewId === 'limits-aml-view') initLimitsAmlManagement();
            if (viewId === 'transaction-monitoring-live-view' ||
                viewId === 'transaction-monitoring-analytics-view' ||
                viewId === 'transaction-monitoring-sars-view' ||
                viewId === 'transaction-monitoring-reports-view') {
                initTransactionMonitoring();
            }
        }
    }

    // Attach listeners to all nav items and subitems
    document.querySelectorAll('.nav-item, .nav-subitem').forEach(item => {
        item.addEventListener('click', (e) => {
            const viewName = item.getAttribute('data-view');
            if (viewName) {
                e.preventDefault();
                e.stopPropagation(); // Prevent parent nav-item click handler from firing

                // Construct view ID correctly
                let viewId = viewName + '-view';
                if (viewName === 'users') viewId = 'user-management-view';
                if (viewName === 'roles') viewId = 'role-management-view';
                if (viewName === 'sar') viewId = 'sar-view';
                if (viewName === 'audit') viewId = 'audit-view';
                if (viewName === 'case-timeline') viewId = 'case-timeline-view';
                if (viewName === 'case-network') viewId = 'case-network-view';
                if (viewName === 'case-queues') viewId = 'case-queues-view';
                if (viewName === 'risk-analytics') viewId = 'risk-analytics-view';
                if (viewName === 'compliance-calendar') viewId = 'compliance-calendar-view';
                if (viewName === 'regulatory-reports') viewId = 'regulatory-reports-view';
                if (viewName === 'transaction-monitoring-live') viewId = 'transaction-monitoring-live-view';
                if (viewName === 'transaction-monitoring-analytics') viewId = 'transaction-monitoring-analytics-view';
                if (viewName === 'transaction-monitoring-sars') viewId = 'transaction-monitoring-sars-view';
                if (viewName === 'transaction-monitoring-reports') viewId = 'transaction-monitoring-reports-view';
                if (viewName === 'transaction-monitoring') viewId = 'transaction-monitoring-live-view'; // Default to Live Monitoring

                // If it's a subitem, ensure the submenu is expanded
                if (item.classList.contains('nav-subitem')) {
                    const navSection = item.closest('.nav-section');
                    const submenu = navSection?.querySelector('.nav-submenu');
                    const expandIcon = navSection?.querySelector('.expand-icon');

                    if (submenu && !submenu.classList.contains('show')) {
                        // Close other open submenus (accordion behavior)
                        document.querySelectorAll('.nav-submenu.show').forEach(openMenu => {
                            if (openMenu !== submenu) {
                                openMenu.classList.remove('show');
                                const otherIcon = openMenu.closest('.nav-section')?.querySelector('.expand-icon');
                                if (otherIcon) otherIcon.style.transform = 'rotate(0deg)';
                            }
                        });

                        // Expand this submenu
                        submenu.classList.add('show');
                        if (expandIcon) {
                            expandIcon.style.transform = 'rotate(180deg)';
                            expandIcon.style.transition = 'transform 0.2s ease';
                        }
                    }
                }

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

    // --- Accordion Toggle for Sidebar Nav Sections ---
    document.querySelectorAll('.nav-section .nav-item').forEach(navItem => {
        const expandIcon = navItem.querySelector('.expand-icon');
        if (expandIcon) {
            navItem.addEventListener('click', (e) => {
                // Don't toggle if clicking on a subitem (let subitem handler handle it)
                if (e.target.closest('.nav-subitem')) {
                    return;
                }

                // Only toggle if clicking on the parent nav-item with expand icon
                const navSection = navItem.closest('.nav-section');
                const submenu = navSection?.querySelector('.nav-submenu');

                if (submenu) {
                    e.preventDefault();
                    e.stopPropagation();

                    // Close other open submenus (accordion behavior)
                    document.querySelectorAll('.nav-submenu.show').forEach(openMenu => {
                        if (openMenu !== submenu) {
                            openMenu.classList.remove('show');
                            const otherIcon = openMenu.closest('.nav-section')?.querySelector('.expand-icon');
                            if (otherIcon) otherIcon.style.transform = 'rotate(0deg)';
                        }
                    });

                    // Toggle current submenu
                    submenu.classList.toggle('show');
                    expandIcon.style.transform = submenu.classList.contains('show') ? 'rotate(180deg)' : 'rotate(0deg)';
                    expandIcon.style.transition = 'transform 0.2s ease';
                }
            });
        }
    });

    // --- Merchant Management ---
    window.openAddMerchantModal = function () {
        document.getElementById('addMerchantModal').style.display = 'block';
    };

    window.closeAddMerchantModal = function () {
        document.getElementById('addMerchantModal').style.display = 'none';
    };

    // Handle Add Merchant form submission
    const addMerchantForm = document.getElementById('addMerchantForm');
    if (addMerchantForm) {
        addMerchantForm.addEventListener('submit', function (e) {
            e.preventDefault();
            const formData = new FormData(e.target);
            const data = Object.fromEntries(formData.entries());

            fetch('merchants', getFetchOptions('POST', data))
                .then(res => {
                    if (res.ok) {
                        closeAddMerchantModal();
                        fetchMerchants();
                        e.target.reset();
                        alert('Merchant created successfully!');
                    } else {
                        return res.text().then(text => {
                            throw new Error(text || 'Failed to create merchant');
                        });
                    }
                })
                .catch(err => {
                    handleApiError(err, 'createMerchant');
                    alert('Failed to create merchant: ' + err.message);
                });
        });
    }

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
        fetch('dashboard/sanctions/status')
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
        fetch('dashboard/fraud-metrics')
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
    let caseAgingHeatmapChart = null;
    let alertDispositionChart = null;
    let geographicRiskMap = null;
    let geographicRiskMarkers = [];

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

        // Transaction Volume Chart - Fetch real data from backend
        const txCtx = document.getElementById('transactionChart');
        if (txCtx && typeof Chart !== 'undefined') {
            if (transactionChart) transactionChart.destroy();

            // Initialize chart with empty data first
            transactionChart = new Chart(txCtx, {
                type: 'line',
                data: {
                    labels: [],
                    datasets: [{
                        label: 'Transactions',
                        data: [],
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

            // Fetch real transaction volume data from backend
            fetch('dashboard/transaction-volume?days=7', getFetchOptions())
                .then(res => {
                    if (!res.ok) throw new Error(`HTTP ${res.status}`);
                    return res.json();
                })
                .then(volumeData => {
                    if (volumeData && volumeData.labels && volumeData.data && transactionChart) {
                        transactionChart.data.labels = volumeData.labels;
                        transactionChart.data.datasets[0].data = volumeData.data;
                        transactionChart.update();
                    }
                })
                .catch(err => {
                    console.error('Error fetching transaction volume:', err);
                    // Fallback to empty chart or show error message
                });
        }

        // Case Aging Heatmap Chart
        const agingCtx = document.getElementById('caseAgingHeatmapChart');
        if (agingCtx && typeof Chart !== 'undefined') {
            if (caseAgingHeatmapChart) caseAgingHeatmapChart.destroy();

            fetch('dashboard/case-aging', getFetchOptions())
                .then(res => res.json())
                .then(data => {
                    const agingData = data.agingDistribution || {};
                    caseAgingHeatmapChart = new Chart(agingCtx, {
                        type: 'bar',
                        data: {
                            labels: ['0-7 days', '8-14 days', '15-30 days', '31-60 days', '60+ days'],
                            datasets: [{
                                label: 'Cases',
                                data: [
                                    agingData['0-7'] || 0,
                                    agingData['8-14'] || 0,
                                    agingData['15-30'] || 0,
                                    agingData['31-60'] || 0,
                                    agingData['60+'] || 0
                                ],
                                backgroundColor: [
                                    'rgba(39, 174, 96, 0.8)',   // Green for new
                                    'rgba(241, 196, 15, 0.8)',  // Yellow
                                    'rgba(243, 156, 18, 0.8)', // Orange
                                    'rgba(231, 76, 60, 0.8)',   // Red
                                    'rgba(192, 57, 43, 0.8)'    // Dark red
                                ],
                                borderColor: [
                                    '#27ae60',
                                    '#f1c40f',
                                    '#f39c12',
                                    '#e74c3c',
                                    '#c0392b'
                                ],
                                borderWidth: 1
                            }]
                        },
                        options: {
                            responsive: true,
                            maintainAspectRatio: false,
                            plugins: {
                                legend: { display: false },
                                title: {
                                    display: true,
                                    text: 'Case Aging Distribution',
                                    color: 'rgba(255, 255, 255, 0.9)'
                                }
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
                })
                .catch(err => {
                    console.error('Error loading case aging data:', err);
                    // Initialize with empty data
                    caseAgingHeatmapChart = new Chart(agingCtx, {
                        type: 'bar',
                        data: {
                            labels: ['0-7 days', '8-14 days', '15-30 days', '31-60 days', '60+ days'],
                            datasets: [{ data: [0, 0, 0, 0, 0] }]
                        },
                        options: {
                            responsive: true,
                            maintainAspectRatio: false,
                            plugins: { legend: { display: false } }
                        }
                    });
                });
        }

        // Alert Disposition Rates Chart
        const dispositionCtx = document.getElementById('alertDispositionChart');
        if (dispositionCtx && typeof Chart !== 'undefined') {
            if (alertDispositionChart) alertDispositionChart.destroy();

            fetch('alerts/disposition-stats', getFetchOptions())
                .then(res => res.json())
                .then(data => {
                    const distribution = data.distribution || {};
                    alertDispositionChart = new Chart(dispositionCtx, {
                        type: 'doughnut',
                        data: {
                            labels: ['False Positive', 'True Positive', 'SAR Filed', 'Escalated', 'Pending'],
                            datasets: [{
                                data: [
                                    distribution['FALSE_POSITIVE'] || 0,
                                    distribution['TRUE_POSITIVE'] || 0,
                                    distribution['TRUE_POSITIVE_SAR_FILED'] || 0,
                                    distribution['ESCALATED'] || 0,
                                    distribution['PENDING'] || 0
                                ],
                                backgroundColor: [
                                    '#95a5a6',  // Gray for false positive
                                    '#27ae60',  // Green for true positive
                                    '#3498db',  // Blue for SAR filed
                                    '#f39c12',  // Orange for escalated
                                    '#e74c3c'   // Red for pending
                                ],
                                borderWidth: 2,
                                borderColor: 'rgba(255, 255, 255, 0.1)'
                            }]
                        },
                        options: {
                            responsive: true,
                            maintainAspectRatio: false,
                            plugins: {
                                legend: {
                                    position: 'right',
                                    labels: { color: 'rgba(255, 255, 255, 0.9)' }
                                },
                                title: {
                                    display: true,
                                    text: 'Alert Disposition Rates',
                                    color: 'rgba(255, 255, 255, 0.9)'
                                }
                            }
                        }
                    });
                })
                .catch(err => {
                    console.error('Error loading alert disposition data:', err);
                    // Initialize with empty data
                    alertDispositionChart = new Chart(dispositionCtx, {
                        type: 'doughnut',
                        data: {
                            labels: ['False Positive', 'True Positive', 'SAR Filed', 'Escalated', 'Pending'],
                            datasets: [{ data: [0, 0, 0, 0, 0] }]
                        },
                        options: {
                            responsive: true,
                            maintainAspectRatio: false,
                            plugins: { legend: { position: 'right' } }
                        }
                    });
                });
        }

        // Initialize Geographic Risk Heatmap Map
        initGeographicRiskMap();
    }

    // Initialize Geographic Risk Heatmap Map
    function initGeographicRiskMap() {
        const mapContainer = document.getElementById('geographicRiskMap');
        if (!mapContainer || typeof L === 'undefined') {
            console.warn('Map container or Leaflet not available');
            return;
        }

        // Destroy existing map if it exists
        if (geographicRiskMap) {
            geographicRiskMap.remove();
            geographicRiskMap = null;
        }

        // Initialize map centered on world view
        geographicRiskMap = L.map('geographicRiskMap', {
            center: [20, 0],
            zoom: 2,
            minZoom: 2,
            maxZoom: 6,
            worldCopyJump: true
        });

        // Add OpenStreetMap tile layer
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
            attribution: '© OpenStreetMap contributors',
            maxZoom: 19
        }).addTo(geographicRiskMap);

        // Load geographic risk data
        loadGeographicRiskData();

        // Add event listener for period change
        const periodSelect = document.getElementById('geographicRiskPeriod');
        if (periodSelect) {
            periodSelect.addEventListener('change', loadGeographicRiskData);
        }
    }

    // Load Geographic Risk Data from API
    function loadGeographicRiskData() {
        const period = document.getElementById('geographicRiskPeriod')?.value || 90;
        const endDate = new Date();
        const startDate = new Date();
        startDate.setDate(startDate.getDate() - parseInt(period));

        fetch(`analytics/risk/heatmap/geographic?startDate=${startDate.toISOString()}&endDate=${endDate.toISOString()}`, getFetchOptions())
            .then(async res => {
                if (!res.ok) {
                    await handleFetchError(res, 'loadGeographicRiskData');
                    return null;
                }
                return res.json();
            })
            .then(data => {
                if (data) {
                    renderGeographicRiskMap(data);
                }
            })
            .catch(err => {
                console.error('Error loading geographic risk data:', err);
                handleApiError(err, 'loadGeographicRiskData');
            });
    }

    // Render Geographic Risk on Map
    function renderGeographicRiskMap(heatmapData) {
        if (!geographicRiskMap) {
            initGeographicRiskMap();
            return;
        }

        // Clear existing markers
        geographicRiskMarkers.forEach(marker => geographicRiskMap.removeLayer(marker));
        geographicRiskMarkers = [];

        // Country coordinates mapping (ISO 3166-1 alpha-3 to lat/lng)
        const countryCoordinates = getCountryCoordinates();

        // Process heatmap data
        const countries = Object.entries(heatmapData);
        let maxRiskScore = 0;
        let maxCaseCount = 0;

        // Find max values for normalization
        countries.forEach(([countryCode, data]) => {
            if (data.averageRiskScore > maxRiskScore) maxRiskScore = data.averageRiskScore;
            if (data.caseCount > maxCaseCount) maxCaseCount = data.caseCount;
        });

        // Add markers for each country
        countries.forEach(([countryCode, data]) => {
            const coords = countryCoordinates[countryCode];
            if (!coords) {
                console.warn(`Coordinates not found for country: ${countryCode}`);
                return;
            }

            // Calculate color based on risk score
            const riskScore = data.averageRiskScore || 0;
            const color = getRiskColor(riskScore);
            const opacity = Math.max(0.3, Math.min(0.9, riskScore));

            // Create circle marker
            const marker = L.circleMarker(coords, {
                radius: Math.max(5, Math.min(30, (data.caseCount / maxCaseCount) * 30)),
                fillColor: color,
                color: '#fff',
                weight: 2,
                opacity: 1,
                fillOpacity: opacity
            });

            // Create popup with country details
            const popupContent = `
                <div style="min-width: 200px;">
                    <h4 style="margin: 0 0 10px 0; color: #2c3e50;">${getCountryName(countryCode)}</h4>
                    <div style="margin-bottom: 8px;">
                        <strong>Country Code:</strong> ${countryCode}<br>
                        <strong>Risk Score:</strong> <span style="color: ${color}; font-weight: bold;">${(riskScore * 100).toFixed(1)}%</span><br>
                        <strong>Case Count:</strong> ${data.caseCount}<br>
                        <strong>Average Risk:</strong> ${(data.averageRiskScore * 100).toFixed(1)}%
                    </div>
                    <div style="font-size: 0.85em; color: #7f8c8d;">
                        Risk Level: <strong>${getRiskLevel(riskScore)}</strong>
                    </div>
                </div>
            `;

            marker.bindPopup(popupContent);
            marker.addTo(geographicRiskMap);
            geographicRiskMarkers.push(marker);
        });

        // Update legend
        updateGeographicRiskLegend(maxRiskScore);
    }

    // Get risk color based on score
    function getRiskColor(riskScore) {
        if (riskScore >= 0.75) return '#c0392b'; // Dark red - Critical
        if (riskScore >= 0.5) return '#e74c3c';  // Red - High
        if (riskScore >= 0.25) return '#f39c12';  // Orange - Medium
        return '#27ae60';                         // Green - Low
    }

    // Get risk level text
    function getRiskLevel(riskScore) {
        if (riskScore >= 0.75) return 'CRITICAL';
        if (riskScore >= 0.5) return 'HIGH';
        if (riskScore >= 0.25) return 'MEDIUM';
        return 'LOW';
    }

    // Update map legend
    function updateGeographicRiskLegend(maxRiskScore) {
        const legend = document.getElementById('geographicRiskLegend');
        if (!legend) return;

        legend.innerHTML = `
            <div class="legend-title">Risk Levels</div>
            <div class="legend-item">
                <span class="legend-color" style="background-color: #c0392b;"></span>
                <span>Critical (${(maxRiskScore * 0.75 * 100).toFixed(0)}+%)</span>
            </div>
            <div class="legend-item">
                <span class="legend-color" style="background-color: #e74c3c;"></span>
                <span>High (50-75%)</span>
            </div>
            <div class="legend-item">
                <span class="legend-color" style="background-color: #f39c12;"></span>
                <span>Medium (25-50%)</span>
            </div>
            <div class="legend-item">
                <span class="legend-color" style="background-color: #27ae60;"></span>
                <span>Low (0-25%)</span>
            </div>
            <div class="legend-note">Circle size indicates case count</div>
        `;
    }

    // Get country coordinates (ISO 3166-1 alpha-3 codes)
    function getCountryCoordinates() {
        // Common country coordinates - can be expanded
        return {
            'USA': [39.8283, -98.5795],
            'GBR': [55.3781, -3.4360],
            'CAN': [56.1304, -106.3468],
            'AUS': [-25.2744, 133.7751],
            'DEU': [51.1657, 10.4515],
            'FRA': [46.2276, 2.2137],
            'ITA': [41.8719, 12.5674],
            'ESP': [40.4637, -3.7492],
            'NLD': [52.1326, 5.2913],
            'BEL': [50.5039, 4.4699],
            'CHE': [46.8182, 8.2275],
            'AUT': [47.5162, 14.5501],
            'SWE': [60.1282, 18.6435],
            'NOR': [60.4720, 8.4689],
            'DNK': [56.2639, 9.5018],
            'FIN': [61.9241, 25.7482],
            'POL': [51.9194, 19.1451],
            'CZE': [49.8175, 15.4730],
            'HUN': [47.1625, 19.5033],
            'ROU': [45.9432, 24.9668],
            'BGR': [42.7339, 25.4858],
            'GRC': [39.0742, 21.8243],
            'PRT': [39.3999, -8.2245],
            'IRL': [53.4129, -8.2439],
            'JPN': [36.2048, 138.2529],
            'CHN': [35.8617, 104.1954],
            'IND': [20.5937, 78.9629],
            'KOR': [35.9078, 127.7669],
            'SGP': [1.3521, 103.8198],
            'MYS': [4.2105, 101.9758],
            'THA': [15.8700, 100.9925],
            'IDN': [-0.7893, 113.9213],
            'PHL': [12.8797, 121.7740],
            'VNM': [14.0583, 108.2772],
            'BRA': [-14.2350, -51.9253],
            'ARG': [-38.4161, -63.6167],
            'CHL': [-35.6751, -71.5430],
            'COL': [4.5709, -74.2973],
            'MEX': [23.6345, -102.5528],
            'ZAF': [-30.5595, 22.9375],
            'EGY': [26.8206, 30.8025],
            'NGA': [9.0820, 8.6753],
            'KEN': [-0.0236, 37.9062],
            'ARE': [23.4241, 53.8478],
            'SAU': [23.8859, 45.0792],
            'ISR': [31.0461, 34.8516],
            'TUR': [38.9637, 35.2433],
            'RUS': [61.5240, 105.3188],
            'UKR': [48.3794, 31.1656],
            'AFG': [33.9391, 67.7100],
            'IRN': [32.4279, 53.6880],
            'IRQ': [33.2232, 43.6793],
            'SYR': [34.8021, 38.9968],
            'PRK': [40.3399, 127.5101],
            'PAK': [30.3753, 69.3451],
            'BGD': [23.6850, 90.3563],
            'LKA': [7.8731, 80.7718],
            'NZL': [-40.9006, 174.8860]
        };
    }

    // Get country name from code
    function getCountryName(countryCode) {
        const countryNames = {
            'USA': 'United States', 'GBR': 'United Kingdom', 'CAN': 'Canada', 'AUS': 'Australia',
            'DEU': 'Germany', 'FRA': 'France', 'ITA': 'Italy', 'ESP': 'Spain', 'NLD': 'Netherlands',
            'BEL': 'Belgium', 'CHE': 'Switzerland', 'AUT': 'Austria', 'SWE': 'Sweden', 'NOR': 'Norway',
            'DNK': 'Denmark', 'FIN': 'Finland', 'POL': 'Poland', 'CZE': 'Czech Republic', 'HUN': 'Hungary',
            'ROU': 'Romania', 'BGR': 'Bulgaria', 'GRC': 'Greece', 'PRT': 'Portugal', 'IRL': 'Ireland',
            'JPN': 'Japan', 'CHN': 'China', 'IND': 'India', 'KOR': 'South Korea', 'SGP': 'Singapore',
            'MYS': 'Malaysia', 'THA': 'Thailand', 'IDN': 'Indonesia', 'PHL': 'Philippines', 'VNM': 'Vietnam',
            'BRA': 'Brazil', 'ARG': 'Argentina', 'CHL': 'Chile', 'COL': 'Colombia', 'MEX': 'Mexico',
            'ZAF': 'South Africa', 'EGY': 'Egypt', 'NGA': 'Nigeria', 'KEN': 'Kenya', 'ARE': 'UAE',
            'SAU': 'Saudi Arabia', 'ISR': 'Israel', 'TUR': 'Turkey', 'RUS': 'Russia', 'UKR': 'Ukraine',
            'AFG': 'Afghanistan', 'IRN': 'Iran', 'IRQ': 'Iraq', 'SYR': 'Syria', 'PRK': 'North Korea',
            'PAK': 'Pakistan', 'BGD': 'Bangladesh', 'LKA': 'Sri Lanka', 'NZL': 'New Zealand'
        };
        return countryNames[countryCode] || countryCode;
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
    window.getCsrfToken = getCsrfToken;

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
    window.getFetchOptions = getFetchOptions;

    // Best-effort: bootstrap default currency from current user's PSP (falls back to USD)
    (function initDefaultCurrencyFromUserProfile() {
        if (!window.currencyFormatter || typeof window.currencyFormatter.setDefaultCurrency !== 'function') return;
        fetch('users/me', getFetchOptions())
            .then(res => {
                if (!res.ok) throw new Error(`HTTP ${res.status}`);
                return res.json();
            })
            .then(user => {
                const pspCurrency = user && user.psp ? user.psp.currency : null;
                if (pspCurrency) window.currencyFormatter.setDefaultCurrency(pspCurrency);
            })
            .catch(() => {
                // Ignore - keep fallback currency
            });
    })();

    // --- Enhanced Error Handling ---
    function handleApiError(err, context) {
        console.error(`Error in ${context}:`, err);

        // Check if error is due to session expiration
        if (err && (err.status === 401 || err.status === 403)) {
            if (window.sessionManager) {
                window.sessionManager.handleSessionExpired();
            } else {
                window.location.href = 'login.html?expired=true';
            }
            return;
        }

        // Extract error details from response if available
        let errorMessage = 'An error occurred';
        let errorDetails = null;
        let errorCode = null;
        let traceId = null;

        if (err.response) {
            // If err is a Response object from fetch
            err.response.json().then(errorData => {
                errorMessage = errorData.message || errorMessage;
                errorDetails = errorData.details || errorData.errors;
                errorCode = errorData.errorCode;
                traceId = errorData.traceId;
                showErrorNotification(errorMessage, errorDetails, errorCode, traceId, context);
            }).catch(() => {
                showErrorNotification(errorMessage, null, null, null, context);
            });
        } else if (err.message) {
            errorMessage = err.message;
            showErrorNotification(errorMessage, null, null, null, context);
        } else {
            showErrorNotification(errorMessage, null, null, null, context);
        }
    }
    window.handleApiError = handleApiError;

    // Show success notification
    function showSuccessNotification(message) {
        let notificationContainer = document.getElementById('success-notification-container');
        if (!notificationContainer) {
            notificationContainer = document.createElement('div');
            notificationContainer.id = 'success-notification-container';
            notificationContainer.style.cssText = 'position: fixed; top: 20px; right: 20px; z-index: 10000; max-width: 400px;';
            document.body.appendChild(notificationContainer);
        }

        const notificationId = 'success-' + Date.now();
        const notificationHtml = `
            <div id="${notificationId}" class="success-notification" style="
                background: linear-gradient(135deg, rgba(39, 174, 96, 0.95), rgba(46, 213, 115, 0.95));
                color: white;
                padding: 16px 20px;
                border-radius: 8px;
                margin-bottom: 10px;
                box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
                animation: slideInRight 0.3s ease-out;
            ">
                <div style="display: flex; justify-content: space-between; align-items: center;">
                    <div style="flex: 1;">
                        <i class="fas fa-check-circle" style="margin-right: 8px;"></i>
                        <strong>${message}</strong>
                    </div>
                    <button onclick="document.getElementById('${notificationId}').remove()" 
                            style="background: none; border: none; color: white; font-size: 18px; cursor: pointer; padding: 0; margin-left: 10px; opacity: 0.8;">
                        &times;
                    </button>
                </div>
            </div>
        `;
        notificationContainer.insertAdjacentHTML('beforeend', notificationHtml);

        setTimeout(() => {
            const notification = document.getElementById(notificationId);
            if (notification) {
                notification.style.animation = 'slideOutRight 0.3s ease-out';
                setTimeout(() => notification.remove(), 300);
            }
        }, 5000);
    }

    // Show error notification with detailed information
    function showErrorNotification(message, details, errorCode, traceId, context) {
        // Create or get error notification container
        let notificationContainer = document.getElementById('error-notification-container');
        if (!notificationContainer) {
            notificationContainer = document.createElement('div');
            notificationContainer.id = 'error-notification-container';
            notificationContainer.style.cssText = 'position: fixed; top: 20px; right: 20px; z-index: 10000; max-width: 400px;';
            document.body.appendChild(notificationContainer);
        }

        const notificationId = 'error-' + Date.now();
        let notificationHtml = `
            <div id="${notificationId}" class="error-notification" style="
                background: linear-gradient(135deg, rgba(231, 76, 60, 0.95), rgba(192, 57, 43, 0.95));
                color: white;
                padding: 16px 20px;
                border-radius: 8px;
                margin-bottom: 10px;
                box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
                animation: slideInRight 0.3s ease-out;
            ">
                <div style="display: flex; justify-content: space-between; align-items: start; margin-bottom: 8px;">
                    <div style="flex: 1;">
                        <strong style="font-size: 14px; display: block; margin-bottom: 4px;">
                            <i class="fas fa-exclamation-circle" style="margin-right: 6px;"></i>
                            ${context ? context + ' Error' : 'Error'}
                        </strong>
                        <div style="font-size: 13px; opacity: 0.95;">${message}</div>
                    </div>
                    <button onclick="document.getElementById('${notificationId}').remove()" 
                            style="background: none; border: none; color: white; font-size: 18px; cursor: pointer; padding: 0; margin-left: 10px; opacity: 0.8;">
                        &times;
                    </button>
                </div>
        `;

        if (errorCode) {
            notificationHtml += `
                <div style="font-size: 11px; opacity: 0.8; margin-top: 6px;">
                    <strong>Error Code:</strong> ${errorCode}
                </div>
            `;
        }

        if (traceId) {
            notificationHtml += `
                <div style="font-size: 11px; opacity: 0.8; margin-top: 4px;">
                    <strong>Trace ID:</strong> <code style="background: rgba(0,0,0,0.2); padding: 2px 4px; border-radius: 3px;">${traceId}</code>
                </div>
            `;
        }

        if (details) {
            if (typeof details === 'object' && !Array.isArray(details)) {
                if (details.fieldErrors) {
                    notificationHtml += `
                        <div style="margin-top: 8px; padding-top: 8px; border-top: 1px solid rgba(255,255,255,0.2);">
                            <strong style="font-size: 11px; display: block; margin-bottom: 4px;">Field Errors:</strong>
                            <ul style="margin: 0; padding-left: 20px; font-size: 11px; opacity: 0.9;">
                    `;
                    for (const [field, error] of Object.entries(details.fieldErrors)) {
                        notificationHtml += `<li><strong>${field}:</strong> ${error}</li>`;
                    }
                    notificationHtml += `</ul></div>`;
                } else {
                    notificationHtml += `
                        <div style="margin-top: 8px; padding-top: 8px; border-top: 1px solid rgba(255,255,255,0.2); font-size: 11px; opacity: 0.9;">
                            <pre style="margin: 0; white-space: pre-wrap;">${JSON.stringify(details, null, 2)}</pre>
                        </div>
                    `;
                }
            }
        }

        notificationHtml += `</div>`;
        notificationContainer.insertAdjacentHTML('beforeend', notificationHtml);

        // Auto-remove after 8 seconds
        setTimeout(() => {
            const notification = document.getElementById(notificationId);
            if (notification) {
                notification.style.animation = 'slideOutRight 0.3s ease-out';
                setTimeout(() => notification.remove(), 300);
            }
        }, 8000);
    }

    // Enhanced fetch error handler that extracts error details from response
    window.handleFetchError = async function (response, context) {
        let errorData = null;
        try {
            const contentType = response.headers.get('content-type');
            if (contentType && contentType.includes('application/json')) {
                errorData = await response.json();
            }
        } catch (e) {
            console.warn('Could not parse error response as JSON:', e);
        }

        const error = {
            status: response.status,
            statusText: response.statusText,
            message: errorData?.message || `HTTP ${response.status}: ${response.statusText}`,
            response: { json: () => Promise.resolve(errorData || {}) },
            ...errorData
        };

        handleApiError(error, context);
        throw error;
    };

    // Update all fetch calls to use getFetchOptions
    window.fetchUsers = function () {
        fetch('users', getFetchOptions())
            .then(res => {
                if (!res.ok) throw new Error(`HTTP ${res.status}`);
                return res.json();
            })
            .then(users => {
                const tbody = document.querySelector('#users-table tbody');
                if (!tbody) return;
                tbody.innerHTML = '';
                users.forEach(user => {
                    const userId = user.userId || user.id;
                    const tr = document.createElement('tr');
                    tr.innerHTML = `
                        <td>${userId || 'N/A'}</td>
                        <td>${user.username || 'N/A'}</td>
                        <td>${user.email || 'N/A'}</td>
                        <td><span class="badge badge-info">${user.role ? user.role.name : 'N/A'}</span></td>
                        <td>${user.psp ? (user.psp.tradingName || user.psp.legalName) : 'System'}</td>
                        <td><span class="status-badge ${user.enabled ? 'resolved' : 'escalated'}">${user.enabled ? 'Active' : 'Disabled'}</span></td>
                        <td>
                            <button class="action-btn" title="Edit User" onclick="editUser(${userId})"><i class="fas fa-edit"></i></button>
                            <button class="action-btn" title="Delete User" onclick="deleteUser(${userId})"><i class="fas fa-trash"></i></button>
                            <button class="action-btn" title="${user.enabled ? 'Disable' : 'Enable'} User" onclick="toggleUserStatus(${userId}, ${user.enabled})"><i class="fas fa-${user.enabled ? 'ban' : 'check'}"></i></button>
                        </td>
                    `;
                    tbody.appendChild(tr);
                });
            })
            .catch(err => handleApiError(err, 'fetchUsers'));
    };

    // --- Edit User Functions ---
    window.editUser = function (userId) {
        if (!userId) {
            alert('Error: User ID is missing.');
            return;
        }

        fetch(`users/${userId}`, getFetchOptions())
            .then(res => {
                if (!res.ok) throw new Error(`HTTP ${res.status}`);
                return res.json();
            })
            .then(user => {
                // Use the actual user ID from the parameter or response
                const actualUserId = userId || user.id || user.userId;
                if (!actualUserId) {
                    alert('Error: Could not determine user ID.');
                    return;
                }

                document.getElementById('editUserId').value = actualUserId;
                document.getElementById('editUsername').value = user.username || '';
                document.getElementById('editEmail').value = user.email || '';
                document.getElementById('editFirstName').value = user.firstName || '';
                document.getElementById('editLastName').value = user.lastName || '';
                document.getElementById('editPassword').value = ''; // Password field is read-only in edit
                document.getElementById('editUserEnabled').checked = user.enabled !== false;

                // Populate role dropdown
                fetchRolesForEditSelect(user.role?.id);
                fetchPspsForEditSelect(user.psp?.id || user.psp?.pspId);

                // Show the modal
                const modal = document.getElementById('editUserModal');
                if (modal) {
                    modal.style.display = 'block';
                } else {
                    alert('Error: Edit user modal not found.');
                }
            })
            .catch(err => {
                console.error('Error loading user details:', err);
                handleApiError(err, 'editUser');
                alert('Failed to load user details: ' + err.message);
            });
    };

    window.closeEditUserModal = function () {
        document.getElementById('editUserModal').style.display = 'none';
    };

    window.deleteUser = function (userId) {
        if (!userId) {
            alert('Error: User ID is missing.');
            return;
        }

        if (!confirm(`Are you sure you want to delete user ID ${userId}?\n\nThis action cannot be undone and will permanently remove the user from the system.`)) {
            return;
        }

        fetch(`users/${userId}`, getFetchOptions('DELETE'))
            .then(res => {
                if (res.ok) {
                    alert('User deleted successfully');
                    fetchUsers();
                } else {
                    return res.text().then(text => {
                        throw new Error(text || 'Failed to delete user');
                    }).catch(() => {
                        throw new Error('Failed to delete user');
                    });
                }
            })
            .catch(err => {
                console.error('Error deleting user:', err);
                handleApiError(err, 'deleteUser');
                alert('Failed to delete user: ' + err.message);
            });
    };

    window.toggleUserStatus = function (userId, currentStatus) {
        if (!userId) {
            alert('Error: User ID is missing.');
            return;
        }

        const action = currentStatus ? 'disable' : 'enable';
        const actionText = currentStatus ? 'disable' : 'enable';
        const warningText = currentStatus ?
            'Disabling this user will prevent them from logging in and accessing the system.' :
            'Enabling this user will allow them to log in and access the system.';

        if (!confirm(`Are you sure you want to ${actionText} user ID ${userId}?\n\n${warningText}`)) {
            return;
        }

        fetch(`users/${userId}/${action}`, getFetchOptions('POST'))
            .then(res => {
                if (res.ok) {
                    alert(`User ${actionText}d successfully`);
                    fetchUsers();
                } else {
                    return res.text().then(text => {
                        throw new Error(text || `Failed to ${actionText} user`);
                    }).catch(() => {
                        throw new Error(`Failed to ${actionText} user`);
                    });
                }
            })
            .catch(err => {
                console.error(`Error ${actionText}ing user:`, err);
                handleApiError(err, 'toggleUserStatus');
                alert(`Failed to ${actionText} user: ` + err.message);
            });
    };

    function fetchRolesForEditSelect(selectedRoleId) {
        fetch('roles', getFetchOptions())
            .then(res => res.json())
            .then(roles => {
                const select = document.getElementById('editUserRoleSelect');
                if (!select) return;
                select.innerHTML = '';
                roles.forEach(role => {
                    const opt = document.createElement('option');
                    opt.value = role.id;
                    opt.innerText = role.name;
                    if (role.id === selectedRoleId) opt.selected = true;
                    select.appendChild(opt);
                });
            })
            .catch(err => console.log('Could not fetch roles for edit:', err));
    }

    function fetchPspsForEditSelect(selectedPspId) {
        fetch('admin/psp', getFetchOptions())
            .then(res => res.json())
            .then(psps => {
                const select = document.getElementById('editUserPspSelect');
                if (!select) return;
                while (select.options.length > 1) select.remove(1);
                psps.forEach(psp => {
                    const opt = document.createElement('option');
                    opt.value = psp.id || psp.pspId;
                    opt.innerText = psp.tradingName || psp.legalName;
                    if ((psp.id || psp.pspId) === selectedPspId) opt.selected = true;
                    select.appendChild(opt);
                });
            })
            .catch(err => console.log('Could not fetch PSPs for edit:', err));
    }

    // Handle Edit User form submission
    const editUserForm = document.getElementById('editUserForm');
    if (editUserForm) {
        editUserForm.addEventListener('submit', function (e) {
            e.preventDefault();
            const userId = document.getElementById('editUserId').value;
            if (!userId) {
                alert('Error: User ID is missing. Please try again.');
                return;
            }

            // Build data object matching UpdateUserRequest DTO
            const data = {
                firstName: document.getElementById('editFirstName').value || null,
                lastName: document.getElementById('editLastName').value || null,
                email: document.getElementById('editEmail').value || null,
                roleId: document.getElementById('editUserRoleSelect').value ?
                    parseInt(document.getElementById('editUserRoleSelect').value) : null
            };

            // Remove null values
            Object.keys(data).forEach(key => {
                if (data[key] === null || data[key] === '') {
                    delete data[key];
                }
            });

            fetch(`users/${userId}`, getFetchOptions('PUT', data))
                .then(res => {
                    if (res.ok) {
                        closeEditUserModal();
                        fetchUsers();
                        alert('User updated successfully');
                    } else {
                        return res.text().then(text => {
                            throw new Error(text || 'Failed to update user');
                        }).catch(() => {
                            throw new Error('Failed to update user');
                        });
                    }
                })
                .catch(err => {
                    handleApiError(err, 'updateUser');
                    alert('Failed to update user: ' + err.message);
                });
        });
    }

    window.fetchRoles = function () {
        fetch('roles', getFetchOptions())
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
                            <button class="action-btn" title="Edit Role" onclick="editRole(${role.id})"><i class="fas fa-edit"></i></button>
                            <button class="action-btn danger" title="Delete Role" onclick="deleteRole(${role.id})"><i class="fas fa-trash"></i></button>
                        </td>
                    `;
                    tbody.appendChild(tr);
                });
            })
            .catch(err => handleApiError(err, 'fetchRoles'));
    };

    // --- Role Actions ---
    window.editRole = function (id) {
        // Populate and open edit modal
        // Need to add edit role logic similar to edit user
        fetch(`roles/${id}`, getFetchOptions()) // Roles endpoint usually returns list, might need specific endpoint or filter
        // Assuming roles are fetched in list, but we might need single get.
        // Let's implement based on what we have. API analysis showed GET /roles lists all.
        // We might need to iterate or add GET /roles/{id}
        // Actually RoleController doesn't have GET /{id}. 
        // We can find it from the list or add endpoint.
        // For now, let's just show alert
        alert("Edit Role " + id + " - Note: Backend GET /roles/{id} might be missing, checking implementation plan.");
    };

    window.deleteRole = function (id) {
        if (!confirm("Delete role?")) return;
        fetch(`roles/${id}`, getFetchOptions('DELETE'))
            .then(res => {
                if (res.ok) {
                    alert('Role deleted');
                    fetchRoles();
                } else {
                    alert('Failed to delete role');
                }
            });
    };

    // Update form submissions to use CSRF
    const addUserForm = document.getElementById('addUserForm');
    if (addUserForm) {
        addUserForm.addEventListener('submit', function (e) {
            e.preventDefault();
            const formData = new FormData(e.target);
            const data = Object.fromEntries(formData.entries());

            fetch('users', getFetchOptions('POST', data))
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

            fetch('roles', getFetchOptions('POST', data))
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
        fetch('roles', getFetchOptions())
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
        fetch('admin/psp', getFetchOptions())
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
        fetch('auth/permissions', getFetchOptions())
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

    window.fetchCases = function (statusFilter) {
        // Get filter value from dropdown if not provided
        if (statusFilter === undefined) {
            const filterEl = document.getElementById('caseStatusFilter');
            statusFilter = filterEl ? filterEl.value : '';
        }

        // Build URL with filter parameter
        let url = 'compliance/cases';
        if (statusFilter && statusFilter !== '') {
            url += '?status=' + encodeURIComponent(statusFilter);
        }

        fetch(url, getFetchOptions())
            .then(res => {
                if (!res.ok) throw new Error(`HTTP ${res.status}`);
                return res.json();
            })
            .then(cases => {
                // Update the badge count with actual count of cases
                updateCasesCount();

                const tbody = document.querySelector('#cases-table tbody');
                if (!tbody) return;
                tbody.innerHTML = '';
                if (!cases || cases.length === 0) {
                    tbody.innerHTML = '<tr><td colspan="8" style="text-align:center;padding:20px;">No cases found' + (statusFilter ? ' with status: ' + statusFilter : '') + '.</td></tr>';
                    return;
                }
                cases.forEach(c => {
                    const tr = document.createElement('tr');
                    const merchantName = c.merchant ? (c.merchant.legalName || c.merchant.tradingName || c.merchantId) : (c.merchantId || 'N/A');
                    const priority = c.priority || 'MEDIUM';
                    const status = c.status || 'NEW';
                    const assignedTo = c.assignedTo ? (c.assignedTo.username || c.assignedTo.firstName || 'Assigned') : 'Unassigned';
                    const slaDeadline = c.slaDeadline ? new Date(c.slaDeadline).toLocaleDateString() : 'N/A';
                    const daysOpen = c.daysOpen || (c.createdAt ? Math.floor((new Date() - new Date(c.createdAt)) / (1000 * 60 * 60 * 24)) : 0);

                    tr.innerHTML = `
                        <td><span class="case-id">${c.caseReference || ('CASE-' + c.id)}</span></td>
                        <td>${merchantName}</td>
                        <td><span class="priority-badge ${priority.toLowerCase()}">${priority}</span></td>
                        <td><span class="status-badge ${getStatusClass(status)}">${status}</span></td>
                        <td>${assignedTo}</td>
                        <td>${slaDeadline}</td>
                        <td>${daysOpen} days</td>
                        <td>
                            <div class="action-btns">
                                <button class="action-btn" title="View details" onclick="(function(id){fetch('http://127.0.0.1:7243/ingest/328b5c76-f5f4-4b08-a869-a5ae08fcc3e6',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'dashboard.js:1668',message:'viewCaseDetail button clicked',data:{caseId:id,funcExists:typeof viewCaseDetail},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'G'})}).catch(()=>{}); viewCaseDetail(id)})(${c.id})" data-case-id="${c.id}">
                                    <i class="fas fa-eye"></i>
                                </button>
                                <button class="action-btn" title="Edit Case" onclick="(function(id){fetch('http://127.0.0.1:7243/ingest/328b5c76-f5f4-4b08-a869-a5ae08fcc3e6',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'dashboard.js:1671',message:'editCase button clicked',data:{caseId:id,funcExists:typeof editCase},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'G'})}).catch(()=>{}); editCase(id)})(${c.id})" data-case-id="${c.id}">
                                    <i class="fas fa-edit"></i>
                                </button>
                                <button class="action-btn danger" title="Delete Case" onclick="(function(id){fetch('http://127.0.0.1:7243/ingest/328b5c76-f5f4-4b08-a869-a5ae08fcc3e6',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'dashboard.js:1674',message:'deleteCase button clicked',data:{caseId:id,funcExists:typeof deleteCase},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'G'})}).catch(()=>{}); deleteCase(id)})(${c.id})" data-case-id="${c.id}">
                                    <i class="fas fa-trash"></i>
                                </button>
                            </div>
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

    // Filter cases in real-time
    window.filterCases = function () {
        const statusFilter = document.getElementById('caseStatusFilter');
        if (statusFilter) {
            const selectedStatus = statusFilter.value;
            fetchCases(selectedStatus);
        } else {
            fetchCases();
        }
    };

    // Use viewCaseDetail from case-management.js if available, otherwise implement it
    if (typeof window.viewCaseDetail === 'undefined') {
        window.viewCaseDetail = function (caseId) {
            if (typeof showView === 'function') {
                showView('case-detail-view');
            }
            // Load case details
            fetch(`compliance/cases/${caseId}`, getFetchOptions())
                .then(res => {
                    if (!res.ok) throw new Error(`HTTP ${res.status}`);
                    return res.json();
                })
                .then(caseData => {
                    console.log('Case details loaded:', caseData);
                    // Case detail view will be handled by case-management.js if available
                })
                .catch(err => {
                    console.error('Error loading case details:', err);
                    alert('Error loading case details: ' + err.message);
                });
        };
    }

    window.editCase = function (caseId) {
        // #region agent log
        fetch('http://127.0.0.1:7243/ingest/328b5c76-f5f4-4b08-a869-a5ae08fcc3e6', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ location: 'dashboard.js:1723', message: 'editCase called', data: { caseId: caseId, getFetchOptionsExists: typeof getFetchOptions }, timestamp: Date.now(), sessionId: 'debug-session', runId: 'run1', hypothesisId: 'A' }) }).catch(() => { });
        // #endregion
        // Load case data and open edit modal
        try {
            const fetchOpts = getFetchOptions();
            // #region agent log
            fetch('http://127.0.0.1:7243/ingest/328b5c76-f5f4-4b08-a869-a5ae08fcc3e6', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ location: 'dashboard.js:1727', message: 'getFetchOptions result', data: { fetchOpts: fetchOpts ? JSON.stringify(fetchOpts) : null }, timestamp: Date.now(), sessionId: 'debug-session', runId: 'run1', hypothesisId: 'B' }) }).catch(() => { });
            // #endregion
            fetch(`compliance/cases/${caseId}`, fetchOpts)
                .then(res => {
                    if (!res.ok) throw new Error(`HTTP ${res.status}`);
                    return res.json();
                })
                .then(caseData => {
                    // #region agent log
                    fetch('http://127.0.0.1:7243/ingest/328b5c76-f5f4-4b08-a869-a5ae08fcc3e6', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ location: 'dashboard.js:1730', message: 'editCase fetch success', data: { caseId: caseId, hasCaseData: !!caseData }, timestamp: Date.now(), sessionId: 'debug-session', runId: 'run1', hypothesisId: 'C' }) }).catch(() => { });
                    // #endregion
                    // Open edit modal with case data
                    alert('Edit Case functionality - Case ID: ' + caseId + '\n\nThis will open an edit modal with case data.');
                    // TODO: Implement edit modal
                })
                .catch(err => {
                    // #region agent log
                    fetch('http://127.0.0.1:7243/ingest/328b5c76-f5f4-4b08-a869-a5ae08fcc3e6', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ location: 'dashboard.js:1736', message: 'editCase fetch error', data: { caseId: caseId, error: err.message, stack: err.stack }, timestamp: Date.now(), sessionId: 'debug-session', runId: 'run1', hypothesisId: 'D' }) }).catch(() => { });
                    // #endregion
                    console.error('Error loading case for edit:', err);
                    alert('Error loading case: ' + err.message);
                });
        } catch (err) {
            // #region agent log
            fetch('http://127.0.0.1:7243/ingest/328b5c76-f5f4-4b08-a869-a5ae08fcc3e6', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ location: 'dashboard.js:1740', message: 'editCase exception', data: { caseId: caseId, error: err.message, stack: err.stack }, timestamp: Date.now(), sessionId: 'debug-session', runId: 'run1', hypothesisId: 'E' }) }).catch(() => { });
            // #endregion
            alert('Error: ' + err.message);
        }
    };

    window.deleteCase = function (caseId) {
        // #region agent log
        fetch('http://127.0.0.1:7243/ingest/328b5c76-f5f4-4b08-a869-a5ae08fcc3e6', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ location: 'dashboard.js:1741', message: 'deleteCase called', data: { caseId: caseId, getFetchOptionsExists: typeof getFetchOptions }, timestamp: Date.now(), sessionId: 'debug-session', runId: 'run1', hypothesisId: 'A' }) }).catch(() => { });
        // #endregion
        if (!caseId) {
            alert('Error: Case ID is missing.');
            return;
        }

        if (!confirm(`Are you sure you want to delete case ${caseId}?\n\nThis action cannot be undone and will permanently remove the case and all associated data from the system.`)) {
            // #region agent log
            fetch('http://127.0.0.1:7243/ingest/328b5c76-f5f4-4b08-a869-a5ae08fcc3e6', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ location: 'dashboard.js:1747', message: 'deleteCase cancelled', data: { caseId: caseId }, timestamp: Date.now(), sessionId: 'debug-session', runId: 'run1', hypothesisId: 'F' }) }).catch(() => { });
            // #endregion
            return;
        }

        try {
            const fetchOpts = getFetchOptions('DELETE');
            // #region agent log
            fetch('http://127.0.0.1:7243/ingest/328b5c76-f5f4-4b08-a869-a5ae08fcc3e6', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ location: 'dashboard.js:1751', message: 'deleteCase fetch starting', data: { caseId: caseId, fetchOpts: fetchOpts ? JSON.stringify(fetchOpts) : null }, timestamp: Date.now(), sessionId: 'debug-session', runId: 'run1', hypothesisId: 'B' }) }).catch(() => { });
            // #endregion
            fetch(`compliance/cases/${caseId}`, fetchOpts)
                .then(res => {
                    // #region agent log
                    fetch('http://127.0.0.1:7243/ingest/328b5c76-f5f4-4b08-a869-a5ae08fcc3e6', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ location: 'dashboard.js:1752', message: 'deleteCase response received', data: { caseId: caseId, status: res.status, ok: res.ok }, timestamp: Date.now(), sessionId: 'debug-session', runId: 'run1', hypothesisId: 'C' }) }).catch(() => { });
                    // #endregion
                    if (res.ok) {
                        // Show success message
                        const tbody = document.querySelector('#cases-table tbody');
                        if (tbody) {
                            const row = tbody.querySelector(`tr button[data-case-id="${caseId}"]`)?.closest('tr');
                            if (row) {
                                row.style.opacity = '0.5';
                                row.style.transition = 'opacity 0.3s';
                                setTimeout(() => {
                                    fetchCases(); // Refresh the list
                                }, 300);
                            } else {
                                fetchCases(); // Refresh the list
                            }
                        }
                    } else {
                        return res.json().then(err => {
                            throw new Error(err.message || 'Failed to delete case');
                        }).catch(() => {
                            throw new Error('Failed to delete case');
                        });
                    }
                })
                .catch(err => {
                    // #region agent log
                    fetch('http://127.0.0.1:7243/ingest/328b5c76-f5f4-4b08-a869-a5ae08fcc3e6', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ location: 'dashboard.js:1773', message: 'deleteCase error', data: { caseId: caseId, error: err.message, stack: err.stack }, timestamp: Date.now(), sessionId: 'debug-session', runId: 'run1', hypothesisId: 'D' }) }).catch(() => { });
                    // #endregion
                    console.error('Error deleting case:', err);
                    alert('Error deleting case: ' + err.message);
                });
        } catch (err) {
            // #region agent log
            fetch('http://127.0.0.1:7243/ingest/328b5c76-f5f4-4b08-a869-a5ae08fcc3e6', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ location: 'dashboard.js:1778', message: 'deleteCase exception', data: { caseId: caseId, error: err.message, stack: err.stack }, timestamp: Date.now(), sessionId: 'debug-session', runId: 'run1', hypothesisId: 'E' }) }).catch(() => { });
            // #endregion
            alert('Error: ' + err.message);
        }
    };

    window.fetchMerchants = function () {
        fetch('merchants', getFetchOptions())
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
                                <button class="action-btn" title="View Details" onclick="(function(id){fetch('http://127.0.0.1:7243/ingest/328b5c76-f5f4-4b08-a869-a5ae08fcc3e6',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'dashboard.js:1830',message:'viewMerchant button clicked',data:{merchantId:id,funcExists:typeof viewMerchant},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'G'})}).catch(()=>{}); viewMerchant(id)})(${m.merchantId})"><i class="fas fa-eye"></i></button>
                                <button class="action-btn" title="Edit" onclick="(function(id){fetch('http://127.0.0.1:7243/ingest/328b5c76-f5f4-4b08-a869-a5ae08fcc3e6',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'dashboard.js:1831',message:'editMerchant button clicked',data:{merchantId:id,funcExists:typeof editMerchant},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'G'})}).catch(()=>{}); editMerchant(id)})(${m.merchantId})"><i class="fas fa-edit"></i></button>
                                <button class="action-btn" title="Settings" onclick="merchantSettings(${m.merchantId})"><i class="fas fa-cog"></i></button>
                                <button class="action-btn accent" title="Force Manual Settlement" onclick="forceSettlement(${m.merchantId})"><i class="fas fa-hand-holding-usd"></i></button>
                                <button class="action-btn danger" title="Delete Merchant" onclick="(function(id){fetch('http://127.0.0.1:7243/ingest/328b5c76-f5f4-4b08-a869-a5ae08fcc3e6',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({location:'dashboard.js:1834',message:'deleteMerchant button clicked',data:{merchantId:id,funcExists:typeof deleteMerchant},timestamp:Date.now(),sessionId:'debug-session',runId:'run1',hypothesisId:'G'})}).catch(()=>{}); deleteMerchant(id)})(${m.merchantId})"><i class="fas fa-trash"></i></button>
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
        fetch('reporting/summary-detailed/summary', getFetchOptions())
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
    if (typeof window.originalShowView === 'undefined') {
        window.originalShowView = showView;
    }
    showView = function (viewId) {
        if (window.originalShowView) {
            window.originalShowView(viewId);
        }
        // Stop transaction stream whenever leaving the transactions view
        if (viewId !== 'transactions-view' && typeof stopTransactionStream === 'function') {
            stopTransactionStream();
        }
        if (viewId === 'dashboard-view') {
            setTimeout(() => {
                initCharts();
                fetchDashboardStats();
            }, 100); // Small delay to ensure DOM is ready
        }
    };
    // Expose a global showView for inline handlers and other modules
    window.showView = showView;

    // --- SAR Reports Management ---
    window.fetchSarReports = function (statusFilter) {
        // Get filter value from dropdown if not provided
        if (statusFilter === undefined) {
            const filterEl = document.getElementById('sarStatusFilter');
            statusFilter = filterEl ? filterEl.value : '';
        }

        // Build URL with filter parameter
        let url = 'compliance/sar';
        if (statusFilter && statusFilter !== '') {
            url += '?status=' + encodeURIComponent(statusFilter);
        }

        fetch(url, getFetchOptions())
            .then(res => {
                if (!res.ok) throw new Error(`HTTP ${res.status}`);
                return res.json();
            })
            .then(sars => {
                // Update the badge count with actual count of non-exported SARs
                updateSarCount();

                const container = document.querySelector('#sar-view .table-card');
                if (!container) return;
                if (!sars || sars.length === 0) {
                    container.innerHTML = '<p>No SAR reports available' + (statusFilter ? ' with status: ' + statusFilter : '') + '.</p>';
                    return;
                }
                let html = '<table class="data-table"><thead><tr><th>SAR Reference</th><th>Status</th><th>Type</th><th>Jurisdiction</th><th>Created</th><th>Actions</th></tr></thead><tbody>';
                sars.forEach(sar => {
                    const sarRef = sar.sarReference || ('SAR-' + sar.id);
                    const status = sar.status || 'DRAFT';
                    const sarType = sar.suspiciousActivityType || sar.sarType || 'N/A';
                    const jurisdiction = sar.jurisdiction || 'N/A';
                    const createdAt = sar.createdAt ? new Date(sar.createdAt).toLocaleDateString() : 'N/A';
                    const sarId = sar.id;

                    html += `<tr>
                        <td><span class="case-id">${sarRef}</span></td>
                        <td><span class="status-badge ${getStatusClass(status)}">${status}</span></td>
                        <td>${sarType}</td>
                        <td>${jurisdiction}</td>
                        <td>${createdAt}</td>
                        <td>
                            <div class="action-btns">
                                <button class="action-btn" title="View SAR" onclick="viewSar(${sarId})" data-sar-id="${sarId}">
                                    <i class="fas fa-eye"></i>
                                </button>
                                <button class="action-btn" title="Edit SAR" onclick="editSar(${sarId})" data-sar-id="${sarId}">
                                    <i class="fas fa-edit"></i>
                                </button>
                                <button class="action-btn danger" title="Delete SAR" onclick="deleteSar(${sarId})" data-sar-id="${sarId}">
                                    <i class="fas fa-trash"></i>
                                </button>
                            </div>
                        </td>
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

    // Filter SAR reports in real-time
    window.filterSarReports = function () {
        const statusFilter = document.getElementById('sarStatusFilter');
        if (statusFilter) {
            const selectedStatus = statusFilter.value;
            fetchSarReports(selectedStatus);
        } else {
            fetchSarReports();
        }
    };

    // Filter SAR reports in real-time
    window.filterSarReports = function () {
        const statusFilter = document.getElementById('sarStatusFilter');
        if (statusFilter) {
            const selectedStatus = statusFilter.value;
            fetchSarReports(selectedStatus);
        } else {
            fetchSarReports();
        }
    };

    // SAR CRUD Operations
    window.viewSar = function (sarId) {
        if (!sarId) {
            console.error('viewSar called without sarId');
            alert('Error: SAR ID is missing.');
            return;
        }

        fetch(`compliance/sar/${sarId}`, getFetchOptions())
            .then(async res => {
                if (!res.ok) {
                    const contentType = res.headers.get('content-type');
                    if (contentType && contentType.includes('application/json')) {
                        try {
                            const errorData = await res.json();
                            throw new Error(errorData.message || errorData.error || `HTTP ${res.status}`);
                        } catch (parseError) {
                            throw new Error(`HTTP ${res.status}: ${res.statusText}`);
                        }
                    } else {
                        throw new Error(`HTTP ${res.status}: ${res.statusText}`);
                    }
                }
                const contentType = res.headers.get('content-type');
                if (contentType && contentType.includes('application/json')) {
                    return res.json();
                } else {
                    const text = await res.text();
                    throw new Error(`Unexpected response format. Expected JSON but got: ${contentType || 'unknown'}`);
                }
            })
            .then(sarData => {
                // Display SAR details in a modal
                const sarRef = sarData.sarReference || ('SAR-' + sarId);
                const status = sarData.status || 'N/A';
                const sarType = sarData.suspiciousActivityType || sarData.sarType || 'N/A';
                const jurisdiction = sarData.jurisdiction || 'N/A';
                const createdAt = sarData.createdAt ? new Date(sarData.createdAt).toLocaleString() : 'N/A';
                const body = sarData.body || sarData.content || sarData.description || 'No content available.';

                const escapeHtml = (text) => {
                    const div = document.createElement('div');
                    div.textContent = text;
                    return div.innerHTML;
                };

                const modalHtml = `
                    <div id="sar-modal" class="modal" style="display: block;">
                        <div class="modal-content" style="max-width: 700px;">
                            <span class="close" onclick="closeSarModal()">&times;</span>
                            <h2>SAR Details</h2>
                            <div style="margin-top: 20px;">
                                <p><strong>SAR Reference:</strong> ${escapeHtml(sarRef)}</p>
                                <p><strong>Status:</strong> <span class="status-badge ${getStatusClass(status)}">${escapeHtml(status)}</span></p>
                                <p><strong>Type:</strong> ${escapeHtml(sarType)}</p>
                                <p><strong>Jurisdiction:</strong> ${escapeHtml(jurisdiction)}</p>
                                <p><strong>Created:</strong> ${escapeHtml(createdAt)}</p>
                                ${body ? `<hr style="margin: 15px 0; border: none; border-top: 1px solid var(--glass-border, #e0e0e0);"><div style="padding: 15px; background: rgba(255, 255, 255, 0.05); border-radius: 8px; white-space: pre-wrap; word-wrap: break-word;">${escapeHtml(body)}</div>` : ''}
                            </div>
                            <div style="margin-top: 20px; text-align: right;">
                                <button class="btn-secondary" onclick="closeSarModal()">Close</button>
                            </div>
                        </div>
                    </div>
                `;

                const existingModal = document.getElementById('sar-modal');
                if (existingModal) {
                    existingModal.remove();
                }

                document.body.insertAdjacentHTML('beforeend', modalHtml);

                const modal = document.getElementById('sar-modal');
                if (modal) {
                    modal.addEventListener('click', function (event) {
                        if (event.target === modal) {
                            closeSarModal();
                        }
                    });
                }
            })
            .catch(err => {
                console.error('Error loading SAR:', err);
                alert('Error loading SAR: ' + (err.message || 'Please try again.'));
                handleApiError(err, 'viewSar');
            });
    };

    window.closeSarModal = function () {
        const modal = document.getElementById('sar-modal');
        if (modal) {
            modal.remove();
        }
    };

    window.editSar = function (sarId) {
        if (!sarId) {
            console.error('editSar called without sarId');
            alert('Error: SAR ID is missing.');
            return;
        }

        fetch(`compliance/sar/${sarId}`, getFetchOptions())
            .then(async res => {
                if (!res.ok) {
                    const contentType = res.headers.get('content-type');
                    if (contentType && contentType.includes('application/json')) {
                        try {
                            const errorData = await res.json();
                            throw new Error(errorData.message || errorData.error || `HTTP ${res.status}`);
                        } catch (parseError) {
                            throw new Error(`HTTP ${res.status}: ${res.statusText}`);
                        }
                    } else {
                        throw new Error(`HTTP ${res.status}: ${res.statusText}`);
                    }
                }
                const contentType = res.headers.get('content-type');
                if (contentType && contentType.includes('application/json')) {
                    return res.json();
                } else {
                    const text = await res.text();
                    throw new Error(`Unexpected response format. Expected JSON but got: ${contentType || 'unknown'}`);
                }
            })
            .then(sarData => {
                alert('Edit SAR functionality - SAR ID: ' + sarId + '\n\nThis will open an edit modal.\n\nNote: SAR editing may be restricted based on regulatory requirements.');
                // TODO: Implement SAR edit modal
            })
            .catch(err => {
                console.error('Error loading SAR for edit:', err);
                alert('Error loading SAR: ' + (err.message || 'Please try again.'));
                handleApiError(err, 'editSar');
            });
    };

    window.deleteSar = function (sarId) {
        if (!sarId) {
            alert('Error: SAR ID is missing.');
            return;
        }

        if (!confirm(`Are you sure you want to delete SAR ${sarId}?\n\nThis action cannot be undone and will permanently remove the SAR report and all associated data from the system.\n\nWARNING: Deleting SARs may violate regulatory compliance requirements.`)) {
            return;
        }

        fetch(`compliance/sar/${sarId}`, getFetchOptions('DELETE'))
            .then(async res => {
                if (res.ok) {
                    fetchSarReports(); // Refresh the list
                    return;
                }
                const contentType = res.headers.get('content-type');
                if (contentType && contentType.includes('application/json')) {
                    try {
                        const errorData = await res.json();
                        throw new Error(errorData.message || errorData.error || 'Failed to delete SAR');
                    } catch (parseError) {
                        throw new Error(`HTTP ${res.status}: ${res.statusText}`);
                    }
                } else {
                    throw new Error(`HTTP ${res.status}: ${res.statusText}`);
                }
            })
            .catch(err => {
                console.error('Error deleting SAR:', err);
                alert('Error deleting SAR: ' + (err.message || 'Please try again.'));
                handleApiError(err, 'deleteSar');
            });
    };

    // --- Audit Logs Management ---
    function escapeHtml(value) {
        return String(value == null ? '' : value)
            .replaceAll('&', '&amp;')
            .replaceAll('<', '&lt;')
            .replaceAll('>', '&gt;')
            .replaceAll('"', '&quot;')
            .replaceAll("'", '&#39;');
    }

    function buildAuditLogsUrl() {
        const params = new URLSearchParams();

        const limit = document.getElementById('auditFilterLimit')?.value || '100';
        params.set('limit', limit);

        const username = document.getElementById('auditFilterUsername')?.value?.trim();
        const actionType = document.getElementById('auditFilterAction')?.value?.trim();
        const entityType = document.getElementById('auditFilterEntityType')?.value?.trim();
        const entityId = document.getElementById('auditFilterEntityId')?.value?.trim();
        const success = document.getElementById('auditFilterSuccess')?.value?.trim();
        const ipAddress = document.getElementById('auditFilterIp')?.value?.trim();
        const sessionId = document.getElementById('auditFilterSession')?.value?.trim();
        const start = document.getElementById('auditFilterStart')?.value?.trim();
        const end = document.getElementById('auditFilterEnd')?.value?.trim();

        if (username) params.set('username', username);
        if (actionType) params.set('actionType', actionType);
        if (entityType) params.set('entityType', entityType);
        if (entityId) params.set('entityId', entityId);
        if (success === 'true' || success === 'false') params.set('success', success);
        if (ipAddress) params.set('ipAddress', ipAddress);
        if (sessionId) params.set('sessionId', sessionId);
        if (start) params.set('start', start);
        if (end) params.set('end', end);

        return 'audit/logs?' + params.toString();
    }

    window.resetAuditLogFilters = function () {
        const ids = [
            'auditFilterUsername',
            'auditFilterAction',
            'auditFilterEntityType',
            'auditFilterEntityId',
            'auditFilterSuccess',
            'auditFilterIp',
            'auditFilterSession',
            'auditFilterStart',
            'auditFilterEnd'
        ];
        ids.forEach(id => {
            const el = document.getElementById(id);
            if (el) el.value = '';
        });
        const limitEl = document.getElementById('auditFilterLimit');
        if (limitEl) limitEl.value = '100';
        window.fetchAuditLogs();
    };

    window.fetchAuditLogs = function () {
        const resultsContainer = document.getElementById('audit-logs-container') || document.querySelector('#audit-view .table-card');
        if (resultsContainer) {
            resultsContainer.innerHTML = '<p>Loading system events...</p>';
        }

        fetch(buildAuditLogsUrl(), getFetchOptions())
            .then(res => {
                if (!res.ok) throw new Error(`HTTP ${res.status}`);
                return res.json();
            })
            .then(logs => {
                window._lastAuditLogs = Array.isArray(logs) ? logs : [];
                const container = document.getElementById('audit-logs-container') || document.querySelector('#audit-view .table-card');
                if (!container) return;
                if (!logs || logs.length === 0) {
                    container.innerHTML = '<p>No audit logs found.</p>';
                    return;
                }

                let html = `
                    <div class="table-header" style="display:flex;justify-content:space-between;align-items:center;margin-bottom:10px;">
                        <div><strong>${logs.length}</strong> events</div>
                        <div class="text-muted">Last updated: ${new Date().toLocaleTimeString()}</div>
                    </div>
                    <table class="data-table">
                        <thead>
                            <tr>
                                <th>Timestamp</th>
                                <th>User</th>
                                <th>Role</th>
                                <th>Action</th>
                                <th>Entity</th>
                                <th>Entity ID</th>
                                <th>IP</th>
                                <th>Session</th>
                                <th>Result</th>
                                <th>Reason / Error</th>
                            </tr>
                        </thead>
                        <tbody>
                `;

                logs.forEach(log => {
                    const ts = log.timestamp ? new Date(log.timestamp).toLocaleString() : 'N/A';
                    const user = log.username || 'System';
                    const role = log.userRole || '';
                    const action = log.actionType || 'N/A';
                    const entityType = log.entityType || 'N/A';
                    const entityId = log.entityId || 'N/A';
                    const ip = log.ipAddress || '';
                    const sessionId = log.sessionId || '';
                    const ok = (log.success === true);
                    const resultBadge = ok
                        ? '<span class="status-badge resolved">SUCCESS</span>'
                        : '<span class="status-badge dismissed">FAILED</span>';
                    const reasonOrError = log.errorMessage || log.reason || '';

                    html += `
                        <tr>
                            <td>${escapeHtml(ts)}</td>
                            <td>${escapeHtml(user)}</td>
                            <td>${escapeHtml(role)}</td>
                            <td><span class="badge badge-info">${escapeHtml(action)}</span></td>
                            <td>${escapeHtml(entityType)}</td>
                            <td>${escapeHtml(entityId)}</td>
                            <td><small>${escapeHtml(ip)}</small></td>
                            <td><small>${escapeHtml(sessionId)}</small></td>
                            <td>${resultBadge}</td>
                            <td>${escapeHtml(reasonOrError)}</td>
                        </tr>
                    `;
                });

                html += '</tbody></table>';
                container.innerHTML = html;
            })
            .catch(err => {
                const container = document.getElementById('audit-logs-container') || document.querySelector('#audit-view .table-card');
                if (container) container.innerHTML = '<p>Error loading audit logs. Please try again later.</p>';
                handleApiError(err, 'fetchAuditLogs');
            });
    };

    window.exportAuditLogsCsv = function () {
        const logs = Array.isArray(window._lastAuditLogs) ? window._lastAuditLogs : [];
        if (!logs.length) {
            alert('No audit logs available to export. Run a search first.');
            return;
        }

        function csvEscape(v) {
            const s = String(v == null ? '' : v);
            return `"${s.replaceAll('"', '""')}"`;
        }

        const header = [
            'timestamp',
            'username',
            'userRole',
            'actionType',
            'entityType',
            'entityId',
            'success',
            'ipAddress',
            'sessionId',
            'reason',
            'errorMessage'
        ];

        let csv = header.join(',') + '\n';
        logs.forEach(log => {
            const row = [
                log.timestamp || '',
                log.username || '',
                log.userRole || '',
                log.actionType || '',
                log.entityType || '',
                log.entityId || '',
                (log.success === true) ? 'true' : 'false',
                log.ipAddress || '',
                log.sessionId || '',
                log.reason || '',
                log.errorMessage || ''
            ].map(csvEscape);
            csv += row.join(',') + '\n';
        });

        const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = `audit_logs_${new Date().toISOString().split('T')[0]}.csv`;
        link.click();
        URL.revokeObjectURL(url);
    };

    // --- Transactions Management ---
    let transactionRefreshInterval = null;

    window.fetchTransactions = function () {
        const container = document.querySelector('#transactions-view .table-card');
        if (!container) return;

        // Show loading state
        container.innerHTML = '<div class="loading-spinner"></div><p style="text-align:center;margin-top:10px;">Loading transactions...</p>';

        fetch('transactions?limit=100', getFetchOptions())
            .then(res => {
                if (!res.ok) {
                    throw new Error(`HTTP ${res.status}: ${res.statusText}`);
                }
                return res.json();
            })
            .then(transactions => {
                if (!transactions || transactions.length === 0) {
                    container.innerHTML = '<div class="empty-state"><i class="fas fa-inbox"></i><p>No recent transactions found.</p><p class="text-muted">Transactions will appear here as they are processed.</p></div>';
                    return;
                }

                let html = `
                    <div class="transactions-header">
                        <div class="transactions-stats">
                            <span><strong>${transactions.length}</strong> transactions</span>
                            <span class="text-muted">Last updated: ${new Date().toLocaleTimeString()}</span>
                        </div>
                        <div class="transactions-controls">
                            <button class="btn-secondary btn-sm" onclick="toggleTransactionStream()" id="stream-toggle-btn">
                                <i class="fas fa-pause"></i> Pause Stream
                            </button>
                            <button class="btn-secondary btn-sm" onclick="fetchTransactions()">
                                <i class="fas fa-sync"></i> Refresh
                            </button>
                        </div>
                    </div>
                    <div class="transactions-table-wrapper">
                        <table class="data-table" id="transactions-table">
                            <thead>
                                <tr>
                                    <th>Transaction ID</th>
                                    <th>Merchant ID</th>
                                    <th>Terminal ID</th>
                                    <th>Amount</th>
                                    <th>Currency</th>
                                    <th>Timestamp</th>
                                    <th>IP Address</th>
                                    <th>Status</th>
                                    <th>Actions</th>
                                </tr>
                            </thead>
                            <tbody id="transactions-tbody">
                `;

                transactions.forEach(txn => {
                    const currency = txn.currency || (window.currencyFormatter ? window.currencyFormatter.getDefaultCurrency() : 'USD');
                    const amount = txn.amountCents ? (txn.amountCents / 100) : 0;
                    const timestamp = txn.txnTs ? new Date(txn.txnTs).toLocaleString() : 'N/A';
                    const timeAgo = txn.txnTs ? getTimeAgo(new Date(txn.txnTs)) : '';

                    html += `
                        <tr class="transaction-row" data-txn-id="${txn.txnId}">
                            <td><span class="txn-id">${txn.txnId || 'N/A'}</span></td>
                            <td>${txn.merchantId || 'N/A'}</td>
                            <td><small>${txn.terminalId || 'N/A'}</small></td>
                            <td><strong>${formatCurrency(amount, currency)}</strong></td>
                            <td>${currency}</td>
                            <td>
                                <div>${timestamp}</div>
                                <small class="text-muted">${timeAgo}</small>
                            </td>
                            <td><small>${txn.ipAddress || 'N/A'}</small></td>
                            <td><span class="status-badge resolved">Processed</span></td>
                        </tr>
                    `;
                });

                html += `
                            </tbody>
                        </table>
                    </div>
                `;

                container.innerHTML = html;

                // Start auto-refresh if not already running
                if (transactionRefreshInterval === null) {
                    startTransactionStream();
                }
            })
            .catch(err => {
                console.error('Error fetching transactions:', err);
                container.innerHTML = `
                    <div class="error-state">
                        <i class="fas fa-exclamation-circle"></i>
                        <p><strong>Error loading transactions</strong></p>
                        <p class="text-muted">${err.message || 'Please try again later.'}</p>
                        <button class="btn-primary" onclick="fetchTransactions()" style="margin-top:10px;">
                            <i class="fas fa-redo"></i> Retry
                        </button>
                    </div>
                `;
                handleApiError(err, 'fetchTransactions');
            });
    };

    function startTransactionStream() {
        if (transactionRefreshInterval !== null) {
            clearInterval(transactionRefreshInterval);
        }
        transactionRefreshInterval = setInterval(() => {
            // Only refresh if transactions view is visible
            const transactionsView = document.getElementById('transactions-view');
            if (transactionsView && transactionsView.style.display !== 'none') {
                refreshTransactions();
            }
        }, 5000); // Refresh every 5 seconds

        const toggleBtn = document.getElementById('stream-toggle-btn');
        if (toggleBtn) {
            toggleBtn.innerHTML = '<i class="fas fa-pause"></i> Pause Stream';
            toggleBtn.onclick = toggleTransactionStream;
        }
    }

    function stopTransactionStream() {
        if (transactionRefreshInterval !== null) {
            clearInterval(transactionRefreshInterval);
            transactionRefreshInterval = null;
        }

        const toggleBtn = document.getElementById('stream-toggle-btn');
        if (toggleBtn) {
            toggleBtn.innerHTML = '<i class="fas fa-play"></i> Resume Stream';
            toggleBtn.onclick = toggleTransactionStream;
        }
    }

    window.toggleTransactionStream = function () {
        if (transactionRefreshInterval === null) {
            startTransactionStream();
        } else {
            stopTransactionStream();
        }
    };

    function refreshTransactions() {
        fetch('transactions?limit=100', getFetchOptions())
            .then(res => {
                if (!res.ok) return;
                return res.json();
            })
            .then(transactions => {
                if (!transactions || transactions.length === 0) return;

                const tbody = document.getElementById('transactions-tbody');
                if (!tbody) return;

                // Get existing transaction IDs
                const existingIds = new Set(
                    Array.from(tbody.querySelectorAll('tr[data-txn-id]'))
                        .map(tr => tr.getAttribute('data-txn-id'))
                );

                // Add new transactions at the top
                transactions.forEach(txn => {
                    const txnId = String(txn.txnId);
                    if (!existingIds.has(txnId)) {
                        const currency = txn.currency || (window.currencyFormatter ? window.currencyFormatter.getDefaultCurrency() : 'USD');
                        const amount = txn.amountCents ? (txn.amountCents / 100) : 0;
                        const timestamp = txn.txnTs ? new Date(txn.txnTs).toLocaleString() : 'N/A';
                        const timeAgo = txn.txnTs ? getTimeAgo(new Date(txn.txnTs)) : '';

                        const tr = document.createElement('tr');
                        tr.className = 'transaction-row new-transaction';
                        tr.setAttribute('data-txn-id', txnId);
                        tr.innerHTML = `
                            <td><span class="txn-id">${txn.txnId || 'N/A'}</span></td>
                            <td>${txn.merchantId || 'N/A'}</td>
                            <td><small>${txn.terminalId || 'N/A'}</small></td>
                            <td><strong>${formatCurrency(amount, currency)}</strong></td>
                            <td>${currency}</td>
                            <td>
                                <div>${timestamp}</div>
                                <small class="text-muted">${timeAgo}</small>
                            </td>
                            <td><small>${txn.ipAddress || 'N/A'}</small></td>
                            <td><span class="status-badge resolved">Processed</span></td>
                        `;
                        tbody.insertBefore(tr, tbody.firstChild);

                        // Remove highlight after animation
                        setTimeout(() => {
                            tr.classList.remove('new-transaction');
                        }, 2000);

                        // Limit table to 100 rows
                        const rows = tbody.querySelectorAll('tr');
                        if (rows.length > 100) {
                            rows[rows.length - 1].remove();
                        }
                    }
                });

                // Update last updated time
                const stats = document.querySelector('.transactions-stats span:last-child');
                if (stats) {
                    stats.textContent = `Last updated: ${new Date().toLocaleTimeString()}`;
                }
            })
            .catch(err => {
                console.error('Error refreshing transactions:', err);
            });
    }

    // Note: view switching logic (including stopping transaction stream) is handled in showView wrapper above.

    // --- Reports Management ---
    window.generateAnalyticsReport = function () {
        if (!confirm('Generate new analytics report?')) return;

        // Use relative path for API call
        fetch('reporting/analytics/generate', getFetchOptions('POST'))
            .then(res => {
                if (res.ok) {
                    alert('Report generation started. Check back in a few moments.');
                    setTimeout(fetchReports, 2000); // Refresh list
                } else {
                    alert('Failed to start report generation');
                }
            })
            .catch(err => {
                console.error('Error generating report:', err);
                // Fallback for demo/if endpoint doesn't exist
                alert('Report generation simulation: Success');
                fetchReports();
            });
    };

    window.fetchReports = function () {
        fetch('reporting/summary', getFetchOptions())
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
    window.fetchAlerts = function (statusFilter) {
        // Get filter value from dropdown if not provided
        if (statusFilter === undefined) {
            const filterEl = document.getElementById('alertStatusFilter');
            statusFilter = filterEl ? filterEl.value : '';
        }

        // Build URL with filter parameter
        let url = 'alerts';
        const params = [];
        if (statusFilter && statusFilter !== '') {
            params.push('status=' + encodeURIComponent(statusFilter));
        }
        params.push('limit=100');
        if (params.length > 0) {
            url += '?' + params.join('&');
        }

        fetch(url, getFetchOptions())
            .then(res => {
                if (!res.ok) throw new Error(`HTTP ${res.status}`);
                return res.json();
            })
            .then(alerts => {
                // Update the badge count with actual count
                updateAlertsCount();

                const container = document.querySelector('#alerts-view .alerts-grid');
                if (!container) return;
                if (!alerts || alerts.length === 0) {
                    container.innerHTML = '<div class="alert-card"><p>No alerts found' + (statusFilter ? ' with status: ' + statusFilter : '') + '.</p></div>';
                    return;
                }
                let html = '<table class="data-table"><thead><tr><th>Alert ID</th><th>Reason</th><th>Transaction ID</th><th>Score</th><th>Severity</th><th>Status</th><th>Created</th><th>Actions</th></tr></thead><tbody>';
                alerts.forEach(alert => {
                    const severity = alert.severity || 'INFO';
                    const severityClass = severity.toLowerCase();
                    const timeAgo = alert.createdAt ? getTimeAgo(new Date(alert.createdAt)) : 'Unknown';
                    const createdAt = alert.createdAt ? new Date(alert.createdAt).toLocaleString() : 'N/A';
                    const alertId = alert.id || alert.alertId || 'N/A';
                    const reason = alert.reason || 'Alert';
                    const txnId = alert.txnId || 'N/A';
                    const score = alert.score || 'N/A';
                    const status = alert.status || 'open';

                    html += `
                        <tr>
                            <td>${alertId}</td>
                            <td><strong>${reason}</strong></td>
                            <td>${txnId}</td>
                            <td>${score}</td>
                            <td><span class="badge badge-${severityClass}">${severity}</span></td>
                            <td><span class="status-badge ${status === 'open' ? 'warning' : 'resolved'}">${status}</span></td>
                            <td>${createdAt}<br><small class="text-muted">${timeAgo}</small></td>
                            <td>
                                <div class="action-btns">
                                    <button class="action-btn" title="View Alert" onclick="viewAlert(${alertId})" data-alert-id="${alertId}">
                                        <i class="fas fa-eye"></i>
                                    </button>
                                    <button class="action-btn" title="Resolve Alert" onclick="resolveAlert(${alertId})" data-alert-id="${alertId}">
                                        <i class="fas fa-check"></i>
                                    </button>
                                    <button class="action-btn danger" title="Delete Alert" onclick="deleteAlert(${alertId})" data-alert-id="${alertId}">
                                        <i class="fas fa-trash"></i>
                                    </button>
                                </div>
                            </td>
                        </tr>
                    `;
                });
                html += '</tbody></table>';
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

    // Filter alerts in real-time
    window.filterAlerts = function () {
        const statusFilter = document.getElementById('alertStatusFilter');
        if (statusFilter) {
            const selectedStatus = statusFilter.value;
            fetchAlerts(selectedStatus);
        } else {
            fetchAlerts();
        }
    };

    // Alert CRUD Operations
    window.viewAlert = function (alertId) {
        if (!alertId) {
            console.error('viewAlert called without alertId');
            alert('Error: Alert ID is missing.');
            return;
        }

        fetch(`alerts/${alertId}`, getFetchOptions())
            .then(async res => {
                if (!res.ok) {
                    const contentType = res.headers.get('content-type');
                    if (contentType && contentType.includes('application/json')) {
                        try {
                            const errorData = await res.json();
                            throw new Error(errorData.message || errorData.error || `HTTP ${res.status}`);
                        } catch (parseError) {
                            throw new Error(`HTTP ${res.status}: ${res.statusText}`);
                        }
                    } else {
                        throw new Error(`HTTP ${res.status}: ${res.statusText}`);
                    }
                }
                const contentType = res.headers.get('content-type');
                if (contentType && contentType.includes('application/json')) {
                    return res.json();
                } else {
                    const text = await res.text();
                    throw new Error(`Unexpected response format. Expected JSON but got: ${contentType || 'unknown'}`);
                }
            })
            .then(alertData => {
                // Display alert details in a modal
                const reason = alertData.reason || alertData.description || 'N/A';
                const txnId = alertData.txnId || alertData.transactionId || 'N/A';
                const score = alertData.score || alertData.riskScore || 'N/A';
                const severity = alertData.severity || alertData.priority || 'N/A';
                const status = alertData.status || 'N/A';
                const createdAt = alertData.createdAt ? new Date(alertData.createdAt).toLocaleString() : 'N/A';

                const escapeHtml = (text) => {
                    const div = document.createElement('div');
                    div.textContent = text;
                    return div.innerHTML;
                };

                const modalHtml = `
                    <div id="alert-modal" class="modal" style="display: block;">
                        <div class="modal-content" style="max-width: 700px;">
                            <span class="close" onclick="closeAlertModal()">&times;</span>
                            <h2>Alert Details</h2>
                            <div style="margin-top: 20px;">
                                <p><strong>Alert ID:</strong> ${escapeHtml(alertId)}</p>
                                <p><strong>Status:</strong> <span class="status-badge ${getStatusClass(status)}">${escapeHtml(status)}</span></p>
                                <p><strong>Severity:</strong> <span class="status-badge ${severity === 'HIGH' || severity === 'CRITICAL' ? 'high' : 'medium'}">${escapeHtml(severity)}</span></p>
                                <p><strong>Score:</strong> ${escapeHtml(score)}</p>
                                <p><strong>Transaction ID:</strong> ${escapeHtml(txnId)}</p>
                                <p><strong>Reason:</strong> ${escapeHtml(reason)}</p>
                                <p><strong>Created:</strong> ${escapeHtml(createdAt)}</p>
                            </div>
                            <div style="margin-top: 20px; text-align: right;">
                                <button class="btn-secondary" onclick="closeAlertModal()">Close</button>
                            </div>
                        </div>
                    </div>
                `;

                const existingModal = document.getElementById('alert-modal');
                if (existingModal) {
                    existingModal.remove();
                }

                document.body.insertAdjacentHTML('beforeend', modalHtml);

                const modal = document.getElementById('alert-modal');
                if (modal) {
                    modal.addEventListener('click', function (event) {
                        if (event.target === modal) {
                            closeAlertModal();
                        }
                    });
                }
            })
            .catch(err => {
                console.error('Error loading alert:', err);
                alert('Error loading alert: ' + (err.message || 'Please try again.'));
                handleApiError(err, 'viewAlert');
            });
    };

    window.closeAlertModal = function () {
        const modal = document.getElementById('alert-modal');
        if (modal) {
            modal.remove();
        }
    };

    window.resolveAlert = function (alertId) {
        if (!alertId) {
            alert('Error: Alert ID is missing.');
            return;
        }

        if (!confirm(`Are you sure you want to mark alert ${alertId} as resolved?\n\nThis will change the alert status to resolved. You can still view it later, but it will no longer appear in active alerts.`)) {
            return;
        }

        fetch(`alerts/${alertId}/resolve`, getFetchOptions('PUT'))
            .then(async res => {
                if (res.ok) {
                    fetchAlerts(); // Refresh the list
                    return;
                }
                const contentType = res.headers.get('content-type');
                if (contentType && contentType.includes('application/json')) {
                    try {
                        const errorData = await res.json();
                        throw new Error(errorData.message || errorData.error || 'Failed to resolve alert');
                    } catch (parseError) {
                        throw new Error(`HTTP ${res.status}: ${res.statusText}`);
                    }
                } else {
                    throw new Error(`HTTP ${res.status}: ${res.statusText}`);
                }
            })
            .catch(err => {
                console.error('Error resolving alert:', err);
                alert('Error resolving alert: ' + (err.message || 'Please try again.'));
                handleApiError(err, 'resolveAlert');
            });
    };

    window.deleteAlert = function (alertId) {
        if (!alertId) {
            alert('Error: Alert ID is missing.');
            return;
        }

        if (!confirm(`Are you sure you want to delete alert ${alertId}?\n\nThis action cannot be undone and will permanently remove the alert from the system.`)) {
            return;
        }

        fetch(`alerts/${alertId}`, getFetchOptions('DELETE'))
            .then(async res => {
                if (res.ok) {
                    fetchAlerts(); // Refresh the list
                    return;
                }
                const contentType = res.headers.get('content-type');
                if (contentType && contentType.includes('application/json')) {
                    try {
                        const errorData = await res.json();
                        throw new Error(errorData.message || errorData.error || 'Failed to delete alert');
                    } catch (parseError) {
                        throw new Error(`HTTP ${res.status}: ${res.statusText}`);
                    }
                } else {
                    throw new Error(`HTTP ${res.status}: ${res.statusText}`);
                }
            })
            .catch(err => {
                console.error('Error deleting alert:', err);
                alert('Error deleting alert: ' + (err.message || 'Please try again.'));
                handleApiError(err, 'deleteAlert');
            });
    };

    // --- User Profile ---
    window.fetchUserProfile = function () {
        fetch('users/me', getFetchOptions())
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
                // Update messages count after fetching messages
                updateMessagesCount();
            })
            .catch(err => {
                const container = document.getElementById('messages-content');
                if (container) container.innerHTML = '<p>Error loading messages.</p>';
            });
    };

    // --- Settings Management ---
    window.fetchSettings = function () {
        fetch('settings', getFetchOptions())
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

                fetch('sanctions/screen', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify(data)
                })
                    .then(res => {
                        if (!res.ok) {
                            return res.json().then(errorData => {
                                throw new Error(errorData.message || errorData.error || `HTTP ${res.status}`);
                            }).catch(() => {
                                throw new Error(`HTTP ${res.status}: ${res.statusText}`);
                            });
                        }
                        return res.json();
                    })
                    .then(result => {
                        const hasHits = (result.hits && result.hits.length > 0) || (result.matches && result.matches.length > 0);
                        if (hasHits) {
                            const hits = result.hits || result.matches || [];
                            let html = `<div class="screening-hit high"><strong>HIT FOUND: ${hits.length} matches</strong></div>`;
                            hits.forEach(hit => {
                                html += `
                                <div class="hit-detail">
                                    <p><strong>Name:</strong> ${hit.matchedName || hit.name || 'N/A'}</p>
                                    <p><strong>List:</strong> ${hit.sourceList || hit.list || hit.listName || 'N/A'}</p>
                                    <p><strong>Reason:</strong> ${hit.reason || 'N/A'}</p>
                                    ${hit.similarityScore !== undefined ? `<p><strong>Match Score:</strong> ${(hit.similarityScore * 100).toFixed(2)}%</p>` : ''}
                                </div>
                            `;
                            });
                            contentDiv.innerHTML = html;
                        } else {
                            contentDiv.innerHTML = '<div class="screening-hit low"><strong>NO HITS FOUND</strong> - Clear</div>';
                        }
                    })
                    .catch(err => {
                        console.error('Screening error:', err);
                        contentDiv.innerHTML = `<p class="error">Error performing screening: ${err.message || 'Please try again.'}</p>`;
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

        fetch('sanctions/screen', getFetchOptions('POST', requestBody))
            .then(async res => {
                if (!res.ok) {
                    // Check if response is JSON before trying to parse
                    const contentType = res.headers.get('content-type');
                    if (contentType && contentType.includes('application/json')) {
                        try {
                            const errorData = await res.json();
                            throw new Error(errorData.message || errorData.error || `HTTP ${res.status}`);
                        } catch (parseError) {
                            // If JSON parsing fails, throw generic error
                            throw new Error(`HTTP ${res.status}: ${res.statusText}`);
                        }
                    } else {
                        // Response is not JSON (likely HTML error page)
                        const text = await res.text();
                        throw new Error(`HTTP ${res.status}: ${res.statusText}. ${text.substring(0, 100)}`);
                    }
                }
                // Check content type for successful responses too
                const contentType = res.headers.get('content-type');
                if (contentType && contentType.includes('application/json')) {
                    return res.json();
                } else {
                    // If not JSON, try to parse as text and show error
                    const text = await res.text();
                    throw new Error(`Unexpected response format. Expected JSON but got: ${contentType || 'unknown'}. Response: ${text.substring(0, 200)}`);
                }
            })
            .then(result => {
                const resultsDiv = document.getElementById('screening-results');
                const contentDiv = document.getElementById('screening-results-content');
                if (!resultsDiv || !contentDiv) return;

                let html = '<div class="screening-result">';
                html += `<h4>Results for: ${result.screenedName || name}</h4>`;

                const hasMatches = result.match === true || (result.matches && result.matches.length > 0);
                html += `<p><strong>Status:</strong> <span class="status-badge ${hasMatches ? 'high' : 'resolved'}">${hasMatches ? 'MATCH FOUND' : 'NO MATCH'}</span></p>`;

                if (result.status) {
                    html += `<p><strong>Screening Status:</strong> ${result.status}</p>`;
                }

                if (result.matchCount !== undefined) {
                    html += `<p><strong>Match Count:</strong> ${result.matchCount}</p>`;
                }

                if (hasMatches && result.matches && result.matches.length > 0) {
                    html += '<h5>Matches:</h5><ul>';
                    result.matches.forEach(match => {
                        html += `<li><strong>${match.name || match.matchedName || 'N/A'}</strong> - ${match.list || match.listName || match.sourceList || 'Unknown List'}`;
                        if (match.similarityScore !== undefined) {
                            html += ` (${(match.similarityScore * 100).toFixed(2)}% match)`;
                        }
                        if (match.reason) {
                            html += ` - ${match.reason}`;
                        }
                        html += '</li>';
                    });
                    html += '</ul>';
                } else {
                    html += '<p>No matches found in sanctions lists.</p>';
                }

                if (result.confidence !== undefined) {
                    html += `<p><strong>Confidence:</strong> ${(result.confidence * 100).toFixed(2)}%</p>`;
                }

                if (result.screeningProvider) {
                    html += `<p><strong>Screening Provider:</strong> ${result.screeningProvider}</p>`;
                }

                html += '</div>';
                contentDiv.innerHTML = html;
                resultsDiv.style.display = 'block';
            })
            .catch(err => {
                console.error('Screening error:', err);
                const resultsDiv = document.getElementById('screening-results');
                const contentDiv = document.getElementById('screening-results-content');
                if (resultsDiv && contentDiv) {
                    // Extract a user-friendly error message
                    let errorMsg = err.message || 'An unexpected error occurred';
                    // If it's a network error or connection issue
                    if (errorMsg.includes('Failed to fetch') || errorMsg.includes('NetworkError')) {
                        errorMsg = 'Unable to connect to the server. Please check your connection and try again.';
                    } else if (errorMsg.includes('Unexpected token')) {
                        errorMsg = 'Server returned an invalid response. Please contact support if this persists.';
                    } else if (errorMsg.includes('HTTP 404')) {
                        errorMsg = 'Screening endpoint not found. Please contact support.';
                    } else if (errorMsg.includes('HTTP 500')) {
                        errorMsg = 'Server error occurred. Please try again later.';
                    }
                    contentDiv.innerHTML = `<p class="error">Error performing screening: ${errorMsg}</p>`;
                    resultsDiv.style.display = 'block';
                } else {
                    alert('Error performing screening: ' + (err.message || 'Please try again.'));
                }
                handleApiError(err, 'performScreening');
            });
    };

    // --- Profile View ---
    window.fetchUserProfile = function () {
        fetch('users/me', getFetchOptions())
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
    // Store messages data for viewMessage function
    let messagesCache = [];

    window.fetchMessages = function (unreadOnly = false) {
        const url = unreadOnly ? '/api/v1/messages?unreadOnly=true' : '/api/v1/messages';
        fetch(url, getFetchOptions())
            .then(async res => {
                if (!res.ok) {
                    const contentType = res.headers.get('content-type');
                    if (contentType && contentType.includes('application/json')) {
                        try {
                            const errorData = await res.json();
                            throw new Error(errorData.message || errorData.error || `HTTP ${res.status}`);
                        } catch (parseError) {
                            throw new Error(`HTTP ${res.status}: ${res.statusText}`);
                        }
                    } else {
                        const text = await res.text();
                        throw new Error(`HTTP ${res.status}: ${res.statusText}`);
                    }
                }
                const contentType = res.headers.get('content-type');
                if (contentType && contentType.includes('application/json')) {
                    return res.json();
                } else {
                    const text = await res.text();
                    throw new Error(`Unexpected response format. Expected JSON but got: ${contentType || 'unknown'}`);
                }
            })
            .then(messages => {
                const container = document.getElementById('messages-content');
                if (!container) return;

                // Store messages in cache for viewMessage function
                messagesCache = messages || [];

                if (!messages || messages.length === 0) {
                    container.innerHTML = '<p>No messages found.</p>';
                    return;
                }

                let html = '<table class="data-table"><thead><tr><th>From</th><th>Subject</th><th>Time</th><th>Status</th><th>Actions</th></tr></thead><tbody>';
                messages.forEach(msg => {
                    const timestamp = msg.timestamp ? new Date(msg.timestamp).toLocaleString() : 'N/A';
                    const readClass = msg.read ? 'read' : 'unread';
                    const msgId = msg.id || msg.messageId || '';
                    html += `<tr class="${readClass}">
                        <td>${msg.from || 'System'}</td>
                        <td><strong>${msg.subject || 'No Subject'}</strong></td>
                        <td>${timestamp}</td>
                        <td><span class="status-badge ${msg.read ? 'resolved' : 'high'}">${msg.read ? 'Read' : 'Unread'}</span></td>
                        <td><button class="action-btn" onclick="(function(id){viewMessage(id)})(${msgId})" data-message-id="${msgId}"><i class="fas fa-eye"></i></button></td>
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
        if (!messageId) {
            console.error('viewMessage called without messageId');
            alert('Error: Message ID is missing.');
            return;
        }

        // Find message in cache
        const message = messagesCache.find(msg => (msg.id === messageId || msg.messageId === messageId));

        if (!message) {
            // Try to fetch message details from API
            fetch(`messages/${messageId}`, getFetchOptions())
                .then(async res => {
                    if (!res.ok) {
                        const contentType = res.headers.get('content-type');
                        if (contentType && contentType.includes('application/json')) {
                            try {
                                const errorData = await res.json();
                                throw new Error(errorData.message || errorData.error || `HTTP ${res.status}`);
                            } catch (parseError) {
                                throw new Error(`HTTP ${res.status}: ${res.statusText}`);
                            }
                        } else {
                            throw new Error(`HTTP ${res.status}: ${res.statusText}`);
                        }
                    }
                    const contentType = res.headers.get('content-type');
                    if (contentType && contentType.includes('application/json')) {
                        return res.json();
                    } else {
                        const text = await res.text();
                        throw new Error(`Unexpected response format. Expected JSON but got: ${contentType || 'unknown'}`);
                    }
                })
                .then(msg => {
                    displayMessageDetails(msg);
                    // Mark as read if unread
                    if (!msg.read) {
                        markMessageAsRead(messageId);
                    }
                })
                .catch(err => {
                    console.error('Error fetching message details:', err);
                    alert('Error loading message details: ' + (err.message || 'Please try again.'));
                    handleApiError(err, 'viewMessage');
                });
        } else {
            displayMessageDetails(message);
            // Mark as read if unread
            if (!message.read) {
                markMessageAsRead(messageId);
            }
        }
    };

    function displayMessageDetails(message) {
        const timestamp = message.timestamp ? new Date(message.timestamp).toLocaleString() : 'N/A';
        const from = message.from || 'System';
        const subject = message.subject || 'No Subject';
        const body = message.body || message.content || 'No content available.';

        // Escape HTML to prevent XSS
        const escapeHtml = (text) => {
            const div = document.createElement('div');
            div.textContent = text;
            return div.innerHTML;
        };

        // Create modal HTML
        const modalHtml = `
            <div id="message-modal" class="modal" style="display: block;">
                <div class="modal-content" style="max-width: 600px;">
                    <span class="close" onclick="closeMessageModal()">&times;</span>
                    <h2>Message Details</h2>
                    <div style="margin-top: 20px;">
                        <p><strong>From:</strong> ${escapeHtml(from)}</p>
                        <p><strong>Subject:</strong> ${escapeHtml(subject)}</p>
                        <p><strong>Time:</strong> ${escapeHtml(timestamp)}</p>
                        <p><strong>Status:</strong> <span class="status-badge ${message.read ? 'resolved' : 'high'}">${message.read ? 'Read' : 'Unread'}</span></p>
                        <hr style="margin: 15px 0; border: none; border-top: 1px solid var(--glass-border, #e0e0e0);">
                        <div style="padding: 15px; background: rgba(255, 255, 255, 0.05); border-radius: 8px; white-space: pre-wrap; word-wrap: break-word;">${escapeHtml(body)}</div>
                    </div>
                    <div style="margin-top: 20px; text-align: right;">
                        <button class="btn-secondary" onclick="closeMessageModal()">Close</button>
                    </div>
                </div>
            </div>
        `;

        // Remove existing modal if any
        const existingModal = document.getElementById('message-modal');
        if (existingModal) {
            existingModal.remove();
        }

        // Add modal to body
        document.body.insertAdjacentHTML('beforeend', modalHtml);

        // Close modal when clicking outside
        const modal = document.getElementById('message-modal');
        if (modal) {
            modal.addEventListener('click', function (event) {
                if (event.target === modal) {
                    closeMessageModal();
                }
            });
        }
    }

    window.closeMessageModal = function () {
        const modal = document.getElementById('message-modal');
        if (modal) {
            modal.remove();
        }
    };

    function markMessageAsRead(messageId) {
        fetch(`messages/${messageId}/read`, getFetchOptions('PUT'))
            .then(res => {
                if (res.ok) {
                    // Refresh messages to update read status
                    fetchMessages();
                }
            })
            .catch(err => {
                console.error('Error marking message as read:', err);
                // Don't show error to user, just log it
            });
    }

    // --- Settings View ---
    window.fetchSettings = function () {
        fetch('settings', getFetchOptions())
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

        fetch('settings', getFetchOptions('PUT', settings))
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
    function formatCurrency(amount, currencyCode) {
        if (window.currencyFormatter && typeof window.currencyFormatter.format === 'function') {
            return window.currencyFormatter.format(amount, currencyCode);
        }
        const code = (currencyCode || 'USD');
        return new Intl.NumberFormat('en-US', { style: 'currency', currency: code }).format(amount);
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
    window.handleLogout = function (event) {
        event.preventDefault();
        if (window.sessionManager) {
            window.sessionManager.destroy();
        }
        // Invalidate session on backend
        fetch('auth/session/invalidate', {
            method: 'POST',
            credentials: 'include'
        }).finally(() => {
            window.location.href = 'logout';
        });
    };

    // --- Merchant Edit/Delete ---
    window.editMerchant = function (id) {
        // #region agent log
        fetch('http://127.0.0.1:7243/ingest/328b5c76-f5f4-4b08-a869-a5ae08fcc3e6', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ location: 'dashboard.js:3201', message: 'editMerchant called', data: { merchantId: id, getFetchOptionsExists: typeof getFetchOptions }, timestamp: Date.now(), sessionId: 'debug-session', runId: 'run1', hypothesisId: 'A' }) }).catch(() => { });
        // #endregion
        try {
            fetch('merchants/' + id, getFetchOptions())
                .then(async res => {
                    if (!res.ok) {
                        await handleFetchError(res, 'editMerchant');
                        return;
                    }
                    return res.json();
                })
                .then(m => {
                    if (!m) return; // Error already handled
                    document.getElementById('editMerchantId').value = m.merchantId;
                    document.getElementById('editMerchantLegalName').value = m.legalName || '';
                    document.getElementById('editMerchantTradingName').value = m.tradingName || '';
                    document.getElementById('editMerchantEmail').value = m.contactEmail || '';
                    document.getElementById('editMerchantBusinessType').value = m.businessType || 'RETAIL';
                    document.getElementById('editMerchantStatus').value = m.status || 'ACTIVE';
                    document.getElementById('editMerchantModal').style.display = 'block';
                })
                .catch(err => {
                    // #region agent log
                    fetch('http://127.0.0.1:7243/ingest/328b5c76-f5f4-4b08-a869-a5ae08fcc3e6', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ location: 'dashboard.js:3220', message: 'editMerchant error', data: { merchantId: id, error: err.message }, timestamp: Date.now(), sessionId: 'debug-session', runId: 'run1', hypothesisId: 'D' }) }).catch(() => { });
                    // #endregion
                    // Error already handled by handleFetchError
                    console.error('Error in editMerchant:', err);
                });
        } catch (err) {
            // #region agent log
            fetch('http://127.0.0.1:7243/ingest/328b5c76-f5f4-4b08-a869-a5ae08fcc3e6', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ location: 'dashboard.js:3225', message: 'editMerchant exception', data: { merchantId: id, error: err.message }, timestamp: Date.now(), sessionId: 'debug-session', runId: 'run1', hypothesisId: 'E' }) }).catch(() => { });
            // #endregion
            alert('Error: ' + err.message);
        }
    };

    window.closeEditMerchantModal = function () {
        document.getElementById('editMerchantModal').style.display = 'none';
    };

    const editMerchantForm = document.getElementById('editMerchantForm');
    if (editMerchantForm) {
        editMerchantForm.addEventListener('submit', function (e) {
            e.preventDefault();
            const id = document.getElementById('editMerchantId').value;
            const formData = new FormData(e.target);
            const data = {};
            formData.forEach((value, key) => data[key] = value);

            fetch('merchants/' + id, getFetchOptions('PUT', data))
                .then(async res => {
                    if (res.ok) {
                        showSuccessNotification("Merchant updated successfully");
                        closeEditMerchantModal();
                        fetchMerchants();
                    } else {
                        await handleFetchError(res, 'updateMerchant');
                    }
                })
                .catch(err => handleApiError(err, 'updateMerchant'));
        });
    }

    window.deleteMerchant = function (id) {
        // #region agent log
        fetch('http://127.0.0.1:7243/ingest/328b5c76-f5f4-4b08-a869-a5ae08fcc3e6', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ location: 'dashboard.js:3253', message: 'deleteMerchant called', data: { merchantId: id, getFetchOptionsExists: typeof getFetchOptions }, timestamp: Date.now(), sessionId: 'debug-session', runId: 'run1', hypothesisId: 'A' }) }).catch(() => { });
        // #endregion
        if (!id) {
            alert('Error: Merchant ID is missing.');
            return;
        }

        if (!confirm(`Are you sure you want to delete merchant ${id}?\n\nThis action cannot be undone and will permanently remove the merchant and all associated data from the system.\n\nWARNING: This may affect transaction processing and compliance records.`)) {
            return;
        }
        try {
            fetch('merchants/' + id, getFetchOptions('DELETE'))
                .then(res => {
                    if (res.ok) {
                        alert("Merchant deleted");
                        fetchMerchants();
                    } else {
                        alert("Failed to delete merchant");
                    }
                })
                .catch(err => {
                    // #region agent log
                    fetch('http://127.0.0.1:7243/ingest/328b5c76-f5f4-4b08-a869-a5ae08fcc3e6', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ location: 'dashboard.js:3271', message: 'deleteMerchant error', data: { merchantId: id, error: err.message }, timestamp: Date.now(), sessionId: 'debug-session', runId: 'run1', hypothesisId: 'D' }) }).catch(() => { });
                    // #endregion
                    handleApiError(err, 'deleteMerchant');
                });
        } catch (err) {
            // #region agent log
            fetch('http://127.0.0.1:7243/ingest/328b5c76-f5f4-4b08-a869-a5ae08fcc3e6', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ location: 'dashboard.js:3275', message: 'deleteMerchant exception', data: { merchantId: id, error: err.message }, timestamp: Date.now(), sessionId: 'debug-session', runId: 'run1', hypothesisId: 'E' }) }).catch(() => { });
            // #endregion
            alert('Error: ' + err.message);
        }
    };

    window.merchantSettings = function (id) { alert("Settings for merchant " + id + " (Coming Soon)"); };
    window.forceSettlement = function (id) { alert("Force settlement for merchant " + id + " (Coming Soon)"); };


    // --- Role Edit/Delete ---
    window.editRole = function (id) {
        fetch('roles/' + id, getFetchOptions())
            .then(res => {
                if (!res.ok) throw new Error("Failed to fetch role");
                return res.json();
            })
            .then(r => {
                document.getElementById('editRoleId').value = r.id;
                document.getElementById('editRoleName').value = r.name || '';
                document.getElementById('editRoleDescription').value = r.description || '';
                document.getElementById('editRoleModal').style.display = 'block';
            })
            .catch(err => alert("Error fetching role details: " + err.message));
    };

    window.closeEditRoleModal = function () {
        document.getElementById('editRoleModal').style.display = 'none';
    };

    const editRoleForm = document.getElementById('editRoleForm');
    if (editRoleForm) {
        editRoleForm.addEventListener('submit', function (e) {
            e.preventDefault();
            const id = document.getElementById('editRoleId').value;
            const formData = new FormData(e.target);
            const data = {
                name: formData.get('name'),
                description: formData.get('description')
            };

            fetch('roles/' + id, getFetchOptions('PUT', data))
                .then(res => {
                    if (res.ok) {
                        alert("Role updated");
                        closeEditRoleModal();
                        fetchRoles();
                    } else {
                        alert("Failed to update role");
                    }
                })
                .catch(err => handleApiError(err, 'updateRole'));
        });
    }

    window.deleteRole = function (id) {
        if (!id) {
            alert('Error: Role ID is missing.');
            return;
        }

        if (!confirm(`Are you sure you want to delete role ID ${id}?\n\nThis action cannot be undone and will permanently remove the role from the system.\n\nWARNING: Users assigned to this role may lose access. Please reassign users before deleting.`)) {
            return;
        }
        fetch('roles/' + id, getFetchOptions('DELETE'))
            .then(res => {
                if (res.ok) {
                    alert("Role deleted");
                    fetchRoles();
                } else {
                    alert("Failed to delete role");
                }
            })
            .catch(err => handleApiError(err, 'deleteRole'));
    };

    // View Merchant Details with Beneficial Owners and Screening Results
    window.viewMerchant = function (id) {
        const modal = document.getElementById('viewMerchantModal');
        const content = document.getElementById('viewMerchantContent');

        if (!modal || !content) {
            console.error('View merchant modal not found');
            return;
        }

        modal.style.display = 'block';
        content.innerHTML = '<div class="loading-spinner"></div><p style="text-align:center;">Loading merchant details...</p>';

        fetch('merchants/' + id, getFetchOptions())
            .then(async res => {
                if (!res.ok) {
                    await handleFetchError(res, 'viewMerchant');
                    content.innerHTML = '<div class="error-message"><p>Error loading merchant details. Please try again.</p></div>';
                    return null;
                }
                return res.json();
            })
            .then(merchant => {
                if (!merchant) return; // Error already handled
                let html = '<div class="merchant-view-container">';

                // Basic Information
                html += '<div class="merchant-section"><h3>Basic Information</h3>';
                html += '<div class="info-grid">';
                html += `<div class="info-item"><label>Legal Name:</label><span>${merchant.legalName || 'N/A'}</span></div>`;
                html += `<div class="info-item"><label>Trading Name:</label><span>${merchant.tradingName || 'N/A'}</span></div>`;
                html += `<div class="info-item"><label>Contact Email:</label><span>${merchant.contactEmail || 'N/A'}</span></div>`;
                html += `<div class="info-item"><label>Business Type:</label><span>${merchant.businessType || 'N/A'}</span></div>`;
                html += `<div class="info-item"><label>Status:</label><span class="status-badge ${(merchant.status || '').toLowerCase()}">${merchant.status || 'N/A'}</span></div>`;
                html += `<div class="info-item"><label>Country:</label><span>${merchant.country || 'N/A'}</span></div>`;
                html += `<div class="info-item"><label>MCC:</label><span>${merchant.mcc || 'N/A'} ${merchant.mccDescription ? '(' + merchant.mccDescription + ')' : ''}</span></div>`;
                html += `<div class="info-item"><label>KYC Status:</label><span class="status-badge ${(merchant.kycStatus || '').toLowerCase()}">${merchant.kycStatus || 'N/A'}</span></div>`;
                html += '</div></div>';

                // Risk Assessment
                html += '<div class="merchant-section"><h3>Risk Assessment</h3>';
                html += '<div class="info-grid">';
                const riskLevel = merchant.riskLevel || 'UNKNOWN';
                const riskScore = merchant.riskScore || 0;
                html += `<div class="info-item"><label>Risk Level:</label><span class="risk-badge ${riskLevel.toLowerCase()}">${riskLevel}</span></div>`;
                html += `<div class="info-item"><label>Risk Score:</label><span>${riskScore}/100</span></div>`;
                html += '</div></div>';

                // Merchant Screening Results
                html += '<div class="merchant-section"><h3>Merchant Screening Results</h3>';
                if (merchant.merchantScreeningResult) {
                    const screening = merchant.merchantScreeningResult;
                    const status = screening.status || 'UNKNOWN';
                    const matchCount = screening.matchCount || 0;
                    html += '<div class="screening-result-card">';
                    html += `<div class="screening-status ${status.toLowerCase()}">Status: ${status}</div>`;
                    html += `<div class="screening-details">`;
                    html += `<p><strong>Match Count:</strong> ${matchCount}</p>`;
                    html += `<p><strong>Screening Provider:</strong> ${merchant.screeningProvider || 'N/A'}</p>`;
                    if (merchant.screenedAt) {
                        html += `<p><strong>Screened At:</strong> ${new Date(merchant.screenedAt).toLocaleString()}</p>`;
                    }
                    html += '</div></div>';
                } else {
                    html += '<p class="no-data">No screening results available</p>';
                }
                html += '</div>';

                // Beneficial Owners
                html += '<div class="merchant-section"><h3>Beneficial Owners</h3>';
                if (merchant.beneficialOwnerResults && merchant.beneficialOwnerResults.length > 0) {
                    html += '<div class="owners-list">';
                    merchant.beneficialOwnerResults.forEach(owner => {
                        html += '<div class="owner-card">';
                        html += `<h4>${owner.fullName || 'Unknown'}</h4>`;
                        html += '<div class="owner-details">';
                        if (owner.screeningResult) {
                            const ownerStatus = owner.screeningResult.status || 'UNKNOWN';
                            html += `<div class="screening-status ${ownerStatus.toLowerCase()}">Screening: ${ownerStatus}</div>`;
                        }
                        html += `<div class="owner-flags">`;
                        if (owner.isSanctioned) {
                            html += '<span class="flag-badge sanctioned">SANCTIONED</span>';
                        }
                        if (owner.isPep) {
                            html += '<span class="flag-badge pep">PEP</span>';
                        }
                        if (!owner.isSanctioned && !owner.isPep) {
                            html += '<span class="flag-badge clear">CLEAR</span>';
                        }
                        html += '</div>';
                        html += '</div></div>';
                    });
                    html += '</div>';
                } else {
                    html += '<p class="no-data">No beneficial owners registered</p>';
                }
                html += '</div>';

                // Limits & Usage
                html += '<div class="merchant-section"><h3>Limits & Usage</h3>';
                html += '<div class="info-grid">';
                html += `<div class="info-item"><label>Daily Limit:</label><span>$${merchant.dailyLimit || '0'}</span></div>`;
                html += `<div class="info-item"><label>Current Usage:</label><span>$${merchant.currentUsage || '0'}</span></div>`;
                const usagePercent = merchant.dailyLimit && merchant.dailyLimit > 0
                    ? ((merchant.currentUsage || 0) / merchant.dailyLimit * 100).toFixed(1)
                    : 0;
                html += `<div class="info-item"><label>Usage:</label><span>${usagePercent}%</span></div>`;
                html += '</div></div>';

                html += '</div>';
                content.innerHTML = html;
            })
            .catch(err => {
                console.error('Error loading merchant:', err);
                handleApiError(err, 'viewMerchant');
                content.innerHTML = '<div class="error-message"><p>Error loading merchant details. Please check the error notification.</p></div>';
            });
    };

    window.closeViewMerchantModal = function () {
        document.getElementById('viewMerchantModal').style.display = 'none';
    };

    // --- Live Alerts for Dashboard ---
    window.fetchLiveAlerts = function () {
        fetch('dashboard/live-alerts?limit=5', getFetchOptions())
            .then(res => {
                if (!res.ok) throw new Error(`HTTP ${res.status}`);
                return res.json();
            })
            .then(alerts => {
                // Find the alerts-list in the dashboard-view
                const alertsList = document.querySelector('#dashboard-view .alerts-list');
                if (!alertsList) return;

                if (!alerts || alerts.length === 0) {
                    alertsList.innerHTML = '<div class="alert-item"><div class="alert-content"><p>No active alerts</p></div></div>';
                    return;
                }

                let html = '';
                alerts.forEach(alert => {
                    const severity = alert.severity || 'INFO';
                    const severityClass = severity.toLowerCase();
                    const timeAgo = alert.createdAt ? getTimeAgo(new Date(alert.createdAt)) : 'Unknown';

                    // Format alert message based on available data
                    let alertMessage = alert.reason || 'Transaction Alert';
                    if (alert.merchantId) {
                        alertMessage += ` - Merchant #${alert.merchantId}`;
                    }
                    if (alert.score) {
                        alertMessage += ` (Score: ${alert.score.toFixed(2)})`;
                    }
                    if (alert.txnId) {
                        alertMessage += ` | Txn: ${alert.txnId}`;
                    }

                    html += `
                        <div class="alert-item ${severityClass}">
                            <div class="alert-icon"><i class="fas fa-exclamation-triangle"></i></div>
                            <div class="alert-content">
                                <strong>${alertMessage}</strong>
                                ${alert.action ? `<p>Action: ${alert.action}</p>` : ''}
                                <span class="alert-time">${timeAgo}</span>
                            </div>
                        </div>
                    `;
                });
                alertsList.innerHTML = html;
            })
            .catch(err => {
                console.error('Error fetching live alerts:', err);
                // Keep default content on error
            });
    };

    // --- Recent Activity (Transactions) for Dashboard ---
    window.fetchRecentActivity = function () {
        fetch('dashboard/recent-transactions?limit=5', getFetchOptions())
            .then(res => {
                if (!res.ok) throw new Error(`HTTP ${res.status}`);
                return res.json();
            })
            .then(transactions => {
                // Find the activity-timeline in the dashboard-view
                const activityTimeline = document.querySelector('#dashboard-view .activity-timeline');
                if (!activityTimeline) return;

                if (!transactions || transactions.length === 0) {
                    activityTimeline.innerHTML = '<div class="activity-item"><div class="activity-content"><p>No recent transactions</p></div></div>';
                    return;
                }

                let html = '';
                transactions.forEach(txn => {
                    const timeAgo = txn.txnTs ? getTimeAgo(new Date(txn.txnTs)) : 'Unknown';

                    // Format amount
                    let amountDisplay = 'N/A';
                    if (txn.amountCents) {
                        const amount = (txn.amountCents / 100);
                        const currency = txn.currency || (window.currencyFormatter ? window.currencyFormatter.getDefaultCurrency() : 'USD');
                        amountDisplay = formatCurrency(amount, currency);
                    }

                    // Format transaction description
                    let description = `Transaction ${txn.txnId || 'N/A'}`;
                    if (txn.merchantId) {
                        description = `Merchant ${txn.merchantId} - ${amountDisplay}`;
                    } else {
                        description = `${description} - ${amountDisplay}`;
                    }

                    // Add terminal info if available
                    if (txn.terminalId) {
                        description += ` (Terminal: ${txn.terminalId})`;
                    }

                    html += `
                        <div class="activity-item">
                            <div class="activity-dot"></div>
                            <div class="activity-content">
                                <span class="activity-user">${description}</span>
                                <span class="activity-time">${timeAgo}</span>
                            </div>
                        </div>
                    `;
                });
                activityTimeline.innerHTML = html;
            })
            .catch(err => {
                console.error('Error fetching recent activity:', err);
                // Keep default content on error
            });
    };

    // Load initial data and initialize charts
    fetchSanctionsStatus();
    initCharts();
    updateAlertsCount();
    updateSarCount();
    updateCasesCount();
    updateMessagesCount();
    fetchLiveAlerts();
    fetchRecentActivity();

    // Ensure all modals are hidden on page load
    document.querySelectorAll('.modal').forEach(modal => {
        modal.style.display = 'none';
    });
});

// Function to update alerts count badge
window.updateAlertsCount = function () {
    fetch('alerts/count/active', getFetchOptions())
        .then(res => {
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            return res.json();
        })
        .then(data => {
            const badge = document.getElementById('alerts-count-badge');
            if (badge) {
                const count = data.count || 0;
                badge.textContent = count;
                // Hide badge if count is 0
                if (count === 0) {
                    badge.style.display = 'none';
                } else {
                    badge.style.display = 'inline-block';
                }
            }
        })
        .catch(err => {
            console.error('Error fetching alerts count:', err);
            // Keep default value on error
        });
};

// Update alerts count periodically (every 30 seconds)
setInterval(window.updateAlertsCount, 30000);

// Function to update SAR reports count badge (non-exported SARs)
window.updateSarCount = function () {
    fetch('compliance/sar/count/not-exported', getFetchOptions())
        .then(res => {
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            return res.json();
        })
        .then(data => {
            const badge = document.getElementById('sar-count-badge');
            if (badge) {
                const count = data.count || 0;
                badge.textContent = count;
                // Hide badge if count is 0
                if (count === 0) {
                    badge.style.display = 'none';
                } else {
                    badge.style.display = 'inline-block';
                }
            }
        })
        .catch(err => {
            console.error('Error fetching SAR count:', err);
            // Keep default value on error
        });
};

// Update SAR count periodically (every 30 seconds)
setInterval(window.updateSarCount, 30000);

// Function to update cases count badge
window.updateCasesCount = function () {
    fetch('compliance/cases/count', getFetchOptions())
        .then(res => {
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            return res.json();
        })
        .then(data => {
            const badge = document.getElementById('cases-count-badge');
            if (badge) {
                const count = data.count || 0;
                badge.textContent = count;
                // Hide badge if count is 0
                if (count === 0) {
                    badge.style.display = 'none';
                } else {
                    badge.style.display = 'inline-block';
                }
            }
        })
        .catch(err => {
            console.error('Error fetching cases count:', err);
            // Keep default value on error
        });
};

// Update cases count periodically (every 30 seconds)
setInterval(window.updateCasesCount, 30000);

// Function to update messages count badge (unread messages)
window.updateMessagesCount = function () {
    fetch('messages/unread/count', getFetchOptions())
        .then(res => {
            if (!res.ok) throw new Error(`HTTP ${res.status}`);
            return res.json();
        })
        .then(data => {
            const badge = document.getElementById('messages-count-badge');
            if (badge) {
                const count = data.count || 0;
                badge.textContent = count;
                // Hide badge if count is 0
                if (count === 0) {
                    badge.style.display = 'none';
                } else {
                    badge.style.display = 'inline-block';
                }
            }
        })
        .catch(err => {
            console.error('Error fetching messages count:', err);
            // Keep default value on error
        });
};

// Update messages count periodically (every 30 seconds)
setInterval(window.updateMessagesCount, 30000);

// Update live alerts periodically (every 30 seconds) - only if dashboard view is visible
setInterval(() => {
    const dashboardView = document.getElementById('dashboard-view');
    if (dashboardView && dashboardView.style.display !== 'none') {
        window.fetchLiveAlerts();
        window.fetchRecentActivity();
    }
}, 30000);
