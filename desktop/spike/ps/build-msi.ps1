# Spike only: WiX 3.14 heat/candle/light on the Compose app image -> MSIs at several versions (same image).
param([string]$Image, [string]$Out, [string[]]$Versions = @('1.0.0', '1.0.1', '1.0.2'))
$ErrorActionPreference = 'Stop'
$wix = $null
foreach ($c in @($env:WIX, 'C:\Program Files (x86)\WiX Toolset v3.14', 'C:\Program Files (x86)\WiX Toolset v3.11')) {
    if ($c -and (Test-Path (Join-Path $c 'bin\candle.exe'))) { $wix = Join-Path $c 'bin'; break }
}
if (-not $wix) {
    Write-Host "WiX 3.x not on this image: downloading wix314-binaries.zip (MS-RL) from the WiX GitHub releases"
    $zip = Join-Path $env:RUNNER_TEMP 'wix314.zip'
    for ($i = 1; $i -le 6; $i++) {
        try { Invoke-WebRequest -UseBasicParsing -Uri 'https://github.com/wixtoolset/wix3/releases/download/wix3141rtm/wix314-binaries.zip' -OutFile $zip; break }
        catch { Write-Host "WiX download attempt $i failed: $($_.Exception.Message)"; Start-Sleep -Seconds (15 * $i) }
    }
    $wix = Join-Path $env:RUNNER_TEMP 'wix314'
    Expand-Archive $zip -DestinationPath $wix -Force
}
$ver = (Get-Item (Join-Path $wix 'candle.exe')).VersionInfo.ProductVersion
Write-Host "WIX $wix version $ver"
New-Item -ItemType Directory -Force $Out | Out-Null
Set-Content -Path (Join-Path $Out 'wix-version.txt') -Value "$wix $ver"
$obj = Join-Path $Out 'obj'
New-Item -ItemType Directory -Force $obj | Out-Null
& (Join-Path $wix 'heat.exe') dir $Image -nologo -cg AppFiles -dr INSTALLFOLDER -ag -sfrag -srd -sreg -scom -var var.AppDir -out (Join-Path $obj 'AppFiles.wxs')
if ($LASTEXITCODE) { throw "heat $LASTEXITCODE" }
$product = Join-Path $PSScriptRoot '..\wix\Product.wxs'
foreach ($v in $Versions) {
    $o = Join-Path $obj $v
    New-Item -ItemType Directory -Force $o | Out-Null
    & (Join-Path $wix 'candle.exe') -nologo -arch x64 -ext WixUtilExtension "-dVersion=$v" "-dAppDir=$Image" -out "$o\" $product (Join-Path $obj 'AppFiles.wxs')
    if ($LASTEXITCODE) { throw "candle $LASTEXITCODE" }
    & (Join-Path $wix 'light.exe') -nologo -ext WixUtilExtension -sval -out (Join-Path $Out "TransfusionVS-$v.msi") "$o\Product.wixobj" "$o\AppFiles.wixobj"
    if ($LASTEXITCODE) { throw "light $LASTEXITCODE" }
}
Get-ChildItem $Out -Filter *.msi | ForEach-Object { Write-Host "MSI $($_.Name) $($_.Length) bytes" }
