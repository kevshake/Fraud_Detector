// Regulatory Reports Functions

// Helper to get CSRF token
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

function getHeaders() {
    const headers = {
        'X-Requested-With': 'XMLHttpRequest',
        'Content-Type': 'application/json'
    };
    const csrfToken = getCsrfToken();
    if (csrfToken) {
        headers['X-XSRF-TOKEN'] = csrfToken;
    }
    return headers;
}

window.generateCtr = async function () {
    const startDate = new Date();
    startDate.setMonth(startDate.getMonth() - 1);

    try {
        const response = await fetch(
            `reporting/regulatory/ctr?startDate=${startDate.toISOString()}&endDate=${new Date().toISOString()}`,
            { credentials: 'include', headers: getHeaders() }
        );
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        const report = await response.json();
        displayRegulatoryReport('CTR', report);
    } catch (error) {
        console.error('Error generating CTR:', error);
        alert('Error generating CTR report: ' + error.message);
    }
};

window.generateLctr = async function () {
    const startDate = new Date();
    startDate.setMonth(startDate.getMonth() - 1);

    try {
        const response = await fetch(
            `reporting/regulatory/lctr?startDate=${startDate.toISOString()}&endDate=${new Date().toISOString()}`,
            { credentials: 'include', headers: getHeaders() }
        );
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        const report = await response.json();
        displayRegulatoryReport('LCTR', report);
    } catch (error) {
        console.error('Error generating LCTR:', error);
        alert('Error generating LCTR report: ' + error.message);
    }
};

window.generateIftr = async function () {
    const startDate = new Date();
    startDate.setMonth(startDate.getMonth() - 1);

    try {
        const response = await fetch(
            `reporting/regulatory/iftr?startDate=${startDate.toISOString()}&endDate=${new Date().toISOString()}`,
            { credentials: 'include', headers: getHeaders() }
        );
        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        const report = await response.json();
        displayRegulatoryReport('IFTR', report);
    } catch (error) {
        console.error('Error generating IFTR:', error);
        alert('Error generating IFTR report: ' + error.message);
    }
};

function displayRegulatoryReport(reportType, report) {
    const container = document.getElementById('regulatory-reports-results');

    // Cache last generated report for safe export buttons (avoid embedding large JSON in HTML)
    if (!window._regulatoryReportsCache) {
        window._regulatoryReportsCache = {};
    }
    window._regulatoryReportsCache[reportType] = report;

    const hasDetails = report.transactionDetails && report.transactionDetails.length > 0;
    const transactions = hasDetails ? report.transactionDetails : (report.transactions || []);
    const reportCurrency = getReportCurrency(report, transactions);
    const totalsByCurrency = getTotalsByCurrency(report, transactions);
    const totalAmountLines = buildTotalsByCurrencyLines(totalsByCurrency);
    const thresholdLines = report.threshold
        ? buildThresholdByCurrencyLines(report.threshold, totalsByCurrency, reportCurrency)
        : [];

    if (!container) {
        const totalAmountText = totalAmountLines.length ? totalAmountLines.join('\n') : formatMoney(report.totalAmount || 0, reportCurrency);
        const thresholdText = thresholdLines.length ? `\nThreshold: ${thresholdLines.join('\n')}` : '';
        alert(`${reportType} Report Generated!\nTransaction Count: ${report.transactionCount || 0}\nTotal Amount:\n${totalAmountText}${thresholdText}`);
        return;
    }

    let tableHtml = '';
    if (transactions.length > 0) {
        if (hasDetails) {
            // Detailed table with enriched data
            tableHtml = buildDetailedTable(reportType, transactions, reportCurrency);
        } else {
            // Basic table with transaction entities
            tableHtml = buildBasicTable(reportType, transactions, reportCurrency);
        }
    } else {
        tableHtml = '<div class="report-empty-state"><p>No transactions found for this report period.</p></div>';
    }

    container.innerHTML = `
        <div class="report-result-card">
            <div class="report-header">
                <h3><i class="fas fa-file-invoice-dollar"></i> ${reportType} Report</h3>
                <div class="report-actions">
                    <button class="btn-primary" onclick="exportReport('${reportType}')">
                        <i class="fas fa-download"></i> Export JSON
                    </button>
                    <button class="btn-secondary" onclick="exportCsv('${reportType}')">
                        <i class="fas fa-file-csv"></i> Export CSV
                    </button>
                </div>
            </div>
            <div class="report-stats">
                <div class="stat-item">
                    <span class="stat-label">Report Period</span>
                    <span class="stat-value">${formatDate(report.startDate)} - ${formatDate(report.endDate)}</span>
                </div>
                <div class="stat-item">
                    <span class="stat-label">Transaction Count</span>
                    <span class="stat-value">${report.transactionCount || 0}</span>
                </div>
                <div class="stat-item">
                    <span class="stat-label">Total Amount</span>
                    <span class="stat-value">${totalAmountLines.length ? totalAmountLines.join('<br>') : formatMoney(report.totalAmount || 0, reportCurrency)}</span>
                </div>
                ${report.threshold ? `<div class="stat-item">
                    <span class="stat-label">Threshold</span>
                    <span class="stat-value">${thresholdLines.length ? thresholdLines.join('<br>') : formatMoney(report.threshold, reportCurrency)}</span>
                </div>` : ''}
            </div>
            ${tableHtml}
        </div>
    `;
}

function buildDetailedTable(reportType, transactions, reportCurrency) {
    if (reportType === 'CTR') {
        return `
            <div class="report-table-container">
                <table class="regulatory-report-table">
                    <thead>
                        <tr>
                            <th>Transaction ID</th>
                            <th>Date & Time</th>
                            <th>Amount</th>
                            <th>Currency</th>
                            <th>Transaction Type</th>
                            <th>Merchant Name</th>
                            <th>Merchant Country</th>
                            <th>Merchant Registration</th>
                            <th>Financial Institution</th>
                            <th>Terminal ID</th>
                            <th>IP Address</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${transactions.map(tx => `
                            <tr>
                                <td><span class="txn-id">${tx.transactionId || 'N/A'}</span></td>
                                <td>${formatDateTime(tx.transactionDate)}</td>
                                <td><strong>${formatMoney(tx.amount, tx.currency || reportCurrency)}</strong></td>
                                <td>${tx.currency || reportCurrency}</td>
                                <td><span class="badge badge-info">${tx.transactionType || 'UNKNOWN'}</span></td>
                                <td>${tx.merchantName || 'N/A'}<br><small class="text-muted">${tx.merchantTradingName || ''}</small></td>
                                <td>${tx.merchantCountry || 'N/A'}</td>
                                <td><small>${tx.merchantRegistrationNumber || 'N/A'}</small></td>
                                <td>${tx.financialInstitutionName || 'N/A'}<br><small class="text-muted">${tx.financialInstitutionCode || ''}</small></td>
                                <td><small>${tx.terminalId || 'N/A'}</small></td>
                                <td><small>${tx.ipAddress || 'N/A'}</small></td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        `;
    } else if (reportType === 'LCTR') {
        return `
            <div class="report-table-container">
                <table class="regulatory-report-table">
                    <thead>
                        <tr>
                            <th>Transaction ID</th>
                            <th>Date & Time</th>
                            <th>Amount</th>
                            <th>Currency</th>
                            <th>Transaction Type</th>
                            <th>Merchant Name</th>
                            <th>Merchant Address</th>
                            <th>Merchant Country</th>
                            <th>Financial Institution</th>
                            <th>Terminal ID</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${transactions.map(tx => `
                            <tr>
                                <td><span class="txn-id">${tx.transactionId || 'N/A'}</span></td>
                                <td>${formatDateTime(tx.transactionDate)}</td>
                                <td><strong>${formatMoney(tx.amount, tx.currency || reportCurrency)}</strong></td>
                                <td>${tx.currency || reportCurrency}</td>
                                <td><span class="badge badge-warning">${tx.transactionType || 'UNKNOWN'}</span></td>
                                <td>${tx.merchantName || 'N/A'}<br><small class="text-muted">${tx.merchantTradingName || ''}</small></td>
                                <td><small>${tx.merchantAddress || 'N/A'}</small></td>
                                <td>${tx.merchantCountry || 'N/A'}</td>
                                <td>${tx.financialInstitutionName || 'N/A'}<br><small class="text-muted">${tx.financialInstitutionCode || ''}</small></td>
                                <td><small>${tx.terminalId || 'N/A'}</small></td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        `;
    } else if (reportType === 'IFTR') {
        return `
            <div class="report-table-container">
                <table class="regulatory-report-table">
                    <thead>
                        <tr>
                            <th>Transaction ID</th>
                            <th>Date & Time</th>
                            <th>Amount</th>
                            <th>Currency</th>
                            <th>Transfer Type</th>
                            <th>Origin Country</th>
                            <th>Destination Country</th>
                            <th>Financial Institution</th>
                            <th>Merchant Name</th>
                            <th>Merchant Address</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${transactions.map(tx => `
                            <tr>
                                <td><span class="txn-id">${tx.transactionId || 'N/A'}</span></td>
                                <td>${formatDateTime(tx.transactionDate)}</td>
                                <td><strong>${formatMoney(tx.amount, tx.currency || reportCurrency)}</strong></td>
                                <td>${tx.currency || reportCurrency}</td>
                                <td><span class="badge badge-success">${tx.transferType || 'UNKNOWN'}</span></td>
                                <td>${tx.originCountry || 'N/A'}</td>
                                <td>${tx.destinationCountry || 'N/A'}</td>
                                <td>${tx.financialInstitutionName || 'N/A'}<br><small class="text-muted">${tx.financialInstitutionCode || ''}</small></td>
                                <td>${tx.merchantName || 'N/A'}<br><small class="text-muted">${tx.merchantTradingName || ''}</small></td>
                                <td><small>${tx.merchantAddress || 'N/A'}</small></td>
                            </tr>
                        `).join('')}
                    </tbody>
                </table>
            </div>
        `;
    }
    return '';
}

function buildBasicTable(reportType, transactions, reportCurrency) {
    return `
        <div class="report-table-container">
            <table class="regulatory-report-table">
                <thead>
                    <tr>
                        <th>Transaction ID</th>
                        <th>Date & Time</th>
                        <th>Amount</th>
                        <th>Currency</th>
                        <th>Merchant ID</th>
                        <th>Terminal ID</th>
                    </tr>
                </thead>
                <tbody>
                    ${transactions.map(tx => `
                        <tr>
                            <td><span class="txn-id">${tx.txnId || tx.transactionId || 'N/A'}</span></td>
                            <td>${formatDateTime(tx.txnTs || tx.transactionDate)}</td>
                            <td><strong>${formatMoney(tx.amountCents ? tx.amountCents / 100 : (tx.amount || 0), tx.currency || reportCurrency)}</strong></td>
                            <td>${tx.currency || reportCurrency}</td>
                            <td>${tx.merchantId || 'N/A'}</td>
                            <td><small>${tx.terminalId || 'N/A'}</small></td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        </div>
    `;
}

function formatDate(dateStr) {
    if (!dateStr) return 'N/A';
    const date = new Date(dateStr);
    return date.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
}

function formatDateTime(dateStr) {
    if (!dateStr) return 'N/A';
    const date = new Date(dateStr);
    return date.toLocaleString('en-US', { 
        year: 'numeric', 
        month: 'short', 
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
    });
}

function getReportCurrency(report, transactions) {
    // If the backend ever provides a report-level currency, prefer it
    if (report && report.currency) return String(report.currency).toUpperCase();

    const set = new Set();
    (transactions || []).forEach(tx => {
        const c = tx && tx.currency ? String(tx.currency).trim().toUpperCase() : null;
        if (c) set.add(c);
    });

    if (set.size === 1) return Array.from(set)[0];
    if (window.currencyFormatter && typeof window.currencyFormatter.getDefaultCurrency === 'function') {
        return window.currencyFormatter.getDefaultCurrency();
    }
    return 'USD';
}

function getTotalsByCurrency(report, transactions) {
    // Prefer backend-computed totals (Map<String, BigDecimal> -> JSON object)
    const backendTotals = report && (report.totalAmountByCurrency || report.totalsByCurrency);
    if (backendTotals && typeof backendTotals === 'object') {
        const totals = {};
        Object.entries(backendTotals).forEach(([cur, val]) => {
            const currency = (cur || 'USD').toString().trim().toUpperCase() || 'USD';
            const num = typeof val === 'string' ? parseFloat(val) : Number(val);
            totals[currency] = (totals[currency] || 0) + (Number.isFinite(num) ? num : 0);
        });
        return totals;
    }

    // Fallback: compute totals from transaction rows
    const totals = {};
    (transactions || []).forEach(tx => {
        const currency = (tx && tx.currency ? String(tx.currency).trim().toUpperCase() : '') || 'USD';
        let amount = 0;
        if (tx && tx.amountCents != null) {
            amount = Number(tx.amountCents) / 100;
        } else if (tx && tx.amount != null) {
            amount = typeof tx.amount === 'string' ? parseFloat(tx.amount) : Number(tx.amount);
        }
        if (!Number.isFinite(amount)) amount = 0;
        totals[currency] = (totals[currency] || 0) + amount;
    });
    return totals;
}

function buildTotalsByCurrencyLines(totalsByCurrency) {
    const currencies = Object.keys(totalsByCurrency || {}).filter(Boolean);
    if (currencies.length === 0) return [];

    // Single currency -> show normally (symbol)
    if (currencies.length === 1) {
        const c = currencies[0];
        return [formatMoney(totalsByCurrency[c], c)];
    }

    // Multi-currency -> always show currency code for clarity
    return currencies
        .sort()
        .map(c => formatMoney(totalsByCurrency[c], c, { currencyDisplay: 'code' }));
}

function buildThresholdByCurrencyLines(threshold, totalsByCurrency, fallbackCurrency) {
    const currencies = Object.keys(totalsByCurrency || {}).filter(Boolean);
    if (currencies.length === 0) {
        return [formatMoney(threshold, fallbackCurrency)];
    }
    if (currencies.length === 1) {
        return [formatMoney(threshold, currencies[0])];
    }
    return currencies
        .sort()
        .map(c => formatMoney(threshold, c, { currencyDisplay: 'code' }));
}

function formatMoney(amount, currencyCode, options) {
    if (window.currencyFormatter && typeof window.currencyFormatter.format === 'function') {
        return window.currencyFormatter.format(amount, currencyCode, options);
    }
    // Fallback (should rarely happen)
    const num = amount == null ? 0 : (typeof amount === 'string' ? parseFloat(amount) : Number(amount));
    const safeNum = Number.isFinite(num) ? num : 0;
    const currency = currencyCode || 'USD';
    try {
        return new Intl.NumberFormat('en-US', { style: 'currency', currency }).format(safeNum);
    } catch (e) {
        return `${currency} ${safeNum.toFixed(2)}`;
    }
}

window.exportReport = function (reportType, reportData) {
    const data = reportData || (window._regulatoryReportsCache ? window._regulatoryReportsCache[reportType] : null);
    if (!data) {
        alert('No report data available to export.');
        return;
    }

    // JSON export
    const dataStr = JSON.stringify(data, null, 2);
    const dataBlob = new Blob([dataStr], { type: 'application/json' });
    const url = URL.createObjectURL(dataBlob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `${reportType}_${new Date().toISOString().split('T')[0]}.json`;
    link.click();
    URL.revokeObjectURL(url);
};

window.exportCsv = function (reportType, reportData) {
    const data = reportData || (window._regulatoryReportsCache ? window._regulatoryReportsCache[reportType] : null);
    if (!data) {
        alert('No report data available to export.');
        return;
    }

    const transactions = data.transactionDetails || data.transactions || [];
    if (transactions.length === 0) {
        alert('No transactions to export');
        return;
    }

    let csv = '';
    if (reportType === 'CTR') {
        csv = 'Transaction ID,Date,Amount,Currency,Transaction Type,Merchant Name,Merchant Trading Name,Merchant Country,Merchant Registration,Financial Institution,Financial Institution Code,Terminal ID,IP Address\n';
        transactions.forEach(tx => {
            const txnId = tx.transactionId || tx.txnId || '';
            const date = formatDateTime(tx.transactionDate || tx.txnTs);
            const currency = tx.currency || '';
            const amount = tx.amount != null ? tx.amount : (tx.amountCents != null ? (Number(tx.amountCents) / 100) : '');
            csv += `${txnId},${date},${amount},${currency},${tx.transactionType || ''},"${(tx.merchantName || '').replace(/"/g, '""')}","${(tx.merchantTradingName || '').replace(/"/g, '""')}",${tx.merchantCountry || ''},"${(tx.merchantRegistrationNumber || '').replace(/"/g, '""')}","${(tx.financialInstitutionName || '').replace(/"/g, '""')}",${tx.financialInstitutionCode || ''},${tx.terminalId || ''},${tx.ipAddress || ''}\n`;
        });
    } else if (reportType === 'LCTR') {
        csv = 'Transaction ID,Date,Amount,Currency,Transaction Type,Merchant Name,Merchant Trading Name,Merchant Address,Merchant Country,Financial Institution,Terminal ID\n';
        transactions.forEach(tx => {
            const txnId = tx.transactionId || tx.txnId || '';
            const date = formatDateTime(tx.transactionDate || tx.txnTs);
            const currency = tx.currency || '';
            const amount = tx.amount != null ? tx.amount : (tx.amountCents != null ? (Number(tx.amountCents) / 100) : '');
            csv += `${txnId},${date},${amount},${currency},${tx.transactionType || ''},"${(tx.merchantName || '').replace(/"/g, '""')}","${(tx.merchantTradingName || '').replace(/"/g, '""')}","${(tx.merchantAddress || '').replace(/"/g, '""')}",${tx.merchantCountry || ''},"${(tx.financialInstitutionName || '').replace(/"/g, '""')}",${tx.terminalId || ''}\n`;
        });
    } else if (reportType === 'IFTR') {
        csv = 'Transaction ID,Date,Amount,Currency,Transfer Type,Origin Country,Destination Country,Financial Institution,Financial Institution Code,Merchant Name,Merchant Trading Name,Merchant Address\n';
        transactions.forEach(tx => {
            const txnId = tx.transactionId || tx.txnId || '';
            const date = formatDateTime(tx.transactionDate || tx.txnTs);
            const currency = tx.currency || '';
            const amount = tx.amount != null ? tx.amount : (tx.amountCents != null ? (Number(tx.amountCents) / 100) : '');
            csv += `${txnId},${date},${amount},${currency},${tx.transferType || ''},${tx.originCountry || ''},${tx.destinationCountry || ''},"${(tx.financialInstitutionName || '').replace(/"/g, '""')}",${tx.financialInstitutionCode || ''},"${(tx.merchantName || '').replace(/"/g, '""')}","${(tx.merchantTradingName || '').replace(/"/g, '""')}","${(tx.merchantAddress || '').replace(/"/g, '""')}"\n`;
        });
    }

    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `${reportType}_${new Date().toISOString().split('T')[0]}.csv`;
    link.click();
    URL.revokeObjectURL(url);
};
