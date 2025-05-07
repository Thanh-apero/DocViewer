package com.cherry.lib.doc.office.system

enum class ViewMode {
    HORIZONTAL_SNAP, VERTICAL_SNAP, HORIZONTAL_CONTINUOUS, VERTICAL_CONTINUOUS;
    fun isHorizontal(): Boolean {
        return this == HORIZONTAL_SNAP || this == HORIZONTAL_CONTINUOUS
    }

    fun isSnap(): Boolean {
        return this == HORIZONTAL_SNAP || this == VERTICAL_SNAP
    }
}