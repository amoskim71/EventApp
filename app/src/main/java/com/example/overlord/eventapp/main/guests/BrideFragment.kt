package com.example.overlord.eventapp.main.guests

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.overlord.eventapp.R
import java.io.Serializable
import android.view.View
import android.widget.LinearLayout
import com.bumptech.glide.Glide
import com.example.overlord.eventapp.base.BaseFragment
import com.example.overlord.eventapp.extensions.logError
import com.example.overlord.eventapp.model.User
import kotlinx.android.synthetic.main.fragment_bride.*
import kotlinx.android.synthetic.main.fragment_guest_item.view.*
import java.util.*

class BrideFragment : BaseFragment() {

    class FragmentInputs(val firstName: String = "Diksha", val surname: String = "Agarwal") : Serializable

    interface FragmentInteractor : Serializable {
        //Implement your methods here
        fun onButtonPressed(message: String)
    }

    private var inputs: BrideFragment.FragmentInputs? = null
    private var interactor: FragmentInteractor? = null

    companion object {
        @JvmStatic
        fun newInstance(inputs: BrideFragment.FragmentInputs?, interactor: FragmentInteractor) =
            BrideFragment().apply {this.interactor = interactor
                arguments = Bundle().apply { putSerializable("inputs", inputs) }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inputs = arguments?.getSerializable("inputs") as BrideFragment.FragmentInputs?
        //Initialize Heavier things here because onCreateView and onViewCreated are called much more number of times
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_bride, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        bride_guest_recycler_view.layoutManager = LinearLayoutManager(context, LinearLayout.VERTICAL, false)

        bride_guest_recycler_view.adapter = BrideGuestAdapter()
    }

    inner class BrideGuestAdapter(val guestList: ArrayList<User>) : RecyclerView.Adapter<BrideGuestAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BrideGuestAdapter.ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.fragment_guest_item, parent, false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: BrideGuestAdapter.ViewHolder, position: Int) {
            holder.bindItems(guestList[position])
        }

        override fun getItemCount(): Int {
            return guestList.size
        }

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

            fun bindItems(guest : User) {

                Glide.with(itemView).load(guest.profile_photo).into(itemView.guest_profile_image)
                itemView.guest_name.text = guest.name
                itemView.guest_relation.text = guest.relation

                if(guest.relation == "Bride") {
                    itemView.bride_groom_image.visibility = View.VISIBLE
                    Glide.with(itemView).load(R.drawable.crown).into(itemView.bride_groom_image)
                }

                itemView.call_button.setOnClickListener {
                    base.withPermissions(
                        Manifest.permission.CALL_PHONE
                    ).execute({
                        startActivity( Intent(Intent.ACTION_CALL).setData(Uri.parse(guest.phoneno)) )
                    }, base::logError)
                }

                itemView.message_button.setOnClickListener {
                    startActivity( Intent(Intent.ACTION_VIEW)
                        .setType("vnd.android-dir/mms-sms")
                        .putExtra("Phone Number", guest.phoneno) )
                }

            }
        }
    }
}