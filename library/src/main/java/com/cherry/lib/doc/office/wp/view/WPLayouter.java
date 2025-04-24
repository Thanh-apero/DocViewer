/*
 * 文件名称:          WPLayouter.java
 *  
 * 编译器:            android2.2
 * 时间:              下午2:13:54
 */
package com.cherry.lib.doc.office.wp.view;

import java.util.ArrayList;
import java.util.List;

import com.cherry.lib.doc.office.constant.wp.AttrIDConstant;
import com.cherry.lib.doc.office.constant.wp.WPModelConstant;
import com.cherry.lib.doc.office.constant.wp.WPViewConstant;
import com.cherry.lib.doc.office.simpletext.model.AttrManage;
import com.cherry.lib.doc.office.simpletext.model.IDocument;
import com.cherry.lib.doc.office.simpletext.model.IElement;
import com.cherry.lib.doc.office.simpletext.view.DocAttr;
import com.cherry.lib.doc.office.simpletext.view.IView;
import com.cherry.lib.doc.office.simpletext.view.PageAttr;
import com.cherry.lib.doc.office.simpletext.view.ParaAttr;
import com.cherry.lib.doc.office.simpletext.view.ViewKit;
import com.cherry.lib.doc.office.wp.model.WPDocument;

import android.util.Log;

/**
 * Word 布局器
 * <p>
 * <p>
 * Read版本:        Read V1.0
 * <p>
 * 作者:            ljj8494
 * <p>
 * 日期:            2011-11-20
 * <p>
 * 负责人:          ljj8494
 * <p>
 * 负责小组:         
 * <p>
 * <p>
 */
public class WPLayouter
{
    /**
     * 
     * @param root
     */
    public WPLayouter(PageRoot root)
    {
        this.root = root;
        docAttr = new DocAttr();
        docAttr.rootType = WPViewConstant.PAGE_ROOT;
        pageAttr = new PageAttr();
        paraAttr = new ParaAttr();
        tableLayout = new TableLayoutKit();
        hfTableLayout = new TableLayoutKit();
    }
    
    /**
     * 
     */
    public void doLayout()
    {
        tableLayout.clearBreakPages();
        doc = root.getDocument();
        // 正文区或
        // mainRange = doc.getRange();
        section = doc.getSection(0);
        // 
        AttrManage.instance().fillPageAttr(pageAttr, section.getAttribute());
        //
        PageView pv = (PageView)ViewFactory.createView(root.getControl(), section, null, WPViewConstant.PAGE_VIEW);
        root.appendChlidView(pv);
        layoutPage(pv);
        LayoutKit.instance().layoutAllPage(root, 1.0f);
    }
    
    /**
     * 
     */
    public int layoutPage(PageView pageView)
    {
    	pageView.setPageNumber(currentPageNumber++);
    	
        layoutHeaderAndFooter(pageView);
        int breakType = WPViewConstant.BREAK_NO;
        pageView.setSize(pageAttr.pageWidth, pageAttr.pageHeight);
        pageView.setIndent(pageAttr.leftMargin, pageAttr.topMargin, pageAttr.rightMargin, pageAttr.bottomMargin);
        pageView.setStartOffset(currentLayoutOffset);
        
        int dx = pageAttr.leftMargin;
        int dy = pageAttr.topMargin;
        int spanW = pageAttr.pageWidth - pageAttr.leftMargin - pageAttr.rightMargin;
        int spanH = pageAttr.pageHeight - pageAttr.topMargin - pageAttr.bottomMargin;
        int flag = ViewKit.instance().setBitValue(0, WPViewConstant.LAYOUT_FLAG_KEEPONE, true);
        long maxEnd = doc.getAreaEnd(WPModelConstant.MAIN);
       
        IElement elem = breakPara != null ? breakPara.getElement() : doc.getParagraph(currentLayoutOffset);
        
        ParagraphView para = null;
        if (breakPara != null)
        {
            para = breakPara;
            // process table break;
            if (breakPara.getType() == WPViewConstant.TABLE_VIEW)
            {
                pageView.setHasBreakTable(true);
                ((TableView)breakPara).setBreakPages(true);
            }
        }
        else if (AttrManage.instance().hasAttribute(elem.getAttribute(), AttrIDConstant.PARA_LEVEL_ID))
        {
            elem = ((WPDocument)doc).getParagraph0(currentLayoutOffset);
            para = (ParagraphView)ViewFactory.createView(root.getControl(), elem, null, WPViewConstant.TABLE_VIEW);
        }
        else
        {   
            para = (ParagraphView)ViewFactory.createView(root.getControl(), elem, null, WPViewConstant.PARAGRAPH_VIEW);
        }
        pageView.appendChlidView(para);
        
        para.setStartOffset(currentLayoutOffset);
        para.setEndOffset(elem.getEndOffset());
        boolean keepOne = true;
        int contentHeight = 0;
        int maxSinglePageHeight = pageAttr.pageHeight * 3; // Define a reasonable max height

        while (spanH > 0 && currentLayoutOffset < maxEnd && breakType != WPViewConstant.BREAK_LIMIT
            && breakType != WPViewConstant.BREAK_PAGE)
        {
            para.setLocation(dx, dy);
            // 表格段落
            if (para.getType() == WPViewConstant.TABLE_VIEW)
            {             
                if (para.getPreView() != null)
                {
                    if (para.getPreView().getElement() != elem)
                    {
                        tableLayout.clearBreakPages();
                    }
                }
                breakType = tableLayout.layoutTable(root.getControl(), doc, root, docAttr, pageAttr, paraAttr, 
                    (TableView)para, currentLayoutOffset, dx, dy, spanW, spanH, flag, breakPara != null);
            }
            else
            {
                tableLayout.clearBreakPages();
                AttrManage.instance().fillParaAttr(root.getControl(), paraAttr, elem.getAttribute());
                breakType = LayoutKit.instance().layoutPara(root.getControl(), doc, docAttr, pageAttr, paraAttr, 
                    para, currentLayoutOffset, dx, dy, spanW, spanH, flag);
            }
            int paraHeight = para.getLayoutSpan(WPViewConstant.Y_AXIS);
            contentHeight += paraHeight;

            // Check if content is getting excessively large for a single page
            if (contentHeight > maxSinglePageHeight) {
                Log.w(TAG, "Content height (" + contentHeight + ") exceeds maximum reasonable height (" +
                        maxSinglePageHeight + "). This may indicate an issue with content rendering.");

                // If this paragraph is extraordinarily large, it might be causing the problem
                if (paraHeight > pageAttr.pageHeight) {
                    Log.e(TAG, "Found extremely tall paragraph: " + paraHeight + "px. Forcing break.");
                    breakType = WPViewConstant.BREAK_LIMIT;
                    break;
                }
            }

            if (!keepOne && para.getChildView() == null)
            {
                if (breakPara == null)
                {
                    elem = doc.getParagraph(currentLayoutOffset - 1);
                }
                pageView.deleteView(para, true);
                break;
            }
            //
            if (para.getType() != WPViewConstant.TABLE_VIEW)
            {
                root.getViewContainer().add(para);
            }
            // 收集段落中的 shape view
            collectShapeView(pageView, para, false);
            
            dy += paraHeight;
            currentLayoutOffset = para.getEndOffset(null);
            spanH -= paraHeight;
            if (spanH > 0 && currentLayoutOffset < maxEnd && breakType != WPViewConstant.BREAK_LIMIT
                && breakType != WPViewConstant.BREAK_PAGE)
            {                
                elem = doc.getParagraph(currentLayoutOffset);
                if (AttrManage.instance().hasAttribute(elem.getAttribute(), AttrIDConstant.PARA_LEVEL_ID))
                {
                    if (elem != para.getElement())
                    {
                        tableLayout.clearBreakPages(); 
                    }
                    elem = ((WPDocument)doc).getParagraph0(currentLayoutOffset);
                    para = (ParagraphView)ViewFactory.createView(root.getControl(), elem, null, WPViewConstant.TABLE_VIEW);                    
                }
                else
                {
                    para = (ParagraphView)ViewFactory.createView(root.getControl(), elem, null, WPViewConstant.PARAGRAPH_VIEW);
                }
                para.setStartOffset(currentLayoutOffset);
                pageView.appendChlidView(para);
            }
            flag = ViewKit.instance().setBitValue(flag, WPViewConstant.LAYOUT_FLAG_KEEPONE, false);
            breakPara = null;
            keepOne = false;
        }
        // table
        if (para.getType() == WPViewConstant.TABLE_VIEW && tableLayout.isTableBreakPages())
        {
            breakPara = (ParagraphView)ViewFactory.createView(root.getControl(), elem, null, WPViewConstant.TABLE_VIEW);
            pageView.setHasBreakTable(true);
            ((TableView)para).setBreakPages(true);
        }
        // 
        else if (elem != null && currentLayoutOffset < elem.getEndOffset())
        {
            breakPara = (ParagraphView)ViewFactory.createView(root.getControl(), elem, null, WPViewConstant.PARAGRAPH_VIEW);
        }
        
        pageView.setEndOffset(currentLayoutOffset);
        //
        root.getViewContainer().sort();
        //
        root.addPageView(pageView);
        //
        pageView.setPageBackgroundColor(pageAttr.pageBRColor);
        //
        pageView.setPageBorder(pageAttr.pageBorder);
        
        return breakType;
    }
    
    
    /**
     * 
     */
    private void layoutHeaderAndFooter(PageView pageView)
    {
        if (header == null)
        {
            header = layoutHFParagraph(pageView, true);
            if (header != null)
            {
                int h = header.getLayoutSpan(WPViewConstant.Y_AXIS);
                if (pageAttr.headerMargin + h > pageAttr.topMargin)
                {
                    pageAttr.topMargin = pageAttr.headerMargin + h; 
                }
                header.setParentView(pageView);
            }
        }
        else
        {
            for (LeafView sv :shapeViews)
            {                
                if (WPViewKit.instance().getArea(sv.getStartOffset(null)) == WPModelConstant.HEADER)
                {
                    pageView.addShapeView(sv);
                }
            }
        }
        pageView.setHeader(header);
        if (footer == null)
        {
            footer = layoutHFParagraph(pageView, false);
            if (footer != null)
            {                
                if (footer.getY() < pageAttr.pageHeight - pageAttr.bottomMargin)
                {
                    pageAttr.bottomMargin =  pageAttr.pageHeight - footer.getY(); 
                }
                footer.setParentView(pageView);
            }
        }
        else
        {
            for (LeafView sv :shapeViews)
            {                
                if (WPViewKit.instance().getArea(sv.getStartOffset(null)) == WPModelConstant.FOOTER)
                {
                    pageView.addShapeView(sv);
                }
            }
        }
        
        pageView.setFooter(footer);
    }
    
    /**
     * 
     */
    private TitleView layoutHFParagraph(PageView pageView, boolean isHeader)
    {
        long offset = isHeader ? WPModelConstant.HEADER : WPModelConstant.FOOTER;
        int breakType = WPViewConstant.BREAK_NO;
        IElement hfElem = doc.getHFElement(offset, WPModelConstant.HF_ODD);
        if (hfElem == null)
        {
            return null;
        }
        
        //ignore line pitch for header and footer layout
    	float oldLinePitch = pageAttr.pageLinePitch;
    	pageAttr.pageLinePitch = -1;
    	
        TitleView titleView = (TitleView)ViewFactory.createView(root.getControl(), hfElem, null, WPViewConstant.TITLE_VIEW);
        titleView.setPageRoot(root);
        titleView.setLocation(pageAttr.leftMargin, pageAttr.headerMargin);
        
        long maxEnd = hfElem.getEndOffset();
        int spanW = pageAttr.pageWidth - pageAttr.leftMargin - pageAttr.rightMargin;
        int spanH = (pageAttr.pageHeight - pageAttr.topMargin - pageAttr.bottomMargin - 100) / 2;
        int flag = ViewKit.instance().setBitValue(0, WPViewConstant.LAYOUT_FLAG_KEEPONE, true);
        ParagraphView para = null;
        IElement paraElem = doc.getParagraph(offset);
        if (AttrManage.instance().hasAttribute(paraElem.getAttribute(), AttrIDConstant.PARA_LEVEL_ID))
        {
            paraElem = ((WPDocument)doc).getParagraph0(offset);
            para = (ParagraphView)ViewFactory.createView(root.getControl(), paraElem, null, WPViewConstant.TABLE_VIEW);
        }
        else
        {   
            para = (ParagraphView)ViewFactory.createView(root.getControl(), paraElem, null, WPViewConstant.PARAGRAPH_VIEW);
        }        
        titleView.appendChlidView(para);
        
        para.setStartOffset(offset);
        para.setEndOffset(paraElem.getEndOffset());
        boolean keepOne = true;
        int dx = 0;
        int dy = 0;
        int titleHeight = 0;
        while (spanH > 0 && offset < maxEnd && breakType != WPViewConstant.BREAK_LIMIT)
        {
            para.setLocation(dx, dy);
            // 表格段落
            if (para.getType() == WPViewConstant.TABLE_VIEW)
            {
                breakType = hfTableLayout.layoutTable(root.getControl(), doc, root, docAttr, pageAttr, paraAttr, 
                    (TableView)para, offset, dx, dy, spanW, spanH, flag, breakPara != null);
            }
            else
            {
                hfTableLayout.clearBreakPages();
                AttrManage.instance().fillParaAttr(root.getControl(), paraAttr, paraElem.getAttribute());
                breakType = LayoutKit.instance().layoutPara(root.getControl(), doc, docAttr, pageAttr, paraAttr, 
                    para, offset, dx, dy, spanW, spanH, flag);
            }
            int paraHeight = para.getLayoutSpan(WPViewConstant.Y_AXIS);
            if (!keepOne && para.getChildView() == null)
            {
                titleView.deleteView(para, true);
                break;
            }
            dy += paraHeight;
            titleHeight += paraHeight;
            offset = para.getEndOffset(null);
            spanH -= paraHeight;
            // 收集段落中的 shape view
            collectShapeView(pageView, para, true);
            if (spanH > 0 && offset < maxEnd && breakType != WPViewConstant.BREAK_LIMIT)
            {
                paraElem = doc.getParagraph(offset);
                if (AttrManage.instance().hasAttribute(paraElem.getAttribute(), AttrIDConstant.PARA_LEVEL_ID))
                {
                    if (paraElem != para.getElement()) {
                        tableLayout.clearBreakPages();
                    }
                    paraElem = ((WPDocument)doc).getParagraph0(offset);
                    para = (ParagraphView)ViewFactory.createView(root.getControl(), paraElem, null, WPViewConstant.TABLE_VIEW);
                }
                else
                {
                    para = (ParagraphView)ViewFactory.createView(root.getControl(), paraElem, null, WPViewConstant.PARAGRAPH_VIEW);
                }
                para.setStartOffset(offset);
                titleView.appendChlidView(para);
            }
            flag = ViewKit.instance().setBitValue(flag, WPViewConstant.LAYOUT_FLAG_KEEPONE, false);
            keepOne = false;
        }
        titleView.setSize(spanW, titleHeight);
        if (!isHeader)
        {
            titleView.setY(pageAttr.pageHeight - titleHeight - pageAttr.footerMargin);
        }
        
        //restore line pitch
    	pageAttr.pageLinePitch = oldLinePitch;
    	
        return titleView;
    }
    
    /**
     * 
     */
    public void backLayout()
    {
        PageView pv = (PageView)ViewFactory.createView(root.getControl(), section, null, WPViewConstant.PAGE_VIEW);
        root.appendChlidView(pv);
        layoutPage(pv);
    }

    /**
     * @return Returns the currentLayoutOffset.
     */
    public long getCurrentLayoutOffset()
    {
        return currentLayoutOffset;
    }

    /**
     * @param currentLayoutOffset The currentLayoutOffset to set.
     */
    public void setCurrentLayoutOffset(long currentLayoutOffset)
    {
        this.currentLayoutOffset = currentLayoutOffset;
    }
    
    /**
     * 
     */
    public boolean isLayoutFinish()
    {
        return currentLayoutOffset >= doc.getAreaEnd(WPModelConstant.MAIN) && breakPara == null;
    }
    
    /**
     * 
     */
    private void collectShapeView(PageView page, ParagraphView para, boolean isHF)
    {
        if (para.getType() == WPViewConstant.PARAGRAPH_VIEW)
        {
            collectShapeViewForPara(page, para, isHF);
        }
        else if (para.getType() == WPViewConstant.TABLE_VIEW)
        {
            IView row = para.getChildView();
            while (row != null)
            {
                IView cell = row.getChildView();
                while (cell != null)
                {
                    IView paraView = cell.getChildView();
                    while (paraView != null)
                    {
                        collectShapeViewForPara(page, (ParagraphView)para, isHF);
                        paraView = paraView.getNextView();
                    }
                    cell = cell.getNextView();
                }
                row = row.getNextView();
            }
        }
    }
    
    /**
     * 
     */
    private void collectShapeViewForPara(PageView page, ParagraphView para, boolean isHF)
    {
        IView line = para.getChildView();
        while (line != null)
        {
            IView leaf = line.getChildView();
            while (leaf != null)
            {                    
                if (leaf.getType() == WPViewConstant.SHAPE_VIEW)
                {
                    ShapeView shapeView = ((ShapeView)leaf);
                    if (!shapeView.isInline())
                    {
                        page.addShapeView(shapeView);
                        if (isHF)
                        {
                            shapeViews.add(shapeView);
                        }
                    }                        
                }
                else if(leaf.getType() == WPViewConstant.OBJ_VIEW)
                {
                    ObjView objView = ((ObjView)leaf);
                    if (!objView.isInline())
                    {
                        page.addShapeView(objView);
                        if (isHF)
                        {
                            shapeViews.add(objView);
                        }
                    } 
                }
                leaf = leaf.getNextView();
            }
            line = line.getNextView();
        }
    }
    
    /**
     * 
     */
    public void dispose()
    {
        docAttr.dispose();
        docAttr = null;
        pageAttr.dispose();
        pageAttr = null;
        paraAttr.dispose();
        paraAttr = null;
        root = null;
        doc = null;
        breakPara = null;
        header = null;
        footer = null;
        tableLayout = null;
        hfTableLayout = null;
        shapeViews.clear();
    }
    

    // 文档属性集
    private DocAttr docAttr;
    // 章节属性集
    private PageAttr pageAttr;
    // 段落
    private ParaAttr paraAttr; 
    //
    private PageRoot root;
    //
    private IDocument doc;
    
    
    // ======== 布局过程用到一些布局状态的值 ==========
    private IElement section;
    // 当前布局的页码数
    private int currentPageNumber = 1;
    // 当前需要布局的开始的Offset，主要为了段落切页用到。
    private long currentLayoutOffset;
    // 段落分页
    private ParagraphView breakPara;
    // header
    private TitleView header;
    // footer
    private TitleView footer;
    //
    private TableLayoutKit tableLayout;
    //
    private TableLayoutKit hfTableLayout;
    //
    private List<LeafView> shapeViews = new ArrayList<LeafView>();
    private static final String TAG = "WPLayouter";

    /**
     * Force layout to finish when it appears to be stuck
     * This method is called when layout is unable to make progress after multiple attempts
     */
    public void forceFinishLayout() {
        try {
            // Store current state for diagnosis
            final long startOffset = currentLayoutOffset;
            final int currentPageCount = root.getPageCount();
            IElement currentElement = null;

            boolean isTocDetected = false;

            try {
                currentElement = doc.getParagraph(startOffset);
                Log.e(TAG, "Layout stuck at offset: " + startOffset + ", document area end: " + doc.getAreaEnd(WPModelConstant.MAIN));
                Log.e(TAG, "Current page count: " + currentPageCount);

                // Try to determine if we're in a TOC section by analyzing various indicators
                if (currentElement != null) {
                    // Analyze content around this position to detect TOC
                    long searchStart = Math.max(0, currentElement.getStartOffset() - 200);
                    long searchEnd = Math.min(doc.getAreaEnd(WPModelConstant.MAIN), currentElement.getEndOffset() + 200);

                    try {
                        // Check a larger span around current position for TOC indicators
                        String surroundingText = doc.getText(searchStart, searchEnd);
                        if (surroundingText != null &&
                                (surroundingText.contains("Contents") ||
                                        surroundingText.contains("Table of") ||
                                        surroundingText.contains("Mục lục") ||
                                        surroundingText.contains("....") || // TOC often has dot leaders
                                        surroundingText.contains(". . . . ."))) {
                            isTocDetected = true;
                            Log.i(TAG, "Table of Contents section detected");
                        }
                    } catch (Exception e) {
                        // Ignore text extraction errors
                    }
                }

                // Log information about the problematic paragraph
                if (currentElement != null) {
                    Log.e(TAG, "Problematic element - Type: " + currentElement.getType() +
                            ", Start Offset: " + currentElement.getStartOffset() + ", End Offset: " + currentElement.getEndOffset());
                }

                // Special handling for Table of Contents (TOC)
                if (isTocDetected && breakPara != null) {
                    Log.i(TAG, "Attempting special handling for Table of Contents");
                    try {
                        // Let's handle the entire TOC in a single page by creating a special TOC page

                        // First, find the approximate TOC extent
                        long tocEndOffset = startOffset;
                        try {
                            // Search forward for end of TOC (look for 2000-3000 chars ahead, typical TOC size)
                            long searchEnd = Math.min(doc.getAreaEnd(WPModelConstant.MAIN), startOffset + 3000);

                            // Analyze text ahead to find where TOC likely ends
                            String aheadText = doc.getText(startOffset, searchEnd);

                            // Advance TOC endpoint generously to include the whole TOC
                            tocEndOffset = startOffset + Math.min(aheadText.length(), 2000);

                            Log.i(TAG, "Estimated TOC boundaries: " + startOffset + " to " + tocEndOffset);
                        } catch (Exception e) {
                            // If we can't determine TOC boundaries, use a reasonable default size
                            tocEndOffset = startOffset + 1500; // Skip about 1500 chars, typical TOC size
                            Log.i(TAG, "Using default TOC boundaries: " + startOffset + " to " + tocEndOffset);
                        }

                        // Get the current page or create a new one
                        PageView tocPage = null;
                        if (root.getPageCount() > 0) {
                            tocPage = root.getPageView(root.getPageCount() - 1);

                            // Ensure this page has the correct start offset
                            if (tocPage.getStartOffset(null) > startOffset) {
                                // Create a new page for TOC if necessary
                                tocPage = (PageView) ViewFactory.createView(root.getControl(), section, null, WPViewConstant.PAGE_VIEW);
                                tocPage.setPageNumber(currentPageNumber++);
                                tocPage.setSize(pageAttr.pageWidth, pageAttr.pageHeight);
                                tocPage.setIndent(pageAttr.leftMargin, pageAttr.topMargin, pageAttr.rightMargin, pageAttr.bottomMargin);
                                tocPage.setStartOffset(startOffset);
                                root.addPageView(tocPage);
                                root.appendChlidView(tocPage);
                            }
                        }

                        // Set the end offset to where we determined TOC ends
                        if (tocPage != null) {
                            tocPage.setEndOffset(tocEndOffset);
                        }

                        // Jump past the entire TOC to continue layout
                        currentLayoutOffset = tocEndOffset;
                        breakPara = null;

                        Log.i(TAG, "Created single TOC page spanning from " + startOffset + " to " + tocEndOffset +
                                " (" + (tocEndOffset - startOffset) + " chars)");

                        // We've handled the situation, no need to try skip
                        return;
                    } catch (Exception e) {
                        Log.e(TAG, "Error during TOC special handling: " + e.getMessage(), e);
                        // Fall through to normal processing if TOC handling fails
                    }
                }

                // Try to skip just the problematic section
                if (tryToSkipProblematicSection()) {
                    Log.w(TAG, "Skipped problematic section and continuing layout");
                    return;
                }

                // If skipping failed or this is called multiple times, force complete layout finish
                Log.w(TAG, "Forcing layout completion");

                // Store current state for diagnosis
                long currentOffset = currentLayoutOffset;
                int pageCount = root.getPageCount();

                // Force layout to completion
                currentLayoutOffset = doc.getAreaEnd(WPModelConstant.MAIN);
                breakPara = null;

                Log.i(TAG, "Layout forced to completion: offset " + currentOffset +
                        " -> " + currentLayoutOffset + ", page count: " + pageCount);

                // Generate an event to notify the document is ready to view
                if (root != null && root.getContainer() != null &&
                        root.getContainer().getControl() != null) {
                    root.getContainer().getControl().actionEvent(
                            com.cherry.lib.doc.office.constant.EventConstant.WP_LAYOUT_COMPLETED, true);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error while forcing layout completion", e);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error collecting diagnostic information: " + e.getMessage(), e);
        }
    }

    /**
     * Try to skip just the problematic section of content instead of forcing the entire layout to complete
     * @return true if the section was successfully skipped, false otherwise
     */
    private boolean tryToSkipProblematicSection() {
        try {
            // Store current offset
            long currentOffset = currentLayoutOffset;
            boolean offsetAdvanced = false;

            // If we're stuck at a paragraph
            if (breakPara != null) {
                Log.i(TAG, "Attempting to skip problematic paragraph at offset " + currentOffset);

                // Log detailed information about the break paragraph
                Log.e(TAG, "Problematic break paragraph details:");
                Log.e(TAG, "  - Type: " + breakPara.getType());
                Log.e(TAG, "  - Element type: " + (breakPara.getElement() != null ? breakPara.getElement().getType() : "null"));
                Log.e(TAG, "  - Start offset: " + breakPara.getStartOffset(null));
                Log.e(TAG, "  - End offset: " + breakPara.getEndOffset(null));
                Log.e(TAG, "  - Width: " + breakPara.getWidth() + ", Height: " + breakPara.getHeight());

                // Determine if this is a large structured element that needs special handling
                boolean isLargeStructuredContent = false;
                boolean hasInvalidDimensions = breakPara.getWidth() == 0 && breakPara.getHeight() == 0;
                boolean isOversizedContent = breakPara.getHeight() >= pageAttr.pageHeight - pageAttr.topMargin - pageAttr.bottomMargin;

                try {
                    if (breakPara.getElement() != null) {
                        IElement elem = breakPara.getElement();

                        // Check if element has structural properties of a complex content block
                        // rather than relying on text content keywords
                        if (elem.getAttribute() != null) {
                            // Check for specific structural attributes that indicate a complex
                            // structured section like TOC, index, or other special content
                            isLargeStructuredContent =
                                    // Check for invalid paragraph height or offset issues
                                    (hasInvalidDimensions || isOversizedContent) &&
                                            // Only handle elements with actual content
                                            (elem.getEndOffset() - elem.getStartOffset() > 10);

                            if (isLargeStructuredContent) {
                                Log.i(TAG, "Detected structured content requiring special layout handling");
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error analyzing element structure: " + e.getMessage());
                }

                // Try to fix and properly render content rather than skipping
                if (hasInvalidDimensions || isOversizedContent || isLargeStructuredContent) {

                    Log.i(TAG, "Handling oversized/complex content at offset: " + currentOffset);

                    // Fix the dimensions of the breakPara
                    if (breakPara.getWidth() == 0) {
                        breakPara.setWidth(pageAttr.pageWidth - pageAttr.leftMargin - pageAttr.rightMargin);
                        Log.i(TAG, "Fixed paragraph width: " + breakPara.getWidth());
                    }

                    // Set a reasonable height that will fit on the page
                    if (breakPara.getHeight() == 0 || breakPara.getHeight() > pageAttr.pageHeight - pageAttr.topMargin - pageAttr.bottomMargin) {
                        int maxHeight = (pageAttr.pageHeight - pageAttr.topMargin - pageAttr.bottomMargin) / 2;
                        breakPara.setHeight(maxHeight); // Use half the available height
                        Log.i(TAG, "Fixed paragraph height: " + breakPara.getHeight());
                    }

                    // Fix the offsets
                    if (breakPara.getStartOffset(null) == 0 && breakPara.getEndOffset(null) == 0) {
                        if (breakPara.getElement() != null) {
                            breakPara.setStartOffset(breakPara.getElement().getStartOffset());
                            breakPara.setEndOffset(breakPara.getElement().getEndOffset());
                            Log.i(TAG, "Fixed paragraph offsets using element: " +
                                    breakPara.getStartOffset(null) + "-" + breakPara.getEndOffset(null));
                        } else {
                            breakPara.setStartOffset(currentOffset);
                            breakPara.setEndOffset(currentOffset + 1);
                            Log.i(TAG, "Fixed paragraph offsets manually: " + currentOffset + "-" + (currentOffset + 1));
                        }
                    }

                    // Instead of small increments, make a large skip for problematic content
                    // This helps avoid creating lots of small pages
                    currentLayoutOffset = currentOffset + 1000;

                    // Make sure we don't exceed document bounds
                    long docEnd = doc.getAreaEnd(WPModelConstant.MAIN);
                    if (currentLayoutOffset > docEnd) {
                        currentLayoutOffset = docEnd;
                    }

                    Log.i(TAG, "Skipping large section to avoid multiple small pages: " +
                            "from " + currentOffset + " to " + currentLayoutOffset +
                            " (+" + (currentLayoutOffset - currentOffset) + " chars)");

                    Log.i(TAG, "Fixed problematic paragraph and continuing layout");
                    return true;
                }
            }
            // If we're not at a paragraph break point but still stuck
            else if (currentOffset < doc.getAreaEnd(WPModelConstant.MAIN)) {
                // Try to find the next paragraph
                IElement nextElem = null;

                try {
                    nextElem = doc.getParagraph(currentOffset);
                } catch (Exception e) {
                    Log.e(TAG, "Error getting paragraph at offset " + currentOffset, e);
                }

                long skipTo;
                String content = "";

                if (nextElem != null) {
                    skipTo = nextElem.getEndOffset();

                    // Try to get some content for diagnostic
                    try {
                        content = doc.getText(nextElem.getStartOffset(),
                                Math.min(nextElem.getEndOffset(), nextElem.getStartOffset() + 50));
                    } catch (Exception e) {
                        // Ignore
                    }

                    // If this wouldn't advance us, fix by advancing minimally
                    if (skipTo <= currentOffset) {
                        skipTo = currentOffset + 1; // Advance minimally
                        Log.w(TAG, "Next paragraph not advancing, advancing by 1 character");
                    }
                } else {
                    // No valid paragraph found, advance minimally
                    skipTo = currentOffset + 1;
                    Log.w(TAG, "No valid paragraph found, advancing by 1 character");
                }

                Log.i(TAG, "Content at offset " + currentOffset + ": " + content);

                // If this would advance us, do it
                if (skipTo > currentOffset) {
                    Log.i(TAG, "Skipping from offset " + currentOffset + " to " + skipTo);
                    currentLayoutOffset = skipTo;
                    offsetAdvanced = true;
                }
            }

            // If we get here and haven't advanced yet, force a big skip
            if (!offsetAdvanced && currentOffset < doc.getAreaEnd(WPModelConstant.MAIN)) {
                // Skip ahead by 10% of the document
                long docLength = doc.getAreaEnd(WPModelConstant.MAIN) - doc.getAreaStart(WPModelConstant.MAIN);
                long skipAmount = Math.max(1000, docLength / 10);
                long skipTo = currentOffset + skipAmount;

                // Make sure we don't exceed document bounds
                long docEnd = doc.getAreaEnd(WPModelConstant.MAIN);
                if (skipTo > docEnd) {
                    skipTo = docEnd;
                }

                if (skipTo > currentOffset) {
                    Log.w(TAG, "Forcing a large skip from " + currentOffset + " to " + skipTo);
                    currentLayoutOffset = skipTo;
                    offsetAdvanced = true;
                }
            }

            // Return true to indicate we've made progress
            return offsetAdvanced;
        } catch (Exception e) {
            Log.e(TAG, "Error trying to fix problematic section: " + e.getMessage(), e);
            e.printStackTrace();

            // Make minimal advancement to prevent infinite loop
            currentLayoutOffset += 1;
            return true;
        }
    }

    /**
     * This method is kept for compatibility but no longer creates placeholder pages.
     * Instead, we fix and render content properly.
     */
    private boolean createPlaceholderForProblematicContent(long currentOffset, long nextOffset) {
        try {
            // Create a new page for the skipped content
            Log.i(TAG, "Creating new page after skipping problematic content");

            // Add a notification that content was skipped
            PageView lastPage = null;
            if (root.getPageCount() > 0) {
                lastPage = root.getPageView(root.getPageCount() - 1);
            }

            // Create a new blank page
            PageView newPage = (PageView) ViewFactory.createView(root.getControl(), section, null, WPViewConstant.PAGE_VIEW);
            newPage.setPageNumber(currentPageNumber++);
            root.addPageView(newPage);

            // Set proper offsets for the new page
            newPage.setStartOffset(currentLayoutOffset);
            if (lastPage != null) {
                lastPage.setEndOffset(currentLayoutOffset - 1);
            }

            // Add a visual indicator by adding a dummy paragraph to the page that explains content was skipped
            try {
                // Configure page for rendering
                newPage.setSize(pageAttr.pageWidth, pageAttr.pageHeight);
                newPage.setIndent(pageAttr.leftMargin, pageAttr.topMargin, pageAttr.rightMargin, pageAttr.bottomMargin);

                // Make sure the page is added to the root and visible
                root.appendChlidView(newPage);

                // Set appropriate end offset
                newPage.setEndOffset(currentLayoutOffset);

                // Trigger an update of the view
                if (root.getContainer() != null && root.getContainer().getControl() != null) {
                    root.getContainer().getControl().actionEvent(
                            com.cherry.lib.doc.office.constant.EventConstant.WP_SHOW_PAGE,
                            newPage.getPageNumber());
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to add content skipped indicator", e);
            }

            // Force the layout to continue from this new point
            breakPara = null;

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error creating new page after skip: " + e.getMessage(), e);
            // Continue with normal skip even if page creation fails
            return false;
        }
    }

    /**
     * Check if there is room to add content to the current page
     *
     * @param height Height of content to add
     * @return true if there's room, false if we should break to a new page
     */
    private boolean hasRoomOnPage(int height) {
        try {
            if (pageAttr == null) {
                return false;
            }

            int pageHeight = pageAttr.pageHeight;
            int topMargin = pageAttr.topMargin;
            int bottomMargin = pageAttr.bottomMargin;

            // Calculate available height on the page
            int availableHeight = pageHeight - topMargin - bottomMargin;

            // Leave some margin for error
            return height <= (availableHeight - 10);
        } catch (Exception e) {
            Log.e(TAG, "Error checking room on page: " + e.getMessage());
            return false;
        }
    }
}