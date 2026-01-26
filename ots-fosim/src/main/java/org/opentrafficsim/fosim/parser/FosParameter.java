package org.opentrafficsim.fosim.parser;

/**
 * Parsed parameter info. This class contains parameters as defined in the .fos file.
 * <p>
 * Copyright (c) 2023-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
 */
class FosParameter
{
    /** Value. */
    public final double value;

    /** Name. */
    public final String name;

    /**
     * Parses a single parameter.
     * @param string value of a parameter line.
     */
    public FosParameter(final String string)
    {
        String[] fields = FosParser.splitStringByBlank(string, 2);
        this.value = Double.parseDouble(fields[0]);
        this.name = fields[1];
    }

    @Override
    public String toString()
    {
        return "FosParameter " + this.name + " = " + this.value + "]";
    }
}