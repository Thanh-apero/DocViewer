package com.cherry.lib.doc.widget

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Environment
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.webkit.*
import androidx.constraintlayout.widget.ConstraintLayout
import com.cherry.lib.doc.R
import com.cherry.lib.doc.databinding.DocWebViewBinding
import com.cherry.lib.doc.interfaces.OnWebLoadListener

/*
 * -----------------------------------------------------------------
 * Copyright (C) 2018-2028, by Victor, All rights reserved.
 * -----------------------------------------------------------------
 * File: ProgressWebView
 * Author: Victor
 * Date: 2022/3/1 18:28
 * Description: 
 * -----------------------------------------------------------------
 */
class DocWebView : ConstraintLayout, DownloadListener {
    val TAG = "DocWebView"
    var isLastLoadSuccess = false//是否成功加载完成过web，成功过后的网络异常 不改变web
    var isError = false
    var openLinkBySysBrowser = false//是否使用系统浏览器打开http链接
    var mOnWebLoadListener: OnWebLoadListener? = null
    
    private val binding: DocWebViewBinding

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle) {
        binding = DocWebViewBinding.inflate(LayoutInflater.from(context), this, true)
        initView()
    }

    fun initView() {
        binding.mDocView.webChromeClient = DocWebChromeClient()
        binding.mDocView.webViewClient = DocWebViewClient()
        //设置可以支持缩放
        binding.mDocView.settings.setSupportZoom(true)
        //设置出现缩放工具
        binding.mDocView.settings.builtInZoomControls = true
        //设定缩放控件隐藏
        binding.mDocView.settings.displayZoomControls = true
        //设置可在大视野范围内上下左右拖动，并且可以任意比例缩放
        binding.mDocView.settings.useWideViewPort = true
        //设置默认加载的可视范围是大视野范围
        binding.mDocView.settings.loadWithOverviewMode = true
        //自适应屏幕 SINGLE_COLUMN：把所有内容放大到webview等宽的一列中 NORMAL：正常显示不做任何渲染。NARROW_COLUMNS：可能的话让所有列的宽度不超过屏幕宽度
        binding.mDocView.settings.layoutAlgorithm = WebSettings.LayoutAlgorithm.SINGLE_COLUMN

        binding.mDocView.settings.javaScriptEnabled = true
        binding.mDocView.settings.domStorageEnabled = true
        binding.mDocView.settings.allowFileAccess = true
        binding.mDocView.settings.allowFileAccessFromFileURLs = true
        binding.mDocView.settings.allowUniversalAccessFromFileURLs = true
        binding.mDocView.settings.cacheMode = WebSettings.LOAD_NO_CACHE

        binding.mDocView.setDownloadListener(this)
    }

    private fun setProgress(newProgress: Int) {
        mOnWebLoadListener?.OnWebLoadProgress(newProgress)
    }

    @SuppressLint("JavascriptInterface")
    fun addJavascriptInterface(jsInterface: Any) {
        binding.mDocView.addJavascriptInterface(jsInterface, "SSDJsBirdge")
    }

    fun reload() {
        isError = false
        binding.mDocView.reload()
    }

    fun loadUrl(url: String) {
        isError = false
        try {
            binding.mDocView.loadUrl(url)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun loadData(htmlData: String) {
        binding.mDocView.loadData(htmlData, "text/html", "utf-8")
    }

    fun loadData(htmlData: String, secondLinkBySysBrowser: Boolean) {
        openLinkBySysBrowser = secondLinkBySysBrowser
        binding.mDocView.loadData(htmlData, "text/html", "utf-8")
    }

    fun downloadFile(url: String?, contentDisposition: String?, mimeType: String?) {
        val request = DownloadManager.Request(Uri.parse(url))
        // 允许媒体扫描，根据下载的文件类型被加入相册、音乐等媒体库
        request.allowScanningByMediaScanner()
        // 设置通知的显示类型，下载进行时和完成后显示通知
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        // 设置通知栏的标题，如果不设置，默认使用文件名
        request.setTitle("下载完成")
        // 设置通知栏的描述
//                    request.setDescription("This is description");
        // 允许在计费流量下下载
        request.setAllowedOverMetered(true)
        // 允许该记录在下载管理界面可见
        request.setVisibleInDownloadsUi(true)
        // 允许漫游时下载
        request.setAllowedOverRoaming(true)
        val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
        Log.e(TAG, "downloadFile()-fileName = $fileName")
        request.setDestinationInExternalPublicDir(Environment.getExternalStorageDirectory().toString() + "/Download/", fileName)
        val downloadManager = binding.mDocView.context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        // 添加一个下载任务
        val downloadId = downloadManager.enqueue(request)

    }

    override fun onDownloadStart(
        url: String?,
        userAgent: String?,
        contentDisposition: String?,
        mimeType: String?,
        contentLength: Long
    ) {
        Log.e(TAG, "onDownloadStart()......url = $url")
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse(url)
        context.startActivity(intent)
    }

    fun canGoBack(): Boolean {
        val canGoBack = binding.mDocView.canGoBack()
        if (canGoBack) {
            binding.mDocView.goBack()
        }
        return canGoBack
    }

    fun onPause() {
        binding.mDocView.pauseTimers()
    }

    fun onResume() {
        binding.mDocView.resumeTimers()
    }

    /**
     * must be called on the main thread
     */
    fun onDestroy() {
        try {
            binding.mDocView.clearHistory()
            binding.mDocView.clearCache(true)
            binding.mDocView.loadUrl("about:blank") // clearView() should be changed to loadUrl("about:blank"), since clearView() is deprecated now
            binding.mDocView.freeMemory()
            binding.mDocView.pauseTimers()
            binding.mDocView.destroy() // Note that mWebView.destroy() and mWebView = null do the exact same thing
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setWebViewBackgroundColor(isBlack: Boolean) {
        if (isBlack) {
            //防止加载html白屏(针对播放视频)
            setBackgroundColor(Color.BLACK)
        }

    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        onDestroy()
    }

    private inner class DocWebChromeClient : WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            setProgress(newProgress)
        }

        override fun onReceivedTitle(view: WebView, title: String) {
            super.onReceivedTitle(view, title)
            if (title.contains("html")) {
                return
            }
            mOnWebLoadListener?.onTitle(title)
        }
    }

    private inner class DocWebViewClient : WebViewClient() {
        override fun onPageFinished(view: WebView, url: String) {
            super.onPageFinished(view, url)
            //在访问失败的时候会首先回调onReceivedError，然后再回调onPageFinished。
            if (!isError) {
                isLastLoadSuccess = true
                mOnWebLoadListener?.OnWebLoadProgress(100)
            }
        }

        override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
            super.onReceivedError(view, request, error)
            //在访问失败的时候会首先回调onReceivedError，然后再回调onPageFinished。
            isError = true
            if (!isLastLoadSuccess) {//之前成功加载完成过，不会回调
                mOnWebLoadListener?.OnWebLoadProgress(100)
            }
        }
    }
}