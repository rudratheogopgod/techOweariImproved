package com.example.testingapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.testingapp.databinding.ActivityUserDetailsBinding

class userDetails : AppCompatActivity() {
    lateinit var binding: ActivityUserDetailsBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val exception = binding.exceptionMessage
        val progressBar = binding.progressBar
        binding.submitButton.setOnClickListener {
            progressBar.visibility = View.VISIBLE

            val fullName = binding.fullName.text.toString()
            val bloodGroup = binding.bloodGroup.text.toString()
            val gender = binding.gender.text.toString()
            val healthCondition = binding.healthConditions.text.toString()
            val familyContact = binding.familyContact.text.toString()
            val age = binding.age.text.toString()

            val isSuccess = saveUserData(this, fullName, bloodGroup, gender, healthCondition, familyContact, age)
            if (isSuccess) {
                val intent = Intent(this, ImageTaking::class.java)
                startActivity(intent)
                finish()
            } else {
                progressBar.visibility = View.INVISIBLE
                exception.text = "Some Error Occurred, Please Try again"
            }
        }
    }
}

private fun userDetails.saveUserData(
    context: Context,
    fullName: String,
    bloodGroup: String,
    gender: String,
    healthConditions: String,
    familyContact: String,
    age: String
): Boolean {
    val sharePreferences = context.getSharedPreferences("UserData", Context.MODE_PRIVATE)
    val editor = sharePreferences.edit()
    editor.putString("FULL_NAME", fullName)
    editor.putString("BLOOD_GROUP", bloodGroup)
    editor.putString("GENDER", gender)
    editor.putString("HEALTH_CONDITIONS", healthConditions)
    editor.putString("FAMILY_CONTACT", familyContact)
    editor.putString("AGE", age)
    editor.apply()
    return editor.commit()
}
