import {
  Box,
  Paper,
  Typography,
  TextField,
  Button,
  Grid,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  Alert,
  CircularProgress,
  Tabs,
  Tab,
  Chip,
  Tooltip,
  Divider,
} from "@mui/material";
import { apiClient } from "../../lib/apiClient";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";
import { useState, useEffect } from "react";
import { BRAND_THEMES } from "../../config/themes";
import { useAuth } from "../../contexts/AuthContext";


interface Psp {
  id: number;
  code: string;
  name: string;
  status: string;
}

interface PspTheme {
  pspId: number;
  pspName: string;
  brandingTheme?: string;
  primaryColor?: string;
  secondaryColor?: string;
  accentColor?: string;
  logoUrl?: string;
  fontFamily?: string;
  fontSize?: string;
  buttonRadius?: string;
  buttonStyle?: string;
  navStyle?: string;
}

interface ThemePresets {
  [key: string]: {
    primaryColor: string;
    secondaryColor: string;
    accentColor: string;
  };
}

function TabPanel(props: { children?: React.ReactNode; index: number; value: number }) {
  const { children, value, index, ...other } = props;
  return (
    <div role="tabpanel" hidden={value !== index} {...other}>
      {value === index && <Box sx={{ pt: 3 }}>{children}</Box>}
    </div>
  );
}

export default function SettingsPage() {
  const queryClient = useQueryClient();
  const [tabValue, setTabValue] = useState(0);
  const [selectedPspId, setSelectedPspId] = useState<number | null>(null);
  const [themeData, setThemeData] = useState<PspTheme | null>(null);
  const [saving, setSaving] = useState(false);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Fetch all PSPs
  const { data: psps, isLoading: isLoadingPsps } = useQuery<Psp[]>({
    queryKey: ["settings", "psps"],
    queryFn: () => apiClient.get<Psp[]>("settings/psps"),
  });

  // Fetch theme presets
  const { data: presets } = useQuery<ThemePresets>({
    queryKey: ["settings", "themes", "presets"],
    queryFn: () => apiClient.get<ThemePresets>("settings/themes/presets"),
  });

  // Fetch PSP theme when PSP is selected
  const { data: currentTheme, isLoading: isLoadingTheme } = useQuery<PspTheme>({
    queryKey: ["settings", "psps", selectedPspId, "theme"],
    queryFn: () => apiClient.get<PspTheme>(`settings/psps/${selectedPspId}/theme`),
    enabled: selectedPspId !== null,
  });

  // Update theme data when current theme loads
  useEffect(() => {
    if (currentTheme) {
      setThemeData(currentTheme);
    }
  }, [currentTheme]);

  // Update theme mutation
  const updateThemeMutation = useMutation({
    mutationFn: async (data: Partial<PspTheme>) => {
      return apiClient.put<PspTheme>(`settings/psps/${selectedPspId}/theme`, data);
    },
    onSuccess: () => {
      setSuccessMessage("Theme updated successfully!");
      queryClient.invalidateQueries({ queryKey: ["settings", "psps", selectedPspId, "theme"] });
      queryClient.invalidateQueries({ queryKey: ["user", "me"] }); // Refresh user to get new theme
      setTimeout(() => setSuccessMessage(null), 3000);
    },
    onError: () => {
      setSuccessMessage("Failed to update theme");
      setTimeout(() => setSuccessMessage(null), 3000);
    },
  });

  const handlePspChange = (pspId: number) => {
    setSelectedPspId(pspId);
    setThemeData(null);
  };

  const handlePresetSelect = (presetId: string) => {
    if (!presets || !themeData) return;
    const preset = presets[presetId];
    if (preset) {
      setThemeData({
        ...themeData,
        brandingTheme: presetId,
        primaryColor: preset.primaryColor,
        secondaryColor: preset.secondaryColor,
        accentColor: preset.accentColor,
      });
    }
  };

  const handleSaveTheme = async () => {
    if (!selectedPspId || !themeData) return;
    setSaving(true);
    try {
      await updateThemeMutation.mutateAsync({
        brandingTheme: themeData.brandingTheme,
        primaryColor: themeData.primaryColor,
        secondaryColor: themeData.secondaryColor,
        accentColor: themeData.accentColor,
        logoUrl: themeData.logoUrl,
        fontFamily: themeData.fontFamily,
        fontSize: themeData.fontSize,
        buttonRadius: themeData.buttonRadius,
        buttonStyle: themeData.buttonStyle,
        navStyle: themeData.navStyle,
      });
    } finally {
      setSaving(false);
    }
  };

  const { user } = useAuth();
  const isSuperAdmin = user?.pspId === 0;

  // System Settings Interface
  interface SystemSettings {
    maintenanceMode: boolean;
    debugLogging: boolean;
    riskThresholdHigh: number;
    riskThresholdMedium: number;
    auditRetentionDays: number;
    allowCrossBorderTxns: boolean;
  }

  // Fetch system settings
  const { data: systemSettingsData, isLoading: isLoadingSystemSettings } = useQuery<SystemSettings>({
    queryKey: ["settings", "system"],
    queryFn: () => apiClient.get<SystemSettings>("settings/system"),
    enabled: isSuperAdmin,
  });

  // System Settings State (with defaults)
  const [systemSettings, setSystemSettings] = useState<SystemSettings>({
    maintenanceMode: false,
    debugLogging: false,
    riskThresholdHigh: 80,
    riskThresholdMedium: 50,
    auditRetentionDays: 90,
    allowCrossBorderTxns: true,
  });

  // Update local state when data loads
  useEffect(() => {
    if (systemSettingsData) {
      setSystemSettings(systemSettingsData);
    }
  }, [systemSettingsData]);

  const handleSystemSettingChange = (setting: string, value: any) => {
    setSystemSettings(prev => ({
      ...prev,
      [setting]: value
    }));
  };

  // Update system settings mutation
  const updateSystemSettingsMutation = useMutation({
    mutationFn: async (data: SystemSettings) => {
      return apiClient.put<SystemSettings>("settings/system", data);
    },
    onSuccess: () => {
      setSuccessMessage("System settings saved successfully");
      queryClient.invalidateQueries({ queryKey: ["settings", "system"] });
      setTimeout(() => setSuccessMessage(null), 3000);
    },
    onError: () => {
      setSuccessMessage("Failed to save system settings");
      setTimeout(() => setSuccessMessage(null), 3000);
    },
  });

  const handleSaveSystemSettings = async () => {
    await updateSystemSettingsMutation.mutateAsync(systemSettings);
  };

  return (
    <Box>
      <Typography variant="h4" sx={{ color: "text.primary", mb: 3, fontWeight: 600 }}>
        Settings
      </Typography>

      <Tabs value={tabValue} onChange={(_, newValue) => setTabValue(newValue)} sx={{ mb: 3 }}>
        <Tab label="PSP Theme Management" />
        {isSuperAdmin && <Tab label="System Settings" />}
      </Tabs>

      <TabPanel value={tabValue} index={0}>
        <Paper sx={{ p: 3, backgroundColor: "background.paper", border: "1px solid rgba(0,0,0,0.1)" }}>
          <Typography variant="h6" sx={{ color: "text.primary", mb: 3 }}>
            PSP Theme Customization
          </Typography>

          {successMessage && (
            <Alert severity={successMessage.includes("successfully") ? "success" : "error"} sx={{ mb: 3 }}>
              {successMessage}
            </Alert>
          )}

          {/* PSP Selection */}
          <Grid container spacing={3} sx={{ mb: 3 }}>
            <Grid item xs={12} md={6}>
              <Tooltip title="Select a PSP to customize its theme and branding" arrow>
                <FormControl fullWidth>
                  <InputLabel>Select PSP</InputLabel>
                  <Select
                    value={selectedPspId || ""}
                    onChange={(e) => handlePspChange(e.target.value as number)}
                    label="Select PSP"
                    disabled={isLoadingPsps}
                  >
                  {psps?.map((psp) => (
                    <MenuItem key={psp.id} value={psp.id}>
                      {psp.name} ({psp.code}) - {psp.status}
                    </MenuItem>
                  ))}
                  </Select>
                </FormControl>
              </Tooltip>
            </Grid>
          </Grid>

          {isLoadingTheme && selectedPspId ? (
            <Box sx={{ display: "flex", justifyContent: "center", p: 4 }}>
              <CircularProgress />
            </Box>
          ) : themeData ? (
            <>
              {/* Theme Presets */}
              <Box sx={{ mb: 4 }}>
                <Typography variant="subtitle1" sx={{ mb: 2, fontWeight: 600 }}>
                  Theme Presets
                </Typography>
                <Box sx={{ display: "flex", gap: 2, flexWrap: "wrap" }}>
                  {BRAND_THEMES.map((preset) => (
                    <Tooltip key={preset.id} title={preset.name} arrow>
                      <Chip
                        label={preset.name}
                        onClick={() => handlePresetSelect(preset.id)}
                        sx={{
                          cursor: "pointer",
                          backgroundColor:
                            themeData.brandingTheme === preset.id ? preset.primaryColor : "transparent",
                          color: themeData.brandingTheme === preset.id ? "#fff" : "text.primary",
                          border: `2px solid ${preset.primaryColor}`,
                          "&:hover": {
                            backgroundColor: preset.primaryColor + "20",
                          },
                        }}
                      />
                    </Tooltip>
                  ))}
                </Box>
              </Box>

              {/* Color Customization */}
              <Grid container spacing={3} sx={{ mb: 3 }}>
                <Grid item xs={12} md={4}>
                  <Tooltip title="Main brand color used for primary buttons and highlights" arrow>
                    <TextField
                      fullWidth
                      label="Primary Color"
                      type="color"
                      value={themeData.primaryColor || "#8B4049"}
                      onChange={(e) => setThemeData({ ...themeData, primaryColor: e.target.value })}
                      InputLabelProps={{ shrink: true }}
                    />
                  </Tooltip>
                </Grid>
                <Grid item xs={12} md={4}>
                  <Tooltip title="Secondary brand color used for accents and secondary elements" arrow>
                    <TextField
                      fullWidth
                      label="Secondary Color"
                      type="color"
                      value={themeData.secondaryColor || "#C9A961"}
                      onChange={(e) => setThemeData({ ...themeData, secondaryColor: e.target.value })}
                      InputLabelProps={{ shrink: true }}
                    />
                  </Tooltip>
                </Grid>
                <Grid item xs={12} md={4}>
                  <Tooltip title="Accent color used for error states and emphasis" arrow>
                    <TextField
                      fullWidth
                      label="Accent Color"
                      type="color"
                      value={themeData.accentColor || "#A0525C"}
                      onChange={(e) => setThemeData({ ...themeData, accentColor: e.target.value })}
                      InputLabelProps={{ shrink: true }}
                    />
                  </Tooltip>
                </Grid>
                <Grid item xs={12} md={6}>
                  <Tooltip title="URL to the PSP's logo image (PNG, SVG, or JPG format)" arrow>
                    <TextField
                      fullWidth
                      label="Logo URL"
                      value={themeData.logoUrl || ""}
                      onChange={(e) => setThemeData({ ...themeData, logoUrl: e.target.value })}
                      placeholder="https://example.com/logo.png"
                    />
                  </Tooltip>
                </Grid>
                <Grid item xs={12} md={6}>
                  <Tooltip title="Font family for the entire application (e.g., 'Inter', 'Outfit', sans-serif)" arrow>
                    <TextField
                      fullWidth
                      label="Font Family"
                      value={themeData.fontFamily || ""}
                      onChange={(e) => setThemeData({ ...themeData, fontFamily: e.target.value })}
                      placeholder="'Inter', 'Outfit', sans-serif"
                    />
                  </Tooltip>
                </Grid>
                <Grid item xs={12} md={6}>
                  <Tooltip title="Base font size for the application (e.g., 14px or 1rem)" arrow>
                    <TextField
                      fullWidth
                      label="Font Size"
                      value={themeData.fontSize || ""}
                      onChange={(e) => setThemeData({ ...themeData, fontSize: e.target.value })}
                      placeholder="14px or 1rem"
                    />
                  </Tooltip>
                </Grid>
                <Grid item xs={12} md={6}>
                  <Tooltip title="Border radius for buttons (e.g., 12px or 0.5rem)" arrow>
                    <TextField
                      fullWidth
                      label="Button Border Radius"
                      value={themeData.buttonRadius || ""}
                      onChange={(e) => setThemeData({ ...themeData, buttonRadius: e.target.value })}
                      placeholder="12px or 0.5rem"
                    />
                  </Tooltip>
                </Grid>
                <Grid item xs={12} md={6}>
                  <Tooltip title="Visual style for buttons: Flat (minimal), Raised (elevated), or Outlined (border only)" arrow>
                    <FormControl fullWidth>
                      <InputLabel>Button Style</InputLabel>
                      <Select
                        value={themeData.buttonStyle || "flat"}
                        onChange={(e) => setThemeData({ ...themeData, buttonStyle: e.target.value })}
                        label="Button Style"
                      >
                        <MenuItem value="flat">Flat</MenuItem>
                        <MenuItem value="raised">Raised</MenuItem>
                        <MenuItem value="outlined">Outlined</MenuItem>
                      </Select>
                    </FormControl>
                  </Tooltip>
                </Grid>
                <Grid item xs={12} md={6}>
                  <Tooltip title="Navigation layout style: Drawer (side panel) or Top Bar (horizontal menu)" arrow>
                    <FormControl fullWidth>
                      <InputLabel>Navigation Style</InputLabel>
                      <Select
                        value={themeData.navStyle || "drawer"}
                        onChange={(e) => setThemeData({ ...themeData, navStyle: e.target.value })}
                        label="Navigation Style"
                      >
                        <MenuItem value="drawer">Drawer</MenuItem>
                        <MenuItem value="topbar">Top Bar</MenuItem>
                      </Select>
                    </FormControl>
                  </Tooltip>
                </Grid>
              </Grid>

              <Box sx={{ display: "flex", gap: 2, justifyContent: "flex-end" }}>
                <Tooltip title="Reset theme to the last saved configuration" arrow>
                  <Button variant="outlined" onClick={() => setThemeData(currentTheme || null)}>
                    Reset
                  </Button>
                </Tooltip>
                <Tooltip title="Save the current theme configuration for this PSP" arrow>
                  <Button
                    variant="contained"
                    onClick={handleSaveTheme}
                    disabled={saving}
                    sx={{ backgroundColor: "#a93226", "&:hover": { backgroundColor: "#922b21" } }}
                  >
                    {saving ? "Saving..." : "Save Theme"}
                  </Button>
                </Tooltip>
              </Box>
            </>
          ) : selectedPspId ? (
            <Alert severity="info">Loading theme configuration...</Alert>
          ) : (
            <Alert severity="info">Please select a PSP to manage its theme.</Alert>
          )}
        </Paper>
      </TabPanel>

      {isSuperAdmin && (
        <TabPanel value={tabValue} index={1}>
          <Paper sx={{ p: 3, backgroundColor: "background.paper", border: "1px solid rgba(0,0,0,0.1)" }}>
            <Typography variant="h6" sx={{ color: "text.primary", mb: 3 }}>
              System Configuration
            </Typography>

            <Grid container spacing={3}>
              {isLoadingSystemSettings ? (
                <Grid item xs={12}>
                  <Box sx={{ display: "flex", justifyContent: "center", p: 4 }}>
                    <CircularProgress />
                  </Box>
                </Grid>
              ) : (
                <>
                  <Grid item xs={12} md={6}>
                    <Typography variant="subtitle2" sx={{ mb: 1 }}>System Status</Typography>
                    <Box sx={{ display: "flex", flexDirection: "column", gap: 2 }}>
                      <Tooltip title="Enable maintenance mode to restrict access for system updates" arrow>
                        <TextField
                          select
                          label="Maintenance Mode"
                          value={systemSettings.maintenanceMode ? "true" : "false"}
                          onChange={(e) => handleSystemSettingChange('maintenanceMode', e.target.value === "true")}
                          fullWidth
                          disabled={updateSystemSettingsMutation.isPending}
                        >
                          <MenuItem value="true">Enabled</MenuItem>
                          <MenuItem value="false">Disabled</MenuItem>
                        </TextField>
                      </Tooltip>
                      <Tooltip title="Enable debug logging for detailed system diagnostics" arrow>
                        <TextField
                          select
                          label="Debug Logging"
                          value={systemSettings.debugLogging ? "true" : "false"}
                          onChange={(e) => handleSystemSettingChange('debugLogging', e.target.value === "true")}
                          fullWidth
                          disabled={updateSystemSettingsMutation.isPending}
                        >
                          <MenuItem value="true">Enabled</MenuItem>
                          <MenuItem value="false">Disabled</MenuItem>
                        </TextField>
                      </Tooltip>
                    </Box>
                  </Grid>

              <Grid item xs={12} md={6}>
                <Typography variant="subtitle2" sx={{ mb: 1 }}>Risk & Compliance</Typography>
                <Box sx={{ display: "flex", flexDirection: "column", gap: 2 }}>
                  <Tooltip title="Risk score threshold (0-100) above which transactions are considered high risk" arrow>
                    <TextField
                      label="High Risk Score Threshold"
                      type="number"
                      value={systemSettings.riskThresholdHigh}
                      onChange={(e) => handleSystemSettingChange('riskThresholdHigh', parseInt(e.target.value))}
                      fullWidth
                      disabled={updateSystemSettingsMutation.isPending}
                    />
                  </Tooltip>
                  <Tooltip title="Risk score threshold (0-100) above which transactions are considered medium risk" arrow>
                    <TextField
                      label="Medium Risk Score Threshold"
                      type="number"
                      value={systemSettings.riskThresholdMedium}
                      onChange={(e) => handleSystemSettingChange('riskThresholdMedium', parseInt(e.target.value))}
                      fullWidth
                      disabled={updateSystemSettingsMutation.isPending}
                    />
                  </Tooltip>
                </Box>
              </Grid>

              <Grid item xs={12} md={6}>
                <Typography variant="subtitle2" sx={{ mb: 1 }}>Data Policies</Typography>
                <Box sx={{ display: "flex", flexDirection: "column", gap: 2 }}>
                  <Tooltip title="Number of days to retain audit logs before automatic deletion" arrow>
                    <TextField
                      label="Audit Log Retention (Days)"
                      type="number"
                      value={systemSettings.auditRetentionDays}
                      onChange={(e) => handleSystemSettingChange('auditRetentionDays', parseInt(e.target.value))}
                      fullWidth
                      disabled={updateSystemSettingsMutation.isPending}
                    />
                  </Tooltip>
                </Box>
              </Grid>

              <Grid item xs={12}>
                <Divider sx={{ my: 2 }} />
                <Box sx={{ display: "flex", justifyContent: "flex-end" }}>
                  <Tooltip title="Save all system configuration settings" arrow>
                    <Button 
                      variant="contained" 
                      color="primary" 
                      onClick={handleSaveSystemSettings}
                      disabled={updateSystemSettingsMutation.isPending}
                    >
                      {updateSystemSettingsMutation.isPending ? "Saving..." : "Save System Settings"}
                    </Button>
                  </Tooltip>
                </Box>
              </Grid>
                </>
              )}
            </Grid>
          </Paper>
        </TabPanel>
      )}
    </Box>
  );
}
