package dev.chuds.stillnotes.ui.components

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * App-wide haptic performer. Provided at the root either as a real performer (firing
 * a TextHandleMove tick) or as a no-op when the user disables haptics in settings.
 * Verb composables call this before invoking the user's onClick.
 */
val LocalHaptics = staticCompositionLocalOf<() -> Unit> { {} }
