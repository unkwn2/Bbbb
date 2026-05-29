package com.sr.openbyd

enum class WindowType(val id: Int, val descriptionRes: Int) {
    LEFT_FRONT(1, R.string.window_left_front),
    RIGHT_FRONT(2, R.string.window_right_front),
    LEFT_REAR(3, R.string.window_left_rear),
    RIGHT_REAR(4, R.string.window_right_rear),
    MOON_ROOF(5, R.string.window_moon_roof),
    SUNSHADE(6, R.string.window_sunshade),
    ALL(0, R.string.window_all)
}

enum class WindowState(val value: Int, val descriptionRes: Int) {
    OPEN(1, R.string.state_open),
    CLOSE(2, R.string.state_close)
}
