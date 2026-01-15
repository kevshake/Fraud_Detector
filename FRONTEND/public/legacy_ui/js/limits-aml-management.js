/**
 * Limits & AML Management Module
 * Handles all limits management functionality
 */

let currentTab = 'merchant-limits';

let limitsRefreshInterval = null;

function initLimitsAmlManagement() {
    loadDashboardStats();
    loadTransactionLimits(); // Load the Transaction Limits table
    setupTabNavigation();
    loadCurrentTab();
    
    // Set up real-time refresh every 30 seconds
    if (limitsRefreshInterval) {
        clearInterval(limitsRefreshInterval);
    }
    limitsRefreshInterval = setInterval(() => {
        loadDashboardStats();
        loadTransactionLimits(); // Refresh transaction limits table
    }, 30000); // Refresh every 30 seconds
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
    fetch('limits/dashboard/stats', {
        credentials: 'include',
        headers: {
            'Content-Type': 'application/json',
            'X-Requested-With': 'XMLHttpRequest'
        }
    })
    .then(response => {
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        return response.json();
    })
    .then(data => {
        // Update Daily Transaction Limit card
        const dailyLimitEl = document.getElementById('dailyLimit');
        if (dailyLimitEl) {
            dailyLimitEl.textContent = formatCurrency(data.dailyTransactionLimit || 0);
        }
        
        // Update Monthly Volume Cap card
        const monthlyLimitEl = document.getElementById('monthlyLimit');
        if (monthlyLimitEl) {
            monthlyLimitEl.textContent = formatCurrency(data.monthlyVolumeCap || 0);
        }
        
        // Update High-Risk Threshold card
        const riskThresholdEl = document.getElementById('riskThreshold');
        if (riskThresholdEl) {
            riskThresholdEl.textContent = formatCurrency(data.highRiskThreshold || 0);
        }
        
        // Update Active Rules card
        const activeRulesEl = document.getElementById('activeRulesCount');
        if (activeRulesEl) {
            activeRulesEl.textContent = data.activeRulesCount || 0;
        }
        
        // Update other stats if elements exist
        const activeMerchantsEl = document.getElementById('limits-active-merchants');
        if (activeMerchantsEl) {
            activeMerchantsEl.textContent = data.activeMerchants || 0;
        }
        
        const dailyUsageEl = document.getElementById('limits-daily-usage');
        if (dailyUsageEl) {
            dailyUsageEl.textContent = formatCurrency(data.totalDailyUsage || 0);
        }
        
        // Calculate usage percentage
        const usagePercentEl = document.getElementById('limits-usage-percent');
        if (usagePercentEl && data.dailyTransactionLimit) {
            const dailyLimit = typeof data.dailyTransactionLimit === 'string' ? 
                parseFloat(data.dailyTransactionLimit) : data.dailyTransactionLimit;
            const dailyUsage = typeof data.totalDailyUsage === 'string' ? 
                parseFloat(data.totalDailyUsage) : data.totalDailyUsage;
            
            if (dailyLimit && dailyLimit > 0) {
                const usagePercent = (dailyUsage / dailyLimit * 100).toFixed(1);
                usagePercentEl.textContent = usagePercent + '% of limit';
            } else {
                usagePercentEl.textContent = 'N/A';
            }
        }
        
        const riskAlertsEl = document.getElementById('limits-risk-alerts');
        if (riskAlertsEl) {
            riskAlertsEl.textContent = data.riskAlerts || 0;
        }
        
        const successRateEl = document.getElementById('limits-success-rate');
        if (successRateEl) {
            successRateEl.textContent = (data.avgSuccessRate || 0).toFixed(1) + '%';
        }
    })
    .catch(error => {
        console.error('Error loading dashboard stats:', error);
    });
}

function loadMerchantLimits() {
    fetch('limits/merchant', {
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
    fetch('limits/global', {
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
    fetch('limits/risk-thresholds', {
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
    fetch('limits/velocity-rules', {
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
    fetch('limits/country-compliance', {
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
function formatCurrency(amount, currencyCode) {
    const currency =
        currencyCode ||
        (window.currencyFormatter && typeof window.currencyFormatter.getDefaultCurrency === 'function'
            ? window.currencyFormatter.getDefaultCurrency()
            : (window.appDefaultCurrency || 'USD'));

    if (window.currencyFormatter && typeof window.currencyFormatter.formatCompact === 'function') {
        return window.currencyFormatter.formatCompact(amount, currency);
    }

    // Fallback (should rarely happen)
    const num = amount == null ? 0 : (typeof amount === 'string' ? parseFloat(amount) : Number(amount));
    const safeNum = Number.isFinite(num) ? num : 0;
    try {
        return new Intl.NumberFormat('en-US', { style: 'currency', currency }).format(safeNum);
    } catch (e) {
        return `${currency} ${safeNum.toFixed(2)}`;
    }
}

function formatLimitValue(value, type) {
    if (!value && value !== 0) {
        if (type === 'COUNT' || type === 'VELOCITY') return '0';
        return formatCurrency(0);
    }
    
    // Handle BigDecimal from backend (may come as number or string)
    const num = typeof value === 'string' ? parseFloat(value) : (typeof value === 'number' ? value : parseFloat(value));
    if (isNaN(num)) {
        if (type === 'COUNT' || type === 'VELOCITY') return '0';
        return formatCurrency(0);
    }
    
    if (type === 'COUNT' || type === 'VELOCITY') {
        return num.toLocaleString();
    }
    // For VOLUME type, values are already in dollars (not cents)
    return formatCurrency(num);
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
function openAddLimitModal() {
    openEditTransactionLimitModal(null);
}

function openEditTransactionLimitModal(limit) {
    const modal = document.getElementById('editTransactionLimitModal');
    if (!modal) {
        alert('Edit modal not found. Please refresh the page.');
        return;
    }
    
    if (limit) {
        // Edit mode
        document.getElementById('editLimitId').value = limit.id || '';
        document.getElementById('editLimitName').value = limit.name || '';
        document.getElementById('editLimitDescription').value = limit.description || '';
        document.getElementById('editLimitType').value = limit.limitType || 'VOLUME';
        document.getElementById('editLimitValue').value = limit.limitValue || '';
        document.getElementById('editLimitPeriod').value = limit.period || 'DAY';
        document.getElementById('editLimitStatus').value = limit.status || 'ACTIVE';
        document.querySelector('#editTransactionLimitModal h2').textContent = 'Edit Transaction Limit';
    } else {
        // Add mode
        document.getElementById('editLimitId').value = '';
        document.getElementById('editLimitName').value = '';
        document.getElementById('editLimitDescription').value = '';
        document.getElementById('editLimitType').value = 'VOLUME';
        document.getElementById('editLimitValue').value = '';
        document.getElementById('editLimitPeriod').value = 'DAY';
        document.getElementById('editLimitStatus').value = 'ACTIVE';
        document.querySelector('#editTransactionLimitModal h2').textContent = 'Add Transaction Limit';
    }
    
    modal.style.display = 'block';
}

function closeEditTransactionLimitModal() {
    const modal = document.getElementById('editTransactionLimitModal');
    if (modal) {
        modal.style.display = 'none';
    }
}

function openAddGlobalLimitModal() {
    openAddLimitModal();
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

// Load Transaction Limits table (uses Global Limits)
function loadTransactionLimits() {
    fetch('limits/global', {
        credentials: 'include',
        headers: {
            'Content-Type': 'application/json',
            'X-Requested-With': 'XMLHttpRequest'
        }
    })
    .then(response => {
        if (!response.ok) {
            return response.json().then(errorData => {
                throw new Error(errorData.message || errorData.error || `HTTP ${response.status}`);
            }).catch(() => {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            });
        }
        return response.json();
    })
    .then(data => {
        const tbody = document.querySelector('#limits-table tbody');
        if (!tbody) {
            console.warn('Limits table tbody not found');
            return;
        }
        
        tbody.innerHTML = '';
        
        // Handle error response
        if (data.error) {
            tbody.innerHTML = `<tr><td colspan="6" style="text-align: center; padding: 2rem; color: #e74c3c;">${data.message || data.error || 'Error loading transaction limits'}</td></tr>`;
            return;
        }
        
        // Ensure data is an array
        const limits = Array.isArray(data) ? data : [];
        
        if (limits.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" style="text-align: center; padding: 2rem;">No transaction limits configured</td></tr>';
            return;
        }
        
        limits.forEach(limit => {
            try {
                const row = createTransactionLimitRow(limit);
                tbody.appendChild(row);
            } catch (e) {
                console.error('Error creating limit row:', e, limit);
            }
        });
    })
    .catch(error => {
        console.error('Error loading transaction limits:', error);
        const tbody = document.querySelector('#limits-table tbody');
        if (tbody) {
            tbody.innerHTML = `<tr><td colspan="6" style="text-align: center; padding: 2rem; color: #e74c3c;">Error loading transaction limits: ${error.message || 'Please try again'}</td></tr>`;
        }
    });
}

function createTransactionLimitRow(limit) {
    if (!limit) {
        console.error('Invalid limit object:', limit);
        return null;
    }
    
    const tr = document.createElement('tr');
    
    // Format limit type for display
    const limitTypeDisplay = formatLimitTypeName(limit.name || limit.limitType || 'N/A');
    
    // Format period for display
    const periodDisplay = formatPeriodDisplay(limit.period || 'N/A');
    
    // Format limit value
    const limitValueDisplay = formatLimitValue(limit.limitValue, limit.limitType);
    
    // Determine applies to text
    const appliesTo = limit.description ? limit.description : 'All Merchants';
    
    // Status badge
    const statusClass = limit.status === 'ACTIVE' ? 'resolved' : 'escalated';
    const statusText = limit.status === 'ACTIVE' ? 'Active' : (limit.status || 'Inactive');
    
    // Ensure id exists for edit button
    const limitId = limit.id || limit.limitId || 0;
    
    tr.innerHTML = `
        <td>${limitTypeDisplay}</td>
        <td>${limitValueDisplay}</td>
        <td>${periodDisplay}</td>
        <td>${appliesTo}</td>
        <td><span class="status-badge ${statusClass}">${statusText}</span></td>
        <td>
            <button class="action-btn" onclick="editTransactionLimit(${limitId})" title="Edit Transaction Limit">
                <i class="fas fa-edit"></i>
            </button>
        </td>
    `;
    return tr;
}

function formatLimitTypeName(name) {
    if (!name) return 'N/A';
    // Convert names like "Daily Transaction Limit" to "Daily Transaction"
    return name.replace(/Limit$/i, '').trim();
}

function formatPeriodDisplay(period) {
    if (!period) return 'N/A';
    const periodMap = {
        'DAY': '24 Hours',
        'HOUR': '1 Hour',
        'MINUTE': 'Per Minute',
        'WEEK': '7 Days',
        'MONTH': '30 Days',
        'TRANSACTION': 'Per Transaction'
    };
    return periodMap[period.toUpperCase()] || period;
}

function editGlobalLimit(id) {
    if (!id) {
        alert('Error: Limit ID is missing.');
        return;
    }
    
    // Fetch the limit details
    fetch(`limits/global`, {
        credentials: 'include',
        headers: {
            'Content-Type': 'application/json',
            'X-Requested-With': 'XMLHttpRequest'
        }
    })
    .then(response => {
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        return response.json();
    })
    .then(limits => {
        const limit = limits.find(l => l.id === id);
        if (!limit) {
            alert('Limit not found');
            return;
        }
        
        // Populate edit modal
        openEditTransactionLimitModal(limit);
    })
    .catch(error => {
        console.error('Error loading limit details:', error);
        alert('Failed to load limit details: ' + error.message);
    });
}

// Alias for editTransactionLimit
window.editTransactionLimit = function(id) {
    editGlobalLimit(id);
};

function deleteGlobalLimit(id) {
    if (confirm('Are you sure you want to delete this global limit?')) {
        fetch(`limits/global/${id}`, {
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
        fetch(`limits/velocity-rules/${id}`, {
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

// Handle Edit Transaction Limit form submission
document.addEventListener('DOMContentLoaded', function() {
    const editLimitForm = document.getElementById('editTransactionLimitForm');
    if (editLimitForm) {
        editLimitForm.addEventListener('submit', function(e) {
            e.preventDefault();
            
            const limitId = document.getElementById('editLimitId').value;
            const isEdit = limitId && limitId !== '';
            
            const data = {
                name: document.getElementById('editLimitName').value,
                description: document.getElementById('editLimitDescription').value,
                limitType: document.getElementById('editLimitType').value,
                limitValue: parseFloat(document.getElementById('editLimitValue').value),
                period: document.getElementById('editLimitPeriod').value,
                status: document.getElementById('editLimitStatus').value
            };
            
            // Validate required fields
            if (!data.name || !data.limitType || data.limitValue === null || !data.period || !data.status) {
                alert('Please fill in all required fields.');
                return;
            }
            
            const url = isEdit ? `limits/global/${limitId}` : 'limits/global';
            const method = isEdit ? 'PUT' : 'POST';
            
            fetch(url, {
                method: method,
                credentials: 'include',
                headers: {
                    'Content-Type': 'application/json',
                    'X-Requested-With': 'XMLHttpRequest'
                },
                body: JSON.stringify(data)
            })
            .then(response => {
                if (!response.ok) {
                    return response.text().then(text => {
                        throw new Error(text || `HTTP ${response.status}`);
                    });
                }
                return response.json();
            })
            .then(result => {
                alert(isEdit ? 'Transaction limit updated successfully!' : 'Transaction limit created successfully!');
                closeEditTransactionLimitModal();
                loadTransactionLimits(); // Refresh the table
                loadDashboardStats(); // Refresh dashboard stats
            })
            .catch(error => {
                console.error('Error saving transaction limit:', error);
                alert('Failed to save transaction limit: ' + error.message);
            });
        });
    }
    
    // Close modal when clicking outside
    const editLimitModal = document.getElementById('editTransactionLimitModal');
    if (editLimitModal) {
        editLimitModal.addEventListener('click', function(e) {
            if (e.target === editLimitModal) {
                closeEditTransactionLimitModal();
            }
        });
    }
});

