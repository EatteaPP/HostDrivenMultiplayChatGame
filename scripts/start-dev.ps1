Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$projectRoot = Split-Path -Parent $PSScriptRoot
$backendCwd = $projectRoot
$frontendCwd = Join-Path $projectRoot "frontend"

if (-not (Test-Path -LiteralPath $frontendCwd)) {
    throw "frontend directory not found: $frontendCwd"
}

$backendCommandTemplate = @'
$host.UI.RawUI.WindowTitle = "HostGame Backend (Spring Boot)"
$env:JAVA_HOME="C:\Tools\JAVA\java-17"
$env:MAVEN_HOME="C:\Users\EatteaPP\tools\apache-maven-3.9.14"
$env:Path="C:\Tools\JAVA\java-17\bin;C:\Users\EatteaPP\tools\apache-maven-3.9.14\bin;C:\Program Files\Git\cmd;" + $env:Path
Set-Location -LiteralPath "__BACKEND_CWD__"
mvn spring-boot:run
'@

$frontendCommandTemplate = @'
$host.UI.RawUI.WindowTitle = "HostGame Frontend (Vite)"
$env:NVM_HOME="C:\Users\EatteaPP\AppData\Local\nvm"
$env:NVM_SYMLINK="C:\nvm4w\nodejs"
$env:Path="$env:NVM_HOME;$env:NVM_SYMLINK;$env:Path"
Set-Location -LiteralPath "__FRONTEND_CWD__"
& "C:\nvm4w\nodejs\npm.cmd" run dev -- --host 127.0.0.1 --port 5173
'@

$backendCommand = $backendCommandTemplate.Replace("__BACKEND_CWD__", $backendCwd)
$frontendCommand = $frontendCommandTemplate.Replace("__FRONTEND_CWD__", $frontendCwd)

$backendEncoded = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($backendCommand))
$frontendEncoded = [Convert]::ToBase64String([Text.Encoding]::Unicode.GetBytes($frontendCommand))

Start-Process -FilePath "powershell.exe" -ArgumentList @(
    "-NoExit",
    "-EncodedCommand",
    $backendEncoded
)

Start-Process -FilePath "powershell.exe" -ArgumentList @(
    "-NoExit",
    "-EncodedCommand",
    $frontendEncoded
)

Write-Host "Opened two dev consoles: Backend and Frontend."
