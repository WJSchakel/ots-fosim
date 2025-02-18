package org.opentrafficsim.fosim.parser;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.djunits.unit.Unit;
import org.djunits.value.vdouble.scalar.base.DoubleScalarRel;
import org.opentrafficsim.base.parameters.ParameterType;
import org.opentrafficsim.base.parameters.ParameterTypeNumeric;
import org.opentrafficsim.core.distributions.Generator;
import org.opentrafficsim.core.gtu.GtuType;
import org.opentrafficsim.core.parameters.ParameterFactoryByType;
import org.opentrafficsim.core.units.distributions.ContinuousDistDoubleScalar;

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

    /** Parameters. */
    private final Map<Integer, Map<ParameterType<?>, ParameterEntry<?, ?>>> map = new LinkedHashMap<>();

    /**
     * Add a scalar parameter.
     * @param vehicleType the vehicle type
     * @param parameterType the parameter type
     * @param value the value of the parameter
     * @param <T> parameter value type
     */
    public <T> void addParameter(final int vehicleType, final ParameterType<T> parameterType, final T value)
    {
        assureTypeInMap(vehicleType);
        this.map.get(vehicleType).put(parameterType, new ScalarEntry<>(parameterType, value));
    }

    /**
     * Add a distributed parameter (djunits).
     * @param vehicleType the vehicle type
     * @param parameterType the parameter type
     * @param distribution ContinuousDistDoubleScalar.Rel&lt;T,U&gt;; the distribution of the parameter
     * @param <U> unit type
     * @param <T> parameter value type
     */
    public <U extends Unit<U>, T extends DoubleScalarRel<U, T>> void addParameter(final int vehicleType,
            final ParameterTypeNumeric<T> parameterType, final ContinuousDistDoubleScalar.Rel<T, U> distribution)
    {
        assureTypeInMap(vehicleType);
        this.map.get(vehicleType).put(parameterType, new DistributedEntry<>(parameterType, distribution));
    }

    /**
     * Add a distributed parameter (integer).
     * @param vehicleType the vehicle type
     * @param parameterType the parameter type
     * @param distribution the distribution of the parameter
     */
    public void addParameter(final Integer vehicleType, final ParameterType<Integer> parameterType,
            final DistDiscrete distribution)
    {
        assureTypeInMap(vehicleType);
        this.map.get(vehicleType).put(parameterType, new DistributedEntryInteger(parameterType, distribution));
    }

    /**
     * Add a distributed parameter (double).
     * @param vehicleType the vehicle type
     * @param parameterType the parameter type
     * @param distribution the distribution of the parameter
     */
    public void addParameter(final int vehicleType, final ParameterType<Double> parameterType,
            final DistContinuous distribution)
    {
        assureTypeInMap(vehicleType);
        this.map.get(vehicleType).put(parameterType, new DistributedEntryDouble(parameterType, distribution));
    }

    /**
     * Assures the vehicle type is in the map.
     * @param vehicleType the vehicle type
     */
    private void assureTypeInMap(final int vehicleType)
    {
        if (!this.map.containsKey(vehicleType))
        {
            this.map.put(vehicleType, new LinkedHashMap<>());
        }
    }

    /**
     * Sets all behavioral parameters in the parameter factory. These exclude the vehicle parameters, or parameters that govern
     * what model to use (rather than with what value).
     * @param gtuTypes list of GTU types indexed at their vehicle type index.
     * @param parameterFactory parameter factory to set the parameter values in, scalar or distributed.
     */
    public void setAllInParameterFactory(final List<GtuType> gtuTypes, final ParameterFactoryByType parameterFactory)
    {
        for (Entry<Integer, Map<ParameterType<?>, ParameterEntry<?, ?>>> entry : this.map.entrySet())
        {
            GtuType gtuType = gtuTypes.get(entry.getKey());
            for (ParameterEntry<?, ?> parameterEntry : entry.getValue().values())
            {
                parameterEntry.setInParameterFactory(gtuType, parameterFactory);
            }
        }
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
         * @param parameterType the parameter type
         */
        public ParameterEntry(final P parameterType)
        {
            this.parameterType = parameterType;
        }

        /**
         * Returns the parameter type.
         * @return parameter type.
         */
        P getParameterType()
        {
            return this.parameterType;
        }

        /**
         * Sets this parameter value, as scalar or distributed value, in the parameter factory.
         * @param gtuType GTU type.
         * @param factory factory to set the value in.
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
         * @param parameterType the parameter type
         * @param value the fixed value
         */
        ScalarEntry(final ParameterType<T> parameterType, final T value)
        {
            super(parameterType);
            this.value = value;
        }

        /** {@inheritDoc} */
        @Override
        public T draw()
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
    public final class DistributedEntry<U extends Unit<U>, T extends DoubleScalarRel<U, T>>
            extends ParameterEntry<ParameterTypeNumeric<T>, T>
    {
        /** Distribution of the parameter. */
        private final ContinuousDistDoubleScalar.Rel<T, U> distribution;

        /**
         * @param parameterType the parameter type
         * @param distribution ContinuousDistDoubleScalar.Rel&lt;T,U&gt;; the distribution of the parameter
         */
        DistributedEntry(final ParameterTypeNumeric<T> parameterType, final ContinuousDistDoubleScalar.Rel<T, U> distribution)
        {
            super(parameterType);
            this.distribution = distribution;
        }

        /** {@inheritDoc} */
        @Override
        public T draw()
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
         * @param parameterType the parameter type
         * @param distribution parameter distribution
         */
        DistributedEntryDouble(final ParameterType<Double> parameterType, final DistContinuous distribution)
        {
            super(parameterType);
            this.distribution = distribution;
        }

        /** {@inheritDoc} */
        @Override
        public Double draw()
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
         * @param parameterType the parameter type
         * @param distribution parameter distribution
         */
        DistributedEntryInteger(final ParameterType<Integer> parameterType, final DistDiscrete distribution)
        {
            super(parameterType);
            this.distribution = distribution;
        }

        /** {@inheritDoc} */
        @Override
        public Integer draw()
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
