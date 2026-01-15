# Bulk replace dark mode colors with theme-aware properties
$pagesPath = "d:\PROJECTS\POS_GATEWAY\APP\AML_FRAUD_DETECTOR\FRONTEND\src\pages"

# Get all .tsx files recursively
$files = Get-ChildItem -Path $pagesPath -Filter "*.tsx" -Recurse

foreach ($file in $files) {
    $content = Get-Content $file.FullName -Raw
    $modified = $false
    
    # Replace hardcoded dark backgrounds
    if ($content -match 'backgroundColor: "#1a1a1a"') {
        $content = $content -replace 'backgroundColor: "#1a1a1a"', 'backgroundColor: "background.paper"'
        $modified = $true
    }
    
    if ($content -match 'backgroundColor: "#2a2a2a"') {
        $content = $content -replace 'backgroundColor: "#2a2a2a"', 'backgroundColor: "background.paper"'
        $modified = $true
    }
    
    # Replace hardcoded borders
    if ($content -match 'border: "1px solid rgba\(255,255,255,0\.1\)"') {
        $content = $content -replace 'border: "1px solid rgba\(255,255,255,0\.1\)"', 'border: "1px solid rgba(0,0,0,0.1)"'
        $modified = $true
    }
    
    if ($content -match 'borderBottom: "1px solid rgba\(255,255,255,0\.1\)"') {
        $content = $content -replace 'borderBottom: "1px solid rgba\(255,255,255,0\.1\)"', 'borderBottom: "1px solid rgba(0,0,0,0.1)"'
        $modified = $true
    }
    
    # Replace white text colors with theme colors
    if ($content -match 'color: "#fff"') {
        $content = $content -replace 'color: "#fff"', 'color: "text.primary"'
        $modified = $true
    }
    
    if ($content -match 'color: "rgba\(255,255,255,0\.8\)"') {
        $content = $content -replace 'color: "rgba\(255,255,255,0\.8\)"', 'color: "text.primary"'
        $modified = $true
    }
    
    if ($content -match 'color: "rgba\(255,255,255,0\.7\)"') {
        $content = $content -replace 'color: "rgba\(255,255,255,0\.7\)"', 'color: "text.secondary"'
        $modified = $true
    }
    
    if ($content -match 'color: "rgba\(255,255,255,0\.6\)"') {
        $content = $content -replace 'color: "rgba\(255,255,255,0\.6\)"', 'color: "text.secondary"'
        $modified = $true
    }
    
    if ($content -match 'color: "rgba\(255,255,255,0\.5\)"') {
        $content = $content -replace 'color: "rgba\(255,255,255,0\.5\)"', 'color: "text.disabled"'
        $modified = $true
    }
    
    if ($modified) {
        Set-Content -Path $file.FullName -Value $content -NoNewline
        Write-Host "Updated: $($file.FullName)"
    }
}

Write-Host "`nDone! Updated all page files with light theme colors."
