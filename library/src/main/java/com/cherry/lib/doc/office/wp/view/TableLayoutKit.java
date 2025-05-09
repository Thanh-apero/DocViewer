/*
 * 文件名称:          TableLayoutKit.java
 *  
 * 编译器:            android2.2
 * 时间:              上午11:26:53
 */
package com.cherry.lib.doc.office.wp.view;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Vector;

import com.cherry.lib.doc.office.constant.MainConstant;
import com.cherry.lib.doc.office.constant.wp.WPAttrConstant;
import com.cherry.lib.doc.office.constant.wp.WPViewConstant;
import com.cherry.lib.doc.office.simpletext.model.AttrManage;
import com.cherry.lib.doc.office.simpletext.model.IDocument;
import com.cherry.lib.doc.office.simpletext.model.IElement;
import com.cherry.lib.doc.office.simpletext.view.DocAttr;
import com.cherry.lib.doc.office.simpletext.view.IRoot;
import com.cherry.lib.doc.office.simpletext.view.IView;
import com.cherry.lib.doc.office.simpletext.view.PageAttr;
import com.cherry.lib.doc.office.simpletext.view.ParaAttr;
import com.cherry.lib.doc.office.simpletext.view.TableAttr;
import com.cherry.lib.doc.office.simpletext.view.ViewKit;
import com.cherry.lib.doc.office.system.IControl;
import com.cherry.lib.doc.office.wp.model.CellElement;
import com.cherry.lib.doc.office.wp.model.RowElement;
import com.cherry.lib.doc.office.wp.model.TableElement;

/**
 * 表格布局算法
 * <p>
 * <p>
 * Read版本:        Read V1.0
 * <p>
 * 作者:            ljj8494
 * <p>
 * 日期:            2012-5-14
 * <p>
 * 负责人:          ljj8494
 * <p>
 * 负责小组:         
 * <p>
 * <p>
 */
public class TableLayoutKit
{   
    
    /**
     * 
     */
    public TableLayoutKit()
    {
        breakPagesCell = new LinkedHashMap<Integer, BreakPagesCell>();
    }
    /**
     * 布局段浇
     * @param docAttr       文档属性
     * @param pageAttr      页面属性
     * @param paraAttr      段浇属性
     * @param tableView     布局表格视图
     * @param startOffset   布局开始Offset
     * @param x             布局开始x值
     * @param y             布局开始y值
     * @param w             布局的宽度
     * @param h             布局的高度
     * @param flag          布局标记
     * @param isBreakPages  是否跨页
     * @return
     */
    public int layoutTable(IControl control, IDocument doc, IRoot root, DocAttr docAttr, PageAttr pageAttr, ParaAttr paraAttr,
                           TableView tableView, long startOffset, int x, int y, int w, int h, int flag, boolean isBreakPages) {
        mergedCell.clear();

        int span = h;
        int dx = 0;
        int dy = 0; // Relative Y for placing new rows within the tableView
        int breakType = WPViewConstant.BREAK_NO;
        TableElement tableElem = (TableElement) tableView.getElement();
        AttrManage.instance().fillTableAttr(tableAttr, tableElem.getAttribute());
        flag = ViewKit.instance().setBitValue(flag, WPViewConstant.LAYOUT_PARA_IN_TABLE, true);
        boolean keepOne = ViewKit.instance().getBitValue(flag, WPViewConstant.LAYOUT_FLAG_KEEPONE);
        long maxEndOffsetInTableElement = tableElem.getEndOffset();

        int accumulatedTableHeight = 0; // Total height of rows laid out in this pass
        int maxTableWidth = 0; // Max width of the table in this pass
        RowView currentRowView = null; // The RowView currently being processed or last processed
        long currentLayoutProcessingOffset = startOffset; // The offset from which to fetch the next content

        RowElement explicitBreakRowToProcess = null;
        if (isBreakPages && this.breakRowElement != null && this.breakRowElement.getStartOffset() >= startOffset) {
            explicitBreakRowToProcess = this.breakRowElement;
            currentLayoutProcessingOffset = this.breakRowElement.getStartOffset(); // Ensure we start at the break row
        }
        this.breakRowElement = null; // Reset for this layout pass

        boolean alreadyProcessedExplicitBreak = false;
        int loopCount = 0;

        while (currentLayoutProcessingOffset < maxEndOffsetInTableElement && accumulatedTableHeight < span) {
            loopCount++;

            if (explicitBreakRowToProcess != null && !alreadyProcessedExplicitBreak) {
            } else if (accumulatedTableHeight >= span) {
                break;
            }

            isRowBreakPages = false; // Reset for each row
            IElement rowElementToLayout;
            boolean isProcessingExplicitBreakThisIteration = false;

            if (explicitBreakRowToProcess != null && !alreadyProcessedExplicitBreak) {
                rowElementToLayout = explicitBreakRowToProcess;
                isProcessingExplicitBreakThisIteration = true;
                alreadyProcessedExplicitBreak = true; // Consume it for this pass
            } else {
                if (currentLayoutProcessingOffset >= maxEndOffsetInTableElement) {
                    break;
                }
                rowElementToLayout = tableElem.getElementForIndex(rowIndex);
                rowIndex++; // Increment rowIndex after fetching
            }

            if (rowElementToLayout == null) {
                break;
            }

            if (!isProcessingExplicitBreakThisIteration) {
                currentLayoutProcessingOffset = rowElementToLayout.getStartOffset(); // Ensure offset matches new row
            }

            if (currentRowView != null) {
                // System.out.println("Calling layoutMergedCell for previous rowView");
                layoutMergedCell(currentRowView, (RowElement) rowElementToLayout, false);
                dy = accumulatedTableHeight;
            } else {
                dy = accumulatedTableHeight; // Should be 0 for the first row
            }
            currentRowView = (RowView) ViewFactory.createView(control, rowElementToLayout, null, WPViewConstant.TABLE_ROW_VIEW);
            tableView.appendChlidView(currentRowView);
            currentRowView.setStartOffset(currentLayoutProcessingOffset);
            currentRowView.setLocation(dx, dy);
            int heightAvailableForRowLayout = span - accumulatedTableHeight;

            breakType = layoutRow(control, doc, root, docAttr, pageAttr, paraAttr, currentRowView, currentLayoutProcessingOffset, dx, dy, w, heightAvailableForRowLayout, flag, isProcessingExplicitBreakThisIteration);
            int rowActualHeightInSpan = currentRowView.getLayoutSpan(WPViewConstant.Y_AXIS);

            if (rowActualHeightInSpan <= 0 && !keepOne && !(isRowBreakPages && rowActualHeightInSpan == 0)) {
                tableView.deleteView(currentRowView, true);

                IView lastChild = tableView.getChildView();
                if (lastChild != null) {
                    while (lastChild.getNextView() != null) {
                        lastChild = lastChild.getNextView();
                    }
                }
                currentRowView = (RowView) lastChild;

                this.breakRowElement = (RowElement) rowElementToLayout;
                breakType = WPViewConstant.BREAK_LIMIT;
                break;
            }

            maxTableWidth = Math.max(maxTableWidth, currentRowView.getLayoutSpan(WPViewConstant.X_AXIS));
            accumulatedTableHeight += rowActualHeightInSpan;
            currentLayoutProcessingOffset = currentRowView.getEndOffset(null);

            if (isRowBreakPages) {
                this.breakRowElement = (RowElement) rowElementToLayout;
                breakType = WPViewConstant.BREAK_LIMIT;
                break;
            }

            if (accumulatedTableHeight >= span && currentLayoutProcessingOffset < maxEndOffsetInTableElement) {
                if (currentLayoutProcessingOffset < rowElementToLayout.getEndOffset()) {
                    this.breakRowElement = (RowElement) rowElementToLayout;
                } else {
                    IElement nextRowElemAfterFilledSpan = tableElem.getElementForIndex(rowIndex); // rowIndex already incremented if new row was fetched
                    if (nextRowElemAfterFilledSpan != null) {
                        this.breakRowElement = (RowElement) nextRowElemAfterFilledSpan;
                    } else {
                        // this.breakRowElement remains null if all content up to maxEndOffsetInTableElement is laid out
                    }
                }
                breakType = WPViewConstant.BREAK_LIMIT;
                break;
            }
            keepOne = false;
        }
        layoutMergedCell(currentRowView, null, true);

        tableView.setSize(maxTableWidth, accumulatedTableHeight);

        if (this.breakRowElement != null) {
            tableView.setEndOffset(currentLayoutProcessingOffset);
        } else {
            tableView.setEndOffset(currentLayoutProcessingOffset);
        }

        if (docAttr.rootType == WPViewConstant.PAGE_ROOT) {
            byte hor = (byte) AttrManage.instance().getParaHorizontalAlign(tableElem.getAttribute());
            int want = w - maxTableWidth;
            if (hor == WPAttrConstant.PARA_HOR_ALIGN_CENTER || hor == WPAttrConstant.PARA_HOR_ALIGN_RIGHT) {
                if (hor == WPAttrConstant.PARA_HOR_ALIGN_CENTER) {
                    want /= 2;
                }
                tableView.setX(tableView.getX() + want);
            } else {
                tableView.setX(tableView.getX() - tableAttr.leftMargin
                        + (int) (AttrManage.instance().getParaIndentLeft(tableElem.getAttribute()) * MainConstant.TWIPS_TO_PIXEL));
            }
        }
        breakRowView = currentRowView;
        return breakType;
    }

    /**
     * 
     */
    private void clearCurrentRowBreakPageCell(IElement currentElem)
    {
        Vector<Integer> keys = new Vector<Integer>();
        for (Integer key : breakPagesCell.keySet())
        {
            BreakPagesCell bc = breakPagesCell.get(key);
            if (bc.getCell().getStartOffset() >= currentElem.getStartOffset()
                && bc.getCell().getEndOffset() <= currentElem.getEndOffset())
            {
                keys.add(key);
            }
        }
        for (Integer key : keys)
        {
            breakPagesCell.remove(key);
        }
    }
    
    /**
     * 布局行
     * @param docAttr       文档属性
     * @param pageAttr      页面属性
     * @param paraAttr      段浇属性
     * @param rowView       布局行视图
     * @param startOffset   布局开始Offset
     * @param x             布局开始x值
     * @param y             布局开始y值
     * @param w             布局的宽度
     * @param h             布局的高度
     * @param flag          布局标记
     * @return
     */
    public int layoutRow(IControl control, IDocument doc, IRoot root, DocAttr docAttr, PageAttr pageAttr, ParaAttr paraAttr,
        RowView rowView, long startOffset, int x, int y, int w, int h, int flag, boolean isBreakPages)
    {
        int dx = 0;
        int dy = 0;
        int breakType =  WPViewConstant.BREAK_NO;
        
        RowElement rowElem = (RowElement)rowView.getElement();
        long maxEnd = rowElem.getEndOffset();
        int rowHeight = (int)(AttrManage.instance().getTableRowHeight(rowElem.getAttribute()) * MainConstant.TWIPS_TO_PIXEL);
        int rowWidth = 0;
        int cellWidth;
        int cellHeight;
        int maxCellHeight = 0;
        int maxRowHeight = rowHeight;
        int cellIndex = 0;
        boolean isNullCell;
        boolean isInvalid = true;
        
        while (cellIndex < rowElem.getCellNumber()
            /*|| breakPagesCell.size() > 0 && isBreakPages*/)
        {
            IElement cellElem = null;
            isNullCell = false;
            if (isBreakPages && breakPagesCell.size() > 0)
            {
                if (breakPagesCell.containsKey(cellIndex))
                {
                    BreakPagesCell breakCell = breakPagesCell.remove(cellIndex);
                    cellElem =  breakCell.getCell();
                    startOffset = breakCell.getBreakOffset();
                }
                else
                {
                    cellElem = rowElem.getElementForIndex(cellIndex);
                    isNullCell = true;
                }
            }
            else
            {   
                cellElem = rowElem.getElementForIndex(cellIndex);
                if (cellElem == null)
                {
                    break;
                }
                startOffset = cellElem.getStartOffset();
                isNullCell = startOffset ==  cellElem.getEndOffset();
                if (!isNullCell && breakRowView != null && isBreakPages)
                {
                    CellView temp = breakRowView.getCellView((short)cellIndex);
                    if (temp != null)
                    {
                        isNullCell = temp.getEndOffset(null) == cellElem.getEndOffset();
                    }
                }
            }
            
            CellView cellView = (CellView)ViewFactory.createView(control, cellElem, null, WPViewConstant.TABLE_CELL_VIEW);
            rowView.appendChlidView(cellView);
            cellView.setStartOffset(startOffset);
            cellView.setLocation(dx, dy);
            cellView.setColumn((short)cellIndex);
            
            if (isNullCell)
            {
                cellView.setFirstMergedCell(isBreakPages);
                breakType = layoutCellForNull(doc, root, docAttr, pageAttr, paraAttr, cellView, startOffset, dx, dy, w, h, flag, cellIndex, isBreakPages);
            }
            else
            {
                cellView.setFirstMergedCell(isBreakPages || AttrManage.instance().isTableVerFirstMerged(cellElem.getAttribute()));
                cellView.setMergedCell(AttrManage.instance().isTableVerMerged(cellElem.getAttribute()));
                breakType = layoutCell(control, doc, root, docAttr, pageAttr, paraAttr, cellView, startOffset, dx, dy, w, h, flag, cellIndex, isBreakPages);
            }
            cellWidth = cellView.getLayoutSpan(WPViewConstant.X_AXIS);
            cellHeight = cellView.getLayoutSpan(WPViewConstant.Y_AXIS); 
            isInvalid = isInvalid && cellHeight == 0;
            dx += cellWidth;
            rowWidth += cellWidth;
            w -= cellWidth;
            if (!cellView.isMergedCell())
            {
                maxRowHeight = Math.max(maxRowHeight, cellHeight);
            }
            if (cellView.isFirstMergedCell())
            {
                mergedCell.add(cellView);
            }
            maxCellHeight = Math.max(maxCellHeight, cellHeight);
            startOffset = cellView.getEndOffset(null);
            cellIndex++;
        }
        CellView cellView = (CellView)rowView.getChildView();
        while (cellView != null)
        {
            if (!cellView.isMergedCell()
                && cellView.getLayoutSpan(WPViewConstant.Y_AXIS) < maxRowHeight)
            {
                cellView.setHeight(maxRowHeight - cellView.getTopIndent() - cellView.getBottomIndent());
                CellElement cellElem = (CellElement)cellView.getElement();
                if(cellElem != null)
                {
                	tableAttr.cellVerticalAlign = (byte)AttrManage.instance().getTableCellVerAlign(cellElem.getAttribute());
                }
                layoutCellVerticalAlign(cellView);                
            }
            cellView = (CellView)cellView.getNextView();
        }
        rowView.setEndOffset(maxEnd);
        if (isInvalid)
        {
            maxRowHeight = Integer.MAX_VALUE;
        }
        rowView.setSize(rowWidth, maxRowHeight);
        breakRowView =  null;
        return breakType;
    }
    
    /**
     * 布局单元格
     * @param docAttr       文档属性
     * @param pageAttr      页面属性
     * @param paraAttr      段浇属性
     * @param cellView      布局单元格视图
     * @param startOffset   布局开始Offset
     * @param x             布局开始x值
     * @param y             布局开始y值
     * @param w             布局的宽度
     * @param h             布局的高度
     * @param flag          布局标记
     * @return
     */
    public int layoutCell(IControl control, IDocument doc, IRoot root, DocAttr docAttr, PageAttr pageAttr, ParaAttr paraAttr,
        CellView cellView, long startOffset, int x, int y, int w, int h, int flag, int cellIndex, boolean isBreakPages)
    {        
        CellElement cellElem = (CellElement)cellView.getElement();
        AttrManage.instance().fillTableAttr(tableAttr, cellElem.getAttribute());
        cellView.setBackground(tableAttr.cellBackground);
        
        cellView.setIndent(tableAttr.leftMargin, tableAttr.topMargin, tableAttr.rightMargin, tableAttr.bottomMargin);
        int dx = tableAttr.leftMargin;
        int dy = tableAttr.topMargin;
        
        int breakType = WPViewConstant.BREAK_NO;
        
        long maxEnd = cellElem.getEndOffset();
        int cellHeight = 0;
        int spanH = h - tableAttr.topMargin - tableAttr.bottomMargin;
        int cellWidth = tableAttr.cellWidth - tableAttr.leftMargin - tableAttr.rightMargin;
        
        while (startOffset < maxEnd && spanH > 0 && breakType != WPViewConstant.BREAK_LIMIT)
        {
            IElement paraElem = doc.getParagraph(startOffset);
            ParagraphView paraView = (ParagraphView)ViewFactory.createView(control, paraElem, null, WPViewConstant.PARAGRAPH_VIEW);
            cellView.appendChlidView(paraView);
            paraView.setStartOffset(startOffset);
            paraView.setLocation(dx, dy);
            //
            AttrManage.instance().fillParaAttr(cellView.getControl(), paraAttr, paraElem.getAttribute());
            breakType = LayoutKit.instance().layoutPara(control, doc, docAttr, pageAttr, paraAttr, paraView, 
                startOffset, dx, dy, cellWidth, spanH, flag);
            //
            int paraHeight = paraView.getLayoutSpan(WPViewConstant.Y_AXIS);
            if (paraView.getChildView() == null)
            {
                cellView.deleteView(paraView, true);
                break;
            }
            if (root.getViewContainer() != null)
            {
                root.getViewContainer().add(paraView);
            }
            
            dy += paraHeight;
            cellHeight += paraHeight;
            spanH -= paraHeight;
            
            startOffset = paraView.getEndOffset(null);
            paraView.setEndOffset(startOffset);
            flag = ViewKit.instance().setBitValue(flag, WPViewConstant.LAYOUT_FLAG_KEEPONE, false);
        }
        if (startOffset < maxEnd)
        {
            if (!breakPagesCell.containsKey(cellIndex) && cellWidth > 0)
            {
                breakPagesCell.put(cellIndex, new BreakPagesCell(cellElem, startOffset));
                isRowBreakPages = true;
            }
        }
        cellView.setEndOffset(startOffset);
        cellView.setSize(cellWidth, cellHeight);
        return breakType;
    }
    
    /**
     * 布局空单元格
     * @param docAttr       文档属性
     * @param pageAttr      页面属性
     * @param paraAttr      段浇属性
     * @param cellView      布局单元格视图
     * @param startOffset   布局开始Offset
     * @param x             布局开始x值
     * @param y             布局开始y值
     * @param w             布局的宽度
     * @param h             布局的高度
     * @param flag          布局标记
     * @return
     */
    public int layoutCellForNull(IDocument doc, IRoot root, DocAttr docAttr, PageAttr pageAttr, ParaAttr paraAttr,
        CellView cellView, long startOffset, int x, int y, int w, int h, int flag, int cellIndex, boolean isBreakPages)
    {
        CellElement cellElem = (CellElement)cellView.getElement();
        AttrManage.instance().fillTableAttr(tableAttr, cellElem.getAttribute());        
        cellView.setIndent(tableAttr.leftMargin, tableAttr.topMargin, tableAttr.rightMargin, tableAttr.bottomMargin);
        int cellHeight = 0;
        int cellWidth = tableAttr.cellWidth - tableAttr.leftMargin - tableAttr.rightMargin;
        cellView.setSize(cellWidth, cellHeight);
        return WPViewConstant.BREAK_NO;
    }
    
    /**
     * 
     */
    private void layoutMergedCell(RowView row, RowElement nextRowElem, boolean isLastRow)
    {       
        if (row == null)
        {
            return;
        }
        int maxY = row.getY() + row.getLayoutSpan(WPViewConstant.Y_AXIS);
        if (isLastRow)
        {
            for (CellView cell : mergedCell)
            {
                if (cell.getParentView() != null)
                {
                    cell.setHeight(maxY - cell.getParentView().getY());
                    layoutCellVerticalAlign(cell);
                }
            }
            mergedCell.clear();
            return;
        }
        for (CellView cell : mergedCell)
        {
            maxY = Math.max(maxY, cell.getParentView().getY() + cell.getLayoutSpan(WPViewConstant.Y_AXIS));
        }
        Vector<CellView> vector = new Vector<CellView>(); 
        for(CellView cell : mergedCell)
        {            
            IElement cellElem = nextRowElem.getElementForIndex(cell.getColumn());
            if (cellElem == null)
            {
                continue;
            }
            if (!AttrManage.instance().isTableVerMerged(cellElem.getAttribute())
                || AttrManage.instance().isTableVerFirstMerged(cellElem.getAttribute()))
            {
                int cellHeight = cell.getLayoutSpan(WPViewConstant.Y_AXIS);
                if (cell.getParentView().getY() + cellHeight < maxY)
                {
                    cell.setHeight( maxY - cell.getParentView().getY());
                    layoutCellVerticalAlign(cell);
                }
                else
                {
                    row.setHeight(maxY - row.getY());
                    CellView cellView = (CellView)row.getChildView();
                    while (cellView != null)
                    {
                        if (!cellView.isMergedCell())
                        {
                            int oldHeight = cellView.getHeight();
                            cellView.setHeight(maxY - cellView.getParentView().getY());
                            if(oldHeight != cellView.getHeight())
                            {
                                layoutCellVerticalAlign(cellView);
                            }
                        }
                        cellView = (CellView)cellView.getNextView();
                    }
                }
                vector.add(cell);
            }
        }
        for (CellView cell : vector)
        {
            maxY = cell.getParentView().getY() + cell.getLayoutSpan(WPViewConstant.Y_AXIS);
            if (maxY > row.getY() + row.getLayoutSpan(WPViewConstant.Y_AXIS))
            {
                cell.setHeight(row.getY() + row.getLayoutSpan(WPViewConstant.Y_AXIS) - cell.getY());
            }
            mergedCell.remove(cell);
        }
    }
    
    /**
     * 判断给行是否跨行
     */
    private boolean isBreakPages(RowView rowView)
    {
        IView view = rowView.getChildView();
        while (view != null)
        {
            IElement elem = view.getElement();
            if (view.getEndOffset(null) != elem.getEndOffset()
                && view.getWidth() > 0)
            {
                return true;
            }
            view = view.getNextView();
        }
        return false;
    }
    
    /**
     * 
     */
    private void layoutCellVerticalAlign(CellView cellView)
    {
        if (tableAttr.cellVerticalAlign ==  WPAttrConstant.PARA_VER_ALIGN_TOP)
        {
            return;
        }
        int textHeight = 0;
        IView para = cellView.getChildView();
        while (para != null)
        {
            textHeight += para.getLayoutSpan(WPViewConstant.Y_AXIS);
            para = para.getNextView();
        }
        int want = cellView.getLayoutSpan(WPViewConstant.Y_AXIS) - textHeight;
        int verAlignmnet = AttrManage.instance().getTableCellVerAlign(cellView.getElement().getAttribute());
        if (verAlignmnet == WPAttrConstant.PARA_VER_ALIGN_CENTER
            || verAlignmnet == WPAttrConstant.PARA_VER_ALIGN_BOTTOM)
        {
            if (verAlignmnet == WPAttrConstant.PARA_VER_ALIGN_CENTER)
            {
                want /= 2;
            }
            para = cellView.getChildView();
            while (para != null)
            {
                para.setY(para.getY() + want);
                para = para.getNextView();
            }
        }
    }
    
    /**
     * 表格是否跨页
     */
    public boolean isTableBreakPages()
    {
        return breakPagesCell.size() > 0
            || breakRowElement !=  null;
    }
    
    /**
     * 
     */
    public void clearBreakPages()
    {
        rowIndex = 0;
        breakRowElement = null;
        breakPagesCell.clear();
        breakRowView = null;
    }
    
    /**
     * 
     */
    public void dispose()
    {
        breakRowElement = null;
        breakPagesCell.clear();
        breakRowView = null;
    }
    
    private boolean isRowBreakPages;
    private short rowIndex;
    private RowElement breakRowElement;
    private RowView breakRowView;
    private Map<Integer, BreakPagesCell> breakPagesCell;
    private TableAttr tableAttr = new TableAttr();
    private Vector<CellView> mergedCell = new Vector<CellView>(); 
}