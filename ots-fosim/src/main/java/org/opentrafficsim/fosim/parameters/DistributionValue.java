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
public class DistributionValue implements DefaultValue
{

    /** Distribution type. */
    public final DistributionType type;

    /**
     * Value. This is solely used when a parameter distribution is selected by the user, where the mean/mode/median needs to get
     * a default value. This value is then the default value of the parameter. In case that default value is a distribution
     * (i.e. this distribution), this field is the typical value.
     */
    public final double value;

    /** Names and values of distribution parameters. */
    public final LinkedHashMap<String, Double> values = new LinkedHashMap<>();;

    /**
     * Constructor.
     * @param type Type; distribution type.
     * @param value double; value of the default mean/mode/median value in another distribution should the user select it.
     */
    private DistributionValue(final DistributionType type, final double value)
    {
        this.type = type;
        this.value = value;
    }

    /**
     * Creates an exponential distribution.
     * @param lambda double; mean value.
     * @return DistributionValue; created distribution.
     */
    public static DistributionValue exponential(final double lambda)
    {
        DistributionValue dist = new DistributionValue(DistributionType.Triangular, lambda);
        dist.values.put("lambda", lambda);
        return dist;
    }
    
    /**
     * Creates a triangular distribution.
     * @param min double; minimum value.
     * @param mode double; mode value.
     * @param max double; maximum value.
     * @return DistributionValue; created distribution.
     */
    public static DistributionValue triangular(final double min, final double mode, final double max)
    {
        DistributionValue dist = new DistributionValue(DistributionType.Triangular, mode);
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
     * @return DistributionValue; created distribution.
     */
    public static DistributionValue normal(final double mu, final double sigma, final double min, final double max)
    {
        DistributionValue dist = new DistributionValue(DistributionType.Normal, mu);
        dist.values.put("mu", mu);
        dist.values.put("sigma", sigma);
        dist.values.put("min", min);
        dist.values.put("max", max);
        return dist;
    }

    /**
     * Creates a truncated log-normal distribution.
     * @param mean double; mean value of underlying normal distribution.
     * @param std double; standard deviation of underlying normal distribution.
     * @param min double; minimum value.
     * @param max double; maximum value.
     * @return DistributionValue; created distribution.
     */
    public static DistributionValue logNormal(final double mean, final double std, final double min, final double max)
    {
        DistributionValue dist = new DistributionValue(DistributionType.LogNormal, mean);
        dist.values.put("mu", mean);
        dist.values.put("sigma", std);
        dist.values.put("min", min);
        dist.values.put("max", max);
        return dist;
    }

    /**
     * Creates a uniform distribution.
     * @param min double; minimum value.
     * @param max double; maximum value.
     * @return DistributionValue; created distribution.
     */
    public static DistributionValue uniform(final double min, final double max)
    {
        DistributionValue dist = new DistributionValue(DistributionType.Uniform, 0.5 * (min + max));
        dist.values.put("min", min);
        dist.values.put("max", max);
        return dist;
    }

}
