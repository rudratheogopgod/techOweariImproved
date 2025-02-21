package com.example.testingapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.testingapp.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private var backPressedOnce = false
    private lateinit var binding: ActivityMainBinding //initializing the binding and firebase late in it var
    private lateinit var firebaseAuth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (checkLoginStatus(this)){
            val intent = Intent(this, Home::class.java)
            startActivity(intent)
            finish()
        }
        firebaseAuth = FirebaseAuth.getInstance()
        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        binding.registerText.setOnClickListener{
            val intent = Intent(this, Signup::class.java)
            startActivity(intent)
        }

        binding.loginButton.setOnClickListener{
            val incorrectFields = binding.incorrectFields
            incorrectFields.visibility = View.INVISIBLE
            val progressBar = binding.progressBar
            progressBar.visibility = View.VISIBLE
            val email = binding.emailInput.text.toString()
            val pass = binding.passwordInput.text.toString()

            if (email.isNotEmpty() && pass.isNotEmpty()){
                firebaseAuth.signInWithEmailAndPassword(email, pass).addOnCompleteListener{
                    if (it.isSuccessful){
                        progressBar.visibility = View.INVISIBLE
                        setLoggedInStatus(this, true)
                        Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, userDetails::class.java)
                        startActivity(intent)
                        finish()
                    }else{
                        progressBar.visibility = View.INVISIBLE
                        val incorrectFields = binding.incorrectFields
                        incorrectFields.visibility = View.VISIBLE
                        incorrectFields.text = "Incorrect Username or Password"
                    }
                }
            }else{
                progressBar.visibility = View.INVISIBLE
                val incorrectFields = binding.incorrectFields
                incorrectFields.visibility = View.VISIBLE
                incorrectFields.text = "Empty Fields are not allowed"
            }
        }

    }
    override fun onBackPressed() {
        if (backPressedOnce) {
            super.onBackPressed()
            return
        }

        this.backPressedOnce = true
        Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show()

        Handler(Looper.getMainLooper()).postDelayed({
            backPressedOnce = false
        }, 2000) // 2 seconds delay
    }
    private fun MainActivity.setLoggedInStatus(context: Context, set: Boolean){
        val sharePreferences = context.getSharedPreferences("LoginStatus", Context.MODE_PRIVATE)
        val editor = sharePreferences.edit()
        editor.putBoolean("LOGIN_STATUS", set)
        editor.apply()
    }

    public fun checkLoginStatus(context: Context): Boolean{
        val sharePreferences = context.getSharedPreferences("LoginStatus", Context.MODE_PRIVATE)
        val status = sharePreferences.getBoolean("LOGIN_STATUS", false)//false as the default value
        return status
    }
}