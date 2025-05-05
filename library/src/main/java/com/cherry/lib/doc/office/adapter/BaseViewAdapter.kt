package com.cherry.lib.doc.office.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.cherry.lib.doc.R
import com.cherry.lib.doc.databinding.ItemPageViewBinding
import kotlinx.coroutines.CoroutineScope

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

    open var currentPage: Int = -1
    open val thumbnailLoader: DocViewThumbnailLoader? = null
    abstract fun setupAdapter(scope: CoroutineScope)
    private val loadingItems = HashSet<Int>()
    private var itemOnClick: (ItemPageView) -> Unit = {}
    fun setItemOnClickListener(listener: (ItemPageView) -> Unit) {
        itemOnClick = listener
    }

    class PageViewHolder(val binding: ItemPageViewBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val binding = ItemPageViewBinding.bind(
            LayoutInflater.from(parent.context).inflate(R.layout.item_page_view, parent, false)
        )
        return PageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        val item = getItem(position)
        holder.binding.pageNumber.text = item.pageNumber.toString()
        holder.binding.pageView.setOnClickListener {
            itemOnClick.invoke(item)
        }
        holder.binding.root.strokeColor = ContextCompat.getColor(
            holder.itemView.context,
            if (currentPage == position) R.color.background_thumbnail_selected else R.color.background_thumbnail_unselect
        )
        loadThumbnailForPosition(holder, position)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int, payloads: List<Any?>) {
        if (payloads.contains(PAYLOAD_PAGE_SELECTED)) {
            holder.binding.root.strokeColor = ContextCompat.getColor(
                holder.itemView.context,
                if (currentPage == position) R.color.background_thumbnail_selected else R.color.background_thumbnail_unselect
            )
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

        holder.binding.pageView.setImageBitmap(null)
        holder.binding.pageView.setBackgroundColor(0xFFEEEEEE.toInt())
        holder.binding.loadingIndicator.visibility = ViewGroup.VISIBLE
        thumbnailLoader?.loadThumbnail(pageNumber)?.let { bitmap ->
            if (holder.adapterPosition == position) {
                holder.binding.pageView.setBackgroundColor(0x00000000)
                holder.binding.pageView.setImageBitmap(bitmap)
                holder.binding.loadingIndicator.visibility = ViewGroup.GONE
            }

            loadingItems.remove(pageNumber)
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        thumbnailLoader?.clearCache()
    }
}