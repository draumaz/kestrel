package xyz.draumaz.kestrel.ui

import android.view.View
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

/*
 * Material's sheets and dialogs centre themselves on the whole window, which on the Surface
 * Duo's two screens puts them right across the hinge. These stand in for them (same names and
 * parameters, imported from here instead of material3) and move each one onto a single screen:
 * the thread pane's when a thread opened it, else the main pane's. Without a hinge (one screen,
 * a tablet, a seamless fold) they're Material's own, unchanged.
 */

/** [androidx.compose.material3.ModalBottomSheet], kept to one screen of a dual-screen device. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    properties: ModalBottomSheetProperties = ModalBottomSheetProperties(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val split = LocalPaneSplit.current?.takeIf { it.hinge }
    val inPane = when {
        split == null -> Modifier
        // Side by side: narrowed to its screen (less a margin each side) and moved over it.
        !split.stacked -> Modifier.clearOfHinge().widthIn(max = (if (LocalInEndPane.current) split.end else split.start) - 16.dp)
        // Stacked: it rises from the bottom anyway; just never past the hinge.
        else -> Modifier.heightIn(max = split.end)
    }
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = inPane.then(modifier),
        sheetState = sheetState,
        properties = properties,
        content = content,
    )
}

/** [androidx.compose.material3.AlertDialog], kept to one screen of a dual-screen device. */
@Composable
fun AlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    properties: DialogProperties = DialogProperties(),
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismissRequest,
        // The buttons are always there: they move the dialog's window from inside it.
        confirmButton = { DialogOnOneScreen(); confirmButton() },
        modifier = modifier,
        dismissButton = dismissButton,
        icon = icon,
        title = title,
        text = text,
        properties = properties,
    )
}

/**
 * Moves the dialog window this is composed in from the window's centre to its pane's: dialog
 * windows are centred (Gravity.CENTER), so their x / y are offsets from the middle.
 */
@Composable
private fun DialogOnOneScreen() {
    val split = LocalPaneSplit.current?.takeIf { it.hinge } ?: return
    val inEnd = LocalInEndPane.current
    val view = LocalView.current
    val density = LocalDensity.current
    DisposableEffect(split, inEnd, view) {
        val window = view.dialogWindow()
        if (window != null) {
            val total = split.start + split.gap + split.end
            val paneCentre = if (inEnd) split.endOffset + split.end / 2 else split.start / 2
            val shift = with(density) { (paneCentre - total / 2).roundToPx() }
            window.attributes = window.attributes.apply {
                if (split.stacked) y = shift else x = shift
            }
        }
        onDispose { }
    }
}

private fun View.dialogWindow(): android.view.Window? {
    var v: Any? = this
    while (v != null) {
        if (v is DialogWindowProvider) return v.window
        v = (v as? View)?.parent
    }
    return null
}
