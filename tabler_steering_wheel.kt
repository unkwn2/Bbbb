package com.sr.openbyd.models

import com.sr.openbyd.R

/**
 * Shared enum for all supported actions in the app.
 * Used by both button mappings (steering wheel) and startup action sequences.
 */
enum class ActionType(val labelRes: Int) {
    NONE(R.string.action_none),
    LAUNCH_APP(R.string.action_launch_app),
    LAUNCH_APP_ON_SPLIT_SCREEN(R.string.action_split_screen),
    LAUNCH_APP_ON_CLUSTER(R.string.action_launch_app_cluster),
    TOGGLE_CLUSTER_CAST(R.string.action_toggle_cluster),
    TOGGLE_AC(R.string.action_toggle_ac),
    TOGGLE_WINDOW_DRIVER(R.string.action_toggle_window),
    //TOGGLE_AUXILIARY_LIGHTS,

    // more native actions can be added here
}
