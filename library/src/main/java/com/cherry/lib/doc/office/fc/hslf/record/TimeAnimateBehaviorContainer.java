/*
 * 文件名称:          TimeAnimateBehaviorContainer.java
 *  
 * 编译器:            android2.2
 * 时间:              上午10:56:19
 */
package com.cherry.lib.doc.office.fc.hslf.record;

import androidx.annotation.Keep;

import com.cherry.lib.doc.office.fc.hslf.record.PositionDependentRecordContainer;
import com.cherry.lib.doc.office.fc.hslf.record.Record;

/**
 * TODO: 文件注释
 * <p>
 * <p>
 * Read版本:        Read V1.0
 * <p>
 * 作者:            jqin
 * <p>
 * 日期:            2013-1-7
 * <p>
 * 负责人:           jqin
 * <p>
 * 负责小组:           
 * <p>
 * <p>
 */
public class TimeAnimateBehaviorContainer extends PositionDependentRecordContainer
{
    private byte[] _header;
    @Keep
    public static long RECORD_ID = 0xF12B;
    
    /**
     * We are of type 0xF144
     */
    public long getRecordType()
    {
        return RECORD_ID;
    }
    
    /**
     * Set things up, and find our more interesting children
     */
    protected TimeAnimateBehaviorContainer(byte[] source, int start, int len)
    {
        // Grab the header
        _header = new byte[8];
        System.arraycopy(source, start, _header, 0, 8);

        // Find our children
        _children = Record.findChildRecords(source, start + 8, len - 8);
    }

}
