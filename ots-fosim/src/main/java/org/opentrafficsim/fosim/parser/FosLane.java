package org.opentrafficsim.fosim.parser;

import org.djunits.value.vdouble.scalar.Length;
import org.djunits.value.vdouble.scalar.Speed;
import org.djutils.exceptions.Throw;
import org.opentrafficsim.road.network.lane.Lane;

/**
 * Parsed lane info. The interpretation if this has been reverse-engineered by setting various properties in a .fos file.
 * <p>
 * Copyright (c) 2023-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
 */
class FosLane
{
    /**
     * Type:
     * <ul>
     * <li>u: unused</li>
     * <li>l: can only change left</li>
     * <li>r: can only change right</li>
     * <li>c: can change in both directions</li>
     * <li>s: single lane (no lane change in either direction)</li>
     * <li>L: must go left (striped area)</li>
     * <li>R: must go right (striped area)</li>
     * <li>X: should not be here (beyond striped area)</li>
     * </ul>
     */
    public final String type;

    /**
     * Taper:
     * <ul>
     * <li>- no</li>
     * <li>&gt; merge taper</li>
     * <li>&lt; diverge taper</li>
     * <li>/ merge taper adjacent</li>
     * <li>\ diverge taper adjacent</li>
     */
    public final String taper;

    /**
     * Lane number out; for diagonal sections. For both a merge taper and its adjacent lane, this is the same lane as the
     * merge taper. For diverge tapers, these values are not affected.
     */
    public final int laneOut;

    /** No overtaking trucks. */
    public final boolean noOvertakingTrucks;

    /** Speed suppression. */
    public final double speedSuppression;

    /** Speed limit. */
    public final Speed speedLimit;

    /** Slope (unit unknown, not used in FOSIM). */
    public final double slope;

    /** All lane change required (function unknown, not used in FOSIM). */
    public final boolean allLaneChangeRequired;

    /** Lane width. */
    public final Length laneWidth;

    /** Road works. */
    public final boolean roadWorks;

    /** Trajectory control. */
    public final boolean trajectoryControl;
    
    /** Lane object created. */
    private Lane lane;

    /**
     * Switched lane.
     * <ul>
     * <li>0: none</li>
     * <li>&lt;0: rush-hour lane, must change left</li>
     * <li>&gt;0: plus lane, must change left</li>
     * <li>abs value: index of area times</li>
     * </ul>
     * area time).
     */
    private final int switchedLane;

    /**
     * Parses the information of a single lane in a single section.
     * @param string string describing a single lane in a single section.
     */
    public FosLane(final String string)
    {
        // example string: r,-, 0, 0,1.0,100.0,0.000,0,3.50,0,0[,-1]
        // documented comma-separated fields do not match actual fields, nor in type, nor in number
        String[] fields = FosParser.splitAndTrimString(string, ",");
        this.type = fields[0];
        this.taper = fields[1];
        this.laneOut = Integer.parseInt(fields[2]);
        this.noOvertakingTrucks = fields[3].equals("1"); // "0" otherwise
        this.speedSuppression = Double.parseDouble(fields[4]);
        this.speedLimit = Speed.of(Double.parseDouble(fields[5]), "km/h");
        this.slope = Double.parseDouble(fields[6]);
        this.allLaneChangeRequired = fields[7].equals("1"); // "0" otherwise
        this.laneWidth = Length.instantiateSI(Double.parseDouble(fields[8]));
        this.roadWorks = fields[9].equals("1"); // "0" otherwise
        this.trajectoryControl = fields[10].equals("1"); // "0" otherwise
        this.switchedLane = fields.length > 11 ? Integer.parseInt(fields[11]) : 0;
    }

    /**
     * Whether vehicles may change left.
     * @param stripedAreas striped areas setting.
     * @return whether vehicles may change left.
     */
    public boolean canChangeLeft(final boolean stripedAreas)
    {
        return (this.type.equals("l") || this.type.equals("c") || (this.type.equals("L") && stripedAreas));
    }

    /**
     * Whether vehicles may change right.
     * @param stripedAreas striped areas setting.
     * @return whether vehicles may change right.
     */
    public boolean canChangeRight(final boolean stripedAreas)
    {
        return (this.type.equals("r") || this.type.equals("c") || (this.type.equals("R") && stripedAreas));
    }

    /**
     * Whether this should be considered a shoulder.
     * @param stripedAreas striped areas setting.
     * @return whether this should be considered a shoulder.
     */
    public boolean isShoulder(final boolean stripedAreas)
    {
        return (this.type.equals("L") && !stripedAreas) || (this.type.equals("R") && !stripedAreas) || this.type.equals("X");
    }
    
    /**
     * Whether this lane is switched (rush-hour lane or plus lane).
     * @return whether this lane is switched (rush-hour lane or plus lane).
     */
    public boolean isSwitched()
    {
        return this.switchedLane != 0;
    }

    /**
     * Returns the number of the switched are times for this lane, if it is switched.
     * @return number of the switched are times for this lane.
     */
    public int switchedAreaTimesNumber()
    {
        Throw.when(!isSwitched(), RuntimeException.class, "Requesting switch times of lane that is not switched.");
        return Math.abs(this.switchedLane);
    }
    
    /**
     * Get the lane.
     * @return lane.
     */
    public Lane getLane()
    {
        return this.lane;
    }

    /**
     * Set the lane.
     * @param lane lane.
     */
    public void setLane(final Lane lane)
    {
        this.lane = lane;
    }

    /** {@inheritDoc} */
    @Override
    public String toString()
    {
        return "FosLane " + this.type + this.taper + ", " + this.speedLimit;
    }
}