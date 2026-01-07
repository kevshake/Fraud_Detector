// Password Reset UI
(function () {
    function getCsrfToken() {
        const cookies = document.cookie.split(';');
        for (let cookie of cookies) {
            const [name, value] = cookie.trim().split('=');
            if (name === 'XSRF-TOKEN') return decodeURIComponent(value);
        }
        return null;
    }

    async function ensureCsrfCookie() {
        try {
            await fetch('auth/csrf', { credentials: 'include' });
        } catch (e) {
            // ignore
        }
    }

    function headers() {
        const h = {
            'Content-Type': 'application/json',
            'X-Requested-With': 'XMLHttpRequest'
        };
        const csrf = getCsrfToken();
        if (csrf) h['X-XSRF-TOKEN'] = csrf;
        return h;
    }

    function setAlert(type, msg) {
        const el = document.getElementById('alert');
        if (!el) return;
        el.className = 'alert ' + (type || '');
        el.textContent = msg;
        el.style.display = 'block';
    }

    function clearAlert() {
        const el = document.getElementById('alert');
        if (!el) return;
        el.className = 'alert';
        el.textContent = '';
        el.style.display = 'none';
    }

    function getTokenFromUrl() {
        const params = new URLSearchParams(window.location.search);
        return params.get('token');
    }

    function showConfirmMode() {
        const subtitle = document.getElementById('subtitle');
        if (subtitle) subtitle.textContent = 'Reset to default password';
        const requestPanel = document.getElementById('request-panel');
        const confirmPanel = document.getElementById('confirm-panel');
        if (requestPanel) requestPanel.style.display = 'none';
        if (confirmPanel) confirmPanel.style.display = 'block';
    }

    async function requestReset() {
        clearAlert();
        const identifier = document.getElementById('identifier')?.value?.trim();
        if (!identifier) {
            setAlert('error', 'Please enter your username or email.');
            return;
        }

        const btn = document.getElementById('requestBtn');
        if (btn) {
            btn.disabled = true;
            btn.textContent = 'Sending...';
        }

        try {
            const res = await fetch('auth/password-reset/request', {
                method: 'POST',
                credentials: 'include',
                headers: headers(),
                body: JSON.stringify({ identifier })
            });
            const data = await res.json().catch(() => ({}));
            if (!res.ok) {
                throw new Error(data.message || `HTTP ${res.status}`);
            }
            setAlert('success', data.message || 'If the account exists, a reset link has been sent.');
        } catch (e) {
            setAlert('error', 'Failed to request reset: ' + (e.message || e));
        } finally {
            if (btn) {
                btn.disabled = false;
                btn.textContent = 'Send Reset Link';
            }
        }
    }

    async function confirmReset(token) {
        clearAlert();
        
        if (!token || token.trim().length === 0) {
            setAlert('error', 'Invalid reset token. Please request a new reset link.');
            return;
        }

        const btn = document.getElementById('confirmBtn');
        if (btn) {
            btn.disabled = true;
            btn.textContent = 'Resetting...';
        }

        try {
            const res = await fetch('auth/password-reset/confirm', {
                method: 'POST',
                credentials: 'include',
                headers: headers(),
                body: JSON.stringify({ token })
            });
            const data = await res.json().catch(() => ({}));
            if (!res.ok) {
                throw new Error(data.message || `HTTP ${res.status}`);
            }
            setAlert('success', data.message || 'Password reset successful. Your password has been reset to the default password. Please check your email for details. Redirecting to login...');
            setTimeout(() => {
                window.location.href = 'login.html';
            }, 3000);
        } catch (e) {
            setAlert('error', 'Failed to reset password: ' + (e.message || e));
        } finally {
            if (btn) {
                btn.disabled = false;
                btn.textContent = 'Reset Password to Default';
            }
        }
    }

    document.addEventListener('DOMContentLoaded', async function () {
        await ensureCsrfCookie();
        const token = getTokenFromUrl();
        if (token) showConfirmMode();

        document.getElementById('requestBtn')?.addEventListener('click', requestReset);
        document.getElementById('confirmBtn')?.addEventListener('click', () => confirmReset(token));
    });
})();


