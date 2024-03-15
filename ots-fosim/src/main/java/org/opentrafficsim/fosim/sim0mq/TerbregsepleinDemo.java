package org.opentrafficsim.fosim.sim0mq;

import java.io.IOException;
import java.rmi.RemoteException;

import javax.naming.NamingException;

import org.djutils.cli.CliUtil;
import org.djutils.exceptions.Try;
import org.opentrafficsim.base.Resource;
import org.opentrafficsim.base.parameters.ParameterException;
import org.opentrafficsim.core.dsol.OtsAnimator;
import org.opentrafficsim.core.geometry.OtsGeometryException;
import org.opentrafficsim.core.network.NetworkException;
import org.opentrafficsim.draw.core.OtsDrawingException;
import org.opentrafficsim.fosim.parser.FosParser;
import org.opentrafficsim.road.network.RoadNetwork;

import nl.tudelft.simulation.dsol.SimRuntimeException;
import nl.tudelft.simulation.language.DSOLException;

/**
 * Demo of Terbregseplein network.
 * <p>
 * Copyright (c) 2023-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
 */
public class TerbregsepleinDemo extends SingleLaneDemo
{

    /**
     * Main method.
     * @param args String[]; command line arguments.
     * @throws Exception on any exception during simulation.
     */
    public static void main(final String... args) throws Exception
    {
        TerbregsepleinDemo demo = new TerbregsepleinDemo();
        CliUtil.execute(demo, args);
        demo.setupSimulator();
    }

    /**
     * Starts a simulator/animator.
     * @throws SimRuntimeException timing exception
     * @throws NamingException naming exception
     * @throws RemoteException communication exception
     * @throws DSOLException exception in DSOL
     * @throws OtsDrawingException exception in GUI
     */
    @Override
    protected void setupSimulator()
            throws SimRuntimeException, NamingException, RemoteException, DSOLException, OtsDrawingException
    {
        this.network =
                Try.assign(() -> this.setupSimulation(), RuntimeException.class, "Exception while setting up simulation.");
        this.simulator = this.network.getSimulator();
        if (this.simulator instanceof OtsAnimator)
        {
            ((OtsAnimator) this.simulator).setSpeedFactor(Double.MAX_VALUE, false);
        }
        new Worker().start();
    }

    /**
     * Builds the network and demand.
     * @return OtsRoadNetwork; network.
     * @throws NetworkException exception in network
     * @throws OtsGeometryException exception in geometry
     * @throws ParameterException wrong parameter value
     * @throws SimRuntimeException timing exception
     * @throws IOException when stream cannot be read
     */
    private RoadNetwork setupSimulation()
            throws NetworkException, OtsGeometryException, SimRuntimeException, ParameterException, IOException
    {
        FosParser parser = new FosParser();
        parser.parseFromStream(Resource.getResourceAsStream("/Terbregseplein_6.5_aangepast.fos"));
        this.app = parser.getApplication();
        return parser.getNetwork();
    }

}
