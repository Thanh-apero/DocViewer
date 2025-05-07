package com.cherry.lib.doc

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.cherry.lib.doc.bean.DocEngine
import com.cherry.lib.doc.util.Constant
import com.cherry.lib.doc.databinding.ActivityDocViewerBinding
import com.cherry.lib.doc.office.system.ViewMode

open class DocViewerActivity : AppCompatActivity() {
    private val TAG = "DocViewerActivity"
    
    private lateinit var binding: ActivityDocViewerBinding

    companion object {
        fun launchDocViewer(
            activity: AppCompatActivity, docSourceType: Int, path: String?,
            fileType: Int? = null, engine: Int? = null
        ) {
            var intent = Intent(activity, DocViewerActivity::class.java)
            intent.putExtra(Constant.INTENT_SOURCE_KEY, docSourceType)
            intent.putExtra(Constant.INTENT_DATA_KEY, path)
            intent.putExtra(Constant.INTENT_TYPE_KEY, fileType)
            intent.putExtra(Constant.INTENT_ENGINE_KEY, engine)
            activity.startActivity(intent)
        }
    }

    var docSourceType = 0
    var fileType = -1
    var engine: Int = DocEngine.INTERNAL.value
    var docUrl: String? = null// 文件地址

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDocViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initView()
        initData(intent)
    }

    fun initView() {
    }

    fun initData(intent: Intent?) {
        docUrl = intent?.getStringExtra(Constant.INTENT_DATA_KEY)
        docSourceType = intent?.getIntExtra(Constant.INTENT_SOURCE_KEY, 0) ?: 0
        fileType = intent?.getIntExtra(Constant.INTENT_TYPE_KEY, -1) ?: -1
        engine = intent?.getIntExtra(Constant.INTENT_ENGINE_KEY, DocEngine.INTERNAL.value) ?: DocEngine.INTERNAL.value

        binding.mDocView.openDoc(this,docUrl,docSourceType,fileType,false, DocEngine.values().first { it.value == engine })
        binding.changeViewMode.setOnClickListener {
            when(binding.mDocView.getControl()?.viewMode){
                ViewMode.VERTICAL_SNAP -> binding.mDocView.getControl()?.changeViewMode(ViewMode.VERTICAL_CONTINUOUS)
                ViewMode.VERTICAL_CONTINUOUS -> binding.mDocView.getControl()?.changeViewMode(ViewMode.HORIZONTAL_CONTINUOUS)
                ViewMode.HORIZONTAL_CONTINUOUS -> binding.mDocView.getControl()?.changeViewMode(ViewMode.HORIZONTAL_SNAP)
                ViewMode.HORIZONTAL_SNAP -> binding.mDocView.getControl()?.changeViewMode(ViewMode.VERTICAL_SNAP)
                else -> {}
            }
        }
        Log.e(TAG, "initData-docUrl = $docUrl")
        Log.e(TAG, "initData-docSourceType = $docSourceType")
        Log.e(TAG, "initData-fileType = $fileType")
        Log.e(TAG, "initData-engine = $engine")
    }

}