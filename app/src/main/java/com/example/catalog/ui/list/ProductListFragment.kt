package com.example.catalog.ui.list

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.catalog.BuildConfig
import com.example.catalog.CatalogApp
import com.example.catalog.R
import com.example.catalog.core.Category
import com.example.catalog.core.SyncStatus
import com.example.catalog.core.UiState
import com.example.catalog.data.ApiMode
import com.example.catalog.data.ChaosMode
import com.example.catalog.data.DebugSettings
import com.example.catalog.databinding.FragmentProductListBinding
import com.example.catalog.ui.Formatters
import com.example.catalog.ui.ViewModelFactories
import com.example.catalog.ui.details.ProductDetailsFragment
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class ProductListFragment : Fragment() {

    private var _binding: FragmentProductListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProductListViewModel by viewModels { ViewModelFactories.productList }

    private val debugSettings: DebugSettings
        get() = (requireActivity().application as CatalogApp).container.debugSettings

    private lateinit var adapter: ProductAdapter
    private var lastStatus: SyncStatus? = null
    private var shownCategories: List<Category>? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProductListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupToolbar()

        adapter = ProductAdapter { product ->
            findNavController().navigate(
                R.id.action_list_to_details,
                bundleOf(ProductDetailsFragment.ARG_PRODUCT_ID to product.id),
            )
        }
        binding.recycler.adapter = adapter

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.onSyncClicked()
            // Postęp pokazuje pasek statusu; kółko chowamy od razu
            binding.swipeRefresh.isRefreshing = false
        }

        lastStatus = null
        shownCategories = null

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.uiState.collect(::renderList) }
                launch { viewModel.categories.collect(::renderCategories) }
                launch { viewModel.syncStatus.collect(::renderStatus) }
            }
        }
    }

    private fun setupToolbar() {
        val toolbar = binding.toolbar
        toolbar.inflateMenu(R.menu.menu_product_list)
        toolbar.menu.setGroupVisible(R.id.group_debug, BuildConfig.DEBUG)
        if (BuildConfig.DEBUG) syncDebugChecks()
        toolbar.setOnMenuItemClickListener(::onMenuItemClicked)
    }

    private fun onMenuItemClicked(item: MenuItem): Boolean {
        val settings = debugSettings
        when (item.itemId) {
            R.id.action_sync -> viewModel.onSyncClicked()
            R.id.api_remote -> settings.apiMode = ApiMode.REMOTE
            R.id.api_assets -> settings.apiMode = ApiMode.OFFLINE_ASSETS
            R.id.chaos_none -> settings.chaosMode = ChaosMode.NONE
            R.id.chaos_503 -> settings.chaosMode = ChaosMode.HTTP_503
            R.id.chaos_timeout -> settings.chaosMode = ChaosMode.TIMEOUT
            R.id.chaos_no_network -> settings.chaosMode = ChaosMode.NO_NETWORK
            else -> return false
        }
        syncDebugChecks()
        return true
    }

    /** Zaznacza w menu bieżące ustawienia debug. */
    private fun syncDebugChecks() {
        val menu = binding.toolbar.menu
        val settings = debugSettings
        val apiId = when (settings.apiMode) {
            ApiMode.REMOTE -> R.id.api_remote
            ApiMode.OFFLINE_ASSETS -> R.id.api_assets
        }
        val chaosId = when (settings.chaosMode) {
            ChaosMode.NONE -> R.id.chaos_none
            ChaosMode.HTTP_503 -> R.id.chaos_503
            ChaosMode.TIMEOUT -> R.id.chaos_timeout
            ChaosMode.NO_NETWORK -> R.id.chaos_no_network
        }
        menu.findItem(apiId)?.isChecked = true
        menu.findItem(chaosId)?.isChecked = true
    }

    private fun renderList(state: UiState<List<com.example.catalog.core.Product>>) {
        binding.loading.isVisible = state is UiState.Loading
        binding.emptyText.isVisible = state is UiState.Empty || state is UiState.Error
        binding.emptyText.text = when (state) {
            is UiState.Error -> state.message
            else -> getString(R.string.empty_list)
        }
        adapter.submitList((state as? UiState.Success)?.data ?: emptyList())
    }

    private fun renderCategories(categories: List<Category>) {
        if (categories == shownCategories) return
        shownCategories = categories
        val group = binding.chipGroup
        group.setOnCheckedStateChangeListener(null)
        group.removeAllViews()

        val selected = viewModel.selectedCategory.value
        group.addView(createChip(getString(R.string.chip_all), slug = null, checked = selected == null))
        categories.forEach { category ->
            group.addView(createChip(category.name, category.slug, checked = selected == category.slug))
        }
        group.setOnCheckedStateChangeListener { chipGroup, checkedIds ->
            val chip = checkedIds.firstOrNull()?.let { chipGroup.findViewById<Chip>(it) }
            viewModel.onCategorySelected(chip?.tag as String?)
        }
    }

    private fun createChip(text: String, slug: String?, checked: Boolean): Chip =
        Chip(requireContext()).apply {
            id = View.generateViewId()
            this.text = text
            tag = slug
            isCheckable = true
            isChecked = checked
        }

    private fun renderStatus(status: SyncStatus) {
        binding.statusText.text = Formatters.syncStatus(requireContext(), status)
        binding.statusProgress.isVisible = status is SyncStatus.Running ||
            status is SyncStatus.WaitingForNetwork ||
            status is SyncStatus.Retrying
        binding.statusIcon.setImageResource(
            when (status) {
                is SyncStatus.Idle -> R.drawable.ic_schedule
                SyncStatus.WaitingForNetwork -> R.drawable.ic_cloud_off
                SyncStatus.Running -> R.drawable.ic_sync
                is SyncStatus.Retrying -> R.drawable.ic_sync
                is SyncStatus.Succeeded -> R.drawable.ic_check
                is SyncStatus.Failed -> R.drawable.ic_error
            },
        )
        // Snackbar tylko przy przejściu w stan błędu (nie przy każdej emisji)
        if (status is SyncStatus.Failed && lastStatus !is SyncStatus.Failed) {
            Snackbar.make(binding.root, Formatters.syncStatus(requireContext(), status), Snackbar.LENGTH_LONG)
                .setAction(R.string.retry) { viewModel.onSyncClicked() }
                .show()
        }
        lastStatus = status
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
