package org.opentrafficsim.fosim.sim0mq.trace;

import java.util.ArrayList;
import java.util.List;

import org.djunits.value.vfloat.scalar.FloatAcceleration;
import org.djunits.value.vfloat.scalar.FloatDuration;
import org.djunits.value.vfloat.scalar.FloatLength;
import org.djunits.value.vfloat.scalar.FloatSpeed;
import org.djunits.value.vfloat.vector.FloatAccelerationVector;
import org.djunits.value.vfloat.vector.FloatDurationVector;
import org.djunits.value.vfloat.vector.FloatLengthVector;
import org.djunits.value.vfloat.vector.FloatSpeedVector;

/**
 * Trace data contained.
 * @author wjschakel
 */
public class TraceData
{

    /** Column list. */
    private List<Object>[] data;

    /**
     * Constructor.
     * @param numberOfColumns number of columns
     */
    @SuppressWarnings("unchecked")
    public TraceData(final int numberOfColumns)
    {
        List<List<Object>> list = new ArrayList<>();
        for (int i = 0; i < numberOfColumns; i++)
        {
            list.add(new ArrayList<>());
        }
        this.data = list.toArray(new List[numberOfColumns]);
    }

    /**
     * Append data. Objects must be in types of columns.
     * @param row data to append
     */
    public void append(final Object... row)
    {
        for (int i = 0; i < row.length; i++)
        {
            this.data[i].add(row[i]);
        }
    }

    /**
     * Return column as integer array.
     * @param column column number
     * @return column as integer array
     */
    public Integer[] asInteger(final int column)
    {
        return this.data[column].toArray(new Integer[this.data[column].size()]);
    }

    /**
     * Return column as String array.
     * @param column column number
     * @return column as String array
     */
    public String[] asString(final int column)
    {
        return this.data[column].toArray(new String[this.data[column].size()]);
    }

    /**
     * Return column as FloatLengthVector.
     * @param column column number
     * @return column as FloatLengthVector
     */
    public FloatLengthVector asLength(final int column)
    {
        return new FloatLengthVector(this.data[column].toArray(new FloatLength[this.data[column].size()]));
    }

    /**
     * Return column as FloatDurationVector.
     * @param column column number
     * @return column as FloatDurationVector
     */
    public FloatDurationVector asDuration(final int column)
    {
        return new FloatDurationVector(this.data[column].toArray(new FloatDuration[this.data[column].size()]));
    }

    /**
     * Return column as FloatSpeedVector.
     * @param column column number
     * @return column as FloatSpeedVector
     */
    public FloatSpeedVector asSpeed(final int column)
    {
        return new FloatSpeedVector(this.data[column].toArray(new FloatSpeed[this.data[column].size()]));
    }

    /**
     * Return column as FloatAccelerationVector.
     * @param column column number
     * @return column as FloatAccelerationVector
     */
    public FloatAccelerationVector asAcceleration(final int column)
    {
        return new FloatAccelerationVector(this.data[column].toArray(new FloatAcceleration[this.data[column].size()]));
    }

    /**
     * Clear all data.
     */
    public void clear()
    {
        for (List<Object> list : this.data)
        {
            list.clear();
        }
    }

}
