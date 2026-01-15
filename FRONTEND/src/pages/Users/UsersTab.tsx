import { useState } from "react";
import {
    Box,
    Paper,
    Table,
    TableBody,
    TableCell,
    TableContainer,
    TableHead,
    TableRow,
    Button,
    Chip,
    IconButton,
    Dialog,
    DialogTitle,
    DialogContent,
    DialogActions,
    TextField,
    FormControl,
    InputLabel,
    Select,
    MenuItem,
    Switch,
    FormControlLabel,
    Typography,
    Alert,
    Tooltip,
} from "@mui/material";
import {
    Add as AddIcon,
    Edit as EditIcon,
    Delete as DeleteIcon,
    PersonOff as DisableIcon,
    PersonAdd as EnableIcon,
} from "@mui/icons-material";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { User, Role, Psp } from "../../types/userManagement";

export default function UsersTab() {
    const queryClient = useQueryClient();
    const [openDialog, setOpenDialog] = useState(false);
    const [editingUser, setEditingUser] = useState<User | null>(null);
    const [formData, setFormData] = useState({
        username: "",
        email: "",
        firstName: "",
        lastName: "",
        password: "",
        roleId: "",
        pspId: "",
        enabled: true,
    });

    // Fetch users
    const { data: users, isLoading } = useQuery<User[]>({
        queryKey: ["users"],
        queryFn: async () => {
            const response = await fetch("/api/v1/users");
            if (!response.ok) throw new Error("Failed to fetch users");
            return response.json();
        },
    });

    // Fetch roles for dropdown
    const { data: roles } = useQuery<Role[]>({
        queryKey: ["roles"],
        queryFn: async () => {
            const response = await fetch("/api/v1/roles");
            if (!response.ok) throw new Error("Failed to fetch roles");
            return response.json();
        },
    });

    // Fetch PSPs for dropdown
    const { data: psps } = useQuery<Psp[]>({
        queryKey: ["psps"],
        queryFn: async () => {
            const response = await fetch("/api/v1/psp");
            if (!response.ok) throw new Error("Failed to fetch PSPs");
            return response.json();
        },
    });

    // Create/Update user mutation
    const saveUserMutation = useMutation({
        mutationFn: async (userData: any) => {
            const url = editingUser ? `/api/v1/users/${editingUser.id}` : "/api/v1/users";
            const method = editingUser ? "PUT" : "POST";
            const response = await fetch(url, {
                method,
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify(userData),
            });
            if (!response.ok) throw new Error("Failed to save user");
            return response.json();
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ["users"] });
            handleCloseDialog();
        },
    });

    // Delete user mutation
    const deleteUserMutation = useMutation({
        mutationFn: async (userId: number) => {
            const response = await fetch(`/api/v1/users/${userId}`, { method: "DELETE" });
            if (!response.ok) throw new Error("Failed to delete user");
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ["users"] });
        },
    });

    // Toggle user enabled status
    const toggleUserMutation = useMutation({
        mutationFn: async ({ userId, enabled }: { userId: number; enabled: boolean }) => {
            const response = await fetch(`/api/v1/users/${userId}/toggle`, {
                method: "PATCH",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ enabled }),
            });
            if (!response.ok) throw new Error("Failed to toggle user status");
            return response.json();
        },
        onSuccess: () => {
            queryClient.invalidateQueries({ queryKey: ["users"] });
        },
    });

    const handleOpenDialog = (user?: User) => {
        if (user) {
            setEditingUser(user);
            setFormData({
                username: user.username,
                email: user.email,
                firstName: user.firstName,
                lastName: user.lastName,
                password: "",
                roleId: user.role.id.toString(),
                pspId: user.psp?.id.toString() || "",
                enabled: user.enabled,
            });
        } else {
            setEditingUser(null);
            setFormData({
                username: "",
                email: "",
                firstName: "",
                lastName: "",
                password: "",
                roleId: "",
                pspId: "",
                enabled: true,
            });
        }
        setOpenDialog(true);
    };

    const handleCloseDialog = () => {
        setOpenDialog(false);
        setEditingUser(null);
    };

    const handleSave = () => {
        const userData: any = {
            username: formData.username,
            email: formData.email,
            firstName: formData.firstName,
            lastName: formData.lastName,
            roleId: parseInt(formData.roleId),
            pspId: formData.pspId ? parseInt(formData.pspId) : null,
            enabled: formData.enabled,
        };

        if (!editingUser || formData.password) {
            userData.password = formData.password;
        }

        saveUserMutation.mutate(userData);
    };

    const handleDelete = (userId: number) => {
        if (window.confirm("Are you sure you want to delete this user?")) {
            deleteUserMutation.mutate(userId);
        }
    };

    const handleToggleEnabled = (userId: number, currentStatus: boolean) => {
        toggleUserMutation.mutate({ userId, enabled: !currentStatus });
    };

    return (
        <Box>
            <Box sx={{ display: "flex", justifyContent: "flex-end", mb: 3 }}>
                <Tooltip title="Create a new user account with specified role and permissions" arrow>
                    <Button
                        variant="contained"
                        startIcon={<AddIcon />}
                        onClick={() => handleOpenDialog()}
                        sx={{ backgroundColor: "#8B4049", "&:hover": { backgroundColor: "#6B3037" } }}
                    >
                        Add User
                    </Button>
                </Tooltip>
            </Box>

            <TableContainer component={Paper} sx={{ backgroundColor: "background.paper", border: "1px solid rgba(0,0,0,0.1)" }}>
                <Table>
                    <TableHead>
                        <TableRow>
                            <TableCell sx={{ color: "text.secondary", fontWeight: 600 }}>Username</TableCell>
                            <TableCell sx={{ color: "text.secondary", fontWeight: 600 }}>Name</TableCell>
                            <TableCell sx={{ color: "text.secondary", fontWeight: 600 }}>Email</TableCell>
                            <TableCell sx={{ color: "text.secondary", fontWeight: 600 }}>Role</TableCell>
                            <TableCell sx={{ color: "text.secondary", fontWeight: 600 }}>PSP</TableCell>
                            <TableCell sx={{ color: "text.secondary", fontWeight: 600 }}>Status</TableCell>
                            <TableCell sx={{ color: "text.secondary", fontWeight: 600 }}>Created</TableCell>
                            <TableCell sx={{ color: "text.secondary", fontWeight: 600 }}>Actions</TableCell>
                        </TableRow>
                    </TableHead>
                    <TableBody>
                        {isLoading ? (
                            <TableRow>
                                <TableCell colSpan={8} align="center" sx={{ py: 4, color: "text.secondary" }}>
                                    Loading users...
                                </TableCell>
                            </TableRow>
                        ) : users && users.length > 0 ? (
                            users.map((user) => (
                                <TableRow key={user.id} hover>
                                    <TableCell sx={{ color: "text.primary", fontWeight: 500 }}>{user.username}</TableCell>
                                    <TableCell sx={{ color: "text.primary" }}>
                                        {user.firstName} {user.lastName}
                                    </TableCell>
                                    <TableCell sx={{ color: "text.primary" }}>{user.email}</TableCell>
                                    <TableCell>
                                        <Chip
                                            label={user.role.name}
                                            size="small"
                                            sx={{
                                                backgroundColor: "#8B404920",
                                                color: "#8B4049",
                                                border: "1px solid #8B4049",
                                            }}
                                        />
                                    </TableCell>
                                    <TableCell sx={{ color: "text.primary" }}>{user.psp?.name || "System"}</TableCell>
                                    <TableCell>
                                        <Chip
                                            label={user.enabled ? "Active" : "Disabled"}
                                            size="small"
                                            sx={{
                                                backgroundColor: user.enabled ? "#2ecc7120" : "#95a5a620",
                                                color: user.enabled ? "#2ecc71" : "#95a5a6",
                                                border: `1px solid ${user.enabled ? "#2ecc71" : "#95a5a6"}`,
                                            }}
                                        />
                                    </TableCell>
                                    <TableCell sx={{ color: "text.secondary", fontSize: "0.875rem" }}>
                                        {new Date(user.createdAt).toLocaleDateString()}
                                    </TableCell>
                                    <TableCell>
                                        <Box sx={{ display: "flex", gap: 0.5 }}>
                                            <Tooltip title="Edit user details and permissions" arrow>
                                                <IconButton size="small" onClick={() => handleOpenDialog(user)} sx={{ color: "#8B4049" }}>
                                                    <EditIcon fontSize="small" />
                                                </IconButton>
                                            </Tooltip>
                                            <Tooltip title={user.enabled ? "Disable this user account" : "Enable this user account"} arrow>
                                                <IconButton
                                                    size="small"
                                                    onClick={() => handleToggleEnabled(user.id, user.enabled)}
                                                    sx={{ color: user.enabled ? "#f39c12" : "#2ecc71" }}
                                                >
                                                    {user.enabled ? <DisableIcon fontSize="small" /> : <EnableIcon fontSize="small" />}
                                                </IconButton>
                                            </Tooltip>
                                            <Tooltip title="Permanently delete this user account" arrow>
                                                <IconButton size="small" onClick={() => handleDelete(user.id)} sx={{ color: "#e74c3c" }}>
                                                    <DeleteIcon fontSize="small" />
                                                </IconButton>
                                            </Tooltip>
                                        </Box>
                                    </TableCell>
                                </TableRow>
                            ))
                        ) : (
                            <TableRow>
                                <TableCell colSpan={8} align="center" sx={{ py: 4, color: "text.secondary" }}>
                                    No users found
                                </TableCell>
                            </TableRow>
                        )}
                    </TableBody>
                </Table>
            </TableContainer>

            {/* Create/Edit User Dialog */}
            <Dialog open={openDialog} onClose={handleCloseDialog} maxWidth="sm" fullWidth>
                <DialogTitle>{editingUser ? "Edit User" : "Create New User"}</DialogTitle>
                <DialogContent>
                    <Box sx={{ display: "flex", flexDirection: "column", gap: 2, pt: 2 }}>
                        {saveUserMutation.isError && (
                            <Alert severity="error">Failed to save user. Please try again.</Alert>
                        )}

                        <Tooltip title="Enter a unique username for login. Cannot be changed after creation." arrow placement="top">
                            <TextField
                                label="Username"
                                value={formData.username}
                                onChange={(e) => setFormData({ ...formData, username: e.target.value })}
                                fullWidth
                                required
                                disabled={!!editingUser}
                            />
                        </Tooltip>

                        <Box sx={{ display: "flex", gap: 2 }}>
                            <Tooltip title="Enter the user's first name" arrow placement="top">
                                <TextField
                                    label="First Name"
                                    value={formData.firstName}
                                    onChange={(e) => setFormData({ ...formData, firstName: e.target.value })}
                                    fullWidth
                                    required
                                />
                            </Tooltip>
                            <Tooltip title="Enter the user's last name" arrow placement="top">
                                <TextField
                                    label="Last Name"
                                    value={formData.lastName}
                                    onChange={(e) => setFormData({ ...formData, lastName: e.target.value })}
                                    fullWidth
                                    required
                                />
                            </Tooltip>
                        </Box>

                        <Tooltip title="Enter a valid email address for notifications and account recovery" arrow placement="top">
                            <TextField
                                label="Email"
                                type="email"
                                value={formData.email}
                                onChange={(e) => setFormData({ ...formData, email: e.target.value })}
                                fullWidth
                                required
                            />
                        </Tooltip>

                        <Tooltip title={editingUser ? "Enter a new password to change it, or leave blank to keep the current password" : "Enter a secure password for the user account"} arrow placement="top">
                            <TextField
                                label={editingUser ? "New Password (leave blank to keep current)" : "Password"}
                                type="password"
                                value={formData.password}
                                onChange={(e) => setFormData({ ...formData, password: e.target.value })}
                                fullWidth
                                required={!editingUser}
                            />
                        </Tooltip>

                        <Tooltip title="Select the role that determines user permissions and access levels" arrow placement="top">
                            <FormControl fullWidth required>
                                <InputLabel>Role</InputLabel>
                                <Select
                                    value={formData.roleId}
                                    onChange={(e) => setFormData({ ...formData, roleId: e.target.value })}
                                    label="Role"
                                >
                                    {roles?.map((role) => (
                                        <MenuItem key={role.id} value={role.id.toString()}>
                                            {role.name} {role.psp ? `(${role.psp.name})` : "(System)"}
                                        </MenuItem>
                                    ))}
                                </Select>
                            </FormControl>
                        </Tooltip>

                        <Tooltip title="Optionally assign this user to a specific Payment Service Provider (PSP). Leave empty for system-wide access." arrow placement="top">
                            <FormControl fullWidth>
                                <InputLabel>PSP (Optional)</InputLabel>
                                <Select
                                    value={formData.pspId}
                                    onChange={(e) => setFormData({ ...formData, pspId: e.target.value })}
                                    label="PSP (Optional)"
                                >
                                    <MenuItem value="">None (System User)</MenuItem>
                                    {psps?.map((psp) => (
                                        <MenuItem key={psp.id} value={psp.id.toString()}>
                                            {psp.name}
                                        </MenuItem>
                                    ))}
                                </Select>
                            </FormControl>
                        </Tooltip>

                        <Tooltip title="Enable or disable this user account. Disabled users cannot log in." arrow placement="top">
                            <FormControlLabel
                                control={
                                    <Switch
                                        checked={formData.enabled}
                                        onChange={(e) => setFormData({ ...formData, enabled: e.target.checked })}
                                    />
                                }
                                label="Enabled"
                            />
                        </Tooltip>
                    </Box>
                </DialogContent>
                <DialogActions>
                    <Tooltip title="Cancel and discard all changes" arrow>
                        <Button onClick={handleCloseDialog}>Cancel</Button>
                    </Tooltip>
                    <Tooltip title="Save user information and apply changes" arrow>
                        <span>
                            <Button
                                onClick={handleSave}
                                variant="contained"
                                disabled={saveUserMutation.isPending}
                                sx={{ backgroundColor: "#8B4049", "&:hover": { backgroundColor: "#6B3037" } }}
                            >
                                {saveUserMutation.isPending ? "Saving..." : "Save"}
                            </Button>
                        </span>
                    </Tooltip>
                </DialogActions>
            </Dialog>
        </Box>
    );
}

