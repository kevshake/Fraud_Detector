import {
  Box,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
  Chip,
  Button,
  Tooltip,
} from "@mui/material";
import { useAlerts } from "../../features/api/queries";
import type { Priority } from "../../types";

const priorityColors: Record<Priority, string> = {
  CRITICAL: "#e74c3c",
  HIGH: "#e67e22",
  MEDIUM: "#f39c12",
  LOW: "#95a5a6",
};

const statusColors: Record<string, string> = {
  OPEN: "#e74c3c",
  INVESTIGATING: "#f39c12",
  RESOLVED: "#2ecc71",
};

export default function AlertsPage() {
  const { data: alerts, isLoading, isError, error } = useAlerts();

  return (
    <Box>
      <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", mb: 3 }}>
        <Typography variant="h4" sx={{ color: "text.primary", fontWeight: 600 }}>
          Alerts
        </Typography>
        <Tooltip title="Perform bulk actions on selected alerts (e.g., mark as resolved, assign priority)" arrow>
          <Button variant="contained" sx={{ backgroundColor: "#a93226", "&:hover": { backgroundColor: "#922b21" } }}>
            Bulk Actions
          </Button>
        </Tooltip>
      </Box>

      <TableContainer component={Paper} sx={{ backgroundColor: "background.paper", border: "1px solid rgba(0,0,0,0.1)" }}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell sx={{ color: "text.secondary" }}>ID</TableCell>
              <TableCell sx={{ color: "text.secondary" }}>Type</TableCell>
              <TableCell sx={{ color: "text.secondary" }}>Priority</TableCell>
              <TableCell sx={{ color: "text.secondary" }}>Status</TableCell>
              <TableCell sx={{ color: "text.secondary" }}>Description</TableCell>
              <TableCell sx={{ color: "text.secondary" }}>Created</TableCell>
              <TableCell sx={{ color: "text.secondary" }}>Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {isLoading ? (
              <TableRow>
                <TableCell colSpan={7} align="center" sx={{ color: "text.disabled", py: 4 }}>
                  Loading alerts...
                </TableCell>
              </TableRow>
            ) : isError ? (
              <TableRow>
                <TableCell colSpan={7} align="center" sx={{ color: "#e74c3c", py: 4 }}>
                  Error loading alerts: {error instanceof Error ? error.message : "Unknown error"}
                </TableCell>
              </TableRow>
            ) : alerts && alerts.length > 0 ? (
              alerts.map((alert) => (
                <TableRow key={alert.id} hover>
                  <TableCell sx={{ color: "text.primary" }}>#{alert.id}</TableCell>
                  <TableCell sx={{ color: "text.primary" }}>{alert.alertType}</TableCell>
                  <TableCell>
                    <Chip
                      label={alert.priority}
                      size="small"
                      sx={{
                        backgroundColor: priorityColors[alert.priority] + "20",
                        color: priorityColors[alert.priority],
                        border: `1px solid ${priorityColors[alert.priority]}`,
                      }}
                    />
                  </TableCell>
                  <TableCell>
                    <Chip
                      label={alert.status}
                      size="small"
                      sx={{
                        backgroundColor: statusColors[alert.status] + "20",
                        color: statusColors[alert.status],
                        border: `1px solid ${statusColors[alert.status]}`,
                      }}
                    />
                  </TableCell>
                  <TableCell sx={{ color: "text.primary", maxWidth: 300 }}>
                    {alert.description}
                  </TableCell>
                  <TableCell sx={{ color: "text.secondary" }}>
                    {new Date(alert.createdAt).toLocaleDateString()}
                  </TableCell>
                  <TableCell>
                    <Tooltip title="View detailed information about this alert" arrow>
                      <Button size="small" sx={{ color: "#a93226" }}>
                        View
                      </Button>
                    </Tooltip>
                  </TableCell>
                </TableRow>
              ))
            ) : (
              <TableRow>
                <TableCell colSpan={7} align="center" sx={{ color: "text.disabled", py: 4 }}>
                  No alerts found
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>
    </Box>
  );
}

