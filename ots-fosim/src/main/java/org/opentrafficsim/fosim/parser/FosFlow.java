package org.opentrafficsim.fosim.parser;

import java.util.ArrayList;
import java.util.List;

import org.djunits.unit.FrequencyUnit;
import org.djunits.value.vdouble.scalar.Duration;
import org.djunits.value.vdouble.scalar.Frequency;

/**
 * Parsed flow info.
 * <p>
 * Copyright (c) 2023-2023 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://dittlab.tudelft.nl">Wouter Schakel</a>
 */
class FosFlow
{
    /** Time. */
    public final List<Duration> time = new ArrayList<>();

    /** Flow. */
    public final List<Frequency> flow = new ArrayList<>();

    /**
     * Parses a single flow source.
     * @param string String; value of a flow line.
     */
    public FosFlow(final String string)
    {
        String[] fields = FosParser.splitStringByBlank(string, 0);
        for (String subString : fields)
        {
            String[] valueStrings = FosParser.splitAndTrimString(subString, "\\|"); // pipe is a meta character in regex
            this.time.add(Duration.instantiateSI(Double.parseDouble(valueStrings[0])));
            this.flow.add(new Frequency(Double.parseDouble(valueStrings[1]), FrequencyUnit.PER_HOUR));
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString()
    {
        return "FosFlow [time=" + this.time + ", flow=" + this.flow + "]";
    }
}