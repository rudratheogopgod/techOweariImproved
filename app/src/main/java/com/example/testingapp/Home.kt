package com.example.testingapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.telephony.SmsManager
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.testingapp.databinding.ActivityHomeBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import java.io.FileInputStream


class Home : AppCompatActivity() {
    private lateinit var binding: ActivityHomeBinding
    private var countdownTimer: CountDownTimer? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var currentLocation: LatLng? = null
    private var backPressedOnce = false

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult
                for (location in locationResult.locations) {
                    currentLocation = LatLng(location.latitude, location.longitude)
                    val loc = currentLocation
                    loc?.let {
                        // Use the location data as needed
                        Log.d("Location", "Current Location: ${it.latitude}, ${it.longitude}")
                    }
                }
            }
        }

        // Request location permissions
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            // Permissions already granted, start the service
            startLocationUpdates()
        }

        // Request SMS permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), SMS_PERMISSION_REQUEST_CODE)
        }

        val sharedPreferences = getSharedPreferences("UserData", Context.MODE_PRIVATE)
        val fullName = sharedPreferences.getString("FULL_NAME", "")
        val age = sharedPreferences.getString("AGE", "")
        val bloodGroup = sharedPreferences.getString("BLOOD_GROUP", "")
        val healthConditions = sharedPreferences.getString("HEALTH_CONDITIONS", "")
        val gender = sharedPreferences.getString("GENDER", "")
        val checkNumber = sharedPreferences.getString("FAMILY_CONTACT", "")
        val familyContact = if (checkNumber != null && checkNumber.matches("^\\+[0-9]{10,15}$".toRegex())) {
            checkNumber
        } else {
            "+91$checkNumber"
        }

        //navigate to profile
        binding.profilePic.setOnClickListener{
            val intent = Intent(this, Profile::class.java)
            startActivity(intent)
        }

        //navigate to hospital page
        binding.hospital.setOnClickListener{
            val intent = Intent(this, Hospital::class.java)
            startActivity(intent)
        }

        binding.userName.text = fullName
        binding.uersAge.text = "Age - $age"

        binding.police.setOnClickListener{
            makePhoneCall(familyContact)
        }

        binding.emergencyButton.setOnClickListener {
            showConfirmationDialog(familyContact.toString(), fullName.toString(), bloodGroup.toString(), age.toString(), healthConditions.toString(), gender.toString())
            vibe(it)
        }

        val fileName = getFileNameFromStorage()
        if (fileName != null) {
            val bitmap = loadImageFromStorage(fileName)
            if (bitmap != null) {
                binding.profilePic.setImageBitmap(bitmap)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLocationUpdates()
                } else {
                    Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show()
                }
            }
            SMS_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "SMS permission granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show()
                }
            }
            CALL_PHONE_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Call permission granted", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Call permission denied", Toast.LENGTH_SHORT).show()
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

    private fun startLocationUpdates() {
        if (!::fusedLocationClient.isInitialized) {
            Log.e("Home", "FusedLocationClient not initialized")
            return
        }

        val locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
    }

    private fun sendSMS(phoneNumber: String, location: String, userName: String, bloodGroup: String, healthConditions: String, gender: String, age: String) {
        // Validate phone number format (e.g., +1234567890)
        if (!phoneNumber.matches("^\\+[0-9]{10,15}$".toRegex())) {
            Toast.makeText(this, "Invalid phone number format", Toast.LENGTH_SHORT).show()
            Log.e("SMS", "Invalid phone number: $phoneNumber")
            return
        }

        val message = "I need Help. I am $userName. I am $age years old $gender. My blood group is $bloodGroup. Health condition -> $healthConditions. Location -> $location"

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            val smsManager: SmsManager? = SmsManager.getDefault()
            smsManager?.let {
                try {
                    it.sendTextMessage(phoneNumber, null, message, null, null)
                    Toast.makeText(this, "Help message sent!", Toast.LENGTH_SHORT).show()
                    Log.d("SMS", "SMS sent: $message")
                } catch (e: Exception) {
                    Log.e("SMS", "Failed to send SMS: ${e.message}", e)
                    Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } ?: run {
                Log.e("SMS", "SmsManager is null")
                Toast.makeText(this, "SMS service unavailable", Toast.LENGTH_SHORT).show()
            }
        } else {
            // Re-request permission if denied earlier
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), SMS_PERMISSION_REQUEST_CODE)
            Toast.makeText(this, "SMS permission required", Toast.LENGTH_SHORT).show()
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

    private fun showConfirmationDialog(phoneNumber: String, userName: String, bloodGroup: String, age: String, healthConditions: String, gender: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Do you Need Help")
        builder.setMessage("Are you Sure!")
        builder.setPositiveButton("Yes") { _, _ ->
            currentLocation?.let { loc ->
                val locationUrl = "http://maps.google.com/?q=${loc.latitude},${loc.longitude}"
                sendSMS(phoneNumber, locationUrl, userName, bloodGroup, healthConditions, gender, age)
            }
            Toast.makeText(this, "Help has been sent", Toast.LENGTH_SHORT).show()
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
                    // Retry after 2 seconds to get location
                    Handler(Looper.getMainLooper()).postDelayed({
                        currentLocation?.let { loc ->
                            val url = "http://maps.google.com/?q=${loc.latitude},${loc.longitude}"
                            sendSMS(phoneNumber, url, userName, bloodGroup, healthConditions, gender, age)
                        } ?: run {
                            Toast.makeText(this@Home, "Failed to get location", Toast.LENGTH_SHORT).show()
                        }
                    }, 2000)
                } else {
                    val locationUrl = "http://maps.google.com/?q=${currentLocation!!.latitude},${currentLocation!!.longitude}"
                    sendSMS(phoneNumber, locationUrl, userName, bloodGroup, healthConditions, gender, age)
                }
            }
        }.start()
    }


    fun vibe(v: View) {
        val vibrate = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= 26) {
            vibrate.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrate.vibrate(500)
        }
    }

    companion object {
        private const val CALL_PHONE_PERMISSION_REQUEST_CODE = 1
        private const val LOCATION_PERMISSION_REQUEST_CODE = 100
        private const val SMS_PERMISSION_REQUEST_CODE = 101 // Unique code for SMS
    }
}

private fun Home.makePhoneCall(phoneNumber: String) {
    val callIntent = Intent(Intent.ACTION_CALL)
    callIntent.data = Uri.parse("tel:$phoneNumber")
    startActivity(callIntent)
}
