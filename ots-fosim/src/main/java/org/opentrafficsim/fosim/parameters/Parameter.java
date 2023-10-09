package org.opentrafficsim.fosim.parameters;

import java.util.ArrayList;
import java.util.List;

/**
 * Contains information on how and what to display for a parameter.
 * <p>
 * Copyright (c) 2023-2023 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://dittlab.tudelft.nl">Wouter Schakel</a>
 */
public class Parameter
{

    /** Parameter id. */
    public String id;

    /** Dutch parameter name. */
    public String nameNl;

    /** English parameter name. */
    public String nameEn;

    /** Dutch description. */
    public String descriptionNl;

    /** English description. */
    public String descriptionEn;

    /** Unit. */
    public String unit;

    /** Minimum parameter value. */
    public Limit minimum;

    /** Maximum parameter value. */
    public Limit maximum;

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
        this.id = id;
        this.nameNl = name;
        this.nameEn = name;
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
        this.id = id;
        this.nameNl = nameNl;
        this.nameEn = nameEn;
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
     * Set parameter as minimum limit.
     * @param parameter String; parameter.
     * @return Parameter; for method chaining.
     */
    public Parameter setMin(final String parameter)
    {
        this.minimum = new Limit();
        this.minimum.parameter = parameter;
        return this;
    }

    /**
     * Set value as minimum limit.
     * @param value double; value.
     * @return Parameter; for method chaining.
     */
    public Parameter setMin(final double value)
    {
        this.minimum = new Limit();
        this.minimum.value = value;
        return this;
    }

    /**
     * Set minimum limit as factor on parameter.
     * @param parameter String; parameter.
     * @param factor double; factor.
     * @return Parameter; for method chaining.
     */
    public Parameter setMin(final String parameter, final double factor)
    {
        this.minimum = new Limit();
        this.minimum.parameter = parameter;
        this.minimum.value = factor;
        return this;
    }

    /**
     * Set parameter as maximum limit.
     * @param parameter String; parameter.
     * @return Parameter; for method chaining.
     */
    public Parameter setMax(final String parameter)
    {
        this.maximum = new Limit();
        this.maximum.parameter = parameter;
        return this;
    }

    /**
     * Set value as maximum limit.
     * @param value double; value.
     * @return Parameter; for method chaining.
     */
    public Parameter setMax(final double value)
    {
        this.maximum = new Limit();
        this.maximum.value = value;
        return this;
    }

    /**
     * Set maximum limit as factor on parameter.
     * @param parameter String; parameter.
     * @param factor double; factor.
     * @return Parameter; for method chaining.
     */
    public Parameter setMax(final String parameter, final double factor)
    {
        this.maximum = new Limit();
        this.maximum.parameter = parameter;
        this.maximum.value = factor;
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
            else if (defaultValues[i] instanceof Distribution)
            {
                this.defaultValue.add((Distribution) defaultValues[i]);
            }
            else
            {
                throw new IllegalArgumentException("Element at index " + i + " is not a Number, Scalar or Distribution.");
            }
        }
        return this;
    }

    /**
     * Stores parameter name and/or a value to represent the lower or upper limit for a parameter value. In case both are given
     * the value is a factor on the parameters value.
     * <p>
     * Copyright (c) 2023-2023 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved.
     * <br>
     * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
     * </p>
     * @author <a href="https://dittlab.tudelft.nl">Wouter Schakel</a>
     */
    public class Limit
    {

        /** Parameter. */
        public String parameter;

        /** Value. */
        public Double value;

    }

}
