package com.cherry.lib.doc.office.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cherry.lib.doc.R
import com.cherry.lib.doc.databinding.ItemPageViewBinding

abstract class BaseViewAdapter() :
    ListAdapter<ItemPageView, BaseViewAdapter.PageViewHolder>(DIFF) {
    companion object {
        const val PAYLOAD_PAGE_SELECTED = "PAYLOAD_PAGE_SELECTED"
        private val DIFF = object : DiffUtil.ItemCallback<ItemPageView>() {
            override fun areItemsTheSame(oldItem: ItemPageView, newItem: ItemPageView): Boolean {
                return oldItem.pageNumber == newItem.pageNumber
            }

            override fun areContentsTheSame(oldItem: ItemPageView, newItem: ItemPageView): Boolean {
                return oldItem == newItem
            }
        }
    }

    open var totalPage: Int = 0
    open var currentPage: Int = -1
        set(value) {
            val oldValue = field
            field = value
            if (oldValue != value) {
                notifyItemChanged(oldValue, PAYLOAD_PAGE_SELECTED)
                notifyItemChanged(value, PAYLOAD_PAGE_SELECTED)
                scrollToCurrentPage()
            }
        }
    open val thumbnailLoader: DocViewThumbnailLoader? = null
    abstract fun changePage()
    private val loadingItems = HashSet<Int>()
    abstract fun itemOnClick(item: ItemPageView)
    private var recyclerView: RecyclerView? = null

    class PageViewHolder(val binding: ItemPageViewBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val binding = ItemPageViewBinding.bind(
            LayoutInflater.from(parent.context).inflate(R.layout.item_page_view, parent, false)
        )
        return PageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        Log.d("DocViewThumbnailLoader", "onBindViewHolder: $position")
        val item = getItem(position)
        with(holder.binding) {
            pageNumber.text = item.pageNumber.toString()
            pageView.setOnClickListener {
                itemOnClick(item)
            }
            root.strokeColor = ContextCompat.getColor(
                holder.itemView.context,
                if (currentPage == position) R.color.background_thumbnail_selected else R.color.background_thumbnail_unselect
            )
            pageNumber.backgroundTintList = ContextCompat.getColorStateList(
                holder.itemView.context,
                if (currentPage == position) R.color.background_thumbnail_selected else R.color.background_thumbnail_unselect
            )
        }
        loadThumbnailForPosition(holder, position)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int, payloads: List<Any?>) {
        if (payloads.contains(PAYLOAD_PAGE_SELECTED)) {
            with(holder.binding) {
                root.strokeColor = ContextCompat.getColor(
                    holder.itemView.context,
                    if (currentPage == position) R.color.background_thumbnail_selected else R.color.background_thumbnail_unselect
                )
                pageNumber.backgroundTintList = ContextCompat.getColorStateList(
                    holder.itemView.context,
                    if (currentPage == position) R.color.background_thumbnail_selected else R.color.background_thumbnail_unselect
                )
            }
        } else {
            onBindViewHolder(holder, position)
        }
    }

    override fun onViewAttachedToWindow(holder: PageViewHolder) {
        super.onViewAttachedToWindow(holder)
        val position = holder.adapterPosition
        if (position != RecyclerView.NO_POSITION) {
            loadThumbnailForPosition(holder, position)
        }
    }

    private fun loadThumbnailForPosition(holder: PageViewHolder, position: Int) {
        val item = getItem(position)
        val pageNumber = item.pageNumber

        if (loadingItems.contains(pageNumber)) {
            return
        }

        loadingItems.add(pageNumber)
        with(holder.binding) {
            pageView.setImageBitmap(null)
            pageView.setBackgroundColor(0xFFEEEEEE.toInt())
            loadingIndicator.visibility = ViewGroup.VISIBLE
            thumbnailLoader?.loadThumbnail(pageNumber)?.let { bitmap ->
                if (holder.adapterPosition == position) {
                    pageView.setBackgroundColor(0x00000000)
                    pageView.setImageBitmap(bitmap)
                    loadingIndicator.visibility = ViewGroup.GONE
                }

                loadingItems.remove(pageNumber)
            }
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        thumbnailLoader?.clearCache()
        this.recyclerView = null
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }

    private fun scrollToCurrentPage() {
        if (currentPage >= 0) {
            recyclerView?.scrollToPosition(currentPage)
        }
    }
}