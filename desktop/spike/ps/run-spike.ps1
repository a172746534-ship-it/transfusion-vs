# Spike only: the Step 0 checks of CHANGE_PLAN §12, one phase per workflow step. Results: $env:SPIKE_OUT\results.md
param([Parameter(Mandatory)][string]$Phase)
$ErrorActionPreference = 'Continue'
. (Join-Path $PSScriptRoot 'Win32.ps1')
$out = $env:SPIKE_OUT
$msiDir = $env:SPIKE_MSI
New-Item -ItemType Directory -Force $out | Out-Null
$exe = 'C:\Program Files\TransfusionVS\TransfusionVS.exe'
$spike = Join-Path $env:LOCALAPPDATA 'TransfusionVS\spike'
$data = Join-Path $env:LOCALAPPDATA 'TransfusionVS\data'
$sid = ([Security.Principal.WindowsIdentity]::GetCurrent()).User.Value
$restartTask = "TransfusionVS Restart $sid"
$backstopTask = "TransfusionVS Backstop $sid"

function Shot([string]$name) {
    Add-Type -AssemblyName System.Drawing, System.Windows.Forms
    $b = [System.Windows.Forms.Screen]::PrimaryScreen.Bounds
    $bmp = New-Object System.Drawing.Bitmap $b.Width, $b.Height
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.CopyFromScreen($b.Location, [System.Drawing.Point]::Empty, $b.Size)
    $bmp.Save((Join-Path $out "$name.png"), [System.Drawing.Imaging.ImageFormat]::Png)
    $g.Dispose(); $bmp.Dispose()
}
function Colors-In([int]$l, [int]$t, [int]$r, [int]$bt) {
    Add-Type -AssemblyName System.Drawing
    $w = [Math]::Max(1, $r - $l); $h = [Math]::Max(1, $bt - $t)
    $bmp = New-Object System.Drawing.Bitmap $w, $h
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.CopyFromScreen($l, $t, 0, 0, (New-Object System.Drawing.Size $w, $h))
    $set = @{}
    for ($x = 0; $x -lt $w; $x += 7) { for ($y = 0; $y -lt $h; $y += 7) { $set[$bmp.GetPixel($x, $y).ToArgb()] = 1 } }
    $g.Dispose(); $bmp.Dispose()
    return $set.Count
}
function Launch([string]$arg) {
    # A non-admin token on the runner's own logged-on account (§8): runas /trustlevel:0x20000 (Basic User).
    $cmd = 'C:\spike-launch.cmd'
    Set-Content -Path $cmd -Value "@start `"`" `"$exe`" %*" -Encoding ascii
    $p = Start-Process -FilePath runas.exe -ArgumentList "/trustlevel:0x20000", "`"$cmd $arg`"" -PassThru -WindowStyle Hidden
    $p.WaitForExit(15000) | Out-Null
    return $p.ExitCode
}
function AlarmRect { $h = Get-AlarmHwnd; $r = New-Object SpikeW32+RECT; [SpikeW32]::GetWindowRect($h, [ref]$r) | Out-Null; return $r }

switch ($Phase) {
'env' {
    $os = Get-CimInstance Win32_OperatingSystem
    $admin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
    $sess = (Get-Process -Id $PID).SessionId
    Add-Type -AssemblyName System.Windows.Forms
    $scr = [System.Windows.Forms.Screen]::PrimaryScreen
    "# Spike results ($($os.Caption) $($os.Version))`n`n| Check | Result | Detail |`n|---|---|---|" | Set-Content (Join-Path $out 'results.md') -Encoding utf8
    Write-Result 'env' 'INFO' "user=$env:USERNAME sid=$sid admin=$admin session=$sess screen=$($scr.Bounds.Width)x$($scr.Bounds.Height) workArea=$($scr.WorkingArea) audiosrv=$((Get-Service Audiosrv).Status)/$((Get-Service Audiosrv).StartType) seclogon=$((Get-Service seclogon).Status)/$((Get-Service seclogon).StartType) dblclick=$([SpikeW32]::GetDoubleClickTime())"
    Write-Result 'uac' 'INFO' ("EnableLUA=" + (Get-ItemProperty 'HKLM:\SOFTWARE\Microsoft\Windows\CurrentVersion\Policies\System').EnableLUA)
    # Audio: start the services, then try a free, signed virtual device (Scream, MS-PL).
    foreach ($s in 'AudioEndpointBuilder', 'Audiosrv') { Set-Service $s -StartupType Automatic -ErrorAction SilentlyContinue; Start-Service $s -ErrorAction SilentlyContinue }
    try {
        $z = Join-Path $env:RUNNER_TEMP 'scream.zip'
        Invoke-WebRequest 'https://github.com/duncanthrax/scream/releases/download/4.0/Scream4.0.zip' -OutFile $z
        $d = Join-Path $env:RUNNER_TEMP 'scream'
        Expand-Archive $z -DestinationPath $d -Force
        Get-ChildItem $d -Recurse | Select-Object -ExpandProperty FullName | Set-Content (Join-Path $out 'scream-files.txt')
        $inf = Get-ChildItem $d -Recurse -Filter Scream.inf | Where-Object { $_.FullName -match 'x64' } | Select-Object -First 1
        $devcon = Get-ChildItem $d -Recurse -Filter 'devcon*.exe' | Where-Object { $_.Name -match 'x64|amd64' -or $_.FullName -match 'x64' } | Select-Object -First 1
        $certs = Get-ChildItem $d -Recurse -Include *.cer, *.crt
        foreach ($c in $certs) { certutil -f -addstore TrustedPublisher $c.FullName | Out-Null; certutil -f -addstore Root $c.FullName | Out-Null }
        $o = if ($devcon -and $inf) {
            # A driver prompt would block forever on a runner: give it 90 s, then give up.
            $p = Start-Process -FilePath $devcon.FullName -ArgumentList 'install', "`"$($inf.FullName)`"", '*Scream' -PassThru -NoNewWindow -RedirectStandardOutput (Join-Path $out 'devcon.txt')
            if (-not $p.WaitForExit(90000)) { $p | Stop-Process -Force; "devcon timed out after 90 s (a driver prompt?)" } else { "devcon exit $($p.ExitCode): " + (Get-Content (Join-Path $out 'devcon.txt') -Raw) }
        } else { "no devcon/inf: devcon=$devcon inf=$inf" }
        Write-Result 'audio-device' 'INFO' "Scream 4.0 (MS-PL): $o"
    } catch { Write-Result 'audio-device' 'INFO' "Scream failed: $_" }
    Restart-Service Audiosrv -Force -ErrorAction SilentlyContinue
    Start-Sleep 3
    $snd = Get-CimInstance Win32_SoundDevice | ForEach-Object { "$($_.Name) [$($_.Status)]" }
    Write-Result 'audio-endpoints' 'INFO' ("Win32_SoundDevice: " + ($snd -join '; '))
}
'install' {
    $p = Start-Process msiexec.exe -ArgumentList '/i', "`"$msiDir\TransfusionVS-1.0.0.msi`"", '/qn', '/l*v', "`"$out\install-1.0.0.log`"" -Wait -PassThru
    $run = (Get-ItemProperty 'HKLM:\SOFTWARE\Microsoft\Windows\CurrentVersion\Run' -ErrorAction SilentlyContinue).TransfusionVS
    Write-Result 'install' ($(if ($p.ExitCode -eq 0 -and (Test-Path $exe) -and $run) { 'PASS' } else { 'FAIL' })) "exit=$($p.ExitCode) exe=$(Test-Path $exe) HKLM Run='$run'"
    $acl = (Get-Acl 'C:\ProgramData\TransfusionVS' -ErrorAction SilentlyContinue).Access | ForEach-Object { "$($_.IdentityReference):$($_.FileSystemRights)" }
    Write-Result 'marker-folder-acl' 'INFO' ($acl -join '; ')
    Get-ChildItem 'C:\Program Files\TransfusionVS' -Recurse -File | Select-Object -ExpandProperty FullName | Set-Content (Join-Path $out 'installed-files.txt')
    Copy-Item 'C:\Program Files\TransfusionVS\app\TransfusionVS.cfg' $out -ErrorAction SilentlyContinue
}
'launch' {
    $rc = Launch '--logon'
    $t = Wait-Until { (Get-SpikeLog) -match 'TASK readback .*Backstop' } 90
    $log = Get-SpikeLog
    $start = ($log -split "`n" | Where-Object { $_ -match 'START args' } | Select-Object -Last 1)
    Write-Result 'standard-user' ($(if ($start -match 'admin=false') { 'PASS' } else { 'FAIL' })) "runas exit=$rc after ${t}s: $start"
    foreach ($n in $restartTask, $backstopTask) {
        $x = schtasks /query /tn $n /xml 2>&1 | Out-String
        $file = Join-Path $out ("task-" + ($n -replace '[^A-Za-z]', '') + ".xml"); $x | Set-Content $file
        $ok = $x -match 'InteractiveToken' -and $x -match '<MultipleInstancesPolicy>Parallel' -and $x -match '<ExecutionTimeLimit>PT0S' -and $x -match '<Priority>4' -and $x -match 'Z</StartBoundary>'
        Write-Result "task $n" ($(if ($ok) { 'PASS' } else { 'FAIL' })) (($log -split "`n" | Where-Object { $_ -match "TASK create '$n" }) -join ' ')
    }
    $req = powercfg /requests 2>&1 | Out-String
    $req | Set-Content (Join-Path $out 'powercfg-requests.txt')
    $sys = ($req -split 'DISPLAY:|SYSTEM:|AWAYMODE:|EXECUTION:|PERFBOOST:|ACTIVELOCKSCREEN:')
    Write-Result 'power-request' ($(if ($req -match 'TransfusionVS' -and $req -match 'Timers are still running') { 'PASS' } else { 'FAIL' })) (($log -split "`n" | Where-Object { $_ -match 'POWER' }) -join ' ')
    $mods = Get-Content (Join-Path $spike 'modules-start.txt') -ErrorAction SilentlyContinue
    Copy-Item (Join-Path $spike 'modules-start.txt') $out -ErrorAction SilentlyContinue
    $bad = $mods | Where-Object { $_ -notlike 'C:\Program Files\TransfusionVS\*' -and $_ -notlike "$env:SystemRoot\*" }
    Write-Result 'modules-install-folder' ($(if (-not $bad) { 'PASS' } else { 'FAIL' })) ("outside install folder and SystemRoot: " + ($bad -join '; '))
    $hidden = [SpikeW32]::FindWindow('TransfusionVS.Hidden', $null)
    Write-Result 'hidden-window' ($(if ($hidden -ne [IntPtr]::Zero -and -not [SpikeW32]::IsWindowVisible($hidden)) { 'PASS' } else { 'FAIL' })) "hwnd=$hidden visible=$([SpikeW32]::IsWindowVisible($hidden))"
    Send-Cmd 'audio'; Start-Sleep 8
    $a = (Get-SpikeLog) -split "`n" | Where-Object { $_ -match 'AUDIO' }
    Write-Result 'core-audio' 'INFO' ($a -join ' || ')
}
'clock' {
    # Trap 12: the clock moves only inside this step, in try/finally, each move <= 2 h; restored from a stopwatch.
    $sw = [Diagnostics.Stopwatch]::StartNew(); $t0 = Get-Date
    $before = (Get-SpikeLog).Length
    try {
        Set-Date -Date $t0.AddMinutes(30) | Out-Null
        Start-Sleep 4
    } finally {
        Set-Date -Date $t0.Add($sw.Elapsed) | Out-Null
        Restart-Service w32time -ErrorAction SilentlyContinue
    }
    Start-Sleep 2
    $new = (Get-SpikeLog).Substring($before)
    Write-Result 'msg WM_TIMECHANGE (Set-Date)' ($(if ($new -match 'WM_TIMECHANGE') { 'PASS' } else { 'FAIL' })) (($new -split "`n" | Where-Object { $_ -match 'MSG|ZONE' }) -join ' ')
    $tz = (tzutil /g)
    $before = (Get-SpikeLog).Length
    try { tzutil /s 'Nepal Standard Time'; Start-Sleep 4 } finally { tzutil /s $tz }
    Start-Sleep 3
    $new = (Get-SpikeLog).Substring($before)
    Write-Result 'msg tzutil' ($(if ($new -match 'WM_TIMECHANGE|WM_SETTINGCHANGE') { 'PASS' } else { 'FAIL' })) (($new -split "`n" | Where-Object { $_ -match 'MSG|ZONE' }) -join ' ')
    Write-Result 'clock step' 'INFO' "lasted $([int]$sw.Elapsed.TotalSeconds) s; time zone back to $(tzutil /g)"
}
'shutdown' {
    $h = [SpikeW32]::FindWindow('TransfusionVS.Hidden', $null)
    $reason = [SpikeW32]::Reason($h)
    $r1 = [SpikeW32]::Send($h, 0x11, 0, 0, 5000)
    $r2 = [SpikeW32]::Send($h, 0x11, 0, 0, 5000)
    $r3 = [SpikeW32]::Send($h, 0x11, 0, 1, 5000)
    Write-Result 'WM_QUERYENDSESSION refused once' ($(if ($r1 -eq 0 -and $r2 -eq 1) { 'PASS' } else { 'FAIL' })) "first=$r1 second=$r2"
    Write-Result 'ENDSESSION_CLOSEAPP accepted' ($(if ($r3 -eq 1) { 'PASS' } else { 'FAIL' })) "closeApp=$r3"
    Write-Result 'ShutdownBlockReasonQuery (other process)' ($(if ($reason -eq 'Timers are still running') { 'PASS' } else { 'INFO' })) "'$reason'; app's own query: $(((Get-SpikeLog) -split "`n" | Where-Object { $_ -match 'reasonQuery' } | Select-Object -First 1))"
    Send-Cmd 'timer-off'; Start-Sleep 1
    Write-Result 'reason gone after timer-off' ($(if ([SpikeW32]::Reason($h) -notmatch 'Timers') { 'PASS' } else { 'FAIL' })) ([SpikeW32]::Reason($h))
    Send-Cmd 'timer-on'
}
'alarm' {
    $fs = Start-Process pwsh -ArgumentList '-NoProfile', '-File', (Join-Path $PSScriptRoot 'fullscreen.ps1') -PassThru
    Start-Sleep 4
    $fsh = [SpikeW32]::FindWindow($null, 'SpikeFullscreen')
    $fg0 = [SpikeW32]::GetForegroundWindow()
    Send-Cmd 'alarm-show'; Start-Sleep 4
    $h = Get-AlarmHwnd; $r = AlarmRect
    $ex = [long][SpikeW32]::GetWindowLongPtr($h, -20)
    $cx = [int](($r.Left + $r.Right) / 2); $cy = [int](($r.Top + $r.Bottom) / 2)
    $root = [SpikeW32]::Root($cx, $cy)
    $fg1 = [SpikeW32]::GetForegroundWindow()
    Write-Result 'alarm styles' ($(if (($ex -band 0x8) -and ($ex -band 0x08000000) -and ($ex -band 0x40000)) { 'PASS' } else { 'FAIL' })) ("exstyle=0x{0:X} rect={1},{2},{3},{4}" -f $ex, $r.Left, $r.Top, $r.Right, $r.Bottom)
    Write-Result 'alarm above topmost full-screen' ($(if ($root -eq $h -and $fsh -ne [IntPtr]::Zero) { 'PASS' } else { 'FAIL' })) "root at centre=$root alarm=$h fullscreen=$fsh"
    Write-Result 'alarm did not take the foreground' ($(if ($fg1 -eq $fg0) { 'PASS' } else { 'FAIL' })) "before=$fg0 after=$fg1"
    Shot 'alarm-over-fullscreen'
    Write-Result 'alarm drew (software)' ($(if ((Colors-In $r.Left $r.Top $r.Right $r.Bottom) -gt 3) { 'PASS' } else { 'FAIL' })) "colours sampled=$(Colors-In $r.Left $r.Top $r.Right $r.Bottom)"
    $before = (Get-SpikeLog).Length
    Start-Sleep -Milliseconds 2200
    [SpikeW32]::Click($cx, $r.Bottom - 60) | Out-Null; Start-Sleep 1
    $new = (Get-SpikeLog).Substring($before)
    Write-Result 'click registers (SendInput)' ($(if ($new -match 'CLICK done') { 'PASS' } else { 'FAIL' })) (($new -split "`n" | Where-Object { $_ -match 'press|CLICK' }) -join ' ')
    # A slow double click (just inside the double-click time): does AWT report clickCount 2?
    $before = (Get-SpikeLog).Length
    [SpikeW32]::Click($cx, $r.Bottom - 60) | Out-Null; Start-Sleep -Milliseconds ([SpikeW32]::GetDoubleClickTime() - 60); [SpikeW32]::Click($cx, $r.Bottom - 60) | Out-Null
    Start-Sleep 1
    $new = (Get-SpikeLog).Substring($before)
    Write-Result 'double click seen as clickCount 2' ($(if ($new -match 'clickCount=2') { 'PASS' } else { 'FAIL' })) (($new -split "`n" | Where-Object { $_ -match 'press|CLICK' }) -join ' ')
    # Typing into Notepad while an alarm window is (re)created.
    $np = Start-Process notepad -PassThru; Start-Sleep 3
    [SpikeW32]::SetForegroundWindow($np.MainWindowHandle) | Out-Null; Start-Sleep 1
    $fgN = [SpikeW32]::GetForegroundWindow()
    [SpikeW32]::Type('the quick brown ') | Out-Null
    Send-Cmd 'alarm-recreate'
    [SpikeW32]::Type('fox jumps over the lazy dog') | Out-Null
    Start-Sleep 2
    $fgAfter = [SpikeW32]::GetForegroundWindow()
    $edit = [SpikeW32]::FindWindowEx($np.MainWindowHandle, [IntPtr]::Zero, 'Edit', $null)
    if ($edit -eq [IntPtr]::Zero) { $edit = [SpikeW32]::FindWindowEx($np.MainWindowHandle, [IntPtr]::Zero, 'RichEditD2DPT', $null) }
    $text = if ($edit -ne [IntPtr]::Zero) { [SpikeW32]::Title($edit) } else { '<no edit control found>' }
    Write-Result 'Notepad text complete while the alarm appears' ($(if ($text -match 'the quick brown fox jumps over the lazy dog' -and $fgAfter -eq $fgN) { 'PASS' } else { 'FAIL' })) "text='$text' foreground kept=$($fgAfter -eq $fgN)"
    Stop-Process $np -Force -ErrorAction SilentlyContinue
    # Survival: WM_CLOSE, SC_CLOSE, SC_MINIMIZE, Esc, Win+D, Win+M.
    $before = (Get-SpikeLog).Length
    $h = Get-AlarmHwnd
    [SpikeW32]::PostMessage($h, 0x10, [IntPtr]::Zero, [IntPtr]::Zero) | Out-Null
    [SpikeW32]::PostMessage($h, 0x112, [IntPtr]0xF060, [IntPtr]::Zero) | Out-Null
    [SpikeW32]::PostMessage($h, 0x112, [IntPtr]0xF020, [IntPtr]::Zero) | Out-Null
    [SpikeW32]::Chord(0x1B) | Out-Null; Start-Sleep 1
    [SpikeW32]::Chord(0x5B, 0x44) | Out-Null; Start-Sleep 2
    [SpikeW32]::Chord(0x5B, 0x4D) | Out-Null; Start-Sleep 2
    $h = Get-AlarmHwnd; $r = AlarmRect
    $cx = [int](($r.Left + $r.Right) / 2); $cy = [int](($r.Top + $r.Bottom) / 2)
    $state = "visible=$([SpikeW32]::IsWindowVisible($h)) iconic=$([SpikeW32]::IsIconic($h)) cloaked=$([SpikeW32]::Cloaked($h)) topmost=$((([long][SpikeW32]::GetWindowLongPtr($h, -20)) -band 8) -ne 0) root=$([SpikeW32]::Root($cx, $cy)) alarm=$h"
    $ok = [SpikeW32]::IsWindowVisible($h) -and -not [SpikeW32]::IsIconic($h) -and [SpikeW32]::Cloaked($h) -eq 0 -and [SpikeW32]::Root($cx, $cy) -eq $h
    $new = (Get-SpikeLog).Substring($before)
    Write-Result 'survives WM_CLOSE, SC_CLOSE, SC_MINIMIZE, Esc, Win+D, Win+M' ($(if ($ok) { 'PASS' } else { 'FAIL' })) "$state; log: $(($new -split "`n" | Where-Object { $_ -match 'ALARM' }) -join ' ')"
    Shot 'alarm-after-survival'
    # Virtual desktop switch: Win+Ctrl+D makes a new desktop; the alarm must come back on it, uncloaked.
    $before = (Get-SpikeLog).Length
    [SpikeW32]::Chord(0x5B, 0x11, 0x44) | Out-Null; Start-Sleep 4
    $h = Get-AlarmHwnd
    $new = (Get-SpikeLog).Substring($before)
    Shot 'alarm-new-virtual-desktop'
    Write-Result 'virtual desktop switch' ($(if ([SpikeW32]::IsWindowVisible($h) -and [SpikeW32]::Cloaked($h) -eq 0) { 'PASS' } else { 'FAIL' })) "cloaked=$([SpikeW32]::Cloaked($h)) visible=$([SpikeW32]::IsWindowVisible($h)); log: $(($new -split "`n" | Where-Object { $_ -match 'ALARM' }) -join ' ')"
    [SpikeW32]::Chord(0x5B, 0x11, 0x73) | Out-Null; Start-Sleep 2
    # Fight for the top: a topmost window re-asserting every 200 ms over the alarm; a click on Done within 15 s.
    Stop-Process $fs -Force -ErrorAction SilentlyContinue
    $fight = Start-Process pwsh -ArgumentList '-NoProfile', '-File', (Join-Path $PSScriptRoot 'fullscreen.ps1'), '-ReassertMs', '200' -PassThru
    Start-Sleep 3
    $before = (Get-SpikeLog).Length
    $sw = [Diagnostics.Stopwatch]::StartNew(); $clicked = $false
    while ($sw.Elapsed.TotalSeconds -lt 15 -and -not $clicked) {
        $h = Get-AlarmHwnd; $r = AlarmRect; $bx = [int](($r.Left + $r.Right) / 2); $by = $r.Bottom - 60
        if ([SpikeW32]::Root($bx, $by) -eq $h) { [SpikeW32]::Click($bx, $by) | Out-Null; Start-Sleep -Milliseconds 400; $clicked = ((Get-SpikeLog).Substring($before) -match 'CLICK done') }
        Start-Sleep -Milliseconds 300
    }
    Write-Result 'fight for the top (200 ms re-assert)' ($(if ($clicked) { 'PASS' } else { 'FAIL' })) "clicked within $([int]$sw.Elapsed.TotalSeconds) s"
    Stop-Process $fight -Force -ErrorAction SilentlyContinue
}
'render' {
    $cfg = 'C:\Program Files\TransfusionVS\app\TransfusionVS.cfg'
    foreach ($api in 'DIRECT3D', 'SOFTWARE_FAST') {
        Send-Cmd 'quit'; Start-Sleep 2
        (Get-Content $cfg) -replace 'skiko.renderApi=\w+', "skiko.renderApi=$api" | Set-Content $cfg
        Set-Content (Join-Path $spike 'alarm-at-start') '1'
        Launch '--logon' | Out-Null
        Start-Sleep 10
        $r = AlarmRect
        $n = Colors-In $r.Left $r.Top $r.Right $r.Bottom
        Shot "render-$api"
        Write-Result "render $api" ($(if ($n -gt 3) { 'PASS' } else { 'FAIL' })) "colours=$n; $(((Get-SpikeLog) -split "`n" | Where-Object { $_ -match 'ALARM decorated' } | Select-Object -Last 1))"
    }
}
'itmodes' {
    $rows = Join-Path $out 'setup-rows.txt'
    $p = Start-Process $exe -ArgumentList '--setup-check', "`"$rows`"" -Wait -PassThru
    Write-Result '--setup-check' ($(if ($p.ExitCode -eq 0 -and (Test-Path $rows)) { 'PASS' } else { 'FAIL' })) "exit=$($p.ExitCode) rows=$(if (Test-Path $rows) { (Get-Content $rows) -join ' / ' })"
    $t0 = Get-Date
    $p = Start-Process $exe -ArgumentList '--test-alarm' -Wait -PassThru
    $sec = Wait-Until { (Get-SpikeLog) -match 'TEST alarm shown' } 90
    Write-Result '--test-alarm' ($(if ($p.ExitCode -eq 0 -and $sec -ge 55 -and $sec -le 66) { 'PASS' } else { 'FAIL' })) "exit=$($p.ExitCode) shown after $([int]((Get-Date) - $t0).TotalSeconds) s"
}
'crash' {
    Send-Cmd 'crash'
    $gone = Wait-Until { (Get-AppProcs).Count -eq 0 } 20
    Start-Sleep 2
    $files = Get-ChildItem $data -File | Select-Object -ExpandProperty Name
    $dumps = Get-ChildItem $data, "$env:LOCALAPPDATA\CrashDumps" -Recurse -Include *.mdmp, *.dmp -ErrorAction SilentlyContinue
    Write-Result 'crash file in the data folder, no minidump' ($(if (($files -match '^hs_err_') -and -not $dumps) { 'PASS' } else { 'FAIL' })) "exited=$gone files=$($files -join ',') dumps=$($dumps.FullName -join ',')"
    $sec = Wait-Until { (Get-AppProcs).Count -gt 0 } 90
    Start-Sleep 8
    $left = Get-ChildItem $data -File -Filter 'hs_err_*' -ErrorAction SilentlyContinue
    Write-Result 'restart task relaunched after the crash; crash file deleted' ($(if ($sec -ge 0 -and $sec -le 75 -and -not $left) { 'PASS' } else { 'FAIL' })) "relaunched after $([int]$sec) s; left=$($left.Name -join ',')"
}
'restart' {
    Get-AppProcs | Stop-Process -Force
    $sec = Wait-Until { (Get-AppProcs).Count -gt 0 } 90
    Write-Result 'killed -> restart task relaunch' ($(if ($sec -ge 0 -and $sec -le 75) { 'PASS' } else { 'FAIL' })) "after $([int]$sec) s; $(((Get-SpikeLog) -split "`n" | Where-Object { $_ -match 'START args' } | Select-Object -Last 1))"
    Start-Sleep 10
    # Ping: a user start reaches a healthy instance and brings it forward.
    $before = (Get-SpikeLog).Length
    Launch '' | Out-Null; Start-Sleep 6
    $new = (Get-SpikeLog).Substring($before)
    Write-Result 'second instance (user start) answered and showed the main window' ($(if ($new -match 'ping answered=true' -and $new -match 'MAIN shown') { 'PASS' } else { 'FAIL' })) (($new -split "`n" | Where-Object { $_ -match 'SECOND|MSG (PING|SHOW)|MAIN' }) -join ' ')
    # Takeover: the UI thread blocked; a --watchdog launch after 35 s ends it and takes over.
    $before = (Get-SpikeLog).Length
    $old = (Get-AppProcs | Sort-Object StartTime | Select-Object -First 1).Id
    Send-Cmd 'block-ui 60'
    Start-Sleep 36
    Launch '--watchdog' | Out-Null
    Start-Sleep 15
    $new = (Get-SpikeLog).Substring($before)
    $alive = Get-Process -Id $old -ErrorAction SilentlyContinue
    Write-Result 'takeover of a hung instance' ($(if ($new -match 'took over' -and -not $alive) { 'PASS' } else { 'FAIL' })) (($new -split "`n" | Where-Object { $_ -match 'SECOND|PING' }) -join ' ')
}
'upgrade' {
    $before = (Get-SpikeLog).Length
    $p = Start-Process msiexec.exe -ArgumentList '/i', "`"$msiDir\TransfusionVS-1.0.1.msi`"", '/qn', '/l*v', "`"$out\upgrade-1.0.1.log`"" -Wait -PassThru
    $ver = (Get-ItemProperty 'HKLM:\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\*' | Where-Object { $_.DisplayName -eq 'A S' }).DisplayVersion
    Write-Result 'upgrade over a running app with a timer' ($(if ($p.ExitCode -eq 0) { 'PASS' } else { 'FAIL' })) "exit=$($p.ExitCode) (3010 = reboot needed) installed=$ver"
    Copy-Item 'C:\ProgramData\TransfusionVS\spike-installer.log' $out -ErrorAction SilentlyContinue
    $new = (Get-SpikeLog).Substring($before)
    Write-Result 'upgrade: messages and marker' 'INFO' ("marker after=$(Test-Path 'C:\ProgramData\TransfusionVS\installing'); log: " + (($new -split "`n" | Where-Object { $_ -match 'MSG|MARKER|EXIT' }) -join ' '))
    $sec = Wait-Until { (Get-AppProcs).Count -gt 0 } 90
    Write-Result 'app back after the upgrade' ($(if ($sec -ge 0) { 'PASS' } else { 'FAIL' })) "after $([int]$sec) s"
}
'failure' {
    $p = Start-Process msiexec.exe -ArgumentList '/i', "`"$msiDir\TransfusionVS-1.0.2.msi`"", 'SPIKE_FAIL=1', '/qn', '/l*v', "`"$out\failed-1.0.2.log`"" -Wait -PassThru
    $marker = Test-Path 'C:\ProgramData\TransfusionVS\installing'
    $sec = Wait-Until { (Get-AppProcs).Count -gt 0 } 120
    Write-Result 'forced failing install leaves no marker; app back' ($(if ($p.ExitCode -ne 0 -and -not $marker -and $sec -ge 0) { 'PASS' } else { 'FAIL' })) "exit=$($p.ExitCode) marker=$marker app after $([int]$sec) s"
    $p = Start-Process msiexec.exe -ArgumentList '/i', "`"$msiDir\TransfusionVS-1.0.2.msi`"", '/qn', '/l*v', "`"$out\killed-1.0.2.log`"" -PassThru
    $seen = Wait-Until { Test-Path 'C:\ProgramData\TransfusionVS\installing' } 120
    Get-Process msiexec -ErrorAction SilentlyContinue | Stop-Process -Force
    Start-Sleep 3
    $content = Get-Content 'C:\ProgramData\TransfusionVS\installing' -ErrorAction SilentlyContinue
    Restart-Service msiserver -Force -ErrorAction SilentlyContinue
    $before = (Get-SpikeLog).Length
    if (Test-Path $exe) { Launch '--logon' | Out-Null }
    $sec = Wait-Until { (Get-AppProcs).Count -gt 0 } 120
    Start-Sleep 5
    $new = (Get-SpikeLog).Substring($before)
    Write-Result 'msiexec killed after --installer-stop: stale marker ignored' ($(if ($sec -ge 0 -and $new -notmatch 'marker valid') { 'PASS' } else { 'FAIL' })) "marker seen after $([int]$seen) s, content='$($content -join ';')', exe present=$(Test-Path $exe), app after $([int]$sec) s"
}
'uninstall' {
    Send-Cmd 'quit'; Start-Sleep 2
    $rows = Join-Path $out 'setup-rows-none.txt'
    if (Test-Path $exe) {
        $p = Start-Process $exe -ArgumentList '--setup-check', "`"$rows`"" -Wait -PassThru
        Write-Result '--setup-check with no instance' ($(if ($p.ExitCode -eq 2 -and (Get-Content $rows -Raw) -match 'not running') { 'PASS' } else { 'FAIL' })) "exit=$($p.ExitCode) '$((Get-Content $rows -Raw -ErrorAction SilentlyContinue))'"
    }
    Launch '--logon' | Out-Null; Start-Sleep 10
    $code = (Get-ItemProperty 'HKLM:\SOFTWARE\Microsoft\Windows\CurrentVersion\Uninstall\*' | Where-Object { $_.DisplayName -eq 'A S' }).PSChildName
    $p = Start-Process msiexec.exe -ArgumentList '/x', $code, '/qn', '/l*v', "`"$out\uninstall.log`"" -Wait -PassThru
    Start-Sleep 3
    $tasks = schtasks /query /fo csv /nh 2>$null | Where-Object { $_ -match '"\\TransfusionVS ' }
    $run = (Get-ItemProperty 'HKLM:\SOFTWARE\Microsoft\Windows\CurrentVersion\Run' -ErrorAction SilentlyContinue).TransfusionVS
    $procs = Get-AppProcs
    Write-Result 'uninstall while running' ($(if ($p.ExitCode -eq 0 -and -not $tasks -and -not $run -and $procs.Count -eq 0) { 'PASS' } else { 'FAIL' })) "exit=$($p.ExitCode) tasks=$($tasks -join ';') run='$run' processes=$($procs.Count) dataFolderKept=$(Test-Path $data)"
    Copy-Item 'C:\ProgramData\TransfusionVS\spike-installer.log' $out -ErrorAction SilentlyContinue
}
}
Copy-Item (Join-Path $spike 'log.txt') (Join-Path $out 'app-log.txt') -Force -ErrorAction SilentlyContinue
