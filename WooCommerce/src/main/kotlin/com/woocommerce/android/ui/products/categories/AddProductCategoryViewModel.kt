package com.woocommerce.android.ui.products.categories

import android.os.Parcelable
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.R.string
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.launch

class AddProductCategoryViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val productCategoriesRepository: ProductCategoriesRepository,
    private val networkStatus: NetworkStatus
) : ScopedViewModel(savedState, dispatchers) {
    // view state for the add category screen
    val addProductCategoryViewStateData = LiveDataDelegate(savedState, AddProductCategoryViewState())
    private var addProductCategoryViewState by addProductCategoryViewStateData

    override fun onCleared() {
        super.onCleared()
        productCategoriesRepository.onCleanup()
    }

    fun onCategoryNameChanged(categoryName: String) {
        addProductCategoryViewState = if (categoryName.isEmpty()) {
            addProductCategoryViewState.copy(categoryNameErrorMessage = string.add_product_category_empty)
        } else {
            addProductCategoryViewState.copy(categoryNameErrorMessage = 0)
        }
    }

    fun addProductCategory(categoryName: String, parentId: Long = 0L) {
        addProductCategoryViewState = addProductCategoryViewState.copy(displayProgressDialog = true)
        launch {
            if (networkStatus.isConnected()) {
                when (productCategoriesRepository.addProductCategory(categoryName, parentId)) {
                    RequestResult.SUCCESS -> {
                        triggerEvent(ShowSnackbar(string.add_product_category_success))
                        triggerEvent(Exit)
                    }
                    RequestResult.API_ERROR -> {
                        addProductCategoryViewState = addProductCategoryViewState.copy(
                            categoryNameErrorMessage = string.add_product_category_duplicate
                        )
                    }
                    RequestResult.ERROR -> {
                        triggerEvent(ShowSnackbar(string.add_product_category_failed))
                    }
                    else -> { /** No action needed */ }
                }
            } else {
                triggerEvent(ShowSnackbar(string.offline_error))
            }

            // hide progress dialog
            addProductCategoryViewState = addProductCategoryViewState.copy(displayProgressDialog = false)
        }
    }

    @Parcelize
    data class AddProductCategoryViewState(
        val displayProgressDialog: Boolean? = null,
        val categoryNameErrorMessage: Int? = null
    ) : Parcelable

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<AddProductCategoryViewModel>
}
