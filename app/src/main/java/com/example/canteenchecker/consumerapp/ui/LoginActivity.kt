package com.example.canteenchecker.consumerapp.ui

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.canteenchecker.consumerapp.CanteenCheckerApplication
import com.example.canteenchecker.consumerapp.R
import com.example.canteenchecker.consumerapp.databinding.ActivityLoginBinding
import com.example.canteenchecker.consumerapp.proxy.ServiceProxyFactory
import kotlinx.coroutines.*

class LoginActivity : AppCompatActivity() {

    companion object {
        fun createIntent(context: Context?): Intent {
            return Intent(context, LoginActivity::class.java)
        }
    }

    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonLogin.setOnClickListener {
            setUIEnabled(false)

            val userName = binding.editUserName.text.toString()
            val password = binding.editPassworrd.text.toString()

            val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
                error("Exception: ${throwable.localizedMessage}")
            }
            CoroutineScope(Dispatchers.IO + exceptionHandler).launch {
                // background work here
                val authToken = ServiceProxyFactory.createProxy().authenticate(userName, password)
                withContext(Dispatchers.Main) {
                    // UI thread work here
                    if (authToken != null) {
                        // store auth token and return to previous screen
                        (application as CanteenCheckerApplication).authenticationToken = authToken
                        setResult(RESULT_OK)
                        finish()
                    } else {
                        setUIEnabled(true)
                        binding.editPassworrd.text = null
                        Toast.makeText(this@LoginActivity, R.string.message_login_failed, Toast.LENGTH_SHORT).show()
                    }

                }
            }
        }


    }


    private fun setUIEnabled(enabled: Boolean) {
        binding.buttonLogin.isEnabled = enabled
        binding.editUserName.isEnabled = enabled
        binding.editPassworrd.isEnabled = enabled
    }
}