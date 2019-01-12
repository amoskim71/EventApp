package com.example.overlord.eventapp.main.event


import android.os.Bundle
import android.support.v4.app.Fragment
import java.io.Serializable
import com.example.overlord.eventapp.R


class EventFragment : Fragment() {

    //Declare your data here
    class FragmentInputs(val firstName: String = "Diksha", val surname: String = "Agarwal") : Serializable

    interface FragmentInteractor : Serializable {
        //Implement your methods here
        fun onButtonPressed(message: String)
    }

    private lateinit var inputs: FragmentInputs
    private lateinit var interactor: FragmentInteractor

    companion object {
        @JvmStatic
        fun newInstance(inputs: FragmentInputs, interactor: FragmentInteractor) =
            EventFragment().apply {
                arguments = Bundle().apply {
                    putSerializable("inputs", inputs)
                    putSerializable("interactor", interactor)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inputs = arguments?.getSerializable("inputs") as FragmentInputs
        interactor = arguments?.getSerializable("interactor") as FragmentInteractor

        //Initialize Heavier things here because onCreateView and onViewCreated are called much more number of times
    }
}