// Compliance Calendar Functions

window.loadComplianceCalendar = async function() {
    try {
        const upcomingResponse = await fetch('/api/v1/compliance/calendar/upcoming?daysAhead=30', {
            credentials: 'include'
        });
        const upcoming = await upcomingResponse.json();
        renderUpcomingDeadlines(upcoming);
        
        const overdueResponse = await fetch('/api/v1/compliance/calendar/overdue', {
            credentials: 'include'
        });
        const overdue = await overdueResponse.json();
        renderOverdueDeadlines(overdue);
    } catch (error) {
        console.error('Error loading compliance calendar:', error);
    }
};

function renderUpcomingDeadlines(deadlines) {
    const container = document.getElementById('upcoming-deadlines-list');
    if (!container) return;
    
    if (deadlines.length === 0) {
        container.innerHTML = '<p>No upcoming deadlines</p>';
        return;
    }
    
    container.innerHTML = deadlines.map(deadline => `
        <div class="deadline-item">
            <div class="deadline-header">
                <strong>${deadline.deadlineType}</strong>
                <span class="deadline-date">${new Date(deadline.deadlineDate).toLocaleDateString()}</span>
            </div>
            <p>${deadline.description || ''}</p>
            <small>${deadline.jurisdiction || ''}</small>
        </div>
    `).join('');
}

function renderOverdueDeadlines(deadlines) {
    const container = document.getElementById('overdue-deadlines-list');
    if (!container) return;
    
    if (deadlines.length === 0) {
        container.innerHTML = '<p>No overdue deadlines</p>';
        return;
    }
    
    container.innerHTML = deadlines.map(deadline => `
        <div class="deadline-item overdue">
            <div class="deadline-header">
                <strong>${deadline.deadlineType}</strong>
                <span class="deadline-date overdue">${new Date(deadline.deadlineDate).toLocaleDateString()}</span>
            </div>
            <p>${deadline.description || ''}</p>
            <button class="btn-small" onclick="markDeadlineComplete(${deadline.id})">Mark Complete</button>
        </div>
    `).join('');
}

window.openCreateDeadlineModal = function() {
    // TODO: Implement modal for creating deadlines
    alert('Create deadline modal - to be implemented');
};

window.markDeadlineComplete = async function(deadlineId) {
    try {
        const response = await fetch(`/api/v1/compliance/calendar/deadlines/${deadlineId}/complete`, {
            method: 'POST',
            credentials: 'include'
        });
        
        if (response.ok) {
            loadComplianceCalendar();
        }
    } catch (error) {
        console.error('Error marking deadline complete:', error);
    }
};

