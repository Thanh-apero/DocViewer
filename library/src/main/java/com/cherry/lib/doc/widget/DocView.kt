package com.cherry.lib.doc.widget

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.blankj.utilcode.util.UriUtils
import com.cherry.lib.doc.R
import com.cherry.lib.doc.bean.DocEngine
import com.cherry.lib.doc.bean.DocSourceType
import com.cherry.lib.doc.bean.FileType
import com.cherry.lib.doc.databinding.DocViewBinding
import com.cherry.lib.doc.interfaces.OnDocPageChangeListener
import com.cherry.lib.doc.interfaces.OnDownloadListener
import com.cherry.lib.doc.interfaces.OnWebLoadListener
import com.cherry.lib.doc.office.IOffice
import com.cherry.lib.doc.office.adapter.BaseViewAdapter
import com.cherry.lib.doc.office.adapter.PageViewAdapter
import com.cherry.lib.doc.office.system.IControl
import com.cherry.lib.doc.pdf.PdfDownloader
import com.cherry.lib.doc.pdf.PdfPageViewAdapter
import com.cherry.lib.doc.pdf.PdfQuality
import com.cherry.lib.doc.pdf.PdfRendererCore
import com.cherry.lib.doc.util.Constant
import com.cherry.lib.doc.util.FileUtils
import com.cherry.lib.doc.util.ViewUtils.hide
import com.cherry.lib.doc.util.ViewUtils.show
import java.io.File
import java.net.URLEncoder

class DocView : FrameLayout, OnDownloadListener, OnWebLoadListener {

    private val TAG = "DocView"

    var mActivity: Activity? = null
    var lifecycleScope: LifecycleCoroutineScope = (context as AppCompatActivity).lifecycleScope
    private var mPoiViewer: PoiViewer? = null
    private var pdfRendererCore: PdfRendererCore? = null
    private var pdfPageViewAdapter: PdfPageViewAdapter? = null
    private var quality = PdfQuality.NORMAL
    private var engine = DocEngine.INTERNAL
    private var showDivider = true
    private var showPageNum = true
    private var divider: Drawable? = null
    private var runnable = Runnable {}
    var enableLoadingForPages: Boolean = true
    var pbDefaultHeight = 2
    var pbHeight: Int = pbDefaultHeight
    var pbDefaultColor = Color.RED
    var pbColor: Int = pbDefaultColor

    private var pdfRendererCoreInitialised = false
    var pageMargin: Rect = Rect(0,0,0,0)

    var totalPageCount = 0

    var mOnDocPageChangeListener: OnDocPageChangeListener? = null

    var sourceFilePath: String? = null
    var mViewPdfInPage: Boolean = true
    private var pageViewAdapter: BaseViewAdapter? = null

    private lateinit var binding: DocViewBinding
    private var iControl: IControl? = null

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        initView(attrs, defStyle)
    }

    fun initView(attrs: AttributeSet?, defStyle: Int) {
        binding = DocViewBinding.inflate(LayoutInflater.from(context), this, true)

        val typedArray =
            context.obtainStyledAttributes(attrs, R.styleable.DocView, defStyle, 0)
        val ratio =
            typedArray.getInt(R.styleable.DocView_dv_quality, PdfQuality.NORMAL.ratio)
        quality = PdfQuality.values().first { it.ratio == ratio }
        val engineValue =
            typedArray.getInt(R.styleable.DocView_dv_engine, DocEngine.INTERNAL.value)
        engine = DocEngine.values().first { it.value == engineValue }
        showDivider = typedArray.getBoolean(R.styleable.DocView_dv_showDivider, true)
        showPageNum = typedArray.getBoolean(R.styleable.DocView_dv_show_page_num, true)
        divider = typedArray.getDrawable(R.styleable.DocView_dv_divider)
        enableLoadingForPages = typedArray.getBoolean(R.styleable.DocView_dv_enableLoadingForPages, enableLoadingForPages)
        pbHeight = typedArray.getDimensionPixelSize(R.styleable.DocView_dv_page_pb_height, pbDefaultHeight)
        pbColor = typedArray.getColor(R.styleable.DocView_dv_page_pb_color, pbDefaultColor)

        val marginDim = typedArray.getDimensionPixelSize(R.styleable.DocView_dv_page_margin, 0)
        pageMargin = Rect(marginDim, marginDim, marginDim, marginDim).apply {
            top = typedArray.getDimensionPixelSize(R.styleable.DocView_dv_page_marginTop, top)
            left = typedArray.getDimensionPixelSize(R.styleable.DocView_dv_page_marginLeft, left)
            right = typedArray.getDimensionPixelSize(R.styleable.DocView_dv_page_marginRight, right)
            bottom = typedArray.getDimensionPixelSize(R.styleable.DocView_dv_page_marginBottom, bottom)
        }

        var layoutParams = binding.mPlLoadProgress.layoutParams
        layoutParams.height = pbHeight
        binding.mPlLoadProgress.layoutParams = layoutParams

        binding.mPlLoadProgress.progressTintList = ColorStateList.valueOf(pbColor)

        typedArray.recycle()


        runnable = Runnable {
            binding.mPdfPageNo.hide()
        }
    }


    fun openDoc(activity: Activity, docUrl: String?, docSourceType: Int,
                engine: DocEngine = this.engine) {
        mActivity = activity
        openDoc(activity, docUrl, docSourceType,-1,false,engine)
    }

    fun openDoc(activity: Activity?, docUrl: String?,
                docSourceType: Int, fileType: Int,
                viewPdfInPage: Boolean = false,
                engine: DocEngine = this.engine) {
        var fileType = fileType
        var docUrl = docUrl
        var docSourceType = docSourceType
        if (docUrl != null && docSourceType == DocSourceType.URI && fileType == -1) {
            // 如果是URI类型，且文件类型为-1，则获取一下文件类型，保证正确读取
            val uri = docUrl.toUri()
            Log.d(TAG, "openDoc reset uri = $uri")
            val file = UriUtils.uri2File(uri)
            if (file != null) {
                var mimeType = ""
                fileType = FileUtils.getFileTypeForUrl(file.absolutePath)
                if (fileType == FileType.NOT_SUPPORT) {
                    mimeType = FileUtils.getFileMimeType(context, uri) ?: "*/*"
                    fileType = FileUtils.getFileTypeForUrl(FileUtils.mimeExtMap[mimeType] ?: "")
                }
                docUrl = file.absolutePath
                docSourceType = DocSourceType.PATH
                Log.d(TAG, "openDoc reset url = $docUrl")
                Log.d(TAG, "openDoc reset docSourceType = $docSourceType")
                Log.d(TAG, "openDoc reset fileType = $fileType, mimeType = $mimeType")
            } else {
                Log.d(TAG, "file = null")
            }
        }
        Log.e(TAG,"openDoc()......fileType = $fileType")
        mActivity = activity
        mViewPdfInPage = viewPdfInPage
        if (docSourceType == DocSourceType.PATH) {
            sourceFilePath = docUrl
        } else {
            sourceFilePath = null
        }
        if (docSourceType == DocSourceType.URL && fileType != FileType.IMAGE) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP
                || engine == DocEngine.MICROSOFT
                || engine == DocEngine.XDOC
                || engine == DocEngine.GOOGLE) {
                showByWeb(docUrl ?: "",engine)
                return
            }
            downloadFile(docUrl ?: "")
            return
        }

        var type = FileUtils.getFileTypeForUrl(docUrl)
        if (fileType > 0) {
            type = fileType
        }
        when (type) {
            FileType.PDF -> {
            }
            FileType.IMAGE -> {
                if (showPageNum) {
                    showPageNum = false
                }
                Log.e(TAG, "openDoc()......")
                binding.mDocWeb.hide()
                binding.mFlDocContainer.hide()
                binding.mIvImage.show()
                if (docSourceType == DocSourceType.PATH) {
                    Log.e(TAG, "openDoc()......PATH")
                    binding.mIvImage.load(File(docUrl))
                } else {
                    Log.e(TAG, "openDoc()......URL")
                    binding.mIvImage.load(docUrl)
                }
            }
            FileType.NOT_SUPPORT -> {
                if (showPageNum) {
                    showPageNum = false
                }
                Log.e(TAG, "openDoc()......NOT_SUPPORT")
                binding.mDocWeb.show()
                binding.mFlDocContainer.hide()
                binding.mIvImage.hide()
                showByWeb(docUrl ?: "", this.engine)
            }
            else -> {
                Log.e(TAG, "openDoc()......ELSE")
                if (showPageNum) {
                    showPageNum = false
                }
                binding.mDocWeb.hide()
                binding.mFlDocContainer.show()
                binding.mIvImage.hide()
                activity?.let { showDoc(it, binding.mFlDocContainer, docUrl, docSourceType, fileType) }
            }
        }
    }

    fun showDoc(activity: Activity, mDocContainer: ViewGroup?, url: String?, docSourceType: Int, fileType: Int) {
        Log.e(TAG,"showDoc()......")
        var iOffice: IOffice = object: IOffice() {
            override fun getActivity(): Activity {
                return activity
            }

            override fun openFileFinish() {
                pageViewAdapter = PageViewAdapter(appControl).getAdapter()
                setupRecyclerView()
                iControl = appControl
                mDocContainer?.postDelayed({
                    mDocContainer.removeAllViews()
                    mDocContainer.addView(
                        view,
                        RelativeLayout.LayoutParams(
                            RelativeLayout.LayoutParams.MATCH_PARENT,
                            RelativeLayout.LayoutParams.MATCH_PARENT
                        )
                    )
                },200)
            }

            override fun openFileFailed() {
                try {
                    if (mPoiViewer == null) {
                        mPoiViewer = PoiViewer(context)
                    }
                    mPoiViewer?.loadFile(binding.mFlDocContainer, sourceFilePath)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(context, R.string.open_failed, Toast.LENGTH_SHORT).show()
                }
            }

            override fun getAppName(): String {
                return context.resources.getString(R.string.loading)
            }

            override fun getTemporaryDirectory(): File {
                val file = activity.getExternalFilesDir(null)
                return file ?: activity.filesDir
            }

            override fun fullScreen(fullscreen: Boolean) {
            }

            override fun changePage() {
                super.changePage()
                pageViewAdapter?.changePage()
            }

        }
        iOffice.openFile(url, docSourceType, fileType.toString())
    }

    fun initWithUrl(
        url: String,
        pdfQuality: PdfQuality = this.quality,
        engine: DocEngine = this.engine,
        lifecycleScope: LifecycleCoroutineScope = (context as AppCompatActivity).lifecycleScope
    ) {
        this.lifecycleScope = lifecycleScope
        downloadFile(url, pdfQuality, lifecycleScope)
    }

    fun downloadFile(url: String, pdfQuality: PdfQuality = this.quality,
                     lifecycleScope: LifecycleCoroutineScope = (context as AppCompatActivity).lifecycleScope) {
        PdfDownloader(url, this)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun showByWeb(url: String, engine: DocEngine = this.engine) {
        binding.mDocWeb.mOnWebLoadListener = this

        var engineUrl = "engine"
        when (engine) {
            DocEngine.MICROSOFT -> {
                engineUrl = Constant.MICROSOFT_URL
            }
            DocEngine.XDOC -> {
                engineUrl = Constant.XDOC_VIEW_URL
            }
            DocEngine.GOOGLE -> {
                engineUrl = Constant.GOOGLE_URL
            }
            else -> {
                engineUrl = Constant.XDOC_VIEW_URL
            }
        }
        binding.mDocWeb.loadUrl("$engineUrl${URLEncoder.encode(url, "UTF-8")}")
    }

    override fun getDownloadContext() = context

    override fun onDownloadStart() {
        Log.e(TAG,"initWithUrl-onDownloadStart()......")
    }

    override fun onDownloadProgress(currentBytes: Long, totalBytes: Long) {
        var progress = (currentBytes.toFloat() / totalBytes.toFloat() * 100F).toInt()
        if (progress >= 100) {
            progress = 100
        }
        Log.e(TAG,"initWithUrl-onDownloadProgress()......progress = $progress")
        showLoadingProgress(progress)
    }

    override fun onDownloadSuccess(absolutePath: String) {
        Log.e(TAG,"initWithUrl-onDownloadSuccess()......")
        showLoadingProgress(100)
        sourceFilePath = absolutePath
        openDoc(mActivity, absolutePath, DocSourceType.PATH,-1,mViewPdfInPage)
    }

    override fun onError(error: Throwable) {
        error.printStackTrace()
        Log.e(TAG,"initWithUrl-onError()......${error.localizedMessage}")
        showLoadingProgress(100)
    }

    override fun getCoroutineScope() = lifecycleScope

    override fun OnWebLoadProgress(progress: Int) {
        showLoadingProgress(progress)
    }

    override fun onTitle(title: String?) {
    }

    fun showLoadingProgress(progress: Int) {
        if (progress == 100) {
            binding.mPlLoadProgress.hide()
        } else {
            binding.mPlLoadProgress?.show()
            binding.mPlLoadProgress?.progress = progress
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        onDestroy()
    }

    fun closePdfRender() {
        try {
            if (pdfRendererCoreInitialised) {
                pdfRendererCore?.closePdfRender()
                pdfRendererCoreInitialised = false
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    fun onDestroy() {
        mPoiViewer?.recycle()
        closePdfRender()
        mOnDocPageChangeListener = null
    }

    private fun setupRecyclerView(){
        binding.rvPageView.adapter = pageViewAdapter
        binding.rvPageView.layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
    }

    fun getControl(): IControl?{
        return iControl
    }
}