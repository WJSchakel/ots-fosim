package org.opentrafficsim.fosim.parameters;

import java.util.LinkedHashMap;

/**
 * Stores information about a distributed parameter. Implements {@code DefaultValue} so it can be (de)serialized as a default
 * value.
 * <p>
 * Copyright (c) 2023-2023 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://dittlab.tudelft.nl">Wouter Schakel</a>
 */
public class Distribution implements DefaultValue
{

    /** Distribution type. */
    public final Type distribution;

    /** Names and values of distribution parameters. */
    public final LinkedHashMap<String, Double> values = new LinkedHashMap<>();;

    /**
     * Constructor.
     * @param distribution Type; distribution type.
     */
    public Distribution(final Type distribution)
    {
        this.distribution = distribution;
    }

    /**
     * Creates a triangular distribution.
     * @param min double; minimum value.
     * @param mode double; mode value.
     * @param max double; maximum value.
     * @return Distribution; created distribution.
     */
    public static Distribution triangular(final double min, final double mode, final double max)
    {
        Distribution dist = new Distribution(Type.Triangular);
        dist.values.put("min", min);
        dist.values.put("mode", mode);
        dist.values.put("max", max);
        return dist;
    }

    /**
     * Creates a truncated normal distribution.
     * @param mu double; mean value.
     * @param sigma double; standard deviation.
     * @param min double; minimum value.
     * @param max double; maximum value.
     * @return Distribution; created distribution.
     */
    public static Distribution normal(final double mu, final double sigma, final double min, final double max)
    {
        Distribution dist = new Distribution(Type.Normal);
        dist.values.put("mu", mu);
        dist.values.put("sigma", sigma);
        dist.values.put("min", min);
        dist.values.put("max", max);
        return dist;
    }

    /**
     * Creates a truncated log-normal distribution.
     * @param mu double; mean value of underlying normal distribution.
     * @param sigma double; standard deviation of underlying normal distribution.
     * @param min double; minimum value.
     * @param max double; maximum value.
     * @return Distribution; created distribution.
     */
    public static Distribution logNormal(final double mu, final double sigma, final double min, final double max)
    {
        Distribution dist = new Distribution(Type.LogNormal);
        dist.values.put("mu", mu);
        dist.values.put("sigma", sigma);
        dist.values.put("min", min);
        dist.values.put("max", max);
        return dist;
    }

    /**
     * Creates a uniform distribution.
     * @param min double; minimum value.
     * @param max double; maximum value.
     * @return Distribution; created distribution.
     */
    public static Distribution uniform(final double min, final double max)
    {
        Distribution dist = new Distribution(Type.Uniform);
        dist.values.put("min", min);
        dist.values.put("max", max);
        return dist;
    }

    /**
     * Distribution type. 
     * <p>
     * Copyright (c) 2023-2023 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
     * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
     * </p>
     * @author <a href="https://dittlab.tudelft.nl">Wouter Schakel</a>
     */
    public enum Type
    {
        /** Triangular. */
        Triangular,

        /** Normal (truncated). */
        Normal,

        /** Log-normal (truncated). */
        LogNormal,

        /** Uniform. */
        Uniform;
    }

}
