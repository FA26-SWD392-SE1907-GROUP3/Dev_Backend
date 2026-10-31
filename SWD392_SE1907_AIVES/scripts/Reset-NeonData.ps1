[CmdletBinding()]
param(
    [switch]$Execute,
    [string]$ProjectRoot = (Split-Path -Parent $PSScriptRoot),
    [string]$EnvFile,
    [string]$BackupDirectory
)

$ErrorActionPreference = 'Stop'
$project = (Resolve-Path -LiteralPath $ProjectRoot).Path
if (-not (Test-Path -LiteralPath (Join-Path $project 'pom.xml'))) { throw 'ProjectRoot must point to the AIVES Maven project.' }
if (-not $EnvFile) { $EnvFile = Join-Path $project '.env' }
$environmentPath = (Resolve-Path -LiteralPath $EnvFile).Path
if (-not $BackupDirectory) { $BackupDirectory = Join-Path $project 'backups/neon-reset' }
$backupRoot = [IO.Path]::GetFullPath($BackupDirectory)
$helper = Join-Path $PSScriptRoot 'maintenance/NeonDataReset.java'
if (-not (Test-Path -LiteralPath $helper)) { throw 'Missing maintenance/NeonDataReset.java next to the script.' }

$java = $null
if ($env:JAVA_HOME) {
    $candidate = Join-Path $env:JAVA_HOME 'bin/java.exe'
    if (Test-Path -LiteralPath $candidate) { $java = $candidate }
}
if (-not $java) {
    $command = Get-Command java -ErrorAction SilentlyContinue
    if ($command) { $java = $command.Source }
}
if (-not $java) { throw 'Java JDK 21 or newer is required. Set JAVA_HOME or add java to PATH.' }

$cacheRoot = Join-Path $project 'target/neon-reset-tool'
New-Item -ItemType Directory -Path $cacheRoot -Force | Out-Null
$classpathFile = Join-Path $cacheRoot 'runtime-classpath.txt'
$rebuildClasspath = -not (Test-Path -LiteralPath $classpathFile)
if (-not $rebuildClasspath) {
    $classpath = [IO.File]::ReadAllText($classpathFile).Trim()
    $separator = [IO.Path]::PathSeparator
    $missingJars = @($classpath.Split($separator) | Where-Object { -not (Test-Path -LiteralPath $_) })
    $rebuildClasspath = [string]::IsNullOrWhiteSpace($classpath) -or $missingJars.Count -gt 0 -or
        (Get-Item -LiteralPath (Join-Path $project 'pom.xml')).LastWriteTimeUtc -gt (Get-Item -LiteralPath $classpathFile).LastWriteTimeUtc
}
if ($rebuildClasspath) {
    $maven = Join-Path $project 'mvnw.cmd'
    if (-not (Test-Path -LiteralPath $maven)) {
        $command = Get-Command mvn -ErrorAction SilentlyContinue
        if (-not $command) { throw 'Maven wrapper or mvn is required to resolve the existing project dependencies.' }
        $maven = $command.Source
    }
    $dependencyLog = Join-Path $cacheRoot 'dependencies.log'
    Write-Host 'Resolving the existing project dependencies (first run or POM change)...'
    Push-Location -LiteralPath $project
    try {
        & $maven -q '-DincludeScope=runtime' "-Dmdep.outputFile=$classpathFile" dependency:build-classpath *> $dependencyLog
        if ($LASTEXITCODE -ne 0) { throw "Cannot resolve project dependencies. See $dependencyLog" }
    } finally { Pop-Location }
}
$classpath = [IO.File]::ReadAllText($classpathFile).Trim()

function Invoke-ResetTool([string[]]$ToolArguments) {
    & $java --class-path $classpath $helper @ToolArguments
    if ($LASTEXITCODE -ne 0) { throw 'The maintenance tool stopped. No further reset steps were run.' }
}

Invoke-ResetTool @('preview', $environmentPath)
if (-not $Execute) {
    Write-Host ''
    Write-Host 'Preview completed. No database data was changed.'
    Write-Host 'To back up and reset: .\scripts\Reset-NeonData.ps1 -Execute'
    return
}

Write-Host ''
Write-Host 'Backing up application data before the requested reset...'
Invoke-ResetTool @('backup', $environmentPath, $backupRoot)
$pointer = Join-Path $backupRoot 'latest-backup-path.txt'
$backupPath = [IO.File]::ReadAllText($pointer).Trim()
$resolvedBackup = [IO.Path]::GetFullPath($backupPath)
$allowedBackupRoot = $backupRoot.TrimEnd([IO.Path]::DirectorySeparatorChar, [IO.Path]::AltDirectorySeparatorChar) + [IO.Path]::DirectorySeparatorChar
if (-not $resolvedBackup.StartsWith($allowedBackupRoot, [StringComparison]::OrdinalIgnoreCase)) { throw 'Backup path is outside the selected backup directory.' }
if (-not (Test-Path -LiteralPath (Join-Path $resolvedBackup 'manifest.json'))) { throw 'Backup did not finish; reset stopped.' }

Write-Host 'Resetting public application tables; keeping users and courses...'
Invoke-ResetTool @('reset', $environmentPath, $resolvedBackup, '--confirm-keep-users-courses')
Write-Host "Backup: $resolvedBackup"
Write-Host 'Completed. System schemas, users, courses and application code were preserved.'
