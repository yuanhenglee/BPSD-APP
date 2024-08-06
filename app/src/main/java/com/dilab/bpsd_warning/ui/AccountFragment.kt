package com.dilab.bpsd_warning.ui

import android.os.Bundle
import android.content.Intent
import android.content.SharedPreferences
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.util.Log
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import com.dilab.bpsd_warning.databinding.FragmentAccountBinding
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.auth.FirebaseAuth

class AccountFragment : Fragment() {

    private var _binding: FragmentAccountBinding? = null

    private lateinit var auth: FirebaseAuth

    private val binding get() = _binding!!

    //Vivowatch5-KMUH-0001 ~ Vivowatch5-KMUH-0062
//    var devices = (1..62).map { "Vivowatch5-KMUH-00${it.toString().padStart(2, '0')}" }
    private var devices = listOf<String>()

    private lateinit var multiAutoCompleteTextView: MaterialAutoCompleteTextView

    override fun onAttach(context: Context) {
        super.onAttach(context)
        auth = FirebaseAuth.getInstance()

        // little hack to reveal all device option for me
        if (auth.currentUser?.email?.contains("user") == true) {
            val device_id = auth.currentUser?.displayName?.substringAfter("user")
            devices = listOf("Vivowatch5-KMUH-00$device_id")
        }
        else if (auth.currentUser?.email?.contains("admin") == true) {
            devices = (1..62).map { "Vivowatch5-KMUH-00${it.toString().padStart(2, '0')}" }
            devices += "All"
        }
        else {
            Log.d("AccountFragment", "No device found")
            devices = listOf("All")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentAccountBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val currentUser = auth.currentUser

        val emailTextView = binding.emailTextView
        val username = currentUser?.displayName
        if (username != null) {
            emailTextView.text = username
        } else {
            emailTextView.text = currentUser?.email
        }

        setupDeviceDropdown()

        // logout button
        val logoutButton = binding.logoutButton
        logoutButton.setOnClickListener {
            auth.signOut()
            val intent = Intent(context, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }


        return root
    }

    private fun setupDeviceDropdown() {
        multiAutoCompleteTextView = binding.deviceDropdown
        val sharedPreferences: SharedPreferences = requireContext().getSharedPreferences("device", Context.MODE_PRIVATE)

        // if a device is saved, select it
        val savedDevice = sharedPreferences.getString("device", null)
        if (savedDevice != null) {
            if ( savedDevice !in devices) {
                with(sharedPreferences.edit()) {
                    remove("device")
                    apply()
                }
            }
            else
                multiAutoCompleteTextView.setText(savedDevice, false)
        }
        multiAutoCompleteTextView.setAdapter(
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_list_item_single_choice,
                devices
            )
        )
        // Save the selected device
        multiAutoCompleteTextView.setOnItemClickListener { parent, view, position, id ->
            val selectedDevice = parent.getItemAtPosition(position).toString()
            val oldDevice = sharedPreferences.getString("device", null)
            if (oldDevice == selectedDevice ) {
                return@setOnItemClickListener
            }
            else if (oldDevice != null) {
                FirebaseMessaging.getInstance().unsubscribeFromTopic(oldDevice)
                    .addOnCompleteListener() { task ->
                        var msg = "Unsubscribed from $oldDevice"
                        if (!task.isSuccessful) {
                            msg = "Failed to unsubscribe from $oldDevice"
                        }
                        Log.d("AccountFragment", msg)
                    }
            }

            with(sharedPreferences.edit()) {
                putString("device", selectedDevice)
                apply()
            }
            subscribeToTopic(selectedDevice)
        }
    }

    override fun onResume() {
        super.onResume()
        val arrayAdapter = ArrayAdapter<String>(
            requireContext(),
            android.R.layout.simple_list_item_1,
            devices
        )
        multiAutoCompleteTextView.setAdapter(arrayAdapter)
    }

    private fun subscribeToTopic(selectedDevices: String) {
        // subscribe to the selected device
        FirebaseMessaging.getInstance().subscribeToTopic(selectedDevices)
            .addOnCompleteListener() { task ->
                var msg = "Subscribed to $selectedDevices"
                if (!task.isSuccessful) {
                    msg = "Failed to subscribe to $selectedDevices"
                }
                Log.d("AccountFragment", msg)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
