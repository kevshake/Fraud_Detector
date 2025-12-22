param(

    [switch]$SkipBuild

)



Write-Host "=== AML Fraud Detector startup ===" -ForegroundColor Cyan



# 1) Load .env into the current process environment

$envFile = Join-Path $PSScriptRoot ".env"

if (Test-Path $envFile) {

    Write-Host "Loading environment variables from .env" -ForegroundColor Yellow

    Get-Content $envFile |

        Where-Object { $_ -and -not $_.Trim().StartsWith('#') } |

        ForEach-Object {

            if ($_ -match '^(?<key>[^=]+)=(?<value>.*)$') {

                $key = $matches['key'].Trim()

                $value = $matches['value'].Trim()

                [System.Environment]::SetEnvironmentVariable($key, $value, 'Process')

            }

        }

} else {

    Write-Host ".env file not found, using current environment only" -ForegroundColor Yellow

}



# 2) Resolve database settings (must match Spring Boot config)

$databaseUrl  = $env:DATABASE_URL

if (-not $databaseUrl -or [string]::IsNullOrWhiteSpace($databaseUrl)) {

    $databaseUrl = "jdbc:postgresql://localhost:5432/aml_fraud_db"

}



$databaseUser = $env:DATABASE_USERNAME

if (-not $databaseUser -or [string]::IsNullOrWhiteSpace($databaseUser)) {

    Write-Host "WARNING: DATABASE_USERNAME not set, defaulting to 'postgres'" -ForegroundColor Yellow

    $databaseUser = "postgres"

} else {

    Write-Host "Using database username from .env/environment: '$databaseUser'" -ForegroundColor DarkCyan

}



$databasePassword = $env:POSTGRES_DATABASE_PASSWORD

if (-not $databasePassword -or [string]::IsNullOrWhiteSpace($databasePassword)) {

    Write-Host "ERROR: POSTGRES_DATABASE_PASSWORD is not set (in .env or environment)." -ForegroundColor Red

    Write-Host "       Set it in .env or as an environment variable and retry." -ForegroundColor Red

    exit 1

}



# 3) Parse JDBC URL -> host, port, database

#    Expected format: jdbc:postgresql://host:port/database?...

if ($databaseUrl -notmatch '^jdbc:postgresql://([^:/]+)(?::(\d+))?/([^?]+)') {

    Write-Host "ERROR: DATABASE_URL has an unexpected format: $databaseUrl" -ForegroundColor Red

    Write-Host "       Expected: jdbc:postgresql://host:port/database" -ForegroundColor Red

    exit 1

}



$dbHost = $matches[1]

$dbPort = if ($matches[2]) { [int]$matches[2] } else { 5432 }

$dbName = $matches[3]



Write-Host "Using database URL: $databaseUrl" -ForegroundColor DarkCyan

Write-Host "Host: $dbHost, Port: $dbPort, DB: $dbName, User: $databaseUser" -ForegroundColor DarkCyan



# 4) Basic TCP connectivity check

Write-Host "Checking TCP connectivity to ${dbHost}:$dbPort ..." -ForegroundColor Yellow

try {

    $tcpResult = Test-NetConnection -ComputerName $dbHost -Port $dbPort -WarningAction SilentlyContinue

    if (-not $tcpResult.TcpTestSucceeded) {

        Write-Host "ERROR: Cannot reach ${dbHost}:$dbPort (TCP connection failed)." -ForegroundColor Red

        Write-Host "       Ensure PostgreSQL is running and accessible." -ForegroundColor Red

        exit 1

    }

} catch {

    Write-Host "WARNING: Test-NetConnection failed: $_" -ForegroundColor Yellow

}



Write-Host "TCP connectivity to ${dbHost}:$dbPort OK" -ForegroundColor Green



# 5) Optional deep check: use psql if available to validate auth & DB

$psql = Get-Command psql -ErrorAction SilentlyContinue

if ($psql) {

    Write-Host "psql found at: $($psql.Source). Running authentication check..." -ForegroundColor Yellow



    # Use environment variables for psql (handles usernames with spaces correctly)

    $env:PGHOST     = $dbHost

    $env:PGPORT     = "$dbPort"

    $env:PGDATABASE = $dbName

    $env:PGUSER     = $databaseUser

    $env:PGPASSWORD = $databasePassword



    # Run psql with explicit -U flag as well (in case PGUSER env var doesn't work)

    # Using -U with quoted username to handle spaces

    $psqlArgs = @(

        "-h", $dbHost,

        "-p", "$dbPort",

        "-d", $dbName,

        "-U", $databaseUser,

        "-q",

        "-c", "SELECT 1"

    )



    & $psql.Source @psqlArgs 2>&1 | Out-Null

    if ($LASTEXITCODE -ne 0) {

        Write-Host "ERROR: PostgreSQL authentication or database access failed (psql returned $LASTEXITCODE)." -ForegroundColor Red

        Write-Host "       Check DATABASE_URL, DATABASE_USERNAME and POSTGRES_DATABASE_PASSWORD in .env / environment." -ForegroundColor Red

        Write-Host "       Username being used: '$databaseUser'" -ForegroundColor Yellow

        exit 1

    }



    Write-Host "PostgreSQL authentication check passed (SELECT 1)." -ForegroundColor Green

} else {

    Write-Host "psql not found on PATH; skipping deep authentication check." -ForegroundColor Yellow

    Write-Host "TCP connectivity is OK, but credentials themselves were not verified." -ForegroundColor Yellow

}



# 6) Optionally build, then start the Spring Boot application

Set-Location $PSScriptRoot



if (-not $SkipBuild) {

    Write-Host "Running: mvn clean compile -DskipTests" -ForegroundColor Cyan

    mvn clean compile -DskipTests

    if ($LASTEXITCODE -ne 0) {

        Write-Host "ERROR: Maven build failed (exit code $LASTEXITCODE)." -ForegroundColor Red

        exit $LASTEXITCODE

    }

}



Write-Host "Starting application with: mvn spring-boot:run" -ForegroundColor Cyan

mvn spring-boot:run

