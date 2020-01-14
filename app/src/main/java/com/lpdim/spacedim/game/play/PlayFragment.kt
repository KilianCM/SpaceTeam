package com.lpdim.spacedim.game.play


import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.navigation.findNavController
import com.lpdim.spacedim.game.GameViewModel
import com.lpdim.spacedim.R
import com.lpdim.spacedim.databinding.FragmentPlayBinding
import com.lpdim.spacedim.game.MoshiService.eventAdapter
import com.lpdim.spacedim.game.WebSocketLiveData
import com.lpdim.spacedim.game.model.Event
import com.lpdim.spacedim.game.model.EventType
import com.lpdim.spacedim.game.model.UIElement
import com.lpdim.spacedim.game.model.UIType
import kotlinx.android.synthetic.main.fragment_play.*
import timber.log.Timber

class PlayFragment : Fragment() {

    private lateinit var viewModel: GameViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val binding: FragmentPlayBinding = DataBindingUtil.inflate(
            inflater, R.layout.fragment_play, container, false)

        viewModel = ViewModelProviders.of(this).get(GameViewModel::class.java)

        viewModel.event.observe(this, Observer { event ->
            Timber.d(event.toString())
            observeEvent(event)
        })

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.let {
            try {
                val event = eventAdapter.fromJson(it.getString("gameStarted")) as Event.GameStarted
                generateUI(event.uiElementList)
            } catch (e: Exception) {
                e.printStackTrace()
                Timber.e("Impossible to generate action buttons")
            }
        }
    }

    private fun finishGame(event: Event.GameOver) {
        val bundle = bundleOf("gameOver" to eventAdapter.toJson(event))
        view?.findNavController()?.navigate(R.id.action_gameFragment_to_finishFragment, bundle)
    }

    private fun observeEvent(event: Event) {
         when(event.type) {
             EventType.NEXT_LEVEL -> nextLevel(event as Event.NextLevel)
             EventType.NEXT_ACTION -> updateAction(event as Event.NextAction)
             EventType.GAME_OVER -> finishGame(event as Event.GameOver)
         }
    }

    private fun updateAction(event: Event.NextAction) {
        textViewAction.text = event.action.sentence
    }

    private fun nextLevel(event: Event.NextLevel) {
        generateUI(event.uiElementList)
    }

    private fun generateUI(uiElements: List<UIElement>) {
        uiElements.forEachIndexed { index: Int, uiElement: UIElement ->
            generateViewComponent(index, uiElement)
        }
    }

    /**
     * Generate UI element (BUTTON or SWITCH) and add it to layout
     */
    private fun generateViewComponent(index: Int, uiElement: UIElement) {
        var layout = layoutUiElementRow1
        if(index % 2 == 0) layout = layoutUiElementRow2

        Timber.d("Generating ${uiElement.type}")

        var generatedElement: Button? = null
        when(uiElement.type) {
            UIType.BUTTON -> generatedElement = Button(activity)
            UIType.SWITCH -> generatedElement = Switch(activity)
            UIType.SHAKE -> generatedElement = Button(activity) //TODO
        }

        generatedElement.id = uiElement.id
        generatedElement.text = uiElement.content
        generatedElement.setOnClickListener {
            sendPlayerAction(uiElement)
            Timber.d("Click on ${it.id}")
        }

        layout.addView(generatedElement)
    }

    /**
     * Send a PlayerAction event to websocket
     */
    private fun sendPlayerAction(uiElement: UIElement) {
        val playerAction = Event.PlayerAction(uiElement)
        val playerActionJson = eventAdapter.toJson(playerAction)

        WebSocketLiveData.webSocket?.send(playerActionJson)
    }
}
