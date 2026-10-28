package com.b866f.launcher

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.b866f.launcher.databinding.ItemFavoriteBinding

class FavoriteAdapter(
    private val apps: List<AppInfo>,
    private var accentColor: Int,
    private val onClick: (AppInfo) -> Unit
) : RecyclerView.Adapter<FavoriteAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemFavoriteBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemFavoriteBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val app = apps[position]
        holder.binding.appIcon.setImageDrawable(app.icon)
        holder.binding.appName.text = app.appName

        val normal = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 18f
            setColor(Color.parseColor("#1A2E52"))
            setStroke(2, accentColor)
        }

        val focused = GradientDrawable().apply {
            shape = GradientDrawable.RECTANGLE
            cornerRadius = 18f
            setColor(Color.parseColor("#0E1A2C"))
            setStroke(3, accentColor)
        }

        holder.binding.root.background = normal
        holder.binding.root.setOnFocusChangeListener { _, hasFocus ->
            holder.binding.root.background = if (hasFocus) focused else normal
            holder.binding.root.scaleX = if (hasFocus) 1.04f else 1f
            holder.binding.root.scaleY = if (hasFocus) 1.04f else 1f
        }

        holder.binding.root.setOnClickListener {
            onClick(app)
        }
    }

    override fun getItemCount(): Int = apps.size

    fun updateAccent(newAccent: Int) {
        accentColor = newAccent
        notifyDataSetChanged()
    }
}
