package com.woocommerce.android.ui.products.categories

import com.woocommerce.android.ui.products.categories.AddProductCategoryViewModel.ProductCategoryItemUiModel

interface OnProductCategoryClickListener {
    fun onProductCategoryClick(productCategoryItemUiModel: ProductCategoryItemUiModel)
}
