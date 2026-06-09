package com.pelikan.glyphhub.glyph.animation

import com.pelikan.glyphhub.glyph.GlyphTransitionEngine

object GlyphAnimationLibrary {
    val builtInAnimationIds: List<String> =
        GlyphTransitionEngine.transitionIds + listOf("motion_radial_wipe", "motion_orbit_dot", "motion_pulse")

    fun animation(id: String, brightness: Int = 80): GlyphFrameSequence =
        when (id) {
            "motion_radial_wipe" -> GlyphMotionPrimitives.radialWipe(id, "Radial Wipe", brightness = brightness)
            "motion_orbit_dot" -> GlyphMotionPrimitives.orbitDot(id, "Orbit Dot", brightness = brightness)
            "motion_pulse" -> GlyphMotionPrimitives.pulse(id, "Pulse", brightness = brightness)
            else -> GlyphFrameSequence.fromAnimation(GlyphTransitionEngine.animation(id, brightness))
        }
}
