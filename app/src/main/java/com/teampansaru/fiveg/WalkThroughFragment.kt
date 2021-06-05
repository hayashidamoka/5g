package com.teampansaru.fiveg

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment

class WalkThroughFragment : Fragment() {
    var walkThroughType : WalkThroughType? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_walk_through, container, false)
    }

    /*
    * View の初期化。
    *
    * 表示するフラグメントの種類（WalkThroughType）に応じて View を変更している。
    * */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        arguments?.takeIf { it.containsKey(WalkThroughTypeKey) }?.apply {
            walkThroughType = WalkThroughType.values().mapNotNull {
                if (getInt(WalkThroughTypeKey) == it.ordinal) it else null}.first()
            initWalkThroughPage(view)
        }

    }

    private fun initWalkThroughPage(argView : View) {

        val linearLayout: FrameLayout = argView.findViewById(R.id.frame_layout)
        val imageView: ImageView = argView.findViewById(R.id.imageView)
        val description: TextView = argView.findViewById(R.id.description)
        val title: TextView = argView.findViewById(R.id.title)

        when (walkThroughType) {
            WalkThroughType.First -> {
                title.text = getText(R.string.first_fragment_title)
                linearLayout.setBackgroundResource(R.color.walk_through_1)
                imageView.setImageResource(R.drawable.phone_accept)
                description.text = getText(R.string.first_fragment_description)
            }
            WalkThroughType.Second -> {
                title.text = getText(R.string.second_fragment_title)
                linearLayout.setBackgroundResource(R.color.walk_through_1)
                imageView.setImageResource(R.drawable.add_widget)
                description.text = getText(R.string.second_fragment_description)
                description.textSize = 20F
            }
            WalkThroughType.Third -> {
                title.text = getText(R.string.third_fragment_title)
                linearLayout.setBackgroundResource(R.color.walk_through_1)
                imageView.setImageResource(R.drawable.wifi_off)
                description.text = getText(R.string.third_fragment_description)
            }
            WalkThroughType.Fourth -> {
                title.text = getText(R.string.fourth_fragment_title)
                linearLayout.setBackgroundResource(R.color.walk_through_1)
                imageView.setImageResource(R.drawable.five_g_dance)
                description.text = getText(R.string.fourth_fragment_description)
            }
            WalkThroughType.Fifth -> {
                title.text = getText(R.string.fifth_fragment_title)
                linearLayout.setBackgroundResource(R.color.walk_through_1)
                imageView.setImageResource(R.drawable.five_g_pose)
                description.text = getText(R.string.fifth_fragment_description)
                description.textSize = 20F

            }
        }
    }
}