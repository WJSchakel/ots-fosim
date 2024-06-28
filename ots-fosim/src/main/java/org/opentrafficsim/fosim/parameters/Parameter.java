package org.opentrafficsim.fosim.parameters;

import java.util.ArrayList;
import java.util.List;

import org.opentrafficsim.fosim.parameters.distributions.DistributionValue;

/**
 * Contains information on how and what to display for a parameter.
 * <p>
 * Copyright (c) 2023-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
 */
public class Parameter extends MinMax<Parameter>
{

    /** Parameter id. */
    public String id;

    /** Dutch description. */
    public String descriptionNl;

    /** English description. */
    public String descriptionEn;

    /** Unit. */
    public String unit;

    /** List of default values for vehicle-driver classes. */
    public List<DefaultValue> defaultValue;

    /**
     * Constructor using same name for Dutch and English.
     * @param id String; parameter id.
     * @param name String; parameter name.
     * @param unit String; unit.
     */
    public Parameter(final String id, final String name, final String unit)
    {
        super(name);
        this.id = id;
        this.unit = unit;
    }

    /**
     * Constructor with different names between Dutch and English.
     * @param id String; parameter id.
     * @param nameNl String; Dutch parameter name.
     * @param nameEn String; English parameter name.
     * @param unit String; unit.
     */
    public Parameter(final String id, final String nameNl, final String nameEn, final String unit)
    {
        super(nameNl, nameEn);
        this.id = id;
        this.unit = unit;
    }

    /**
     * Set Dutch description.
     * @param descriptionNl String; Dutch description.
     * @return Parameter; for method chaining.
     */
    public Parameter setDescriptionNl(final String descriptionNl)
    {
        this.descriptionNl = descriptionNl;
        return this;
    }

    /**
     * Set English description.
     * @param descriptionEn String; English description.
     * @return Parameter; for method chaining.
     */
    public Parameter setDescriptionEn(final String descriptionEn)
    {
        this.descriptionEn = descriptionEn;
        return this;
    }

    /**
     * Sets default values. Values may be given as {@code Number}, in which case the call to {@code doubleValue()} is wrapped in
     * a {@code Scalar}.
     * @param defaultValues Object...; values of type {@code Number}, {@code Scalar} or {@code Distribution}, in any mixture.
     * @return Parameter; for method chaining.
     * @throws IllegalArgumentException if any of the values is not {@code Number}, {@code Scalar} or {@code Distribution}.
     */
    public Parameter setDefault(final Object... defaultValues)
    {
        this.defaultValue = new ArrayList<>(defaultValues.length);
        for (int i = 0; i < defaultValues.length; i++)
        {
            if (defaultValues[i] instanceof Number)
            {
                this.defaultValue.add(new Scalar(((Number) defaultValues[i]).doubleValue()));
            }
            else if (defaultValues[i] instanceof Scalar)
            {
                this.defaultValue.add((Scalar) defaultValues[i]);
            }
            else if (defaultValues[i] instanceof DistributionValue)
            {
                this.defaultValue.add((DistributionValue) defaultValues[i]);
            }
            else
            {
                throw new IllegalArgumentException("Element at index " + i + " is not a Number, Scalar or Distribution.");
            }
        }
        return this;
    }

}
