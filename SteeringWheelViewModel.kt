/*
Font Awesome Free License
-------------------------

Font Awesome Free is free, open source, and GPL friendly. You can use it for
commercial projects, open source projects, or really almost whatever you want.
Full Font Awesome Free license: https://fontawesome.com/license/free.

# Icons: CC BY 4.0 License (https://creativecommons.org/licenses/by/4.0/)
In the Font Awesome Free download, the CC BY 4.0 license applies to all icons
packaged as SVG and JS file types.

# Fonts: SIL OFL 1.1 License (https://scripts.sil.org/OFL)
In the Font Awesome Free download, the SIL OFL license applies to all icons
packaged as web and desktop font files.

# Code: MIT License (https://opensource.org/licenses/MIT)
In the Font Awesome Free download, the MIT license applies to all non-font and
non-icon files.

# Attribution
Attribution is required by MIT, SIL OFL, and CC BY licenses. Downloaded Font
Awesome Free files already contain embedded comments with sufficient
attribution, so you shouldn't need to do anything additional when using these
files normally.

We've kept attribution comments terse, so we ask that you do not actively work
to remove them from files, especially code. They're a great way for folks to
learn about Font Awesome.

# Brand Icons
All brand icons are trademarks of their respective owners. The use of these
trademarks does not indicate endorsement of the trademark holder by Font
Awesome, nor vice versa. **Please do not use brand logos for any purpose except
to represent the company, product, or service to which they refer.**
*/
package com.sr.openbyd.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val FontAwesomeCarAlt: ImageVector
    get() {
        if (_FontAwesomeCarAlt != null) return _FontAwesomeCarAlt!!
        
        _FontAwesomeCarAlt = ImageVector.Builder(
            name = "car-alt",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 480f,
            viewportHeight = 512f
        ).apply {
            path(
                fill = SolidColor(Color.Black)
            ) {
                moveTo(438.66f, 212.33f)
                lineToRelative(-11.24f, -28.1f)
                lineToRelative(-19.93f, -49.83f)
                curveTo(390.38f, 91.63f, 349.57f, 64f, 303.5f, 64f)
                horizontalLineToRelative(-127f)
                curveToRelative(-46.06f, 0f, -86.88f, 27.63f, -103.99f, 70.4f)
                lineToRelative(-19.93f, 49.83f)
                lineToRelative(-11.24f, 28.1f)
                curveTo(17.22f, 221.5f, 0f, 244.66f, 0f, 272f)
                verticalLineToRelative(48f)
                curveToRelative(0f, 16.12f, 6.16f, 30.67f, 16f, 41.93f)
                verticalLineTo(416f)
                curveToRelative(0f, 17.67f, 14.33f, 32f, 32f, 32f)
                horizontalLineToRelative(32f)
                curveToRelative(17.67f, 0f, 32f, -14.33f, 32f, -32f)
                verticalLineToRelative(-32f)
                horizontalLineToRelative(256f)
                verticalLineToRelative(32f)
                curveToRelative(0f, 17.67f, 14.33f, 32f, 32f, 32f)
                horizontalLineToRelative(32f)
                curveToRelative(17.67f, 0f, 32f, -14.33f, 32f, -32f)
                verticalLineToRelative(-54.07f)
                curveToRelative(9.84f, -11.25f, 16f, -25.8f, 16f, -41.93f)
                verticalLineToRelative(-48f)
                curveToRelative(0f, -27.34f, -17.22f, -50.5f, -41.34f, -59.67f)
                close()
                moveToRelative(-306.73f, -54.16f)
                curveToRelative(7.29f, -18.22f, 24.94f, -30.17f, 44.57f, -30.17f)
                horizontalLineToRelative(127f)
                curveToRelative(19.63f, 0f, 37.28f, 11.95f, 44.57f, 30.17f)
                lineTo(368f, 208f)
                horizontalLineTo(112f)
                lineToRelative(19.93f, -49.83f)
                close()
                moveTo(80f, 319.8f)
                curveToRelative(-19.2f, 0f, -32f, -12.76f, -32f, -31.9f)
                reflectiveCurveTo(60.8f, 256f, 80f, 256f)
                reflectiveCurveToRelative(48f, 28.71f, 48f, 47.85f)
                reflectiveCurveToRelative(-28.8f, 15.95f, -48f, 15.95f)
                close()
                moveToRelative(320f, 0f)
                curveToRelative(-19.2f, 0f, -48f, 3.19f, -48f, -15.95f)
                reflectiveCurveTo(380.8f, 256f, 400f, 256f)
                reflectiveCurveToRelative(32f, 12.76f, 32f, 31.9f)
                reflectiveCurveToRelative(-12.8f, 31.9f, -32f, 31.9f)
                close()
            }
        }.build()
        
        return _FontAwesomeCarAlt!!
    }

private var _FontAwesomeCarAlt: ImageVector? = null