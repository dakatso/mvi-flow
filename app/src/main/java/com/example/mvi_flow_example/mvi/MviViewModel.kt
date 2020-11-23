package com.example.mvi_flow_example.mvi

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.whenStarted
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.scan
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

abstract class MviViewModel<STATE : Any> : ViewModel() {
    private val intents = MutableSharedFlow<Any>(0, 1, BufferOverflow.DROP_OLDEST)
    private lateinit var state: StateFlow<STATE>
    private lateinit var actions: Flow<Any>

    private var isFirstBind = true

    @FlowPreview
    @ExperimentalCoroutinesApi
    protected fun setup(apply: SetupBuilder<STATE>.() -> Unit) {
        val builder = SetupBuilder<STATE>().apply(apply)

        @Suppress("UNCHECKED_CAST")
        val flows = builder.transformations.map { t ->
            val transform = t.transform as Flow<Any>.() -> Flow<Any>
            intents.filter { t.clazz.isInstance(it) }.transform()
        }

        val share = flows
            .merge()
            .scan(builder.initialState to listOf<Any>()) { effect, command ->
                builder.reducer(effect.first, command)
            }
            .shareIn(viewModelScope, SharingStarted.Eagerly)

        actions = share
            .flatMapConcat { it.second.asFlow() }

        state = share
            .map { it.first }
            .distinctUntilChanged()
            .stateIn(viewModelScope, SharingStarted.Eagerly, builder.initialState)
    }

    fun <VIEW> bind(view: VIEW) where VIEW : MviView<STATE>, VIEW : LifecycleOwner {
        view.lifecycleScope.run {

            launchWhenStarted {
                view.intents()
                    .onEach { intents.emit(it) }
                    .catch { it.printStackTrace() }
                    .collect()
            }

            launchWhenStarted {
                state
                    .onEach { view.render(it) }
                    .collect()
            }

            launchWhenStarted {
                actions
                    .onEach { view.actions(it) }
                    .launchValve(view)
            }

            if (isFirstBind) {
                launchWhenStarted { intents.emit(FirstBindIntent) }
                isFirstBind = false
            }
        }
    }

    private fun <T> Flow<T>.launchValve(owner: LifecycleOwner) = owner.lifecycleScope
        .launch { owner.whenStarted { collect() } }

    class SetupBuilder<STATE : Any> {
        val transformations = mutableListOf<Transformation<out Any>>()
        lateinit var reducer: (suspend STATE.(Any) -> Pair<STATE, List<Any>>)
        lateinit var initialState: STATE

        inline fun <reified T : Any> intent(noinline transform: Flow<T>.() -> Flow<Any>) {
            transformations.add(Transformation(T::class, transform))
        }

        fun reduce(reducer: suspend STATE.(Any) -> Pair<STATE, List<Any>>) {
            this.reducer = reducer
        }

        class Transformation<T : Any>(
            val clazz: KClass<T>,
            val transform: Flow<T>.() -> Flow<*>,
        )

        fun STATE.state(apply: STATE.() -> STATE) = Pair(apply.invoke(this), listOf<Any>())
        fun STATE.actions(apply: STATE.() -> List<Any>) = Pair(this, apply.invoke(this))
    }

    object FirstBindIntent
}