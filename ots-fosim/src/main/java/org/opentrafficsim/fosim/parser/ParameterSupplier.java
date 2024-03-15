package org.opentrafficsim.fosim.parser;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.djunits.unit.Unit;
import org.djunits.value.vdouble.scalar.base.AbstractDoubleScalarRel;
import org.opentrafficsim.base.parameters.ParameterException;
import org.opentrafficsim.base.parameters.ParameterType;
import org.opentrafficsim.base.parameters.ParameterTypeInteger;
import org.opentrafficsim.base.parameters.ParameterTypeLength;
import org.opentrafficsim.base.parameters.ParameterTypeNumeric;
import org.opentrafficsim.base.parameters.ParameterTypeSpeed;
import org.opentrafficsim.base.parameters.constraint.Constraint;
import org.opentrafficsim.base.parameters.constraint.NumericConstraint;
import org.opentrafficsim.core.distributions.Generator;
import org.opentrafficsim.core.distributions.ProbabilityException;
import org.opentrafficsim.core.gtu.GtuType;
import org.opentrafficsim.core.parameters.ParameterFactoryByType;
import org.opentrafficsim.core.units.distributions.ContinuousDistDoubleScalar;
import org.opentrafficsim.road.gtu.lane.perception.mental.Fuller;
import org.opentrafficsim.road.gtu.lane.tactical.util.lmrs.LmrsParameters;

import nl.tudelft.simulation.jstats.distributions.DistContinuous;
import nl.tudelft.simulation.jstats.distributions.DistDiscrete;

/**
 * A set of parameter values in scalar or distributed form.
 * <p>
 * Copyright (c) 2023-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
 */
public class ParameterSupplier
{

    /** Length parameter type. */
    public static ParameterTypeLength L = new ParameterTypeLength("L", "Vehicle length", NumericConstraint.POSITIVE);

    /** Width parameter type. */
    public static ParameterTypeLength W = new ParameterTypeLength("W", "Vehicle width", NumericConstraint.POSITIVE);

    /** Maximum vehicle speed parameter type. */
    public static ParameterTypeSpeed V_MAX =
            new ParameterTypeSpeed("V_MAX", "Maximum vehicle speed", NumericConstraint.POSITIVE);

    /** Estimation parameter type. */
    public static ParameterTypeInteger ESTIMATION =
            new ParameterTypeInteger("Estimation", "Whether the driver under- or over-estimates.", new Constraint<Integer>()
            {
                /** {@inheritDoc} */
                @Override
                public boolean accept(final Integer value)
                {
                    return value == -1 || value == 1;
                }

                /** {@inheritDoc} */
                @Override
                public String failMessage()
                {
                    return "Value should be -1 or 1.";
                }
            });

    /** Set of parameter types that do not belong in the behavioral parameters. */
    public static Set<ParameterType<?>> NON_BEHAVIORAL_PARAMETERS = Set.of(L, W, V_MAX, ESTIMATION);

    /** Parameters. */
    private final Map<Integer, Map<ParameterType<?>, ParameterEntry<?, ?>>> map = new LinkedHashMap<>();

    /**
     * Add a scalar parameter.
     * @param vehicleType int; the vehicle type
     * @param parameterType ParameterType&lt;T&gt;; the parameter type
     * @param value T; the value of the parameter
     * @param <T> parameter value type
     */
    public <T> void addParameter(final int vehicleType, final ParameterType<T> parameterType, final T value)
    {
        assureTypeInMap(vehicleType);
        this.map.get(vehicleType).put(parameterType, new ScalarEntry<>(parameterType, value));
    }

    /**
     * Add a distributed parameter (djunits).
     * @param vehicleType int; the vehicle type
     * @param parameterType ParameterTypeNumeric&lt;T&gt;; the parameter type
     * @param distribution ContinuousDistDoubleScalar.Rel&lt;T,U&gt;; the distribution of the parameter
     * @param <U> unit type
     * @param <T> parameter value type
     */
    public <U extends Unit<U>, T extends AbstractDoubleScalarRel<U, T>> void addParameter(final int vehicleType,
            final ParameterTypeNumeric<T> parameterType, final ContinuousDistDoubleScalar.Rel<T, U> distribution)
    {
        assureTypeInMap(vehicleType);
        this.map.get(vehicleType).put(parameterType, new DistributedEntry<>(parameterType, distribution));
    }

    /**
     * Add a distributed parameter (integer).
     * @param vehicleType Integer; the vehicle type
     * @param parameterType ParameterTypeInteger; the parameter type
     * @param distribution DistDiscrete; the distribution of the parameter
     */
    public void addParameter(final Integer vehicleType, final ParameterType<Integer> parameterType,
            final DistDiscrete distribution)
    {
        assureTypeInMap(vehicleType);
        this.map.get(vehicleType).put(parameterType, new DistributedEntryInteger(parameterType, distribution));
    }

    /**
     * Add a distributed parameter (double).
     * @param vehicleType int; the vehicle type
     * @param parameterType ParameterTypeDouble; the parameter type
     * @param distribution DistContinuous; the distribution of the parameter
     */
    public void addParameter(final int vehicleType, final ParameterType<Double> parameterType,
            final DistContinuous distribution)
    {
        assureTypeInMap(vehicleType);
        this.map.get(vehicleType).put(parameterType, new DistributedEntryDouble(parameterType, distribution));
    }

    /**
     * Assures the vehicle type is in the map.
     * @param vehicleType int; the vehicle type
     */
    private void assureTypeInMap(final int vehicleType)
    {
        if (!this.map.containsKey(vehicleType))
        {
            this.map.put(vehicleType, new LinkedHashMap<>());
        }
    }

    /**
     * Returns the parameter value (scalar or distributed) as a generator. This is for vehicle parameters that are set in GTU
     * characteristics.
     * @param <T> parameter type.
     * @param vehicleType int; vehicle type.
     * @param parameterType ParameterType&lt;T&gt;; parameter type.
     * @return Generator&lt;T&gt;T; generator.
     */
    @SuppressWarnings("unchecked")
    public <T> Generator<T> asGenerator(final int vehicleType, final ParameterType<T> parameterType)
    {
        return (Generator<T>) this.map.get(vehicleType).get(parameterType);
    }

    /**
     * Sets all behavioral parameters in the parameter factory. These exclude the vehicle parameters, or parameters that govern
     * what model to use (rather than with what value).
     * @param gtuTypes List&lt;GtuType&gt;; list of GTU types indexed at their vehicle type index.
     * @param parameterFactory ParameterFactoryByType; parameter factory to set the parameter values in, scalar or distributed.
     */
    public void setAllInParameterFactory(final List<GtuType> gtuTypes, final ParameterFactoryByType parameterFactory)
    {
        for (Entry<Integer, Map<ParameterType<?>, ParameterEntry<?, ?>>> entry : this.map.entrySet())
        {
            GtuType gtuType = gtuTypes.get(entry.getKey());
            for (ParameterEntry<?, ?> parameterEntry : entry.getValue().values())
            {
                if (!NON_BEHAVIORAL_PARAMETERS.contains(parameterEntry.getParameterType()))
                {
                    parameterEntry.setInParameterFactory(gtuType, parameterFactory);
                }
            }
        }
    }

    /**
     * Whether the model parameters include social interactions.
     * @return boolean; whether the model parameters include social interactions.
     */
    public boolean includesSocialInteractions()
    {
        return this.map.get(0).containsKey(LmrsParameters.SOCIO);
    }

    /**
     * Whether the model parameters include perception.
     * @return boolean; whether the model parameters include perception.
     */
    public boolean includesPerception()
    {
        return this.map.get(0).containsKey(Fuller.TC);
    }

    /** {@inheritDoc} */
    @Override
    public String toString()
    {
        return "ParameterSupplier [map=" + this.map + "]";
    }

    /**
     * Local storage interface for parameters.
     * <p>
     * Copyright (c) 2013-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved.
     * <br>
     * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
     * </p>
     * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
     * @param <P> parameter type
     * @param <T> value type
     */
    private abstract class ParameterEntry<P extends ParameterType<T>, T> implements Generator<T>
    {
        /** Parameter type. */
        private final P parameterType;

        /**
         * Constructor.
         * @param parameterType P; the parameter type
         */
        public ParameterEntry(final P parameterType)
        {
            this.parameterType = parameterType;
        }

        /**
         * Returns the parameter type.
         * @return P; parameter type.
         */
        P getParameterType()
        {
            return this.parameterType;
        }

        /**
         * Sets this parameter value, as scalar or distributed value, in the parameter factory.
         * @param gtuType GtuType; GTU type.
         * @param factory ParameterFactoryByType; factory to set the value in.
         */
        abstract void setInParameterFactory(GtuType gtuType, ParameterFactoryByType factory);
    }

    /**
     * Fixed parameter.
     * <p>
     * Copyright (c) 2013-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved.
     * <br>
     * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
     * </p>
     * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
     * @param <T> value type
     */
    public final class ScalarEntry<T> extends ParameterEntry<ParameterType<T>, T>
    {
        /** Value. */
        private final T value;

        /**
         * @param parameterType ParameterType&lt;T&gt;; the parameter type
         * @param value T; the fixed value
         */
        ScalarEntry(final ParameterType<T> parameterType, final T value)
        {
            super(parameterType);
            this.value = value;
        }

        /** {@inheritDoc} */
        @Override
        public T draw() throws ProbabilityException, ParameterException
        {
            return this.value;
        }

        /** {@inheritDoc} */
        @Override
        public void setInParameterFactory(final GtuType gtuType, final ParameterFactoryByType factory)
        {
            factory.addParameter(gtuType, getParameterType(), this.value);
        }

        /** {@inheritDoc} */
        @Override
        public String toString()
        {
            return "FixedEntry [parameterType=" + getParameterType() + ", value=" + this.value + "]";
        }
    }

    /**
     * Distributed parameter.
     * <p>
     * Copyright (c) 2013-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved.
     * <br>
     * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
     * </p>
     * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
     * @param <U> unit type
     * @param <T> value type
     */
    public final class DistributedEntry<U extends Unit<U>, T extends AbstractDoubleScalarRel<U, T>>
            extends ParameterEntry<ParameterTypeNumeric<T>, T>
    {
        /** Distribution of the parameter. */
        private final ContinuousDistDoubleScalar.Rel<T, U> distribution;

        /**
         * @param parameterType ParameterTypeNumeric&lt;T&gt;; the parameter type
         * @param distribution ContinuousDistDoubleScalar.Rel&lt;T,U&gt;; the distribution of the parameter
         */
        DistributedEntry(final ParameterTypeNumeric<T> parameterType, final ContinuousDistDoubleScalar.Rel<T, U> distribution)
        {
            super(parameterType);
            this.distribution = distribution;
        }

        /** {@inheritDoc} */
        @Override
        public T draw() throws ProbabilityException, ParameterException
        {
            return this.distribution.draw();
        }

        /** {@inheritDoc} */
        @Override
        public void setInParameterFactory(final GtuType gtuType, final ParameterFactoryByType factory)
        {
            factory.addParameter(gtuType, getParameterType(), this.distribution);
        }

        /** {@inheritDoc} */
        @Override
        public String toString()
        {
            return "DistributedEntry [parameterType=" + getParameterType() + ", distribution=" + this.distribution + "]";
        }
    }

    /**
     * Distributed double value.
     * <p>
     * Copyright (c) 2013-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved.
     * <br>
     * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
     * </p>
     * @author <a href="https://github.com/averbraeck">Alexander Verbraeck</a>
     * @author <a href="https://tudelft.nl/staff/p.knoppers-1">Peter Knoppers</a>
     * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
     */
    public final class DistributedEntryDouble extends ParameterEntry<ParameterType<Double>, Double>
    {
        /** Parameter distribution. */
        private final DistContinuous distribution;

        /**
         * @param parameterType ParameterTypeDouble; the parameter type
         * @param distribution DistContinuous; parameter distribution
         */
        DistributedEntryDouble(final ParameterType<Double> parameterType, final DistContinuous distribution)
        {
            super(parameterType);
            this.distribution = distribution;
        }

        /** {@inheritDoc} */
        @Override
        public Double draw() throws ProbabilityException, ParameterException
        {
            return this.distribution.draw();
        }

        /** {@inheritDoc} */
        @Override
        void setInParameterFactory(final GtuType gtuType, final ParameterFactoryByType factory)
        {
            factory.addParameter(gtuType, getParameterType(), this.distribution);
        }

        /** {@inheritDoc} */
        @Override
        public String toString()
        {
            return "DistributedEntryDouble [parameterType=" + getParameterType() + ", distribution=" + this.distribution + "]";
        }
    }

    /**
     * Distributed integer value.
     * <p>
     * Copyright (c) 2013-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved.
     * <br>
     * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
     * </p>
     * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
     */
    public final class DistributedEntryInteger extends ParameterEntry<ParameterType<Integer>, Integer>
    {
        /** Parameter distribution. */
        private final DistDiscrete distribution;

        /**
         * @param parameterType ParameterTypeInteger; the parameter type
         * @param distribution DistDiscrete; parameter distribution
         */
        DistributedEntryInteger(final ParameterType<Integer> parameterType, final DistDiscrete distribution)
        {
            super(parameterType);
            this.distribution = distribution;
        }

        /** {@inheritDoc} */
        @Override
        public Integer draw() throws ProbabilityException, ParameterException
        {
            return (int) this.distribution.draw();
        }

        /** {@inheritDoc} */
        @Override
        void setInParameterFactory(final GtuType gtuType, final ParameterFactoryByType factory)
        {
            factory.addParameter(gtuType, getParameterType(), this.distribution);
        }

        /** {@inheritDoc} */
        @Override
        public String toString()
        {
            return "DistributedEntryInteger [parameterType=" + getParameterType() + ", distribution=" + this.distribution + "]";
        }
    }

}
