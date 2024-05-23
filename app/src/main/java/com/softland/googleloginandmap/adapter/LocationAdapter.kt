package com.softland.googleloginandmap.adapter

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.softland.googleloginandmap.ui.GoogleMap
import com.softland.googleloginandmap.R
import com.softland.googleloginandmap.data.LocationData



class LocationAdapter(
    private val context: Context,
    private var dataList: List<LocationData>,
    private val activity: Activity
) :
    RecyclerView.Adapter<LocationAdapter.MyViewHolder>() {
    var DURATION: Long = 500
    private var on_attach = true

    // ViewHolder class
    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView =
            itemView.findViewById(R.id.textView1) // Assuming you have a TextView in your item layout with id 'textView'
    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.item_layout,
            parent,
            false
        ) // Assuming you have an item layout with the name 'item_layout'
        return MyViewHolder(itemView)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val item = "${dataList[position].latitude}    ${dataList[position]?.longitude}"
        holder.textView.text = item
        holder.itemView.setOnClickListener {
            val intent = Intent(context, GoogleMap::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            activity.startActivity(intent)
            activity.finish()
        }
        setAnimation(holder.itemView, position)
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount(): Int {
        return dataList.size
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                Log.d(TAG, "onScrollStateChanged: Called $newState")
                on_attach = false
                super.onScrollStateChanged(recyclerView, newState)
            }
        })
        super.onAttachedToRecyclerView(recyclerView)
    }

    @SuppressLint("Recycle")
    private fun setAnimation(itemView: View, i: Int) {
        var i = i
        if (!on_attach) {
            i = -1
        }
        val isNotFirstItem = i == -1
        i++
        itemView.alpha = 0f

        val animatorSet = AnimatorSet()

        // Change animation property to scale and alpha
        val scaleXAnimator = ObjectAnimator.ofFloat(itemView, "scaleX", 0.5f, 1.0f)
        val scaleYAnimator = ObjectAnimator.ofFloat(itemView, "scaleY", 0.5f, 1.0f)
        val alphaAnimator = ObjectAnimator.ofFloat(itemView, "alpha", 0f, 1.0f)

        // Add interpolators for smoother animation
        scaleXAnimator.interpolator = DecelerateInterpolator()
        scaleYAnimator.interpolator = DecelerateInterpolator()
        alphaAnimator.interpolator = AccelerateDecelerateInterpolator()

        // Add delay based on item position
        val delay = if (isNotFirstItem) DURATION / 2 else i * DURATION / 3

        // Set duration for each animator
        scaleXAnimator.duration = 500
        scaleYAnimator.duration = 500
        alphaAnimator.duration = 500

        // Add animators to AnimatorSet
        animatorSet.playTogether(scaleXAnimator, scaleYAnimator, alphaAnimator)
        animatorSet.startDelay = delay

        // Start the animation
        animatorSet.start()
    }
}