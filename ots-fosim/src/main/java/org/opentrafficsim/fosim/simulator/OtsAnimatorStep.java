package org.opentrafficsim.fosim.simulator;

import java.io.Serializable;

import org.djunits.value.vdouble.scalar.Duration;
import org.opentrafficsim.core.dsol.OtsAnimator;

import nl.tudelft.simulation.dsol.formalisms.eventscheduling.SimEventInterface;
import nl.tudelft.simulation.dsol.simtime.SimTime;
import nl.tudelft.simulation.dsol.simulators.ReplicationState;
import nl.tudelft.simulation.dsol.simulators.RunState;
import nl.tudelft.simulation.dsol.simulators.SimulatorInterface;

/**
 * This class extends {@code OtsAnimator} and overrides the {@code DevsRealTimeAnimator.run()} method. This method is a copy,
 * except that no new animation thread is created at every call.
 * <p>
 * Copyright (c) 2023-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
 */
public class OtsAnimatorStep extends OtsAnimator
{

    /** */
    private static final long serialVersionUID = 20240315L;

    /** Animation thread. */
    private AnimationThread animationThread = null;

    /**
     * Constructor.
     * @param simulatorId Serializable; simulator id.
     */
    public OtsAnimatorStep(final Serializable simulatorId)
    {
        super(simulatorId);
    }

    /** {@inheritDoc} */
    @Override
    public void run()
    {
        if (isAnimation() && this.animationThread == null)
        {
            this.animationThread = new AnimationThread(this);
            this.animationThread.start();
        }

        // set the run flag semaphore to signal to startImpl() that the run method has started
        this.runflag = true;

        /* Baseline point for the wallclock time. */
        long wallTime0 = System.currentTimeMillis();

        /* Baseline point for the simulator time. */
        Duration simTime0 = SimTime.copy(getSimulatorTime());

        /* Speed factor is simulation seconds per 1 wallclock second. */
        double currentSpeedFactor = getSpeedFactor();

        /* wall clock milliseconds per 1 simulation clock millisecond. */
        double msec1 = simulatorTimeForWallClockMillis(1.0).doubleValue();

        while (!isStoppingOrStopped() && !this.eventList.isEmpty() && this.simulatorTime.compareTo(this.runUntilTime) <= 0)
        {
            // check if speedFactor has changed. If yes: re-baseline.
            if (currentSpeedFactor != getSpeedFactor())
            {
                wallTime0 = System.currentTimeMillis();
                simTime0 = SimTime.copy(this.simulatorTime);
                currentSpeedFactor = getSpeedFactor();
            }

            // check if we are behind; wantedSimTime is the needed current time on the wall-clock
            double wantedSimTime = (System.currentTimeMillis() - wallTime0) * msec1 * currentSpeedFactor;
            double simTimeSinceBaseline = SimTime.minus(this.simulatorTime, simTime0).doubleValue();

            if (simTimeSinceBaseline < wantedSimTime)
            {
                // we are behind
                if (!isCatchup())
                {
                    // if no catch-up: re-baseline.
                    wallTime0 = System.currentTimeMillis();
                    simTime0 = SimTime.copy(this.simulatorTime);
                }
                else
                {
                    // jump to the required wall-clock related time or to the time of the next event, or to the runUntil time,
                    // whichever comes first
                    synchronized (super.semaphore)
                    {
                        Duration delta = simulatorTimeForWallClockMillis((wantedSimTime - simTimeSinceBaseline) / msec1);
                        Duration absSyncTime = SimTime.plus(this.simulatorTime, delta);
                        Duration eventOrUntilTime = this.eventList.first().getAbsoluteExecutionTime();
                        if (this.runUntilTime.compareTo(eventOrUntilTime) < 0)
                        {
                            eventOrUntilTime = this.runUntilTime;
                        }
                        if (absSyncTime.compareTo(eventOrUntilTime) < 0)
                        {
                            this.simulatorTime = SimTime.copy(absSyncTime);
                            fireUnverifiedTimedEvent(SimulatorInterface.TIME_CHANGED_EVENT, null, this.simulatorTime);
                        }
                        else
                        {
                            this.simulatorTime = SimTime.copy(eventOrUntilTime);
                            fireUnverifiedTimedEvent(SimulatorInterface.TIME_CHANGED_EVENT, null, this.simulatorTime);
                        }
                    }
                }
            }

            // peek at the first event and determine the time difference relative to RT speed; that determines
            // how long we have to wait.
            SimEventInterface<Duration> nextEvent = this.eventList.first();
            Duration nextEventOrUntilTime = nextEvent.getAbsoluteExecutionTime();
            boolean isRunUntil = false;
            if (this.runUntilTime.compareTo(nextEventOrUntilTime) < 0)
            {
                nextEventOrUntilTime = this.runUntilTime;
                isRunUntil = true;
            }
            double wallMillisNextEventSinceBaseline =
                    (nextEventOrUntilTime.doubleValue() - simTime0.doubleValue()) / (msec1 * currentSpeedFactor);

            // wallMillisNextEventSinceBaseline gives the number of milliseconds on the wall clock since baselining for the
            // expected execution time of the next event on the event list .
            if (wallMillisNextEventSinceBaseline >= (System.currentTimeMillis() - wallTime0))
            {
                while (wallMillisNextEventSinceBaseline > System.currentTimeMillis() - wallTime0)
                {
                    try
                    {
                        Thread.sleep(this.getUpdateMsec());
                    }
                    catch (InterruptedException ie)
                    {
                        // do nothing
                        ie = null;
                        Thread.interrupted(); // clear the flag
                    }

                    // did we stop running between events?
                    if (isStoppingOrStopped())
                    {
                        wallMillisNextEventSinceBaseline = 0.0; // jump out of the while loop for sleeping
                        break;
                    }

                    // check if speedFactor has changed. If yes: rebaseline. Try to avoid a jump.
                    if (currentSpeedFactor != getSpeedFactor())
                    {
                        // rebaseline
                        wallTime0 = System.currentTimeMillis();
                        simTime0 = SimTime.copy(this.simulatorTime);
                        currentSpeedFactor = getSpeedFactor();
                        wallMillisNextEventSinceBaseline =
                                (nextEventOrUntilTime.doubleValue() - simTime0.doubleValue()) / (msec1 * currentSpeedFactor);
                    }

                    // check if an event has been inserted. In a real-time situation this can be done by other threads
                    if (!nextEvent.equals(this.eventList.first())) // event inserted by a thread...
                    {
                        nextEvent = this.eventList.first();
                        nextEventOrUntilTime = nextEvent.getAbsoluteExecutionTime();
                        isRunUntil = false;
                        if (this.runUntilTime.compareTo(nextEventOrUntilTime) < 0)
                        {
                            nextEventOrUntilTime = this.runUntilTime;
                            isRunUntil = true;
                        }
                        wallMillisNextEventSinceBaseline =
                                (nextEventOrUntilTime.doubleValue() - simTime0.doubleValue()) / (msec1 * currentSpeedFactor);
                    }

                    // make a small time step for the animation during wallclock waiting, but never beyond the next event
                    // time. Changed 2019-04-30: this is now recalculated based on latest system time after the 'sleep'.
                    synchronized (super.semaphore)
                    {
                        Duration nextEventSimTime = SimTime.copy(nextEventOrUntilTime);
                        Duration deltaToWall0inSimTime =
                                simulatorTimeForWallClockMillis((System.currentTimeMillis() - wallTime0) * currentSpeedFactor);
                        Duration currentWallSimTime = SimTime.plus(simTime0, deltaToWall0inSimTime);
                        if (nextEventSimTime.compareTo(currentWallSimTime) < 0)
                        {
                            if (nextEventSimTime.compareTo(this.simulatorTime) > 0) // don't go back in time
                            {
                                this.simulatorTime = SimTime.copy(nextEventSimTime);
                                fireUnverifiedTimedEvent(SimulatorInterface.TIME_CHANGED_EVENT, null, this.simulatorTime);
                            }
                            wallMillisNextEventSinceBaseline = 0.0; // force breakout of the loop
                        }
                        else
                        {
                            if (currentWallSimTime.compareTo(this.simulatorTime) > 0) // don't go back in time
                            {
                                this.simulatorTime = SimTime.copy(currentWallSimTime);
                                fireUnverifiedTimedEvent(SimulatorInterface.TIME_CHANGED_EVENT, null, this.simulatorTime);
                            }
                        }
                    }
                }
            }

            // only execute an event if we are still running, and if we do not 'run until'...
            if (isRunUntil)
            {
                this.simulatorTime = nextEventOrUntilTime;
            }
            else if (!isStoppingOrStopped())
            {
                synchronized (super.semaphore)
                {
                    if (nextEvent.getAbsoluteExecutionTime().compareTo(this.simulatorTime) != 0)
                    {
                        fireUnverifiedTimedEvent(SimulatorInterface.TIME_CHANGED_EVENT, null,
                                nextEvent.getAbsoluteExecutionTime());
                    }
                    this.simulatorTime = SimTime.copy(nextEvent.getAbsoluteExecutionTime());

                    // carry out all events scheduled on this simulation time, as long as we are still running.
                    while (!isStoppingOrStopped() && !this.eventList.isEmpty()
                            && nextEvent.getAbsoluteExecutionTime().compareTo(this.simulatorTime) == 0)
                    {
                        nextEvent = this.eventList.removeFirst();
                        try
                        {
                            nextEvent.execute();
                            if (this.eventList.isEmpty())
                            {
                                this.simulatorTime = SimTime.copy(this.runUntilTime);
                                this.runState = RunState.STOPPING;
                                this.replicationState = ReplicationState.ENDING;
                                fireUnverifiedTimedEvent(SimulatorInterface.TIME_CHANGED_EVENT, null, this.simulatorTime);
                                break;
                            }
                            int cmp = this.eventList.first().getAbsoluteExecutionTime().compareTo(this.runUntilTime);
                            if ((cmp == 0 && !this.runUntilIncluding) || cmp > 0)
                            {
                                this.simulatorTime = SimTime.copy(this.runUntilTime);
                                this.runState = RunState.STOPPING;
                                fireUnverifiedTimedEvent(SimulatorInterface.TIME_CHANGED_EVENT, null, this.simulatorTime);
                                break;
                            }
                        }
                        catch (Exception exception)
                        {
                            handleSimulationException(exception);
                        }
                        if (!this.eventList.isEmpty())
                        {
                            // peek at next event for while loop.
                            nextEvent = this.eventList.first();
                            nextEventOrUntilTime = nextEvent.getAbsoluteExecutionTime();
                            isRunUntil = false;
                            if (this.runUntilTime.compareTo(nextEventOrUntilTime) < 0)
                            {
                                nextEventOrUntilTime = this.runUntilTime;
                                isRunUntil = true;
                            }
                        }
                    }
                }
            }
        }
        fireTimedEvent(SimulatorInterface.TIME_CHANGED_EVENT, null, this.simulatorTime);

        if (isAnimation())
        {
            updateAnimation();
        }
        else
        {
            // animation status might have changed
            this.animationThread.stopAnimation();
            this.animationThread = null;
        }
    }

}
