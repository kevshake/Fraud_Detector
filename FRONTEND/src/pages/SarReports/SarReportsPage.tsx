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
  Chip,
  Button,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
} from "@mui/material";
import { useSarReports } from "../../features/api/queries";
import type { SarStatus } from "../../types";

const statusColors: Record<SarStatus, string> = {
  DRAFT: "#95a5a6",
  REVIEW: "#f39c12",
  APPROVED: "#2ecc71",
  FILED: "#3498db",
  REJECTED: "#e74c3c",
};

export default function SarReportsPage() {
  const [statusFilter, setStatusFilter] = useState<string>("");
  const { data: sars, isLoading } = useSarReports(statusFilter || undefined);

  return (
    <Box>
      <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", mb: 3 }}>
        <Typography variant="h4" sx={{ color: "text.primary", fontWeight: 600 }}>
          SAR Reports
        </Typography>
        <Box sx={{ display: "flex", gap: 2 }}>
          <FormControl size="small" sx={{ minWidth: 200 }}>
            <InputLabel sx={{ color: "text.secondary" }}>Filter by Status</InputLabel>
            <Select
              value={statusFilter}
              onChange={(e) => setStatusFilter(e.target.value)}
              label="Filter by Status"
              sx={{
                color: "text.primary",
                "& .MuiOutlinedInput-notchedOutline": {
                  borderColor: "rgba(255,255,255,0.3)",
                },
              }}
            >
              <MenuItem value="">All</MenuItem>
              <MenuItem value="DRAFT">Draft</MenuItem>
              <MenuItem value="REVIEW">Review</MenuItem>
              <MenuItem value="APPROVED">Approved</MenuItem>
              <MenuItem value="FILED">Filed</MenuItem>
              <MenuItem value="REJECTED">Rejected</MenuItem>
            </Select>
          </FormControl>
          <Button variant="contained" sx={{ backgroundColor: "#a93226", "&:hover": { backgroundColor: "#922b21" } }}>
            Create SAR
          </Button>
        </Box>
      </Box>

      <TableContainer component={Paper} sx={{ backgroundColor: "background.paper", border: "1px solid rgba(0,0,0,0.1)" }}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell sx={{ color: "text.secondary" }}>Reference</TableCell>
              <TableCell sx={{ color: "text.secondary" }}>Status</TableCell>
              <TableCell sx={{ color: "text.secondary" }}>Activity Type</TableCell>
              <TableCell sx={{ color: "text.secondary" }}>Jurisdiction</TableCell>
              <TableCell sx={{ color: "text.secondary" }}>Created By</TableCell>
              <TableCell sx={{ color: "text.secondary" }}>Created</TableCell>
              <TableCell sx={{ color: "text.secondary" }}>Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {isLoading ? (
              <TableRow>
                <TableCell colSpan={7} align="center" sx={{ color: "text.disabled", py: 4 }}>
                  Loading SAR reports...
                </TableCell>
              </TableRow>
            ) : sars && sars.length > 0 ? (
              sars.map((sar) => (
                <TableRow key={sar.id} hover>
                  <TableCell sx={{ color: "text.primary" }}>{sar.sarReference}</TableCell>
                  <TableCell>
                    <Chip
                      label={sar.status}
                      size="small"
                      sx={{
                        backgroundColor: statusColors[sar.status] + "20",
                        color: statusColors[sar.status],
                        border: `1px solid ${statusColors[sar.status]}`,
                      }}
                    />
                  </TableCell>
                  <TableCell sx={{ color: "text.primary" }}>
                    {sar.suspiciousActivityType}
                  </TableCell>
                  <TableCell sx={{ color: "text.primary" }}>{sar.jurisdiction}</TableCell>
                  <TableCell sx={{ color: "text.primary" }}>
                    {sar.createdBy.username}
                  </TableCell>
                  <TableCell sx={{ color: "text.secondary" }}>
                    {new Date(sar.createdAt).toLocaleDateString()}
                  </TableCell>
                  <TableCell>
                    <Button size="small" sx={{ color: "#a93226" }}>
                      View
                    </Button>
                  </TableCell>
                </TableRow>
              ))
            ) : (
              <TableRow>
                <TableCell colSpan={7} align="center" sx={{ color: "text.disabled", py: 4 }}>
                  No SAR reports found
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>
    </Box>
  );
}

