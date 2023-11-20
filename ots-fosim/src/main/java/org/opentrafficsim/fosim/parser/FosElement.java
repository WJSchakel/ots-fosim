package org.opentrafficsim.fosim.parser;

import java.util.Objects;

/**
 * Class for common functionality of both FOSIM links and nodes.
 * <p>
 * Copyright (c) 2023-2023 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://dittlab.tudelft.nl">Wouter Schakel</a>
 */
abstract class FosElement
{
    /** Unique number. */
    final public int number;

    /**
     * Constructor.
     * @param number int; unique number.
     */
    public FosElement(final int number)
    {
        this.number = number;
    }

    /** {@inheritDoc} */
    @Override
    public final int hashCode()
    {
        return Objects.hash(this.number);
    }

    /** {@inheritDoc} */
    @Override
    public final boolean equals(final Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        FosElement other = (FosElement) obj;
        return this.number == other.number;
    }

    /** {@inheritDoc} */
    @Override
    public final String toString()
    {
        return getClass().getSimpleName() + " " + this.number;
    }
}