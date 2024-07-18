package org.opentrafficsim.fosim.parameters.data;

import java.util.Map;

import org.opentrafficsim.fosim.parameters.distributions.DistributionType;

/**
 * Stores information about a distributed parameter. Implements {@code ValueData} so it can be (de)serialized as a value.
 * <p>
 * Copyright (c) 2023-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
 */
public class DistributionData implements ValueData
{

    /** Distribution type. */
    public DistributionType type;
    
    /** List of parameters that define the distribution. */
    public Map<String, Double> distributionParameters;

    /** {@inheritDoc} */
    @Override
    public String toString()
    {
        return "DistributionData [type=" + type + ", distributionParameters=" + distributionParameters + "]";
    }
    
}
