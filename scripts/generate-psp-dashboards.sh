#!/bin/bash

# Script to generate PSP-specific dashboards with hard-coded PSP filters
# This ensures PSP users can only see their own metrics

# List of PSPs (update this list with your actual PSPs)
PSPS=("PSP_M-PESA" "PSP_PAYPAL" "PSP_STRIPE")

# List of dashboards to generate PSP-specific versions
DASHBOARDS=(
    "01-transaction-overview"
    "02-aml-risk-dashboard"
    "03-fraud-detection-dashboard"
    "04-compliance-dashboard"
    "05-system-performance-dashboard"
    "06-model-performance-dashboard"
    "07-screening-dashboard"
)

# Base directory
BASE_DIR="grafana/dashboards"
PSP_DIR="$BASE_DIR/psp-users"
PLATFORM_ADMIN_DIR="$BASE_DIR/platform-admin"

# Create directories
mkdir -p "$PSP_DIR"
mkdir -p "$PLATFORM_ADMIN_DIR"

echo "Generating PSP-specific dashboards..."

# Generate PSP-specific dashboards
for PSP in "${PSPS[@]}"; do
    PSP_FOLDER="$PSP_DIR/$PSP"
    mkdir -p "$PSP_FOLDER"
    
    echo "Processing PSP: $PSP"
    
    for DASHBOARD in "${DASHBOARDS[@]}"; do
        SOURCE_FILE="$BASE_DIR/$DASHBOARD.json"
        TARGET_FILE="$PSP_FOLDER/$DASHBOARD.json"
        
        if [ ! -f "$SOURCE_FILE" ]; then
            echo "Warning: Source file not found: $SOURCE_FILE"
            continue
        fi
        
        echo "  Creating: $TARGET_FILE"
        
        # Copy dashboard
        cp "$SOURCE_FILE" "$TARGET_FILE"
        
        # Update PSP variable to constant (hard-coded)
        # Replace PSP query variable with constant variable
        sed -i.bak 's/"name": "PSP",/"name": "PSP",\n        "type": "constant",\n        "current": {\n          "selected": false,\n          "text": "'"$PSP"'",\n          "value": "'"$PSP"'"\n        },\n        "hide": 2,/g' "$TARGET_FILE"
        
        # Remove the query-related fields for PSP variable
        sed -i.bak '/"query": {/,/},/d' "$TARGET_FILE"
        sed -i.bak '/"definition": "label_values(psp_code)",/d' "$TARGET_FILE"
        sed -i.bak '/"includeAll": true,/d' "$TARGET_FILE"
        sed -i.bak '/"multi": true,/d' "$TARGET_FILE"
        
        # Update all queries to use hard-coded PSP instead of variable
        sed -i.bak "s/psp_code=~\"\\\$PSP\"/psp_code=\"$PSP\"/g" "$TARGET_FILE"
        sed -i.bak "s/psp_code=~'\\\$PSP'/psp_code=\"$PSP\"/g" "$TARGET_FILE"
        
        # Update dashboard title to include PSP name
        sed -i.bak "s/\"title\": \"AML\/Fraud Detector -/\"title\": \"AML\/Fraud Detector ($PSP) -/g" "$TARGET_FILE"
        
        # Clean up backup files
        rm -f "$TARGET_FILE.bak"
    done
    
    echo "  Completed PSP: $PSP"
done

# Copy platform admin dashboards (keep PSP dropdown)
echo "Copying platform admin dashboards..."
for DASHBOARD in "${DASHBOARDS[@]}"; do
    SOURCE_FILE="$BASE_DIR/$DASHBOARD.json"
    TARGET_FILE="$PLATFORM_ADMIN_DIR/$DASHBOARD.json"
    
    if [ -f "$SOURCE_FILE" ]; then
        cp "$SOURCE_FILE" "$TARGET_FILE"
        echo "  Copied: $TARGET_FILE"
    fi
done

# Copy infrastructure dashboards to platform admin only
INFRASTRUCTURE_DASHBOARDS=(
    "08-infrastructure-resources-dashboard"
    "09-thread-pools-throughput-dashboard"
    "10-circuit-breaker-resilience-dashboard"
    "11-revenue-income-dashboard"
)

echo "Copying infrastructure dashboards to platform admin..."
for DASHBOARD in "${INFRASTRUCTURE_DASHBOARDS[@]}"; do
    SOURCE_FILE="$BASE_DIR/$DASHBOARD.json"
    TARGET_FILE="$PLATFORM_ADMIN_DIR/$DASHBOARD.json"
    
    if [ -f "$SOURCE_FILE" ]; then
        cp "$SOURCE_FILE" "$TARGET_FILE"
        echo "  Copied: $TARGET_FILE"
    fi
done

echo ""
echo "Dashboard generation complete!"
echo ""
echo "Next steps:"
echo "1. Configure Grafana folder permissions:"
echo "   - PSP folders: Assign to respective PSP user groups"
echo "   - Platform Admin folder: Assign to Platform Admin group"
echo "2. Update Grafana dashboard provisioning to use new folder structure"
echo "3. Test access with PSP users and Platform Admins"