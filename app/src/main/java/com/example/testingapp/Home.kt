package com.example.testingapp

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.testingapp.databinding.ActivityHomeBinding
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.core.app.ActivityCompat
import android.Manifest
import android.app.PendingIntent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.CountDownTimer
import android.os.VibrationEffect
import android.os.Vibrator
import android.telephony.SmsManager
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.FileInputStream
import kotlin.text.matches

class Home : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private lateinit var database: DatabaseReference
    private var countdownTimer: CountDownTimer? = null
    private var currentLocation: LatLng? = null
    private var backPressedOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Request SMS permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), SMS_PERMISSION_REQUEST_CODE)
        }

        val sharedPreferences = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val fullName = sharedPreferences.getString("FULL_NAME", "") ?: ""
        val age = sharedPreferences.getString("AGE", "") ?: ""
        val bloodGroup = sharedPreferences.getString("BLOOD_GROUP", "") ?: ""
        val healthConditions = sharedPreferences.getString("HEALTH_CONDITIONS", "") ?: ""
        val gender = sharedPreferences.getString("GENDER", "") ?: ""
        val latitude = sharedPreferences.getFloat("LATITUDE", 0.0f)
        val longitude = sharedPreferences.getFloat("LONGITUDE", 0.0f)
        val locationData = "geo:$latitude,$longitude"
        val locationUrl = if (true) {
            "http://maps.google.com/?q=${latitude},${longitude}"
        } else {
            "Location not available"
        }
        val checkNumber = sharedPreferences.getString("FAMILY_CONTACT", "") ?: ""
        val familyContact = if (checkNumber.matches("^\\+[0-9]{10,15}$".toRegex())) {
            checkNumber
        } else {
            "+91$checkNumber" // Add country code if missing
        }

        binding.userName.text = fullName
        binding.uersAge.text = "Age - ${age}"

        binding.locationButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(locationData))
            intent.setPackage("com.google.android.apps.maps")
            startActivity(intent)
        }

        databaseListner(familyContact, fullName, bloodGroup, age, healthConditions, gender, locationUrl)

        binding.police.setOnClickListener {
            makePhoneCall(familyContact)
        }

        binding.emergencyButton.setOnClickListener {
            sendSMS(familyContact,locationUrl, fullName, bloodGroup, age, healthConditions, gender)
            vibe(it)
        }

        val fileName = getFileNameFromStorage()
        if (fileName != null) {
            val bitmap = loadImageFromStorage(fileName)
            if (bitmap != null) {
                binding.profilePic.setImageBitmap(bitmap)
            } else {
                Toast.makeText(this, "Failed to load profile picture", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Profile picture file not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun databaseListner(phoneNumber: String, userName: String, bloodGroup: String, age: String, healthConditions: String, gender: String, locationUrl: String) {
        val context = this
        val sharePreferences = context.getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val editor = sharePreferences.edit()
        database = FirebaseDatabase.getInstance().getReference()

        val postListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    // Safely retrieve latitude and longitude as Float/Double and convert to String
                    val lat = snapshot.child("user/latitude").value?.toString()?.toDouble() // Convert to String
                    val longi = snapshot.child("user/longitude").value?.toString()?.toDouble() // Convert to String
                    val status = snapshot.child("user/touch_status").value?.toString()?.toBoolean()

                    if (lat != null && longi != null) {
                        editor.putFloat("LATITUDE", lat.toFloat()) // Store as String
                        editor.putFloat("LONGITUDE", longi.toFloat()) // Store as String
                        editor.apply()

                        if (status == true) {
                            sendSMS(phoneNumber, locationUrl, userName, bloodGroup, healthConditions, gender, age)
                        }
                    } else {
                        Toast.makeText(this@Home, "Invalid data in snapshot", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@Home, "Snapshot does not exist", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@Home, "Failed to read pulse data", Toast.LENGTH_SHORT).show()
            }
        }
        database.addValueEventListener(postListener)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            SMS_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "SMS permission granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show()
                }
            }
            CALL_PHONE_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    val sharedPreferences = getSharedPreferences("UserData", Context.MODE_PRIVATE)
                    val checkNumber = sharedPreferences.getString("FAMILY_CONTACT", "") ?: ""
                    val familyContact = if (checkNumber.matches("^\\+[0-9]{10,15}$".toRegex())) {
                        checkNumber
                    } else {
                        "+91$checkNumber"
                    }
                    makePhoneCall(familyContact)
                } else {
                    Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
                }
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
        }, 2000)
    }

    private fun sendSMS(phoneNumber: String, location: String, userName: String, bloodGroup: String, healthConditions: String, gender: String, age: String) {
        val message = "Please Help me! \n I am $userName \n I am $age \n My blood group -> $bloodGroup \n Location -> $location."
        try {
            val smsManager: SmsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phoneNumber, null, message, null, null)
            Log.d("SMS", "SMS triggered: $message")
            Toast.makeText(this, "Help message sent", Toast.LENGTH_SHORT).show()

            // Verify delivery (optional)
            val sentPI = PendingIntent.getBroadcast(this, 0, Intent("SMS_SENT"), PendingIntent.FLAG_IMMUTABLE)
            smsManager.sendTextMessage(phoneNumber, null, message, sentPI, null)
        } catch (e: Exception) {
            Log.e("SMS", "Silent failure: ${e.message}")
        }
    }

    private fun getFileNameFromStorage(): String? {
        return try {
            val fileInputStream: FileInputStream = openFileInput("profilePicFileName.txt")
            val fileNameBytes = fileInputStream.readBytes()
            String(fileNameBytes)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("Storage", "Error reading file name from storage", e)
            null
        }
    }

    private fun loadImageFromStorage(fileName: String): Bitmap? {
        return try {
            val fileInputStream = openFileInput(fileName)
            BitmapFactory.decodeStream(fileInputStream)
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("Storage", "Error loading image from storage", e)
            null
        }
    }

    private fun showConfirmationDialog(phoneNumber: String, userName: String, bloodGroup: String, age: String, healthConditions: String, gender: String, latitude: Float, longitude: Float) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Do you Need Help")
        builder.setMessage("Are you Sure!")
        builder.setPositiveButton("Yes") { _, _ ->
            val locationUrl = if (true) {
                "http://maps.google.com/?q=${latitude},${longitude}"
            } else {
                "Location not available"
            }
            sendSMS(phoneNumber, locationUrl, userName, bloodGroup, healthConditions, gender, age)
            Toast.makeText(this, "Message sent", Toast.LENGTH_SHORT).show()
        }
        builder.setNegativeButton("No") { _, _ ->
            countdownTimer?.cancel()
            countdownTimer = null
            Toast.makeText(this, "No message is sent", Toast.LENGTH_SHORT).show()
        }

        val dialog = builder.create()
        dialog.show()
        countdownTimer = object : CountDownTimer(10000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                dialog.setMessage("Are you Sure! Sending in...${millisUntilFinished / 1000} seconds.")
            }
            override fun onFinish() {
                if (currentLocation == null) {
                    Toast.makeText(this@Home, "Location not available. Retrying...", Toast.LENGTH_SHORT).show()
                    Handler(Looper.getMainLooper()).postDelayed({
                        currentLocation?.let { loc ->
                            // Use the location
                        } ?: run {
                            Toast.makeText(this@Home, "Failed to get location", Toast.LENGTH_SHORT).show()
                        }
                    }, 2000)
                } else {
                    // Use the location
                }
            }
        }.start()
    }


    fun vibe(v: View) {
        val vibrate = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        vibrate?.let {
            if (Build.VERSION.SDK_INT >= 26) {
                it.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                it.vibrate(500)
            }
        } ?: run {
            Toast.makeText(this, "Vibrator service not available", Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        private const val CALL_PHONE_PERMISSION_REQUEST_CODE = 1
        private const val SMS_PERMISSION_REQUEST_CODE = 101
    }
}

private fun Home.makePhoneCall(phoneNumber: String?) {
    if (phoneNumber.isNullOrEmpty()) {
        Toast.makeText(this, "Phone number is missing", Toast.LENGTH_SHORT).show()
        return
    }
    val callIntent = Intent(Intent.ACTION_CALL)
    callIntent.data = Uri.parse("tel:$phoneNumber")
    startActivity(callIntent)
}