// Compliance Calendar Functions

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

function getHeaders(includeContentType = false) {
    const headers = {
        'X-Requested-With': 'XMLHttpRequest'
    };
    if (includeContentType) {
        headers['Content-Type'] = 'application/json';
    }
    const csrfToken = getCsrfToken();
    if (csrfToken) {
        headers['X-XSRF-TOKEN'] = csrfToken;
    }
    return headers;
}

window.loadComplianceCalendar = async function () {
    try {
        const upcomingResponse = await fetch('compliance/calendar/upcoming?daysAhead=30', {
            credentials: 'include',
            headers: getHeaders()
        });
        const upcoming = await upcomingResponse.json();
        renderUpcomingDeadlines(upcoming);

        const overdueResponse = await fetch('compliance/calendar/overdue', {
            credentials: 'include',
            headers: getHeaders()
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

    if (!deadlines || deadlines.length === 0) {
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

    if (!deadlines || deadlines.length === 0) {
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

window.openCreateDeadlineModal = function () {
    const modal = document.getElementById('createDeadlineModal');
    if (modal) {
        modal.style.display = 'block';
    } else {
        console.error('createDeadlineModal not found');
        alert('Modal not found. Please refresh the page.');
    }
};

window.closeCreateDeadlineModal = function () {
    const modal = document.getElementById('createDeadlineModal');
    if (modal) {
        modal.style.display = 'none';
    }
};

// Initialize form submission
document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('createDeadlineForm');
    if (form) {
        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            const formData = new FormData(form);
            const data = {
                deadlineType: formData.get('title'),
                description: formData.get('description'),
                deadlineDate: formData.get('dueDate'),
                priority: formData.get('priority')
            };

            try {
                const response = await fetch('compliance/calendar/deadlines', {
                    method: 'POST',
                    headers: getHeaders(true),
                    body: JSON.stringify(data),
                    credentials: 'include'
                });

                if (response.ok) {
                    closeCreateDeadlineModal();
                    form.reset();
                    loadComplianceCalendar();
                    alert('Deadline added successfully!');
                } else {
                    const errorText = await response.text();
                    alert('Failed to save deadline: ' + (errorText || response.statusText));
                }
            } catch (error) {
                console.error('Error saving deadline:', error);
                alert('An error occurred: ' + error.message);
            }
        });
    }
});

window.markDeadlineComplete = async function (deadlineId) {
    try {
        const response = await fetch(`compliance/calendar/deadlines/${deadlineId}/complete`, {
            method: 'POST',
            headers: getHeaders(),
            credentials: 'include'
        });

        if (response.ok) {
            loadComplianceCalendar();
            alert('Deadline marked as complete!');
        }
    } catch (error) {
        console.error('Error marking deadline complete:', error);
    }
};
