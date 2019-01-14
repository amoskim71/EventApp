package com.example.overlord.eventapp.main.camera

import android.Manifest
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.example.overlord.eventapp.R
import com.example.overlord.eventapp.base.BaseActivity
import com.example.overlord.eventapp.extensions.Firebase.auth
import com.example.overlord.eventapp.extensions.Firebase.firestore
import com.example.overlord.eventapp.extensions.Firebase.storage
import com.example.overlord.eventapp.extensions.logDebug
import com.example.overlord.eventapp.extensions.logError
import com.example.overlord.eventapp.extensions.onTextChange
import com.example.overlord.eventapp.extensions.pushImage
import com.example.overlord.eventapp.mechanisms.compressImage
import com.example.overlord.eventapp.model.Post
import com.example.overlord.eventapp.utils.uniqueName
import kotlinx.android.synthetic.main.fragment_camera.*
import java.io.File
import java.io.Serializable

class CameraFragment : Fragment() {

    class FragmentInputs(val firstName : String = "Shibasis", val surname : String = "Patnaik") : Serializable

    interface FragmentInteractor : Serializable {
        fun onImageUploaded(postID : String)
    }

    private var inputs: FragmentInputs? = null
    private var interactor: FragmentInteractor? = null

    companion object {
        @JvmStatic
        fun newInstance(inputs : FragmentInputs?, interactor : FragmentInteractor) =
            CameraFragment().apply {
                this.interactor = interactor
                arguments = Bundle().apply { putSerializable("inputs", inputs) }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inputs = arguments?.getSerializable("inputs") as FragmentInputs?
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_camera, container, false)
    }

    private var post = Post()
    private var compressedImage : File? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        post.userID = auth.uid ?: "NULL UID"

        photoView.setOnClickListener {
            (activity as BaseActivity).apply {
                withPermissions(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.CAMERA
                ).execute ({
                    takePhoto("Upload Photos")
                        .addOnSuccessListener { image ->
                            logDebug("Cache File ${image.name}")

                            val imageName = uniqueName()

                            compressedImage = compressImage(image, imageName)
                            post.imageID = imageName

                            Glide.with(this).load(compressedImage).into(photoView)
                        }
                }, this::logError)
            }
        }

        contentView.onTextChange { content -> post.content = content }

        submitButton.setOnClickListener {
            interactor?.onImageUploaded("Hello") ?: logError(Error("Null Interactor"))
            compressedImage?.let { image ->
                val newPostID = uniqueName()
                post.postID = newPostID
                firestore.collection("posts")
                    .document(newPostID)
                    .set(post)
                    .addOnSuccessListener {
                        storage.pushImage(compressedImage!!, post.imageID!!)
                            .addOnSuccessListener { resetViews() }
                            .addOnFailureListener { error -> logError(error) }
                    }
                    .addOnFailureListener { error -> logError(error) }
            }
        }
    }

    private fun resetViews() {
        val postID = post.postID

        contentView.setText("")
        photoView.setImageResource(R.drawable.female)

        post = Post()
        compressedImage = null

        interactor?.onImageUploaded(postID)
    }
}
