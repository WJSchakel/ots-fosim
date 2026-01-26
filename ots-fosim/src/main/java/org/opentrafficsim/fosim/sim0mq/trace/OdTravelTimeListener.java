package org.opentrafficsim.fosim.sim0mq.trace;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.djunits.value.vdouble.scalar.Duration;
import org.djunits.value.vfloat.scalar.FloatDuration;
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
public class OdTravelTimeListener implements EventListener
{

    /** Network. */
    private final Network network;

    /** GTU types. */
    private final List<GtuType> gtuTypes;

    /** OD node name mappings (OTS names are the keys, Fosim numbers the fields). */
    private final Map<String, Integer> odNumbers;

    /** Data storage. */
    private final TraceData data;

    /** Previous data on each GTU. */
    private final Map<String, Duration> startTime = new LinkedHashMap<>();

    /**
     * Constructor.
     * @param network network
     * @param gtuTypes gtu types from parser
     * @param odNumbers mappings of OTS to Fosim names
     * @param data data storage
     */
    public OdTravelTimeListener(final Network network, final List<GtuType> gtuTypes, final Map<String, Integer> odNumbers,
            final TraceData data)
    {
        this.network = network;
        this.gtuTypes = new ArrayList<>(gtuTypes);
        this.odNumbers = odNumbers;
        this.data = data;
        this.network.addListener(this, Network.GTU_ADD_EVENT);
        this.network.addListener(this, Network.GTU_REMOVE_EVENT);
    }

    @Override
    public void notify(final Event event)
    {
        String id = (String) event.getContent();
        if (event.getType().equals(Network.GTU_ADD_EVENT))
        {
            this.startTime.put(id, this.network.getSimulator().getSimulatorTime());
        }
        else if (event.getType().equals(Network.GTU_REMOVE_EVENT))
        {
            LaneBasedGtu gtu = (LaneBasedGtu) this.network.getGTU(id).get();
            Duration tStart = this.startTime.remove(gtu.getId());
            FloatDuration t = FloatDuration.ofSI(this.network.getSimulator().getSimulatorTime().floatValue());
            Integer origin = this.odNumbers.get(gtu.getStrategicalPlanner().getOrigin().get().getId());
            Integer dest = this.odNumbers.get(gtu.getStrategicalPlanner().getDestination().get().getId());
            double ttt = this.network.getSimulator().getSimulatorTime().si - tStart.si;
            FloatDuration tt = FloatDuration.ofSI((float) ttt);
            double dx = gtu.getStrategicalPlanner().getDestination().get().getPoint().x
                    - gtu.getStrategicalPlanner().getOrigin().get().getPoint().x;
            FloatSpeed v = FloatSpeed.ofSI((float) (dx / ttt));
            int type = this.gtuTypes.indexOf(gtu.getType());
            this.data.append(t, origin, dest, tt, v, type, Integer.valueOf(id));
        }
    }

}
