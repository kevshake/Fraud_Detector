// Case Management Enhanced Functions

let currentCaseId = null;

// Enhanced Case Fetching with new fields
window.fetchCases = async function() {
    try {
        const response = await fetch('compliance/cases', {
            credentials: 'include'
        });
        const cases = await response.json();
        
        const tbody = document.querySelector('#cases-table tbody');
        if (!tbody) return;
        
        tbody.innerHTML = cases.map(c => `
            <tr onclick="viewCaseDetail(${c.id})" style="cursor: pointer;">
                <td><span class="case-id">${c.caseReference || 'CASE-' + c.id}</span></td>
                <td>${c.merchantId || 'N/A'}</td>
                <td><span class="priority-badge ${c.priority?.toLowerCase() || 'medium'}">${c.priority || 'MEDIUM'}</span></td>
                <td><span class="status-badge ${getStatusClass(c.status)}">${c.status || 'NEW'}</span></td>
                <td>${c.assignedTo?.username || 'Unassigned'}</td>
                <td>${c.slaDeadline ? new Date(c.slaDeadline).toLocaleDateString() : 'N/A'}</td>
                <td>${c.daysOpen || 0} days</td>
                <td>
                    <button class="action-btn" onclick="event.stopPropagation(); viewCaseDetail(${c.id})">
                        <i class="fas fa-eye"></i>
                    </button>
                </td>
            </tr>
        `).join('');
        
        // Update stats
        updateCaseStats(cases);
    } catch (error) {
        console.error('Error fetching cases:', error);
        demoCases();
    }
};

function updateCaseStats(cases) {
    const openCases = cases.filter(c => ['NEW', 'ASSIGNED', 'IN_PROGRESS'].includes(c.status)).length;
    const overdue = cases.filter(c => {
        if (!c.slaDeadline) return false;
        return new Date(c.slaDeadline) < new Date() && c.status !== 'CLOSED_CLEARED';
    }).length;
    const escalated = cases.filter(c => c.escalated || c.status === 'ESCALATED').length;
    const unassigned = cases.filter(c => !c.assignedTo).length;
    
    document.getElementById('openCasesCount').textContent = openCases;
    document.getElementById('overdueCasesCount').textContent = overdue;
    document.getElementById('escalatedCasesCount').textContent = escalated;
    document.getElementById('unassignedCasesCount').textContent = unassigned;
}

// View Case Detail
window.viewCaseDetail = async function(caseId) {
    currentCaseId = caseId;
    showView('case-detail-view');
    
    try {
        const response = await fetch(`compliance/cases/${caseId}`, {
            credentials: 'include'
        });
        const caseData = await response.json();
        
        document.getElementById('case-detail-title').textContent = 
            `Case: ${caseData.caseReference || 'CASE-' + caseId}`;
        
        // Load case info
        document.getElementById('case-info-content').innerHTML = `
            <p><strong>Status:</strong> <span class="status-badge ${getStatusClass(caseData.status)}">${caseData.status}</span></p>
            <p><strong>Priority:</strong> <span class="priority-badge ${caseData.priority?.toLowerCase()}">${caseData.priority}</span></p>
            <p><strong>Assigned To:</strong> ${caseData.assignedTo?.username || 'Unassigned'}</p>
            <p><strong>SLA Deadline:</strong> ${caseData.slaDeadline ? new Date(caseData.slaDeadline).toLocaleString() : 'N/A'}</p>
            <p><strong>Days Open:</strong> ${caseData.daysOpen || 0}</p>
            <p><strong>Description:</strong> ${caseData.description || 'N/A'}</p>
        `;
        
        // Load timeline
        loadCaseTimeline(caseId);
        loadCaseActivities(caseId);
    } catch (error) {
        console.error('Error loading case details:', error);
    }
};

// Load Case Timeline
async function loadCaseTimeline(caseId) {
    try {
        const response = await fetch(`cases/${caseId}/timeline`, {
            credentials: 'include'
        });
        const timeline = await response.json();
        
        const timelineContent = document.getElementById('case-timeline-content');
        timelineContent.innerHTML = timeline.events.map(event => `
            <div class="timeline-event">
                <div class="timeline-dot"></div>
                <div class="timeline-content">
                    <strong>${event.type}</strong>
                    <p>${event.description}</p>
                    <small>${new Date(event.timestamp).toLocaleString()}</small>
                </div>
            </div>
        `).join('');
    } catch (error) {
        console.error('Error loading timeline:', error);
    }
}

// Load Case Activities
async function loadCaseActivities(caseId) {
    try {
        const response = await fetch(`cases/${caseId}/activities?page=0&size=20`, {
            credentials: 'include'
        });
        const activities = await response.json();
        
        const activitiesContent = document.getElementById('case-activities-content');
        if (activities.content) {
            activitiesContent.innerHTML = activities.content.map(activity => `
                <div class="activity-item">
                    <div class="activity-icon"><i class="fas fa-${getActivityIcon(activity.activityType)}"></i></div>
                    <div class="activity-content">
                        <strong>${activity.performedBy?.username || 'System'}</strong>
                        <p>${activity.description || activity.activityType}</p>
                        <small>${new Date(activity.performedAt).toLocaleString()}</small>
                    </div>
                </div>
            `).join('');
        }
    } catch (error) {
        console.error('Error loading activities:', error);
    }
}

function getActivityIcon(activityType) {
    const icons = {
        'CASE_CREATED': 'plus-circle',
        'CASE_ASSIGNED': 'user-plus',
        'CASE_ESCALATED': 'arrow-up',
        'NOTE_ADDED': 'sticky-note',
        'EVIDENCE_ADDED': 'paperclip'
    };
    return icons[activityType] || 'circle';
}

// Case Tab Switching
window.showCaseTab = function(tabName) {
    document.querySelectorAll('.case-tab-content').forEach(tab => tab.classList.remove('active'));
    document.querySelectorAll('.tab-btn').forEach(btn => btn.classList.remove('active'));
    
    document.getElementById(`case-${tabName}-tab`).classList.add('active');
    event.target.classList.add('active');
    
    if (tabName === 'timeline' && currentCaseId) {
        loadCaseTimeline(currentCaseId);
    } else if (tabName === 'activities' && currentCaseId) {
        loadCaseActivities(currentCaseId);
    }
};

// Escalate Case
window.escalateCase = async function() {
    if (!currentCaseId) return;
    
    const reason = prompt('Enter escalation reason:');
    if (!reason) return;
    
    try {
        const response = await fetch(`cases/${currentCaseId}/escalate`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            credentials: 'include',
            body: JSON.stringify({ reason })
        });
        
        if (response.ok) {
            alert('Case escalated successfully');
            viewCaseDetail(currentCaseId);
        }
    } catch (error) {
        console.error('Error escalating case:', error);
    }
};

// View Case Network Graph
window.viewCaseNetwork = function() {
    if (!currentCaseId) return;
    showView('case-network-view');
    loadNetworkGraph(currentCaseId);
};

// Load Network Graph
window.loadNetworkGraph = async function(caseId) {
    if (!caseId) caseId = currentCaseId;
    if (!caseId) return;
    
    const depth = document.getElementById('network-depth')?.value || 2;
    
    try {
        const response = await fetch(`cases/${caseId}/network?depth=${depth}`, {
            credentials: 'include'
        });
        const graph = await response.json();
        
        // Simple network visualization (can be enhanced with D3.js or vis.js)
        const container = document.getElementById('network-graph-container');
        container.innerHTML = `
            <div class="network-stats">
                <p><strong>Nodes:</strong> ${graph.nodes.length}</p>
                <p><strong>Edges:</strong> ${graph.edges.length}</p>
            </div>
            <div class="network-nodes">
                ${graph.nodes.map(node => `
                    <div class="network-node ${node.type.toLowerCase()}">
                        <strong>${node.label}</strong>
                        <small>${node.type}</small>
                    </div>
                `).join('')}
            </div>
        `;
    } catch (error) {
        console.error('Error loading network graph:', error);
    }
};

// Filter Cases
window.filterCases = function() {
    const status = document.getElementById('caseStatusFilter').value;
    fetchCases(); // Will be enhanced to filter by status
};

// Refresh Cases
window.refreshCases = function() {
    fetchCases();
};

