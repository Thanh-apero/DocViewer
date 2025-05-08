package com.cherry.doc

import android.view.View
import android.view.View.OnClickListener
import android.widget.AdapterView.OnItemClickListener
import androidx.recyclerview.widget.RecyclerView
import com.cherry.doc.data.DocGroupInfo
import com.cherry.doc.databinding.RvDocCellBinding

class DocViewHolder : RecyclerView.ViewHolder, OnClickListener {
    var mOnItemClickListener: OnItemClickListener? = null
    private val binding: RvDocCellBinding

    constructor(itemView: View) : super(itemView) {
        binding = RvDocCellBinding.bind(itemView)
        itemView.setOnClickListener(this)
    }

    fun bindData(data: DocGroupInfo?) {
        binding.mTvTypeName.text = data?.typeName

        var cellAdapter = DocCellAdapter(itemView.context, mOnItemClickListener, adapterPosition)
        cellAdapter.showDatas(data?.docList)

        binding.mRvDocCell.adapter = cellAdapter
    }

    override fun onClick(v: View?) {
        mOnItemClickListener?.onItemClick(null, v, adapterPosition, 0)
    }
}