package org.opentrafficsim.fosim;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import org.djunits.value.vdouble.scalar.Duration;
import org.djunits.value.vdouble.scalar.Length;
import org.djunits.value.vdouble.scalar.Speed;
import org.djunits.value.vdouble.scalar.Time;
import org.djutils.exceptions.Throw;
import org.opentrafficsim.core.definitions.DefaultsNl;
import org.opentrafficsim.core.dsol.OtsSimulatorInterface;
import org.opentrafficsim.core.gtu.GtuType;
import org.opentrafficsim.core.gtu.RelativePosition;
import org.opentrafficsim.core.network.NetworkException;
import org.opentrafficsim.road.gtu.lane.LaneBasedGtu;
import org.opentrafficsim.road.network.lane.Lane;
import org.opentrafficsim.road.network.lane.object.LaneBasedObject;
import org.opentrafficsim.road.network.lane.object.detector.LaneDetector;

/**
 * Detector that measures and can return detector measurements for FOSIM.
 * <p>
 * Copyright (c) 2023-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
 */
public class FosDetector extends LaneDetector
{

    /** */
    private static final long serialVersionUID = 20240823L;

    /** Registered passing times in previous detector cross-section. */
    private final Map<String, Time> prevTime;

    /** Registered passing times in this detector cross-section. */
    private final Map<String, Time> thisTime;

    /** Vehicle count. */
    private final List<Integer> count = new ArrayList<>();

    /** Sum of reciprocal speed. */
    private final List<Double> sumReciprocalSpeed = new ArrayList<>();

    /** Vehicle count, for all vehicles that passed an earlier detector. */
    private final List<Integer> travelTimeCount = new ArrayList<>();

    /** Sum of travel time since last detector. */
    private final List<Double> sumTravelTime = new ArrayList<>();

    /** GTU passings. */
    private NavigableMap<Time, Passing> passings = new TreeMap<>();

    /** Current period index. */
    private int index = -1;

    /** Time of first aggregation. */
    private final Time firstAggregation;

    /** Aggregation period. */
    private final Duration aggregationTime;

    /**
     * Constructor.
     * @param id id.
     * @param lane lane.
     * @param longitudinalPosition position on lane.
     * @param simulator simulator.
     * @param prevTime registered passing times in previous detector cross-section.
     * @param thisTime registered passing times in this detector cross-section.
     * @param firstAggregation time of first aggregation.
     * @param aggregationTime aggregation period.
     * @throws NetworkException when the position on the lane is out of bounds
     */
    public FosDetector(final String id, final Lane lane, final Length longitudinalPosition,
            final OtsSimulatorInterface simulator, final Map<String, Time> prevTime, final Map<String, Time> thisTime,
            final Time firstAggregation, final Duration aggregationTime) throws NetworkException
    {
        super(id, lane, longitudinalPosition, RelativePosition.FRONT, LaneBasedObject.makeLine(lane, longitudinalPosition, 1.0),
                DefaultsNl.LOOP_DETECTOR);
        this.prevTime = prevTime;
        this.thisTime = thisTime;
        this.firstAggregation = firstAggregation;
        this.aggregationTime = aggregationTime;
        increasePeriod();
    }

    /**
     * Initialize next measurements and schedule next period.
     */
    private void increasePeriod()
    {
        this.index++;
        this.count.add(0);
        this.sumReciprocalSpeed.add(0.0);
        this.travelTimeCount.add(0);
        this.sumTravelTime.add(0.0);
        Duration time = Duration.instantiateSI(this.firstAggregation.si + this.index * this.aggregationTime.si);
        getSimulator().scheduleEventAbs(time, this, "increasePeriod", null);
    }

    /** {@inheritDoc} */
    @Override
    protected void triggerResponse(final LaneBasedGtu gtu)
    {
        this.count.set(this.index, this.count.get(this.index) + 1);
        this.sumReciprocalSpeed.set(this.index,
                this.sumReciprocalSpeed.get(this.index) + (1.0 / Math.max(gtu.getSpeed().si, 0.05)));
        Time now = getSimulator().getSimulatorAbsTime();
        Time prev = this.prevTime.remove(gtu.getId());
        if (prev != null)
        {
            Duration travelTime = now.minus(prev);
            this.travelTimeCount.set(this.index, this.travelTimeCount.get(this.index) + 1);
            this.sumTravelTime.set(this.index, this.sumTravelTime.get(this.index) + travelTime.si);
            this.passings.put(now, new Passing(now, travelTime, gtu.getSpeed(), gtu.getType(), gtu.getId(),
                    gtu.getStrategicalPlanner().getDestination().getId()));
        }
        else
        {
            this.passings.put(now, new Passing(now, null, gtu.getSpeed(), gtu.getType(), gtu.getId(),
                    gtu.getStrategicalPlanner().getDestination().getId()));
        }
        this.thisTime.put(gtu.getId(), now);
    }

    /**
     * Return vehicle count.
     * @param period period index.
     * @return vehicle count.
     */
    public int getCount(final int period)
    {
        checkPeriod(period);
        return this.count.get(period);
    }

    /**
     * Return sum of reciprocal speed.
     * @param period period index.
     * @return sum of reciprocal speed.
     */
    public double getSumReciprocalSpeed(final int period)
    {
        checkPeriod(period);
        return this.sumReciprocalSpeed.get(period);
    }

    /**
     * Return count of vehicles in travel time sum.
     * @param period period index.
     * @return count of vehicles in travel time sum.
     */
    public int getTravelTimeCount(final int period)
    {
        checkPeriod(period);
        return this.travelTimeCount.get(period);
    }

    /**
     * Return sum of travel time.
     * @param period period index.
     * @return sum of travel time.
     */
    public double getSumTravelTime(final int period)
    {
        checkPeriod(period);
        return this.sumTravelTime.get(period);
    }

    /**
     * Returns the current period index.
     * @return current period index.
     */
    public int getCurrentPeriod()
    {
        return this.index;
    }

    /**
     * Checks that the given period index is a valid index.
     * @param period period index.
     */
    private void checkPeriod(final int period)
    {
        Throw.when(period < 0 || period > this.index, IndexOutOfBoundsException.class, "Index %s is not a valid period.",
                period);
    }

    /**
     * Returns safe copy of the passings since given time.
     * @param startTime start time (exclusive)
     * @return passings since given time
     */
    public Set<Passing> getPassings(final Time startTime)
    {
        return new LinkedHashSet<>(this.passings.tailMap(startTime, false).values());
    }
    
    /**
     * Clear passings up to given time.
     * @param time time (inclusive)
     */
    public void clearPassingsUpTo(final Time time)
    {
        this.passings.headMap(time, true).clear();
    }

    /**
     * Single vehicle passage record.
     * @param time time
     * @param travelTimeSinceDetector travel time since last detector
     * @param speed speed
     * @param gtuType GTU type
     * @param id id
     * @param destination destination node id
     */
    public record Passing(Time time, Duration travelTimeSinceDetector, Speed speed, GtuType gtuType, String id,
            String destination)
    {
    };

}
