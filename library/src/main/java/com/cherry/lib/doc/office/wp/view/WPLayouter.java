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

                // Check if we can get the end offset of the paragraph
                long skipTo = breakPara.getElement().getEndOffset();

                // Force skip forward if we're not making progress
                if (skipTo <= currentOffset) {
                    // Skip forward by a reasonable amount - 1000 characters should be enough
                    // to get past most problematic content without skipping too much
                    skipTo = currentOffset + 1000;
                    Log.w(TAG, "Element end offset isn't advancing, forcing skip to " + skipTo);
                }

                // Make sure we don't exceed document bounds
                long docEnd = doc.getAreaEnd(WPModelConstant.MAIN);
                if (skipTo > docEnd) {
                    skipTo = docEnd;
                }

                currentLayoutOffset = skipTo;
                breakPara = null;

                Log.i(TAG, "Skipped to offset " + currentLayoutOffset);
                offsetAdvanced = true;
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

                if (nextElem != null) {
                    skipTo = nextElem.getEndOffset();

                    // If this wouldn't advance us, use a fixed skip amount
                    if (skipTo <= currentOffset) {
                        skipTo = currentOffset + 1000; // Skip ahead by 1000 characters
                        Log.w(TAG, "Next paragraph not advancing, forcing skip by 1000 characters");
                    }
                } else {
                    // No valid paragraph found, skip ahead by fixed amount
                    skipTo = currentOffset + 1000;
                    Log.w(TAG, "No valid paragraph found, forcing skip by 1000 characters");
                }

                // Make sure we don't exceed document bounds
                long docEnd = doc.getAreaEnd(WPModelConstant.MAIN);
                if (skipTo > docEnd) {
                    skipTo = docEnd;
                }

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

            // If we successfully advanced the offset, trigger creation of a new page
            if (offsetAdvanced) {
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
                    Log.e(TAG, "Error creating new page after skip", e);
                    // Continue with normal skip even if page creation fails
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            Log.e(TAG, "Error trying to skip problematic section", e);
            return false;
        }
    }
}