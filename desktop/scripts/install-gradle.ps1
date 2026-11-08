# Gradle 9.6.1 for the Windows jobs when gradle/actions/setup-gradle fails (services.gradle.org answered 504 on
# 2026-11-08, spike run 42466721018). Two hosts, retries, and the published SHA-256 pinned here.
$ErrorActionPreference = 'Stop'
$sha = '9c0f7faeeb306cb14e4279a3e084ca6b596894089a0638e68a07c945a32c9e14'
$zip = Join-Path $env:RUNNER_TEMP 'gradle-9.6.1-bin.zip'
$hosts = 'https://services.gradle.org', 'https://downloads.gradle.org'
$ok = $false
for ($i = 1; $i -le 6 -and -not $ok; $i++) {
    foreach ($h in $hosts) {
        try {
            Invoke-WebRequest -UseBasicParsing -MaximumRedirection 5 -TimeoutSec 120 "$h/distributions/gradle-9.6.1-bin.zip" -OutFile $zip
            $got = (Get-FileHash $zip -Algorithm SHA256).Hash.ToLower()
            if ($got -eq $sha) { $ok = $true; Write-Host "gradle 9.6.1 from $h (attempt $i), sha256 ok"; break }
            Write-Host "sha256 mismatch from $h`: $got"
        } catch { Write-Host "attempt $i from $h failed: $($_.Exception.Message)" }
    }
    if (-not $ok) { Start-Sleep -Seconds (15 * $i) }
}
if (-not $ok) { throw 'Gradle 9.6.1 could not be downloaded with the right checksum' }
Expand-Archive $zip -DestinationPath $env:RUNNER_TEMP -Force
Join-Path $env:RUNNER_TEMP 'gradle-9.6.1\bin' | Out-File -Append -Encoding utf8 $env:GITHUB_PATH
