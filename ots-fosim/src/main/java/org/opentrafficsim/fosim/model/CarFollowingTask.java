package org.opentrafficsim.fosim.model;

import org.djunits.value.vdouble.scalar.Duration;
import org.opentrafficsim.base.OtsRuntimeException;
import org.opentrafficsim.base.parameters.ParameterException;
import org.opentrafficsim.base.parameters.ParameterTypeDuration;
import org.opentrafficsim.base.parameters.constraint.NumericConstraint;
import org.opentrafficsim.core.gtu.plan.operational.OperationalPlanException;
import org.opentrafficsim.road.gtu.lane.LaneBasedGtu;
import org.opentrafficsim.road.gtu.lane.perception.LanePerception;
import org.opentrafficsim.road.gtu.lane.perception.PerceptionCollectable;
import org.opentrafficsim.road.gtu.lane.perception.RelativeLane;
import org.opentrafficsim.road.gtu.lane.perception.categories.neighbors.NeighborsPerception;
import org.opentrafficsim.road.gtu.lane.perception.categories.neighbors.TaskHeadwayCollector;
import org.opentrafficsim.road.gtu.lane.perception.mental.AbstractTask;
import org.opentrafficsim.road.gtu.lane.perception.object.PerceivedGtu;

@Deprecated
public class CarFollowingTask extends AbstractTask
{

    /** Car-following task parameter. */
    public static final ParameterTypeDuration HEXP = new ParameterTypeDuration("Hexp",
            "Exponential decay of car-following task by headway.", Duration.ofSI(4.0), NumericConstraint.POSITIVE);

    /**
     * Constructor.
     */
    public CarFollowingTask()
    {
        super("car-following");
    }

    @Override
    public double calculateTaskDemand(final LanePerception perception) throws ParameterException
    {
        try
        {
            NeighborsPerception neighbors = perception.getPerceptionCategory(NeighborsPerception.class);
            PerceptionCollectable<PerceivedGtu, LaneBasedGtu> leaders = neighbors.getLeaders(RelativeLane.CURRENT);
            Duration headway = leaders.collect(new TaskHeadwayCollector(perception.getGtu().getSpeed()));
            return headway == null ? 0.0 : Math.exp(-headway.si / perception.getGtu().getParameters().getParameter(HEXP).si);
        }
        catch (OperationalPlanException ex)
        {
            throw new OtsRuntimeException("No NeighborsPerception");
        }
    }
}
