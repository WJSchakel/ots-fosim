package org.opentrafficsim.fosim.sim0mq;

import java.io.IOException;

import org.opentrafficsim.base.Resource;
import org.opentrafficsim.base.parameters.ParameterException;
import org.opentrafficsim.core.dsol.OtsSimulatorInterface;
import org.opentrafficsim.core.network.NetworkException;
import org.opentrafficsim.fosim.parser.FosParser;
import org.opentrafficsim.road.network.RoadNetwork;

import nl.tudelft.simulation.dsol.SimRuntimeException;

/**
 * Demo of Terbregseplein network.
 * <p>
 * Copyright (c) 2023-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
 */
public class TerbregsepleinDemo extends DemoTransceiver
{

    /**
     * Constructor.
     * @param args command line arguments.
     * @throws Exception on any exception during simulation.
     */
    protected TerbregsepleinDemo(final String[] args) throws Exception
    {
        super(args);
    }

    /**
     * Main method.
     * @param args command line arguments.
     * @throws Exception on any exception during simulation.
     */
    public static void main(final String... args) throws Exception
    {
        new TerbregsepleinDemo(args);
    }

    /** {@inheritDoc} */
    @Override
    protected RoadNetwork setupSimulation(final OtsSimulatorInterface sim)
            throws NetworkException, SimRuntimeException, ParameterException, IOException
    {
        FosParser parser = new FosParser().setSimulator(sim);
        parser.parseFromStream(Resource.getResourceAsStream("/Terbregseplein_6.5_aangepast_param.fos"));
        return parser.getNetwork();
    }

}
