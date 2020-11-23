package com.example.mvi_flow_example.one


object OneContract {

    data class OneState(
        val text: String = "",
        val loading: Boolean = false,
    )

    object ButtonClickIntent

    data class CounterCommand(val count: Int)
    data class ToastCommand(val text: String)

    data class ToastAction(val text: String)
}