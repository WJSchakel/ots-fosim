package org.opentrafficsim.fosim.parser;

import org.djunits.unit.FrequencyUnit;
import org.djunits.value.vdouble.scalar.Duration;
import org.djunits.value.vdouble.scalar.Frequency;
import org.djunits.value.vdouble.scalar.Speed;

/**
 * Parsed switched area info.
 * <p>
 * Copyright (c) 2023-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
 */
class FosSwitchedArea
{
    /** Open time. */
    public final Duration openTime;

    /** Close time. */
    public final Duration closeTime;

    /** Open speed (unit unknown, not used in FOSIM). */
    public final Speed openSpeed;

    /** Close speed (unit unknown, not used in FOSIM). */
    public final Speed closeSpeed;

    /** Open intensity (unit unknown, not used in FOSIM). */
    public final Frequency openIntensity;

    /** Open intensity (unit unknown, not used in FOSIM). */
    public final Frequency closeIntensity;

    /** Open mode (function unknown, not used in FOSIM). */
    public final int openMode;

    /** Close mode (function unknown, not used in FOSIM). */
    public final int closeMode;

    /** Detector index (not used in FOSIM). */
    public final int detectorIndex;

    /**
     * Parses a single switch area.
     * @param string value of a switch area line.
     */
    public FosSwitchedArea(final String string)
    {
        String[] fields = FosParser.splitStringByBlank(string, 9);
        this.openTime = Duration.ofSI(Double.parseDouble(fields[0]));
        this.closeTime = Duration.ofSI(Double.parseDouble(fields[1]));
        this.openSpeed = Speed.ofSI(Double.parseDouble(fields[2])); // unit unknown
        this.closeSpeed = Speed.ofSI(Double.parseDouble(fields[3])); // unit unknown
        this.openIntensity = new Frequency(Double.parseDouble(fields[4]), FrequencyUnit.PER_HOUR); // unit unknown
        this.closeIntensity = new Frequency(Double.parseDouble(fields[5]), FrequencyUnit.PER_HOUR); // unit unknown
        this.openMode = Integer.parseInt(fields[6]); // function unknown
        this.closeMode = Integer.parseInt(fields[7]); // function unknown
        this.detectorIndex = Integer.parseInt(fields[8]);
    }

    @Override
    public String toString()
    {
        return "FosSwitchedArea [openTime=" + this.openTime + ", closeTime=" + this.closeTime + "]";
    }
}