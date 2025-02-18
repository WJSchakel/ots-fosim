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

    /** Additional time in QDC method. */
    private static final Duration QDC_TIME = Duration.instantiateSI(600.0);
    
    /** Network. */
    private final RoadNetwork network;

    /** Fosim, PLM or QDC. */
    private final DetectionType stopType;

    /** Threshold speed below which congestion is recognized. */
    private final Speed threshold;

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
     * @param fromLane from lane to check detectors.
     * @param toLane to lane to check detectors.
     * @param detector detector cross section number
     * @param threshold threshold speed below which congestion is recognized.
     */
    public StopCriterion(final RoadNetwork network, final DetectionType detectionType, final int fromLane, final int toLane,
            int detector, final Speed threshold)
    {
        this.network = network;
        this.stopType = detectionType;
        this.threshold = threshold;
        // Find detectors
        for (FosDetector det : network.getObjectMap(FosDetector.class).values())
        {
            String id = det.getId();
            String[] crossSeciontAndLane = id.split("_");
            int detNum = Integer.valueOf(crossSeciontAndLane[0]);
            int lane = Integer.valueOf(crossSeciontAndLane[1]);
            if (fromLane <= lane && lane <= toLane && (detector < 0 || detector == detNum))
            {
                this.detectors.add(det);
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
    public BatchStatus canStop()
    {
        
        // TODO: consider cross-section, rather than individual detectors
        
        if (this.detectors.isEmpty())
        {
            return BatchStatus.STOPPED; // there are no detectors...
        }

        // check whether the next period has been reached
        int period = this.detectors.iterator().next().getCurrentPeriod() - 1;
        if (period <= this.prevPeriod)
        {
            return BatchStatus.RUNNING;
        }
        this.prevPeriod = period;

        // for method Fosim one additional period needs to have been simulated
        if (this.stopType.equals(DetectionType.FOSIM) && this.initialTriggerPeriod < period)
        {
            return BatchStatus.TRIGGERED;
        }

        for (FosDetector detector : this.detectors)
        {
            if (1.0 / (detector.getSumReciprocalSpeed(period) / detector.getCount(period)) < this.threshold.si)
            {
                if (this.stopType.equals(DetectionType.PLM))
                {
                    return BatchStatus.TRIGGERED;
                }
                if (this.stopType.equals(DetectionType.FOSIM))
                {
                    System.out.println("First trigger in period " + period);
                    // need one more period to calculate capacity
                    this.initialTriggerPeriod = period;
                }
                else if (this.stopType.equals(DetectionType.QDC))
                {
                    if (this.initialTriggerTime == null)
                    {
                        // need more time and an additional period to check whether congestion is robust
                        this.initialTriggerTime = this.network.getSimulator().getSimulatorAbsTime();
                    }
                    else if (this.network.getSimulator().getSimulatorAbsTime().si - this.initialTriggerTime.si >= QDC_TIME.si)
                    {
                        return BatchStatus.TRIGGERED;
                    }
                    return BatchStatus.RUNNING;
                }
            }
        }

        // for method QDC, reset if in any period no congestion was found
        if (this.stopType.equals(DetectionType.QDC))
        {
            this.initialTriggerTime = null;
        }

        return BatchStatus.RUNNING;
    }

    /**
     * Congestion detection type.
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
    
    /**
     * Return status of batch step message.
     */
    public enum BatchStatus
    {
        /** Simulation still running. */
        RUNNING,
        
        /** Stop criterion met. */
        TRIGGERED,
        
        /** E.g. no detectors, end of simulation reached. */
        STOPPED;
    }

}
