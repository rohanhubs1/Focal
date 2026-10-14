package com.focal.app.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
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
            name = "ContactlessNfc",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Wave 1 (inner)
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(8f, 9.5f)
                curveTo(9.5f, 11f, 9.5f, 13f, 8f, 14.5f)
            }
            // Wave 2 (middle)
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(11.5f, 6.5f)
                curveTo(14.5f, 9.5f, 14.5f, 14.5f, 11.5f, 17.5f)
            }
            // Wave 3 (outer)
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(15f, 3.5f)
                curveTo(19.5f, 8f, 19.5f, 16f, 15f, 20.5f)
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

    val MoreHorizontal: ImageVector by lazy {
        ImageVector.Builder(
            name = "MoreHorizontal",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(8f, 12f)
                curveTo(8f, 10.9f, 7.1f, 10f, 6f, 10f)
                curveTo(4.9f, 10f, 4f, 10.9f, 4f, 12f)
                curveTo(4f, 13.1f, 4.9f, 14f, 6f, 14f)
                curveTo(7.1f, 14f, 8f, 13.1f, 8f, 12f)
                close()
                moveTo(14f, 12f)
                curveTo(14f, 10.9f, 13.1f, 10f, 12f, 10f)
                curveTo(10.9f, 10f, 10f, 10.9f, 10f, 12f)
                curveTo(10f, 13.1f, 10.9f, 14f, 12f, 14f)
                curveTo(13.1f, 14f, 14f, 13.1f, 14f, 12f)
                close()
                moveTo(20f, 12f)
                curveTo(20f, 10.9f, 19.1f, 10f, 18f, 10f)
                curveTo(16.9f, 10f, 16f, 10.9f, 16f, 12f)
                curveTo(16f, 13.1f, 16.9f, 14f, 18f, 14f)
                curveTo(19.1f, 14f, 20f, 13.1f, 20f, 12f)
                close()
            }
        }.build()
    }

    val Trash: ImageVector by lazy {
        ImageVector.Builder(
            name = "Trash",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(3f, 6f)
                lineTo(21f, 6f)
                moveTo(19f, 6f)
                lineTo(18.2f, 19f)
                curveTo(18.1f, 20.1f, 17.2f, 21f, 16.1f, 21f)
                lineTo(7.9f, 21f)
                curveTo(6.8f, 21f, 5.9f, 20.1f, 5.8f, 19f)
                lineTo(5f, 6f)
                moveTo(8f, 6f)
                verticalLineTo(4f)
                curveTo(8f, 3.45f, 8.45f, 3f, 9f, 3f)
                lineTo(15f, 3f)
                curveTo(15.55f, 3f, 16f, 3.45f, 16f, 4f)
                verticalLineTo(6f)
                moveTo(10f, 11f)
                verticalLineTo(16f)
                moveTo(14f, 11f)
                verticalLineTo(16f)
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
    val Info: ImageVector get() = Shield
    val Message: ImageVector get() = Bell
    val Bolt: ImageVector get() = Lightning
    val Refresh: ImageVector get() = Repeat

    val Share: ImageVector by lazy {
        ImageVector.Builder(
            name = "Share",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round,
                strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round
            ) {
                moveTo(4f, 12f)
                verticalLineTo(20f)
                horizontalLineTo(20f)
                verticalLineTo(12f)
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round,
                strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round
            ) {
                moveTo(12f, 15f)
                lineTo(12f, 3f)
                moveTo(7f, 8f)
                lineTo(12f, 3f)
                lineTo(17f, 8f)
            }
        }.build()
    }

    val Lightning: ImageVector by lazy {
        ImageVector.Builder(
            name = "Lightning",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.White)) {
                moveTo(13f, 2f)
                lineTo(4f, 13f)
                horizontalLineTo(11f)
                lineTo(9f, 22f)
                lineTo(20f, 10f)
                horizontalLineTo(13f)
                lineTo(13f, 2f)
                close()
            }
        }.build()
    }

    val Activity: ImageVector by lazy {
        ImageVector.Builder(
            name = "Activity",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3f, 12f)
                lineTo(7f, 12f)
                lineTo(10f, 4f)
                lineTo(14f, 20f)
                lineTo(17f, 12f)
                lineTo(21f, 12f)
            }
        }.build()
    }

    val User: ImageVector by lazy {
        ImageVector.Builder(
            name = "User",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Head
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(12f, 7f)
                curveTo(13.66f, 7f, 15f, 8.34f, 15f, 10f)
                curveTo(15f, 11.66f, 13.66f, 13f, 12f, 13f)
                curveTo(10.34f, 13f, 9f, 11.66f, 9f, 10f)
                curveTo(9f, 8.34f, 10.34f, 7f, 12f, 7f)
                close()
            }
            // Shoulders
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(6f, 20f)
                curveTo(6f, 16.5f, 9f, 15.5f, 12f, 15.5f)
                curveTo(15f, 15.5f, 18f, 16.5f, 18f, 20f)
            }
        }.build()
    }

    val Target: ImageVector by lazy {
        ImageVector.Builder(
            name = "Target",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.75f
            ) {
                moveTo(12f, 2f)
                curveTo(17.52f, 2f, 22f, 6.48f, 22f, 12f)
                curveTo(22f, 17.52f, 17.52f, 22f, 12f, 22f)
                curveTo(6.48f, 22f, 2f, 17.52f, 2f, 12f)
                curveTo(2f, 6.48f, 6.48f, 2f, 12f, 2f)
                close()
                moveTo(12f, 6.5f)
                curveTo(15.04f, 6.5f, 17.5f, 8.96f, 17.5f, 12f)
                curveTo(17.5f, 15.04f, 15.04f, 17.5f, 12f, 17.5f)
                curveTo(8.96f, 17.5f, 6.5f, 15.04f, 6.5f, 12f)
                curveTo(6.5f, 8.96f, 8.96f, 6.5f, 12f, 6.5f)
                close()
            }
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 10f)
                curveTo(13.1f, 10f, 14f, 10.9f, 14f, 12f)
                curveTo(14f, 13.1f, 13.1f, 14f, 12f, 14f)
                curveTo(10.9f, 14f, 10f, 13.1f, 10f, 12f)
                curveTo(10f, 10.9f, 10.9f, 10f, 12f, 10f)
                close()
            }
        }.build()
    }

    val DeviceBox: ImageVector by lazy {
        ImageVector.Builder(
            name = "DeviceBox",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(5f, 4f)
                curveTo(5f, 2.9f, 5.9f, 2f, 7f, 2f)
                horizontalLineTo(17f)
                curveTo(18.1f, 2f, 19f, 2.9f, 19f, 4f)
                verticalLineTo(20f)
                curveTo(19f, 21.1f, 18.1f, 22f, 17f, 22f)
                horizontalLineTo(7f)
                curveTo(5.9f, 22f, 5f, 21.1f, 5f, 20f)
                close()
                moveTo(9f, 18f)
                horizontalLineTo(15f)
            }
        }.build()
    }

    val Phone: ImageVector by lazy {
        ImageVector.Builder(
            name = "Phone",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(5f, 4f)
                curveTo(5f, 4f, 5.5f, 2f, 7.5f, 2f)
                curveTo(9.5f, 2f, 10.5f, 4.5f, 10.5f, 5.5f)
                curveTo(10.5f, 6.5f, 9.5f, 7.5f, 9.5f, 7.5f)
                curveTo(9.5f, 7.5f, 10f, 10f, 12f, 12f)
                curveTo(14f, 14f, 16.5f, 14.5f, 16.5f, 14.5f)
                curveTo(16.5f, 14.5f, 17.5f, 13.5f, 18.5f, 13.5f)
                curveTo(19.5f, 13.5f, 22f, 14.5f, 22f, 16.5f)
                curveTo(22f, 18.5f, 20f, 19f, 20f, 19f)
                curveTo(18f, 19.5f, 14f, 18.5f, 10f, 14.5f)
                curveTo(6f, 10.5f, 4.5f, 6.5f, 5f, 4.5f)
                close()
            }
        }.build()
    }

    val Coffee: ImageVector by lazy {
        ImageVector.Builder(
            name = "Coffee",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(18f, 8f)
                horizontalLineTo(5f)
                curveTo(5f, 13f, 7f, 16f, 11f, 16f)
                curveTo(15f, 16f, 17f, 13f, 17f, 8f)
                moveTo(17f, 9f)
                horizontalLineTo(19f)
                curveTo(20.5f, 9f, 20.5f, 13f, 19f, 13f)
                horizontalLineTo(16.5f)
                moveTo(4f, 19f)
                horizontalLineTo(18f)
                moveTo(8f, 3f)
                verticalLineTo(5f)
                moveTo(11f, 2f)
                verticalLineTo(5f)
                moveTo(14f, 3f)
                verticalLineTo(5f)
            }
        }.build()
    }

    val Repeat: ImageVector by lazy {
        ImageVector.Builder(
            name = "Repeat",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(17f, 2f)
                lineTo(21f, 6f)
                lineTo(17f, 10f)
                moveTo(3f, 12f)
                verticalLineTo(10f)
                curveTo(3f, 7.8f, 4.8f, 6f, 7f, 6f)
                horizontalLineTo(21f)
                moveTo(7f, 22f)
                lineTo(3f, 18f)
                lineTo(7f, 14f)
                moveTo(21f, 12f)
                verticalLineTo(14f)
                curveTo(21f, 16.2f, 19.2f, 18f, 17f, 18f)
                horizontalLineTo(3f)
            }
        }.build()
    }

    val Clock: ImageVector by lazy {
        ImageVector.Builder(
            name = "Clock",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(12f, 3f)
                curveTo(16.97f, 3f, 21f, 7.03f, 21f, 12f)
                curveTo(21f, 16.97f, 16.97f, 21f, 12f, 21f)
                curveTo(7.03f, 21f, 3f, 16.97f, 3f, 12f)
                curveTo(3f, 7.03f, 7.03f, 3f, 12f, 3f)
                close()
                moveTo(12f, 7f)
                verticalLineTo(12f)
                lineTo(15.5f, 14f)
            }
        }.build()
    }

    val Book: ImageVector by lazy {
        ImageVector.Builder(
            name = "Book",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(4f, 19.5f)
                curveTo(4f, 18.1f, 5.1f, 17f, 6.5f, 17f)
                horizontalLineTo(20f)
                moveTo(6.5f, 3f)
                horizontalLineTo(20f)
                verticalLineTo(21f)
                horizontalLineTo(6.5f)
                curveTo(5.1f, 21f, 4f, 19.9f, 4f, 18.5f)
                verticalLineTo(4.5f)
                curveTo(4f, 3.1f, 5.1f, 3f, 6.5f, 3f)
                close()
            }
        }.build()
    }

    val Sun: ImageVector by lazy {
        ImageVector.Builder(
            name = "Sun",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(12f, 7f)
                curveTo(14.76f, 7f, 17f, 9.24f, 17f, 12f)
                curveTo(17f, 14.76f, 14.76f, 17f, 12f, 17f)
                curveTo(9.24f, 17f, 7f, 14.76f, 7f, 12f)
                curveTo(7f, 9.24f, 9.24f, 7f, 12f, 7f)
                close()
                moveTo(12f, 2f)
                verticalLineTo(4f)
                moveTo(12f, 20f)
                verticalLineTo(22f)
                moveTo(2f, 12f)
                horizontalLineTo(4f)
                moveTo(20f, 12f)
                horizontalLineTo(22f)
                moveTo(4.93f, 4.93f)
                lineTo(6.34f, 6.34f)
                moveTo(17.66f, 17.66f)
                lineTo(19.07f, 19.07f)
                moveTo(4.93f, 19.07f)
                lineTo(6.34f, 17.66f)
                moveTo(17.66f, 6.34f)
                lineTo(19.07f, 4.93f)
            }
        }.build()
    }

    val Moon: ImageVector by lazy {
        ImageVector.Builder(
            name = "Moon",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(21f, 12.79f)
                curveTo(20.35f, 17.73f, 16.29f, 21.67f, 11.31f, 21.99f)
                curveTo(6.33f, 22.31f, 2f, 18.42f, 2f, 13.44f)
                curveTo(2f, 8.46f, 5.94f, 4.13f, 10.92f, 3.81f)
                curveTo(11.49f, 3.77f, 11.89f, 4.35f, 11.66f, 4.88f)
                curveTo(10.4f, 7.78f, 11.13f, 11.23f, 13.57f, 13.43f)
                curveTo(16.01f, 15.63f, 19.64f, 15.93f, 22.42f, 14.21f)
                curveTo(22.92f, 13.9f, 23.51f, 14.37f, 23.36f, 14.92f)
                close()
            }
        }.build()
    }

    val Sprout: ImageVector by lazy {
        ImageVector.Builder(
            name = "Sprout",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(12f, 21f)
                verticalLineTo(10f)
                moveTo(12f, 10f)
                curveTo(12f, 5.5f, 7f, 5.5f, 7f, 5.5f)
                curveTo(7f, 10f, 12f, 10f, 12f, 10f)
                moveTo(12f, 13f)
                curveTo(12f, 8.5f, 17f, 8.5f, 17f, 8.5f)
                curveTo(17f, 13f, 12f, 13f, 12f, 13f)
            }
        }.build()
    }

    val LockOpen: ImageVector by lazy {
        ImageVector.Builder(
            name = "LockOpen",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                // Lock body
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
                // Open shackle (turned up & open)
                moveTo(8f, 10f)
                verticalLineTo(7f)
                curveTo(8f, 4.79f, 9.79f, 3f, 12f, 3f)
                curveTo(14.21f, 3f, 16f, 4.79f, 16f, 7f)
                verticalLineTo(8.5f)
            }
        }.build()
    }

    val Backspace: ImageVector by lazy {
        ImageVector.Builder(
            name = "Backspace",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(21f, 19f)
                horizontalLineTo(9f)
                lineTo(3.5f, 12f)
                lineTo(9f, 5f)
                horizontalLineTo(21f)
                curveTo(21.55f, 5f, 22f, 5.45f, 22f, 6f)
                verticalLineTo(18f)
                curveTo(22f, 18.55f, 21.55f, 19f, 21f, 19f)
                close()
                moveTo(12f, 9.5f)
                lineTo(17f, 14.5f)
                moveTo(17f, 9.5f)
                lineTo(12f, 14.5f)
            }
        }.build()
    }

    val Card: ImageVector by lazy {
        ImageVector.Builder(
            name = "CreditCard",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(4f, 6f)
                horizontalLineTo(20f)
                curveTo(21.1f, 6f, 22f, 6.9f, 22f, 8f)
                verticalLineTo(16f)
                curveTo(22f, 17.1f, 21.1f, 18f, 20f, 18f)
                horizontalLineTo(4f)
                curveTo(2.9f, 18f, 2f, 17.1f, 2f, 16f)
                verticalLineTo(8f)
                curveTo(2f, 6.9f, 2.9f, 6f, 4f, 6f)
                close()
                moveTo(2f, 10f)
                horizontalLineTo(22f)
                moveTo(6f, 14.5f)
                horizontalLineTo(10f)
            }
        }.build()
    }

    val HelpCircle: ImageVector by lazy {
        ImageVector.Builder(
            name = "HelpCircle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(12f, 2f)
                curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
                curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
                curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
                curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
                close()
                moveTo(9.5f, 9f)
                curveTo(9.5f, 7.62f, 10.62f, 6.5f, 12f, 6.5f)
                curveTo(13.38f, 6.5f, 14.5f, 7.62f, 14.5f, 9f)
                curveTo(14.5f, 10.5f, 12.5f, 11.25f, 12.5f, 13f)
                moveTo(12f, 16.5f)
                lineTo(12.01f, 16.5f)
            }
        }.build()
    }

    val InfoCircle: ImageVector by lazy {
        ImageVector.Builder(
            name = "InfoCircle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(12f, 2f)
                curveTo(6.48f, 2f, 2f, 6.48f, 2f, 12f)
                curveTo(2f, 17.52f, 6.48f, 22f, 12f, 22f)
                curveTo(17.52f, 22f, 22f, 17.52f, 22f, 12f)
                curveTo(22f, 6.48f, 17.52f, 2f, 12f, 2f)
                close()
                moveTo(12f, 8f)
                lineTo(12.01f, 8f)
                moveTo(12f, 11.5f)
                verticalLineTo(16f)
            }
        }.build()
    }

    val ShieldCheck: ImageVector by lazy {
        ImageVector.Builder(
            name = "ShieldCheck",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(12f, 2f)
                lineTo(4f, 5f)
                verticalLineTo(11f)
                curveTo(4f, 16.5f, 7.5f, 20.8f, 12f, 22f)
                curveTo(16.5f, 20.8f, 20f, 16.5f, 20f, 11f)
                verticalLineTo(5f)
                close()
                moveTo(9f, 11.5f)
                lineTo(11f, 13.5f)
                lineTo(15.5f, 9f)
            }
        }.build()
    }

    val ChevronRight: ImageVector by lazy {
        ImageVector.Builder(
            name = "ChevronRight",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color(0xFF6B6B6B)),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(9f, 6f)
                lineTo(15f, 12f)
                lineTo(9f, 18f)
            }
        }.build()
    }

    val ChevronLeft: ImageVector by lazy {
        ImageVector.Builder(
            name = "ChevronLeft",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(15f, 6f)
                lineTo(9f, 12f)
                lineTo(15f, 18f)
            }
        }.build()
    }

    val Accessibility: ImageVector by lazy {
        ImageVector.Builder(
            name = "Accessibility",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Head
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.75f
            ) {
                moveTo(12f, 4f)
                curveTo(13.1f, 4f, 14f, 4.9f, 14f, 6f)
                curveTo(14f, 7.1f, 13.1f, 8f, 12f, 8f)
                curveTo(10.9f, 8f, 10f, 7.1f, 10f, 6f)
                curveTo(10f, 4.9f, 10.9f, 4f, 12f, 4f)
                close()
            }
            // Outstretched arms & body & legs
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(5f, 10f)
                lineTo(19f, 10f)
                moveTo(12f, 10f)
                lineTo(12f, 15f)
                moveTo(12f, 15f)
                lineTo(9f, 21f)
                moveTo(12f, 15f)
                lineTo(15f, 21f)
            }
        }.build()
    }

    val DisplayOverlay: ImageVector by lazy {
        ImageVector.Builder(
            name = "DisplayOverlay",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(4f, 6f)
                lineTo(20f, 6f)
                lineTo(20f, 18f)
                lineTo(4f, 18f)
                close()
                moveTo(4f, 11f)
                lineTo(20f, 11f)
            }
        }.build()
    }

    val Battery: ImageVector by lazy {
        ImageVector.Builder(
            name = "Battery",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(7f, 6f)
                lineTo(17f, 6f)
                lineTo(17f, 20f)
                lineTo(7f, 20f)
                close()
                moveTo(10f, 4f)
                lineTo(14f, 4f)
            }
        }.build()
    }

    val InfoCircleAlert: ImageVector by lazy {
        ImageVector.Builder(
            name = "InfoCircleAlert",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(12f, 2f)
                curveTo(17.52f, 2f, 22f, 6.48f, 22f, 12f)
                curveTo(22f, 17.52f, 17.52f, 22f, 12f, 22f)
                curveTo(6.48f, 22f, 2f, 17.52f, 2f, 12f)
                curveTo(2f, 6.48f, 6.48f, 2f, 12f, 2f)
                close()
                moveTo(12f, 7.5f)
                lineTo(12f, 12.5f)
                moveTo(12f, 16.5f)
                lineTo(12f, 17f)
            }
        }.build()
    }

    val ChevronDown: ImageVector by lazy {
        ImageVector.Builder(
            name = "ChevronDown",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(6f, 9f)
                lineTo(12f, 15f)
                lineTo(18f, 9f)
            }
        }.build()
    }

    val ChevronUp: ImageVector by lazy {
        ImageVector.Builder(
            name = "ChevronUp",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(6f, 15f)
                lineTo(12f, 9f)
                lineTo(18f, 15f)
            }
        }.build()
    }

    val Mail: ImageVector by lazy {
        ImageVector.Builder(
            name = "Mail",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(4f, 6f)
                lineTo(20f, 6f)
                lineTo(20f, 18f)
                lineTo(4f, 18f)
                close()
                moveTo(4f, 7f)
                lineTo(12f, 13f)
                lineTo(20f, 7f)
            }
        }.build()
    }

    val Document: ImageVector by lazy {
        ImageVector.Builder(
            name = "Document",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(6f, 3f)
                lineTo(14f, 3f)
                lineTo(18f, 7f)
                lineTo(18f, 21f)
                lineTo(6f, 21f)
                close()
                moveTo(14f, 3f)
                lineTo(14f, 7f)
                lineTo(18f, 7f)
                moveTo(9f, 12f)
                lineTo(15f, 12f)
                moveTo(9f, 16f)
                lineTo(13f, 16f)
            }
        }.build()
    }

    val Code: ImageVector by lazy {
        ImageVector.Builder(
            name = "Code",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(8f, 8f)
                lineTo(4f, 12f)
                lineTo(8f, 16f)
                moveTo(16f, 8f)
                lineTo(20f, 12f)
                lineTo(16f, 16f)
                moveTo(14f, 6f)
                lineTo(10f, 18f)
            }
        }.build()
    }

    val Flashlight: ImageVector by lazy {
        ImageVector.Builder(
            name = "Flashlight",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(9f, 2f)
                lineTo(15f, 2f)
                lineTo(15f, 5f)
                lineTo(13.5f, 8f)
                lineTo(13.5f, 21f)
                curveTo(13.5f, 21.55f, 13.05f, 22f, 12.5f, 22f)
                lineTo(11.5f, 22f)
                curveTo(10.95f, 22f, 10.5f, 21.55f, 10.5f, 21f)
                lineTo(10.5f, 8f)
                lineTo(9f, 5f)
                close()
                moveTo(9f, 5f)
                lineTo(15f, 5f)
                moveTo(12f, 11f)
                lineTo(12f, 13f)
            }
        }.build()
    }

    val WindowPlay: ImageVector by lazy {
        ImageVector.Builder(
            name = "WindowPlay",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Round
            ) {
                moveTo(4f, 5f)
                lineTo(20f, 5f)
                curveTo(21.1f, 5f, 22f, 5.9f, 22f, 7f)
                lineTo(22f, 17f)
                curveTo(22f, 18.1f, 21.1f, 19f, 20f, 19f)
                lineTo(4f, 19f)
                curveTo(2.9f, 19f, 2f, 18.1f, 2f, 17f)
                lineTo(2f, 7f)
                curveTo(2f, 5.9f, 2.9f, 5f, 4f, 5f)
                close()
            }
            path(
                fill = SolidColor(Color.White)
            ) {
                moveTo(10.5f, 9.5f)
                lineTo(15f, 12f)
                lineTo(10.5f, 14.5f)
                close()
            }
        }.build()
    }

    val Search: ImageVector by lazy {
        ImageVector.Builder(
            name = "Search",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(11f, 19f)
                curveTo(15.4183f, 19f, 19f, 15.4183f, 19f, 11f)
                curveTo(19f, 6.58172f, 15.4183f, 3f, 11f, 3f)
                curveTo(6.58172f, 3f, 3f, 6.58172f, 3f, 11f)
                curveTo(3f, 15.4183f, 6.58172f, 19f, 11f, 19f)
                close()
                moveTo(21f, 21f)
                lineTo(16.65f, 16.65f)
            }
        }.build()
    }

    val Crown: ImageVector by lazy {
        ImageVector.Builder(
            name = "Crown",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(3f, 19f)
                lineTo(21f, 19f)
                moveTo(4f, 19f)
                lineTo(5f, 8f)
                lineTo(9.5f, 13f)
                lineTo(12f, 6f)
                lineTo(14.5f, 13f)
                lineTo(19f, 8f)
                lineTo(20f, 19f)
                close()
            }
        }.build()
    }

    val ArrowRight: ImageVector by lazy {
        ImageVector.Builder(
            name = "ArrowRight",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(5f, 12f)
                lineTo(19f, 12f)
                moveTo(12f, 5f)
                lineTo(19f, 12f)
                lineTo(12f, 19f)
            }
        }.build()
    }

    val Eye: ImageVector by lazy {
        ImageVector.Builder(
            name = "Eye",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(1f, 12f)
                curveTo(3.5f, 6.5f, 7.5f, 3.5f, 12f, 3.5f)
                curveTo(16.5f, 3.5f, 20.5f, 6.5f, 23f, 12f)
                curveTo(20.5f, 17.5f, 16.5f, 20.5f, 12f, 20.5f)
                curveTo(7.5f, 20.5f, 3.5f, 17.5f, 1f, 12f)
                close()
            }
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f
            ) {
                moveTo(12f, 9f)
                curveTo(13.66f, 9f, 15f, 10.34f, 15f, 12f)
                curveTo(15f, 13.66f, 13.66f, 15f, 12f, 15f)
                curveTo(10.34f, 15f, 9f, 13.66f, 9f, 12f)
                curveTo(9f, 10.34f, 10.34f, 9f, 12f, 9f)
                close()
            }
        }.build()
    }

    val EyeOff: ImageVector by lazy {
        ImageVector.Builder(
            name = "EyeOff",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(1f, 1f)
                lineTo(23f, 23f)
                moveTo(17.94f, 17.94f)
                curveTo(16.23f, 19.24f, 14.18f, 20f, 12f, 20f)
                curveTo(7.5f, 20f, 3.5f, 17f, 1f, 12f)
                curveTo(2.24f, 9.5f, 4.08f, 7.42f, 6.34f, 5.94f)
                moveTo(9.9f, 4.24f)
                curveTo(10.58f, 4.08f, 11.28f, 4f, 12f, 4f)
                curveTo(16.5f, 4f, 20.5f, 7f, 23f, 12f)
                curveTo(21.94f, 14.12f, 20.44f, 15.93f, 18.57f, 17.26f)
            }
        }.build()
    }

    val Google: ImageVector by lazy {
        ImageVector.Builder(
            name = "GoogleLogo",
            defaultWidth = 20.dp,
            defaultHeight = 20.dp,
            viewportWidth = 48f,
            viewportHeight = 48f
        ).apply {
            // Red quadrant (top-left)
            path(fill = SolidColor(Color(0xFFEA4335))) {
                moveTo(24f, 9.5f)
                curveTo(27.54f, 9.5f, 30.71f, 10.72f, 33.21f, 13.1f)
                lineTo(40.06f, 6.25f)
                curveTo(35.9f, 2.38f, 30.47f, 0f, 24f, 0f)
                curveTo(14.66f, 0f, 6.58f, 5.38f, 2.56f, 13.22f)
                lineTo(10.54f, 19.41f)
                curveTo(12.43f, 13.72f, 17.74f, 9.5f, 24f, 9.5f)
                close()
            }
            // Blue quadrant (right arm)
            path(fill = SolidColor(Color(0xFF4285F4))) {
                moveTo(46.98f, 24.55f)
                curveTo(46.98f, 22.98f, 46.83f, 21.46f, 46.6f, 20f)
                lineTo(24f, 20f)
                lineTo(24f, 29.02f)
                lineTo(36.94f, 29.02f)
                curveTo(36.36f, 31.98f, 34.68f, 34.5f, 32.16f, 36.2f)
                lineTo(39.89f, 42.2f)
                curveTo(44.4f, 38.02f, 46.98f, 31.84f, 46.98f, 24.55f)
                close()
            }
            // Yellow quadrant (bottom-left)
            path(fill = SolidColor(Color(0xFFFBBC05))) {
                moveTo(10.53f, 28.59f)
                curveTo(10.05f, 27.14f, 9.77f, 25.6f, 9.77f, 24f)
                curveTo(9.77f, 22.4f, 10.05f, 20.86f, 10.53f, 19.41f)
                lineTo(2.55f, 13.22f)
                curveTo(0.92f, 16.46f, 0f, 20.12f, 0f, 24f)
                curveTo(0f, 27.88f, 0.92f, 31.54f, 2.56f, 34.78f)
                lineTo(10.53f, 28.59f)
                close()
            }
            // Green quadrant (bottom-right)
            path(fill = SolidColor(Color(0xFF34A853))) {
                moveTo(24f, 48f)
                curveTo(30.48f, 48f, 35.93f, 45.87f, 39.89f, 42.19f)
                lineTo(32.16f, 36.19f)
                curveTo(30.01f, 37.64f, 27.24f, 38.5f, 24f, 38.5f)
                curveTo(17.74f, 38.5f, 12.43f, 34.28f, 10.53f, 28.59f)
                lineTo(2.56f, 34.78f)
                curveTo(6.58f, 42.62f, 14.66f, 48f, 24f, 48f)
                close()
            }
        }.build()
    }
}


