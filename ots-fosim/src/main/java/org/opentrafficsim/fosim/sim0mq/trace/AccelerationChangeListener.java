package org.opentrafficsim.fosim.sim0mq.trace;

import java.rmi.RemoteException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import org.opentrafficsim.road.gtu.lane.LaneBasedGtu;

/**
 * Listener for AccelerationChange trace file data.
 * @author wjschakel
 */
public class AccelerationChangeListener implements EventListener
{

    /** */
    private static final long serialVersionUID = 1L;

    /** Network. */
    private final Network network;

    /** GTU types. */
    private List<GtuType> gtuTypes;

    /** Data storage. */
    private final TraceData data;

    /** Previous data on each GTU. */
    private final Map<String, GtuStamp> previousStamp = new LinkedHashMap<>();

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
    public void notify(final Event event) throws RemoteException
    {
        if (event.getType().equals(LaneBasedGtu.LANEBASED_MOVE_EVENT))
        {
            // TODO: no lane change when leaving diagonal lane?
            Object[] payload = (Object[]) event.getContent();
            String id = (String) payload[0];
            FloatAcceleration toA = FloatAcceleration.instantiateSI(((Acceleration) payload[4]).floatValue());
            String laneId = (String) payload[8];
            int tolane = Integer.valueOf(laneId.split("_")[0]);
            GtuStamp stamp = this.previousStamp.get(id);
            if (stamp != null && (stamp.lane() != tolane || Math.round(stamp.acceleration().si) != Math.round(toA.si)))
            {
                // add row to data
                LaneBasedGtu gtu = (LaneBasedGtu) this.network.getGTU(id);
                FloatDuration t = FloatDuration.instantiateSI(this.network.getSimulator().getSimulatorAbsTime().floatValue());
                int fromln = stamp.lane();
                FloatAcceleration fromA = stamp.acceleration();
                FloatLength pos = FloatLength.instantiateSI(((PositionVector) payload[1]).get(0).floatValue());
                FloatSpeed v = FloatSpeed.instantiateSI(((Speed) payload[3]).floatValue());
                int type = gtuTypes.indexOf(gtu.getType());
                this.data.append(t, fromln, tolane, fromA, toA, pos, v, type, id);
            }
            this.previousStamp.put(id, new GtuStamp(toA, tolane));
        }
        else if (event.getType().equals(Network.GTU_ADD_EVENT))
        {
            LaneBasedGtu gtu = (LaneBasedGtu) this.network.getGTU((String) event.getContent());
            gtu.addListener(this, LaneBasedGtu.LANEBASED_MOVE_EVENT);
        }
        else if (event.getType().equals(Network.GTU_REMOVE_EVENT))
        {
            LaneBasedGtu gtu = (LaneBasedGtu) this.network.getGTU((String) event.getContent());
            gtu.removeListener(this, LaneBasedGtu.LANEBASED_MOVE_EVENT);
            this.previousStamp.remove(gtu.getId());
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
