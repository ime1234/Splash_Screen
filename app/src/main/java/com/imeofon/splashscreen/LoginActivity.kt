package com.imeofon.splashscreen

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.ActionBar
import com.google.firebase.auth.FirebaseAuth
import com.imeofon.splashscreen.RegisterActivity.Companion.TAG

class LoginActivity : AppCompatActivity() {

    lateinit var email: EditText
    lateinit var password: EditText
    lateinit var log_button: Button
    lateinit var register: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        supportActionBar?.displayOptions = ActionBar.DISPLAY_SHOW_CUSTOM
        supportActionBar?.setCustomView(R.layout.abs_layout)

        email = findViewById(R.id.email_login)
        password = findViewById(R.id.password_login)
        log_button = findViewById(R.id.login_button)
        register = findViewById(R.id.back_to_register)

        log_button.setOnClickListener {

            val email = email.text.toString()
            val password = password.text.toString()

            Log.d("LoginActivity", "Attempt login with email: $email")
            Log.d("LoginActivity", "Attempt login with password: $password")

            register.visibility = View.GONE

            FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
                .addOnCompleteListener {
                    if (!it.isSuccessful) return@addOnCompleteListener
                    Log.d(TAG, "Successfully logged in: ${it.result!!.user?.uid}")

                    val intent = Intent(this, LatestMessagesActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    overridePendingTransition(R.anim.enter, R.anim.exist)
                }

            register.setOnClickListener {
                finish()
            }

        }
    }
}