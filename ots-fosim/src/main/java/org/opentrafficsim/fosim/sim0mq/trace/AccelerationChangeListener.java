package org.opentrafficsim.fosim.sim0mq.trace;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.djunits.value.vdouble.scalar.Acceleration;
import org.djunits.value.vdouble.scalar.Speed;
import org.djunits.value.vdouble.vector.PositionVector;
import org.djunits.value.vfloat.scalar.FloatAcceleration;
import org.djunits.value.vfloat.scalar.FloatDuration;
import org.djunits.value.vfloat.scalar.FloatLength;
import org.djunits.value.vfloat.scalar.FloatSpeed;
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
public class AccelerationChangeListener implements EventListener
{

    /** Network. */
    private final Network network;

    /** GTU types. */
    private List<GtuType> gtuTypes;

    /** Data storage. */
    private final TraceData data;

    /** Previous data on each GTU. */
    private final Map<String, GtuStamp> previousStamp = new LinkedHashMap<>();

    /** GTUs that changed lane in their most recent time step. */
    private final Set<String> justChangedLane = new LinkedHashSet<>();

    /**
     * Constructor.
     * @param network network
     * @param gtuTypes gtu types from parser
     * @param data data storage
     */
    public AccelerationChangeListener(final Network network, final List<GtuType> gtuTypes, final TraceData data)
    {
        this.network = network;
        this.gtuTypes = gtuTypes;
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
            long toA10 = Math.round(((Acceleration) payload[4]).si * 10.0);
            String laneId = (String) payload[8];
            Integer tolane = OtsTransceiver.getLaneRowFromId(laneId);
            GtuStamp stamp = this.previousStamp.get(id);
            // Note: toLane != fromLane might just mean the GTU left a diagonal lane, so need to consider justChangedLane
            if (stamp != null && (stamp.acceleration10() != toA10)
                    || !Objects.equals(stamp.lane(), tolane) && this.justChangedLane.contains(id))
            {
                // add row to data
                LaneBasedGtu gtu = (LaneBasedGtu) this.network.getGTU(id).get();
                FloatDuration t = FloatDuration.ofSI(this.network.getSimulator().getSimulatorTime().floatValue());
                int fromln = stamp.lane();
                long fromA10 = stamp.acceleration10();
                FloatLength pos = FloatLength.ofSI(((PositionVector) payload[1]).get(0).floatValue());
                FloatSpeed v = FloatSpeed.ofSI(((Speed) payload[3]).floatValue());
                int type = this.gtuTypes.indexOf(gtu.getType());
                this.data.append(t, fromln, tolane, rounded(fromA10), rounded(toA10), pos, v, type, Integer.valueOf(id));
            }
            this.previousStamp.put(id, new GtuStamp(toA10, tolane));
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
     * Returns the acceleration rounded to the nearest 0.1 m/s/s.
     * @param acceleration10 acceleration * 10
     * @return acceleration rounded to the nearest 0.1 m/s/s
     */
    private static FloatAcceleration rounded(long acceleration10)
    {
        return FloatAcceleration.ofSI(((float) acceleration10) / 10.0f);
    }

    /**
     * Stores previous data on GTU.
     * @param acceleration10 acceleration * 10
     * @param lane lane number
     */
    record GtuStamp(Long acceleration10, Integer lane)
    {
    };

}
