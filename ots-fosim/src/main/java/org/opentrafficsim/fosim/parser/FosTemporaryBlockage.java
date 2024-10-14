package org.opentrafficsim.fosim.parser;

import org.djunits.value.vdouble.scalar.Duration;
import org.djunits.value.vdouble.scalar.Length;

/**
 * Parsed temporary blockage info.
 * <p>
 * Copyright (c) 2023-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
 */
class FosTemporaryBlockage
{
    /** Position. */
    public final Length position;

    /** From lane index. */
    public final int fromLane;

    /** To lane index. */
    public final int toLane;

    /** From time. */
    public final Duration fromTime;

    /** To time. */
    public final Duration toTime;

    /**
     * Parses a single switch area.
     * @param string value of a switch area line.
     */
    public FosTemporaryBlockage(final String string)
    {
        String[] fields = FosParser.splitStringByBlank(string, 5);
        this.position = Length.instantiateSI(Double.parseDouble(fields[0]));
        this.fromLane = Integer.parseInt(fields[1]);
        this.toLane = Integer.parseInt(fields[2]);
        this.fromTime = Duration.instantiateSI(Double.parseDouble(fields[3]));
        this.toTime = Duration.instantiateSI(Double.parseDouble(fields[4]));
    }

    /** {@inheritDoc} */
    @Override
    public String toString()
    {
        return "FosTemporaryBlockage [position=" + this.position + ", fromLane=" + this.fromLane + ", toLane=" + this.toLane
                + ", fromTime=" + this.fromTime + ", toTime=" + this.toTime + "]";
    }
}