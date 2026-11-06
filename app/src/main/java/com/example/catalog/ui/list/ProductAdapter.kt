package com.example.catalog.ui.list

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.example.catalog.R
import com.example.catalog.core.Product
import com.example.catalog.databinding.ItemProductBinding
import com.example.catalog.ui.Formatters

class ProductAdapter(
    private val onClick: (Product) -> Unit,
) : ListAdapter<Product, ProductAdapter.ViewHolder>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(getItem(position))

    inner class ViewHolder(private val binding: ItemProductBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product) {
            val context = binding.root.context
            binding.title.text = product.title
            binding.price.text = Formatters.price(product.price)
            binding.discount.isVisible = product.discountPercentage > 0
            binding.discount.text = context.getString(R.string.discount_format, product.discountPercentage)
            binding.rating.text = context.getString(R.string.rating_format, product.rating)
            binding.thumbnail.load(product.thumbnail) {
                crossfade(true)
                placeholder(R.drawable.ic_image_placeholder)
                error(R.drawable.ic_image_placeholder)
            }
            binding.root.setOnClickListener { onClick(product) }
        }
    }

    private companion object {
        val DIFF = object : DiffUtil.ItemCallback<Product>() {
            override fun areItemsTheSame(oldItem: Product, newItem: Product) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Product, newItem: Product) = oldItem == newItem
        }
    }
}
