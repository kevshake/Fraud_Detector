/**
 * Transaction Monitoring Module
 * Handles transaction monitoring dashboard functionality
 */

let currentMonitoringTab = 'live-monitoring';
let currentRiskFilter = 'All';
let currentDecisionFilter = 'All';

let transactionMonitoringStreamInterval = null;

function initTransactionMonitoring() {
    // Determine which view is active based on current view ID
    const currentView = Array.from(document.querySelectorAll('[id$="-view"]'))
        .find(el => el.style.display !== 'none');
    
    if (!currentView) return;
    
    const viewId = currentView.id;
    
    if (viewId === 'transaction-monitoring-live-view') {
        loadLiveMonitoring();
        startTransactionMonitoringStream();
    } else if (viewId === 'transaction-monitoring-analytics-view') {
        loadAnalytics();
    } else if (viewId === 'transaction-monitoring-sars-view') {
        loadMonitoringSARs();
    } else if (viewId === 'transaction-monitoring-reports-view') {
        // Reports view initialization if needed
    }
}

function startTransactionMonitoringStream() {
    if (transactionMonitoringStreamInterval !== null) {
        clearInterval(transactionMonitoringStreamInterval);
    }
    transactionMonitoringStreamInterval = setInterval(() => {
        const liveView = document.getElementById('transaction-monitoring-live-view');
        if (liveView && liveView.style.display !== 'none') {
            loadLiveMonitoring();
        } else {
            stopTransactionMonitoringStream();
        }
    }, 5000); // Refresh every 5 seconds
}

function stopTransactionMonitoringStream() {
    if (transactionMonitoringStreamInterval !== null) {
        clearInterval(transactionMonitoringStreamInterval);
        transactionMonitoringStreamInterval = null;
    }
}

window.toggleTransactionStream = function() {
    if (transactionMonitoringStreamInterval === null) {
        startTransactionMonitoringStream();
        const btn = document.getElementById('stream-toggle-btn');
        if (btn) btn.innerHTML = '<i class="fas fa-pause"></i> Pause Stream';
    } else {
        stopTransactionMonitoringStream();
        const btn = document.getElementById('stream-toggle-btn');
        if (btn) btn.innerHTML = '<i class="fas fa-play"></i> Resume Stream';
    }
};

// Live Monitoring
function loadLiveMonitoring() {
    const container = document.getElementById('transactions-list');
    if (!container) return;
    
    const riskLevel = document.getElementById('risk-level-filter')?.value || 'All';
    const decision = document.getElementById('decision-filter')?.value || 'All';
    
    // Show loading state
    if (container.innerHTML.trim() === '' || container.innerHTML.includes('Loading')) {
        container.innerHTML = '<div class="loading-spinner"></div><p style="text-align:center;margin-top:10px;">Loading transactions...</p>';
    }
    
    fetch(`monitoring/transactions?riskLevel=${riskLevel}&decision=${decision}&limit=100`, getFetchOptions())
    .then(response => {
        if (!response.ok) throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        return response.json();
    })
    .then(data => {
        if (!data || data.length === 0) {
            container.innerHTML = '<div class="empty-state"><i class="fas fa-inbox"></i><p>No transactions found matching the filters.</p><p class="text-muted">Transactions will appear here as they are processed.</p></div>';
            return;
        }
        renderTransactions(data);
    })
    .catch(error => {
        console.error('Error loading transactions:', error);
        container.innerHTML = `
            <div class="error-state">
                <i class="fas fa-exclamation-circle"></i>
                <p><strong>Error loading transactions</strong></p>
                <p class="text-muted">${error.message || 'Please try again later.'}</p>
                <button class="btn-primary" onclick="loadLiveMonitoring()" style="margin-top:10px;">
                    <i class="fas fa-redo"></i> Retry
                </button>
            </div>
        `;
    });
}

function renderTransactions(transactions) {
    const container = document.getElementById('transactions-list');
    if (!container) return;
    
    container.innerHTML = '';
    
    if (!transactions || transactions.length === 0) {
        container.innerHTML = '<div class="empty-state"><i class="fas fa-inbox"></i><p>No transactions found.</p></div>';
        return;
    }
    
    // Sort by timestamp descending (newest first)
    const sortedTransactions = [...transactions].sort((a, b) => {
        const timeA = a.timestamp || a.txnTs || a.createdAt || 0;
        const timeB = b.timestamp || b.txnTs || b.createdAt || 0;
        return new Date(timeB) - new Date(timeA);
    });
    
    sortedTransactions.forEach(txn => {
        const card = createTransactionCard(txn);
        container.appendChild(card);
    });
}

function createTransactionCard(txn) {
    const card = document.createElement('div');
    card.className = 'transaction-card';
    
    // Extract transaction data - handle both monitoring format and raw transaction format
    const txnId = txn.id || txn.txnId || txn.transactionId || 'N/A';
    const currency =
        txn.currency ||
        (window.currencyFormatter && typeof window.currencyFormatter.getDefaultCurrency === 'function'
            ? window.currencyFormatter.getDefaultCurrency()
            : 'USD');
    const amount = txn.amount != null ? txn.amount : (txn.amountCents ? (txn.amountCents / 100) : 0);
    const merchantId = txn.merchantId || 'N/A';
    const timestamp = txn.timestamp || txn.txnTs || txn.createdAt;
    const riskLevel = txn.riskLevel || (txn.riskScore >= 75 ? 'HIGH' : txn.riskScore >= 50 ? 'MEDIUM' : 'LOW');
    const riskScore = txn.riskScore || 0;
    const decision = txn.decision || txn.action || 'PROCESSED';

    const amountDisplay =
        window.currencyFormatter && typeof window.currencyFormatter.format === 'function'
            ? window.currencyFormatter.format(amount, currency)
            : `${currency} ${Number(amount || 0).toFixed(2)}`;
    
    const riskClass = getRiskLevelClass(riskLevel);
    const decisionClass = getDecisionClass(decision);
    
    card.innerHTML = `
        <div class="transaction-header">
            <div class="transaction-summary">
                <div class="transaction-id">Transaction #${txnId}</div>
                <div class="transaction-amount">${amountDisplay}</div>
                <div class="transaction-merchant">Merchant: ${merchantId}</div>
                <div class="transaction-risk-score">Risk Score: ${riskScore}/100</div>
            </div>
            <div class="transaction-badges">
                <span class="badge badge-${riskClass}">${riskLevel}</span>
                <span class="badge badge-${decisionClass}">${formatDecision(decision)}</span>
            </div>
        </div>
        <div class="transaction-details">
            ${txn.riskIndicators ? txn.riskIndicators.map(ind => `<span class="risk-tag">${ind}</span>`).join('') : ''}
            <div class="transaction-meta">
                <div class="meta-item">
                    <span class="meta-label">Terminal ID:</span>
                    <span class="meta-value">${txn.terminalId || 'N/A'}</span>
                </div>
                <div class="meta-item">
                    <span class="meta-label">IP Address:</span>
                    <span class="meta-value">${txn.ipAddress || 'N/A'}</span>
                </div>
                ${txn.deviceRisk !== undefined ? `
                <div class="meta-item">
                    <span class="meta-label">Device Risk:</span>
                    <span class="meta-value">${txn.deviceRisk || 0}/100</span>
                </div>
                ` : ''}
                ${txn.vpnDetected !== undefined ? `
                <div class="meta-item">
                    <span class="meta-label">VPN/TOR:</span>
                    <span class="meta-value ${txn.vpnDetected ? 'warning' : 'success'}">${txn.vpnDetected ? 'Detected' : 'Clean'}</span>
                </div>
                ` : ''}
                ${txn.sanctionsStatus !== undefined ? `
                <div class="meta-item">
                    <span class="meta-label">Sanctions:</span>
                    <span class="meta-value ${txn.sanctionsStatus === 'FLAGGED' ? 'danger' : 'success'}">${txn.sanctionsStatus || 'CLEAR'}</span>
                </div>
                ` : ''}
                <div class="meta-item">
                    <span class="meta-label">Time:</span>
                    <span class="meta-value">${formatTimestamp(timestamp)}</span>
                    ${timestamp ? `<small class="text-muted">${formatTimeAgo(timestamp)}</small>` : ''}
                </div>
            </div>
        </div>
        <div class="transaction-actions">
            <button class="btn-secondary" onclick="viewTransactionDetails('${txnId}')">
                <i class="fas fa-eye"></i> Details
            </button>
        </div>
    `;
    
    return card;
}

// Analytics
function loadAnalytics() {
    // Load dashboard stats
    fetch('monitoring/dashboard/stats', getFetchOptions())
    .then(response => {
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        return response.json();
    })
    .then(data => {
        const totalEl = document.getElementById('total-monitored');
        const flaggedEl = document.getElementById('flagged-count');
        const flagRateEl = document.getElementById('flag-rate');
        const highRiskEl = document.getElementById('high-risk-count');
        const blockedEl = document.getElementById('blocked-count');
        
        if (totalEl) totalEl.textContent = data.totalMonitored || 0;
        if (flaggedEl) flaggedEl.textContent = data.flagged || 0;
        if (flagRateEl) flagRateEl.textContent = (data.flagRate || 0).toFixed(1) + '% flag rate';
        if (highRiskEl) highRiskEl.textContent = data.highRisk || 0;
        if (blockedEl) blockedEl.textContent = data.blocked || 0;
    })
    .catch(error => {
        console.error('Error loading stats:', error);
        const totalEl = document.getElementById('total-monitored');
        if (totalEl) totalEl.textContent = 'Error';
    });
    
    // Load risk distribution
    const riskChartEl = document.getElementById('risk-distribution-chart');
    if (riskChartEl) {
        riskChartEl.innerHTML = '<div class="loading-spinner"></div>';
        fetch('monitoring/risk-distribution', getFetchOptions())
        .then(response => {
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            return response.json();
        })
        .then(data => {
            renderRiskDistribution(data);
        })
        .catch(error => {
            console.error('Error loading risk distribution:', error);
            riskChartEl.innerHTML = '<div class="error-state"><p>Error loading risk distribution</p></div>';
        });
    }
    
    // Load top risk indicators
    const indicatorsEl = document.getElementById('risk-indicators-list');
    if (indicatorsEl) {
        indicatorsEl.innerHTML = '<div class="loading-spinner"></div>';
        fetch('monitoring/risk-indicators', getFetchOptions())
        .then(response => {
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            return response.json();
        })
        .then(data => {
            renderRiskIndicators(data);
        })
        .catch(error => {
            console.error('Error loading risk indicators:', error);
            indicatorsEl.innerHTML = '<div class="error-state"><p>Error loading risk indicators</p></div>';
        });
    }
    
    // Load recent activity
    const activityEl = document.getElementById('recent-activity-list');
    if (activityEl) {
        activityEl.innerHTML = '<div class="loading-spinner"></div>';
        fetch('monitoring/recent-activity', getFetchOptions())
        .then(response => {
            if (!response.ok) throw new Error(`HTTP ${response.status}`);
            return response.json();
        })
        .then(data => {
            renderRecentActivity(data);
        })
        .catch(error => {
            console.error('Error loading recent activity:', error);
            activityEl.innerHTML = '<div class="error-state"><p>Error loading recent activity</p></div>';
        });
    }
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
    const container = document.getElementById('sars-list');
    if (!container) return;
    
    container.innerHTML = '<div class="loading-spinner"></div><p style="text-align:center;margin-top:10px;">Loading SARs...</p>';
    
    fetch('monitoring/sars', getFetchOptions())
    .then(response => {
        if (!response.ok) throw new Error(`HTTP ${response.status}: ${response.statusText}`);
        return response.json();
    })
    .then(data => {
        if (!data || data.length === 0) {
            container.innerHTML = '<div class="empty-state"><i class="fas fa-file-alt"></i><p>No SARs found.</p><p class="text-muted">SARs generated from monitoring will appear here.</p></div>';
            return;
        }
        renderSARs(data);
    })
    .catch(error => {
        console.error('Error loading SARs:', error);
        container.innerHTML = `
            <div class="error-state">
                <i class="fas fa-exclamation-circle"></i>
                <p><strong>Error loading SARs</strong></p>
                <p class="text-muted">${error.message || 'Please try again later.'}</p>
                <button class="btn-primary" onclick="loadMonitoringSARs()" style="margin-top:10px;">
                    <i class="fas fa-redo"></i> Retry
                </button>
            </div>
        `;
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
    
    const sarId = sar.id || sar.sarId || 'N/A';
    const sarRef = sar.sarReference || `SAR-${sarId}`;
    const priority = sar.priority || 'MEDIUM';
    const status = sar.status || 'DRAFT';
    const priorityClass = getPriorityClass(priority);
    const statusClass = getStatusClass(status);
    
    card.innerHTML = `
        <div class="sar-header">
            <div class="sar-id">${sarRef}</div>
            <div class="sar-badges">
                <span class="badge badge-${priorityClass}">${priority}</span>
                <span class="badge badge-${statusClass}">${formatStatus(status)}</span>
            </div>
        </div>
        <div class="sar-title">${sar.sarType || sar.title || 'Suspicious Activity Report'}</div>
        <div class="sar-description">${sar.description || sar.reason || 'No description available'}</div>
        <div class="sar-footer">
            <div class="sar-meta">
                <span>Created: ${formatDate(sar.createdAt)}</span>
                <span>Jurisdiction: ${sar.jurisdiction || 'N/A'}</span>
                <span>Transactions: ${sar.transactionCount || sar.relatedTransactionCount || 0}</span>
                <span>Status: ${formatStatus(status)}</span>
            </div>
            <div class="sar-actions">
                <button class="btn-secondary btn-sm" onclick="viewSARDetails(${sarId})">
                    <i class="fas fa-eye"></i> View
                </button>
            </div>
        </div>
    `;
    
    return card;
}

window.viewSARDetails = function(sarId) {
    // Navigate to SAR view or show details
    window.showView('sar-view');
    // Could also load specific SAR details here
};

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

window.generateDeclineReport = function() {
    const resultsEl = document.getElementById('reports-results');
    if (resultsEl) {
        resultsEl.innerHTML = '<div class="loading-spinner"></div><p>Generating decline report...</p>';
    }
    
    // Fetch decline report data from backend
    fetch('monitoring/reports/declines', getFetchOptions())
    .then(response => {
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        return response.json();
    })
    .then(data => {
        if (resultsEl) {
            resultsEl.innerHTML = `
                <div class="report-result-card">
                    <h4>Decline Report Generated</h4>
                    <p><strong>Total Declines:</strong> ${data.totalDeclines || 0}</p>
                    <p><strong>Period:</strong> ${data.startDate || 'N/A'} to ${data.endDate || 'N/A'}</p>
                    <button class="btn-primary" onclick="exportReport('decline', ${JSON.stringify(data).replace(/"/g, '&quot;')})">
                        <i class="fas fa-download"></i> Export Report
                    </button>
                </div>
            `;
        }
    })
    .catch(error => {
        console.error('Error generating decline report:', error);
        if (resultsEl) {
            resultsEl.innerHTML = `<div class="error-state"><p>Error generating report: ${error.message}</p></div>`;
        }
    });
};

window.generateMonitoringReport = function() {
    const resultsEl = document.getElementById('reports-results');
    if (resultsEl) {
        resultsEl.innerHTML = '<div class="loading-spinner"></div><p>Generating monitoring summary...</p>';
    }
    
    fetch('monitoring/reports/summary', getFetchOptions())
    .then(response => {
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        return response.json();
    })
    .then(data => {
        if (resultsEl) {
            resultsEl.innerHTML = `
                <div class="report-result-card">
                    <h4>Monitoring Summary Report</h4>
                    <p><strong>Total Monitored:</strong> ${data.totalMonitored || 0}</p>
                    <p><strong>Flagged:</strong> ${data.flagged || 0}</p>
                    <p><strong>High Risk:</strong> ${data.highRisk || 0}</p>
                    <p><strong>Blocked:</strong> ${data.blocked || 0}</p>
                    <button class="btn-primary" onclick="exportReport('monitoring', ${JSON.stringify(data).replace(/"/g, '&quot;')})">
                        <i class="fas fa-download"></i> Export Report
                    </button>
                </div>
            `;
        }
    })
    .catch(error => {
        console.error('Error generating monitoring report:', error);
        if (resultsEl) {
            resultsEl.innerHTML = `<div class="error-state"><p>Error generating report: ${error.message}</p></div>`;
        }
    });
};

window.exportReport = function(type, data) {
    const dataStr = JSON.stringify(data, null, 2);
    const blob = new Blob([dataStr], { type: 'application/json' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `${type}_report_${new Date().toISOString().split('T')[0]}.json`;
    link.click();
    URL.revokeObjectURL(url);
};

// Make getFetchOptions available if not already global
if (typeof getFetchOptions === 'undefined') {
    window.getFetchOptions = function(method = 'GET', body = null) {
        const options = {
            method: method,
            credentials: 'include',
            headers: {
                'Content-Type': 'application/json',
                'X-Requested-With': 'XMLHttpRequest'
            }
        };
        if (body) {
            options.body = typeof body === 'string' ? body : JSON.stringify(body);
        }
        return options;
    };
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

