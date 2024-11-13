package org.opentrafficsim.fosim.parameters.distributions;

import org.opentrafficsim.fosim.parameters.Limit;
import org.opentrafficsim.fosim.parameters.MinMax;

/**
 * Information regarding a distribution parameter, i.e. a parameter of the distribution such as min, mu, mode, max, sigma, etc.
 * <p>
 * Copyright (c) 2023-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
 */
@Deprecated
public class DistributionParameter extends MinMax<DistributionParameter>
{

    /** Parameter id. */
    public String id;
    
    /** Default value. Defined as a {@code Limit} as it contains the right information. */
    public Limit defaultValue;
    
    /**
     * Constructor using same name for Dutch and English.
     * @param id parameter id.
     * @param name parameter name.
     */
    public DistributionParameter(final String id, final String name)
    {
        super(name);
        this.id = id;
    }
    
    /**
     * Constructor with different names between Dutch and English.
     * @param id parameter id.
     * @param nameNl Dutch parameter name.
     * @param nameEn English parameter name.
     */
    public DistributionParameter(final String id, final String nameNl, final String nameEn)
    {
        super(nameNl, nameEn);
        this.id = id;
    }
    
    /**
     * Set parameter as default value.
     * @param parameter parameter.
     * @return for method chaining.
     */
    public DistributionParameter setDefault(final String parameter)
    {
        this.defaultValue = new Limit();
        this.defaultValue.parameter = parameter;
        return this;
    }

    /**
     * Set value as default value.
     * @param value value.
     * @return for method chaining.
     */
    public DistributionParameter setDefault(final double value)
    {
        this.defaultValue = new Limit();
        this.defaultValue.value = value;
        return this;
    }

    /**
     * Set default value as factor on parameter.
     * @param parameter parameter.
     * @param factor factor.
     * @return for method chaining.
     */
    public DistributionParameter setDefault(final String parameter, final double factor)
    {
        this.defaultValue = new Limit();
        this.defaultValue.parameter = parameter;
        this.defaultValue.value = factor;
        return this;
    }

}
