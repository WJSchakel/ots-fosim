package org.opentrafficsim.fosim.parameters.data;

import java.util.List;

/**
 * This class defines data of a single parameter as received from FOSIM.
 * <p>
 * Copyright (c) 2023-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
 */
public class ParameterData
{

    /** Id. */
    public String id;

    /** Values per vehicle type. */
    public List<ValueData> value;

    /** {@inheritDoc} */
    @Override
    public String toString()
    {
        return "ParameterData [id=" + this.id + ", value=" + this.value + "]";
    }

}
