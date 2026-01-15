// Risk Analytics Functions

window.loadRiskAnalytics = async function() {
    const period = document.getElementById('risk-analytics-period')?.value || 30;
    const heatmapType = document.getElementById('risk-analytics-heatmap-type')?.value || 'customer';
    const endDate = new Date();
    const startDate = new Date();
    startDate.setDate(startDate.getDate() - period);
    
    try {
        const heatmapTitle = document.getElementById('risk-heatmap-title');
        if (heatmapTitle) {
            heatmapTitle.textContent = heatmapType === 'merchant' ? 'Merchant Risk Heatmap' : 'Customer Risk Heatmap';
        }

        // Load risk heatmap (customer or merchant)
        const heatmapEndpoint = heatmapType === 'merchant'
            ? 'analytics/risk/heatmap/merchant'
            : 'analytics/risk/heatmap/customer';

        const heatmapResponse = await fetch(
            `${heatmapEndpoint}?startDate=${startDate.toISOString()}&endDate=${endDate.toISOString()}`,
            { credentials: 'include' }
        );
        if (!heatmapResponse.ok) throw new Error(`Heatmap HTTP ${heatmapResponse.status}`);
        const heatmap = await heatmapResponse.json();
        window._lastRiskHeatmap = { type: heatmapType, startDate: startDate.toISOString(), endDate: endDate.toISOString(), data: heatmap };
        renderRiskHeatmap(heatmap, heatmapType);
        
        // Load risk trends
        const trendsResponse = await fetch(
            `analytics/risk/trends?startDate=${startDate.toISOString()}&endDate=${endDate.toISOString()}`,
            { credentials: 'include' }
        );
        if (!trendsResponse.ok) throw new Error(`Trends HTTP ${trendsResponse.status}`);
        const trends = await trendsResponse.json();
        renderRiskTrends(trends);
        
        // Load false positive rate
        const fprResponse = await fetch(
            `analytics/risk/false-positive-rate?startDate=${startDate.toISOString()}&endDate=${endDate.toISOString()}`,
            { credentials: 'include' }
        );
        if (!fprResponse.ok) throw new Error(`FPR HTTP ${fprResponse.status}`);
        const fpr = await fprResponse.json();
        document.getElementById('false-positive-rate').textContent = 
            fpr.falsePositiveRate.toFixed(2) + '%';
    } catch (error) {
        console.error('Error loading risk analytics:', error);
    }
};

function renderRiskHeatmap(heatmap, type) {
    const container = document.getElementById('customer-risk-heatmap');
    const entries = Object.entries(heatmap);
    
    if (entries.length === 0) {
        container.innerHTML = '<p>No risk data available</p>';
        return;
    }

    const idLabel = type === 'merchant' ? 'Merchant ID' : 'Customer ID';
    
    container.innerHTML = `
        <table class="heatmap-table">
            <thead>
                <tr>
                    <th>${idLabel}</th>
                    <th>Case Count</th>
                    <th>Average Risk Score</th>
                    <th>Risk Level</th>
                </tr>
            </thead>
            <tbody>
                ${entries.map(([id, data]) => `
                    <tr>
                        <td>${id}</td>
                        <td>${data.caseCount}</td>
                        <td>${(data.averageRiskScore * 100).toFixed(1)}%</td>
                        <td>
                            <span class="risk-badge ${
                                data.averageRiskScore > 0.7 ? 'high' : 
                                data.averageRiskScore > 0.4 ? 'medium' : 'low'
                            }">
                                ${data.averageRiskScore > 0.7 ? 'HIGH' : 
                                  data.averageRiskScore > 0.4 ? 'MEDIUM' : 'LOW'}
                            </span>
                        </td>
                    </tr>
                `).join('')}
            </tbody>
        </table>
    `;
}

// Backwards compatible alias (older code/docs referenced this name)
function renderCustomerRiskHeatmap(heatmap) {
    renderRiskHeatmap(heatmap, 'customer');
}

window.exportRiskHeatmapCsv = function() {
    const payload = window._lastRiskHeatmap;
    if (!payload || !payload.data) {
        alert('No heatmap data to export yet. Open Risk Analytics first.');
        return;
    }

    const entries = Object.entries(payload.data || {});
    if (!entries.length) {
        alert('No heatmap rows to export.');
        return;
    }

    function csvEscape(v) {
        const s = String(v == null ? '' : v);
        return `"${s.replaceAll('"', '""')}"`;
    }

    const idLabel = payload.type === 'merchant' ? 'merchantId' : 'customerId';
    let csv = `${idLabel},caseCount,averageRiskScore,riskLevel\n`;

    entries.forEach(([id, data]) => {
        const avg = data && typeof data.averageRiskScore === 'number' ? data.averageRiskScore : Number(data?.averageRiskScore || 0);
        const level = avg > 0.7 ? 'HIGH' : avg > 0.4 ? 'MEDIUM' : 'LOW';
        const row = [
            id,
            data?.caseCount ?? '',
            Number.isFinite(avg) ? avg : 0,
            level
        ].map(csvEscape);
        csv += row.join(',') + '\n';
    });

    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `risk_heatmap_${payload.type}_${new Date().toISOString().split('T')[0]}.csv`;
    link.click();
    URL.revokeObjectURL(url);
};

let riskTrendsChartInstance = null;

function renderRiskTrends(trends) {
    const ctx = document.getElementById('risk-trends-chart');
    if (!ctx) return;
    
    const weeklyData = Object.entries(trends.weeklyTrends || {});
    
    if (riskTrendsChartInstance) {
        try { riskTrendsChartInstance.destroy(); } catch (e) { /* ignore */ }
        riskTrendsChartInstance = null;
    }

    riskTrendsChartInstance = new Chart(ctx, {
        type: 'line',
        data: {
            labels: weeklyData.map(([date]) => date),
            datasets: [{
                label: 'Cases',
                data: weeklyData.map(([, count]) => count),
                borderColor: '#3498db',
                backgroundColor: 'rgba(52, 152, 219, 0.1)',
                tension: 0.4
            }]
        },
        options: {
            responsive: true,
            plugins: {
                title: {
                    display: true,
                    text: `Trend: ${trends.trendDirection}`
                }
            }
        }
    });
}

// Auto-reload when filters change (when the view is already open)
document.addEventListener('DOMContentLoaded', () => {
    const periodEl = document.getElementById('risk-analytics-period');
    const typeEl = document.getElementById('risk-analytics-heatmap-type');

    if (periodEl) {
        periodEl.addEventListener('change', () => {
            // Only reload if view is visible
            const view = document.getElementById('risk-analytics-view');
            if (view && view.style.display !== 'none') {
                window.loadRiskAnalytics();
            }
        });
    }
    if (typeEl) {
        typeEl.addEventListener('change', () => {
            const view = document.getElementById('risk-analytics-view');
            if (view && view.style.display !== 'none') {
                window.loadRiskAnalytics();
            }
        });
    }
});

