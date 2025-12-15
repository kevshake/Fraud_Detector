/**
 * AML Fraud Detector - Dashboard JavaScript
 * Handles charts, interactions, and real-time updates
 */

document.addEventListener('DOMContentLoaded', function () {
    initializeCharts();
    initializeNavigation();
    initializeRealTimeUpdates();
});

// ==========================================================================
// CHART INITIALIZATION
// ==========================================================================

function initializeCharts() {
    initRiskPieChart();
    initTransactionLineChart();
}

function initRiskPieChart() {
    const ctx = document.getElementById('riskPieChart');
    if (!ctx) return;

    new Chart(ctx, {
        type: 'doughnut',
        data: {
            labels: ['High Risk', 'Medium Risk', 'Low Risk'],
            datasets: [{
                data: [15, 35, 50],
                backgroundColor: [
                    '#e74c3c',
                    '#f39c12',
                    '#27ae60'
                ],
                borderWidth: 0,
                hoverOffset: 4
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            cutout: '70%',
            plugins: {
                legend: {
                    display: false
                }
            }
        }
    });
}

function initTransactionLineChart() {
    const ctx = document.getElementById('transactionChart');
    if (!ctx) return;

    const gradient = ctx.getContext('2d').createLinearGradient(0, 0, 0, 300);
    gradient.addColorStop(0, 'rgba(241, 196, 15, 0.3)');
    gradient.addColorStop(1, 'rgba(241, 196, 15, 0)');

    const labels = ['Dec 19', 'Dec 25', 'Dec 31', 'Jan 10', 'Jan 14', 'Jan 20', 'Jan 27', 'Jan 30', 'Feb 2', 'Feb 8', 'Feb 15', 'Feb 22', 'Feb 28', 'Mar 7', 'Mar 17'];

    new Chart(ctx, {
        type: 'line',
        data: {
            labels: labels,
            datasets: [{
                label: 'Transactions',
                data: [0.5, 2.5, 3, 4, 3.5, 2.8, 2.5, 2.2, 2.8, 0.8, 1.2, 3.5, 4, 3.2, 4.2],
                borderColor: '#f1c40f',
                backgroundColor: gradient,
                borderWidth: 2,
                fill: true,
                tension: 0.4,
                pointRadius: 0,
                pointHoverRadius: 6,
                pointHoverBackgroundColor: '#f1c40f',
                pointHoverBorderColor: '#fff',
                pointHoverBorderWidth: 2
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            interaction: {
                intersect: false,
                mode: 'index'
            },
            scales: {
                x: {
                    grid: {
                        color: 'rgba(255, 255, 255, 0.05)',
                        drawBorder: false
                    },
                    ticks: {
                        color: 'rgba(255, 255, 255, 0.5)',
                        font: {
                            size: 11
                        }
                    }
                },
                y: {
                    min: 0,
                    max: 5,
                    grid: {
                        color: 'rgba(255, 255, 255, 0.05)',
                        drawBorder: false
                    },
                    ticks: {
                        color: 'rgba(255, 255, 255, 0.5)',
                        font: {
                            size: 11
                        }
                    }
                }
            },
            plugins: {
                legend: {
                    display: false
                },
                tooltip: {
                    backgroundColor: 'rgba(20, 25, 45, 0.95)',
                    borderColor: 'rgba(255, 255, 255, 0.1)',
                    borderWidth: 1,
                    titleColor: '#fff',
                    bodyColor: 'rgba(255, 255, 255, 0.7)',
                    padding: 12,
                    displayColors: false,
                    callbacks: {
                        label: function (context) {
                            return `${context.parsed.y.toFixed(1)}K transactions`;
                        }
                    }
                }
            }
        }
    });
}

// ==========================================================================
// NAVIGATION
// ==========================================================================

function initializeNavigation() {
    // Toggle submenus
    const navItems = document.querySelectorAll('.nav-item');
    navItems.forEach(item => {
        item.addEventListener('click', function (e) {
            const submenu = this.nextElementSibling;
            if (submenu && submenu.classList.contains('nav-submenu')) {
                e.preventDefault();
                submenu.classList.toggle('show');
                this.classList.toggle('active');
            }
        });
    });

    // Chart tabs
    const chartTabs = document.querySelectorAll('.chart-tabs .tab');
    chartTabs.forEach(tab => {
        tab.addEventListener('click', function () {
            this.parentElement.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
            this.classList.add('active');
        });
    });

    // Mobile sidebar toggle
    document.addEventListener('keydown', function (e) {
        if (e.key === 'Escape') {
            document.querySelector('.sidebar').classList.remove('open');
        }
    });
}

// ==========================================================================
// REAL-TIME UPDATES (Simulated)
// ==========================================================================

function initializeRealTimeUpdates() {
    // Simulate real-time stat updates
    setInterval(updateStats, 5000);

    // Simulate new alerts
    setInterval(checkForNewAlerts, 10000);
}

function updateStats() {
    const statValue = document.querySelector('.stat-value');
    if (statValue) {
        const currentValue = parseInt(statValue.textContent);
        const change = Math.floor(Math.random() * 5) - 2; // Random change between -2 and +2
        statValue.textContent = Math.max(0, currentValue + change);
    }
}

function checkForNewAlerts() {
    // Simulate checking for new alerts
    const notificationCount = document.querySelector('.notification-count');
    if (notificationCount) {
        const current = parseInt(notificationCount.textContent);
        if (Math.random() > 0.7) {
            notificationCount.textContent = current + 1;
            showToast('New alert received');
        }
    }
}

function showToast(message) {
    // Create toast notification
    const toast = document.createElement('div');
    toast.className = 'toast-notification';
    toast.innerHTML = `
        <i class="fas fa-bell"></i>
        <span>${message}</span>
    `;

    // Add styles
    toast.style.cssText = `
        position: fixed;
        bottom: 20px;
        right: 20px;
        background: rgba(108, 92, 231, 0.95);
        color: white;
        padding: 12px 20px;
        border-radius: 8px;
        display: flex;
        align-items: center;
        gap: 10px;
        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3);
        z-index: 1000;
        animation: slideIn 0.3s ease;
    `;

    document.body.appendChild(toast);

    // Remove after 3 seconds
    setTimeout(() => {
        toast.style.animation = 'slideOut 0.3s ease';
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

// Add animation keyframes
const style = document.createElement('style');
style.textContent = `
    @keyframes slideIn {
        from { transform: translateX(100%); opacity: 0; }
        to { transform: translateX(0); opacity: 1; }
    }
    @keyframes slideOut {
        from { transform: translateX(0); opacity: 1; }
        to { transform: translateX(100%); opacity: 0; }
    }
`;
document.head.appendChild(style);

// ==========================================================================
// API INTEGRATION (Ready to connect to backend)
// ==========================================================================

const API = {
    baseUrl: '/api/v1',

    async getCases(status = null) {
        const params = status ? `?status=${status}` : '';
        const response = await fetch(`${this.baseUrl}/compliance/cases${params}`);
        return response.json();
    },

    async getCaseStats() {
        const response = await fetch(`${this.baseUrl}/compliance/cases/stats`);
        return response.json();
    },

    async getAlerts() {
        const response = await fetch(`${this.baseUrl}/alerts`);
        return response.json();
    },

    async getSarReports() {
        const response = await fetch(`${this.baseUrl}/compliance/sar`);
        return response.json();
    },

    async getAuditLogs(start, end) {
        const response = await fetch(`${this.baseUrl}/audit/logs/range?start=${start}&end=${end}`);
        return response.json();
    }
};

// ==========================================================================
// THEME MANAGEMENT (For PSP/Bank customization)
// ==========================================================================

const ThemeManager = {
    themes: {
        default: {
            primaryColor: '#6c5ce7',
            primaryLight: '#a29bfe',
            bgGradientStart: '#1a1f37',
            bgGradientEnd: '#0f1322'
        },
        blueBank: {
            primaryColor: '#0066cc',
            primaryLight: '#3399ff',
            bgGradientStart: '#001a33',
            bgGradientEnd: '#000d1a'
        },
        greenFintech: {
            primaryColor: '#00b894',
            primaryLight: '#55efc4',
            bgGradientStart: '#00332a',
            bgGradientEnd: '#001a15'
        },
        redSecurity: {
            primaryColor: '#e74c3c',
            primaryLight: '#ff6b6b',
            bgGradientStart: '#2d1f1f',
            bgGradientEnd: '#1a1212'
        }
    },

    apply(themeName) {
        const theme = this.themes[themeName];
        if (!theme) return;

        const root = document.documentElement;
        root.style.setProperty('--primary-color', theme.primaryColor);
        root.style.setProperty('--primary-light', theme.primaryLight);
        root.style.setProperty('--bg-gradient-start', theme.bgGradientStart);
        root.style.setProperty('--bg-gradient-end', theme.bgGradientEnd);

        // Save preference
        localStorage.setItem('aml-theme', themeName);
    },

    loadSaved() {
        const saved = localStorage.getItem('aml-theme');
        if (saved) {
            this.apply(saved);
        }
    }
};

// Load saved theme on startup
ThemeManager.loadSaved();

// Export for console access
window.ThemeManager = ThemeManager;
window.API = API;
