package com.example.mvi_flow_example.one

import com.example.mvi_flow_example.one.OneContract.ButtonClickIntent
import com.example.mvi_flow_example.one.OneContract.CounterCommand
import com.example.mvi_flow_example.one.OneContract.OneState
import com.example.mvi_flow_example.one.OneContract.ToastAction
import com.example.mvi_flow_example.one.OneContract.ToastCommand
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flatMapMerge
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import com.example.mvi_flow_example.mvi.MviViewModel

@FlowPreview
@ExperimentalCoroutinesApi
class OneViewModel : MviViewModel<OneState>() {
    init {
        setup {
            initialState = OneState()

            intent<FirstBindIntent> {
                flatMapMerge { flowOf(ToastCommand("FirstBind")) }
            }

            intent<ButtonClickIntent> {
                flatMapLatest {
                    (1..6).asFlow()
                        .onEach { delay(500) }
                        .map { CounterCommand(it) }
                        .onStart<Any> { emit(ToastCommand("Start")) }
                        .onCompletion { emit(ToastCommand("Finish")) }
                }
            }

            reduce {
                when (val command = it) {
                    is CounterCommand -> state { copy(text = command.count.toString()) }
                    is ToastCommand -> actions { listOf(ToastAction(command.text)) }
                    else -> throw IllegalArgumentException("$command")
                }
            }
        }
    }
}
