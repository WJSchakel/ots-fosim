package org.opentrafficsim.fosim.parameters;

import java.util.ArrayList;
import java.util.List;

/**
 * Group of parameters. This reflects a model component that can also be enabled or disabled.
 * <p>
 * Copyright (c) 2023-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
 */
public class ParameterGroup
{

    /** Id. */
    public String id;

    /** Parent group. When not {@code null}, this group is only relevant with the parent enabled. */
    public String parent;

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
     * @param groupNl Dutch group name.
     * @param groupEn English group name (doubles as id).
     * @param defaultState default state of the group.
     */
    public ParameterGroup(final String groupNl, final String groupEn, final DefaultState defaultState)
    {
        this.id = groupEn;
        this.groupNl = groupNl;
        this.groupEn = groupEn;
        this.defaultState = defaultState;
    }

    /**
     * Set the parent.
     * @param parent parent id.
     * @return for method chaining.
     */
    public ParameterGroup setParent(final String parent)
    {
        this.parent = parent;
        checkIndent();
        return this;
    }

    /**
     * Set Dutch description.
     * @param descriptionNl Dutch description.
     * @return for method chaining.
     */
    public ParameterGroup setDescriptionNl(final String descriptionNl)
    {
        this.descriptionNl = descriptionNl;
        checkIndent();
        return this;
    }

    /**
     * Set English description.
     * @param descriptionEn English description.
     * @return for method chaining.
     */
    public ParameterGroup setDescriptionEn(final String descriptionEn)
    {
        this.descriptionEn = descriptionEn;
        checkIndent();
        return this;
    }

    /**
     * Makes sure that the group names and descriptions start with a tab if this group has a parent, or do not start with a tab
     * when the group does not have a parent.
     */
    private void checkIndent()
    {
        if (this.parent == null)
        {
            if (this.groupNl.startsWith("&#9;"))
            {
                this.groupNl = this.groupNl.substring(4);
            }
            if (this.groupEn.startsWith("&#9;"))
            {
                this.groupEn = this.groupEn.substring(4);
            }
            if (this.descriptionNl != null && this.descriptionNl.startsWith("&#9;"))
            {
                this.descriptionNl = this.descriptionNl.substring(4);
            }
            if (this.descriptionEn != null && this.descriptionEn.startsWith("&#9;"))
            {
                this.descriptionEn = this.descriptionEn.substring(4);
            }
        }
        else
        {
            if (!this.groupNl.startsWith("&#9;"))
            {
                this.groupNl = "&#9;" + this.groupNl;
            }
            if (!this.groupEn.startsWith("&#9;"))
            {
                this.groupEn = "&#9;" + this.groupEn;
            }
            if (this.descriptionNl != null && !this.descriptionNl.startsWith("&#9;"))
            {
                this.descriptionNl = "&#9;" + this.descriptionNl;
            }
            if (this.descriptionEn != null && !this.descriptionEn.startsWith("&#9;"))
            {
                this.descriptionEn = "&#9;" + this.descriptionEn;
            }
        }
    }

    /**
     * Add parameter to the parameter group.
     * @param parameter parameter.
     * @return for method chaining.
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
