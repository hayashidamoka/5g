package com.teampansaru.fiveg

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter

enum class WalkThroughType{
    First,
    Second,
    Third,
    Fourth
}

const  val WalkThroughTypeKey = "WalkThroughType"

class CustomAdapter (fm : FragmentManager) : FragmentPagerAdapter(fm){

    override fun getCount(): Int = WalkThroughType.values().size

    override fun getItem(position: Int): Fragment {
        val fragment = WalkThroughFragment()
        fragment.arguments = Bundle().apply {
            putInt(WalkThroughTypeKey,
                WalkThroughType.values().mapNotNull { if (position == it.ordinal) it.ordinal else null}.first())
        }
        return fragment
    }
}