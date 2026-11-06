package com.example.catalog.ui.details

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import coil.load
import com.example.catalog.R
import com.example.catalog.databinding.FragmentProductDetailsBinding
import com.example.catalog.ui.Formatters
import com.example.catalog.ui.ViewModelFactories
import kotlinx.coroutines.launch

class ProductDetailsFragment : Fragment() {

    private var _binding: FragmentProductDetailsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProductDetailsViewModel by viewModels {
        ViewModelFactories.productDetails(requireArguments().getInt(ARG_PRODUCT_ID))
    }

    private var shownImages: List<String>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProductDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.toolbar.setNavigationOnClickListener { findNavController().navigateUp() }
        binding.retryDetails.setOnClickListener { viewModel.loadDetails() }
        shownImages = null

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: ProductDetailsUiState) {
        val product = state.product
        if (product != null) {
            binding.toolbar.title = product.title
            binding.title.text = product.title
            binding.category.text = product.category
            binding.price.text = Formatters.price(product.price)
            binding.discount.isVisible = product.discountPercentage > 0
            binding.discount.text = getString(R.string.discount_format, product.discountPercentage)
            binding.rating.text = getString(R.string.rating_format, product.rating)
            if (shownImages == null) {
                binding.image.load(product.thumbnail) {
                    crossfade(true)
                    placeholder(R.drawable.ic_image_placeholder)
                    error(R.drawable.ic_image_placeholder)
                }
            }
        }

        val load = state.details
        binding.detailsProgress.isVisible = load is DetailsLoad.Loading
        binding.offlineInfo.isVisible = load is DetailsLoad.Unavailable
        binding.retryDetails.isVisible = load is DetailsLoad.Unavailable
        binding.detailsGroup.isVisible = load is DetailsLoad.Loaded

        if (load is DetailsLoad.Loaded) {
            val details = load.details
            binding.description.text = details.description.orEmpty()
            binding.brand.text = getString(R.string.brand_format, details.brand ?: getString(R.string.no_brand))
            binding.stock.text = getString(
                R.string.stock_format,
                details.stock ?: 0,
                details.availabilityStatus.orEmpty(),
            )
            binding.tags.text = details.tags.joinToString(" · ") { "#$it" }
            binding.tags.isVisible = details.tags.isNotEmpty()
            renderGallery(details.images)
        }
    }

    private fun renderGallery(images: List<String>) {
        if (images == shownImages) return
        shownImages = images
        images.firstOrNull()?.let { first ->
            binding.image.load(first) {
                crossfade(true)
                placeholder(R.drawable.ic_image_placeholder)
                error(R.drawable.ic_image_placeholder)
            }
        }
        val gallery = binding.gallery
        gallery.removeAllViews()
        val size = resources.getDimensionPixelSize(R.dimen.gallery_thumb)
        val margin = resources.getDimensionPixelSize(R.dimen.space_small)
        images.forEach { url ->
            val imageView = ImageView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(size, size).apply { marginEnd = margin }
                scaleType = ImageView.ScaleType.CENTER_CROP
                setBackgroundResource(R.drawable.bg_thumb)
                load(url) { placeholder(R.drawable.ic_image_placeholder) }
                setOnClickListener { binding.image.load(url) { crossfade(true) } }
            }
            gallery.addView(imageView)
        }
        binding.galleryScroll.isVisible = images.size > 1
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_PRODUCT_ID = "productId"
    }
}
