package org.opentrafficsim.fosim.parameters;

/**
 * Simple wrapper of a double value that implements {@code DefaultValue} so it can be (de)serialized as a default value.
 * <p>
 * Copyright (c) 2023-2023 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://dittlab.tudelft.nl">Wouter Schakel</a>
 */
public class Scalar implements DefaultValue
{
    
    /** Value. */
    private final double value;

    /**
     * Constructor.
     * @param value double; value.
     */
    public Scalar(final double value)
    {
        this.value = value;
    }

    /**
     * Returns the value.
     * @return double; value.
     */
    public double value()
    {
        return this.value;
    }

}
