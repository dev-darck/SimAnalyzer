package com.project.analyzer.telemetry.lmu.impl.mapper

import com.project.analyzer.telemetry.lmu.api.model.LmuScoringInfo
import com.project.analyzer.telemetry.lmu.impl.shm.structure.Rf2ScoringInfo
import com.project.analyzer.utils.shm.toCString

internal fun Rf2ScoringInfo.toModel(): LmuScoringInfo {
    return LmuScoringInfo(
        trackName = trackName.toCString(),
        session = session,
        currentEt = currentEt,
        endEt = endEt,
        maxLaps = maxLaps,
        lapDist = lapDist,
        gamePhase = gamePhase.toUnsignedInt(),
        yellowFlagState = yellowFlagState.toInt(),
        sectorFlag = sectorFlag.map { it.toInt() }.toIntArray(),
        startLight = startLight.toUnsignedInt(),
        numRedLights = numRedLights.toUnsignedInt(),
        inRealtime = inRealtime.toBoolean(),
        playerName = playerName.toCString(),
        plrFileName = plrFileName.toCString(),
        darkCloud = darkCloud,
        raining = raining,
        ambientTemp = ambientTemp,
        trackTemp = trackTemp,
        wind = wind.toVec3(),
        minPathWetness = minPathWetness,
        maxPathWetness = maxPathWetness,
        gameMode = gameMode.toUnsignedInt(),
        isPasswordProtected = isPasswordProtected.toBoolean(),
        serverPort = serverPort.toUnsignedInt(),
        serverPublicIP = serverPublicIP.toUnsignedLong(),
        maxPlayers = maxPlayers,
        serverName = serverName.toCString(),
        startEt = startEt,
        avgPathWetness = avgPathWetness,
        expansion = expansion.copyOf()
    )
}
