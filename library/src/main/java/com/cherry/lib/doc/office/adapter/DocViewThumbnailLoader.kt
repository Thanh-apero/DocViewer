package com.cherry.lib.doc.office.adapter

import android.graphics.Bitmap
import android.util.LruCache
import com.cherry.lib.doc.office.wp.control.Word

class DocViewThumbnailLoader(
    private val word: Word
) {
    companion object{
        private const val TAG = "DocViewThumbnailLoader"
        private const val CACHE_SIZE = 32
    }

    private val thumbnailCache = LruCache<Int, Bitmap>(CACHE_SIZE)

    fun loadThumbnail(pageIndex: Int): Bitmap? {
        val thumbnail = thumbnailCache[pageIndex]
        if (thumbnail != null) {
            return thumbnail
        }
        val bitmap = word.pageToImage(pageIndex)
        if (bitmap != null) {
            thumbnailCache.put(pageIndex, bitmap)
        }
        return bitmap
    }

    fun clearCache() {
        thumbnailCache.evictAll()
    }
}