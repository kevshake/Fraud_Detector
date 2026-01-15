/**
 * API Interceptor
 * Intercepts all fetch requests to handle session expiration
 */

(function() {
    'use strict';

    // Store original fetch
    const originalFetch = window.fetch;

    // Override fetch
    window.fetch = function(...args) {
        return originalFetch.apply(this, args)
            .then(response => {
                // Check for session expiration
                if (response.status === 401 || response.status === 403) {
                    // Don't intercept session check/refresh endpoints
                    const url = args[0];
                    if (typeof url === 'string' && 
                        (url.includes('/auth/session/check') || 
                         url.includes('/auth/session/refresh'))) {
                        return response;
                    }
                    
                    // Handle session expiration
                    if (window.sessionManager) {
                        window.sessionManager.handleSessionExpired();
                    } else {
                        // Fallback: redirect to login
                        const currentPath = window.location.pathname;
                        if (currentPath !== '/login.html' && !currentPath.includes('login')) {
                            sessionStorage.setItem('redirectAfterLogin', currentPath);
                            window.location.href = '/login.html?expired=true';
                        }
                    }
                }
                return response;
            })
            .catch(error => {
                console.error('Fetch error:', error);
                throw error;
            });
    };
})();

