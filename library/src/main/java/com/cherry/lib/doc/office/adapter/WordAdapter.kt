package com.cherry.lib.doc.office.adapter

import com.cherry.lib.doc.office.wp.control.WPControl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class WordAdapter(private val word: WPControl): BaseViewAdapter() {
    override var currentPage = -1
    override val thumbnailLoader = DocViewThumbnailLoader(word)

    init {
        word.wordView.setPageListener { pageNumber ->
            (pageNumber - 1).let {
                if (currentPage != it) {
                    val oldPage = currentPage
                    currentPage = it
                    notifyItemChanged(it, PAYLOAD_PAGE_SELECTED)
                    if (oldPage >= 0) {
                        notifyItemChanged(oldPage, PAYLOAD_PAGE_SELECTED)
                    }
                }
            }
        }
    }

    override fun setupAdapter(scope: CoroutineScope) {
        scope.launch(Dispatchers.Main) {
            var isFinished = false
            while (!isFinished) {
                try {
                    isFinished = word.wordView.isFinishLayout
                } catch (e: Exception) {
                    isFinished = false
                }
                val totalPages = word.wordView.pageCount
                val listItem = List(totalPages) { ItemPageView(it + 1) }
                submitList(listItem)
                delay(500)
            }
        }
    }
}