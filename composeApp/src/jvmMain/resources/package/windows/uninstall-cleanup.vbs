Sub Main()
    Dim shell, fso, roaming, local, res, appName
    Set shell = CreateObject("WScript.Shell")
    Set fso = CreateObject("Scripting.FileSystemObject")

    ' The appName should match the packageName in nativeDistributions or BuildConfig.APP_NAME
    appName = "SimAnalyzer"
    roaming = shell.ExpandEnvironmentStrings("%APPDATA%") & "\" & appName
    local = shell.ExpandEnvironmentStrings("%LOCALAPPDATA%") & "\" & appName

    res = MsgBox("Would you like to delete SimAnalyzer user data (settings, cache, logs)?", 4 + 32, "SimAnalyzer Uninstall")

    If res = 6 Then ' 6 is vbYes
        On Error Resume Next
        If fso.FolderExists(roaming) Then
            fso.DeleteFolder roaming, True
        End If
        If fso.FolderExists(local) Then
            fso.DeleteFolder local, True
        End If
        On Error GoTo 0
    End If
End Sub
