package com.yaawaaa.penbatteryremapper.remapper

data class AppProfile(
    val packageName: String,
    val appName: String,

    val primaryClickAction: RemapAction = RemapAction.PASSTHROUGH,
    val primaryClickShortcut: String = "",
    val primaryClickAction2: RemapAction = RemapAction.PASSTHROUGH,
    val primaryClickAppPackage2: String = "",

    val primaryDoubleClickAction: RemapAction = RemapAction.PASSTHROUGH,
    val primaryDoubleClickShortcut: String = "",
    val primaryDoubleClickAction2: RemapAction = RemapAction.PASSTHROUGH,
    val primaryDoubleClickAppPackage2: String = "",

    val primaryTripleClickAction: RemapAction = RemapAction.PASSTHROUGH,
    val primaryTripleClickShortcut: String = "",
    val primaryTripleClickAction2: RemapAction = RemapAction.PASSTHROUGH,
    val primaryTripleClickAppPackage2: String = "",

    val primaryLongPressAction: RemapAction = RemapAction.PASSTHROUGH,
    val primaryLongPressShortcut: String = "",
    val primaryLongPressAction2: RemapAction = RemapAction.PASSTHROUGH,
    val primaryLongPressAppPackage2: String = "",

    val secondaryClickAction: RemapAction = RemapAction.PASSTHROUGH,
    val secondaryClickShortcut: String = "",
    val secondaryClickAction2: RemapAction = RemapAction.PASSTHROUGH,
    val secondaryClickAppPackage2: String = "",

    val secondaryDoubleClickAction: RemapAction = RemapAction.PASSTHROUGH,
    val secondaryDoubleClickShortcut: String = "",
    val secondaryDoubleClickAction2: RemapAction = RemapAction.PASSTHROUGH,
    val secondaryDoubleClickAppPackage2: String = "",

    val secondaryTripleClickAction: RemapAction = RemapAction.PASSTHROUGH,
    val secondaryTripleClickShortcut: String = "",
    val secondaryTripleClickAction2: RemapAction = RemapAction.PASSTHROUGH,
    val secondaryTripleClickAppPackage2: String = "",

    val secondaryLongPressAction: RemapAction = RemapAction.PASSTHROUGH,
    val secondaryLongPressShortcut: String = "",
    val secondaryLongPressAction2: RemapAction = RemapAction.PASSTHROUGH,
    val secondaryLongPressAppPackage2: String = "",

    val primaryAppPackage: String = "",
    val secondaryAppPackage: String = ""
)
