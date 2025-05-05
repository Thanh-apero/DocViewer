package com.cherry.lib.doc.office.adapter

import com.cherry.lib.doc.office.pg.control.PGControl
import kotlinx.coroutines.CoroutineScope

class PPTAdapter(ppt: PGControl): BaseViewAdapter() {
    override var currentPage = -1
    override val thumbnailLoader = DocViewThumbnailLoader(ppt)
    override fun setupAdapter(scope: CoroutineScope) {

    }

    init {

    }
}