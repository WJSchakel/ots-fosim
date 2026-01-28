package org.opentrafficsim.fosim.parser;

import org.opentrafficsim.base.parameters.ParameterException;
import org.opentrafficsim.base.parameters.ParameterTypeDouble;
import org.opentrafficsim.base.parameters.Parameters;
import org.opentrafficsim.core.gtu.Stateless;
import org.opentrafficsim.core.gtu.plan.operational.OperationalPlanException;
import org.opentrafficsim.road.gtu.lane.perception.LanePerception;
import org.opentrafficsim.road.gtu.lane.perception.RelativeLane;
import org.opentrafficsim.road.gtu.lane.tactical.following.CarFollowingModel;
import org.opentrafficsim.road.gtu.lane.tactical.util.lmrs.Desire;
import org.opentrafficsim.road.gtu.lane.tactical.util.lmrs.LmrsParameters;
import org.opentrafficsim.road.gtu.lane.tactical.util.lmrs.VoluntaryIncentive;

/**
 * Temporary implementation of keep-right incentive due to issue https://github.com/averbraeck/opentrafficsim/issues/266.
 * @author wjschakel
 */
public class FosIncentiveKeep implements VoluntaryIncentive, Stateless<FosIncentiveKeep>
{

    /** Free lane change threshold parameter type. */
    protected static final ParameterTypeDouble DFREE = LmrsParameters.DFREE;

    /** Singleton instance. */
    public static final FosIncentiveKeep SINGLETON = new FosIncentiveKeep();

    /**
     * Constructor.
     */
    private FosIncentiveKeep()
    {
        //
    }

    @Override
    public FosIncentiveKeep get()
    {
        return SINGLETON;
    }

    @Override
    public Desire determineDesire(final Parameters parameters, final LanePerception perception,
            final CarFollowingModel carFollowingModel, final Desire mandatoryDesire, final Desire voluntaryDesire)
            throws ParameterException, OperationalPlanException
    {
        if (mandatoryDesire.right() < 0 || voluntaryDesire.right() < 0
                || !perception.getLaneStructure().exists(RelativeLane.RIGHT)
                // This additional check makes sure the lane is sensible, circumvents:
                // (https://github.com/averbraeck/opentrafficsim/issues/266)
                || perception.getLaneStructure().getCrossSectionRecords(RelativeLane.RIGHT).isEmpty())
        {
            // no desire to go right if more dominant incentives provide a negative desire to go right
            return new Desire(0, 0);
        }
        // keep right with dFree
        return new Desire(0, parameters.getParameter(DFREE));
    }

    @Override
    public String toString()
    {
        return "FosIncentiveKeep";
    }
}
