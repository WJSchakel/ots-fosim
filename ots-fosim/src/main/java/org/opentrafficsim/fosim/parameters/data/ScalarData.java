package org.opentrafficsim.fosim.parameters.data;

/**
 * Simple wrapper of a double value that implements {@code ValueData} so it can be (de)serialized as a value.
 * <p>
 * Copyright (c) 2023-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
 */
public class ScalarData implements ValueData
{

    /** Value. */
    private final double value;

    /**
     * Constructor.
     * @param value value.
     */
    public ScalarData(final double value)
    {
        this.value = value;
    }

    /**
     * Returns the value.
     * @return value.
     */
    public double value()
    {
        return this.value;
    }

    /** {@inheritDoc} */
    @Override
    public String toString()
    {
        return "ScalarData [value=" + this.value + "]";
    }
    
}
