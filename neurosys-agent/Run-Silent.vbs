Set WshShell = CreateObject("WScript.Shell")
If WScript.Arguments.Count > 0 Then
    WshShell.Run "cmd.exe /c """ & WScript.Arguments(0) & """", 0, False
End If
