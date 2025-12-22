/**
 * Limits & AML Management Module
 * Handles all limits management functionality
 */

let currentTab = 'merchant-limits';

function initLimitsAmlManagement() {
    loadDashboardStats();
    setupTabNavigation();
    loadCurrentTab();
}

function setupTabNavigation() {
    document.querySelectorAll('.limits-tab').forEach(tab => {
        tab.addEventListener('click', function() {
            const tabName = this.getAttribute('data-tab');
            switchTab(tabName);
        });
    });
}

function switchTab(tabName) {
    currentTab = tabName;
    
    // Update tab buttons
    document.querySelectorAll('.limits-tab').forEach(tab => {
        tab.classList.remove('active');
        if (tab.getAttribute('data-tab') === tabName) {
            tab.classList.add('active');
        }
    });
    
    // Update tab content
    document.querySelectorAll('.limits-tab-content').forEach(content => {
        content.classList.remove('active');
    });
    
    const targetContent = document.getElementById(tabName + '-tab');
    if (targetContent) {
        targetContent.classList.add('active');
    }
    
    loadCurrentTab();
}

function loadCurrentTab() {
    switch(currentTab) {
        case 'merchant-limits':
            loadMerchantLimits();
            break;
        case 'global-limits':
            loadGlobalLimits();
            break;
        case 'risk-thresholds':
            loadRiskThresholds();
            break;
        case 'velocity-rules':
            loadVelocityRules();
            break;
        case 'country-compliance':
            loadCountryCompliance();
            break;
    }
}

function loadDashboardStats() {
    fetch('/api/v1/limits/dashboard/stats', {
        credentials: 'include'
    })
    .then(response => response.json())
    .then(data => {
        document.getElementById('limits-active-merchants').textContent = data.activeMerchants || 0;
        document.getElementById('limits-daily-usage').textContent = formatCurrency(data.totalDailyUsage || 0);
        
        // Calculate usage percentage (mock for now)
        const usagePercent = 57.0; // This would come from global limits
        document.getElementById('limits-usage-percent').textContent = usagePercent.toFixed(1) + '% of limit';
        
        document.getElementById('limits-risk-alerts').textContent = data.riskAlerts || 0;
        document.getElementById('limits-success-rate').textContent = (data.avgSuccessRate || 0).toFixed(1) + '%';
    })
    .catch(error => {
        console.error('Error loading dashboard stats:', error);
    });
}

function loadMerchantLimits() {
    fetch('/api/v1/limits/merchant', {
        credentials: 'include'
    })
    .then(response => response.json())
    .then(data => {
        const tbody = document.querySelector('#merchant-limits-table tbody');
        tbody.innerHTML = '';
        
        if (data.length === 0) {
            tbody.innerHTML = '<tr><td colspan="8" style="text-align: center; padding: 2rem;">No merchant limits configured</td></tr>';
            return;
        }
        
        data.forEach(limit => {
            const row = createMerchantLimitRow(limit);
            tbody.appendChild(row);
        });
    })
    .catch(error => {
        console.error('Error loading merchant limits:', error);
    });
}

function createMerchantLimitRow(limit) {
    const tr = document.createElement('tr');
    const merchant = limit.merchant || {};
    const riskLevel = merchant.riskLevel || 'LOW';
    
    tr.innerHTML = `
        <td>
            <div>${merchant.legalName || merchant.tradingName || 'N/A'}</div>
            <div style="font-size: 0.85rem; color: #94a3b8;">${merchant.merchantId || ''}</div>
        </td>
        <td><span class="badge badge-${getRiskLevelClass(riskLevel)}">${riskLevel}</span></td>
        <td>${formatCurrency(limit.dailyLimit)}</td>
        <td>${formatCurrency(limit.weeklyLimit) || 'Not set'}</td>
        <td>${formatCurrency(limit.monthlyLimit) || 'Not set'}</td>
        <td>${formatCurrency(limit.perTransactionLimit) || 'Not set'}</td>
        <td><span class="badge badge-${limit.status === 'ACTIVE' ? 'success' : 'secondary'}">${limit.status}</span></td>
        <td>
            <button class="btn-icon" onclick="editMerchantLimit(${merchant.merchantId})" title="Edit">
                <i class="fas fa-edit"></i>
            </button>
        </td>
    `;
    return tr;
}

function loadGlobalLimits() {
    fetch('/api/v1/limits/global', {
        credentials: 'include'
    })
    .then(response => response.json())
    .then(data => {
        const tbody = document.querySelector('#global-limits-table tbody');
        tbody.innerHTML = '';
        
        if (data.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" style="text-align: center; padding: 2rem;">No global limits configured</td></tr>';
            return;
        }
        
        data.forEach(limit => {
            const row = createGlobalLimitRow(limit);
            tbody.appendChild(row);
        });
    })
    .catch(error => {
        console.error('Error loading global limits:', error);
    });
}

function createGlobalLimitRow(limit) {
    const tr = document.createElement('tr');
    const usagePercent = limit.usagePercentage || 0;
    
    tr.innerHTML = `
        <td>
            <div><strong>${limit.name}</strong></div>
            <div style="font-size: 0.85rem; color: #94a3b8;">${limit.description || ''}</div>
        </td>
        <td><span class="badge badge-secondary">${limit.limitType}</span></td>
        <td>${formatLimitValue(limit.limitValue, limit.limitType)}</td>
        <td>
            <div style="color: #27ae60;">${formatLimitValue(limit.currentUsage, limit.limitType)}</div>
            <div style="font-size: 0.85rem; color: #94a3b8;">${usagePercent.toFixed(1)}% used</div>
        </td>
        <td><span class="badge badge-secondary">${limit.period}</span></td>
        <td><span class="badge badge-${limit.status === 'ACTIVE' ? 'success' : 'secondary'}">${limit.status}</span></td>
        <td>
            <button class="btn-icon" onclick="editGlobalLimit(${limit.id})" title="Edit">
                <i class="fas fa-edit"></i>
            </button>
            <button class="btn-icon" onclick="deleteGlobalLimit(${limit.id})" title="Delete">
                <i class="fas fa-trash"></i>
            </button>
        </td>
    `;
    return tr;
}

function loadRiskThresholds() {
    fetch('/api/v1/limits/risk-thresholds', {
        credentials: 'include'
    })
    .then(response => response.json())
    .then(data => {
        const tbody = document.querySelector('#risk-thresholds-table tbody');
        tbody.innerHTML = '';
        
        if (data.length === 0) {
            tbody.innerHTML = '<tr><td colspan="8" style="text-align: center; padding: 2rem;">No risk thresholds configured</td></tr>';
            return;
        }
        
        data.forEach(threshold => {
            const row = createRiskThresholdRow(threshold);
            tbody.appendChild(row);
        });
    })
    .catch(error => {
        console.error('Error loading risk thresholds:', error);
    });
}

function createRiskThresholdRow(threshold) {
    const tr = document.createElement('tr');
    const riskClass = getRiskLevelClass(threshold.riskLevel);
    
    tr.innerHTML = `
        <td><span class="badge badge-${riskClass}">${threshold.riskLevel}</span></td>
        <td>${threshold.description || ''}</td>
        <td>${formatCurrency(threshold.dailyLimit)}</td>
        <td>${formatCurrency(threshold.perTransactionLimit)}</td>
        <td><span class="badge badge-secondary">${threshold.velocityLimit || 0}/hr</span></td>
        <td>${threshold.merchantCount || 0} merchants</td>
        <td><span class="badge badge-${threshold.status === 'ACTIVE' ? 'success' : 'secondary'}">${threshold.status}</span></td>
        <td>
            <button class="btn-icon" onclick="editRiskThreshold('${threshold.riskLevel}')" title="Edit">
                <i class="fas fa-edit"></i>
            </button>
        </td>
    `;
    return tr;
}

function loadVelocityRules() {
    fetch('/api/v1/limits/velocity-rules', {
        credentials: 'include'
    })
    .then(response => response.json())
    .then(data => {
        const tbody = document.querySelector('#velocity-rules-table tbody');
        tbody.innerHTML = '';
        
        if (data.length === 0) {
            tbody.innerHTML = '<tr><td colspan="7" style="text-align: center; padding: 2rem;">No velocity rules configured</td></tr>';
            return;
        }
        
        data.forEach(rule => {
            const row = createVelocityRuleRow(rule);
            tbody.appendChild(row);
        });
    })
    .catch(error => {
        console.error('Error loading velocity rules:', error);
    });
}

function createVelocityRuleRow(rule) {
    const tr = document.createElement('tr');
    const riskClass = getRiskLevelClass(rule.riskLevel);
    const lastTriggered = rule.lastTriggeredAt ? new Date(rule.lastTriggeredAt).toLocaleDateString() : 'Never';
    
    tr.innerHTML = `
        <td>
            <div><strong>${rule.ruleName}</strong></div>
            <div style="font-size: 0.85rem; color: #94a3b8;">${rule.description || ''}</div>
        </td>
        <td>
            <div>Max Txns: ${rule.maxTransactions}</div>
            <div>Max Amount: ${formatCurrency(rule.maxAmount)}</div>
        </td>
        <td>${rule.timeWindowMinutes} minutes</td>
        <td><span class="badge badge-${riskClass}">${rule.riskLevel}</span></td>
        <td>
            <div>Count: ${rule.triggerCount || 0}</div>
            <div style="font-size: 0.85rem; color: #94a3b8;">Last: ${lastTriggered}</div>
        </td>
        <td>
            <label class="toggle-switch">
                <input type="checkbox" ${rule.status === 'ACTIVE' ? 'checked' : ''} 
                       onchange="toggleVelocityRuleStatus(${rule.id}, this.checked)">
                <span class="toggle-slider"></span>
            </label>
        </td>
        <td>
            <button class="btn-icon" onclick="editVelocityRule(${rule.id})" title="Edit">
                <i class="fas fa-edit"></i>
            </button>
            <button class="btn-icon" onclick="deleteVelocityRule(${rule.id})" title="Delete">
                <i class="fas fa-trash"></i>
            </button>
        </td>
    `;
    return tr;
}

function loadCountryCompliance() {
    fetch('/api/v1/limits/country-compliance', {
        credentials: 'include'
    })
    .then(response => response.json())
    .then(data => {
        const tbody = document.querySelector('#country-compliance-table tbody');
        tbody.innerHTML = '';
        
        if (data.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" style="text-align: center; padding: 2rem;">No country compliance rules configured</td></tr>';
            return;
        }
        
        data.forEach(rule => {
            const row = createCountryComplianceRow(rule);
            tbody.appendChild(row);
        });
    })
    .catch(error => {
        console.error('Error loading country compliance:', error);
    });
}

function createCountryComplianceRow(rule) {
    const tr = document.createElement('tr');
    
    tr.innerHTML = `
        <td>
            <div><strong>${rule.countryName}</strong></div>
            <div style="font-size: 0.85rem; color: #94a3b8;">${rule.countryCode}</div>
        </td>
        <td>${rule.complianceRequirements || 'N/A'}</td>
        <td>${rule.transactionRestrictions || 'N/A'}</td>
        <td>${rule.requiredDocumentation || 'N/A'}</td>
        <td><span class="badge badge-${rule.status === 'ACTIVE' ? 'success' : 'secondary'}">${rule.status}</span></td>
        <td>
            <button class="btn-icon" onclick="editCountryCompliance('${rule.countryCode}')" title="Edit">
                <i class="fas fa-edit"></i>
            </button>
        </td>
    `;
    return tr;
}

// Utility functions
function formatCurrency(amount) {
    if (!amount || amount === 0) return '$0';
    const num = typeof amount === 'string' ? parseFloat(amount) : amount;
    if (num >= 1000000) {
        return '$' + (num / 1000000).toFixed(1) + 'M';
    } else if (num >= 1000) {
        return '$' + (num / 1000).toFixed(0) + 'K';
    }
    return '$' + num.toFixed(2);
}

function formatLimitValue(value, type) {
    if (type === 'COUNT' || type === 'VELOCITY') {
        return value.toLocaleString();
    }
    return formatCurrency(value);
}

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

// Modal functions (stubs for now)
function openAddGlobalLimitModal() {
    alert('Add Global Limit modal - to be implemented');
}

function openAddRiskThresholdModal() {
    alert('Add Risk Threshold modal - to be implemented');
}

function openAddVelocityRuleModal() {
    alert('Add Velocity Rule modal - to be implemented');
}

function openAddCountryComplianceModal() {
    alert('Add Country Compliance modal - to be implemented');
}

function editMerchantLimit(merchantId) {
    alert('Edit Merchant Limit - to be implemented');
}

function editGlobalLimit(id) {
    alert('Edit Global Limit - to be implemented');
}

function deleteGlobalLimit(id) {
    if (confirm('Are you sure you want to delete this global limit?')) {
        fetch(`/api/v1/limits/global/${id}`, {
            method: 'DELETE',
            credentials: 'include'
        })
        .then(() => loadGlobalLimits())
        .catch(error => console.error('Error deleting global limit:', error));
    }
}

function editRiskThreshold(riskLevel) {
    alert('Edit Risk Threshold - to be implemented');
}

function editVelocityRule(id) {
    alert('Edit Velocity Rule - to be implemented');
}

function deleteVelocityRule(id) {
    if (confirm('Are you sure you want to delete this velocity rule?')) {
        fetch(`/api/v1/limits/velocity-rules/${id}`, {
            method: 'DELETE',
            credentials: 'include'
        })
        .then(() => loadVelocityRules())
        .catch(error => console.error('Error deleting velocity rule:', error));
    }
}

function toggleVelocityRuleStatus(id, active) {
    // Implementation would update the rule status
    console.log('Toggle velocity rule', id, active);
}

function editCountryCompliance(countryCode) {
    alert('Edit Country Compliance - to be implemented');
}

