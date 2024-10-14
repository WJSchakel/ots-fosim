package org.opentrafficsim.fosim.sim0mq;

import java.util.LinkedHashSet;
import java.util.Set;

import org.djunits.value.vdouble.scalar.Duration;
import org.djunits.value.vdouble.scalar.Speed;
import org.djunits.value.vdouble.scalar.Time;
import org.opentrafficsim.fosim.FosDetector;
import org.opentrafficsim.road.network.RoadNetwork;

/**
 * Defines the stop criterion during a batch run.
 * @author wjschakel
 */
public class StopCriterion
{

    /** Network. */
    private final RoadNetwork network;

    /** Fosim, PLM or QDC. */
    private final DetectionType stopType;

    /** Threshold speed below which congestion is recognized. */
    private final Speed threshold;

    /** Additional time in QDC method. */
    private final Duration additionalTime;

    /** Detectors to check. */
    private final Set<FosDetector> detectors = new LinkedHashSet<>();

    /** Previous period index. */
    private int prevPeriod = -1;

    /** Initial period when speed dropped below threshold. */
    private int initialTriggerPeriod = Integer.MAX_VALUE;

    /** Initial time when speed dropped below threshold. */
    private Time initialTriggerTime = null;

    /**
     * Constructor.
     * @param network network.
     * @param detectionType FOSIM, PLM or QDC.
     * @param threshold threshold speed below which congestion is recognized.
     * @param fromLane from lane to check detectors.
     * @param toLane to lane to check detectors.
     * @param additionalTime additional time in QDC method.
     */
    public StopCriterion(final RoadNetwork network, final DetectionType detectionType, final Speed threshold, final int fromLane,
            final int toLane, final Duration additionalTime)
    {
        this.network = network;
        this.stopType = detectionType;
        this.threshold = threshold;
        this.additionalTime = additionalTime;
        // Find detectors
        for (FosDetector detector : network.getObjectMap(FosDetector.class).values())
        {
            String id = detector.getId();
            String[] crossSeciontAndLane = id.split("_");
            int lane = Integer.valueOf(crossSeciontAndLane[1]);
            if (fromLane <= lane && lane <= toLane)
            {
                detectors.add(detector);
            }
        }
    }

    /**
     * Returns whether the simulation can stop as the stop criterion has been reached. This will be true when either of the
     * following:
     * <ul>
     * <li>The simulation time has passed.</li>
     * <li>There are no detectors.</li>
     * <li>The stop type is <i>PLM</i> and:
     * <ul>
     * <li>the speed ({@code 1 / (sum(1/speed)/count)}) is below the threshold speed at any detector.</li>
     * </ul>
     * </li>
     * <li>The stop type is <i>Fosim</i> and:
     * <ul>
     * <li>a full detector period has passed after the speed has dropped below the threshold speed at any detector.</li>
     * </ul>
     * </li>
     * <li>The stop type is <i>QDC</i> and:
     * <ul>
     * <li>a full detector period has passed after the speed has dropped below the threshold speed at any detector, and</li>
     * <li>a minimum of the additional time has passed since the speed dropped below the threshold, and</li>
     * <li>the speed has been below the threshold during the additional time and period at at least 1 detector.</li>
     * </ul>
     * </li>
     * </ul>
     * @return whether the simulation can stop as the stop criterion has been reached.
     */
    public boolean canStop()
    {
        if (this.network.getSimulator().getSimulatorAbsTime().si > this.network.getSimulator().getReplication().getEndTime().si)
        {
            return true; // end of simulation reached
        }
        if (this.detectors.isEmpty())
        {
            return true; // there are no detectors...
        }

        // check whether the next period has been reached
        int period = this.detectors.iterator().next().getCurrentPeriod() - 1;
        if (period <= this.prevPeriod)
        {
            return false;
        }
        this.prevPeriod = period;

        // for method Fosim one additional period needs to have been simulated
        if (this.stopType.equals(DetectionType.FOSIM) && this.initialTriggerPeriod < period)
        {
            return true;
        }

        boolean congestionFound = false;
        for (FosDetector detector : this.detectors)
        {
            if (1.0 / (detector.getSumReciprocalSpeed(period) / detector.getCount(period)) < this.threshold.si)
            {
                if (this.stopType.equals(DetectionType.PLM))
                {
                    return true;
                }
                if (this.stopType.equals(DetectionType.FOSIM))
                {
                    // need one more period to calculate capacity
                    this.initialTriggerPeriod = period;
                }
                else if (this.stopType.equals(DetectionType.QDC))
                {
                    congestionFound = true;
                    if (this.initialTriggerTime == null)
                    {
                        // need more time and an additional period to check whether congestion is robust
                        this.initialTriggerPeriod = period;
                        this.initialTriggerTime = this.network.getSimulator().getSimulatorAbsTime();
                        return false;
                    }
                    else if (this.network.getSimulator().getSimulatorAbsTime().si
                            - this.initialTriggerTime.si >= this.additionalTime.si && this.initialTriggerPeriod < period)
                    {
                        return true;
                    }
                    break;
                }
            }
        }

        // for method QDC, reset if in any period no congestion was found
        if (this.stopType.equals(DetectionType.QDC) && !congestionFound)
        {
            this.initialTriggerPeriod = Integer.MAX_VALUE;
            this.initialTriggerTime = null;
        }

        return false;
    }
    
    /**
     * Congestion detection type.
     * @author wjschakel
     */
    public enum DetectionType
    {
        /** Congestion triggers directly. */
        PLM,
        
        /** Congestion during at least 1 additional period. */
        FOSIM,
        
        /** Congestion during at least 1 additional period and additional time. */
        QDC;
    }

}
