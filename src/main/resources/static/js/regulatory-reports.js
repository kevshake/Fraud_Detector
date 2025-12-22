// Regulatory Reports Functions

window.generateCtr = async function() {
    const startDate = new Date();
    startDate.setMonth(startDate.getMonth() - 1);
    
    try {
        const response = await fetch(
            `/api/v1/reporting/regulatory/ctr?startDate=${startDate.toISOString()}&endDate=${new Date().toISOString()}`,
            { credentials: 'include' }
        );
        const report = await response.json();
        displayRegulatoryReport('CTR', report);
    } catch (error) {
        console.error('Error generating CTR:', error);
        alert('Error generating CTR report');
    }
};

window.generateLctr = async function() {
    const startDate = new Date();
    startDate.setMonth(startDate.getMonth() - 1);
    
    try {
        const response = await fetch(
            `/api/v1/reporting/regulatory/lctr?startDate=${startDate.toISOString()}&endDate=${new Date().toISOString()}`,
            { credentials: 'include' }
        );
        const report = await response.json();
        displayRegulatoryReport('LCTR', report);
    } catch (error) {
        console.error('Error generating LCTR:', error);
        alert('Error generating LCTR report');
    }
};

window.generateIftr = async function() {
    const startDate = new Date();
    startDate.setMonth(startDate.getMonth() - 1);
    
    try {
        const response = await fetch(
            `/api/v1/reporting/regulatory/iftr?startDate=${startDate.toISOString()}&endDate=${new Date().toISOString()}`,
            { credentials: 'include' }
        );
        const report = await response.json();
        displayRegulatoryReport('IFTR', report);
    } catch (error) {
        console.error('Error generating IFTR:', error);
        alert('Error generating IFTR report');
    }
};

function displayRegulatoryReport(reportType, report) {
    const container = document.getElementById('regulatory-reports-results');
    
    container.innerHTML = `
        <div class="report-result-card">
            <h3>${reportType} Report</h3>
            <div class="report-stats">
                <p><strong>Period:</strong> ${new Date(report.startDate).toLocaleDateString()} - ${new Date(report.endDate).toLocaleDateString()}</p>
                <p><strong>Transaction Count:</strong> ${report.transactionCount}</p>
                <p><strong>Total Amount:</strong> $${report.totalAmount?.toLocaleString() || '0'}</p>
                ${report.threshold ? `<p><strong>Threshold:</strong> $${report.threshold.toLocaleString()}</p>` : ''}
            </div>
            <div class="report-actions">
                <button class="btn-primary" onclick="exportReport('${reportType}', ${JSON.stringify(report).replace(/"/g, '&quot;')})">
                    <i class="fas fa-download"></i> Export
                </button>
            </div>
        </div>
    `;
}

window.exportReport = function(reportType, reportData) {
    // Simple export - can be enhanced with CSV/PDF generation
    const dataStr = JSON.stringify(reportData, null, 2);
    const dataBlob = new Blob([dataStr], { type: 'application/json' });
    const url = URL.createObjectURL(dataBlob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `${reportType}_${new Date().toISOString().split('T')[0]}.json`;
    link.click();
};

