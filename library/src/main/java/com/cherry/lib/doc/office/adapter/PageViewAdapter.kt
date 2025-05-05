package com.cherry.lib.doc.office.adapter

import com.cherry.lib.doc.office.system.IControl
import com.cherry.lib.doc.office.wp.control.WPControl
import com.cherry.lib.doc.office.pg.control.PGControl

class PageViewAdapter(private val iControl: IControl) {
    
    fun getAdapter(): BaseViewAdapter {
        return when(iControl) {
            is WPControl -> WordAdapter(iControl)
            is PGControl -> PPTAdapter(iControl)
            else -> throw IllegalArgumentException("Unsupported control type")
        }
    }
}