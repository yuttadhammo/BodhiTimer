package org.yuttadhammo.BodhiTimer.Const

object TimerState {
    const val RUNNING = 0
    const val STOPPED = 1
    const val PAUSED = 2

    fun getText(number: Int): String {
        return when (number) {
            0 -> "RUNNING"
            1 -> "STOPPED"
            2 -> "PAUSED"
            else -> "UNDEFINED"
        }
    }
}

