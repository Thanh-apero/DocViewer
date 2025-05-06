package com.cherry.lib.doc.office.adapter

import android.graphics.Bitmap
import android.util.Log
import android.util.LruCache
import com.cherry.lib.doc.office.constant.EventConstant
import com.cherry.lib.doc.office.pg.control.PGControl
import com.cherry.lib.doc.office.system.IControl
import com.cherry.lib.doc.office.wp.control.WPControl

class DocViewThumbnailLoader(private val iControl: IControl) {
    companion object {
        private const val TAG = "DocViewThumbnailLoader"
        private const val CACHE_SIZE = 32
    }

    private fun getBitmap(pageIndex: Int): Bitmap? {
        return when (iControl) {
            is WPControl -> iControl.wordView.pageToImage(pageIndex)
            is PGControl -> iControl.getActionValue(EventConstant.PG_SLIDE_TO_IMAGE, pageIndex) as Bitmap?
            else -> null
        }
    }

    private val thumbnailCache = LruCache<Int, Bitmap>(CACHE_SIZE)

    fun loadThumbnail(pageIndex: Int): Bitmap? {
        Log.d(TAG, "loadThumbnail: $pageIndex")
        val thumbnail = thumbnailCache[pageIndex]
        if (thumbnail != null) {
            Log.d(TAG, "loadThumbnail: cache hit for page $pageIndex")
            return thumbnail
        }
        val bitmap = getBitmap(pageIndex)
        if (bitmap != null) {
            thumbnailCache.put(pageIndex, bitmap)
        }
        return bitmap
    }

    fun clearCache() {
        thumbnailCache.evictAll()
    }
}