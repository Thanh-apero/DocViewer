package com.cherry.lib.doc.office.adapter

import android.util.Log
import com.cherry.lib.doc.office.constant.EventConstant
import com.cherry.lib.doc.office.pg.control.PGControl
import kotlinx.coroutines.CoroutineScope

class PPTAdapter(private val ppt: PGControl) : BaseViewAdapter() {
    override val thumbnailLoader = DocViewThumbnailLoader(ppt)

    override fun changePage() {
        val oldPage = currentPage
        currentPage = ppt.currentViewIndex - 1
        notifyItemChanged(currentPage, PAYLOAD_PAGE_SELECTED)
        if (oldPage >= 0) {
            notifyItemChanged(oldPage, PAYLOAD_PAGE_SELECTED)
        }
        val oldTotalPages = totalPage
        totalPage = (ppt.getActionValue(EventConstant.APP_COUNT_PAGES_ID, null) as? Int) ?: 0
        if (oldTotalPages != totalPage) {
            Log.d("PPTAdapter", "totalPage: $totalPage")
            val listItem = List(totalPage) { ItemPageView(it + 1) }
            submitList(listItem)
        }
    }

    override fun itemOnClick(item: ItemPageView) {
        ppt.pgView.showSlide(item.pageNumber - 1, false)
    }

}