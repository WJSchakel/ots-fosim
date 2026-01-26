package org.opentrafficsim.fosim.parser;

import org.djunits.value.vdouble.scalar.Duration;
import org.djunits.value.vdouble.scalar.Length;

/**
 * Parsed traffic light info.
 * <p>
 * Copyright (c) 2023-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
 */
class FosTrafficLight
{
    /** Controller (function unknown, not used in FOSIM). */
    public final String controller;

    /** Position. */
    public final Length position;

    /** Lane. */
    public final int lane;

    /** Number (function unknown, not used in FOSIM). */
    public final int number;

    /** Cycle time. */
    public final Duration cycleTime;

    /** Green time. */
    public final Duration greenTime;

    /** Yellow time. */
    public final Duration yellowTime;

    /** Start offset. */
    public final Duration startOffset;

    /**
     * Parses a single traffic light.
     * @param string value of a traffic light line.
     */
    public FosTrafficLight(final String string)
    {
        String[] fields = FosParser.splitStringByBlank(string, 8);
        this.controller = fields[0];
        this.position = Length.ofSI(Double.parseDouble(fields[1]));
        this.lane = Integer.parseInt(fields[2]);
        this.number = Integer.parseInt(fields[3]);
        this.cycleTime = Duration.ofSI(Double.parseDouble(fields[4]));
        this.greenTime = Duration.ofSI(Double.parseDouble(fields[5]));
        this.yellowTime = Duration.ofSI(Double.parseDouble(fields[6]));
        this.startOffset = Duration.ofSI(Double.parseDouble(fields[7]));
    }

    @Override
    public String toString()
    {
        return "FosTrafficLight [position=" + this.position + ", lane=" + this.lane + ", cycleTime=" + this.cycleTime
                + ", greenTime=" + this.greenTime + ", yellowTime=" + this.yellowTime + ", startOffset=" + this.startOffset
                + "]";
    }
}