package org.opentrafficsim.fosim.parameters;

import java.util.ArrayList;
import java.util.List;

/**
 * Group of parameters. This reflects a model component that can also be enabled or disabled. 
 * <p>
 * Copyright (c) 2023-2023 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://dittlab.tudelft.nl">Wouter Schakel</a>
 */
public class ParameterGroup
{

    /** Dutch group name. */
    public String groupNl;

    /** English group name. */
    public String groupEn;

    /** Default state. */
    public DefaultState defaultState;

    /** Dutch description. */
    public String descriptionNl;

    /** English description. */
    public String descriptionEn;

    /** List of parameters in the group. */
    public List<Parameter> parameters;
    
    /**
     * Constructor.
     * @param groupNl String; Dutch group name.
     * @param groupEn String; English group name.
     * @param defaultState DefaultState; default state of the group.
     */
    public ParameterGroup(final String groupNl, final String groupEn, final DefaultState defaultState)
    {
        this.groupNl = groupNl;
        this.groupEn = groupEn;
        this.defaultState = defaultState;
    }
    
    /**
     * Set Dutch description.
     * @param descriptionNl String; Dutch description.
     * @return ParameterGroup; for method chaining.
     */
    public ParameterGroup setDescriptionNl(final String descriptionNl)
    {
        this.descriptionNl = descriptionNl;
        return this;
    }
    
    /**
     * Set English description.
     * @param descriptionEn String; English description.
     * @return ParameterGroup; for method chaining.
     */
    public ParameterGroup setDescriptionEn(final String descriptionEn)
    {
        this.descriptionEn = descriptionEn;
        return this;
    }
    
    /**
     * Add parameter to the parameter group.
     * @param parameter Parameter; parameter.
     * @return ParameterGroup; for method chaining.
     */
    public ParameterGroup addParameter(final Parameter parameter)
    {
        if (this.parameters == null)
        {
            this.parameters = new ArrayList<>();
        }
        this.parameters.add(parameter);
        return this;
    }

}
