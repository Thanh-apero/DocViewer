/*
 * 文件名称:          LayoutThread.java
 *  
 * 编译器:            android2.2
 * 时间:              下午9:23:20
 */
package com.cherry.lib.doc.office.wp.view;

import android.util.Log;

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

                                // Try to skip the problematic section
                                ((PageRoot) root).getWPLayouter().forceFinishLayout();

                                // Check progress after skipping
                                long afterSkipOffset = ((PageRoot) root).getWPLayouter().getCurrentLayoutOffset();
                                if (afterSkipOffset > newOffset) {
                                    // Skip was successful, reset the failure counter but track that we skipped
                                    Log.i(TAG, "Successfully skipped from " + newOffset + " to " + afterSkipOffset);

                                    // Log detailed information about what was skipped
                                    PageRoot pageRoot = (PageRoot) root;
                                    int pageCount = pageRoot.getPageCount();
                                    Log.i(TAG, "Current page count after skip: " + pageCount);
                                    if (pageCount > 0) {
                                        PageView lastPage = pageRoot.getPageView(pageCount - 1);
                                        if (lastPage != null) {
                                            Log.i(TAG, "Last page number: " + lastPage.getPageNumber() +
                                                    ", offsets: " + lastPage.getStartOffset(null) + "-" + lastPage.getEndOffset(null));
                                        }
                                    }

                                    failedLayoutAttempts = 0;
                                    totalSkippedSections++;
                                } else {
                                    // Skip was unsuccessful
                                    Log.e(TAG, "Failed to skip problematic section");
                                    totalSkippedSections++;
                                }

                                // If we've skipped too many sections, give up
                                if (totalSkippedSections >= MAX_SKIPS) {
                                    Log.e(TAG, "Too many sections skipped, breaking layout loop");
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
                                    Log.i(TAG, "Successfully skipped from " + beforeSkipOffset + " to " + afterSkipOffset);
                                    failedLayoutAttempts = 0;
                                }
                            }

                            totalSkippedSections++;
                            if (totalSkippedSections >= MAX_SKIPS) {
                                Log.e(TAG, "Too many sections skipped, breaking layout loop");
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
