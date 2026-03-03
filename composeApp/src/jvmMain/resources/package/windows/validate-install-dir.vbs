Function ValidateInstallDir()
    Const ForWriting = 2

    Dim fso, installDir, probeParent, probeFolder, probeFile
    Set fso = CreateObject("Scripting.FileSystemObject")

    installDir = Trim(Session.Property("INSTALLDIR"))
    Session.Property("SIMANALYZER_INSTALLDIR_WRITABLE") = ""

    If installDir = "" Then
        Session.Log "SimAnalyzer: INSTALLDIR is empty"
        ValidateInstallDir = 1
        Exit Function
    End If

    If Right(installDir, 1) = "\" Then
        installDir = Left(installDir, Len(installDir) - 1)
    End If

    probeParent = ExistingFolderForPath(fso, installDir)
    If probeParent = "" Then
        Session.Log "SimAnalyzer: no existing parent for INSTALLDIR " & installDir
        ValidateInstallDir = 1
        Exit Function
    End If

    probeFolder = probeParent & "\.simanalyzer-install-test-" & Replace(Replace(CStr(Timer), ".", ""), ",", "")
    probeFile = probeFolder & "\probe.tmp"

    On Error Resume Next

    fso.CreateFolder probeFolder
    If Err.Number <> 0 Then
        Session.Log "SimAnalyzer: failed to create probe folder in " & probeParent & " error=" & Err.Number
        Err.Clear
        ValidateInstallDir = 1
        Exit Function
    End If

    With fso.OpenTextFile(probeFile, ForWriting, True)
        .WriteLine "probe"
        .Close
    End With

    If Err.Number <> 0 Then
        Session.Log "SimAnalyzer: failed to write probe file in " & probeFolder & " error=" & Err.Number
        Err.Clear
        On Error Resume Next
        If fso.FolderExists(probeFolder) Then
            fso.DeleteFolder probeFolder, True
        End If
        ValidateInstallDir = 1
        Exit Function
    End If

    If fso.FileExists(probeFile) Then
        fso.DeleteFile probeFile, True
    End If
    If fso.FolderExists(probeFolder) Then
        fso.DeleteFolder probeFolder, True
    End If

    On Error GoTo 0
    Session.Property("SIMANALYZER_INSTALLDIR_WRITABLE") = "1"
    ValidateInstallDir = 1
End Function

Private Function ExistingFolderForPath(ByVal fso, ByVal path)
    Dim current
    current = path

    Do While Len(current) > 0
        If fso.FolderExists(current) Then
            ExistingFolderForPath = current
            Exit Function
        End If

        If Right(current, 1) = "\" Then
            current = Left(current, Len(current) - 1)
        End If

        current = fso.GetParentFolderName(current)
    Loop

    ExistingFolderForPath = ""
End Function
