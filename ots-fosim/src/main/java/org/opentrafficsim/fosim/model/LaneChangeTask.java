package org.opentrafficsim.fosim.model;

import org.opentrafficsim.base.parameters.ParameterException;
import org.opentrafficsim.base.parameters.Parameters;
import org.opentrafficsim.core.gtu.GtuException;
import org.opentrafficsim.road.gtu.lane.LaneBasedGtu;
import org.opentrafficsim.road.gtu.lane.perception.LanePerception;
import org.opentrafficsim.road.gtu.lane.perception.mental.AbstractTask;
import org.opentrafficsim.road.gtu.lane.tactical.util.lmrs.LmrsParameters;

/**
 * Lane change task. Task demand is equal to the maximum of <i>d<sub>left</sub></i> and <i>d<sub>right</sub></i>.
 * @author wjschakel
 */
public class LaneChangeTask extends AbstractTask
{

    /** 
     * Constructor.
     */
    public LaneChangeTask()
    {
        super("lane-changing");
    }

    /** {@inheritDoc} */
    @Override
    public double calculateTaskDemand(final LanePerception perception, final LaneBasedGtu gtu,
            final Parameters parameters) throws ParameterException, GtuException
    {
        return Math.max(0.0,
                Math.max(parameters.getParameter(LmrsParameters.DLEFT), parameters.getParameter(LmrsParameters.DRIGHT)));
    }

}
