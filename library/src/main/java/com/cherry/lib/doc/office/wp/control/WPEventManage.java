/*
 * 文件名称:          SSEventManage.java
 *
 * 编译器:            android2.2
 * 时间:              下午1:43:24
 */

package com.cherry.lib.doc.office.wp.control;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import com.cherry.lib.doc.office.common.hyperlink.Hyperlink;
import com.cherry.lib.doc.office.common.picture.PictureKit;
import com.cherry.lib.doc.office.constant.EventConstant;
import com.cherry.lib.doc.office.constant.wp.WPViewConstant;
import com.cherry.lib.doc.office.java.awt.Rectangle;
import com.cherry.lib.doc.office.simpletext.model.AttrManage;
import com.cherry.lib.doc.office.simpletext.model.IElement;
import com.cherry.lib.doc.office.system.IControl;
import com.cherry.lib.doc.office.system.beans.AEventManage;

/**
 * SS的事件管理，包括触摸、手型等事件
 * <p>
 * <p>
 * Read版本:        Read V1.0
 * <p>
 * 作者:            ljj8494
 * <p>
 * 日期:            2011-11-9
 * <p>
 * 负责人:          ljj8494
 * <p>
 * 负责小组:
 * <p>
 * <p>
 */
public class WPEventManage extends AEventManage {
    /**
     * @param word     the word view
     * @param control  the control interface
     */
    public WPEventManage(Word word, IControl control) {
        super(word.getContext(), control);
        this.word = word;
    }

    /**
     * 触摸事件
     */
    public boolean onTouch(View v, MotionEvent event) {
        try {
            super.onTouch(v, event);
            int action = event.getAction();
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    PictureKit.instance().setDrawPictrue(true);
                    processDown(v, event);
                    break;
                case MotionEvent.ACTION_MOVE:
                    break;
                case MotionEvent.ACTION_UP:
                    if (zoomChange) {
                        zoomChange = false;
                        if (word.getCurrentRootType() == WPViewConstant.PAGE_ROOT) {
                            control.actionEvent(EventConstant.APP_GENERATED_PICTURE_ID, null);
                        }
                        if (control.getMainFrame().isZoomAfterLayoutForWord()) {
                            control.actionEvent(EventConstant.WP_LAYOUT_NORMAL_VIEW, null);
                        }
                    }
                    word.getControl().actionEvent(EventConstant.SYS_UPDATE_TOOLSBAR_BUTTON_STATUS, null);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            //ErrorUtil.instance().writerLog(e);
            Log.d(TAG, "onTouch: " + e.getMessage());
            control.getSysKit().getErrorKit().writerLog(e);
        }
        return false;
    }

    private static final String TAG = "WPEventManage";

    /**
     * process touch event of down
     *
     * @param v
     * @param event
     */
    protected void processDown(View v, MotionEvent event) {
        int x = convertCoorForX(event.getX());
        int y = convertCoorForY(event.getY());
        long offset = word.viewToModel(x, y, false);
        if (word.getHighlight().isSelectText()) {
            word.getHighlight().removeHighlight();
            word.getStatus().setPressOffset(offset);
            word.postInvalidate();
        }
    }

    /**
     *
     */
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (word.getStatus().isSelectTextStatus()) {
            return true;
        }
        super.onScroll(e1, e2, distanceX, distanceY);
        // 向右
        boolean change = false;
        boolean isScrollX = Math.abs(distanceX) > Math.abs(distanceY);
        Rectangle r = word.getVisibleRect();
        int sX = r.x;
        int sY = r.y;
        float zoom = word.getZoom();
        int wW = 0;
        if (word.getCurrentRootType() == WPViewConstant.NORMAL_ROOT
                && control.getMainFrame().isZoomAfterLayoutForWord()) {
            if (word.getWidth() == word.getWordWidth()) {
                wW = word.getWidth();
            } else {
                wW = (int) (word.getWordWidth() * zoom);
            }
        } else {
            wW = (int) (word.getWordWidth() * zoom);
        }
        int wH = (int) (word.getWordHeight() * zoom);
        // X方向
        if (isScrollX) {
            // 向右
            if (distanceX > 0 && sX + r.width < wW) {
                sX += distanceX;
                if (sX + r.width > wW) {
                    sX = wW - r.width;
                }
                change = true;
            }
            // 向左
            else if (distanceX < 0 && sX > 0) {
                sX += distanceX;
                if (sX < 0) {
                    sX = 0;
                }
                change = true;
            }
        }
        // Y方向
        else {
            // 向下
            if (distanceY > 0 && sY + r.height < wH) {
                sY += distanceY;
                if (sY + r.height > wH) {
                    sY = wH - r.height;
                }
                change = true;
            }
            // 向上
            else if (distanceY < 0 && sY > 0) {
                sY += distanceY;
                if (sY < 0) {
                    sY = 0;
                }
                change = true;
            }
        }
        if (change) {
            isScroll = true;
            word.scrollTo(sX, sY);
        }
        return true;
    }

    /**
     * Fling the scroll view
     *
     * @param velocityX X方向速率
     * @param velocityY Y方向速率
     */
    public void fling(int velocityX, int velocityY) {
        super.fling(velocityX, velocityY);
        Rectangle r = word.getVisibleRect();
        float zoom = word.getZoom();
        int viewWidth = r.width;
        int viewHeight = r.height;

        int contentWidth = 0;
        int contentHeight = 0;

        if (word.getCurrentRootType() == WPViewConstant.NORMAL_ROOT
                && control.getMainFrame().isZoomAfterLayoutForWord()) {
            if (word.getWidth() == word.getWordWidth()) {
                contentWidth = word.getWidth();
            } else {
                contentWidth = (int) (word.getWordWidth() * zoom);
            }

        } else {
            contentWidth = (int) (word.getWordWidth() * zoom);
        }
        contentHeight = (int) (word.getWordHeight() * zoom);

        int maxX = Math.max(0, contentWidth - viewWidth);
        int maxY = Math.max(0, contentHeight - viewHeight);

        int startX = r.x;
        int startY = r.y;

        if (!mScroller.isFinished()) {
            mScroller.abortAnimation();
        }

        mScroller.fling(startX, startY, velocityX, velocityY, 0, maxX, 0, maxY);

        isFling = true;
        word.postInvalidate();
    }

    /**
     *
     */
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            isFling = true;
            PictureKit.instance().setDrawPictrue(false);

            int sX = mScroller.getCurrX();
            int sY = mScroller.getCurrY();

            word.scrollTo(sX, sY);

            word.postInvalidate();

        } else {
            if (isFling) {
                isFling = false;

                if (!PictureKit.instance().isDrawPictrue()) {
                    PictureKit.instance().setDrawPictrue(true);
                }

                control.actionEvent(EventConstant.APP_GENERATED_PICTURE_ID, null);
                control.actionEvent(EventConstant.SYS_UPDATE_TOOLSBAR_BUTTON_STATUS, null);

                word.postInvalidate();
            }
        }
    }

    /**
     * @return
     */
    protected int convertCoorForX(float x) {
        return (int) ((x + word.getScrollX()) / word.getZoom());
    }

    /**
     * @return
     */
    protected int convertCoorForY(float y) {
        return (int) ((y + word.getScrollY()) / word.getZoom());
    }

    /**
     *
     */
    public void dispose() {
        super.dispose();
        word = null;
    }

    //
    private int oldX;
    //
    private int oldY;
    // word
    protected Word word;
}
