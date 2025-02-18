package org.opentrafficsim.fosim.sim0mq;

import java.awt.Dimension;
import java.io.IOException;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;

import org.djunits.value.vdouble.scalar.Duration;
import org.djunits.value.vdouble.scalar.Time;
import org.djutils.exceptions.Try;
import org.opentrafficsim.animation.gtu.colorer.GtuColorer;
import org.opentrafficsim.base.parameters.ParameterException;
import org.opentrafficsim.core.definitions.DefaultsNl;
import org.opentrafficsim.core.dsol.OtsAnimator;
import org.opentrafficsim.core.dsol.OtsSimulatorInterface;
import org.opentrafficsim.core.gtu.GtuType;
import org.opentrafficsim.core.network.NetworkException;
import org.opentrafficsim.core.perception.HistoryManagerDevs;
import org.opentrafficsim.draw.gtu.DefaultCarAnimation.GtuData.GtuMarker;
import org.opentrafficsim.fosim.simulator.OtsAnimatorStep;
import org.opentrafficsim.fosim.simulator.OtsSimulatorStep;
import org.opentrafficsim.road.network.RoadNetwork;
import org.opentrafficsim.swing.gui.OtsAnimationPanel;
import org.opentrafficsim.swing.gui.OtsSimulationApplication;
import org.opentrafficsim.swing.gui.OtsSwingApplication;

import nl.tudelft.simulation.dsol.SimRuntimeException;
import nl.tudelft.simulation.language.DsolException;

/**
 * Transceiver which sets up a 1-hour simulator for demos.
 * <p>
 * Copyright (c) 2024-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
 */
public abstract class DemoTransceiver extends OtsTransceiver
{

    /**
     * Constructor.
     * @param args arguments.
     * @throws Exception
     */
    protected DemoTransceiver(final String[] args) throws Exception
    {
        super(args);
        setupSimulator();
        new Worker().start();
    }

    /**
     * Starts a simulator/animator
     * @throws SimRuntimeException timing exception
     * @throws NamingException naming exception
     * @throws RemoteException communication exception
     * @throws DSOLException exception in DSOL
     * @throws OtsDrawingException exception in GUI
     */
    protected void setupSimulator() throws SimRuntimeException, NamingException, RemoteException, DsolException
    {
        Duration simulationTime = Duration.instantiateSI(3600.0);
        if (!isShowGui())
        {
            setSimulator(new OtsSimulatorStep("Ots-Fosim"));
            final FosimModel fosimModel = new FosimModel(getSimulator(), 1L);
            getSimulator().initialize(Time.ZERO, Duration.ZERO, simulationTime, fosimModel,
                    new HistoryManagerDevs(getSimulator(), Duration.instantiateSI(5.0), Duration.instantiateSI(10.0)));
            setNetwork(Try.assign(() -> this.setupSimulation(getSimulator()), RuntimeException.class,
                    "Exception while setting up simulation."));
            fosimModel.setNetwork(getNetwork());
        }
        else
        {
            OtsAnimatorStep animator = new OtsAnimatorStep("Ots-Fosim");
            setSimulator(animator);
            final FosimModel fosimModel = new FosimModel(getSimulator(), 1L);
            getSimulator().initialize(Time.ZERO, Duration.ZERO, simulationTime, fosimModel,
                    new HistoryManagerDevs(getSimulator(), Duration.instantiateSI(5.0), Duration.instantiateSI(10.0)));
            setNetwork(Try.assign(() -> this.setupSimulation(getSimulator()), RuntimeException.class,
                    "Exception while setting up simulation."));
            fosimModel.setNetwork(getNetwork());
            List<GtuColorer> colorers = OtsSwingApplication.DEFAULT_GTU_COLORERS;
            OtsAnimationPanel animationPanel = new OtsAnimationPanel(fosimModel.getNetwork().getExtent(),
                    new Dimension(800, 600), (OtsAnimator) getSimulator(), fosimModel, colorers, fosimModel.getNetwork());
            Map<GtuType, GtuMarker> markerMap = Map.of(DefaultsNl.TRUCK, GtuMarker.SQUARE);
            new OtsSimulationApplication<FosimModel>(fosimModel, animationPanel, markerMap);
            animator.setSpeedFactor(Double.MAX_VALUE, false);
            // animationPanel.enableSimulationControlButtons();
        }
    }

    /**
     * Builds the network and demand.
     * @param sim simulator, may be {@code null} in which case a simulator should be created.
     * @return network.
     * @throws NetworkException exception in network
     * @throws ParameterException wrong parameter value
     * @throws SimRuntimeException timing exception
     * @throws IOException when stream cannot be read
     */
    abstract protected RoadNetwork setupSimulation(final OtsSimulatorInterface sim)
            throws NetworkException, SimRuntimeException, ParameterException, IOException;

}
