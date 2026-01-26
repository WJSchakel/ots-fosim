package org.opentrafficsim.fosim.sim0mq.trace;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.djunits.value.vdouble.vector.PositionVector;
import org.djunits.value.vfloat.scalar.FloatAcceleration;
import org.djunits.value.vfloat.scalar.FloatDuration;
import org.djunits.value.vfloat.scalar.FloatLength;
import org.djutils.event.Event;
import org.djutils.event.EventListener;
import org.opentrafficsim.core.gtu.GtuType;
import org.opentrafficsim.core.network.Network;
import org.opentrafficsim.fosim.sim0mq.OtsTransceiver;
import org.opentrafficsim.road.gtu.lane.LaneBasedGtu;

/**
 * Listener for AccelerationChange trace file data.
 * @author wjschakel
 */
public class LaneChangeListener implements EventListener
{

    /** Network. */
    private final Network network;

    /** GTU types. */
    private final List<GtuType> gtuTypes;

    /** Data storage. */
    private final TraceData data;

    /** Previous data on each GTU. */
    private final Map<String, Integer> previousStamp = new LinkedHashMap<>();

    /** GTUs that changed lane in their most recent time step. */
    private final Set<String> justChangedLane = new LinkedHashSet<>();

    /**
     * Constructor.
     * @param network network
     * @param gtuTypes gtu types from parser
     * @param data data storage
     */
    public LaneChangeListener(final Network network, final List<GtuType> gtuTypes, final TraceData data)
    {
        this.network = network;
        this.gtuTypes = new ArrayList<>(gtuTypes);
        this.data = data;
        this.network.addListener(this, Network.GTU_ADD_EVENT);
        this.network.addListener(this, Network.GTU_REMOVE_EVENT);
    }

    @Override
    public void notify(final Event event)
    {
        if (event.getType().equals(LaneBasedGtu.LANEBASED_MOVE_EVENT))
        {
            Object[] payload = (Object[]) event.getContent();
            String id = (String) payload[0];
            String laneId = (String) payload[8];
            Integer tolane = OtsTransceiver.getLaneRowFromId(laneId);
            Integer fromln = this.previousStamp.get(id);
            // Note: toLane != fromLane might just mean the GTU left a diagonal lane, so need to consider justChangedLane
            if (fromln != null && !Objects.equals(fromln, tolane) && this.justChangedLane.contains(id))
            {
                // add row to data
                LaneBasedGtu gtu = (LaneBasedGtu) this.network.getGTU(id).get();
                FloatDuration t = FloatDuration.ofSI(this.network.getSimulator().getSimulatorTime().floatValue());
                FloatLength pos = FloatLength.ofSI(((PositionVector) payload[1]).get(0).floatValue());
                int type = this.gtuTypes.indexOf(gtu.getType());
                this.data.append(t, fromln, tolane, pos, type, Integer.valueOf(id));
            }
            this.previousStamp.put(id, tolane);
            this.justChangedLane.remove(id);
        }
        else if (event.getType().equals(Network.GTU_ADD_EVENT))
        {
            LaneBasedGtu gtu = (LaneBasedGtu) this.network.getGTU((String) event.getContent()).get();
            gtu.addListener(this, LaneBasedGtu.LANEBASED_MOVE_EVENT);
            gtu.addListener(this, LaneBasedGtu.LANE_CHANGE_EVENT);
        }
        else if (event.getType().equals(Network.GTU_REMOVE_EVENT))
        {
            LaneBasedGtu gtu = (LaneBasedGtu) this.network.getGTU((String) event.getContent()).get();
            gtu.removeListener(this, LaneBasedGtu.LANEBASED_MOVE_EVENT);
            gtu.removeListener(this, LaneBasedGtu.LANE_CHANGE_EVENT);
            this.previousStamp.remove(gtu.getId());
            this.justChangedLane.remove(gtu.getId());
        }
        else if (event.getType().equals(LaneBasedGtu.LANE_CHANGE_EVENT))
        {
            this.justChangedLane.add((String) ((Object[]) event.getContent())[0]);
        }
    }

    /**
     * Stores previous data on GTU.
     * @param acceleration acceleration
     * @param lane lane number
     */
    record GtuStamp(FloatAcceleration acceleration, Integer lane)
    {
    };

}
