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

// ==========================================================================
// Calendar widget helpers
// ==========================================================================

function parseDeadlineDateValue(value) {
    if (!value) return null;
    try {
        if (typeof value === 'string') {
            const d = new Date(value);
            return Number.isNaN(d.getTime()) ? null : d;
        }
        if (Array.isArray(value)) {
            const [year, month, day, hour = 0, minute = 0, second = 0] = value;
            const d = new Date(year, (month || 1) - 1, day || 1, hour, minute, second);
            return Number.isNaN(d.getTime()) ? null : d;
        }
        const d = new Date(value);
        return Number.isNaN(d.getTime()) ? null : d;
    } catch (e) {
        return null;
    }
}

function pad2(n) {
    return String(n).padStart(2, '0');
}

function toDateKeyLocal(date) {
    if (!date) return '';
    const y = date.getFullYear();
    const m = pad2(date.getMonth() + 1);
    const d = pad2(date.getDate());
    return `${y}-${m}-${d}`;
}

function monthStart(date) {
    return new Date(date.getFullYear(), date.getMonth(), 1);
}

function addMonths(date, delta) {
    return new Date(date.getFullYear(), date.getMonth() + delta, 1);
}

function formatMonthLabel(date) {
    return date.toLocaleString('en-US', { month: 'long', year: 'numeric' });
}

function renderCalendarWidget(deadlines) {
    const grid = document.getElementById('compliance-calendar-grid');
    const monthLabel = document.getElementById('calendarMonthLabel');
    const details = document.getElementById('calendar-day-details');

    if (!grid) return;

    const allDeadlines = Array.isArray(deadlines) ? deadlines : [];

    if (!window._complianceCalendarCurrentMonth) {
        window._complianceCalendarCurrentMonth = new Date();
    }

    const currentMonth = monthStart(window._complianceCalendarCurrentMonth);
    if (monthLabel) monthLabel.textContent = formatMonthLabel(currentMonth);

    // Group deadlines by day
    const byDay = {};
    allDeadlines.forEach(dl => {
        const d = parseDeadlineDateValue(dl.deadlineDate);
        if (!d) return;
        const key = toDateKeyLocal(d);
        if (!byDay[key]) byDay[key] = [];
        byDay[key].push(dl);
    });

    // Build calendar grid (7 weekday headers + 6 weeks)
    const weekdays = ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];
    const firstDay = new Date(currentMonth.getFullYear(), currentMonth.getMonth(), 1);
    const startOffset = firstDay.getDay(); // 0-6, Sun-based
    const startDate = new Date(firstDay);
    startDate.setDate(firstDay.getDate() - startOffset);

    const todayKey = toDateKeyLocal(new Date());
    const now = new Date();
    const todayMidnight = new Date(now.getFullYear(), now.getMonth(), now.getDate());

    let html = '';
    weekdays.forEach(w => {
        html += `<div class="calendar-weekday">${w}</div>`;
    });

    for (let i = 0; i < 42; i++) {
        const cellDate = new Date(startDate);
        cellDate.setDate(startDate.getDate() + i);
        const key = toDateKeyLocal(cellDate);
        const isOtherMonth = cellDate.getMonth() !== currentMonth.getMonth();
        const isToday = key === todayKey;
        const due = byDay[key] || [];

        const overdueCount = due.filter(d => {
            const dd = parseDeadlineDateValue(d.deadlineDate);
            return dd && dd < todayMidnight;
        }).length;

        const dueCount = due.length - overdueCount;

        let badges = '';
        if (dueCount > 0) {
            badges += `<span class="calendar-badge">Due: ${dueCount}</span>`;
        }
        if (overdueCount > 0) {
            badges += `<span class="calendar-badge overdue">Overdue: ${overdueCount}</span>`;
        }

        html += `
            <div class="calendar-day ${isOtherMonth ? 'is-other-month' : ''} ${isToday ? 'is-today' : ''}" data-date="${key}">
                <div class="calendar-day-number">${cellDate.getDate()}</div>
                <div class="calendar-day-badges">${badges}</div>
            </div>
        `;
    }

    grid.innerHTML = html;

    // Wire click handlers
    grid.querySelectorAll('.calendar-day').forEach(el => {
        el.addEventListener('click', () => {
            const dateKey = el.getAttribute('data-date');
            const dayDeadlines = byDay[dateKey] || [];

            if (!details) return;
            if (!dayDeadlines.length) {
                details.style.display = 'block';
                details.innerHTML = `<h4>${dateKey}</h4><p>No deadlines due on this date.</p>`;
                return;
            }

            details.style.display = 'block';
            details.innerHTML = `
                <h4>${dateKey} (${dayDeadlines.length} deadline${dayDeadlines.length > 1 ? 's' : ''})</h4>
                ${dayDeadlines.map(d => `
                    <div class="calendar-deadline-item">
                        <div><strong>${d.deadlineType || 'GENERAL'}</strong></div>
                        <div class="meta">
                            <span>${d.jurisdiction || ''}</span>
                            <span>${d.completed ? 'Completed' : 'Open'}</span>
                        </div>
                        ${d.description ? `<div style="margin-top:6px;">${d.description}</div>` : ''}
                        ${d.id ? `<div style="margin-top:8px;">
                            <button class="btn-small" onclick="markDeadlineComplete(${d.id})">Mark Complete</button>
                        </div>` : ''}
                    </div>
                `).join('')}
            `;
        });
    });
}

window.prevCalendarMonth = function() {
    const cur = window._complianceCalendarCurrentMonth ? monthStart(window._complianceCalendarCurrentMonth) : new Date();
    window._complianceCalendarCurrentMonth = addMonths(cur, -1);
    renderCalendarWidget(window._complianceCalendarDeadlines || []);
};

window.nextCalendarMonth = function() {
    const cur = window._complianceCalendarCurrentMonth ? monthStart(window._complianceCalendarCurrentMonth) : new Date();
    window._complianceCalendarCurrentMonth = addMonths(cur, 1);
    renderCalendarWidget(window._complianceCalendarDeadlines || []);
};

window.loadComplianceCalendar = async function () {
    try {
        const upcomingResponse = await fetch('compliance/calendar/upcoming?daysAhead=30', {
            credentials: 'include',
            headers: getHeaders()
        });
        if (!upcomingResponse.ok) {
            throw new Error(`HTTP ${upcomingResponse.status}`);
        }
        const upcoming = await upcomingResponse.json();
        renderUpcomingDeadlines(upcoming);
        
        // Update upcoming count
        const upcomingCountEl = document.getElementById('upcomingDeadlinesCount');
        if (upcomingCountEl) {
            upcomingCountEl.textContent = upcoming && upcoming.length ? upcoming.length : 0;
        }

        const overdueResponse = await fetch('compliance/calendar/overdue', {
            credentials: 'include',
            headers: getHeaders()
        });
        if (!overdueResponse.ok) {
            throw new Error(`HTTP ${overdueResponse.status}`);
        }
        const overdue = await overdueResponse.json();
        renderOverdueDeadlines(overdue);
        
        // Update overdue count
        const overdueCountEl = document.getElementById('overdueDeadlinesCount');
        if (overdueCountEl) {
            overdueCountEl.textContent = overdue && overdue.length ? overdue.length : 0;
        }

        // Render month calendar widget using upcoming + overdue
        const allDeadlines = []
            .concat(Array.isArray(upcoming) ? upcoming : [])
            .concat(Array.isArray(overdue) ? overdue : []);
        window._complianceCalendarDeadlines = allDeadlines;
        renderCalendarWidget(allDeadlines);
    } catch (error) {
        console.error('Error loading compliance calendar:', error);
        // Show error message to user
        const upcomingContainer = document.getElementById('upcoming-deadlines-list');
        const overdueContainer = document.getElementById('overdue-deadlines-list');
        if (upcomingContainer) {
            upcomingContainer.innerHTML = '<p style="color: #e74c3c;">Error loading upcoming deadlines. Please try again.</p>';
        }
        if (overdueContainer) {
            overdueContainer.innerHTML = '<p style="color: #e74c3c;">Error loading overdue deadlines. Please try again.</p>';
        }
    }
};

function renderUpcomingDeadlines(deadlines) {
    const container = document.getElementById('upcoming-deadlines-list');
    if (!container) return;

    if (!deadlines || deadlines.length === 0) {
        container.innerHTML = '<p>No upcoming deadlines</p>';
        return;
    }

    container.innerHTML = deadlines.map(deadline => {
        // Handle date parsing - LocalDateTime can be serialized as string or array
        let deadlineDate;
        try {
            if (deadline.deadlineDate) {
                if (typeof deadline.deadlineDate === 'string') {
                    deadlineDate = new Date(deadline.deadlineDate);
                } else if (Array.isArray(deadline.deadlineDate)) {
                    // Handle LocalDateTime array format [year, month, day, hour, minute, second]
                    const [year, month, day, hour = 0, minute = 0, second = 0] = deadline.deadlineDate;
                    deadlineDate = new Date(year, month - 1, day, hour, minute, second);
                } else {
                    deadlineDate = new Date(deadline.deadlineDate);
                }
            }
        } catch (e) {
            console.error('Error parsing deadline date:', e);
            deadlineDate = null;
        }
        
        const formattedDate = deadlineDate ? deadlineDate.toLocaleDateString() : 'Date not available';
        
        return `
        <div class="deadline-item">
            <div class="deadline-header">
                <strong>${deadline.deadlineType || 'Unknown'}</strong>
                <span class="deadline-date">${formattedDate}</span>
            </div>
            <p>${deadline.description || ''}</p>
            <small>${deadline.jurisdiction || ''}</small>
        </div>
        `;
    }).join('');
}

function renderOverdueDeadlines(deadlines) {
    const container = document.getElementById('overdue-deadlines-list');
    if (!container) return;

    if (!deadlines || deadlines.length === 0) {
        container.innerHTML = '<p>No overdue deadlines</p>';
        return;
    }

    container.innerHTML = deadlines.map(deadline => {
        // Handle date parsing - LocalDateTime can be serialized as string or array
        let deadlineDate;
        try {
            if (deadline.deadlineDate) {
                if (typeof deadline.deadlineDate === 'string') {
                    deadlineDate = new Date(deadline.deadlineDate);
                } else if (Array.isArray(deadline.deadlineDate)) {
                    // Handle LocalDateTime array format [year, month, day, hour, minute, second]
                    const [year, month, day, hour = 0, minute = 0, second = 0] = deadline.deadlineDate;
                    deadlineDate = new Date(year, month - 1, day, hour, minute, second);
                } else {
                    deadlineDate = new Date(deadline.deadlineDate);
                }
            }
        } catch (e) {
            console.error('Error parsing deadline date:', e);
            deadlineDate = null;
        }
        
        const formattedDate = deadlineDate ? deadlineDate.toLocaleDateString() : 'Date not available';
        const deadlineId = deadline.id || deadline.deadlineId || 0;
        
        return `
        <div class="deadline-item overdue">
            <div class="deadline-header">
                <strong>${deadline.deadlineType || 'Unknown'}</strong>
                <span class="deadline-date overdue">${formattedDate}</span>
            </div>
            <p>${deadline.description || ''}</p>
            <button class="btn-small" onclick="markDeadlineComplete(${deadlineId})">Mark Complete</button>
        </div>
        `;
    }).join('');
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
            
            // Convert date string to LocalDateTime format (ISO string)
            const dueDateStr = formData.get('dueDate');
            let deadlineDate = null;
            if (dueDateStr) {
                // Convert YYYY-MM-DD to ISO datetime string (add time component)
                deadlineDate = dueDateStr + 'T00:00:00';
            }
            
            const data = {
                deadlineType: formData.get('title') || 'GENERAL',
                description: formData.get('description') || '',
                deadlineDate: deadlineDate,
                jurisdiction: formData.get('jurisdiction') || 'US' // Default jurisdiction
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
