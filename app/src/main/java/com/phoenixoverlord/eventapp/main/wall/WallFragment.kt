package com.phoenixoverlord.eventapp.main.wall


import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.SimpleItemAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView

import com.phoenixoverlord.eventapp.R
import com.phoenixoverlord.eventapp.base.BaseFragment
import com.phoenixoverlord.eventapp.extensions.*
import com.phoenixoverlord.eventapp.extensions.Firebase.auth
import com.phoenixoverlord.eventapp.extensions.Firebase.firestore

import com.phoenixoverlord.eventapp.model.Post
import com.phoenixoverlord.eventapp.model.User
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import kotlinx.android.synthetic.main.fragment_wall.*
import kotlinx.android.synthetic.main.fragment_wall_item.*
import kotlinx.android.synthetic.main.fragment_wall_item.view.*
import java.io.Serializable
import java.lang.Error
import java.text.SimpleDateFormat
import java.util.*

class WallFragment : BaseFragment() {

    //Declare your data here
    class FragmentInputs(val postID: String? = null) : Serializable

    interface FragmentInteractor : Serializable {
        //Implement your methods here
        fun addPostFragment(post: Post)
    }

    private var inputs: FragmentInputs? = null
    private var interactor: FragmentInteractor? = null

    companion object {
        @JvmStatic
        fun newInstance(inputs: FragmentInputs?, interactor: FragmentInteractor) =
            WallFragment().apply {
                this.interactor = interactor
                arguments = Bundle().apply { putSerializable("inputs", inputs) }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        inputs = arguments?.getSerializable("inputs") as FragmentInputs?
        //Initialize Heavier things here because onCreateView and onViewCreated are called much more number of times
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_wall, container, false)
    }

    fun setupFirestoreRecyclerView(query : Query) {
        val firestoreOptions = FirestoreRecyclerOptions.Builder<Post>()
            .setLifecycleOwner(this)
            .setQuery(query, Post::class.java)
            .build()

        val firestoreRecyclerAdapter = object : FirestoreRecyclerAdapter<Post, PostHolder>(firestoreOptions) {
            override fun onCreateViewHolder(p0: ViewGroup, p1: Int): PostHolder {
                return PostHolder(
                    p0.inflate(R.layout.fragment_wall_item)
                )
            }

            override fun onBindViewHolder(holder: PostHolder, position: Int, model: Post) {
                holder.bindItems(model)
            }
        }

        wallRecyclerView.setHasFixedSize(true)
        wallRecyclerView.layoutManager = LinearLayoutManager(context, LinearLayout.VERTICAL, false)
        wallRecyclerView.adapter = firestoreRecyclerAdapter
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val firestoreQuery = firestore
            .collection("posts")
            .orderBy("date", Query.Direction.DESCENDING)

        setupFirestoreRecyclerView(firestoreQuery)
    }

    inner class PostHolder(itemView : View) : RecyclerView.ViewHolder(itemView) {
        fun bindItems(post : Post) {
            itemView.apply {

                val postDocument = firestore.collection("posts").document(post.postID)
                val userDocument = firestore.collection("users").document(post.userID)

                val currentUserID = auth.currentUser?.uid

                // Messy
                setupLikeView(post)
                setupLikeButton(post, currentUserID, postDocument)
                setupDateView(post)
                setupCommentButton(post.postID)
                setupImage(post)
                setupProfilePhoto(userDocument, post)
            }
        }

        private fun View.setupLikeView(post: Post) {
            postLikesView.text = "${post.likedByUIDs.size} likes"
            setupLikeDialog(post, postLikesView)
        }

        private fun View.setupLikeButton(
            post: Post,
            currentUserID: String?,
            postDocument: DocumentReference
        ) {
            postLikeButton.isChecked = post.likedByUIDs.contains(currentUserID)

            postLikeButton.setOnCheckedChangedListener { _, checked ->
                val task =
                    if (checked)
                        FieldValue.arrayUnion(currentUserID)
                    else
                        FieldValue.arrayRemove(currentUserID)

                postDocument.update("likedByUIDs", task)
                    .addOnFailureListener { error -> logError(error) }
            }
        }

        private fun View.setupDateView(post: Post) {
            val dateFormat = SimpleDateFormat("dd MMM 'at' h a", Locale.UK)
            postDateView.text = dateFormat.format(post.date)
        }

        private fun View.setupCommentButton(postID : String) {
            postCommentButton.setOnClickListener {
                loadFragment(PostFragment.newInstance(postID))
            }
        }

        private fun View.setupImage(post: Post) {

            val view = this
            post.imageID?.apply {

                base.apply {

                    loadImage(view.postImageView, post.imageID!!).setOnClickListener {
                        loadFragment(ImageFragment.newInstance(post.imageID!!))
                    }

                    downloadImage(post.imageID!!) { file ->
                        view.postShareButton.setOnClickListener {
                            safeIntentDispatch(
                                Intent(Intent.ACTION_SEND).apply {
                                    putExtra(Intent.EXTRA_STREAM, getExternallyAccessibleURI(file))
                                    type = "image/jpeg"
                                }
                            )
                        }
                    }
                }
            } ?: logError(Error("No Image ID"))
        }

        private fun View.setupProfilePhoto(
            userDocument: DocumentReference,
            post: Post
        ) {
            userDocument.addSnapshotListener(User::class.java) { user ->
                postAuthorView.text = user.name
                //ToDo this image can be made by applying Transform on glide.
                base.loadCircularImage(postAuthorProfileView, user.profile_photo)
                postCaptionView.text = post.content
            }
        }
    }


    private fun createLikedByDialog(users : List<String>) : AlertDialog {
        return AlertDialog.Builder(base)
            .setTitle("Liked By")
            .setItems(users.toTypedArray()) { _, _ -> }
            .create()
    }

    private fun setupLikeDialog(post : Post, likeView: View) {

        val userPromises = post.likedByUIDs
            .map { firestore.document("users/$it").get() }

        Tasks.whenAll(userPromises).addOnSuccessListener {
            val userNames =
                userPromises
                    .mapNotNull { it.result }
                    .mapNotNull { it.toObject(User::class.java) }
                    .map { it.name }

            likeView.setOnClickListener {
                createLikedByDialog(userNames).show()
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        fragmentManager?.beginTransaction()
            ?.replace(R.id.fragmentContainer, fragment, fragment.getSimpleName())
            ?.addToBackStack(fragment.getSimpleName())
            ?.commit() ?: logError(Error("Null Fragment Manager"))
    }

}