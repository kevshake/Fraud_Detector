// Risk Analytics Functions

window.loadRiskAnalytics = async function() {
    const period = document.getElementById('risk-analytics-period')?.value || 30;
    const endDate = new Date();
    const startDate = new Date();
    startDate.setDate(startDate.getDate() - period);
    
    try {
        // Load customer risk heatmap
        const heatmapResponse = await fetch(
            `/api/v1/analytics/risk/heatmap/customer?startDate=${startDate.toISOString()}&endDate=${endDate.toISOString()}`,
            { credentials: 'include' }
        );
        const heatmap = await heatmapResponse.json();
        renderCustomerRiskHeatmap(heatmap);
        
        // Load risk trends
        const trendsResponse = await fetch(
            `/api/v1/analytics/risk/trends?startDate=${startDate.toISOString()}&endDate=${endDate.toISOString()}`,
            { credentials: 'include' }
        );
        const trends = await trendsResponse.json();
        renderRiskTrends(trends);
        
        // Load false positive rate
        const fprResponse = await fetch(
            `/api/v1/analytics/risk/false-positive-rate?startDate=${startDate.toISOString()}&endDate=${endDate.toISOString()}`,
            { credentials: 'include' }
        );
        const fpr = await fprResponse.json();
        document.getElementById('false-positive-rate').textContent = 
            fpr.falsePositiveRate.toFixed(2) + '%';
    } catch (error) {
        console.error('Error loading risk analytics:', error);
    }
};

function renderCustomerRiskHeatmap(heatmap) {
    const container = document.getElementById('customer-risk-heatmap');
    const entries = Object.entries(heatmap);
    
    if (entries.length === 0) {
        container.innerHTML = '<p>No risk data available</p>';
        return;
    }
    
    container.innerHTML = `
        <table class="heatmap-table">
            <thead>
                <tr>
                    <th>Customer ID</th>
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

function renderRiskTrends(trends) {
    const ctx = document.getElementById('risk-trends-chart');
    if (!ctx) return;
    
    const weeklyData = Object.entries(trends.weeklyTrends || {});
    
    new Chart(ctx, {
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

