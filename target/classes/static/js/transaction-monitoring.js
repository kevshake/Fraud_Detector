/**
 * Transaction Monitoring Module
 * Handles transaction monitoring dashboard functionality
 */

let currentMonitoringTab = 'live-monitoring';
let currentRiskFilter = 'All';
let currentDecisionFilter = 'All';

function initTransactionMonitoring() {
    setupMonitoringTabs();
    loadCurrentMonitoringTab();
}

function setupMonitoringTabs() {
    document.querySelectorAll('.monitoring-tab').forEach(tab => {
        tab.addEventListener('click', function() {
            const tabName = this.getAttribute('data-tab');
            switchMonitoringTab(tabName);
        });
    });
}

function switchMonitoringTab(tabName) {
    currentMonitoringTab = tabName;
    
    // Update tab buttons
    document.querySelectorAll('.monitoring-tab').forEach(tab => {
        tab.classList.remove('active');
        if (tab.getAttribute('data-tab') === tabName) {
            tab.classList.add('active');
        }
    });
    
    // Update tab content
    document.querySelectorAll('.monitoring-tab-content').forEach(content => {
        content.classList.remove('active');
    });
    
    const targetContent = document.getElementById(tabName + '-tab');
    if (targetContent) {
        targetContent.classList.add('active');
    }
    
    loadCurrentMonitoringTab();
}

function loadCurrentMonitoringTab() {
    switch(currentMonitoringTab) {
        case 'live-monitoring':
            loadLiveMonitoring();
            break;
        case 'analytics':
            loadAnalytics();
            break;
        case 'sars':
            loadMonitoringSARs();
            break;
        case 'reports':
            // Reports tab is static for now
            break;
    }
}

// Live Monitoring
function loadLiveMonitoring() {
    const riskLevel = document.getElementById('risk-level-filter')?.value || 'All';
    const decision = document.getElementById('decision-filter')?.value || 'All';
    
    fetch(`monitoring/transactions?riskLevel=${riskLevel}&decision=${decision}&limit=50`, {
        credentials: 'include'
    })
    .then(response => response.json())
    .then(data => {
        renderTransactions(data);
    })
    .catch(error => {
        console.error('Error loading transactions:', error);
    });
}

function renderTransactions(transactions) {
    const container = document.getElementById('transactions-list');
    container.innerHTML = '';
    
    if (transactions.length === 0) {
        container.innerHTML = '<div class="empty-state">No transactions found</div>';
        return;
    }
    
    transactions.forEach(txn => {
        const card = createTransactionCard(txn);
        container.appendChild(card);
    });
}

function createTransactionCard(txn) {
    const card = document.createElement('div');
    card.className = 'transaction-card';
    
    const riskClass = getRiskLevelClass(txn.riskLevel);
    const decisionClass = getDecisionClass(txn.decision);
    
    card.innerHTML = `
        <div class="transaction-header">
            <div class="transaction-summary">
                <div class="transaction-amount">${txn.amount}</div>
                <div class="transaction-risk-score">Risk Score: ${txn.riskScore}/100</div>
            </div>
            <div class="transaction-badges">
                <span class="badge badge-${riskClass}">${txn.riskLevel}</span>
                <span class="badge badge-${decisionClass}">${formatDecision(txn.decision)}</span>
            </div>
        </div>
        <div class="transaction-details">
            ${txn.riskIndicators ? txn.riskIndicators.map(ind => `<span class="risk-tag">${ind}</span>`).join('') : ''}
            <div class="transaction-meta">
                <div class="meta-item">
                    <span class="meta-label">Device Risk:</span>
                    <span class="meta-value">${txn.deviceRisk || 0}/100</span>
                </div>
                <div class="meta-item">
                    <span class="meta-label">VPN/TOR:</span>
                    <span class="meta-value ${txn.vpnDetected ? 'warning' : 'success'}">${txn.vpnDetected ? 'Detected' : 'Clean'}</span>
                </div>
                <div class="meta-item">
                    <span class="meta-label">Sanctions:</span>
                    <span class="meta-value ${txn.sanctionsStatus === 'FLAGGED' ? 'danger' : 'success'}">${txn.sanctionsStatus || 'CLEAR'}</span>
                </div>
                <div class="meta-item">
                    <span class="meta-label">Time:</span>
                    <span class="meta-value">${formatTimestamp(txn.timestamp)}</span>
                </div>
            </div>
        </div>
        <div class="transaction-actions">
            <button class="btn-secondary" onclick="viewTransactionDetails('${txn.id}')">
                <i class="fas fa-eye"></i> Details
            </button>
        </div>
    `;
    
    return card;
}

// Analytics
function loadAnalytics() {
    // Load dashboard stats
    fetch('monitoring/dashboard/stats', {
        credentials: 'include'
    })
    .then(response => response.json())
    .then(data => {
        document.getElementById('total-monitored').textContent = data.totalMonitored || 0;
        document.getElementById('flagged-count').textContent = data.flagged || 0;
        document.getElementById('flag-rate').textContent = (data.flagRate || 0).toFixed(1) + '% flag rate';
        document.getElementById('high-risk-count').textContent = data.highRisk || 0;
        document.getElementById('blocked-count').textContent = data.blocked || 0;
    })
    .catch(error => console.error('Error loading stats:', error));
    
    // Load risk distribution
    fetch('monitoring/risk-distribution', {
        credentials: 'include'
    })
    .then(response => response.json())
    .then(data => {
        renderRiskDistribution(data);
    })
    .catch(error => console.error('Error loading risk distribution:', error));
    
    // Load top risk indicators
    fetch('monitoring/risk-indicators', {
        credentials: 'include'
    })
    .then(response => response.json())
    .then(data => {
        renderRiskIndicators(data);
    })
    .catch(error => console.error('Error loading risk indicators:', error));
    
    // Load recent activity
    fetch('monitoring/recent-activity', {
        credentials: 'include'
    })
    .then(response => response.json())
    .then(data => {
        renderRecentActivity(data);
    })
    .catch(error => console.error('Error loading recent activity:', error));
}

function renderRiskDistribution(distribution) {
    const container = document.getElementById('risk-distribution-chart');
    const total = (distribution.low || 0) + (distribution.medium || 0) + 
                  (distribution.high || 0) + (distribution.critical || 0);
    const max = Math.max(distribution.low || 0, distribution.medium || 0, 
                        distribution.high || 0, distribution.critical || 0);
    
    container.innerHTML = `
        <div class="risk-bar-item">
            <div class="risk-bar-label">Low (0-25)</div>
            <div class="risk-bar-container">
                <div class="risk-bar risk-bar-low" style="width: ${max > 0 ? (distribution.low / max * 100) : 0}%"></div>
                <span class="risk-bar-value">${distribution.low || 0} transactions</span>
            </div>
        </div>
        <div class="risk-bar-item">
            <div class="risk-bar-label">Medium (26-50)</div>
            <div class="risk-bar-container">
                <div class="risk-bar risk-bar-medium" style="width: ${max > 0 ? (distribution.medium / max * 100) : 0}%"></div>
                <span class="risk-bar-value">${distribution.medium || 0} transactions</span>
            </div>
        </div>
        <div class="risk-bar-item">
            <div class="risk-bar-label">High (51-75)</div>
            <div class="risk-bar-container">
                <div class="risk-bar risk-bar-high" style="width: ${max > 0 ? (distribution.high / max * 100) : 0}%"></div>
                <span class="risk-bar-value">${distribution.high || 0} transactions</span>
            </div>
        </div>
        <div class="risk-bar-item">
            <div class="risk-bar-label">Critical (76-100)</div>
            <div class="risk-bar-container">
                <div class="risk-bar risk-bar-critical" style="width: ${max > 0 ? (distribution.critical / max * 100) : 0}%"></div>
                <span class="risk-bar-value">${distribution.critical || 0} transactions</span>
            </div>
        </div>
    `;
}

function renderRiskIndicators(indicators) {
    const container = document.getElementById('risk-indicators-list');
    container.innerHTML = '';
    
    indicators.forEach(indicator => {
        const item = document.createElement('div');
        item.className = 'risk-indicator-item';
        item.innerHTML = `
            <div class="indicator-name">${indicator.name}</div>
            <div class="indicator-description">${indicator.description}</div>
            <div class="indicator-count">${indicator.count} cases</div>
        `;
        container.appendChild(item);
    });
}

function renderRecentActivity(activities) {
    const container = document.getElementById('recent-activity-list');
    container.innerHTML = '';
    
    activities.forEach(activity => {
        const item = document.createElement('div');
        item.className = 'activity-item';
        const activityClass = getActivityClass(activity.type);
        item.innerHTML = `
            <div class="activity-dot ${activityClass}"></div>
            <div class="activity-content">
                <div class="activity-title">${getActivityTitle(activity.type)}</div>
                <div class="activity-details">
                    Transaction #${activity.transactionId}, ${activity.amount}, ${activity.description}
                </div>
                <div class="activity-time">${formatTimeAgo(activity.timestamp)}</div>
            </div>
        `;
        container.appendChild(item);
    });
}

// SARs
function loadMonitoringSARs() {
    fetch('monitoring/sars', {
        credentials: 'include'
    })
    .then(response => response.json())
    .then(data => {
        renderSARs(data);
    })
    .catch(error => {
        console.error('Error loading SARs:', error);
    });
}

function renderSARs(sars) {
    const container = document.getElementById('sars-list');
    container.innerHTML = '';
    
    if (sars.length === 0) {
        container.innerHTML = '<div class="empty-state">No SARs found</div>';
        return;
    }
    
    sars.forEach(sar => {
        const card = createSARCard(sar);
        container.appendChild(card);
    });
}

function createSARCard(sar) {
    const card = document.createElement('div');
    card.className = 'sar-card';
    
    const priorityClass = getPriorityClass(sar.priority);
    const statusClass = getStatusClass(sar.status);
    
    card.innerHTML = `
        <div class="sar-header">
            <div class="sar-id">${sar.id}</div>
            <div class="sar-badges">
                <span class="badge badge-${priorityClass}">${sar.priority}</span>
                <span class="badge badge-${statusClass}">${formatStatus(sar.status)}</span>
            </div>
        </div>
        <div class="sar-title">${sar.title || 'Suspicious Activity'}</div>
        <div class="sar-description">${sar.description || ''}</div>
        <div class="sar-footer">
            <div class="sar-meta">
                <span>Created: ${formatDate(sar.createdAt)}</span>
                <span>Transactions: ${sar.transactionCount || 0}</span>
                <span>Submitted: ${sar.submitted ? 'Yes' : 'No'}</span>
            </div>
        </div>
    `;
    
    return card;
}

// Utility functions
function getRiskLevelClass(riskLevel) {
    const level = (riskLevel || '').toUpperCase();
    switch(level) {
        case 'LOW': return 'success';
        case 'MEDIUM': return 'warning';
        case 'HIGH': return 'danger';
        case 'CRITICAL': return 'danger';
        default: return 'secondary';
    }
}

function getDecisionClass(decision) {
    const dec = (decision || '').toUpperCase();
    switch(dec) {
        case 'APPROVED': return 'success';
        case 'MANUAL_REVIEW': return 'warning';
        case 'DECLINED': return 'danger';
        default: return 'secondary';
    }
}

function formatDecision(decision) {
    return (decision || '').replace('_', ' ');
}

function formatTimestamp(timestamp) {
    if (!timestamp) return 'N/A';
    const date = new Date(timestamp);
    return date.toLocaleString();
}

function formatDate(date) {
    if (!date) return 'N/A';
    const d = new Date(date);
    return d.toLocaleDateString();
}

function formatTimeAgo(timestamp) {
    if (!timestamp) return 'N/A';
    const now = new Date();
    const time = new Date(timestamp);
    const diffMs = now - time;
    const diffMins = Math.floor(diffMs / 60000);
    
    if (diffMins < 1) return 'Just now';
    if (diffMins < 60) return diffMins + ' min ago';
    const diffHours = Math.floor(diffMins / 60);
    if (diffHours < 24) return diffHours + ' hour' + (diffHours > 1 ? 's' : '') + ' ago';
    const diffDays = Math.floor(diffHours / 24);
    return diffDays + ' day' + (diffDays > 1 ? 's' : '') + ' ago';
}

function getActivityClass(type) {
    switch(type) {
        case 'blocked': return 'danger';
        case 'review': return 'warning';
        case 'approved': return 'success';
        default: return 'secondary';
    }
}

function getActivityTitle(type) {
    switch(type) {
        case 'blocked': return 'High-risk transaction blocked';
        case 'review': return 'Velocity threshold exceeded';
        case 'approved': return 'Low-risk transaction approved';
        default: return 'Transaction processed';
    }
}

function getPriorityClass(priority) {
    const p = (priority || '').toUpperCase();
    switch(p) {
        case 'URGENT': return 'danger';
        case 'HIGH': return 'warning';
        case 'MEDIUM': return 'success';
        default: return 'secondary';
    }
}

function getStatusClass(status) {
    const s = (status || '').toUpperCase();
    switch(s) {
        case 'SUBMITTED': return 'primary';
        case 'ACKNOWLEDGED': return 'success';
        case 'DRAFT': return 'secondary';
        default: return 'secondary';
    }
}

function formatStatus(status) {
    return (status || '').replace('_', ' ');
}

function viewTransactionDetails(transactionId) {
    alert('View transaction details: ' + transactionId);
}

function generateSampleReport() {
    alert('Generating sample report...');
}

// Filter event listeners
document.addEventListener('DOMContentLoaded', function() {
    const riskFilter = document.getElementById('risk-level-filter');
    const decisionFilter = document.getElementById('decision-filter');
    
    if (riskFilter) {
        riskFilter.addEventListener('change', function() {
            currentRiskFilter = this.value;
            loadLiveMonitoring();
        });
    }
    
    if (decisionFilter) {
        decisionFilter.addEventListener('change', function() {
            currentDecisionFilter = this.value;
            loadLiveMonitoring();
        });
    }
});

