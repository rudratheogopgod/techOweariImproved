package com.example.testingapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.testingapp.databinding.ActivitySignupBinding
import com.google.firebase.auth.FirebaseAuth
import android.content.Intent
import android.view.View
import android.widget.Toast

class Signup : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firebaseAuth = FirebaseAuth.getInstance()
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener{
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        binding.registerButton.setOnClickListener{
            val exceptionText = binding.exceptionMessage
            exceptionText.visibility =  View.INVISIBLE
            val progressBar = binding.progressBar
            progressBar.visibility = View.VISIBLE
            val email = binding.email.text.toString()
            val password = binding.password.text.toString()
            val confirmPassword = binding.confirmPassword.text.toString()
            if (email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()){
                if (password == confirmPassword){
                    firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener{
                        if (it.isSuccessful){
                            progressBar.visibility = View.INVISIBLE
                            val intent = Intent(this, UserDetails::class.java)
                            startActivity(intent)
                            Toast.makeText(this, "Successfully Signed Up" , Toast.LENGTH_SHORT).show()
                            finish()
                        }else{
                            progressBar.visibility = View.INVISIBLE
                            Toast.makeText(this, it.exception.toString() , Toast.LENGTH_SHORT).show()
                            exceptionText.visibility =  View.VISIBLE
                            exceptionText.text = "An Error Occured, Please Try Again"
                        }
                    }
                }else{
                    progressBar.visibility = View.INVISIBLE
                    exceptionText.visibility =  View.VISIBLE
                    exceptionText.text = "Passwords do not match"
                }
            }else{
                progressBar.visibility = View.INVISIBLE
                exceptionText.visibility =  View.VISIBLE
                exceptionText.text = "Empty Fields are Not Allowed"
            }
        }
    }
}