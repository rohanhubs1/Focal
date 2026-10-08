package com.focal.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object FocalIcons {
    val Logo: ImageVector by lazy {
        ImageVector.Builder(
            name = "FocalLogo",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Outer focus ring
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f
            ) {
                moveTo(12f, 2f)
                curveTo(17.52f, 2f, 22f, 6.48f, 22f, 12f)
                curveTo(22f, 17.52f, 17.52f, 22f, 12f, 22f)
                curveTo(6.48f, 22f, 2f, 17.52f, 2f, 12f)
                curveTo(2f, 6.48f, 6.48f, 2f, 12f, 2f)
                close()
            }
            // Inner core target
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 8f)
                curveTo(14.21f, 8f, 16f, 9.79f, 16f, 12f)
                curveTo(16f, 14.21f, 14.21f, 16f, 12f, 16f)
                curveTo(9.79f, 16f, 8f, 14.21f, 8f, 12f)
                curveTo(8f, 9.79f, 9.79f, 8f, 12f, 8f)
                close()
            }
            // Aperture cross lines
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.5f
            ) {
                moveTo(12f, 2f)
                lineTo(12f, 6f)
                moveTo(12f, 18f)
                lineTo(12f, 22f)
                moveTo(2f, 12f)
                lineTo(6f, 12f)
                moveTo(18f, 12f)
                lineTo(22f, 12f)
            }
        }.build()
    }

    val Nfc: ImageVector by lazy {
        ImageVector.Builder(
            name = "Nfc",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f
            ) {
                moveTo(4f, 20f)
                curveTo(4f, 11.16f, 11.16f, 4f, 20f, 4f)
                moveTo(4f, 15f)
                curveTo(4f, 8.93f, 8.93f, 4f, 15f, 4f)
                moveTo(4f, 10f)
                curveTo(4f, 6.69f, 6.69f, 4f, 10f, 4f)
            }
            path(fill = SolidColor(Color.White)) {
                moveTo(5f, 19f)
                curveTo(5.55f, 19f, 6f, 19.45f, 6f, 20f)
                curveTo(6f, 20.55f, 5.55f, 21f, 5f, 21f)
                curveTo(4.45f, 21f, 4f, 20.55f, 4f, 20f)
                curveTo(4f, 19.45f, 4.45f, 19f, 5f, 19f)
                close()
            }
        }.build()
    }

    val QrCode: ImageVector by lazy {
        ImageVector.Builder(
            name = "QrCode",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                // Top-left finder
                moveTo(3f, 3f)
                lineTo(9f, 3f)
                lineTo(9f, 9f)
                lineTo(3f, 9f)
                close()
                moveTo(5f, 5f)
                lineTo(7f, 5f)
                lineTo(7f, 7f)
                lineTo(5f, 7f)
                close()
                // Top-right finder
                moveTo(15f, 3f)
                lineTo(21f, 3f)
                lineTo(21f, 9f)
                lineTo(15f, 9f)
                close()
                moveTo(17f, 5f)
                lineTo(19f, 5f)
                lineTo(19f, 7f)
                lineTo(17f, 7f)
                close()
                // Bottom-left finder
                moveTo(3f, 15f)
                lineTo(9f, 15f)
                lineTo(9f, 21f)
                lineTo(3f, 21f)
                close()
                moveTo(5f, 17f)
                lineTo(7f, 17f)
                lineTo(7f, 19f)
                lineTo(5f, 19f)
                close()
                // Data modules
                moveTo(11f, 3f)
                lineTo(13f, 3f)
                lineTo(13f, 5f)
                lineTo(11f, 5f)
                close()
                moveTo(11f, 11f)
                lineTo(13f, 11f)
                lineTo(13f, 13f)
                lineTo(11f, 13f)
                close()
                moveTo(15f, 15f)
                lineTo(17f, 15f)
                lineTo(17f, 17f)
                lineTo(15f, 17f)
                close()
                moveTo(19f, 19f)
                lineTo(21f, 19f)
                lineTo(21f, 21f)
                lineTo(19f, 21f)
                close()
                moveTo(15f, 19f)
                lineTo(17f, 19f)
                lineTo(17f, 21f)
                lineTo(15f, 21f)
                close()
            }
        }.build()
    }

    val Shield: ImageVector by lazy {
        ImageVector.Builder(
            name = "Shield",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f
            ) {
                moveTo(12f, 2f)
                lineTo(4f, 5f)
                verticalLineTo(11f)
                curveTo(4f, 16.5f, 7.5f, 21.3f, 12f, 22.5f)
                curveTo(16.5f, 21.3f, 20f, 16.5f, 20f, 11f)
                verticalLineTo(5f)
                lineTo(12f, 2f)
                close()
            }
        }.build()
    }

    val Fire: ImageVector by lazy {
        ImageVector.Builder(
            name = "Fire",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 2f)
                curveTo(12f, 2f, 8f, 7f, 8f, 11f)
                curveTo(8f, 13.2f, 9.8f, 15f, 12f, 15f)
                curveTo(14.2f, 15f, 16f, 13.2f, 16f, 11f)
                curveTo(16f, 7f, 12f, 2f, 12f, 2f)
                close()
                moveTo(12f, 22f)
                curveTo(7.6f, 22f, 4f, 18.4f, 4f, 14f)
                curveTo(4f, 9.5f, 8.5f, 5.5f, 9f, 5f)
                curveTo(8.7f, 6.8f, 9.3f, 8.5f, 10.5f, 9.7f)
                curveTo(12.3f, 7.7f, 14.5f, 5.7f, 15f, 5f)
                curveTo(15.5f, 5.5f, 20f, 9.5f, 20f, 14f)
                curveTo(20f, 18.4f, 16.4f, 22f, 12f, 22f)
                close()
            }
        }.build()
    }

    val Timer: ImageVector by lazy {
        ImageVector.Builder(
            name = "Timer",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f
            ) {
                moveTo(12f, 3f)
                curveTo(16.97f, 3f, 21f, 7.03f, 21f, 12f)
                curveTo(21f, 16.97f, 16.97f, 21f, 12f, 21f)
                curveTo(7.03f, 21f, 3f, 16.97f, 3f, 12f)
                curveTo(3f, 7.03f, 7.03f, 3f, 12f, 3f)
                close()
                moveTo(12f, 7f)
                lineTo(12f, 12f)
                lineTo(15.5f, 14f)
            }
        }.build()
    }

    val Users: ImageVector by lazy {
        ImageVector.Builder(
            name = "Users",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f
            ) {
                // Main user head
                moveTo(9f, 6f)
                curveTo(10.66f, 6f, 12f, 7.34f, 12f, 9f)
                curveTo(12f, 10.66f, 10.66f, 12f, 9f, 12f)
                curveTo(7.34f, 12f, 6f, 10.66f, 6f, 9f)
                curveTo(6f, 7.34f, 7.34f, 6f, 9f, 6f)
                close()
                // Main user shoulders
                moveTo(3f, 18f)
                curveTo(3f, 15f, 6f, 14f, 9f, 14f)
                curveTo(12f, 14f, 15f, 15f, 15f, 18f)
                // Second user
                moveTo(16f, 8f)
                curveTo(17.1f, 8f, 18f, 8.9f, 18f, 10f)
                curveTo(18f, 11.1f, 17.1f, 12f, 16f, 12f)
                moveTo(17f, 14.5f)
                curveTo(19f, 15.2f, 20f, 16.5f, 20f, 18f)
            }
        }.build()
    }

    val Chart: ImageVector by lazy {
        ImageVector.Builder(
            name = "Chart",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f
            ) {
                moveTo(3f, 3f)
                lineTo(3f, 21f)
                lineTo(21f, 21f)
                moveTo(7f, 16f)
                lineTo(7f, 12f)
                moveTo(12f, 16f)
                lineTo(12f, 8f)
                moveTo(17f, 16f)
                lineTo(17f, 5f)
            }
        }.build()
    }

    val Bell: ImageVector by lazy {
        ImageVector.Builder(
            name = "Bell",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f
            ) {
                moveTo(18f, 8f)
                curveTo(18f, 4.69f, 15.31f, 2f, 12f, 2f)
                curveTo(8.69f, 2f, 6f, 4.69f, 6f, 8f)
                curveTo(6f, 15f, 3f, 17f, 3f, 17f)
                lineTo(21f, 17f)
                curveTo(21f, 17f, 18f, 15f, 18f, 8f)
                close()
                moveTo(10.3f, 20f)
                curveTo(10.7f, 20.6f, 11.3f, 21f, 12f, 21f)
                curveTo(12.7f, 21f, 13.3f, 20.6f, 13.7f, 20f)
            }
        }.build()
    }

    val Add: ImageVector by lazy {
        ImageVector.Builder(
            name = "Add",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.5f
            ) {
                moveTo(12f, 5f)
                lineTo(12f, 19f)
                moveTo(5f, 12f)
                lineTo(19f, 12f)
            }
        }.build()
    }

    val Check: ImageVector by lazy {
        ImageVector.Builder(
            name = "Check",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.5f
            ) {
                moveTo(4f, 12f)
                lineTo(9f, 17f)
                lineTo(20f, 6f)
            }
        }.build()
    }

    val Close: ImageVector by lazy {
        ImageVector.Builder(
            name = "Close",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.5f
            ) {
                moveTo(6f, 6f)
                lineTo(18f, 18f)
                moveTo(6f, 18f)
                lineTo(18f, 6f)
            }
        }.build()
    }

    val ArrowBack: ImageVector by lazy {
        ImageVector.Builder(
            name = "ArrowBack",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.5f
            ) {
                moveTo(19f, 12f)
                lineTo(5f, 12f)
                moveTo(12f, 19f)
                lineTo(5f, 12f)
                lineTo(12f, 5f)
            }
        }.build()
    }

    val ArrowForward: ImageVector by lazy {
        ImageVector.Builder(
            name = "ArrowForward",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.5f
            ) {
                moveTo(5f, 12f)
                lineTo(19f, 12f)
                moveTo(12f, 5f)
                lineTo(19f, 12f)
                lineTo(12f, 19f)
            }
        }.build()
    }

    val Sparkle: ImageVector by lazy {
        ImageVector.Builder(
            name = "Sparkle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 2f)
                lineTo(14.5f, 9.5f)
                lineTo(22f, 12f)
                lineTo(14.5f, 14.5f)
                lineTo(12f, 22f)
                lineTo(9.5f, 14.5f)
                lineTo(2f, 12f)
                lineTo(9.5f, 9.5f)
                close()
            }
        }.build()
    }

    val Stop: ImageVector by lazy {
        ImageVector.Builder(
            name = "Stop",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(6f, 6f)
                lineTo(18f, 6f)
                lineTo(18f, 18f)
                lineTo(6f, 18f)
                close()
            }
        }.build()
    }

    val Smartphone: ImageVector by lazy {
        ImageVector.Builder(
            name = "Smartphone",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f
            ) {
                moveTo(7f, 2f)
                lineTo(17f, 2f)
                curveTo(18.1f, 2f, 19f, 2.9f, 19f, 4f)
                lineTo(19f, 20f)
                curveTo(19f, 21.1f, 18.1f, 22f, 17f, 22f)
                lineTo(7f, 22f)
                curveTo(5.9f, 22f, 5f, 21.1f, 5f, 20f)
                lineTo(5f, 4f)
                curveTo(5f, 2.9f, 5.9f, 2f, 7f, 2f)
                close()
                moveTo(11f, 18f)
                lineTo(13f, 18f)
            }
        }.build()
    }

    val Settings: ImageVector by lazy {
        ImageVector.Builder(
            name = "Settings",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f
            ) {
                moveTo(12f, 15f)
                curveTo(13.66f, 15f, 15f, 13.66f, 15f, 12f)
                curveTo(15f, 10.34f, 13.66f, 9f, 12f, 9f)
                curveTo(10.34f, 9f, 9f, 10.34f, 9f, 12f)
                curveTo(9f, 13.66f, 10.34f, 15f, 12f, 15f)
                close()
                moveTo(19.4f, 15f)
                lineTo(20.8f, 13.4f)
                lineTo(19.2f, 10.6f)
                lineTo(17.3f, 11.2f)
                curveTo(16.7f, 10.6f, 16f, 10.1f, 15.2f, 9.8f)
                lineTo(14.8f, 7.8f)
                lineTo(11.6f, 7.8f)
                lineTo(11.2f, 9.8f)
                curveTo(10.4f, 10.1f, 9.7f, 10.6f, 9.1f, 11.2f)
                lineTo(7.2f, 10.6f)
                lineTo(5.6f, 13.4f)
                lineTo(7f, 15f)
            }
        }.build()
    }

    val Warning: ImageVector by lazy {
        ImageVector.Builder(
            name = "Warning",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f
            ) {
                moveTo(12f, 3f)
                lineTo(22f, 20f)
                lineTo(2f, 20f)
                close()
                moveTo(12f, 9f)
                lineTo(12f, 14f)
                moveTo(12f, 17f)
                lineTo(12.01f, 17f)
            }
        }.build()
    }

    val Home: ImageVector by lazy {
        ImageVector.Builder(
            name = "Home",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f
            ) {
                moveTo(3f, 10f)
                lineTo(12f, 3f)
                lineTo(21f, 10f)
                verticalLineTo(20f)
                curveTo(21f, 20.55f, 20.55f, 21f, 20f, 21f)
                horizontalLineTo(15f)
                verticalLineTo(14f)
                horizontalLineTo(9f)
                verticalLineTo(21f)
                horizontalLineTo(4f)
                curveTo(3.45f, 21f, 3f, 20.55f, 3f, 20f)
                close()
            }
        }.build()
    }

    val Calendar: ImageVector by lazy {
        ImageVector.Builder(
            name = "Calendar",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f
            ) {
                moveTo(4f, 4f)
                horizontalLineTo(20f)
                curveTo(21.1f, 4f, 22f, 4.9f, 22f, 6f)
                verticalLineTo(20f)
                curveTo(22f, 21.1f, 21.1f, 22f, 20f, 22f)
                horizontalLineTo(4f)
                curveTo(2.9f, 22f, 2f, 21.1f, 2f, 20f)
                verticalLineTo(6f)
                curveTo(2f, 4.9f, 2.9f, 4f, 4f, 4f)
                close()
                moveTo(2f, 9f)
                horizontalLineTo(22f)
                moveTo(8f, 2f)
                verticalLineTo(6f)
                moveTo(16f, 2f)
                verticalLineTo(6f)
            }
        }.build()
    }

    val Lock: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lock",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f
            ) {
                moveTo(6f, 10f)
                horizontalLineTo(18f)
                curveTo(19.1f, 10f, 20f, 10.9f, 20f, 12f)
                verticalLineTo(20f)
                curveTo(20f, 21.1f, 19.1f, 22f, 18f, 22f)
                horizontalLineTo(6f)
                curveTo(4.9f, 22f, 4f, 21.1f, 4f, 20f)
                verticalLineTo(12f)
                curveTo(4f, 10.9f, 4.9f, 10f, 6f, 10f)
                close()
                moveTo(8f, 10f)
                verticalLineTo(7f)
                curveTo(8f, 4.79f, 9.79f, 3f, 12f, 3f)
                curveTo(14.21f, 3f, 16f, 4.79f, 16f, 7f)
                verticalLineTo(10f)
            }
        }.build()
    }

    val MoreVert: ImageVector by lazy {
        ImageVector.Builder(
            name = "MoreVert",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 8f)
                curveTo(13.1f, 8f, 14f, 7.1f, 14f, 6f)
                curveTo(14f, 4.9f, 13.1f, 4f, 12f, 4f)
                curveTo(10.9f, 4f, 10f, 4.9f, 10f, 6f)
                curveTo(10f, 7.1f, 10.9f, 8f, 12f, 8f)
                close()
                moveTo(12f, 10f)
                curveTo(10.9f, 10f, 10f, 10.9f, 10f, 12f)
                curveTo(10f, 13.1f, 10.9f, 14f, 12f, 14f)
                curveTo(13.1f, 14f, 14f, 13.1f, 14f, 12f)
                curveTo(14f, 10.9f, 13.1f, 10f, 12f, 10f)
                close()
                moveTo(12f, 16f)
                curveTo(10.9f, 16f, 10f, 16.9f, 10f, 18f)
                curveTo(10f, 19.1f, 10.9f, 20f, 12f, 20f)
                curveTo(13.1f, 20f, 14f, 19.1f, 14f, 18f)
                curveTo(14f, 16.9f, 13.1f, 16f, 12f, 16f)
                close()
            }
        }.build()
    }

    val Menu: ImageVector by lazy {
        ImageVector.Builder(
            name = "Menu",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.5f
            ) {
                moveTo(4f, 6f)
                lineTo(20f, 6f)
                moveTo(4f, 12f)
                lineTo(20f, 12f)
                moveTo(4f, 18f)
                lineTo(20f, 18f)
            }
        }.build()
    }

    val Globe: ImageVector by lazy {
        ImageVector.Builder(
            name = "Globe",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f
            ) {
                moveTo(12f, 2f)
                curveTo(17.52f, 2f, 22f, 6.48f, 22f, 12f)
                curveTo(22f, 17.52f, 17.52f, 22f, 12f, 22f)
                curveTo(6.48f, 22f, 2f, 17.52f, 2f, 12f)
                curveTo(2f, 6.48f, 6.48f, 2f, 12f, 2f)
                close()
                moveTo(2f, 12f)
                lineTo(22f, 12f)
                moveTo(12f, 2f)
                curveTo(14.5f, 5.5f, 16f, 8.5f, 16f, 12f)
                curveTo(16f, 15.5f, 14.5f, 18.5f, 12f, 22f)
                curveTo(9.5f, 18.5f, 8f, 15.5f, 8f, 12f)
                curveTo(8f, 8.5f, 9.5f, 5.5f, 12f, 2f)
                close()
            }
        }.build()
    }

    val BarChart: ImageVector get() = Chart
    val AlertTriangle: ImageVector get() = Shield
    val Play: ImageVector get() = ArrowForward
}
