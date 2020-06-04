package com.woocommerce.android.ui.products.categories

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.ui.products.OnLoadMoreListener
import com.woocommerce.android.ui.products.categories.AddProductCategoryViewModel.ProductCategoryItemUiModel
import com.woocommerce.android.ui.products.categories.ParentCategoryListAdapter.ParentCategoryListViewHolder
import kotlinx.android.synthetic.main.parent_category_list_item.view.*
import org.wordpress.android.util.HtmlUtils

class ParentCategoryListAdapter(
    private val context: Context,
    private val loadMoreListener: OnLoadMoreListener,
    private val clickListener: OnProductCategoryClickListener
) : RecyclerView.Adapter<ParentCategoryListViewHolder>() {
    private val parentCategoryList = ArrayList<ProductCategoryItemUiModel>()

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = parentCategoryList[position].category.remoteCategoryId

    override fun getItemCount() = parentCategoryList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParentCategoryListViewHolder {
        return ParentCategoryListViewHolder(LayoutInflater.from(context)
            .inflate(R.layout.parent_category_list_item, parent, false))
    }

    override fun onBindViewHolder(holder: ParentCategoryListViewHolder, position: Int) {
        val parentCategory = parentCategoryList[position]

        holder.apply {
            txtCategoryName.text = if (parentCategory.category.name.isEmpty()) {
                context.getString(R.string.untitled)
            } else {
                HtmlUtils.fastStripHtml(parentCategory.category.name)
            }

            val newLayoutParams = txtCategoryName.layoutParams as LayoutParams
            newLayoutParams.marginStart = parentCategory.margin
            txtCategoryName.layoutParams = newLayoutParams

            radioButton.isChecked = parentCategory.isSelected

            radioButton.setOnClickListener {
                handleCategoryClick(this, parentCategory)
            }

            itemView.setOnClickListener {
                radioButton.isChecked = !radioButton.isChecked
                handleCategoryClick(this, parentCategory)
            }
        }

        if (position == itemCount - 1) {
            loadMoreListener.onRequestLoadMore()
        }
    }

    private fun handleCategoryClick(
        holder: ParentCategoryListViewHolder,
        parentCategory: ProductCategoryItemUiModel
    ) {
        parentCategory.isSelected = holder.radioButton.isChecked
        clickListener.onProductCategoryClick(parentCategory)
    }

    fun setParentCategories(productsCategories: List<ProductCategoryItemUiModel>) {
        if (parentCategoryList.isEmpty()) {
            parentCategoryList.addAll(productsCategories)
            notifyDataSetChanged()
        } else {
            val diffResult =
                DiffUtil.calculateDiff(ParentCategoryItemDiffUtil(parentCategoryList, productsCategories))
            parentCategoryList.clear()
            parentCategoryList.addAll(productsCategories)
            diffResult.dispatchUpdatesTo(this)
        }
    }

    class ParentCategoryListViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtCategoryName: TextView = view.parentCategoryName
        val radioButton: RadioButton = view.parentCategorySelected
    }

    private class ParentCategoryItemDiffUtil(
        val oldList: List<ProductCategoryItemUiModel>,
        val newList: List<ProductCategoryItemUiModel>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            oldList[oldItemPosition].category.remoteCategoryId == newList[newItemPosition].category.remoteCategoryId

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            return oldItem.category.isSameCategory(newItem.category)
        }
    }
}
