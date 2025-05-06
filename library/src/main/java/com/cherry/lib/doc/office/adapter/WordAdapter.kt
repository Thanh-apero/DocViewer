package com.cherry.lib.doc.office.adapter

import com.cherry.lib.doc.office.wp.control.WPControl

class WordAdapter(private val word: WPControl) : BaseViewAdapter() {
    override val thumbnailLoader = DocViewThumbnailLoader(word)

    override fun changePage() {
        val oldPage = currentPage
        currentPage = word.currentViewIndex - 1
        notifyItemChanged(currentPage, PAYLOAD_PAGE_SELECTED)
        if (oldPage >= 0) {
            notifyItemChanged(oldPage, PAYLOAD_PAGE_SELECTED)
        }
        val oldTotalPages = totalPage
        totalPage = word.wordView.pageCount
        if (oldTotalPages != totalPage) {
            val listItem = List(totalPage) { ItemPageView(it + 1) }
            submitList(listItem)
        }
    }

    override fun itemOnClick(item: ItemPageView) {
        word.wordView.showPage(item.pageNumber - 1, -1)
    }
}