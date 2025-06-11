package org.opentrafficsim.fosim.parameters.distributions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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

    /** String to represent the distribution. */
    public String notation;
    
    /** Parameters of the distribution. */
    public List<DistributionParameter> parameters;

    /** Domain for which this distribution is valid. */
    public Support support = Support.all;

    /**
     * Constructor using same name for Dutch and English.
     * @param type distribution type
     * @param notation symbol used in notation, e.g. "N" for "N(mu=3, sigma=1)"
     * @param name distribution name
     */
    public Distribution(final DistributionType type, final String notation, final String name)
    {
        this(type, notation, name, name);
    }

    /**
     * Constructor with different names between Dutch and English.
     * @param type distribution type
     * @param notation symbol used in notation, e.g. "N" for "N(mu=3, sigma=1)"
     * @param nameNl Dutch distribution name
     * @param nameEn English distribution name
     */
    public Distribution(final DistributionType type, final String notation, final String nameNl, final String nameEn)
    {
        super(nameNl, nameEn);
        this.type = type;
        this.notation = notation;
    }

    /**
     * Sets the valid domain. By default this is {@code Support.all}.
     * @param support support
     * @return for method chaining
     */
    public Distribution setSupport(final Support support)
    {
        this.support = support;
        return this;
    }

    /**
     * Adds a parameter to the distribution.
     * @param parameter distribution parameter
     */
    public void addParameter(final DistributionParameter parameter)
    {
        if (this.parameters == null)
        {
            this.parameters = new ArrayList<>();
        }
        this.parameters.add(parameter);
    }

    @Override
    public String toString()
    {
        String name = this.notation == null ? this.type.name() : this.notation;
        String sep = "";
        StringBuilder str = new StringBuilder(name).append("(");
        for (DistributionParameter param : this.parameters)
        {
            str.append(sep).append(param.id).append("=").append(param.defaultValue.value == null ? param.defaultValue.parameter
                    : String.format(Locale.US, "%.2f", param.defaultValue.value));
            sep = ", ";
        }
        return str.append(")").toString();
    }

}
