package org.opentrafficsim.fosim.parser;

import java.util.ArrayList;
import java.util.List;

import org.djunits.unit.FrequencyUnit;
import org.djunits.unit.TimeUnit;
import org.djunits.value.storage.StorageType;
import org.djunits.value.vdouble.scalar.Frequency;
import org.djunits.value.vdouble.scalar.Time;
import org.djunits.value.vdouble.vector.FrequencyVector;
import org.djunits.value.vdouble.vector.TimeVector;
import org.djunits.value.vdouble.vector.data.DoubleVectorData;

/**
 * Parsed flow info.
 * <p>
 * Copyright (c) 2023-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
 */
class FosFlow
{
    /** Time. */
    public final List<Time> time = new ArrayList<>();

    /** Flow. */
    public final List<Frequency> flow = new ArrayList<>();

    /**
     * Parses a single flow source.
     * @param string value of a flow line.
     */
    public FosFlow(final String string)
    {
        String[] fields = FosParser.splitStringByBlank(string, 0);
        Frequency lastFrequency = null;
        for (String subString : fields)
        {
            String[] valueStrings = FosParser.splitAndTrimString(subString, "\\|"); // pipe is a meta character in regex
            this.time.add(Time.instantiateSI(Double.parseDouble(valueStrings[0])));
            lastFrequency = new Frequency(Double.parseDouble(valueStrings[1]), FrequencyUnit.PER_HOUR);
            this.flow.add(lastFrequency);
        }
    }

    /** {@inheritDoc} */
    @Override
    public String toString()
    {
        return "FosFlow [time=" + this.time + ", flow=" + this.flow + "]";
    }

    /**
     * Returns time as time vector.
     * @return time vector.
     */
    public TimeVector getTimeVector()
    {
        return new TimeVector(DoubleVectorData.instantiate(this.time.toArray(new Time[this.time.size()]), StorageType.DENSE),
                TimeUnit.BASE_SECOND);
    }

    /**
     * Returns demand as frequency vector.
     * @return frequency vector.
     */
    public FrequencyVector getFrequencyVector()
    {
        return new FrequencyVector(
                DoubleVectorData.instantiate(this.flow.toArray(new Frequency[this.flow.size()]), StorageType.DENSE),
                FrequencyUnit.PER_SECOND);
    }

    /**
     * Sets the end time. This makes sure that the last flow value is constantly maintained towards the end of simulation, as is
     * applicable in FOSIM.
     * @param endTime end time
     */
    public void setEndTime(final Time endTime)
    {
        if (this.time.get(this.time.size() - 1).lt(endTime))
        {
            this.time.add(endTime);
            this.flow.add(this.flow.get(this.flow.size() - 1));
        }
    }
}
