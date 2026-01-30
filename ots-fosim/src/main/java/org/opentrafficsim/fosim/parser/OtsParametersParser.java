package org.opentrafficsim.fosim.parser;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.djunits.unit.AccelerationUnit;
import org.djunits.unit.DurationUnit;
import org.djunits.unit.LengthUnit;
import org.djunits.unit.SpeedUnit;
import org.djunits.unit.Unit;
import org.djunits.value.vdouble.scalar.Acceleration;
import org.djunits.value.vdouble.scalar.Duration;
import org.djunits.value.vdouble.scalar.Length;
import org.djunits.value.vdouble.scalar.Speed;
import org.djunits.value.vdouble.scalar.base.DoubleScalarRel;
import org.opentrafficsim.base.parameters.ParameterException;
import org.opentrafficsim.base.parameters.ParameterTypeDouble;
import org.opentrafficsim.base.parameters.ParameterTypeNumeric;
import org.opentrafficsim.base.parameters.ParameterTypes;
import org.opentrafficsim.core.gtu.GtuTemplate;
import org.opentrafficsim.core.gtu.GtuType;
import org.opentrafficsim.core.parameters.ParameterFactoryByType;
import org.opentrafficsim.core.units.distributions.ContinuousDistAcceleration;
import org.opentrafficsim.core.units.distributions.ContinuousDistDoubleScalar;
import org.opentrafficsim.core.units.distributions.ContinuousDistDoubleScalar.Rel;
import org.opentrafficsim.core.units.distributions.ContinuousDistDuration;
import org.opentrafficsim.core.units.distributions.ContinuousDistLength;
import org.opentrafficsim.core.units.distributions.ContinuousDistSpeed;
import org.opentrafficsim.fosim.parameters.ParameterDefinitions;
import org.opentrafficsim.fosim.parameters.data.DistributionData;
import org.opentrafficsim.fosim.parameters.data.ParameterData;
import org.opentrafficsim.fosim.parameters.data.ParameterDataDefinition;
import org.opentrafficsim.fosim.parameters.data.ParameterDataGroup;
import org.opentrafficsim.fosim.parameters.data.ScalarData;
import org.opentrafficsim.fosim.parameters.data.ValueData;
import org.opentrafficsim.road.gtu.lane.perception.mental.AdaptationHeadway;
import org.opentrafficsim.road.gtu.lane.perception.mental.Fuller;
import org.opentrafficsim.road.gtu.lane.perception.mental.ar.ArTaskCarFollowingExp;
import org.opentrafficsim.road.gtu.lane.perception.mental.channel.AdaptationSpeedChannel;
import org.opentrafficsim.road.gtu.lane.perception.mental.channel.ChannelFuller;
import org.opentrafficsim.road.gtu.lane.perception.mental.channel.ChannelTaskScan;
import org.opentrafficsim.road.gtu.lane.perception.mental.channel.ChannelTaskSignal;
import org.opentrafficsim.road.gtu.lane.tactical.following.AbstractIdm;
import org.opentrafficsim.road.gtu.lane.tactical.util.lmrs.LmrsParameters;
import org.opentrafficsim.road.gtu.lane.tactical.util.lmrs.Tailgating;

import nl.tudelft.simulation.jstats.distributions.DistContinuous;
import nl.tudelft.simulation.jstats.distributions.DistExponential;
import nl.tudelft.simulation.jstats.distributions.DistLogNormalTrunc;
import nl.tudelft.simulation.jstats.distributions.DistNormalTrunc;
import nl.tudelft.simulation.jstats.distributions.DistTriangular;
import nl.tudelft.simulation.jstats.distributions.DistUniform;
import nl.tudelft.simulation.jstats.streams.StreamInterface;

/**
 * Helper class for FosParser to parse OTS parameters.
 * <p>
 * Copyright (c) 2024-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
 */
public class OtsParametersParser
{

    /** GTU types. */
    private List<GtuType> gtuTypes;

    /** Parameter definitions. */
    private ParameterDataDefinition otsParameters;

    /**
     * Constructor.
     * @param gtuTypes GTU types.
     * @param otsParameters OTS parameter definitions.
     */
    private OtsParametersParser(final List<GtuType> gtuTypes, final ParameterDataDefinition otsParameters)
    {
        this.gtuTypes = gtuTypes;
        this.otsParameters = otsParameters;
    }

    /**
     * Parse OTS parameters in to templates and parameter factory using parsed GTU types.
     * @param gtuTypes GTU types.
     * @param otsParameters OTS parameter definitions.
     * @param templates templates.
     * @param parameterFactory parameter factory.
     * @param stream stream of random numbers.
     * @throws ParameterException if a parameter is not found for the vehicle type.
     */
    public static void parse(final List<GtuType> gtuTypes, final ParameterDataDefinition otsParameters,
            final Map<GtuType, GtuTemplate> templates, final ParameterFactoryByType parameterFactory,
            final StreamInterface stream) throws ParameterException
    {
        OtsParametersParser parser = new OtsParametersParser(gtuTypes, otsParameters);
        for (int vehicleTypeNumber = 0; vehicleTypeNumber < gtuTypes.size(); vehicleTypeNumber++)
        {
            parser.applyParameters(templates, parameterFactory, stream, vehicleTypeNumber);
        }
    }

    /**
     * Applies all parameter values and distributions, both in the GTU template and parameter factory.
     * @param templates templates.
     * @param parameterFactory parameter factory.
     * @param stream stream of random numbers.
     * @param vehicleTypeNumber vehicle type number.
     * @throws ParameterException if a parameter is not found for the vehicle type.
     */
    private void applyParameters(final Map<GtuType, GtuTemplate> templates, final ParameterFactoryByType parameterFactory,
            final StreamInterface stream, final int vehicleTypeNumber) throws ParameterException
    {
        // GTU templates
        GtuType gtuType = this.gtuTypes.get(vehicleTypeNumber);
        ValueData valueLength = getParameterData(ParameterDefinitions.VEHICLE_GROUP_ID, "l").value.get(vehicleTypeNumber);
        Supplier<Length> length =
                (valueLength instanceof ScalarData) ? () -> Length.ofSI(((ScalarData) valueLength).value())
                        : getDistribution((DistributionData) valueLength, stream, Length.class, LengthUnit.SI);
        ValueData valueWidth = getParameterData(ParameterDefinitions.VEHICLE_GROUP_ID, "w").value.get(vehicleTypeNumber);
        Supplier<Length> width =
                (valueWidth instanceof ScalarData) ? () -> Length.ofSI(((ScalarData) valueWidth).value())
                        : getDistribution((DistributionData) valueWidth, stream, Length.class, LengthUnit.SI);
        ValueData valueMaxV = getParameterData(ParameterDefinitions.VEHICLE_GROUP_ID, "vMax").value.get(vehicleTypeNumber);
        Supplier<Speed> speed =
                (valueMaxV instanceof ScalarData) ? () -> new Speed(((ScalarData) valueMaxV).value(), SpeedUnit.KM_PER_HOUR)
                        : getDistribution((DistributionData) valueMaxV, stream, Speed.class, SpeedUnit.KM_PER_HOUR);

        templates.put(gtuType, new GtuTemplate(gtuType, length, width, speed));

        // figure out which components to use
        boolean social = false;
        boolean perception = false;
        boolean estimation = false;
        if (this.otsParameters != null)
        {
            for (ParameterDataGroup group : this.otsParameters.parameterGroups)
            {
                if (group.id.equals(ParameterDefinitions.SOCIAL_GROUP_ID) && group.state != null
                        && group.state[vehicleTypeNumber].isActive())
                {
                    social = true;
                }
                else if (group.id.equals(ParameterDefinitions.PERCEPTION_GROUP_ID) && group.state != null
                        && group.state[vehicleTypeNumber].isActive())
                {
                    perception = true;
                }
                else if (group.id.equals(ParameterDefinitions.ESTIMATION_GROUP_ID) && group.state != null
                        && group.state[vehicleTypeNumber].isActive())
                {
                    estimation = true;
                }
            }
        }

        // Driver: Tmax, Tmin, fSpeed
        addParameter(parameterFactory, ParameterTypes.TMAX, ParameterDefinitions.DRIVER_GROUP_ID, "Tmax", Duration.class,
                DurationUnit.SI, stream, vehicleTypeNumber);
        // T = Tmax
        ValueData valueData = getParameterData(ParameterDefinitions.DRIVER_GROUP_ID, "Tmax").value.get(vehicleTypeNumber);
        Duration t = Duration.ofSI(valueData instanceof ScalarData ? ((ScalarData) valueData).value()
                : getTypicalValue((DistributionData) valueData));

        parameterFactory.addParameter(this.gtuTypes.get(vehicleTypeNumber), ParameterTypes.T, t);
        addParameter(parameterFactory, ParameterTypes.TMIN, ParameterDefinitions.DRIVER_GROUP_ID, "Tmin", Duration.class,
                DurationUnit.SI, stream, vehicleTypeNumber);
        addParameter(parameterFactory, ParameterTypes.FSPEED, ParameterDefinitions.DRIVER_GROUP_ID, "fSpeed", stream,
                vehicleTypeNumber);

        // Car-following: a, b0, b, bCrit, s0, delta
        addParameter(parameterFactory, ParameterTypes.A, ParameterDefinitions.FOLLOWING_GROUP_ID, "a", Acceleration.class,
                AccelerationUnit.SI, stream, vehicleTypeNumber);
        addParameter(parameterFactory, ParameterTypes.B0, ParameterDefinitions.FOLLOWING_GROUP_ID, "b0", Acceleration.class,
                AccelerationUnit.SI, stream, vehicleTypeNumber);
        addParameter(parameterFactory, ParameterTypes.B, ParameterDefinitions.FOLLOWING_GROUP_ID, "b", Acceleration.class,
                AccelerationUnit.SI, stream, vehicleTypeNumber);
        addParameter(parameterFactory, ParameterTypes.BCRIT, ParameterDefinitions.FOLLOWING_GROUP_ID, "bCrit",
                Acceleration.class, AccelerationUnit.SI, stream, vehicleTypeNumber);
        addParameter(parameterFactory, ParameterTypes.S0, ParameterDefinitions.FOLLOWING_GROUP_ID, "s0", Length.class,
                LengthUnit.SI, stream, vehicleTypeNumber);
        addParameter(parameterFactory, AbstractIdm.DELTA, ParameterDefinitions.FOLLOWING_GROUP_ID, "delta", stream,
                vehicleTypeNumber);
        // n (nLeaders) is set where the IdmPlusMulti car-following model is set

        // Lane-changing: dFree, dSyn, dCoop, tau, x0, t0, vGain, vCong
        addParameter(parameterFactory, LmrsParameters.DFREE, ParameterDefinitions.LC_GROUP_ID, "dFree", stream,
                vehicleTypeNumber);
        addParameter(parameterFactory, LmrsParameters.DSYNC, ParameterDefinitions.LC_GROUP_ID, "dSync", stream,
                vehicleTypeNumber);
        addParameter(parameterFactory, LmrsParameters.DCOOP, ParameterDefinitions.LC_GROUP_ID, "dCoop", stream,
                vehicleTypeNumber);
        addParameter(parameterFactory, ParameterTypes.TAU, ParameterDefinitions.LC_GROUP_ID, "tau", Duration.class,
                DurationUnit.SI, stream, vehicleTypeNumber);
        addParameter(parameterFactory, ParameterTypes.LOOKAHEAD, ParameterDefinitions.LC_GROUP_ID, "x0", Length.class,
                LengthUnit.SI, stream, vehicleTypeNumber);
        addParameter(parameterFactory, ParameterTypes.T0, ParameterDefinitions.LC_GROUP_ID, "t0", Duration.class,
                DurationUnit.SI, stream, vehicleTypeNumber);
        addParameter(parameterFactory, LmrsParameters.VGAIN, ParameterDefinitions.LC_GROUP_ID, "vGain", Speed.class,
                SpeedUnit.KM_PER_HOUR, stream, vehicleTypeNumber);
        addParameter(parameterFactory, ParameterTypes.VCONG, ParameterDefinitions.LC_GROUP_ID, "vCong", Speed.class,
                SpeedUnit.KM_PER_HOUR, stream, vehicleTypeNumber);

        // Social interactions: sigma, (courtesy)
        if (social)
        {
            addParameter(parameterFactory, LmrsParameters.SOCIO, ParameterDefinitions.SOCIAL_GROUP_ID, "sigma", stream,
                    vehicleTypeNumber);
            // Rho (init) = 0.0
            parameterFactory.addParameter(this.gtuTypes.get(vehicleTypeNumber), Tailgating.RHO, 0.0);
        }

        // Perception: tau_min, tau_max, TC, td_scan, td_signal, x0_d, h_exp, beta_T, beta_v0, f_over (as estimation factor)
        if (perception)
        {
            addParameter(parameterFactory, ChannelFuller.TAU_MIN, ParameterDefinitions.PERCEPTION_GROUP_ID, "tau_min",
                    Duration.class, DurationUnit.SI, stream, vehicleTypeNumber);
            addParameter(parameterFactory, ChannelFuller.TAU_MAX, ParameterDefinitions.PERCEPTION_GROUP_ID, "tau_max",
                    Duration.class, DurationUnit.SI, stream, vehicleTypeNumber);
            addParameter(parameterFactory, Fuller.TC, ParameterDefinitions.PERCEPTION_GROUP_ID, "TC", stream,
                    vehicleTypeNumber);
            addParameter(parameterFactory, ChannelTaskScan.TDSCAN, ParameterDefinitions.PERCEPTION_GROUP_ID, "td_scan", stream,
                    vehicleTypeNumber);
            addParameter(parameterFactory, ChannelTaskSignal.TDSIGNAL, ParameterDefinitions.PERCEPTION_GROUP_ID, "td_signal",
                    stream, vehicleTypeNumber);
            addParameter(parameterFactory, ChannelTaskSignal.X0_D, ParameterDefinitions.PERCEPTION_GROUP_ID, "x0_d",
                    Length.class, LengthUnit.SI, stream, vehicleTypeNumber);
            addParameter(parameterFactory, ArTaskCarFollowingExp.HEXP, ParameterDefinitions.PERCEPTION_GROUP_ID, "h_exp",
                    Duration.class, DurationUnit.SI, stream, vehicleTypeNumber);
            addParameter(parameterFactory, AdaptationHeadway.BETA_T, ParameterDefinitions.PERCEPTION_GROUP_ID, "beta_T", stream,
                    vehicleTypeNumber);
            addParameter(parameterFactory, AdaptationSpeedChannel.BETA_V0, ParameterDefinitions.PERCEPTION_GROUP_ID, "beta_v0",
                    stream, vehicleTypeNumber);
            if (estimation)
            {
                setEstimationFactor(vehicleTypeNumber, stream, parameterFactory);
            }
        }

    }

    /**
     * Sets the estimation factor as a random distribution based on fraction of over estimation.
     * @param vehicleTypeNumber vehicle type number
     * @param stream stream
     * @param parameterFactory parameter factory
     * @throws ParameterException if the parameter could not be found
     */
    private void setEstimationFactor(final int vehicleTypeNumber, final StreamInterface stream,
            final ParameterFactoryByType parameterFactory) throws ParameterException
    {
        ValueData fractionOver =
                getParameterData(ParameterDefinitions.ESTIMATION_GROUP_ID, "f_over").value.get(vehicleTypeNumber);
        double f;
        if (fractionOver instanceof ScalarData scalarFractionOver)
        {
            f = scalarFractionOver.value();
        }
        else
        {
            DistributionData distributionFractionOver = (DistributionData) fractionOver;
            switch (distributionFractionOver.type)
            {
                case Exponential:
                    f = Math.max(0.0, Math.min(1.0, distributionFractionOver.distributionParameters.get("lambda")));
                    break;
                case Triangular:
                    f = Math.max(0.0, Math.min(1.0, distributionFractionOver.distributionParameters.get("mode")));
                    break;
                case Normal:
                    f = Math.max(0.0, Math.min(1.0, distributionFractionOver.distributionParameters.get("mu")));
                    break;
                case LogNormal:
                    double mu = distributionFractionOver.distributionParameters.get("mu");
                    double sigma = distributionFractionOver.distributionParameters.get("sigma");
                    f = Math.max(0.0, Math.min(1.0, Math.exp(mu + sigma * sigma / 2.0)));
                    break;
                case Uniform:
                    double min = distributionFractionOver.distributionParameters.get("min");
                    double max = distributionFractionOver.distributionParameters.get("max");
                    f = Math.max(0.0, Math.min(1.0, (min + max) / 2.0));
                    break;
                default:
                    // ignore then
                    f = 0.5;
            }
        }
        parameterFactory.addParameter(this.gtuTypes.get(vehicleTypeNumber), Fuller.OVER_EST, new DistContinuous(stream)
        {
            @Override
            public double getProbabilityDensity(final double x)
            {
                // this is not correct as we need to define a continuous probability distribution for what is actually a
                // discrete value distribution
                return x == 1.0 ? f : (f == -1.0 ? 1.0 - f : 0.0);
            }

            @Override
            public double draw()
            {
                return getStream().nextDouble() < f ? 1.0 : -1.0;
            }
        });
    }

    /**
     * Returns the parameter data under given group and parameter id.
     * @param parameterGroup parameter group id.
     * @param parameterId parameter id.
     * @return parameter data under given group and parameter id.
     * @throws ParameterException if the parameter does not exist in the group, or the group does not exist.
     */
    private ParameterData getParameterData(final String parameterGroup, final String parameterId) throws ParameterException
    {
        for (ParameterDataGroup group : this.otsParameters.parameterGroups)
        {
            if (group.id.equals(parameterGroup))
            {
                for (ParameterData parameterData : group.parameters)
                {
                    if (parameterData.id.equals(parameterId))
                    {
                        return parameterData;
                    }
                }
            }
        }
        throw new ParameterException("Parameter " + parameterId + " in group " + parameterGroup + " not found.");
    }

    /**
     * Add double parameter to parameter factory, scalar or distributed.
     * @param parameterFactory parameter factory.
     * @param parameterType parameter type.
     * @param parameterGroup parameter group id.
     * @param parameterId parameter id.
     * @param stream random number stream.
     * @param vehicleTypeNumber vehicle type number.
     * @throws ParameterException if the parameter does not exist in the group, or the group does not exist.
     */
    private final void addParameter(final ParameterFactoryByType parameterFactory, final ParameterTypeDouble parameterType,
            final String parameterGroup, final String parameterId, final StreamInterface stream, final int vehicleTypeNumber)
            throws ParameterException
    {
        ValueData valueData = getParameterData(parameterGroup, parameterId).value.get(vehicleTypeNumber);
        if (valueData instanceof ScalarData)
        {
            double value = ((ScalarData) valueData).value();
            parameterFactory.addParameter(this.gtuTypes.get(vehicleTypeNumber), parameterType, value);
        }
        else
        {
            DistributionData distribution = ((DistributionData) valueData);
            DistContinuous dist = getDistribution(distribution, stream);
            parameterFactory.addParameter(this.gtuTypes.get(vehicleTypeNumber), parameterType, dist);
        }
    }

    /**
     * Add DJUNITS parameter to parameter factory, scalar or distributed.
     * @param <U> unit type.
     * @param <T> scalar type.
     * @param parameterFactory parameter factory.
     * @param parameterType parameter type.
     * @param parameterGroup parameter group id.
     * @param parameterId parameter id.
     * @param clazz class of scalar type.
     * @param unit unit.
     * @param stream random number stream.
     * @param vehicleTypeNumber vehicle type number.
     * @throws ParameterException if the parameter does not exist in the group, or the group does not exist.
     */
    @SuppressWarnings("unchecked")
    private final <U extends Unit<U>, T extends DoubleScalarRel<U, T>> void addParameter(
            final ParameterFactoryByType parameterFactory, final ParameterTypeNumeric<T> parameterType,
            final String parameterGroup, final String parameterId, final Class<T> clazz, final U unit,
            final StreamInterface stream, final int vehicleTypeNumber) throws ParameterException
    {
        ValueData valueData = getParameterData(parameterGroup, parameterId).value.get(vehicleTypeNumber);
        if (valueData instanceof ScalarData)
        {
            T value;
            if (unit.equals(LengthUnit.SI) || unit.equals(LengthUnit.KILOMETER))
            {
                value = (T) new Length(((ScalarData) valueData).value(), (LengthUnit) unit);
            }
            else if (unit.equals(SpeedUnit.SI) || unit.equals(SpeedUnit.KM_PER_HOUR))
            {
                value = (T) new Speed(((ScalarData) valueData).value(), (SpeedUnit) unit);
            }
            else if (unit.equals(AccelerationUnit.SI))
            {
                value = (T) new Acceleration(((ScalarData) valueData).value(), (AccelerationUnit) unit);
            }
            else if (unit.equals(DurationUnit.SI))
            {
                value = (T) new Duration(((ScalarData) valueData).value(), (DurationUnit) unit);
            }
            else
            {
                throw new ParameterException("Unknown unit " + unit);
            }
            parameterFactory.addParameter(this.gtuTypes.get(vehicleTypeNumber), parameterType, value);
        }
        else
        {
            if (unit.equals(LengthUnit.SI) || unit.equals(LengthUnit.KILOMETER))
            {
                parameterFactory.addParameter(this.gtuTypes.get(vehicleTypeNumber), parameterType,
                        (ContinuousDistDoubleScalar.Rel<T, U>) getDistribution((DistributionData) valueData, stream,
                                Length.class, (LengthUnit) unit));
            }
            else if (unit.equals(SpeedUnit.SI) || unit.equals(SpeedUnit.KM_PER_HOUR))
            {
                parameterFactory.addParameter(this.gtuTypes.get(vehicleTypeNumber), parameterType,
                        (ContinuousDistDoubleScalar.Rel<T, U>) getDistribution((DistributionData) valueData, stream,
                                Speed.class, (SpeedUnit) unit));
            }
            else if (unit.equals(AccelerationUnit.SI))
            {
                parameterFactory.addParameter(this.gtuTypes.get(vehicleTypeNumber), parameterType,
                        (ContinuousDistDoubleScalar.Rel<T, U>) getDistribution((DistributionData) valueData, stream,
                                Acceleration.class, (AccelerationUnit) unit));
            }
            else if (unit.equals(DurationUnit.SI))
            {
                parameterFactory.addParameter(this.gtuTypes.get(vehicleTypeNumber), parameterType,
                        (ContinuousDistDoubleScalar.Rel<T, U>) getDistribution((DistributionData) valueData, stream,
                                Duration.class, (DurationUnit) unit));
            }
            else
            {
                throw new ParameterException("Unknown unit " + unit);
            }
        }
    }

    /**
     * Get DJUNITS distribution.
     * @param <U> unit type.
     * @param <T> scalar type.
     * @param distribution distribution from FOSIM.
     * @param stream random number stream.
     * @param clazz class of scalar type.
     * @param unit unit.
     * @return DJUNITS distribution.
     * @throws ParameterException if the unit is not supported.
     */
    @SuppressWarnings("unchecked")
    private <U extends Unit<U>, T extends DoubleScalarRel<U, T>> ContinuousDistDoubleScalar.Rel<T, U> getDistribution(
            final DistributionData distribution, final StreamInterface stream, final Class<T> clazz, final U unit)
            throws ParameterException
    {
        DistContinuous dist = getDistribution(distribution, stream);
        if (unit.equals(LengthUnit.SI) || unit.equals(LengthUnit.KILOMETER))
        {
            return (Rel<T, U>) new ContinuousDistLength(dist, (LengthUnit) unit);
        }
        if (unit.equals(SpeedUnit.SI) || unit.equals(SpeedUnit.KM_PER_HOUR))
        {
            return (Rel<T, U>) new ContinuousDistSpeed(dist, (SpeedUnit) unit);
        }
        if (unit.equals(AccelerationUnit.SI))
        {
            return (Rel<T, U>) new ContinuousDistAcceleration(dist, (AccelerationUnit) unit);
        }
        if (unit.equals(DurationUnit.SI))
        {
            return (Rel<T, U>) new ContinuousDistDuration(dist, (DurationUnit) unit);
        }
        throw new ParameterException("Unknown unit " + unit);
    }

    /**
     * Get double distribution.
     * @param distribution distribution from FOSIM.
     * @param stream random number stream.
     * @return double distribution.
     * @throws ParameterException if the distribution is not supported.
     */
    private final DistContinuous getDistribution(final DistributionData distribution, final StreamInterface stream)
            throws ParameterException
    {
        switch (distribution.type)
        {
            case Exponential:
                return new DistExponential(stream, distribution.distributionParameters.get("lambda"));
            case Triangular:
                return new DistTriangular(stream, distribution.distributionParameters.get("min"),
                        distribution.distributionParameters.get("mode"), distribution.distributionParameters.get("max"));
            case Normal:
                return new DistNormalTrunc(stream, distribution.distributionParameters.get("mu"),
                        distribution.distributionParameters.get("sigma"), distribution.distributionParameters.get("min"),
                        distribution.distributionParameters.get("max"));
            case LogNormal:
                return new DistLogNormalTrunc(stream, distribution.distributionParameters.get("mu"),
                        distribution.distributionParameters.get("sigma"), distribution.distributionParameters.get("min"),
                        distribution.distributionParameters.get("max"));
            case Uniform:
                return new DistUniform(stream, distribution.distributionParameters.get("min"),
                        distribution.distributionParameters.get("max"));
            default:
                throw new ParameterException("Unknown distribution type " + distribution.type);
        }
    }

    /**
     * Get the typical value for a distribution, e.g. the mean or mode.
     * @param distribution distribution from FOSIM.
     * @return double distribution.
     * @throws ParameterException if the distribution is not supported.
     */
    private final double getTypicalValue(final DistributionData distribution) throws ParameterException
    {
        switch (distribution.type)
        {
            case Exponential:
                return 1.0 / distribution.distributionParameters.get("lambda");
            case Triangular:
                return distribution.distributionParameters.get("mode");
            case Normal:
                return distribution.distributionParameters.get("mu");
            case LogNormal:
                return distribution.distributionParameters.get("mean");
            case Uniform:
                return 0.5 * (distribution.distributionParameters.get("min") + distribution.distributionParameters.get("max"));
            default:
                throw new ParameterException("Unknown distribution type " + distribution.type);
        }
    }

}
