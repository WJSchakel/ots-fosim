package org.opentrafficsim.fosim.parameters;

/**
 * Class that defines parameter limits. 
 * <p>
 * Copyright (c) 2023-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
 * @param <T> type of object itself
 */
public class MinMax<T extends MinMax<T>> extends BiLingual
{

    /** Minimum parameter value. */
    public Limit minimum;

    /** Maximum parameter value. */
    public Limit maximum;
    
    /**
     * Constructor using same name for Dutch and English.
     * @param name parameter name.
     */
    public MinMax(final String name)
    {
        super(name);
    }
    
    /**
     * Constructor with different names between Dutch and English.
     * @param nameNl Dutch parameter name.
     * @param nameEn English parameter name.
     */
    public MinMax(final String nameNl, final String nameEn)
    {
        super(nameNl, nameEn);
    }
    
    /**
     * Set parameter as minimum limit.
     * @param parameter parameter.
     * @return for method chaining.
     */
    @SuppressWarnings("unchecked")
    public T setMin(final String parameter)
    {
        this.minimum = new Limit();
        this.minimum.parameter = parameter;
        return (T) this;
    }

    /**
     * Set value as minimum limit.
     * @param value value.
     * @return for method chaining.
     */
    @SuppressWarnings("unchecked")
    public T setMin(final double value)
    {
        this.minimum = new Limit();
        this.minimum.value = value;
        return (T) this;
    }

    /**
     * Set minimum limit as factor on parameter.
     * @param parameter parameter.
     * @param factor factor.
     * @return for method chaining.
     */
    @SuppressWarnings("unchecked")
    public T setMin(final String parameter, final double factor)
    {
        this.minimum = new Limit();
        this.minimum.parameter = parameter;
        this.minimum.value = factor;
        return (T) this;
    }

    /**
     * Set parameter as maximum limit.
     * @param parameter parameter.
     * @return for method chaining.
     */
    @SuppressWarnings("unchecked")
    public T setMax(final String parameter)
    {
        this.maximum = new Limit();
        this.maximum.parameter = parameter;
        return (T) this;
    }

    /**
     * Set value as maximum limit.
     * @param value value.
     * @return for method chaining.
     */
    @SuppressWarnings("unchecked")
    public T setMax(final double value)
    {
        this.maximum = new Limit();
        this.maximum.value = value;
        return (T) this;
    }

    /**
     * Set maximum limit as factor on parameter.
     * @param parameter parameter.
     * @param factor factor.
     * @return for method chaining.
     */
    @SuppressWarnings("unchecked")
    public T setMax(final String parameter, final double factor)
    {
        this.maximum = new Limit();
        this.maximum.parameter = parameter;
        this.maximum.value = factor;
        return (T) this;
    }
    
}
