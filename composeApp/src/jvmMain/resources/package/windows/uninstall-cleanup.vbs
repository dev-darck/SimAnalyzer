Sub Main()
    Dim shell, fso, roaming, legacyLocal, dataRoot, res, appName, registryPath
    Set shell = CreateObject("WScript.Shell")
    Set fso = CreateObject("Scripting.FileSystemObject")

    appName = "SimAnalyzer"
    registryPath = "HKCU\Software\" & appName & "\DataRoot"
    roaming = shell.ExpandEnvironmentStrings("%APPDATA%") & "\" & appName
    legacyLocal = shell.ExpandEnvironmentStrings("%LOCALAPPDATA%") & "\" & appName
    dataRoot = ReadRegistryValue(shell, registryPath)

    If dataRoot = "" Then
        dataRoot = roaming
    End If

    res = MsgBox("Would you like to delete SimAnalyzer user data (settings, cache, logs)?", 4 + 32, "SimAnalyzer Uninstall")

    If res = 6 Then ' 6 is vbYes
        DeleteFolderIfExists fso, dataRoot
        If LCase(legacyLocal) <> LCase(dataRoot) Then
            DeleteFolderIfExists fso, legacyLocal
        End If
    End If
End Sub

Private Sub DeleteFolderIfExists(ByVal fso, ByVal path)
    On Error Resume Next
    If fso.FolderExists(path) Then
        fso.DeleteFolder path, True
    End If
    On Error GoTo 0
End Sub

Private Function ReadRegistryValue(ByVal shell, ByVal keyPath)
    On Error Resume Next
    ReadRegistryValue = shell.RegRead(keyPath)
    If Err.Number <> 0 Then
        ReadRegistryValue = ""
        Err.Clear
    End If
    On Error GoTo 0
End Function
