# Spike only: Win32 calls for the checks (window facts, SendInput, messages).
if (-not ("SpikeW32" -as [type])) {
Add-Type -TypeDefinition @"
using System;
using System.Runtime.InteropServices;
using System.Text;
public static class SpikeW32 {
  [StructLayout(LayoutKind.Sequential)] public struct RECT { public int Left, Top, Right, Bottom; }
  [StructLayout(LayoutKind.Sequential)] public struct POINT { public int X, Y; public POINT(int x, int y) { X = x; Y = y; } }
  [StructLayout(LayoutKind.Sequential)] public struct MOUSEINPUT { public int dx; public int dy; public uint mouseData; public uint dwFlags; public uint time; public IntPtr dwExtraInfo; }
  [StructLayout(LayoutKind.Sequential)] public struct KEYBDINPUT { public ushort wVk; public ushort wScan; public uint dwFlags; public uint time; public IntPtr dwExtraInfo; }
  [StructLayout(LayoutKind.Explicit)] public struct InputUnion { [FieldOffset(0)] public MOUSEINPUT mi; [FieldOffset(0)] public KEYBDINPUT ki; }
  [StructLayout(LayoutKind.Sequential)] public struct INPUT { public uint type; public InputUnion u; }

  [DllImport("user32.dll", CharSet = CharSet.Unicode)] public static extern IntPtr FindWindow(string cls, string title);
  [DllImport("user32.dll", CharSet = CharSet.Unicode)] public static extern IntPtr FindWindowEx(IntPtr parent, IntPtr after, string cls, string title);
  [DllImport("user32.dll")] public static extern IntPtr SendMessageTimeout(IntPtr h, uint m, IntPtr w, IntPtr l, uint f, uint t, out IntPtr r);
  [DllImport("user32.dll")] public static extern bool PostMessage(IntPtr h, uint m, IntPtr w, IntPtr l);
  [DllImport("user32.dll", CharSet = CharSet.Unicode)] public static extern IntPtr SendMessage(IntPtr h, uint m, IntPtr w, StringBuilder l);
  [DllImport("user32.dll", CharSet = CharSet.Unicode)] public static extern bool ShutdownBlockReasonQuery(IntPtr h, StringBuilder b, ref uint n);
  [DllImport("user32.dll")] public static extern IntPtr GetForegroundWindow();
  [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr h);
  [DllImport("user32.dll")] public static extern bool GetWindowRect(IntPtr h, out RECT r);
  [DllImport("user32.dll")] public static extern IntPtr WindowFromPoint(POINT p);
  [DllImport("user32.dll")] public static extern IntPtr GetAncestor(IntPtr h, uint f);
  [DllImport("user32.dll")] public static extern bool IsIconic(IntPtr h);
  [DllImport("user32.dll")] public static extern bool IsWindowVisible(IntPtr h);
  [DllImport("user32.dll", EntryPoint = "GetWindowLongPtrW")] public static extern IntPtr GetWindowLongPtr(IntPtr h, int i);
  [DllImport("dwmapi.dll")] public static extern int DwmGetWindowAttribute(IntPtr h, int a, out int v, int s);
  [DllImport("user32.dll", SetLastError = true)] public static extern uint SendInput(uint n, INPUT[] i, int s);
  [DllImport("user32.dll")] public static extern bool SetCursorPos(int x, int y);
  [DllImport("user32.dll")] public static extern uint GetDoubleClickTime();
  [DllImport("user32.dll")] public static extern int GetWindowThreadProcessId(IntPtr h, out int pid);
  [DllImport("ntdll.dll")] public static extern int NtSuspendProcess(IntPtr h);
  [DllImport("ntdll.dll")] public static extern int NtResumeProcess(IntPtr h);

  public static string Reason(IntPtr h) {
    uint n = 512; var sb = new StringBuilder((int)n);
    bool ok = ShutdownBlockReasonQuery(h, sb, ref n);
    return ok ? sb.ToString() : ("<none, error " + Marshal.GetLastWin32Error() + ">");
  }
  public static IntPtr Send(IntPtr h, uint m, long w, long l, uint timeout) {
    IntPtr r; IntPtr ok = SendMessageTimeout(h, m, new IntPtr(w), new IntPtr(l), 2, timeout, out r);
    return ok == IntPtr.Zero ? new IntPtr(-1) : r;
  }
  static INPUT Mouse(uint flags) { var i = new INPUT(); i.type = 0; i.u.mi.dwFlags = flags; return i; }
  static INPUT Key(ushort vk, ushort scan, uint flags) { var i = new INPUT(); i.type = 1; i.u.ki.wVk = vk; i.u.ki.wScan = scan; i.u.ki.dwFlags = flags; return i; }
  public static uint Click(int x, int y) {
    SetCursorPos(x, y);
    return SendInput(2, new[] { Mouse(0x0002), Mouse(0x0004) }, Marshal.SizeOf(typeof(INPUT)));
  }
  public static uint Chord(params ushort[] vks) {
    var list = new System.Collections.Generic.List<INPUT>();
    foreach (var v in vks) list.Add(Key(v, 0, 0));
    for (int k = vks.Length - 1; k >= 0; k--) list.Add(Key(vks[k], 0, 2));
    return SendInput((uint)list.Count, list.ToArray(), Marshal.SizeOf(typeof(INPUT)));
  }
  public static uint Type(string s) {
    var list = new System.Collections.Generic.List<INPUT>();
    foreach (char c in s) { list.Add(Key(0, c, 4)); list.Add(Key(0, c, 4 | 2)); }
    return SendInput((uint)list.Count, list.ToArray(), Marshal.SizeOf(typeof(INPUT)));
  }
  public static string Title(IntPtr h) { var sb = new StringBuilder(512); SendMessage(h, 0x000D, new IntPtr(512), sb); return sb.ToString(); }
  public static IntPtr Root(int x, int y) { return GetAncestor(WindowFromPoint(new POINT(x, y)), 2); }
  public static int Cloaked(IntPtr h) { int v; DwmGetWindowAttribute(h, 14, out v, 4); return v; }
}
"@
}

function Write-Result([string]$id, [string]$verdict, [string]$detail) {
    $line = "| $id | $verdict | $($detail -replace '\|','/' -replace "`r?`n",' ') |"
    Add-Content -Path (Join-Path $env:SPIKE_OUT 'results.md') -Value $line -Encoding utf8
    Write-Host "RESULT $id $verdict $detail"
}

function Send-Cmd([string]$c) {
    $dir = Join-Path $env:LOCALAPPDATA 'TransfusionVS\spike'
    New-Item -ItemType Directory -Force $dir | Out-Null
    $f = Join-Path $dir ("cmd-{0:D13}.txt" -f [DateTimeOffset]::UtcNow.ToUnixTimeMilliseconds())
    Set-Content -Path $f -Value $c -Encoding ascii
    Start-Sleep -Milliseconds 800
}

function Get-SpikeLog { $p = Join-Path $env:LOCALAPPDATA 'TransfusionVS\spike\log.txt'; if (Test-Path $p) { Get-Content $p -Raw } else { '' } }
function Get-AlarmHwnd { $p = Join-Path $env:LOCALAPPDATA 'TransfusionVS\spike\alarm-hwnd.txt'; if (Test-Path $p) { [IntPtr][long](Get-Content $p -Raw).Trim() } else { [IntPtr]::Zero } }
function Get-AppProcs { @(Get-Process -Name TransfusionVS -ErrorAction SilentlyContinue) }
function Wait-Until([scriptblock]$cond, [int]$seconds) {
    $sw = [Diagnostics.Stopwatch]::StartNew()
    while ($sw.Elapsed.TotalSeconds -lt $seconds) { if (& $cond) { return $sw.Elapsed.TotalSeconds } ; Start-Sleep -Milliseconds 250 }
    return -1
}
