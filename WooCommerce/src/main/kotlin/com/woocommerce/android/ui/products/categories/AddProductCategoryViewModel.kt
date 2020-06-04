package com.woocommerce.android.ui.products.categories

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.R.string
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.ProductCategory
import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.model.sortCategories
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
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
    private val arguments: ParentCategoryListFragmentArgs by savedState.navArgs()

    // view state for the add category screen
    val addProductCategoryViewStateData = LiveDataDelegate(savedState, AddProductCategoryViewState())
    private var addProductCategoryViewState by addProductCategoryViewStateData

    // view state for the parent category list screen
    val parentCategoryListViewStateData = LiveDataDelegate(savedState, ParentCategoryListViewState())
    private var parentCategoryListViewState by parentCategoryListViewStateData

    private val _parentCategories = MutableLiveData<List<ProductCategoryItemUiModel>>()
    val parentCategories: LiveData<List<ProductCategoryItemUiModel>> = _parentCategories

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

    fun addProductCategory(categoryName: String, parentId: Long = getSelectedParentId()) {
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

    fun fetchParentCategories() {
        if (_parentCategories.value == null) {
            loadParentCategories()
        }
    }

    fun onParentCategorySelected(parentId: Long) {
        addProductCategoryViewState = addProductCategoryViewState.copy(selectedParentId = parentId)
        triggerEvent(Exit)
    }

    fun getSelectedParentId() = addProductCategoryViewState.selectedParentId ?: 0L

    fun getSelectedParentCategoryName(): String? {
        val selectedRemoteId = getSelectedParentId()
        return if (selectedRemoteId != 0L) {
            productCategoriesRepository.getProductCategoryByRemoteId(selectedRemoteId)?.name ?: ""
        } else ""
    }

    /**
     * Refreshes the list of categories by calling the [loadParentCategories] method
     * which eventually checks, if there is anything new to fetch from the server
     *
     */
    fun refreshParentCategories() {
        parentCategoryListViewState = parentCategoryListViewState.copy(isRefreshing = true)
        loadParentCategories()
    }

    /**
     * Loads the list of categories from the database or from the server.
     * This depends on whether categories are stored in the database, and if any new ones are
     * required to be fetched.
     *
     * @param loadMore Whether to load more categories after the ones loaded
     */
    private fun loadParentCategories(loadMore: Boolean = false) {
        if (parentCategoryListViewState.isLoading == true) {
            WooLog.d(WooLog.T.PRODUCTS, "already loading parent categories")
            return
        }

        if (loadMore && !productCategoriesRepository.canLoadMoreProductCategories) {
            WooLog.d(WooLog.T.PRODUCTS, "can't load more parent categories")
            return
        }

        launch {
            val showSkeleton: Boolean
            if (loadMore) {
                showSkeleton = false
            } else {
                // if this is the initial load, first get the categories from the db and show them immediately
                val productsInDb = productCategoriesRepository.getProductCategoriesList()
                if (productsInDb.isEmpty()) {
                    showSkeleton = true
                } else {
                    _parentCategories.value = sortAndStyleParentCategories(
                        arguments.selectedParentId, productsInDb
                    )
                    showSkeleton = parentCategoryListViewState.isRefreshing == false
                }
            }
            parentCategoryListViewState = parentCategoryListViewState.copy(
                isLoading = true,
                isLoadingMore = loadMore,
                isSkeletonShown = showSkeleton,
                isEmptyViewVisible = false
            )
            fetchParentCategories(loadMore = loadMore)
        }
    }

    /**
     * Triggered when the user scrolls past the point of loaded categories
     * already displayed on the screen or on record.
     */
    fun onLoadMoreParentCategoriesRequested() {
        loadParentCategories(loadMore = true)
    }

    /**
     * This method is used to fetch the categories from the backend. It does not
     * check the database.
     *
     * @param loadMore Whether this is another page or the first one
     */
    private suspend fun fetchParentCategories(loadMore: Boolean = false) {
        if (networkStatus.isConnected()) {
            _parentCategories.value = sortAndStyleParentCategories(
                arguments.selectedParentId,
                productCategoriesRepository.fetchProductCategories(loadMore = loadMore)
            )

            parentCategoryListViewState = parentCategoryListViewState.copy(
                isLoading = true,
                canLoadMore = productCategoriesRepository.canLoadMoreProductCategories,
                isEmptyViewVisible = _parentCategories.value?.isEmpty() == true)
        } else {
            triggerEvent(ShowSnackbar(string.offline_error))
        }

        parentCategoryListViewState = parentCategoryListViewState.copy(
            isSkeletonShown = false,
            isLoading = false,
            isLoadingMore = false,
            isRefreshing = false
        )
    }

    /**
     * The method takes in a list of product categories and calculates the order and grouping of categories
     * by their parent ids. This creates a stable sorted list of categories by name. The returned list also
     * has margin data, which can be used to visually represent categories in a hierarchy under their
     * parent ids.
     *
     * @param parentCategories the list of parent categories to sort and style
     * @return [List<ProductCategoryItemUiModel>] the sorted styled list of categories
     */
    private fun sortAndStyleParentCategories(
        selectedParentId: Long,
        parentCategories: List<ProductCategory>
    ): List<ProductCategoryItemUiModel> {
        // Sort all incoming categories by their parent
        val sortedList = parentCategories.sortCategories()

        // Mark the parent category as selected in the sorted list
        for (productCategoryItemUiModel in sortedList) {
            if (productCategoryItemUiModel.category.remoteCategoryId == selectedParentId) {
                productCategoryItemUiModel.isSelected = true
            }
        }
        return sortedList.toList()
    }

    data class ProductCategoryItemUiModel(
        val category: ProductCategory,
        var margin: Int = ProductCategory.DEFAULT_PRODUCT_CATEGORY_MARGIN,
        var isSelected: Boolean = false
    )

    @Parcelize
    data class AddProductCategoryViewState(
        val displayProgressDialog: Boolean? = null,
        val categoryNameErrorMessage: Int? = null,
        val selectedParentId: Long? = null
    ) : Parcelable

    @Parcelize
    data class ParentCategoryListViewState(
        val isSkeletonShown: Boolean? = null,
        val isLoading: Boolean? = null,
        val isLoadingMore: Boolean? = null,
        val canLoadMore: Boolean? = null,
        val isRefreshing: Boolean? = null,
        val isEmptyViewVisible: Boolean? = null
    ) : Parcelable

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<AddProductCategoryViewModel>
}
