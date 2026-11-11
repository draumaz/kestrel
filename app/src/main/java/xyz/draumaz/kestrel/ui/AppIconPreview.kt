package xyz.draumaz.kestrel.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import xyz.draumaz.kestrel.core.AppIcon

/** The launcher icon as the home screen shows it: its colour layer under the Saturn mark, cropped to a circle. */
@Composable
fun AppIconPreview(icon: AppIcon, size: Dp, modifier: Modifier = Modifier) {
    Box(modifier.size(size).clip(CircleShape)) {
        // Adaptive icons are 108 units with the visible area in the middle 72: scale to crop like a launcher.
        val m = Modifier.fillMaxSize().graphicsLayer { scaleX = 1.5f; scaleY = 1.5f }
        Image(painterResource(icon.background), null, m)
        Image(painterResource(icon.foreground), null, m)
    }
}
