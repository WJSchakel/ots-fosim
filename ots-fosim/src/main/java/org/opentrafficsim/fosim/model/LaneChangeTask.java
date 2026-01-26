package org.opentrafficsim.fosim.model;

import org.opentrafficsim.base.parameters.ParameterException;
import org.opentrafficsim.base.parameters.Parameters;
import org.opentrafficsim.road.gtu.lane.perception.LanePerception;
import org.opentrafficsim.road.gtu.lane.perception.mental.AbstractTask;
import org.opentrafficsim.road.gtu.lane.tactical.util.lmrs.LmrsParameters;

/**
 * Lane change task. Task demand is equal to the maximum of <i>d<sub>left</sub></i> and <i>d<sub>right</sub></i>.
 * <p>
 * Copyright (c) 2023-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
 */
@Deprecated
public class LaneChangeTask extends AbstractTask
{

    /**
     * Constructor.
     */
    public LaneChangeTask()
    {
        super("lane-changing");
    }

    @Override
    public double calculateTaskDemand(final LanePerception perception) throws ParameterException
    {
        Parameters parameters = perception.getGtu().getParameters();
        return Math.max(0.0,
                Math.max(parameters.getParameter(LmrsParameters.DLEFT), parameters.getParameter(LmrsParameters.DRIGHT)));
    }

}
