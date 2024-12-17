package org.opentrafficsim.fosim.sim0mq;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.djunits.value.vdouble.scalar.Duration;
import org.djunits.value.vdouble.scalar.Length;
import org.djunits.value.vdouble.scalar.Time;
import org.djutils.logger.CategoryLogger;
import org.opentrafficsim.draw.graphs.ContourDataSource;
import org.opentrafficsim.draw.graphs.GraphPath;
import org.opentrafficsim.draw.graphs.GraphPath.Section;
import org.opentrafficsim.draw.graphs.GraphUtil;
import org.opentrafficsim.kpi.interfaces.LaneData;
import org.opentrafficsim.kpi.sampling.SamplerData;
import org.opentrafficsim.kpi.sampling.Trajectory;
import org.opentrafficsim.kpi.sampling.Trajectory.SpaceTimeView;
import org.opentrafficsim.kpi.sampling.TrajectoryGroup;

/**
 * Trimmed-down version of {@code ContourDataSource}.
 * <p>
 * Copyright (c) 2013-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://github.com/averbraeck">Alexander Verbraeck</a>
 * @author <a href="https://tudelft.nl/staff/p.knoppers-1">Peter Knoppers</a>
 * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
 * @see ContourDataSource
 */
public class FosContourDataSource
{

    // *****************************
    // *** CONTEXTUAL PROPERTIES ***
    // *****************************

    /** Sampler data. */
    private final SamplerData<?> samplerData;

    /** Path. */
    private final GraphPath<? extends LaneData<?>> path;

    /** Space axis. */
    private final Axis spaceAxis;

    /** Time axis. */
    private final Axis timeAxis;

    // *****************
    // *** PLOT DATA ***
    // *****************

    /** Total distance traveled per cell. */
    private float[][] distance;

    /** Total time traveled per cell. */
    private float[][] time;

    // ********************
    // *** CONSTRUCTORS ***
    // ********************

    /**
     * Constructor for non-default input.
     * @param samplerData SamplerData&lt;?&gt;; sampler data
     * @param delay Duration; delay so critical future events have occurred, e.g. GTU's next move's to extend trajectories
     * @param path GraphPath&lt;? extends LaneData&gt;; path
     */
    public FosContourDataSource(final SamplerData<?> samplerData, final GraphPath<? extends LaneData<?>> path)
    {
        this.samplerData = samplerData;
        this.path = path;
        this.spaceAxis = new Axis();
        this.timeAxis = new Axis();
    }

    // ************************************
    // *** PLOT INTERFACING AND GETTERS ***
    // ************************************

    /**
     * Returns the path for an {@code AbstractContourPlot} using this {@code ContourDataSource}.
     * @return GraphPath&lt;? extends LaneData&gt;; the path
     */
    final GraphPath<? extends LaneData<?>> getPath()
    {
        return this.path;
    }

    // ************************
    // *** UPDATING METHODS ***
    // ************************

    public void update(final Duration startTime, final Duration dt, final Duration finishTime, final Length startPosition,
            final Length dx, final Length finishPosition)
    {
        this.timeAxis.setMinValue(startTime.si);
        this.timeAxis.setGranularity(dt.si);
        this.timeAxis.setMaxValue(finishTime.si);
        this.spaceAxis.setMinValue(startPosition.si);
        this.spaceAxis.setGranularity(dx.si);
        this.spaceAxis.setMaxValue(finishPosition.si);

        double[] timeTicks = this.timeAxis.getTicks();
        double[] spaceTicks = this.spaceAxis.getTicks();
        int nSpace = spaceTicks.length - 1;
        int nTime = timeTicks.length - 1;
        this.distance = new float[nSpace][nTime];
        this.time = new float[nSpace][nTime];

        // loop cells to update data
        for (int j = 0; j < nTime; j++)
        {
            Time tFrom = Time.instantiateSI(timeTicks[j]);
            Time tTo = Time.instantiateSI(timeTicks[j + 1]);
            // we never filter time, time always spans the entire simulation, it will contain tFrom till tTo
            for (int i = 0; i < nSpace; i++)
            {
                // only first loop with offset, later in time, none of the space was done in the previous update
                Length xFrom = Length.instantiateSI(spaceTicks[i]);
                Length xTo = Length.instantiateSI(Math.min(spaceTicks[i + 1], this.path.getTotalLength().si));

                // init cell data
                double totalDistance = 0.0;
                double totalTime = 0.0;

                // aggregate series in cell
                for (int series = 0; series < this.path.getNumberOfSeries(); series++)
                {
                    // obtain trajectories
                    List<TrajectoryGroup<?>> trajectories = new ArrayList<>();
                    for (Section<? extends LaneData<?>> section : getPath().getSections())
                    {
                        TrajectoryGroup<?> trajectoryGroup = this.samplerData.getTrajectoryGroup(section.getSource(series));
                        // when null, this is created by OtsTransceiver.Worker.dummyLaneData()
                        if (null != trajectoryGroup)
                        {
                            trajectories.add(trajectoryGroup);
                        }
                    }

                    // filter groups (lanes) that overlap with section i
                    List<TrajectoryGroup<?>> included = new ArrayList<>();
                    List<Length> xStart = new ArrayList<>();
                    List<Length> xEnd = new ArrayList<>();
                    for (int k = 0; k < trajectories.size(); k++)
                    {
                        TrajectoryGroup<?> trajectoryGroup = trajectories.get(k);
                        LaneData<?> lane = trajectoryGroup.getLane();
                        Length startDistance = this.path.getStartDistance(this.path.get(k));
                        if (startDistance.si + this.path.get(k).length().si > spaceTicks[i]
                                && startDistance.si < spaceTicks[i + 1])
                        {
                            included.add(trajectoryGroup);
                            double scale = this.path.get(k).length().si / lane.getLength().si;
                            // divide by scale, so we go from base length to section length
                            xStart.add(Length.max(xFrom.minus(startDistance).divide(scale), Length.ZERO));
                            xEnd.add(Length.min(xTo.minus(startDistance).divide(scale), trajectoryGroup.getLane().getLength()));
                        }
                    }

                    // accumulate distance and time of trajectories
                    for (int k = 0; k < included.size(); k++)
                    {
                        TrajectoryGroup<?> trajectoryGroup = included.get(k);
                        for (Trajectory<?> trajectory : trajectoryGroup.getTrajectories())
                        {
                            // for optimal operations, we first do quick-reject based on time, as by far most trajectories
                            // during the entire time span of simulation will not apply to a particular cell in space-time
                            if (GraphUtil.considerTrajectory(trajectory, tFrom, tTo))
                            {
                                // again for optimal operations, we use a space-time view only (we don't need more)
                                SpaceTimeView spaceTimeView;
                                try
                                {
                                    spaceTimeView = trajectory.getSpaceTimeView(xStart.get(k), xEnd.get(k), tFrom, tTo);
                                }
                                catch (IllegalArgumentException exception)
                                {
                                    CategoryLogger.always().debug(exception,
                                            "Unable to generate space-time view from x = {} to {} and t = {} to {}.",
                                            xStart.get(k), xEnd.get(k), tFrom, tTo);
                                    continue;
                                }
                                totalDistance += spaceTimeView.getDistance().si;
                                totalTime += spaceTimeView.getTime().si;
                            }
                        }
                    }
                }

                this.distance[i][j] = (float) totalDistance;
                this.time[i][j] = (float) totalTime;
            }
        }
    }

    // ******************************
    // *** DATA RETRIEVAL METHODS ***
    // ******************************

    /**
     * Returns the total distance data.
     * @return total distance data
     */
    public float[][] getTotalDistance()
    {
        return this.distance;
    }

    /**
     * Returns the total time data.
     * @return total time data
     */
    public float[][] getTotalTime()
    {
        return this.time;
    }

    /** {@inheritDoc} */
    @Override
    public String toString()
    {
        return "ContourDataSource [samplerData=" + this.samplerData + ", path=" + this.path + ", spaceAxis=" + this.spaceAxis
                + ", timeAxis=" + this.timeAxis + ", distance=" + Arrays.toString(this.distance) + ", time="
                + Arrays.toString(this.time) + "]";
    }

    // **********************
    // *** HELPER CLASSES ***
    // **********************

    /**
     * Class to store and determine axis information such as granularity, ticks, and range.
     * <p>
     * Copyright (c) 2013-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved.
     * <br>
     * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
     * </p>
     * @author <a href="https://github.com/averbraeck">Alexander Verbraeck</a>
     * @author <a href="https://tudelft.nl/staff/p.knoppers-1">Peter Knoppers</a>
     * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
     */
    static class Axis
    {
        /** Minimum value. */
        private double minValue;

        /** Maximum value. */
        private double maxValue;

        /** Selected granularity. */
        private double granularity;

        /** Tick values. */
        private double[] ticks;

        /**
         * Sets the minimum value.
         * @param minValue minimum value
         */
        void setMinValue(final double minValue)
        {
            if (this.minValue != minValue)
            {
                this.minValue = minValue;
                this.ticks = null;
            }
        }

        /**
         * Sets the maximum value.
         * @param maxValue double; maximum value
         */
        void setMaxValue(final double maxValue)
        {
            if (this.maxValue != maxValue)
            {
                this.maxValue = maxValue;
                this.ticks = null;
            }
        }

        /**
         * Sets the granularity.
         * @param granularity double; granularity
         */
        void setGranularity(final double granularity)
        {
            if (this.granularity != granularity)
            {
                this.granularity = granularity;
                this.ticks = null;
            }
        }

        /**
         * Returns the ticks, which are calculated if needed.
         * @return double[]; ticks
         */
        double[] getTicks()
        {
            if (this.ticks == null)
            {
                int n = getBinCount() + 1;
                this.ticks = new double[n];
                for (int i = 0; i < n; i++)
                {
                    if (i == n - 1)
                    {
                        this.ticks[i] = this.minValue + Math.min(i * this.granularity, this.maxValue);
                    }
                    else
                    {
                        this.ticks[i] = this.minValue + i * this.granularity;
                    }
                }
            }
            return this.ticks;
        }

        /**
         * Calculates the number of bins.
         * @return int; number of bins
         */
        int getBinCount()
        {
            return (int) Math.ceil((this.maxValue - this.minValue) / this.granularity);
        }

        /** {@inheritDoc} */
        @Override
        public String toString()
        {
            return "Axis [minValue=" + this.minValue + ", maxValue=" + this.maxValue + ", granularity=" + this.granularity
                    + ", ticks=" + Arrays.toString(this.ticks) + "]";
        }
    }

}
