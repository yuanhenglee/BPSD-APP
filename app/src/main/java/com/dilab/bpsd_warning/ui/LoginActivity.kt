package com.dilab.bpsd_warning.ui

import android.content.Intent
import android.content.DialogInterface
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import com.dilab.bpsd_warning.R
import com.dilab.bpsd_warning.MainActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

//    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)


        auth = FirebaseAuth.getInstance()

        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val showPasswordCheckBox = findViewById<CheckBox>(R.id.showPasswordCheckBox)
//        val resetPasswordButton = findViewById<Button>(R.id.resetPasswordButton)
        val loginButton = findViewById<Button>(R.id.loginButton)

        showPasswordCheckBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                passwordEditText.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                passwordEditText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            passwordEditText.setSelection(passwordEditText.text.length)
        }

        loginButton.setOnClickListener {
            var email = emailEditText.text.toString()
            // turn things like "user01" into "nhrikmu+user01@gmail.com"
            if (!email.contains("@")) {
                email = "nhrikmu+" + email + "@gmail.com"
            }
            val password = passwordEditText.text.toString()

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d("LoginActivity", "signInWithEmail:success")
                        val user = auth.currentUser
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w("LoginActivity", "signInWithEmail:failure", task.exception)
                        Log.w("LoginActivity", "email: $email, password: $password")
                        Toast.makeText(baseContext, "Authentication failed.", Toast.LENGTH_SHORT).show()
                    }
                }
        }

//        resetPasswordButton.setOnClickListener {
//            val email = emailEditText.text.toString()
//
//            // pop up a dialog to confirm the email address
//            passwordResetConfirm(email)
//        }

    }

//    private fun passwordResetConfirm(email: String) {
//        // pop up a dialog to confirm the email address
//        val builder = AlertDialog.Builder(this)
//        builder.setTitle("Reset Password")
//
//        if (email.isEmpty()) {
//            builder.setMessage("Please enter an email address.")
//            builder.setPositiveButton("OK") { dialog: DialogInterface, _: Int ->
//                dialog.dismiss()
//            }
//            builder.show()
//            return
//        }
//
//        builder.setMessage("Are you sure you want to reset the password for $email?")
//        builder.setPositiveButton("Yes") { _, _ ->
//            var fixed_email = email
//            if (!email.contains("@"))
//                fixed_email = "nhrikmu+" + email + "@gmail.com"
//            auth.sendPasswordResetEmail(fixed_email)
//                .addOnCompleteListener { task ->
//                    if (task.isSuccessful) {
//                        // Email sent.
//                        Toast.makeText(baseContext, "Reset password email sent.", Toast.LENGTH_SHORT).show()
//                    } else {
//                        Toast.makeText(baseContext, "Failed to send reset password email.", Toast.LENGTH_SHORT).show()
//                    }
//                }
//        }
//        builder.setNegativeButton("No") { dialog: DialogInterface, _: Int ->
//            dialog.dismiss()
//        }
//        builder.show()
//    }

}
