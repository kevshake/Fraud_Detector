import { Box, Typography, Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Paper, Chip, Grid, Card, CardContent } from "@mui/material";
import {
  useMonitoringTransactions,
  useMonitoringDashboardStats,
  useMonitoringRecentActivity,
} from "../../features/api/queries";

export default function TransactionMonitoringLive() {
  const { data: transactions, isLoading: transactionsLoading } = useMonitoringTransactions();
  const { data: stats, isLoading: statsLoading } = useMonitoringDashboardStats();
  const { data: recentActivity, isLoading: activityLoading } = useMonitoringRecentActivity();

  return (
    <Box>
      <Typography variant="h6" sx={{ color: "text.primary", mb: 3 }}>
        Live Transaction Monitoring
      </Typography>

      {stats && !statsLoading && (
        <Grid container spacing={2} sx={{ mb: 3 }}>
          {Object.entries(stats).map(([key, value]) => (
            <Grid item xs={12} sm={6} md={3} key={key}>
              <Card sx={{ backgroundColor: "background.paper", border: "1px solid rgba(0,0,0,0.1)" }}>
                <CardContent>
                  <Typography variant="body2" sx={{ color: "text.secondary", mb: 1, textTransform: "capitalize" }}>
                    {key.replace(/([A-Z])/g, " $1").trim()}
                  </Typography>
                  <Typography variant="h5" sx={{ color: "text.primary" }}>
                    {typeof value === "number" ? value.toLocaleString() : String(value)}
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
          ))}
        </Grid>
      )}

      <Grid container spacing={3}>
        <Grid item xs={12} md={8}>
          <Paper sx={{ backgroundColor: "background.paper", border: "1px solid rgba(0,0,0,0.1)" }}>
            <Box sx={{ p: 2, borderBottom: "1px solid rgba(0,0,0,0.1)" }}>
              <Typography variant="h6" sx={{ color: "text.primary" }}>
                Monitored Transactions
              </Typography>
            </Box>
            <TableContainer>
              <Table>
                <TableHead>
                  <TableRow>
                    <TableCell sx={{ color: "text.secondary" }}>ID</TableCell>
                    <TableCell sx={{ color: "text.secondary" }}>Merchant</TableCell>
                    <TableCell sx={{ color: "text.secondary" }}>Amount</TableCell>
                    <TableCell sx={{ color: "text.secondary" }}>Decision</TableCell>
                    <TableCell sx={{ color: "text.secondary" }}>Timestamp</TableCell>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {transactionsLoading ? (
                    <TableRow>
                      <TableCell colSpan={5} align="center" sx={{ color: "text.disabled", py: 4 }}>
                        Loading transactions...
                      </TableCell>
                    </TableRow>
                  ) : transactions && Array.isArray(transactions) && transactions.length > 0 ? (
                    transactions.slice(0, 20).map((txn: any, idx: number) => (
                      <TableRow key={idx} hover>
                        <TableCell sx={{ color: "text.primary" }}>#{txn.id || idx}</TableCell>
                        <TableCell sx={{ color: "text.primary" }}>{txn.merchantId || "N/A"}</TableCell>
                        <TableCell sx={{ color: "text.primary" }}>
                          {txn.amountCents ? `$${(txn.amountCents / 100).toFixed(2)}` : "N/A"}
                        </TableCell>
                        <TableCell>
                          <Chip
                            label={txn.decision || "ALLOW"}
                            size="small"
                            sx={{
                              backgroundColor: txn.decision === "BLOCK" ? "#e74c3c20" : "#2ecc7120",
                              color: txn.decision === "BLOCK" ? "#e74c3c" : "#2ecc71",
                              border: `1px solid ${txn.decision === "BLOCK" ? "#e74c3c" : "#2ecc71"}`,
                            }}
                          />
                        </TableCell>
                        <TableCell sx={{ color: "text.secondary" }}>
                          {txn.txnTs ? new Date(txn.txnTs).toLocaleString() : "N/A"}
                        </TableCell>
                      </TableRow>
                    ))
                  ) : (
                    <TableRow>
                      <TableCell colSpan={5} align="center" sx={{ color: "text.disabled", py: 4 }}>
                        No transactions found
                      </TableCell>
                    </TableRow>
                  )}
                </TableBody>
              </Table>
            </TableContainer>
          </Paper>
        </Grid>
        <Grid item xs={12} md={4}>
          <Paper sx={{ p: 2, backgroundColor: "background.paper", border: "1px solid rgba(0,0,0,0.1)" }}>
            <Typography variant="h6" sx={{ color: "text.primary", mb: 2 }}>
              Recent Activity
            </Typography>
            {activityLoading ? (
              <Typography sx={{ color: "text.disabled" }}>Loading activity...</Typography>
            ) : recentActivity && Array.isArray(recentActivity) && recentActivity.length > 0 ? (
              <Box>
                {recentActivity.slice(0, 10).map((activity: any, idx: number) => (
                  <Box
                    key={idx}
                    sx={{
                      p: 1.5,
                      mb: 1,
                      backgroundColor: "background.paper",
                      borderRadius: 1,
                      border: "1px solid rgba(0,0,0,0.1)",
                    }}
                  >
                    <Typography variant="body2" sx={{ color: "text.primary" }}>
                      {activity.description || activity.action || "Activity"}
                    </Typography>
                    <Typography variant="caption" sx={{ color: "text.disabled" }}>
                      {activity.timestamp ? new Date(activity.timestamp).toLocaleString() : ""}
                    </Typography>
                  </Box>
                ))}
              </Box>
            ) : (
              <Typography sx={{ color: "text.disabled" }}>No recent activity</Typography>
            )}
          </Paper>
        </Grid>
      </Grid>
    </Box>
  );
}

