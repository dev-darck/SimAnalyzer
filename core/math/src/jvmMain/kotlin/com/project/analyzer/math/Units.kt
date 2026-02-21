package com.project.analyzer.math

public data class Units(
    val speed: SpeedUnit,
    val pressure: PressureUnit,
    val temperature: TemperatureUnit,
    val distance: DistanceUnit,
) {

    public companion object {

        public val Default: Units = Units(
            speed = SpeedUnit.KMH,
            pressure = PressureUnit.KPA,
            temperature = TemperatureUnit.C,
            distance = DistanceUnit.M,
        )
    }
}

public enum class SpeedUnit {
    KMH,
    MPS,
}

public enum class PressureUnit {
    KPA,
    PSI,
}

public enum class TemperatureUnit {
    C,
    F,
}

public enum class DistanceUnit {
    M,
    KM,
}
