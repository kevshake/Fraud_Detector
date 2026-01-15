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
} from "@mui/material";
import { useMerchants } from "../../features/api/queries";

const riskColors: Record<string, string> = {
  LOW: "#2ecc71",
  MEDIUM: "#f39c12",
  HIGH: "#e74c3c",
};

export default function MerchantsPage() {
  const { data: merchants, isLoading } = useMerchants();

  return (
    <Box>
      <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", mb: 3 }}>
        <Typography variant="h4" sx={{ color: "text.primary", fontWeight: 600 }}>
          Merchants
        </Typography>
        <Button variant="contained" sx={{ backgroundColor: "#a93226", "&:hover": { backgroundColor: "#922b21" } }}>
          Add Merchant
        </Button>
      </Box>

      <TableContainer component={Paper} sx={{ backgroundColor: "background.paper", border: "1px solid rgba(0,0,0,0.1)" }}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell sx={{ color: "text.secondary" }}>Merchant ID</TableCell>
              <TableCell sx={{ color: "text.secondary" }}>Business Name</TableCell>
              <TableCell sx={{ color: "text.secondary" }}>MCC</TableCell>
              <TableCell sx={{ color: "text.secondary" }}>Risk Level</TableCell>
              <TableCell sx={{ color: "text.secondary" }}>KRS</TableCell>
              <TableCell sx={{ color: "text.secondary" }}>CRA</TableCell>
              <TableCell sx={{ color: "text.secondary" }}>KYC Status</TableCell>
              <TableCell sx={{ color: "text.secondary" }}>Contract Status</TableCell>
              <TableCell sx={{ color: "text.secondary" }}>Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {isLoading ? (
              <TableRow>
                <TableCell colSpan={7} align="center" sx={{ color: "text.disabled", py: 4 }}>
                  Loading merchants...
                </TableCell>
              </TableRow>
            ) : merchants && merchants.length > 0 ? (
              merchants.map((merchant) => (
                <TableRow key={merchant.id} hover>
                  <TableCell sx={{ color: "text.primary" }}>{merchant.merchantId}</TableCell>
                  <TableCell sx={{ color: "text.primary" }}>{merchant.businessName}</TableCell>
                  <TableCell sx={{ color: "text.primary" }}>{merchant.mcc || "N/A"}</TableCell>
                  <TableCell>
                    {merchant.riskLevel && (
                      <Chip
                        label={merchant.riskLevel}
                        size="small"
                        sx={{
                          backgroundColor: riskColors[merchant.riskLevel] + "20",
                          color: riskColors[merchant.riskLevel],
                          border: `1px solid ${riskColors[merchant.riskLevel]}`,
                        }}
                      />
                    )}
                  </TableCell>
                  <TableCell sx={{ color: "text.primary" }}>{merchant.krs?.toFixed(1) || "N/A"}</TableCell>
                  <TableCell sx={{ color: "text.primary" }}>{merchant.cra?.toFixed(1) || "N/A"}</TableCell>
                  <TableCell sx={{ color: "text.primary" }}>
                    {merchant.kycStatus || "N/A"}
                  </TableCell>
                  <TableCell sx={{ color: "text.primary" }}>
                    {merchant.contractStatus || "N/A"}
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
                  No merchants found
                </TableCell>
              </TableRow>
            )}
          </TableBody>
        </Table>
      </TableContainer>
    </Box>
  );
}

