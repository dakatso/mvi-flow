package com.example.mvi_flow_example.one

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.mvi_flow_example.databinding.ActivityMainBinding
import com.example.mvi_flow_example.mvi.MviView
import com.example.mvi_flow_example.one.OneContract.ButtonClickIntent
import com.example.mvi_flow_example.one.OneContract.OneState
import com.example.mvi_flow_example.one.OneContract.ToastAction
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge

@FlowPreview
@ExperimentalCoroutinesApi
class OneActivity : AppCompatActivity(), MviView<OneState> {
    override val viewModel by lazy { ViewModelProvider(this).get(OneViewModel::class.java) }

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun intents(): Flow<Any> = merge(binding.btnStart.clicks().map { ButtonClickIntent })

    override fun render(state: OneState) {
        binding.btnStart.text = state.text
    }
    

    override fun actions(action: Any) {
        when (action) {
            is ToastAction -> Toast.makeText(this, action.text, Toast.LENGTH_SHORT).show()
        }
    }

    @ExperimentalCoroutinesApi
    fun View.clicks(): Flow<View> {
        return callbackFlow {
            setOnClickListener { offer(it) }
            awaitClose { setOnClickListener(null) }
        }
    }
}
