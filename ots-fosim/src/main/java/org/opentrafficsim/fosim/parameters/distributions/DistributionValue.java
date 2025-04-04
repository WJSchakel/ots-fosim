package org.opentrafficsim.fosim.parameters.distributions;

import java.util.LinkedHashMap;

import org.opentrafficsim.fosim.parameters.DefaultValue;

/**
 * Stores information about a distributed parameter. Implements {@code DefaultValue} so it can be (de)serialized as a default
 * value.
 * <p>
 * Copyright (c) 2023-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
 */
@Deprecated
public class DistributionValue implements DefaultValue
{

    /** Distribution type. */
    public final DistributionType type;

    /**
     * Default value. This is solely used when a parameter distribution is selected by the user, where the mean/mode/median
     * needs to get a default value. This value is then the default value of the parameter. In case that default value is a
     * distribution (i.e. this distribution), this field is the typical value.
     */
    public final double defaultValue;

    /** Names and values of distribution parameters. */
    public final LinkedHashMap<String, Double> values = new LinkedHashMap<>();

    /**
     * Constructor.
     * @param type distribution type.
     * @param defaultValue value of the default mean/mode/median value in another distribution should the user select it.
     */
    private DistributionValue(final DistributionType type, final double defaultValue)
    {
        this.type = type;
        this.defaultValue = defaultValue;
    }

    /**
     * Creates an exponential distribution.
     * @param lambda mean value.
     * @return created distribution.
     */
    public static DistributionValue exponential(final double lambda)
    {
        DistributionValue dist = new DistributionValue(DistributionType.Exponential, lambda);
        dist.values.put("lambda", lambda);
        return dist;
    }

    /**
     * Creates a triangular distribution.
     * @param min minimum value.
     * @param mode mode value.
     * @param max maximum value.
     * @return created distribution.
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
     * @param mu mean value.
     * @param sigma standard deviation.
     * @param min minimum value.
     * @param max maximum value.
     * @return created distribution.
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
     * @param mean mean value of underlying normal distribution.
     * @param std standard deviation of underlying normal distribution.
     * @param min minimum value.
     * @param max maximum value.
     * @return created distribution.
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
     * @param min minimum value.
     * @param max maximum value.
     * @return created distribution.
     */
    public static DistributionValue uniform(final double min, final double max)
    {
        DistributionValue dist = new DistributionValue(DistributionType.Uniform, 0.5 * (min + max));
        dist.values.put("min", min);
        dist.values.put("max", max);
        return dist;
    }

}
