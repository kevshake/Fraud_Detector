document.addEventListener('DOMContentLoaded', function () {
    // Navigation
    const navItems = document.querySelectorAll('.nav-item');
    const dashboardView = document.getElementById('dashboard-view');
    const userView = document.getElementById('user-management-view');
    const roleView = document.getElementById('role-management-view');

    // Simple routing logic
    navItems.forEach(item => {
        item.addEventListener('click', (e) => {
            // e.preventDefault();
            const text = item.querySelector('span').innerText;

            if (text === 'Dashboard') {
                showView('dashboard');
            } else if (text === 'User Management') {
                showView('users');
                fetchUsers();
                fetchRolesForSelect();
                fetchPspsForSelect(); // Assuming this endpoint exists or implemented
            } else if (text === 'Settings') {
                // ... settings view
            }

            // Highlight active nav
            navItems.forEach(nav => nav.classList.remove('active'));
            item.classList.add('active');
        });
    });

    // We can also add a listener specifically for the "Administration" submenu items if they existed, 
    // but in index.html "User Management" is a top level item now (or close to it).
    // Specifically, current index.html has User Management as a sibling to Settings.

    // Also handle "Administration -> Roles" if user wants separate Role management view?
    // In index.html, we only have "User Management".
    // I added "Role Management" view in HTML but no nav link was created in index.html explicitly for "Roles".
    // I should probably add a "Roles" link or sub-link.
    // For now, let's assume "User Management" page might have tabs or we just added "Role Management" view but no way to get there.
    // Wait, I should add a "App Management" or similar nav item, or just put Roles under User Management?
    // I'll stick to what I have: I need a trigger for Role Management.
    // I'll add a "Roles" button in the User Management view or just add a Nav Item dynamically if missing.
    // Or simpler: I will hijack the "Settings" for now or just add a fake link via JS to the sidebar if I can't edit HTML easily again.
    // Actually, I can just interpret "User Management" as "Identity Management" and show two buttons there?
    // No, better to add a proper Nav Item. But I don't want to edit HTML again just for one line.
    // Users usually expect "Roles" near "Users".
    // I'll add "Roles" to the sidebar using JS for now.

    const sidebarNav = document.querySelector('.sidebar-nav');
    // Find User Management link
    const userLink = Array.from(document.querySelectorAll('.nav-item')).find(el => el.innerText.includes('User Management'));
    if (userLink) {
        const roleLink = document.createElement('a');
        roleLink.href = "#";
        roleLink.className = "nav-item";
        roleLink.innerHTML = '<i class="fas fa-user-tag"></i><span>Role Management</span>';
        roleLink.onclick = (e) => {
            e.preventDefault();
            showView('roles');
            fetchRoles();
            fetchPspsForSelect();
            fetchPermissions();

            document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
            roleLink.classList.add('active');
        };
        // Insert after User Management
        userLink.parentNode.insertBefore(roleLink, userLink.nextSibling);
    }

    function showView(viewName) {
        dashboardView.style.display = 'none';
        userView.style.display = 'none';
        roleView.style.display = 'none';

        if (viewName === 'dashboard') dashboardView.style.display = 'block';
        if (viewName === 'users') userView.style.display = 'block';
        if (viewName === 'roles') roleView.style.display = 'block';
    }

    // --- User Management ---
    window.fetchUsers = function () {
        fetch('/api/v1/users')
            .then(res => res.json())
            .then(users => {
                const tbody = document.querySelector('#users-table tbody');
                tbody.innerHTML = '';
                users.forEach(user => {
                    const tr = document.createElement('tr');
                    tr.innerHTML = `
                        <td>${user.userId || user.id}</td>
                        <td>${user.username}</td>
                        <td>${user.email}</td>
                        <td><span class="badge badge-info">${user.role ? user.role.name : 'N/A'}</span></td>
                        <td>${user.psp ? user.psp.tradingName : 'System'}</td>
                        <td><span class="status-badge ${user.enabled ? 'resolved' : 'escalated'}">${user.enabled ? 'Active' : 'Disabled'}</span></td>
                        <td>
                            <button class="action-btn"><i class="fas fa-edit"></i></button>
                            <button class="action-btn"><i class="fas fa-trash"></i></button>
                        </td>
                    `;
                    tbody.appendChild(tr);
                });
            })
            .catch(err => console.error('Error fetching users:', err));
    };

    window.openAddUserModal = function () {
        document.getElementById('addUserModal').style.display = 'block';
    }

    window.closeAddUserModal = function () {
        document.getElementById('addUserModal').style.display = 'none';
    }

    document.getElementById('addUserForm').addEventListener('submit', function (e) {
        e.preventDefault();
        const formData = new FormData(e.target);
        const data = Object.fromEntries(formData.entries());

        fetch('/api/v1/users', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        })
            .then(res => {
                if (res.ok) {
                    closeAddUserModal();
                    fetchUsers();
                    e.target.reset();
                } else {
                    alert('Failed to create user');
                }
            });
    });

    // --- Role Management ---
    window.fetchRoles = function () {
        fetch('/api/v1/roles')
            .then(res => res.json())
            .then(roles => {
                const tbody = document.querySelector('#roles-table tbody');
                tbody.innerHTML = '';
                roles.forEach(role => {
                    const tr = document.createElement('tr');
                    tr.innerHTML = `
                        <td>${role.name}</td>
                        <td>${role.description || ''}</td>
                        <td>${role.psp ? role.psp.tradingName : 'Global (System)'}</td>
                        <td>${role.permissions ? role.permissions.length + ' permissions' : '0'}</td>
                        <td>
                            <button class="action-btn"><i class="fas fa-edit"></i></button>
                        </td>
                    `;
                    tbody.appendChild(tr);
                });
            });
    };

    window.openAddRoleModal = function () {
        document.getElementById('addRoleModal').style.display = 'block';
    }
    window.closeAddRoleModal = function () {
        document.getElementById('addRoleModal').style.display = 'none';
    }

    document.getElementById('addRoleForm').addEventListener('submit', function (e) {
        e.preventDefault();
        const formData = new FormData(e.target);
        const data = Object.fromEntries(formData.entries());

        // Handle permissions checkbox array
        const permissions = [];
        document.querySelectorAll('input[name="permissions"]:checked').forEach(cb => {
            permissions.push(cb.value);
        });
        data.permissions = permissions;

        fetch('/api/v1/roles', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        })
            .then(res => {
                if (res.ok) {
                    closeAddRoleModal();
                    fetchRoles();
                    e.target.reset();
                } else {
                    alert('Failed to create role');
                }
            });
    });

    // Helpers
    function fetchRolesForSelect() {
        fetch('/api/v1/roles')
            .then(res => res.json())
            .then(roles => {
                const select = document.getElementById('userRoleSelect');
                select.innerHTML = '';
                roles.forEach(role => {
                    const opt = document.createElement('option');
                    opt.value = role.id;
                    opt.innerText = role.name;
                    select.appendChild(opt);
                });
            });
    }

    function fetchPspsForSelect() {
        // Need an endpoint for this. Assuming /api/v1/admin/psp exists and returns list
        fetch('/api/v1/admin/psp')
            .then(res => res.json())
            .then(psps => {
                const userSelect = document.getElementById('userPspSelect');
                const roleSelect = document.getElementById('rolePspSelect');

                // Clear existing (keep first option System)
                while (userSelect.options.length > 1) userSelect.remove(1);
                while (roleSelect.options.length > 1) roleSelect.remove(1);

                psps.forEach(psp => {
                    const opt1 = document.createElement('option');
                    opt1.value = psp.id || psp.pspId;
                    opt1.innerText = psp.tradingName || psp.legalName;
                    userSelect.appendChild(opt1);

                    const opt2 = opt1.cloneNode(true);
                    roleSelect.appendChild(opt2);
                });
            })
            .catch(err => console.log('Could not fetch PSPs, maybe not admin'));
    }

    function fetchPermissions() {
        fetch('/api/v1/auth/permissions')
            .then(res => res.json())
            .then(perms => {
                const container = document.getElementById('permissionsCheckboxes');
                container.innerHTML = '';
                perms.forEach(perm => {
                    const div = document.createElement('div');
                    div.className = 'checkbox-item';
                    div.innerHTML = `
                        <input type="checkbox" name="permissions" value="${perm}" id="perm_${perm}">
                        <label for="perm_${perm}">${perm}</label>
                    `;
                    container.appendChild(div);
                });
            });
    }

    // Initial load
    if (window.location.hash === '#users') {
        showView('users');
        fetchUsers();
    }
});
