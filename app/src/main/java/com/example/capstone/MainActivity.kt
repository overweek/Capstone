package com.example.capstone

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.capstone.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {


    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }
    private var code : String ="ixpayhTvaQr46BKBbLFk"


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.startbtn.setOnClickListener {
            val intent = Intent(this, RecipePage::class.java)
            intent.putExtra("code", code)
            startActivity(intent)
        }

    }
}