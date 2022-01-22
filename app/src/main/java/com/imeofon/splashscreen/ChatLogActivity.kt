package com.imeofon.splashscreen

import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import com.imeofon.splashscreen.models.ChatMessage
import com.imeofon.splashscreen.models.User
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.imeofon.splashscreen.utils.DateUtils.getFormattedTimeChatLog
import kotlinx.android.synthetic.main.chat_from_row.view.*
import kotlinx.android.synthetic.main.chat_to_row.view.*


class ChatLogActivity : AppCompatActivity() {

    lateinit var swiperefresh: SwipeRefreshLayout
    lateinit var recyclerview_chat_log: RecyclerView
    lateinit var send_button_chat_log: FloatingActionButton
    lateinit var edittext_chat_log: EditText

    companion object {
        val TAG = ChatLogActivity::class.java.simpleName
    }

    val adapter = GroupAdapter<RecyclerView.ViewHolder>()

    // Bundle Data
    private val toUser: User?
        get() = intent.getParcelableExtra(NewMessageActivity.USER_KEY)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_log)

        swiperefresh=findViewById(com.google.firebase.database.R.id.swiperefresh)
        recyclerview_chat_log=findViewById(com.google.firebase.database.R.id.recyclerview_chat_log)
        send_button_chat_log=findViewById(com.google.firebase.database.R.id.send_button_chat_log)
        edittext_chat_log=findViewById(com.google.firebase.database.R.id.edittext_chat_log)

        swiperefresh.setColorSchemeColors(ContextCompat.getColor(this, com.google.firebase.database.R.color.colorAccent))

        recyclerview_chat_log.adapter = adapter

        supportActionBar?.title = toUser?.name

        listenForMessages()

        send_button_chat_log.setOnClickListener {
            performSendMessage()
        }
    }

    private fun listenForMessages() {
        swiperefresh.isEnabled = true
        swiperefresh.isRefreshing = true

        val fromId = FirebaseAuth.getInstance().uid ?: return
        val toId = toUser?.uid
        val ref = FirebaseDatabase.getInstance().getReference("/user-messages/$fromId/$toId")

        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(databaseError: DatabaseError) {
                Log.d(TAG, "database error: " + databaseError.message)
            }

            override fun onDataChange(dataSnapshot: DataSnapshot) {
                Log.d(TAG, "has children: " + dataSnapshot.hasChildren())
                if (!dataSnapshot.hasChildren()) {
                    swiperefresh.isRefreshing = false
                    swiperefresh.isEnabled = false
                }
            }
        })

        ref.addChildEventListener(object : ChildEventListener {
            override fun onCancelled(databaseError: DatabaseError) {
            }

            override fun onChildMoved(dataSnapshot: DataSnapshot, previousChildName: String?) {
            }

            override fun onChildChanged(dataSnapshot: DataSnapshot, previousChildName: String?) {
            }

            override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
                dataSnapshot.getValue(ChatMessage::class.java)?.let {
                    if (it.fromId == FirebaseAuth.getInstance().uid) {
                        val currentUser = LatestMessagesActivity.currentUser ?: return
                        adapter.add(ChatFromItem(it.text, currentUser, it.timestamp))
                    } else {
                        adapter.add(ChatToItem(it.text, toUser!!, it.timestamp))
                    }
                }
                recyclerview_chat_log.scrollToPosition(adapter.itemCount - 1)
                swiperefresh.isRefreshing = false
                swiperefresh.isEnabled = false
            }

            override fun onChildRemoved(dataSnapshot: DataSnapshot) {
            }

        })

    }

    private fun performSendMessage() {
        val text = edittext_chat_log.text.toString()
        if (text.isEmpty()) {
            Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        val fromId = FirebaseAuth.getInstance().uid ?: return
        val toId = toUser?.uid

        val reference = FirebaseDatabase.getInstance().getReference("/user-messages/$fromId/$toId").push()
        val toReference = FirebaseDatabase.getInstance().getReference("/user-messages/$toId/$fromId").push()

        val chatMessage = ChatMessage(reference.key!!, text, fromId, toId.toString(), System.currentTimeMillis() / 1000)
        reference.setValue(chatMessage)
            .addOnSuccessListener {
                Log.d(TAG, "Saved our chat message: ${reference.key}")
                edittext_chat_log.text.clear()
                recyclerview_chat_log.smoothScrollToPosition(adapter.itemCount - 1)
            }

        toReference.setValue(chatMessage)


        val latestMessageRef = FirebaseDatabase.getInstance().getReference("/latest-messages/$fromId/$toId")
        latestMessageRef.setValue(chatMessage)

        val latestMessageToRef = FirebaseDatabase.getInstance().getReference("/latest-messages/$toId/$fromId")
        latestMessageToRef.setValue(chatMessage)
    }

}

class ChatFromItem(val text: String, val user: com.google.firebase.firestore.auth.User, val timestamp: Long) : Item<ViewHolder>() {

    override fun bind(viewHolder: ViewHolder, position: Int) {

        viewHolder.itemView.textview_from_row.text = text
        viewHolder.itemView.from_msg_time.text = getFormattedTimeChatLog(timestamp)

        val targetImageView = viewHolder.itemView.imageview_chat_from_row

        if (!user.profileImageUrl!!.isEmpty()) {

            val requestOptions = RequestOptions().placeholder(com.google.firebase.database.R.drawable.no_image2)


            Glide.with(targetImageView.context)
                .load(user.profileImageUrl)
                .thumbnail(0.1f)
                .apply(requestOptions)
                .into(targetImageView)

        }
    }

    override fun getLayout(): Int {
        return com.google.firebase.database.R.layout.chat_from_row
    }

}

class ChatToItem(val text: String, val user: User, val timestamp: Long) : Item<ViewHolder>() {

    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.textview_to_row.text = text
        viewHolder.itemView.to_msg_time.text = getFormattedTimeChatLog(timestamp)

        val targetImageView = viewHolder.itemView.imageview_chat_to_row

        if (!user.profileImageUrl!!.isEmpty()) {

            val requestOptions =
                RequestOptions().placeholder(com.google.firebase.database.R.drawable.no_image2)

            Glide.with(targetImageView.context)
                .load(user.profileImageUrl)
                .thumbnail(0.1f)
                .apply(requestOptions)
                .into(targetImageView)

        }
    }

    override fun getLayout(): Int {
        return com.google.firebase.database.R.layout.chat_to_row
    }

}

