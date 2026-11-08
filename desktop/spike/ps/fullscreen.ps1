# Spike only: a topmost, borderless, full-screen window (Citrix/RDP-style) that also re-asserts topmost if asked.
param([int]$ReassertMs = 0)
Add-Type -AssemblyName System.Windows.Forms
$f = New-Object System.Windows.Forms.Form
$f.FormBorderStyle = 'None'
$f.WindowState = 'Maximized'
$f.TopMost = $true
$f.BackColor = [System.Drawing.Color]::DarkGreen
$f.Text = 'SpikeFullscreen'
if ($ReassertMs -gt 0) {
    $t = New-Object System.Windows.Forms.Timer
    $t.Interval = $ReassertMs
    $t.Add_Tick({ $f.TopMost = $false; $f.TopMost = $true })
    $t.Start()
}
[System.Windows.Forms.Application]::Run($f)
