package org.opentrafficsim.fosim.parameters.distributions;

import java.util.ArrayList;
import java.util.List;

import org.opentrafficsim.fosim.parameters.BiLingual;

/**
 * Description of a distribution applicable on parameters.
 * <p>
 * Copyright (c) 2023-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
 */
public class Distribution extends BiLingual
{

    /** Distribution type. */
    public DistributionType type;

    /** Parameters of the distribution. */
    public List<DistributionParameter> parameters;

    /** Range for which this distribution is valid. */
    public ValidRange validRange = ValidRange.all;

    /**
     * Constructor using same name for Dutch and English.
     * @param type distribution type.
     * @param name distribution name.
     */
    public Distribution(final DistributionType type, final String name)
    {
        super(name);
        this.type = type;
    }

    /**
     * Constructor with different names between Dutch and English.
     * @param type distribution type.
     * @param nameNl Dutch distribution name.
     * @param nameEn English distribution name.
     */
    public Distribution(final DistributionType type, final String nameNl, final String nameEn)
    {
        super(nameNl, nameEn);
        this.type = type;
    }

    /**
     * Sets the valid range. By default this is {@code ValidRange.all}.
     * @param validRange valid range.
     * @return for method chaining.
     */
    public Distribution setValidRange(final ValidRange validRange)
    {
        this.validRange = validRange;
        return this;
    }

    /**
     * Adds a parameter to the distribution.
     * @param parameter distribution parameter.
     */
    public void addParameter(final DistributionParameter parameter)
    {
        if (this.parameters == null)
        {
            this.parameters = new ArrayList<>();
        }
        this.parameters.add(parameter);
    }

}
