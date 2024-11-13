package org.opentrafficsim.fosim.parameters.data;

import java.util.List;

import org.opentrafficsim.fosim.parameters.DefaultState;

/**
 * This class defines a parameter data group as received from FOSIM.
 * <p>
 * Copyright (c) 2023-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
 */
public class ParameterDataGroup
{

    /** Id. */
    public String id;
    
    /** Group state. */
    public DefaultState state;
    
    /** List of parameters in the group. */
    public List<ParameterData> parameters;

    /** {@inheritDoc} */
    @Override
    public String toString()
    {
        return "ParameterDataGroup [id=" + this.id + ", parameters=" + this.parameters + "]";
    }
    
}
