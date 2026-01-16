# Session Management Implementation

## ✅ Features Implemented

### 1. Backend Session Configuration
- **Session Timeout**: Configurable (default: 30 minutes / 1800 seconds)
- **Session Cookie**: Secure, HttpOnly configuration
- **Session Tracking**: Cookie-based session tracking
- **Session Invalidation**: Automatic on timeout or manual logout

### 2. Frontend Session Manager
- **Activity Tracking**: Monitors mouse, keyboard, scroll, touch events
- **Auto-Refresh**: Automatically refreshes session on user activity
- **Session Warning**: Shows warning modal 5 minutes before expiration
- **Session Expiration**: Handles expired sessions gracefully
- **Auto-Redirect**: Redirects to login on session expiration

### 3. Session Endpoints
- `GET /api/v1/auth/session/check` - Check session validity
- `POST /api/v1/auth/session/refresh` - Refresh/extend session
- `GET /api/v1/auth/session/info` - Get session information
- `POST /api/v1/auth/session/invalidate` - Invalidate session
- `GET /api/v1/auth/csrf` - Get CSRF token

## 🔧 Configuration

### Application Properties
```properties
# Session timeout in seconds (default: 1800 = 30 minutes)
server.servlet.session.timeout=1800

# Session cookie configuration
server.servlet.session.cookie.name=JSESSIONID
server.servlet.session.cookie.http-only=true
server.servlet.session.cookie.secure=false
server.servlet.session.cookie.max-age=1800
```

### Frontend Configuration
The session manager automatically:
- Detects session timeout from server response
- Adjusts warning time based on timeout
- Refreshes session every 5 minutes of activity
- Checks session validity every minute

## 📋 How It Works

### Session Lifecycle

1. **Login**: User logs in → Session created with timeout
2. **Activity**: User interacts with page → Last activity time updated
3. **Auto-Refresh**: Every 5 minutes of activity → Session refreshed on backend
4. **Warning**: 5 minutes before timeout → Warning modal shown
5. **Expiration**: Session timeout reached → User redirected to login

### Activity Tracking

The session manager tracks:
- Mouse movements and clicks
- Keyboard input
- Scroll events
- Touch events (mobile)
- Page visibility changes

### Session Refresh

- Automatic refresh happens every 5 minutes if user is active
- Manual refresh via "Extend Session" button in warning modal
- Session refresh updates last accessed time on server

## 🎯 User Experience

### Session Warning Modal
- Appears 5 minutes before session expiration
- Shows time remaining
- Options:
  - **Extend Session**: Refreshes session and continues
  - **Logout Now**: Immediately logs out

### Session Expiration
- Automatic redirect to login page
- Message: "Your session has expired"
- Preserves redirect path for post-login navigation

### Seamless Navigation
- Session persists across page navigation
- No re-authentication needed when switching menus
- Automatic session refresh on activity

## 🔒 Security Features

1. **HttpOnly Cookies**: Prevents XSS attacks
2. **Secure Cookies**: HTTPS-only in production
3. **CSRF Protection**: CSRF tokens for state-changing operations
4. **Session Invalidation**: Proper cleanup on logout/timeout
5. **Single Session**: Maximum 1 session per user (configurable)

## 📝 Usage

### For Users
- Simply use the application normally
- Session will auto-refresh on activity
- Warning will appear before expiration
- Click "Extend Session" to continue working

### For Developers
- Session timeout is configurable via `application.properties`
- Frontend automatically adapts to server timeout
- All API calls are intercepted for session checks
- Session state is checked on page load

## ⚙️ Customization

### Change Session Timeout
```properties
# In application.properties
server.servlet.session.timeout=3600  # 1 hour
```

### Change Warning Time
```javascript
// In session-manager.js
this.warningTime = 10 * 60 * 1000; // 10 minutes before timeout
```

### Change Activity Check Interval
```javascript
// In session-manager.js
this.activityCheckInterval = 60 * 1000; // Check every minute
```

## 🚀 Benefits

1. **No Repeated Logins**: Session persists across navigation
2. **Security**: Automatic timeout prevents unauthorized access
3. **User-Friendly**: Clear warnings and easy session extension
4. **Automatic**: No user intervention needed for normal use
5. **Configurable**: Easy to adjust timeout and behavior

