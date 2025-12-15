/**
 * AML Fraud Detector - Dashboard JavaScript
 * Handles charts, interactions, and real-time updates
 */

document.addEventListener('DOMContentLoaded', function () {
    initializeCharts();
    initializeNavigation();
    initializeRealTimeUpdates();

    // Default view
    ViewRenderer.render('dashboard');
});

// ==========================================================================
// VIEW RENDERING (SPA Architecture)
// ==========================================================================

const ViewRenderer = {
    render(viewName) {
        // Prevent re-rendering if already active (optional optimization)

        const mainContent = document.querySelector('.main-content');
        if (!mainContent) return;

        // Find or create a content container for dynamic loading
        let contentContainer = document.getElementById('dynamic-content');
        if (!contentContainer) {
            // Structure checks
            const header = document.querySelector('.top-header'); // Keep top header

            // Allow existing static content to be cleared or moved
            // Strategy: Clear everything after top-header
            const newContainer = document.createElement('div');
            newContainer.id = 'dynamic-content';

            // Move helpful elements or just clear. 
            // For a clean slate, we remove siblings of header
            while (header.nextSibling) {
                header.parentNode.removeChild(header.nextSibling);
            }

            mainContent.appendChild(newContainer);
            contentContainer = newContainer;
        }

        showToast(`Loading ${viewName}...`);
        contentContainer.style.opacity = '0.5';

        setTimeout(() => {
            contentContainer.innerHTML = this.getTemplate(viewName);
            contentContainer.style.opacity = '1';
            this.initializeViewScripts(viewName);
        }, 200);
    },

    getTemplate(viewName) {
        const v = viewName.toLowerCase().trim();
        const templates = {
            'dashboard': this.getDashboardTemplate(),
            'overview': this.getDashboardTemplate(),
            'analytics': this.getAnalyticsTemplate(),
            'widgets': this.getWidgetsTemplate(),
            'cases': this.getCasesTemplate(),
            'sar reports': this.getSarTemplate(),
            'alerts': this.getAlertsTemplate(),
            'merchants': this.getMerchantsTemplate(),
            'transactions': this.getTransactionsTemplate(),
            'screening': this.getScreeningTemplate(),
            'profile': this.getProfileTemplate(),
            'messages': this.getMessagesTemplate(),
            'settings': this.getSettingsTemplate(),
            'user management': this.getUserManagementTemplate(),
            'reports': this.getReportsTemplate(),
            'audit logs': this.getAuditLogsTemplate()
        };
        return templates[v] || this.getGenericTemplate(viewName);
    },

    initializeViewScripts(viewName) {
        const v = viewName.toLowerCase();
        if (['dashboard', 'overview', 'analytics'].includes(v)) {
            if (typeof initializeCharts === 'function') initializeCharts();
        }
        // Add specific inits for other pages if needed
    },

    // --- TEMPLATES ---

    getDashboardTemplate() {
        return `
            <div class="page-header">
                <h1>Analytics <span class="subtitle">Fraud Detection Performance</span></h1>
            </div>
            
            <div class="stats-grid">
                <div class="stat-card">
                    <div class="stat-header"><span>Cases Today</span><button class="close-btn">&times;</button></div>
                    <div class="stat-value">247</div>
                    <div class="stat-details">
                        <div class="stat-detail"><span class="value">+32</span><span class="label">New</span></div>
                        <div class="stat-detail"><span class="value">89%</span><span class="label">Resolved</span></div>
                        <div class="stat-detail"><span class="value">2.3%</span><span class="label">Escalated</span></div>
                    </div>
                </div>
                <div class="stat-card">
                    <div class="stat-header"><span>Risk Breakdown</span><button class="close-btn">&times;</button></div>
                    <div class="chart-container"><canvas id="riskPieChart"></canvas></div>
                </div>
                <div class="stat-card">
                    <div class="stat-header"><span>SAR Performance</span><button class="close-btn">&times;</button></div>
                    <div class="progress-stats">
                        <div class="progress-item"><span class="label"><i class="fas fa-circle" style="color: #27ae60;"></i> Filed</span><span class="value">78%</span></div>
                        <div class="progress-bar-container"><div class="progress-bar filed" style="width: 78%;"></div></div>
                        <div class="progress-item"><span class="label"><i class="fas fa-circle" style="color: #3498db;"></i> Pending</span><span class="value">22%</span></div>
                        <div class="progress-bar-container"><div class="progress-bar pending" style="width: 22%;"></div></div>
                    </div>
                </div>
                <div class="stat-card">
                    <div class="stat-header"><span>System Overview</span><button class="close-btn">&times;</button></div>
                    <div class="system-stats">
                        <div class="system-stat"><span class="value">99.9%</span><span class="label">Uptime</span></div>
                        <div class="system-stat"><span class="value">1.2ms</span><span class="label">Avg Response</span></div>
                        <div class="system-stat"><span class="value">30K+</span><span class="label">TPS</span></div>
                    </div>
                </div>
            </div>

            <div class="charts-grid">
                <div class="chart-card wide">
                    <div class="card-header">
                        <h3>Daily <strong>Transaction Volume</strong></h3>
                        <div class="chart-tabs">
                            <button class="tab active">All</button>
                            <button class="tab">Flagged</button>
                            <button class="tab">Approved</button>
                        </div>
                    </div>
                    <div class="line-chart-container"><canvas id="transactionChart"></canvas></div>
                </div>
            </div>
            ${this.getCasesTableMarkup(5, 'Recent Cases')}
        `;
    },

    getAnalyticsTemplate() { return this.getDashboardTemplate(); },

    getCasesTemplate() {
        return `
            <div class="page-header">
                <h1>Case Management <span class="subtitle">Active Investigations</span></h1>
                <div class="header-actions">
                     <button class="btn-primary"><i class="fas fa-plus"></i> New Case</button>
                     <button class="btn-secondary"><i class="fas fa-download"></i> Export</button>
                </div>
            </div>
            ${this.getCasesTableMarkup(15, 'All Active Cases')}
        `;
    },

    getSarTemplate() {
        return `
            <div class="page-header">
                <h1>SAR Reports <span class="subtitle">Suspicious Activity Filings</span></h1>
                <div class="header-actions">
                    <button class="btn-primary"><i class="fas fa-file-contract"></i> File New SAR</button>
                </div>
            </div>
            <div class="table-card">
                <table class="data-table">
                    <thead><tr><th>Report ID</th><th>Subject</th><th>Type</th><th>Filed Date</th><th>Status</th><th>Actions</th></tr></thead>
                    <tbody>
                        <tr><td>SAR-2024-0089</td><td>Global Trade LLC</td><td>Structuring</td><td>2024-03-15</td><td><span class="status-badge resolved">Filed</span></td><td><button class="action-btn"><i class="fas fa-eye"></i></button></td></tr>
                        <tr><td>SAR-2024-0092</td><td>Unknown Person</td><td>Money Laundering</td><td>2024-03-18</td><td><span class="status-badge pending">Draft</span></td><td><button class="action-btn"><i class="fas fa-edit"></i></button></td></tr>
                        <tr><td>SAR-2024-0095</td><td>Crypto Corp</td><td>Dark Web</td><td>2024-03-19</td><td><span class="status-badge pending">Review</span></td><td><button class="action-btn"><i class="fas fa-eye"></i></button></td></tr>
                    </tbody>
                </table>
            </div>
        `;
    },

    getAlertsTemplate() {
        return `
            <div class="page-header">
                <h1>Alerts <span class="subtitle">Real-time Risk Notifications</span></h1>
            </div>
            <div class="alerts-list full-page">
                <div class="alert-item high">
                    <div class="alert-icon"><i class="fas fa-exclamation-triangle"></i></div>
                    <div class="alert-content">
                        <strong>High-Risk Transaction Detected</strong>
                        <p>Merchant #4521 - $45,000 transfer to high-risk jurisdiction (KY).</p>
                        <span class="alert-time">2 minutes ago</span>
                    </div>
                </div>
                <div class="alert-item medium">
                    <div class="alert-icon"><i class="fas fa-info-circle"></i></div>
                    <div class="alert-content">
                        <strong>Sanctions Match Found</strong>
                        <p>Potential match: "Global Trade LLC"</p>
                        <span class="alert-time">15 minutes ago</span>
                    </div>
                </div>
            </div>
        `;
    },

    getScreeningTemplate() {
        return `
            <div class="page-header"><h1>Sanctions Screening</h1></div>
            <div class="stat-card wide" style="padding: 20px;">
                <h3>Screen Entity / Person</h3>
                <div style="margin-top: 15px;">
                    <label>Name</label>
                    <input type="text" class="form-input" style="width: 100%; margin-bottom: 10px; padding: 10px; background: #1a1f37; border: 1px solid #444; color: white;">
                    <button class="btn-primary" style="padding: 10px 20px;" onclick="showToast('Screening...')">Screen Now</button>
                </div>
            </div>
        `;
    },

    getTransactionsTemplate() {
        return `
            <div class="page-header"><h1>Transactions</h1></div>
            <div class="table-card">
                 <table class="data-table">
                    <thead><tr><th>Txn ID</th><th>Date</th><th>Amount</th><th>Merchant</th><th>Status</th></tr></thead>
                    <tbody>
                        <tr><td>TXN-8842</td><td>2024-03-20</td><td>$1,250.00</td><td>TechFlow</td><td><span class="status-badge resolved">Approved</span></td></tr>
                        <tr><td>TXN-8843</td><td>2024-03-20</td><td>$45,000.00</td><td>Global Pay</td><td><span class="status-badge escalated">Blocked</span></td></tr>
                    </tbody>
                 </table>
            </div>
        `;
    },

    getMerchantsTemplate() {
        return `<div class="page-header"><h1>Merchants</h1></div><div class="stat-card">Merchant Management Interface</div>`;
    },

    getCasesTableMarkup(count, title) {
        let rows = '';
        for (let i = 0; i < count; i++) {
            rows += `
                <tr>
                    <td><span class="case-id">CASE-2024-${1247 - i}</span></td>
                    <td>Merchant ${100 + i}</td>
                    <td><span class="risk-badge ${i % 3 == 0 ? 'high' : (i % 2 == 0 ? 'medium' : 'low')}">${Math.floor(Math.random() * 100)}</span></td>
                    <td><span class="status-badge ${i % 3 == 0 ? 'investigating' : (i % 2 == 0 ? 'pending' : 'resolved')}">${i % 3 == 0 ? 'Investigating' : (i % 2 == 0 ? 'Pending' : 'Resolved')}</span></td>
                    <td>Agent ${String.fromCharCode(65 + i)}</td>
                    <td>${i + 1} hours ago</td>
                    <td><button class="action-btn"><i class="fas fa-eye"></i></button></td>
                </tr>
            `;
        }
        return `
            <div class="table-card">
                <div class="card-header"><h3>${title || 'Cases'}</h3></div>
                <table class="data-table">
                    <thead><tr><th>Case ID</th><th>Merchant</th><th>Risk Score</th><th>Status</th><th>Assigned</th><th>Time</th><th>Action</th></tr></thead>
                    <tbody>${rows}</tbody>
                </table>
            </div>
        `;
    },

    getGenericTemplate(title) {
        return `
            <div class="page-header"><h1>${title}</h1></div>
            <div class="stat-card wide" style="text-align: center; padding: 50px;">
                <i class="fas fa-wrench" style="font-size: 3rem; color: #6c5ce7; margin-bottom: 20px;"></i>
                <h3>Feature Coming Soon</h3>
                <p>The ${title} module is currently being implemented.</p>
            </div>
        `;
    },

    getWidgetsTemplate() {
        return `
            <div class="page-header"><h1>Widgets <span class="subtitle">Utility & Monitoring</span></h1></div>
            <div class="card-grid">
                <div class="stat-card">
                    <div class="stat-header"><span>System Health</span><i class="fas fa-heartbeat"></i></div>
                    <div style="margin-top: 15px;">
                        <div class="progress-item"><span class="label">CPU Usage</span><span class="value">24%</span></div>
                        <div class="progress-bar-container"><div class="progress-bar" style="width: 24%; background: #00cec9;"></div></div>
                        <div class="progress-item" style="margin-top: 10px;"><span class="label">Memory</span><span class="value">58%</span></div>
                        <div class="progress-bar-container"><div class="progress-bar" style="width: 58%; background: #fd79a8;"></div></div>
                    </div>
                </div>
                <div class="stat-card">
                    <div class="stat-header"><span>Active Users</span><i class="fas fa-users"></i></div>
                    <div class="stat-value">1,248</div>
                    <div class="stat-details"><span class="status-badge resolved">Stable</span></div>
                </div>
                 <div class="stat-card">
                    <div class="stat-header"><span>Queue Status</span><i class="fas fa-layer-group"></i></div>
                     <div style="margin-top: 15px;">
                         <div style="display: flex; justify-content: space-between; margin-bottom: 5px;"><span>Sanctions</span> <strong>12</strong></div>
                         <div style="display: flex; justify-content: space-between; margin-bottom: 5px;"><span>Transactions</span> <strong>450</strong></div>
                         <div style="display: flex; justify-content: space-between;"><span>Reports</span> <strong>3</strong></div>
                     </div>
                </div>
                <div class="stat-card">
                    <div class="stat-header"><span>Quick Actions</span><i class="fas fa-bolt"></i></div>
                    <div style="display: flex; gap: 10px; flex-wrap: wrap; margin-top: 15px;">
                        <button class="btn-small" style="flex: 1;">Clear Cache</button>
                        <button class="btn-small" style="flex: 1;">Restart Service</button>
                    </div>
                </div>
            </div>
        `;
    },

    getProfileTemplate() {
        return `
            <div class="page-header"><h1>User Profile</h1></div>
            <div class="card-grid" style="grid-template-columns: 1fr 2fr;">
                <div class="stat-card">
                    <div style="text-align: center; padding: 20px;">
                        <div style="width: 80px; height: 80px; background: #6c5ce7; border-radius: 50%; margin: 0 auto; display: flex; align-items: center; justify-content: center; font-size: 2rem;">A</div>
                        <h3 style="margin: 15px 0 5px;">Admin User</h3>
                        <p style="color: #aaa; font-size: 0.9em;">Senior Risk Analyst</p>
                        <div style="margin-top: 20px;">
                            <span class="status-badge resolved">Active</span>
                        </div>
                    </div>
                    <div style="border-top: 1px solid rgba(255,255,255,0.1); padding: 15px;">
                        <div style="display: flex; justify-content: space-between; margin-bottom: 8px;"><span style="color:#aaa;">Role</span> <strong>Admin</strong></div>
                        <div style="display: flex; justify-content: space-between; margin-bottom: 8px;"><span style="color:#aaa;">Dept</span> <strong>Security</strong></div>
                        <div style="display: flex; justify-content: space-between;"><span style="color:#aaa;">Joined</span> <strong>Jan 2024</strong></div>
                    </div>
                </div>
                <div class="stat-card">
                    <div class="stat-header"><span>Edit Profile</span></div>
                    <div class="form-container" style="margin-top: 15px;">
                        <div class="form-group" style="display: grid; grid-template-columns: 1fr 1fr; gap: 15px;">
                            <div><label>First Name</label><input type="text" value="Admin" class="form-input" style="width: 100%;"></div>
                            <div><label>Last Name</label><input type="text" value="User" class="form-input" style="width: 100%;"></div>
                        </div>
                        <div class="form-group" style="margin-top: 15px;">
                            <label>Email Address</label>
                            <input type="email" value="admin@example.com" class="form-input" style="width: 100%;">
                        </div>
                        <div class="form-group" style="margin-top: 15px;">
                            <label>Phone</label>
                            <input type="tel" value="+1 (555) 000-0000" class="form-input" style="width: 100%;">
                        </div>
                        <div style="margin-top: 20px; text-align: right;">
                             <button class="btn-primary">Save Changes</button>
                        </div>
                    </div>
                </div>
            </div>
        `;
    },

    getMessagesTemplate() {
        return `
            <div class="page-header">
                <h1>Messages <span class="subtitle">Internal Communications</span></h1>
                <div class="header-actions"><button class="btn-primary">Compose</button></div>
            </div>
            <div class="card-grid" style="grid-template-columns: 1fr 3fr;">
                <div class="stat-card" style="padding: 10px;">
                    <div class="nav-item active" style="padding: 10px; border-radius: 4px; cursor: pointer; background: rgba(108, 92, 231, 0.2);">Inbox <span class="badge" style="float: right;">3</span></div>
                    <div class="nav-item" style="padding: 10px; border-radius: 4px; cursor: pointer;">Sent</div>
                    <div class="nav-item" style="padding: 10px; border-radius: 4px; cursor: pointer;">Drafts</div>
                    <div class="nav-item" style="padding: 10px; border-radius: 4px; cursor: pointer;">Trash</div>
                </div>
                <div class="stat-card" style="padding: 0;">
                    <table class="data-table" style="margin: 0;">
                        <tbody>
                            <tr style="cursor: pointer;">
                                <td style="width: 40px;"><i class="fas fa-envelope" style="color: #6c5ce7;"></i></td>
                                <td><strong>System Alert</strong></td>
                                <td>Maintenance Scheduled for tonight...</td>
                                <td style="text-align: right; color: #aaa;">10:30 AM</td>
                            </tr>
                             <tr style="cursor: pointer;">
                                <td style="width: 40px;"><i class="fas fa-envelope-open" style="color: #aaa;"></i></td>
                                <td><strong>Sarah Audit</strong></td>
                                <td>RE: Q1 Compliance Report Review...</td>
                                <td style="text-align: right; color: #aaa;">Yesterday</td>
                            </tr>
                             <tr style="cursor: pointer;">
                                <td style="width: 40px;"><i class="fas fa-envelope-open" style="color: #aaa;"></i></td>
                                <td><strong>Mike Reviewer</strong></td>
                                <td>Case #1234 Escalation Request...</td>
                                <td style="text-align: right; color: #aaa;">Mar 18</td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </div>
        `;
    },
    getSettingsTemplate() {
        return `
            <div class="page-header"><h1>System Settings</h1></div>
            <div class="card-grid">
                <div class="stat-card wide" style="padding: 20px;">
                    <h3>General Configuration</h3>
                    <div style="margin-top: 15px; display: grid; gap: 15px;">
                        <div class="form-group">
                            <label>Application Name</label>
                            <input type="text" value="AML Fraud Detector" class="form-input" style="width: 100%;">
                        </div>
                        <div class="form-group">
                            <label>Risk Threshold (Default)</label>
                            <input type="range" min="0" max="100" value="75" style="width: 100%;">
                        </div>
                        <div class="form-group">
                            <label>Maintenance Mode</label>
                            <label class="switch"><input type="checkbox"> <span class="slider"></span></label>
                        </div>
                    </div>
                </div>

                <div class="stat-card wide" style="padding: 20px;">
                    <h3>Theme & Branding <span class="badge" style="background: #e17055;">Super Admin</span></h3>
                    <p style="color: #aaa; font-size: 0.9em; margin-bottom: 15px;">Customize the appearance for this tenant (PSP/Bank).</p>
                    
                    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 20px;">
                        <div>
                            <label style="display: block; margin-bottom: 10px;">Brand Logo</label>
                            <div style="border: 2px dashed #444; padding: 20px; text-align: center; border-radius: 8px; cursor: pointer;" onclick="showToast('Uploading logo...')">
                                <i class="fas fa-cloud-upload-alt" style="font-size: 2rem; color: #6c5ce7;"></i>
                                <p style="margin-top: 10px; font-size: 0.9em;">Click to upload PNG/SVG</p>
                            </div>
                        </div>
                        <div>
                             <label style="display: block; margin-bottom: 10px;">Primary Color</label>
                             <div style="display: flex; align-items: center; gap: 10px; margin-bottom: 15px;">
                                <input type="color" value="#6c5ce7" id="primaryColorPicker" style="border: none; width: 40px; height: 40px; cursor: pointer; background: none;">
                                <span style="font-family: monospace;">#6c5ce7</span>
                             </div>
                             
                             <label style="display: block; margin-bottom: 10px;">Secondary Color</label>
                             <div style="display: flex; align-items: center; gap: 10px;">
                                <input type="color" value="#00cec9" style="border: none; width: 40px; height: 40px; cursor: pointer; background: none;">
                                <span style="font-family: monospace;">#00cec9</span>
                             </div>
                        </div>
                    </div>
                    
                    <div style="margin-top: 20px; border-top: 1px solid #444; padding-top: 15px;">
                        <label style="display: block; margin-bottom: 10px;">Appearance</label>
                        <div style="display: flex; gap: 15px;">
                            <label style="display: flex; align-items: center; gap: 8px;"><input type="radio" name="theme" checked> <span>Dark Mode</span></label>
                            <label style="display: flex; align-items: center; gap: 8px;"><input type="radio" name="theme"> <span>Light Mode</span></label>
                            <label style="display: flex; align-items: center; gap: 8px;"><input type="radio" name="theme"> <span>High Contrast</span></label>
                        </div>
                    </div>

                    <div style="margin-top: 20px; text-align: right;">
                        <button class="btn-secondary">Reset Default</button>
                        <button class="btn-primary" onclick="showToast('Applying new theme...')">Apply Changes</button>
                    </div>
                </div>
            </div>
        `;
    },

    getUserManagementTemplate() {
        return `
            <div class="page-header">
                <h1>User Management <span class="subtitle">RBAC Administration</span></h1>
                <div class="header-actions">
                    <button class="btn-primary" onclick="showToast('Opening New User Modal...')"><i class="fas fa-user-plus"></i> Add User</button>
                </div>
            </div>
            <div class="table-card">
                <table class="data-table">
                    <thead><tr><th>User</th><th>Role</th><th>Department</th><th>Last Login</th><th>Status</th><th>Actions</th></tr></thead>
                    <tbody>
                        <tr>
                            <td><div style="display: flex; align-items: center; gap: 10px;"><div style="width: 30px; height: 30px; background: #6c5ce7; border-radius: 50%; display: flex; align-items: center; justify-content: center;">A</div> <div><strong>Admin User</strong><br><span style="font-size: 0.8em; color: #888;">admin@example.com</span></div></div></td>
                            <td><span class="status-badge" style="background: rgba(108, 92, 231, 0.2); color: #a29bfe;">Administrator</span></td>
                            <td>IT Security</td>
                            <td>Just now</td>
                            <td><span class="status-badge resolved">Active</span></td>
                            <td><button class="action-btn"><i class="fas fa-edit"></i></button></td>
                        </tr>
                        <tr>
                            <td><div style="display: flex; align-items: center; gap: 10px;"><div style="width: 30px; height: 30px; background: #00cec9; border-radius: 50%; display: flex; align-items: center; justify-content: center;">J</div> <div><strong>Jane Analyst</strong><br><span style="font-size: 0.8em; color: #888;">jane.doe@example.com</span></div></div></td>
                            <td><span class="status-badge" style="background: rgba(0, 206, 201, 0.2); color: #81ecec;">Analyst</span></td>
                            <td>Compliance</td>
                            <td>2 hours ago</td>
                            <td><span class="status-badge resolved">Active</span></td>
                            <td><button class="action-btn"><i class="fas fa-edit"></i></button></td>
                        </tr>
                        <tr>
                            <td><div style="display: flex; align-items: center; gap: 10px;"><div style="width: 30px; height: 30px; background: #fd79a8; border-radius: 50%; display: flex; align-items: center; justify-content: center;">M</div> <div><strong>Mike Reviewer</strong><br><span style="font-size: 0.8em; color: #888;">mike.r@example.com</span></div></div></td>
                            <td><span class="status-badge" style="background: rgba(253, 121, 168, 0.2); color: #fab1a0;">Reviewer</span></td>
                            <td>Risk Ops</td>
                            <td>1 day ago</td>
                            <td><span class="status-badge pending">Away</span></td>
                            <td><button class="action-btn"><i class="fas fa-edit"></i></button></td>
                        </tr>
                         <tr>
                            <td><div style="display: flex; align-items: center; gap: 10px;"><div style="width: 30px; height: 30px; background: #b2bec3; border-radius: 50%; display: flex; align-items: center; justify-content: center;">S</div> <div><strong>Sarah Audit</strong><br><span style="font-size: 0.8em; color: #888;">s.audit@example.com</span></div></div></td>
                            <td><span class="status-badge">Auditor</span></td>
                            <td>Internal Audit</td>
                            <td>Yesterday</td>
                            <td><span class="status-badge escalated">Locked</span></td>
                            <td><button class="action-btn"><i class="fas fa-unlock"></i></button></td>
                        </tr>
                    </tbody>
                </table>
            </div>
        `;
    },

    getReportsTemplate() {
        return `
            <div class="page-header"><h1>Reports center</h1></div>
            <div class="card-grid">
                <div class="stat-card">
                    <div class="stat-header"><span>Monthly SAR Summary</span></div>
                     <p style="color: #aaa; font-size: 0.9em; margin: 10px 0;">Comprehensive breakdown of all SARs filed in the last 30 days.</p>
                     <button class="btn-primary" style="width: 100%;">Generate PDF</button>
                </div>
                 <div class="stat-card">
                    <div class="stat-header"><span>Alert Efficiency</span></div>
                     <p style="color: #aaa; font-size: 0.9em; margin: 10px 0;">Analysis of false positives vs actionable alerts.</p>
                     <button class="btn-primary" style="width: 100%;">Generate CSV</button>
                </div>
            </div>
        `;
    },

    getAuditLogsTemplate() {
        return `
            <div class="page-header"><h1>Audit Logs <span class="subtitle">System Activity</span></h1></div>
            <div class="table-card">
                <table class="data-table">
                    <thead><tr><th>Timestamp</th><th>User</th><th>Action</th><th>Resource</th><th>Details</th></tr></thead>
                    <tbody>
                        <tr><td>2024-03-20 15:45:22</td><td>admin@example.com</td><td>LOGIN</td><td>System</td><td>Successful login from 192.168.1.5</td></tr>
                        <tr><td>2024-03-20 14:12:10</td><td>jane.doe@example.com</td><td>UPDATE_CASE</td><td>CASE-2024-1245</td><td>Changed status to 'Investigating'</td></tr>
                        <tr><td>2024-03-20 11:30:05</td><td>system</td><td>AUTO_SCREEN</td><td>TXN-8843</td><td>Screening check triggered</td></tr>
                         <tr><td>2024-03-20 09:15:00</td><td>admin@example.com</td><td>CONFIG_CHANGE</td><td>Rules Engine</td><td>Updated high-risk threshold to 85</td></tr>
                    </tbody>
                </table>
            </div>
        `;
    }
};

// ==========================================================================
// CHART INITIALIZATION
// ==========================================================================

function initializeCharts() {
    const riskCtx = document.getElementById('riskPieChart');
    if (riskCtx) {
        new Chart(riskCtx, {
            type: 'doughnut',
            data: {
                labels: ['High Risk', 'Medium Risk', 'Low Risk'],
                datasets: [{
                    data: [15, 35, 50],
                    backgroundColor: ['#e74c3c', '#f39c12', '#27ae60'],
                    borderWidth: 0
                }]
            },
            options: { responsive: true, maintainAspectRatio: false, cutout: '70%', plugins: { legend: { display: false } } }
        });
    }

    const txnCtx = document.getElementById('transactionChart');
    if (txnCtx) {
        const gradient = txnCtx.getContext('2d').createLinearGradient(0, 0, 0, 300);
        gradient.addColorStop(0, 'rgba(241, 196, 15, 0.3)');
        gradient.addColorStop(1, 'rgba(241, 196, 15, 0)');

        new Chart(txnCtx, {
            type: 'line',
            data: {
                labels: ['Dec 19', 'Dec 25', 'Dec 31', 'Jan 10', 'Jan 14', 'Jan 20', 'Jan 27'],
                datasets: [{
                    label: 'Transactions',
                    data: [0.5, 2.5, 3, 4, 3.5, 2.8, 2.5],
                    borderColor: '#f1c40f',
                    backgroundColor: gradient,
                    borderWidth: 2,
                    fill: true,
                    tension: 0.4,
                    pointRadius: 0
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                interaction: { intersect: false, mode: 'index' },
                scales: {
                    x: { grid: { color: 'rgba(255, 255, 255, 0.05)' }, ticks: { color: 'rgba(255, 255, 255, 0.5)' } },
                    y: { min: 0, grid: { color: 'rgba(255, 255, 255, 0.05)' }, ticks: { color: 'rgba(255, 255, 255, 0.5)' } }
                },
                plugins: { legend: { display: false } }
            }
        });
    }
}

// ==========================================================================
// NAVIGATION
// ==========================================================================

function initializeNavigation() {
    const navItems = document.querySelectorAll('.nav-item');
    navItems.forEach(item => {
        item.addEventListener('click', function (e) {
            e.preventDefault();

            const submenu = this.nextElementSibling;
            const hasSubmenu = submenu && submenu.classList.contains('nav-submenu');

            if (hasSubmenu) {
                submenu.classList.toggle('show');
                this.classList.toggle('active');
            } else {
                document.querySelectorAll('.nav-item').forEach(el => el.classList.remove('active'));
                document.querySelectorAll('.nav-subitem').forEach(el => el.classList.remove('active'));
                this.classList.add('active');

                const text = this.querySelector('span:not(.badge)')?.textContent.trim() || 'Dashboard';
                ViewRenderer.render(text);

                if (window.innerWidth < 768) {
                    document.querySelector('.sidebar').classList.remove('open');
                }
            }
        });
    });

    const subItems = document.querySelectorAll('.nav-subitem');
    subItems.forEach(item => {
        item.addEventListener('click', function (e) {
            e.preventDefault();
            document.querySelectorAll('.nav-item').forEach(el => el.classList.remove('active'));
            document.querySelectorAll('.nav-subitem').forEach(el => el.classList.remove('active'));
            this.classList.add('active');

            this.closest('.nav-section')?.querySelector('.nav-item')?.classList.add('active');

            const text = this.textContent.trim();
            ViewRenderer.render(text);
        });
    });

    const chartTabs = document.querySelectorAll('.chart-tabs .tab');
    chartTabs.forEach(tab => {
        tab.addEventListener('click', function () {
            this.parentElement.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
            this.classList.add('active');
        });
    });
}

// ==========================================================================
// REAL-TIME UPDATES
// ==========================================================================

function initializeRealTimeUpdates() {
    setInterval(updateStats, 5000);
    setInterval(checkForNewAlerts, 10000);
}

function updateStats() {
    const statValue = document.querySelector('.stat-value');
    if (statValue) {
        const currentValue = parseInt(statValue.textContent);
        const change = Math.floor(Math.random() * 5) - 2;
        statValue.textContent = Math.max(0, currentValue + change);
    }
}

function checkForNewAlerts() {
    const notificationCount = document.querySelector('.notification-count');
    if (notificationCount) {
        const current = parseInt(notificationCount.textContent);
        if (Math.random() > 0.8) {
            notificationCount.textContent = current + 1;
            showToast('New alert received');
        }
    }
}

function showToast(message) {
    const toast = document.createElement('div');
    toast.className = 'toast-notification';
    toast.innerHTML = `<i class="fas fa-bell"></i><span>${message}</span>`;
    toast.style.cssText = `
        position: fixed; bottom: 20px; right: 20px;
        background: rgba(108, 92, 231, 0.95); color: white;
        padding: 12px 20px; border-radius: 8px;
        display: flex; align-items: center; gap: 10px;
        box-shadow: 0 4px 20px rgba(0, 0, 0, 0.3); z-index: 1000;
        animation: slideIn 0.3s ease;
    `;

    document.body.appendChild(toast);
    setTimeout(() => {
        toast.style.animation = 'slideOut 0.3s ease';
        setTimeout(() => toast.remove(), 300);
    }, 3000);
}

// Styles
const style = document.createElement('style');
style.textContent = `
    @keyframes slideIn { from { transform: translateX(100%); opacity: 0; } to { transform: translateX(0); opacity: 1; } }
    @keyframes slideOut { from { transform: translateX(0); opacity: 1; } to { transform: translateX(100%); opacity: 0; } }
`;
document.head.appendChild(style);
