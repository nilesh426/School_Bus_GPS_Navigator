package com.example.schoolbusapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private val db = FirebaseFirestore.getInstance()

    override fun onStart() {
        super.onStart()

        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            redirectUserByRole(user.uid)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvStatus = findViewById<TextView>(R.id.tvStatus)
        val tvGoRegister = findViewById<TextView>(R.id.tvGoRegister)

        tvGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val pass = etPassword.text.toString().trim()

            if (email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Enter email & password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            tvStatus.text = "Logging in..."
            btnLogin.isEnabled = false

            auth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener {
                    val uid = FirebaseAuth.getInstance().currentUser?.uid
                    if (uid != null) {
                        redirectUserByRole(uid)
                    } else {
                        tvStatus.text = ""
                        btnLogin.isEnabled = true
                        Toast.makeText(this, "Login failed", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    tvStatus.text = ""
                    btnLogin.isEnabled = true
                    Toast.makeText(this, "Login failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
        }
    }

    private fun redirectUserByRole(uid: String) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->

                if (!doc.exists()) {
                    Toast.makeText(this, "No user role found in Firestore", Toast.LENGTH_LONG).show()
                    auth.signOut()
                    return@addOnSuccessListener
                }

                val roleRaw = doc.getString("role") ?: ""
                val role = roleRaw.lowercase().trim()

                val intent = when (role) {
                    "parent" -> Intent(this, ParentDashboardActivity::class.java)
                    "driver" -> Intent(this, DriverDashboardActivity::class.java)
                    "admin" -> Intent(this, AdminDashboardActivity::class.java)
                    else -> null
                }

                if (intent == null) {
                    Toast.makeText(this, "Invalid role: $roleRaw", Toast.LENGTH_LONG).show()
                    auth.signOut()
                    return@addOnSuccessListener
                }

                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Role fetch failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }
}