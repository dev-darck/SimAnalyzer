package com.project.analyzer.ui.icons

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.vector.ImageVector

public val Icons.Filled.System: ImageVector
    get() {
        if (_System != null) {
            return _System!!
        }
        _System = materialIcon(name = "Filled.System") {
            // Viewport: 22.0w x 22.0h (for info)
            materialPath(pathFillType = PathFillType.NonZero) {
                moveTo(11.0f, 0.0f)
                curveTo(8.82441f, 0.0f, 6.69767f, 0.645139f, 4.88873f, 1.85383f)
                curveTo(3.07979f, 3.06253f, 1.66989f, 4.78049f, 0.83733f, 6.79048f)
                curveTo(0.00476613f, 8.80047f, -0.213071f, 11.0122f, 0.211367f, 13.146f)
                curveTo(0.635804f, 15.2798f, 1.68345f, 17.2398f, 3.22183f, 18.7782f)
                curveTo(4.76021f, 20.3165f, 6.72022f, 21.3642f, 8.85401f, 21.7886f)
                curveTo(10.9878f, 22.2131f, 13.1995f, 21.9952f, 15.2095f, 21.1627f)
                curveTo(17.2195f, 20.3301f, 18.9375f, 18.9202f, 20.1462f, 17.1113f)
                curveTo(21.3549f, 15.3023f, 22.0f, 13.1756f, 22.0f, 11.0f)
                curveTo(21.9969f, 8.08356f, 20.837f, 5.28746f, 18.7748f, 3.22523f)
                curveTo(16.7125f, 1.16299f, 13.9164f, 0.00307981f, 11.0f, 0.0f)
                moveTo(1.69231f, 11.0f)
                curveTo(1.69511f, 8.5323f, 2.67664f, 6.16648f, 4.42156f, 4.42156f)
                curveTo(6.16649f, 2.67663f, 8.53231f, 1.69511f, 11.0f, 1.69231f)
                verticalLineTo(20.3077f)
                curveTo(8.53231f, 20.3049f, 6.16649f, 19.3234f, 4.42156f, 17.5784f)
                curveTo(2.67664f, 15.8335f, 1.69511f, 13.4677f, 1.69231f, 11.0f)
            }
        }
        return _System!!
    }

private var _System: ImageVector? = null
