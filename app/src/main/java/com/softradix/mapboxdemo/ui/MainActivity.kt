package com.softradix.mapboxdemo.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.softradix.mapboxdemo.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var mainBinding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainBinding = ActivityMainBinding.inflate(layoutInflater)
        mainBinding.root.apply {
            setContentView(this)
        }
    }

}