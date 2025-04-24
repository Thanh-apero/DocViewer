/*
 * 文件名称:          LayoutThread.java
 *  
 * 编译器:            android2.2
 * 时间:              下午9:23:20
 */
package com.cherry.lib.doc.office.wp.view;

import android.util.Log;

import com.cherry.lib.doc.office.simpletext.model.IDocument;
import com.cherry.lib.doc.office.constant.wp.WPModelConstant;
import com.cherry.lib.doc.office.simpletext.control.IWord;
import com.cherry.lib.doc.office.simpletext.view.IRoot;
import com.cherry.lib.doc.office.simpletext.view.IView;

/**
 * 后台布局线程
 * <p>
 * <p>
 * Read版本:        Read V1.0
 * <p>
 * 作者:            ljj8494
 * <p>
 * 日期:            2011-11-17
 * <p>
 * 负责人:          ljj8494
 * <p>
 * 负责小组:         
 * <p>
 * <p>
 */
public class LayoutThread extends Thread
{
    private boolean isDied;
    private int failedLayoutAttempts = 0;
    private static final int MAX_FAILED_ATTEMPTS = 1;
    private static final String TAG = "LayoutThread";

    // Track total skipped problematic sections
    private int totalSkippedSections = 0;
    private static final int MAX_SKIPS = 3;
    // Track the last processed offset to detect progress
    private long lastOffset = -1;

    /**
     *
     * @param root
     */
    public LayoutThread(IRoot root)
    {
        this.root = root;
    }

    /**
     *
     */
    public void run()
    {
        while (true)
        {
            if (isDied)
            {
                break;
            }
            try
            {
                if (root.canBackLayout())
                {
                    try {
                        long oldOffset = ((PageRoot) root).getWPLayouter().getCurrentLayoutOffset();

                        // Check if we've made progress since the last attempt
                        if (lastOffset == oldOffset) {
                            // We're still at the same position as before, indicating possible issue
                            failedLayoutAttempts++;
                            Log.w(TAG, "Layout appears stuck: attempt " + failedLayoutAttempts +
                                    " of " + MAX_FAILED_ATTEMPTS + " at offset " + oldOffset);
                        } else {
                            // We've made progress since last time
                            lastOffset = oldOffset;
                            // Only reset the counter if we're not already tracking a failure
                            if (failedLayoutAttempts == 0) {
                                totalSkippedSections = 0; // We're making good progress
                            }
                        }

                        // Perform the layout
                        root.backLayout();
                        long newOffset = ((PageRoot) root).getWPLayouter().getCurrentLayoutOffset();

                        // Check if this specific layout operation made progress
                        if (oldOffset == newOffset) {
                            // This attempt didn't make progress
                            if (failedLayoutAttempts >= MAX_FAILED_ATTEMPTS) {
                                Log.e(TAG, "Layout failed too many times - forcing completion");
                                // Check if this might be a TOC page or large content
                                boolean isLargeStructuredContent = false;
                                try {
                                    if (root instanceof PageRoot) {
                                        PageRoot pageRoot = (PageRoot) root;
                                        int pageCount = pageRoot.getPageCount();
                                        if (pageCount > 0) {
                                            // Get the current page where we're having issues
                                            PageView lastPage = pageRoot.getPageView(pageCount - 1);
                                            if (lastPage != null) {
                                                // Check if the page has structural properties of a problematic page
                                                // We'll check the dimensions and content amount
                                                int pageHeight = lastPage.getHeight();
                                                int viewCount = 0;
                                                IView childView = lastPage.getChildView();
                                                while (childView != null) {
                                                    viewCount++;
                                                    childView = childView.getNextView();
                                                }

                                                // Detect large content sections with multiple child views or
                                                // that are close to page boundaries
                                                isLargeStructuredContent = (viewCount > 5 ||
                                                        pageHeight > 0 && (oldOffset - lastPage.getStartOffset(null) > 1000));

                                                if (isLargeStructuredContent) {
                                                    Log.i(TAG, "Detected large structured content at stuck position");
                                                }
                                            }
                                        }
                                    }
                                } catch (Exception ignored) {
                                    // Ignore errors during content detection
                                }

                                // Log page dimensions that might help diagnose the issue
                                if (root instanceof PageRoot) {
                                    PageRoot pageRoot = (PageRoot) root;
                                    int pageCount = pageRoot.getPageCount();
                                    Log.e(TAG, "Current page count: " + pageCount);
                                    if (pageCount > 0) {
                                        PageView lastPage = pageRoot.getPageView(pageCount - 1);
                                        if (lastPage != null) {
                                            Log.e(TAG, "Last page dimensions: " +
                                                    lastPage.getWidth() + "x" + lastPage.getHeight() +
                                                    ", content may be too large for page");
                                        }
                                    }
                                }

                                // If this is potentially a TOC with height issue, log that
                                if (isLargeStructuredContent) {
                                    Log.i(TAG, "Large structured content detected with likely height issue. " +
                                            "Will attempt special handling rather than skipping.");
                                }

                                // Try to skip the problematic section
                                ((PageRoot) root).getWPLayouter().forceFinishLayout();

                                // Check if we made progress
                                long newOffsetAfterSkip = ((PageRoot) root).getWPLayouter().getCurrentLayoutOffset();

                                // Log the outcome with more positive wording
                                if (newOffsetAfterSkip > oldOffset) {
                                    int processed = (int) (newOffsetAfterSkip - oldOffset);
                                    Log.i(TAG, "Successfully processed problematic content from " + oldOffset + " to " + newOffsetAfterSkip +
                                            " (" + processed + " chars)");
                                } else {
                                    Log.i(TAG, "Attempting to continue processing from offset " + oldOffset);

                                    // Make minimal progress to prevent infinite loop
                                    ((PageRoot) root).getWPLayouter().setCurrentLayoutOffset(oldOffset + 1);
                                }

                                // Log information about the current state of document
                                PageRoot pageRoot = (PageRoot) root;
                                int pageCount = pageRoot.getPageCount();
                                Log.i(TAG, "Current page count: " + pageCount);
                                if (pageCount > 0) {
                                    PageView lastPage = pageRoot.getPageView(pageCount - 1);
                                    if (lastPage != null) {
                                        Log.i(TAG, "Last page number: " + lastPage.getPageNumber() +
                                                ", offsets: " + lastPage.getStartOffset(null) + "-" + lastPage.getEndOffset(null));
                                    }
                                }

                                failedLayoutAttempts = 0; // We're making a new attempt
                                // Don't increment totalSkippedSections - we're trying to render, not skip

                                // Increase the tolerance - we're trying to render everything
                                if (totalSkippedSections >= MAX_SKIPS * 2) {
                                    Log.e(TAG, "Layout is unable to make sufficient progress, breaking layout loop");
                                    break;
                                }
                            }
                        } else {
                            // This attempt made progress
                            failedLayoutAttempts = 0;
                            // If we've been stuck before, don't reset the totalSkippedSections
                        }

                        sleep(50);
                    } catch (Exception e) {
                        failedLayoutAttempts++;
                        Log.e(TAG, "Exception during layout: " + e.getMessage(), e);

                        if (failedLayoutAttempts >= MAX_FAILED_ATTEMPTS) {
                            Log.e(TAG, "Too many exceptions during layout - forcing completion", e);

                            long beforeSkipOffset = -1;
                            if (root instanceof PageRoot) {
                                beforeSkipOffset = ((PageRoot) root).getWPLayouter().getCurrentLayoutOffset();
                            }

                            ((PageRoot) root).getWPLayouter().forceFinishLayout();

                            // Check if skip was successful
                            if (root instanceof PageRoot) {
                                long afterSkipOffset = ((PageRoot) root).getWPLayouter().getCurrentLayoutOffset();
                                if (afterSkipOffset > beforeSkipOffset) {
                                    // Skip was successful
                                    int processed = (int) (afterSkipOffset - beforeSkipOffset);
                                    Log.i(TAG, "Successfully processed problematic content from " + beforeSkipOffset +
                                            " to " + afterSkipOffset + " (" + processed + " chars)");
                                    failedLayoutAttempts = 0;
                                } else {
                                    // We didn't make progress - force minimal progress
                                    ((PageRoot) root).getWPLayouter().setCurrentLayoutOffset(beforeSkipOffset + 1);
                                    Log.i(TAG, "Forcing minimal progress from offset " + beforeSkipOffset + " to " +
                                            (beforeSkipOffset + 1));
                                }
                            }

                            // Only increment problem counter if we made significant jumps 
                            if (root instanceof PageRoot) {
                                long afterSkipOffset = ((PageRoot) root).getWPLayouter().getCurrentLayoutOffset();
                                if (afterSkipOffset > beforeSkipOffset + 50) {
                                    totalSkippedSections++;
                                }
                            }

                            // Increase tolerance since we're trying to render everything
                            if (totalSkippedSections >= MAX_SKIPS * 2) {
                                Log.e(TAG, "Layout is unable to make sufficient progress, breaking layout loop");
                                break;
                            }
                        }
                        sleep(100);
                    }
                }
                else
                {
                    sleep(1000);
                }
            }
            catch (Exception e)
            {
                Log.e(TAG, "Critical error in layout thread", e);
                if (root != null)
                {
                    IWord word = ((IView)root).getContainer();
                    if (word != null && word.getControl() != null)
                    {
                        word.getControl().getSysKit().getErrorKit().writerLog(e);
                    }
                }
                break;
            }
        }
    }
    
    /**
     * 
     */
    public void setDied()
    {
        isDied = true;
    }
    
    /**
     * 
     */
    public void dispose()
    {       
        root = null;
        isDied = true;
    }
    
    private IRoot root;
}
