package com.woocommerce.android.ui.products.categories

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.products.BaseProductFragment
import com.woocommerce.android.ui.products.ProductDetailViewModel
import com.woocommerce.android.widgets.CustomProgressDialog
import kotlinx.android.synthetic.main.fragment_add_product_category.*

class AddProductCategoryFragment : BaseProductFragment() {
    private var progressDialog: CustomProgressDialog? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        setHasOptionsMenu(true)
        return inflater.inflate(R.layout.fragment_add_product_category, container, false)
    }

    override fun getFragmentTitle() = getString(R.string.product_add_category)

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_done, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_done -> {
                viewModel.addProductCategory(getCategoryName())
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers(viewModel)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        product_category_name.setOnTextChangedListener { viewModel.onCategoryNameChanged(it.toString()) }
    }

    override fun onRequestAllowBackPress() = true

    private fun setupObservers(viewModel: ProductDetailViewModel) {
        viewModel.addProductCategoryViewStateData.observe(viewLifecycleOwner) { old, new ->
            new.categoryNameErrorMessage?.takeIfNotEqualTo(old?.categoryNameErrorMessage) {
                displayCategoryNameError(it)
            }
            new.displayProgressDialog?.takeIfNotEqualTo(old?.displayProgressDialog) { showProgressDialog(it) }
        }
    }

    private fun displayCategoryNameError(messageId: Int) {
        if (messageId != 0) {
            product_category_name.error = getString(messageId)
            showUpdateMenuItem(false)
        } else {
            product_category_name.clearError()
            showUpdateMenuItem(true)
        }
    }

    private fun showProgressDialog(show: Boolean) {
        if (show) {
            hideProgressDialog()
            progressDialog = CustomProgressDialog.show(
                getString(R.string.product_update_dialog_title),
                getString(R.string.product_update_dialog_message)
            ).also { it.show(parentFragmentManager, CustomProgressDialog.TAG) }
            progressDialog?.isCancelable = false
        } else {
            hideProgressDialog()
        }
    }

    private fun hideProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    private fun getCategoryName() = product_category_name.getText()
}
