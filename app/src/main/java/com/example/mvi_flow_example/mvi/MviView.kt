package com.example.mvi_flow_example.mvi

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.OnLifecycleEvent
import kotlinx.coroutines.flow.Flow

interface MviView<STATE : Any> : LifecycleOwner {
    val viewModel: MviViewModel<STATE>

    fun init() {
        lifecycle.addObserver(object : LifecycleObserver {
            @Suppress("unused")
            @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
            fun create() = viewModel.bind(this@MviView)
        })
    }

    fun intents(): Flow<Any>
    fun render(state: STATE)
    fun actions(action: Any)
}