param([switch]$CreateAdmin)
$ErrorActionPreference = 'Stop'
Set-Location -LiteralPath (Split-Path -Parent $PSScriptRoot)

function Read-Secret([string]$Prompt) {
    $secureValue = Read-Host $Prompt -AsSecureString
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secureValue)
    try { return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer) }
}

if (-not $env:DB_URL) { $env:DB_URL = 'jdbc:sqlserver://localhost:1433;databaseName=AIVES;encrypt=true;trustServerCertificate=true' }
if (-not $env:DB_USERNAME) { $env:DB_USERNAME = Read-Host 'SQL Server login (for example sa)' }
if (-not $env:DB_PASSWORD) { $env:DB_PASSWORD = Read-Secret 'SQL Server password' }
if ($CreateAdmin) {
    $env:ADMIN_USERNAME = Read-Host 'New AIVES admin username (for example quoc)'
    $env:ADMIN_PASSWORD = Read-Secret 'New AIVES admin password (minimum 8 characters)'
    $env:ADMIN_EMAIL = Read-Host 'New AIVES admin email'
}
Write-Host 'Starting backend. Use your AIVES account to sign in; the SQL Server account is separate.'
& .\mvnw.cmd spring-boot:run
