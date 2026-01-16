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
  Typography,
  TextField,
  MenuItem,
  Grid,
  Button,
  Chip,
  Tooltip,
  TablePagination,
} from "@mui/material";
import { Search as SearchIcon, FilterList as FilterIcon } from "@mui/icons-material";
import { useAuditLogs, useAllPsps } from "../../features/api/queries";
import { useAuth } from "../../contexts/AuthContext";

export default function AuditLogsPage() {
  const { user } = useAuth();
  const isSuperAdmin = user?.pspId === 0;

  // Default to last 30 days
  const defaultStartDate = new Date();
  defaultStartDate.setDate(defaultStartDate.getDate() - 30);

  const [page, setPage] = useState({ index: 0, size: 25 });
  const [filters, setFilters] = useState({
    startDate: defaultStartDate.toISOString().split("T")[0],
    endDate: new Date().toISOString().split("T")[0],
    actionType: "",
    username: "",
    pspId: "",
  });

  const { data: logs, isLoading } = useAuditLogs({
    page: page.index,
    size: page.size,
    start: filters.startDate ? `${filters.startDate}T00:00:00` : undefined,
    end: filters.endDate ? `${filters.endDate}T23:59:59` : undefined,
    actionType: filters.actionType || undefined,
    username: filters.username || undefined,
    pspId: filters.pspId ? parseInt(filters.pspId) : undefined,
  });

  const { data: psps } = useAllPsps();

  const handleFilterChange = (field: string, value: any) => {
    setFilters((prev) => ({ ...prev, [field]: value }));
  };

  const actionTypes = ["LOGIN", "LOGOUT", "CREATE", "UPDATE", "DELETE", "VIEW", "EXPORT", "OVERRIDE"];

  return (
    <Box>
      <Typography variant="h4" sx={{ color: "text.primary", mb: 3, fontWeight: 600 }}>
        Audit Logs
      </Typography>

      {/* Filters */}
      <Paper sx={{ p: 2, mb: 3, backgroundColor: "background.paper", border: "1px solid rgba(0,0,0,0.1)" }}>
        <Grid container spacing={2} alignItems="center">
          <Grid item xs={12} md={isSuperAdmin ? 2 : 3}>
            <Tooltip title="Filter by start date" arrow>
              <TextField
                label="Start Date"
                type="date"
                value={filters.startDate}
                onChange={(e) => handleFilterChange("startDate", e.target.value)}
                fullWidth
                size="small"
                InputLabelProps={{ shrink: true }}
              />
            </Tooltip>
          </Grid>
          <Grid item xs={12} md={isSuperAdmin ? 2 : 3}>
            <Tooltip title="Filter by end date" arrow>
              <TextField
                label="End Date"
                type="date"
                value={filters.endDate}
                onChange={(e) => handleFilterChange("endDate", e.target.value)}
                fullWidth
                size="small"
                InputLabelProps={{ shrink: true }}
              />
            </Tooltip>
          </Grid>
          <Grid item xs={12} md={isSuperAdmin ? 2 : 3}>
            <Tooltip title="Filter by action type" arrow>
              <TextField
                select
                label="Action"
                value={filters.actionType}
                onChange={(e) => handleFilterChange("actionType", e.target.value)}
                fullWidth
                size="small"
              >
                <MenuItem value="">All Actions</MenuItem>
                {actionTypes.map((action) => (
                  <MenuItem key={action} value={action}>
                    {action}
                  </MenuItem>
                ))}
              </TextField>
            </Tooltip>
          </Grid>
          <Grid item xs={12} md={isSuperAdmin ? 2 : 3}>
            <Tooltip title="Search by username" arrow>
              <TextField
                label="Username"
                value={filters.username}
                onChange={(e) => handleFilterChange("username", e.target.value)}
                fullWidth
                size="small"
                InputProps={{
                  endAdornment: <SearchIcon color="action" fontSize="small" />,
                }}
              />
            </Tooltip>
          </Grid>

          {isSuperAdmin && (
            <Grid item xs={12} md={2}>
              <Tooltip title="Filter by Payment Service Provider" arrow>
                <TextField
                  select
                  label="PSP"
                  value={filters.pspId}
                  onChange={(e) => handleFilterChange("pspId", e.target.value)}
                  fullWidth
                  size="small"
                >
                  <MenuItem value="">All PSPs</MenuItem>
                  {psps?.map((psp: any) => (
                    <MenuItem key={psp.id} value={psp.id.toString()}>
                      {psp.name}
                    </MenuItem>
                  ))}
                </TextField>
              </Tooltip>
            </Grid>
          )}

          <Grid item xs={12} md={2}>
            <Button
              variant="outlined"
              startIcon={<FilterIcon />}
              onClick={() => setFilters({
                startDate: defaultStartDate.toISOString().split("T")[0],
                endDate: new Date().toISOString().split("T")[0],
                actionType: "",
                username: "",
                pspId: "",
              })}
              fullWidth
            >
              Reset
            </Button>
          </Grid>
        </Grid>
      </Paper>

      <TableContainer component={Paper} sx={{ backgroundColor: "background.paper", border: "1px solid rgba(0,0,0,0.1)" }}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell sx={{ color: "text.secondary", fontWeight: 600 }}>Timestamp</TableCell>
              <TableCell sx={{ color: "text.secondary", fontWeight: 600 }}>PSP</TableCell>
              <TableCell sx={{ color: "text.secondary", fontWeight: 600 }}>User</TableCell>
              <TableCell sx={{ color: "text.secondary", fontWeight: 600 }}>Action</TableCell>
              <TableCell sx={{ color: "text.secondary", fontWeight: 600 }}>Entity Type</TableCell>
              <TableCell sx={{ color: "text.secondary", fontWeight: 600 }}>Entity ID</TableCell>
              <TableCell sx={{ color: "text.secondary", fontWeight: 600 }}>Details</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {isLoading ? (
              <TableRow>
                <TableCell colSpan={7} align="center" sx={{ color: "text.disabled", py: 4 }}>
                  Loading audit logs...
                </TableCell>
              </TableRow>
            ) : logs && logs.content && logs.content.length > 0 ? (
              logs.content.map((log) => (
                <TableRow key={log.id} hover>
                  <TableCell sx={{ color: "text.primary", fontSize: "0.875rem" }}>
                    {new Date(log.timestamp).toLocaleString()}
                  </TableCell>
                  <TableCell sx={{ color: "text.secondary", fontSize: "0.875rem" }}>
                    {log.pspId ? (log.pspId === 0 ? "System" : `PSP #${log.pspId}`) : "System"}
                  </TableCell>
                  <TableCell sx={{ color: "text.primary" }}>{log.username}</TableCell>
                  <TableCell>
                    <Chip
                      label={log.actionType}
                      size="small"
                      sx={{
                        backgroundColor:
                          log.actionType === 'DELETE' ? '#ffebee' :
                            log.actionType === 'CREATE' ? '#e8f5e9' :
                              log.actionType === 'UPDATE' ? '#e3f2fd' : '#f5f5f5',
                        color:
                          log.actionType === 'DELETE' ? '#c62828' :
                            log.actionType === 'CREATE' ? '#2e7d32' :
                              log.actionType === 'UPDATE' ? '#1565c0' : '#616161',
                        fontWeight: 500
                      }}
                    />
                  </TableCell>
                  <TableCell sx={{ color: "text.primary" }}>{log.entityType}</TableCell>
                  <TableCell sx={{ color: "text.primary" }}>{log.entityId}</TableCell>
                  <TableCell sx={{ color: "text.secondary" }}>
                    {log.reason || log.details || "N/A"}
                  </TableCell>
                </TableRow>
              ))
            ) : (
              <TableRow>
                <TableCell colSpan={7} align="center" sx={{ color: "text.disabled", py: 4 }}>
                  No audit logs found for the selected period
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
        <TablePagination
          rowsPerPageOptions={[10, 25, 50, 100]}
          component="div"
          count={logs?.totalElements || 0}
          rowsPerPage={page.size}
          page={page.index}
          onPageChange={(_, newPage) => setPage(prev => ({ ...prev, index: newPage }))}
          onRowsPerPageChange={(e) => setPage({ index: 0, size: parseInt(e.target.value, 10) })}
        />
      </TableContainer>
    </Box>
}
