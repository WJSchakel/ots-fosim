package org.opentrafficsim.fosim.parameters.distributions;

import org.opentrafficsim.fosim.parameters.Limit;
import org.opentrafficsim.fosim.parameters.MinMax;

/**
 * Information regarding a distribution parameter.
 * <p>
 * Copyright (c) 2023-2023 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://dittlab.tudelft.nl">Wouter Schakel</a>
 */
public class DistributionParameter extends MinMax<DistributionParameter>
{

    /** Parameter id. */
    public String id;
    
    /** Default value. Defined as a {@code Limit} as it contains the right information. */
    public Limit defaultValue;
    
    /**
     * Constructor using same name for Dutch and English.
     * @param id String; parameter id.
     * @param name String; parameter name.
     */
    public DistributionParameter(final String id, final String name)
    {
        super(name);
        this.id = id;
    }
    
    /**
     * Constructor with different names between Dutch and English.
     * @param id String; parameter id.
     * @param nameNl String; Dutch parameter name.
     * @param nameEn String; English parameter name.
     */
    public DistributionParameter(final String id, final String nameNl, final String nameEn)
    {
        super(nameNl, nameEn);
        this.id = id;
    }
    
    /**
     * Set parameter as default value.
     * @param parameter String; parameter.
     * @return T; for method chaining.
     */
    public DistributionParameter setDefault(final String parameter)
    {
        this.defaultValue = new Limit();
        this.defaultValue.parameter = parameter;
        return this;
    }

    /**
     * Set value as default value.
     * @param value double; value.
     * @return T; for method chaining.
     */
    public DistributionParameter setDefault(final double value)
    {
        this.defaultValue = new Limit();
        this.defaultValue.value = value;
        return this;
    }

    /**
     * Set default value as factor on parameter.
     * @param parameter String; parameter.
     * @param factor double; factor.
     * @return T; for method chaining.
     */
    public DistributionParameter setDefault(final String parameter, final double factor)
    {
        this.defaultValue = new Limit();
        this.defaultValue.parameter = parameter;
        this.defaultValue.value = factor;
        return this;
    }

}
