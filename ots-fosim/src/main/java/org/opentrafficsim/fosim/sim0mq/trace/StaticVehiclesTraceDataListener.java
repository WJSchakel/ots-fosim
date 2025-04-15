package org.opentrafficsim.fosim.sim0mq.trace;

import java.rmi.RemoteException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.djutils.event.Event;
import org.djutils.event.EventListener;
import org.opentrafficsim.core.gtu.GtuType;
import org.opentrafficsim.core.network.Network;
import org.opentrafficsim.road.gtu.lane.LaneBasedGtu;

/**
 * Listener that stores GTU type, origin and destination per GTU for the Vehicles trace information.
 * @author wjschakel
 */
public class StaticVehiclesTraceDataListener implements EventListener
{

    /** */
    private static final long serialVersionUID = 1L;

    /** Network. */
    private final Network network;

    /** GTU types. */
    private final Map<String, GtuType> gtuTypes = new LinkedHashMap<>();

    /** Origins. */
    private final Map<String, String> origins = new LinkedHashMap<>();

    /** Destinations. */
    private final Map<String, String> destinations = new LinkedHashMap<>();

    /**
     * Constructor.
     * @param network network
     */
    public StaticVehiclesTraceDataListener(final Network network)
    {
        this.network = network;
        this.network.addListener(this, Network.GTU_ADD_EVENT);
    }

    /**
     * Returns the GTU type.
     * @param gtuId GTU id
     * @return GTU type
     */
    public GtuType getGtuType(final String gtuId)
    {
        return this.gtuTypes.get(gtuId);
    }

    /**
     * Returns the origin.
     * @param gtuId GTU id
     * @return origin
     */
    public String getOrigin(final String gtuId)
    {
        return this.origins.get(gtuId);
    }

    /**
     * Returns the destination.
     * @param gtuId GTU id
     * @return destination
     */
    public String getDestination(final String gtuId)
    {
        return this.destinations.get(gtuId);
    }

    @Override
    public void notify(final Event event) throws RemoteException
    {
        if (event.getType().equals(Network.GTU_ADD_EVENT))
        {
            // At this stage the GTU has not strategic planner yet, get that at first LANEBASED_MOVE_EVENT
            this.network.getGTU((String) event.getContent()).addListener(this, LaneBasedGtu.LANEBASED_MOVE_EVENT);
        }
        else
        {
            String gtuId = (String) ((Object[]) event.getContent())[0];
            LaneBasedGtu gtu = (LaneBasedGtu) this.network.getGTU(gtuId);
            this.gtuTypes.put(gtuId, gtu.getType());
            this.origins.put(gtuId, gtu.getStrategicalPlanner().getOrigin().getId());
            this.destinations.put(gtuId, gtu.getStrategicalPlanner().getDestination().getId());
            this.network.removeListener(this, LaneBasedGtu.LANEBASED_MOVE_EVENT);
        }
    }

}
