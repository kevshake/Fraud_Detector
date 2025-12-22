/**
 * Session Management Module
 * Handles session timeout, auto-refresh, and expiration warnings
 */

class SessionManager {
    constructor() {
        this.sessionTimeout = 30 * 60 * 1000; // 30 minutes in milliseconds
        this.warningTime = 5 * 60 * 1000; // Show warning 5 minutes before timeout
        this.checkInterval = 60 * 1000; // Check every minute
        this.activityCheckInterval = 30 * 1000; // Check for activity every 30 seconds
        
        this.lastActivityTime = Date.now();
        this.sessionTimer = null;
        this.warningTimer = null;
        this.activityCheckTimer = null;
        this.warningCountdownInterval = null;
        this.warningShown = false;
        
        this.init();
    }

    init() {
        // Restore session timeout from localStorage if available
        const savedTimeout = localStorage.getItem('sessionTimeout');
        if (savedTimeout) {
            this.sessionTimeout = parseInt(savedTimeout);
        }

        // Track user activity
        this.trackActivity();
        
        // Start session monitoring
        this.startSessionMonitoring();
        
        // Check session validity on page load
        this.checkSessionValidity();
    }

    /**
     * Track user activity to reset session timeout
     */
    trackActivity() {
        const activityEvents = ['mousedown', 'mousemove', 'keypress', 'scroll', 'touchstart', 'click'];
        
        activityEvents.forEach(event => {
            document.addEventListener(event, () => {
                this.updateLastActivity();
            }, { passive: true });
        });

        // Also track visibility changes
        document.addEventListener('visibilitychange', () => {
            if (!document.hidden) {
                this.updateLastActivity();
                this.checkSessionValidity();
            }
        });
    }

    /**
     * Update last activity time and reset session timeout
     */
    updateLastActivity() {
        const now = Date.now();
        const timeSinceLastActivity = now - this.lastActivityTime;
        
        // Always update last activity time on any user interaction
        this.lastActivityTime = now;
        
        // If user has been inactive for more than 2 minutes, refresh session on backend
        // This ensures session is kept alive during active use
        if (timeSinceLastActivity > 2 * 60 * 1000) {
            this.refreshSession();
        } else {
            // Just restart timers to reset countdown
            this.startSessionMonitoring();
        }
        
        // Update status indicator
        this.updateSessionStatus('active');
    }

    /**
     * Start session monitoring
     */
    startSessionMonitoring() {
        // Clear existing timers
        this.clearTimers();

        // Calculate time until timeout
        const timeUntilTimeout = this.sessionTimeout - (Date.now() - this.lastActivityTime);
        const timeUntilWarning = timeUntilTimeout - this.warningTime;

        // Set warning timer
        if (timeUntilWarning > 0) {
            this.warningTimer = setTimeout(() => {
                this.showSessionWarning();
            }, timeUntilWarning);
        }

        // Set session timeout timer
        if (timeUntilTimeout > 0) {
            this.sessionTimer = setTimeout(() => {
                this.handleSessionTimeout();
            }, timeUntilTimeout);
        }

        // Periodic activity check
        this.activityCheckTimer = setInterval(() => {
            this.checkActivityAndRefresh();
        }, this.activityCheckInterval);
    }

    /**
     * Check activity and refresh session if needed
     */
    checkActivityAndRefresh() {
        const timeSinceLastActivity = Date.now() - this.lastActivityTime;
        const timeUntilTimeout = this.sessionTimeout - timeSinceLastActivity;
        
        // Update status indicator based on time remaining
        if (timeUntilTimeout < this.warningTime) {
            this.updateSessionStatus('expiring');
        } else if (timeUntilTimeout < this.warningTime * 2) {
            this.updateSessionStatus('warning');
        } else {
            this.updateSessionStatus('active');
        }
        
        // If user has been inactive for more than 5 minutes, refresh session on backend
        // This keeps the session alive if user is still on the page but not actively interacting
        if (timeSinceLastActivity > 5 * 60 * 1000 && timeSinceLastActivity < 10 * 60 * 1000) {
            this.refreshSession();
        }
        
        // If timeout has passed, handle expiration
        if (timeUntilTimeout <= 0) {
            this.handleSessionTimeout();
        }
    }

    /**
     * Refresh session on backend
     */
    async refreshSession() {
        try {
            // Make a lightweight request to keep session alive
            const response = await fetch('/api/v1/auth/session/refresh', {
                method: 'POST',
                credentials: 'include',
                headers: {
                    'Content-Type': 'application/json',
                    'X-Requested-With': 'XMLHttpRequest'
                }
            });

            if (response.ok) {
                const data = await response.json();
                // Update session timeout from server response
                if (data.timeRemaining) {
                    this.sessionTimeout = data.timeRemaining * 1000;
                    localStorage.setItem('sessionTimeout', this.sessionTimeout.toString());
                } else if (data.sessionTimeout) {
                    this.sessionTimeout = data.sessionTimeout * 1000;
                    localStorage.setItem('sessionTimeout', this.sessionTimeout.toString());
                }
                // Reset last activity time to now (session refreshed, countdown resets)
                this.lastActivityTime = Date.now();
                this.warningShown = false;
                this.updateSessionStatus('active');
                // Restart monitoring with fresh timers (countdown resets)
                this.startSessionMonitoring();
            } else if (response.status === 401 || response.status === 403) {
                // Session expired
                this.handleSessionExpired();
            }
        } catch (error) {
            console.error('Error refreshing session:', error);
            // Don't show error to user, just log it
        }
    }
    
    /**
     * Update session status indicator
     */
    updateSessionStatus(status) {
        const indicator = document.getElementById('sessionStatusIndicator');
        const icon = document.getElementById('sessionStatusIcon');
        const text = document.getElementById('sessionStatusText');
        
        if (!indicator || !icon || !text) return;
        
        indicator.classList.remove('active', 'warning', 'expiring');
        
        switch (status) {
            case 'active':
                indicator.classList.add('active');
                icon.style.color = '#27ae60';
                text.textContent = 'Session Active';
                break;
            case 'warning':
                indicator.classList.add('active', 'warning');
                icon.style.color = '#f39c12';
                const timeRemaining = Math.ceil((this.sessionTimeout - (Date.now() - this.lastActivityTime)) / 1000 / 60);
                text.textContent = `Expires in ${timeRemaining}m`;
                break;
            case 'expiring':
                indicator.classList.add('active', 'expiring');
                icon.style.color = '#e74c3c';
                text.textContent = 'Session Expiring';
                break;
        }
    }

    /**
     * Check session validity and sync with server
     */
    async checkSessionValidity() {
        try {
            const response = await fetch('/api/v1/auth/session/check', {
                method: 'GET',
                credentials: 'include',
                headers: {
                    'X-Requested-With': 'XMLHttpRequest'
                }
            });

            if (response.ok) {
                const data = await response.json();
                if (data.valid && data.timeRemaining) {
                    // Sync timeout with server
                    this.sessionTimeout = data.timeRemaining * 1000;
                    localStorage.setItem('sessionTimeout', this.sessionTimeout.toString());
                    
                    // Update last activity time based on server's last accessed time
                    if (data.lastAccessedTime) {
                        // Calculate time since last server activity
                        const serverLastAccess = new Date(data.lastAccessedTime).getTime();
                        const timeSinceServerActivity = Date.now() - serverLastAccess;
                        // Set our last activity to account for server-side activity
                        this.lastActivityTime = Date.now() - Math.min(timeSinceServerActivity, 5 * 60 * 1000);
                    } else {
                        this.lastActivityTime = Date.now();
                    }
                    
                    this.updateSessionStatus('active');
                    // Restart monitoring with synced times
                    this.startSessionMonitoring();
                } else {
                    this.handleSessionExpired();
                }
            } else if (response.status === 401 || response.status === 403) {
                this.handleSessionExpired();
            }
        } catch (error) {
            console.error('Error checking session:', error);
        }
    }

    /**
     * Show session warning with live countdown
     */
    showSessionWarning() {
        if (this.warningShown) return;
        
        this.warningShown = true;
        this.updateSessionStatus('expiring');
        
        // Create warning modal
        const warningModal = document.createElement('div');
        warningModal.id = 'session-warning-modal';
        warningModal.className = 'session-warning-modal';
        warningModal.innerHTML = `
            <div class="session-warning-content">
                <div class="session-warning-icon">
                    <i class="fas fa-clock"></i>
                </div>
                <h3>Session Expiring Soon</h3>
                <p>Your session will expire in <strong id="session-warning-countdown">calculating...</strong> due to inactivity.</p>
                <p>Click "Extend Session" to continue working, or you will be automatically logged out.</p>
                <div class="session-warning-actions">
                    <button class="btn-primary" onclick="window.sessionManager.extendSession()">
                        <i class="fas fa-sync"></i> Extend Session
                    </button>
                    <button class="btn-secondary" onclick="window.sessionManager.handleSessionTimeout()">
                        <i class="fas fa-sign-out-alt"></i> Logout Now
                    </button>
                </div>
            </div>
        `;
        
        document.body.appendChild(warningModal);
        
        // Update countdown every second
        const updateCountdown = () => {
            const timeRemaining = Math.ceil((this.sessionTimeout - (Date.now() - this.lastActivityTime)) / 1000);
            const countdownEl = document.getElementById('session-warning-countdown');
            
            if (timeRemaining <= 0) {
                clearInterval(this.warningCountdownInterval);
                this.handleSessionTimeout();
                return;
            }
            
            if (countdownEl) {
                const minutes = Math.floor(timeRemaining / 60);
                const seconds = timeRemaining % 60;
                if (timeRemaining > 60) {
                    countdownEl.textContent = `${minutes} minute${minutes !== 1 ? 's' : ''}`;
                } else {
                    countdownEl.textContent = `${seconds} second${seconds !== 1 ? 's' : ''}`;
                }
            }
        };
        
        // Initial update
        updateCountdown();
        // Update every second
        this.warningCountdownInterval = setInterval(updateCountdown, 1000);
        
        // Auto-extend if user clicks anywhere on the page
        const autoExtend = () => {
            this.extendSession();
            document.removeEventListener('click', autoExtend);
        };
        document.addEventListener('click', autoExtend, { once: true });
    }

    /**
     * Extend session
     */
    async extendSession() {
        this.updateLastActivity();
        await this.refreshSession();
        this.hideWarning();
    }

    /**
     * Hide warning modal
     */
    hideWarning() {
        const modal = document.getElementById('session-warning-modal');
        if (modal) {
            modal.remove();
        }
        this.warningShown = false;
    }

    /**
     * Handle session timeout - automatically logs out user
     */
    handleSessionTimeout() {
        this.clearTimers();
        this.hideWarning();
        
        // Show timeout message briefly
        const timeoutModal = document.createElement('div');
        timeoutModal.id = 'session-timeout-modal';
        timeoutModal.className = 'session-timeout-modal';
        timeoutModal.innerHTML = `
            <div class="session-timeout-content">
                <div class="session-timeout-icon">
                    <i class="fas fa-exclamation-triangle"></i>
                </div>
                <h3>Session Expired</h3>
                <p>Your session has expired due to inactivity.</p>
                <p>You will be redirected to the login page.</p>
                <div class="session-timeout-actions">
                    <button class="btn-primary" onclick="window.sessionManager.redirectToLogin()">
                        <i class="fas fa-sign-in-alt"></i> Go to Login
                    </button>
                </div>
            </div>
        `;
        
        document.body.appendChild(timeoutModal);
        
        // Invalidate session on backend
        fetch('/api/v1/auth/session/invalidate', {
            method: 'POST',
            credentials: 'include'
        }).catch(() => {
            // Ignore errors, we're logging out anyway
        });
        
        // Auto-redirect after 2 seconds
        setTimeout(() => {
            this.redirectToLogin();
        }, 2000);
    }

    /**
     * Handle session expired (from server response)
     */
    handleSessionExpired() {
        this.clearTimers();
        this.hideWarning();
        this.redirectToLogin();
    }

    /**
     * Redirect to login page
     */
    redirectToLogin() {
        // Save current URL for redirect after login
        const currentPath = window.location.pathname + window.location.search;
        if (currentPath !== '/login.html' && currentPath !== '/') {
            sessionStorage.setItem('redirectAfterLogin', currentPath);
        }
        
        window.location.href = '/login.html?expired=true';
    }

    /**
     * Clear all timers
     */
    clearTimers() {
        if (this.sessionTimer) {
            clearTimeout(this.sessionTimer);
            this.sessionTimer = null;
        }
        if (this.warningTimer) {
            clearTimeout(this.warningTimer);
            this.warningTimer = null;
        }
        if (this.activityCheckTimer) {
            clearInterval(this.activityCheckTimer);
            this.activityCheckTimer = null;
        }
        if (this.warningCountdownInterval) {
            clearInterval(this.warningCountdownInterval);
            this.warningCountdownInterval = null;
        }
    }

    /**
     * Destroy session manager
     */
    destroy() {
        this.clearTimers();
        this.hideWarning();
    }
}

// Initialize session manager when DOM is ready
let sessionManager;
document.addEventListener('DOMContentLoaded', () => {
    // Only initialize if not on login page
    const currentPath = window.location.pathname;
    if (currentPath !== '/login.html' && currentPath !== '/' && !currentPath.includes('login')) {
        sessionManager = new SessionManager();
        window.sessionManager = sessionManager;
    }
    
    // Handle redirect after login
    const redirectPath = sessionStorage.getItem('redirectAfterLogin');
    if (redirectPath && currentPath === '/index.html') {
        sessionStorage.removeItem('redirectAfterLogin');
        // Optionally redirect to saved path
    }
    
    // Show expired message if redirected from expired session
    const urlParams = new URLSearchParams(window.location.search);
    if (urlParams.get('expired') === 'true') {
        showExpiredMessage();
    }
});

function showExpiredMessage() {
    // Remove expired parameter from URL
    const url = new URL(window.location);
    url.searchParams.delete('expired');
    window.history.replaceState({}, '', url);
    
    // Show a brief notification
    const notification = document.createElement('div');
    notification.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        background: rgba(231, 76, 60, 0.9);
        color: white;
        padding: 15px 20px;
        border-radius: 8px;
        z-index: 10000;
        box-shadow: 0 4px 12px rgba(0,0,0,0.3);
    `;
    notification.innerHTML = '<i class="fas fa-exclamation-triangle"></i> Your session has expired. Please log in again.';
    document.body.appendChild(notification);
    
    setTimeout(() => {
        notification.remove();
    }, 5000);
}

