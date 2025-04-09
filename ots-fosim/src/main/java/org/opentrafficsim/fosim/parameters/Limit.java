package org.opentrafficsim.fosim.parameters;

/**
 * Stores parameter name and/or a value to represent the lower or upper limit for a parameter value. In case both are given
 * the value is a factor on the parameters value.
 * <p>
 * Copyright (c) 2023-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved.
 * <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
 */
public class Limit
{

    /** Parameter id. */
    public String parameter;

    /** Value. */
    public Double value;
    
}