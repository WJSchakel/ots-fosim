package org.opentrafficsim.fosim.sim0mq;

import java.util.LinkedHashMap;
import java.util.Map;

import org.opentrafficsim.core.dsol.AbstractOtsModel;
import org.opentrafficsim.core.dsol.OtsSimulatorInterface;
import org.opentrafficsim.road.network.RoadNetwork;

import nl.tudelft.simulation.dsol.SimRuntimeException;
import nl.tudelft.simulation.jstats.streams.MersenneTwister;
import nl.tudelft.simulation.jstats.streams.StreamInterface;

/**
 * Model for FOSIM simulations.
 * <p>
 * Copyright (c) 2023-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
 */
public class FosimModel extends AbstractOtsModel
{
    /** */
    private static final long serialVersionUID = 20231123L;

    /** Network. */
    private RoadNetwork network;
    
    /** Seed. */
    private final long seed;
    
    /**
     * Constructor.
     * @param simulator simulator.
     * @param seed seed.
     */
    public FosimModel(final OtsSimulatorInterface simulator, final long seed)
    {
        super(simulator);
        this.seed = seed;
    }

    /** {@inheritDoc} */
    @Override
    public void constructModel() throws SimRuntimeException
    {
        Map<String, StreamInterface> streams = new LinkedHashMap<>();
        StreamInterface stream = new MersenneTwister(this.seed);
        streams.put("generation", stream);
        stream = new MersenneTwister(this.seed + 1);
        streams.put("default", stream);
        getSimulator().getModel().getStreams().putAll(streams);
    }
    
    /**
     * Set network.
     * @param network network.
     */
    public void setNetwork(final RoadNetwork network)
    {
        this.network = network;
    }

    /** {@inheritDoc} */
    @Override
    public RoadNetwork getNetwork()
    {
        return this.network;
    }
}