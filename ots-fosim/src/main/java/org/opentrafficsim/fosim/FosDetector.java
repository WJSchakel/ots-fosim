package org.opentrafficsim.fosim;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.djunits.Throw;
import org.djunits.value.vdouble.scalar.Duration;
import org.djunits.value.vdouble.scalar.Length;
import org.djunits.value.vdouble.scalar.Time;
import org.opentrafficsim.core.dsol.OtsSimulatorInterface;
import org.opentrafficsim.core.gtu.RelativePosition;
import org.opentrafficsim.core.network.NetworkException;
import org.opentrafficsim.road.definitions.DefaultsRoadNl;
import org.opentrafficsim.road.gtu.lane.LaneBasedGtu;
import org.opentrafficsim.road.network.lane.Lane;
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

    /** Flow measurements, i.e. vehicle count. */
    private final List<Integer> flow = new ArrayList<>();

    /** Sum of reciprocal speed. */
    private final List<Double> reciprocalSpeed = new ArrayList<>();

    /** Flow measurements, i.e. vehicle count, for all vehicles that passed an earlier detector. */
    private final List<Integer> travelFlow = new ArrayList<>();

    /** Sum of travel time since last detector. */
    private final List<Double> travelingTime = new ArrayList<>();

    /** Current period index. */
    private int index = -1;

    /** Time of first aggregation. */
    private final Time firstAggregation;

    /** Aggregation period. */
    private final Duration aggregationTime;

    /**
     * Constructor.
     * @param id String; id.
     * @param lane Lane; lane.
     * @param longitudinalPosition Length; position on lane.
     * @param simulator OtsSimulatorInterface; simulator.
     * @param prevTime Map&lt;String, Time&gt;; registered passing times in previous detector cross-section.
     * @param thisTime Map&lt;String, Time&gt;; registered passing times in this detector cross-section.
     * @param firstAggregation Time; time of first aggregation.
     * @param aggregationTime Duration; aggregation period.
     * @throws NetworkException when the position on the lane is out of bounds
     */
    public FosDetector(final String id, final Lane lane, final Length longitudinalPosition,
            final OtsSimulatorInterface simulator, final Map<String, Time> prevTime, final Map<String, Time> thisTime,
            final Time firstAggregation, final Duration aggregationTime) throws NetworkException
    {
        super(id, lane, longitudinalPosition, RelativePosition.FRONT, simulator, DefaultsRoadNl.LOOP_DETECTOR);
        this.prevTime = prevTime;
        this.thisTime = thisTime;
        this.firstAggregation = firstAggregation;
        this.aggregationTime = aggregationTime;
        aggregate();
    }

    /**
     * Aggregate data and schedule next aggregation.
     */
    private void aggregate()
    {
        // Note: data is self-aggregating, we only need to move to the next period
        this.index++;
        this.flow.add(0);
        this.reciprocalSpeed.add(0.0);
        this.travelFlow.add(0);
        this.travelingTime.add(0.0);
        Duration time = Duration.instantiateSI(firstAggregation.si + this.index * this.aggregationTime.si);
        getSimulator().scheduleEventAbs(time, this, "aggregate", null);
    }

    /** {@inheritDoc} */
    @Override
    protected void triggerResponse(final LaneBasedGtu gtu)
    {
        this.flow.set(this.index, this.flow.get(this.index) + 1);
        this.reciprocalSpeed.set(this.index, this.reciprocalSpeed.get(this.index) + (1.0 / Math.max(gtu.getSpeed().si, 0.05)));
        Time now = getSimulator().getSimulatorAbsTime();
        Time prev = this.prevTime.remove(gtu.getId());
        if (prev != null)
        {
            this.travelFlow.set(this.index, this.travelFlow.get(this.index) + 1);
            this.travelingTime.set(this.index, this.travelingTime.get(this.index) + now.minus(prev).si);
        }
        this.thisTime.put(gtu.getId(), now);
    }

    /**
     * Return flow value.
     * @param period int; period index.
     * @return flow value.
     */
    public int getFlow(final int period)
    {
        checkPeriod(period);
        return this.flow.get(period);
    }

    /**
     * Return reciprocal speed value.
     * @param period int; period index.
     * @return reciprocal speed value.
     */
    public double getReciprocalSpeed(final int period)
    {
        checkPeriod(period);
        return this.reciprocalSpeed.get(period);
    }

    /**
     * Return travel flow value.
     * @param period int; period index.
     * @return travel flow value.
     */
    public int getTravelFlow(final int period)
    {
        checkPeriod(period);
        return this.travelFlow.get(period);
    }

    /**
     * Return traveling time value.
     * @param period int; period index.
     * @return traveling time value.
     */
    public double getTravelingTime(final int period)
    {
        checkPeriod(period);
        return this.travelingTime.get(period);
    }

    /**
     * Checks that the given period index is a valid index.
     * @param period int; period index.
     */
    private void checkPeriod(final int period)
    {
        Throw.when(period < 0 || period > this.index, IndexOutOfBoundsException.class, "Index %s is not a valid period.",
                period);
    }

}
