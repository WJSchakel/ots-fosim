package org.opentrafficsim.fosim.sim0mq;

import java.awt.Dimension;
import java.io.IOException;
import java.lang.reflect.Field;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.naming.NamingException;

import org.djunits.unit.FrequencyUnit;
import org.djunits.unit.SpeedUnit;
import org.djunits.unit.TimeUnit;
import org.djunits.value.storage.StorageType;
import org.djunits.value.vdouble.scalar.Direction;
import org.djunits.value.vdouble.scalar.Duration;
import org.djunits.value.vdouble.scalar.Length;
import org.djunits.value.vdouble.scalar.Speed;
import org.djunits.value.vdouble.scalar.Time;
import org.djunits.value.vdouble.vector.FrequencyVector;
import org.djunits.value.vdouble.vector.TimeVector;
import org.djunits.value.vdouble.vector.data.DoubleVectorData;
import org.djutils.cli.CliUtil;
import org.djutils.exceptions.Try;
import org.djutils.serialization.SerializationException;
import org.opentrafficsim.base.parameters.ParameterException;
import org.opentrafficsim.core.animation.gtu.colorer.GtuColorer;
import org.opentrafficsim.core.definitions.Defaults;
import org.opentrafficsim.core.definitions.DefaultsNl;
import org.opentrafficsim.core.dsol.OtsAnimator;
import org.opentrafficsim.core.dsol.OtsSimulator;
import org.opentrafficsim.core.dsol.OtsSimulatorInterface;
import org.opentrafficsim.core.geometry.OtsGeometryException;
import org.opentrafficsim.core.geometry.OtsLine3d;
import org.opentrafficsim.core.geometry.OtsPoint3d;
import org.opentrafficsim.core.gtu.Gtu;
import org.opentrafficsim.core.gtu.GtuException;
import org.opentrafficsim.core.gtu.GtuType;
import org.opentrafficsim.core.network.NetworkException;
import org.opentrafficsim.core.network.Node;
import org.opentrafficsim.draw.core.OtsDrawingException;
import org.opentrafficsim.fosim.simulator.OtsAnimatorStep;
import org.opentrafficsim.road.definitions.DefaultsRoadNl;
import org.opentrafficsim.road.gtu.generator.characteristics.DefaultLaneBasedGtuCharacteristicsGeneratorOd;
import org.opentrafficsim.road.gtu.generator.characteristics.DefaultLaneBasedGtuCharacteristicsGeneratorOd.Factory;
import org.opentrafficsim.road.gtu.lane.LaneBasedGtu;
import org.opentrafficsim.road.gtu.lane.plan.operational.LaneChange;
import org.opentrafficsim.road.gtu.lane.tactical.lmrs.IncentiveRoute;
import org.opentrafficsim.road.gtu.lane.tactical.lmrs.Lmrs;
import org.opentrafficsim.road.gtu.lane.tactical.util.lmrs.LmrsParameters;
import org.opentrafficsim.road.gtu.strategical.LaneBasedStrategicalRoutePlannerFactory;
import org.opentrafficsim.road.network.RoadNetwork;
import org.opentrafficsim.road.network.lane.CrossSectionLink;
import org.opentrafficsim.road.network.lane.Lane;
import org.opentrafficsim.road.network.lane.changing.LaneKeepingPolicy;
import org.opentrafficsim.road.network.lane.object.detector.SinkDetector;
import org.opentrafficsim.road.od.Categorization;
import org.opentrafficsim.road.od.Category;
import org.opentrafficsim.road.od.Interpolation;
import org.opentrafficsim.road.od.OdApplier;
import org.opentrafficsim.road.od.OdMatrix;
import org.opentrafficsim.road.od.OdOptions;
import org.opentrafficsim.swing.gui.OtsAnimationPanel;
import org.opentrafficsim.swing.gui.OtsSimulationApplication;
import org.opentrafficsim.swing.gui.OtsSwingApplication;
import org.sim0mq.Sim0MQException;
import org.sim0mq.message.Sim0MQMessage;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import nl.tudelft.simulation.dsol.SimRuntimeException;
import nl.tudelft.simulation.jstats.streams.StreamInterface;
import nl.tudelft.simulation.language.DSOLException;
import picocli.CommandLine.Option;

/**
 * Simple technical demo of a single straight lane.
 * <p>
 * Copyright (c) 2023-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
 */
// TODO: abstract this class to one that leaves the specification of the simulation to sub classes
public class SingleLaneDemo
{

    /** Federation id to receive/sent messages. */
    @Option(names = "--federationId", description = "Federation id to receive/sent messages", defaultValue = "Ots_Fosim")
    private String federation;

    /** Ots id to receive/sent messages. */
    @Option(names = "--otsId", description = "Ots id to receive/sent messages", defaultValue = "Ots")
    private String ots;

    /** Fosim id to receive/sent messages. */
    @Option(names = "--fosimId", description = "Fosim id to receive/sent messages", defaultValue = "Fosim")
    private String fosim;

    /** Endianness. */
    @Option(names = "--bigEndian", description = "Big-endianness", defaultValue = "true")
    private Boolean bigEndian;

    /** Port number. */
    @Option(names = "--port", description = "Port number", defaultValue = "5556")
    private int port;

    /** Simulation step. */
    @Option(names = "--step", description = "Simulation step", defaultValue = "0,5s")
    private Duration step;

    /** Show GUI. */
    @Option(names = "--gui", description = "Whether to show GUI", defaultValue = "false")
    protected boolean showGUI;

    /** The simulator. */
    protected OtsSimulatorInterface simulator;

    /** The network. */
    protected RoadNetwork network;

    /** Application screen. */
    protected OtsSimulationApplication<FosimModel> app;

    /** Step number. */
    private int stepNumber = 1;

    /**
     * Main method.
     * @param args String[]; command line arguments.
     * @throws Exception on any exception during simulation.
     */
    public static void main(final String... args) throws Exception
    {
        SingleLaneDemo demo = new SingleLaneDemo();
        CliUtil.execute(demo, args);
        demo.setupSimulator();
    }

    /**
     * Starts a simulator/animator
     * @throws SimRuntimeException timing exception
     * @throws NamingException naming exception
     * @throws RemoteException communication exception
     * @throws DSOLException exception in DSOL
     * @throws OtsDrawingException exception in GUI
     */
    protected void setupSimulator()
            throws SimRuntimeException, NamingException, RemoteException, DSOLException, OtsDrawingException
    {
        Duration simulationTime = Duration.instantiateSI(3600.0);
        if (!this.showGUI)
        {
            this.simulator = new OtsSimulator("Ots-Fosim");
            final FosimModel fosimModel = new FosimModel(this.simulator, 1L);
            this.simulator.initialize(Time.ZERO, Duration.ZERO, simulationTime, fosimModel);
            this.network = Try.assign(() -> this.setupSimulation(this.simulator), RuntimeException.class,
                    "Exception while setting up simulation.");
            fosimModel.setNetwork(this.network);
        }
        else
        {
            OtsAnimator animator = new OtsAnimatorStep("Ots-Fosim");
            this.simulator = animator;
            final FosimModel fosimModel = new FosimModel(this.simulator, 1L);
            this.simulator.initialize(Time.ZERO, Duration.ZERO, simulationTime, fosimModel);
            this.network = Try.assign(() -> this.setupSimulation(this.simulator), RuntimeException.class,
                    "Exception while setting up simulation.");
            fosimModel.setNetwork(this.network);
            GtuColorer colorer = OtsSwingApplication.DEFAULT_COLORER;
            OtsAnimationPanel animationPanel = new OtsAnimationPanel(fosimModel.getNetwork().getExtent(),
                    new Dimension(800, 600), (OtsAnimator) this.simulator, fosimModel, colorer, fosimModel.getNetwork());
            this.app = new OtsSimulationApplication<FosimModel>(fosimModel, animationPanel);
            animator.setSpeedFactor(Double.MAX_VALUE, false);
            // animationPanel.enableSimulationControlButtons();
        }
        new Worker().start();
    }

    /**
     * Builds the network and demand.
     * @param sim OtsSimulatorInterface; simulator.
     * @return OtsRoadNetwork; network.
     * @throws NetworkException exception in network
     * @throws OtsGeometryException exception in geometry
     * @throws ParameterException wrong parameter value
     * @throws SimRuntimeException timing exception
     * @throws IOException when stream cannot be read
     */
    private RoadNetwork setupSimulation(final OtsSimulatorInterface sim)
            throws NetworkException, OtsGeometryException, SimRuntimeException, ParameterException, IOException
    {
        RoadNetwork network = new RoadNetwork("Ots-Fosim", sim);

        OtsPoint3d pointFrom = new OtsPoint3d(0.0, -1.75, 0.0);
        OtsPoint3d pointTo = new OtsPoint3d(2000.0, -1.75, 0.0);

        Node nodeFrom = new Node(network, "From", pointFrom, Direction.ZERO);
        Node nodeTo = new Node(network, "To", pointTo, Direction.ZERO);

        OtsLine3d designLine = new OtsLine3d(pointFrom, pointTo);
        CrossSectionLink link = new CrossSectionLink(network, "Link", nodeFrom, nodeTo, DefaultsNl.FREEWAY, designLine,
                LaneKeepingPolicy.KEEPRIGHT);

        Lane lane = new Lane(link, "1", Length.ZERO, Length.instantiateSI(3.5), DefaultsRoadNl.FREEWAY,
                Map.of(DefaultsNl.ROAD_USER, new Speed(100.0, SpeedUnit.KM_PER_HOUR)));

        DoubleVectorData timeData =
                DoubleVectorData.instantiate(new double[] {0.0, 3600.0}, TimeUnit.BASE_SECOND.getScale(), StorageType.DENSE);
        TimeVector timeVector = new TimeVector(timeData, TimeUnit.BASE_SECOND);
        List<Node> origins = new ArrayList<>();
        origins.add(nodeFrom);
        List<Node> destinations = new ArrayList<>();
        destinations.add(nodeTo);
        OdMatrix od = new OdMatrix("OD", origins, destinations, Categorization.UNCATEGORIZED, timeVector, Interpolation.LINEAR);
        DoubleVectorData flowData = DoubleVectorData.instantiate(new double[] {1500.0, 1500.0},
                FrequencyUnit.PER_HOUR.getScale(), StorageType.DENSE);
        FrequencyVector flowVector = new FrequencyVector(flowData, FrequencyUnit.PER_HOUR);
        od.putDemandVector(nodeFrom, nodeTo, Category.UNCATEGORIZED, flowVector);

        OdOptions odOptions = new OdOptions();

        StreamInterface stream = this.simulator.getModel().getStreams().get("generation");
        LaneBasedStrategicalRoutePlannerFactory defaultLmrsFactory =
                DefaultLaneBasedGtuCharacteristicsGeneratorOd.defaultLmrs(stream);
        Factory characteristicsGeneratorFactory = new Factory(defaultLmrsFactory);
        GtuType.registerTemplateSupplier(DefaultsNl.CAR, Defaults.NL);
        odOptions.set(OdOptions.GTU_TYPE, characteristicsGeneratorFactory.create());

        OdApplier.applyOd(network, od, odOptions, DefaultsRoadNl.ROAD_USERS);

        new SinkDetector(lane, Length.instantiateSI(1950.0), sim, DefaultsRoadNl.ROAD_USERS);

        return network;
    }

    /**
     * Run a simulation step, where a 'step' is defined as a fixed time step. Note that within Ots usually a step is defined as
     * a single event in DSOL.
     */
    private synchronized void step()
    {
        Duration until = this.step.times(this.stepNumber++);
        if (!this.showGUI)
        {
            // This code makes single-lane demo run at 80x without animation
            this.simulator.scheduleEventAbs(until, this, "showTime", null);
            while (this.simulator.getSimulatorTime().lt(until))
            {
                this.simulator.step();
            }
        }
        else
        {
            // This code makes single-lane demo run at 20x without animation
            // This code makes single-lane demo run at 10x with OtsAnimator
            // This code makes single-lane demo run at 20x with OtsAnimatorStep
            if (this.simulator.isStartingOrRunning())
            {
                this.simulator.stop();
            }
            this.simulator.runUpToAndIncluding(until);
            while (this.simulator.isStartingOrRunning())
            {
                try
                {
                    // In order to allow resources to go to other processes, we sleep before checking again
                    Thread.sleep(3);
                }
                catch (InterruptedException e)
                {
                }
            }
        }
    }

    /**
     * Shows the time. This is mostly a dummy method scheduled at the 'run until' time such that the simulator stops at this
     * time.
     */
    @SuppressWarnings("unused") // used through scheduling
    private void showTime()
    {
        System.out.println(this.simulator.getSimulatorTime());
    }

    /**
     * Worker thread to listen to messages and respond.
     * @author wjschakel
     */
    protected class Worker extends Thread
    {

        /** */
        private ZContext context;

        /** the socket. */
        private ZMQ.Socket responder;

        /** Next message id. */
        private int messageId = 0;

        /**
         * Constructor.
         */
        public Worker()
        {
        }

        /** {@inheritDoc} */
        @Override
        public void run()
        {
            this.context = new ZContext(1);
            this.responder = this.context.createSocket(SocketType.REP);
            this.responder.bind("tcp://*:" + SingleLaneDemo.this.port);
            System.out.println("Server is running");

            try
            {
                while (!Thread.currentThread().isInterrupted())
                {
                    // Wait for next request from the client
                    byte[] request = this.responder.recv(ZMQ.DONTWAIT);
                    while (request == null)
                    {
                        try
                        {
                            // In order to allow resources to go to other processes, we sleep before checking again
                            Thread.sleep(3);
                        }
                        catch (InterruptedException e)
                        {
                        }
                        request = this.responder.recv(ZMQ.DONTWAIT);
                    }
                    Sim0MQMessage message = Sim0MQMessage.decode(request);

                    if ("STEP".equals(message.getMessageTypeId()))
                    {
                        SingleLaneDemo.this.step();
                        this.responder.send(Sim0MQMessage.encodeUTF8(SingleLaneDemo.this.bigEndian,
                                SingleLaneDemo.this.federation, SingleLaneDemo.this.ots, SingleLaneDemo.this.fosim,
                                "STEP_REPLY", this.messageId++, new Object[0]), 0);
                    }
                    else if ("VEHICLE_REQUEST".equals(message.getMessageTypeId()))
                    {
                        int numGtus = SingleLaneDemo.this.network.getGTUs().size();
                        Map<Gtu, LaneChange> lcInfo = new LinkedHashMap<>();
                        Map<Gtu, Double> madatoryDesire = new LinkedHashMap<>();
                        for (Gtu gtu : SingleLaneDemo.this.network.getGTUs())
                        {
                            LaneBasedGtu laneGtu = (LaneBasedGtu) gtu;
                            Lmrs lmrs = (Lmrs) laneGtu.getTacticalPlanner();
                            // need to use reflection to access certain information
                            try
                            {
                                Field lcField = Lmrs.class.getDeclaredField("laneChange");
                                lcField.setAccessible(true);
                                LaneChange lc = (LaneChange) lcField.get(lmrs);
                                if (lc.isChangingLane())
                                {
                                    madatoryDesire.put(gtu, lmrs.getLatestDesire(IncentiveRoute.class).get(lc.getDirection()));
                                    lcInfo.put(gtu, lc);
                                }
                            }
                            catch (NoSuchFieldException | SecurityException | IllegalArgumentException
                                    | IllegalAccessException ex)
                            {
                                // ignore; no LC will be shown in FOSIM
                            }
                        }
                        int lcGtus = lcInfo.size();
                        Object[] payload = new Object[1 + 5 * (numGtus - lcGtus) + 7 * lcGtus];
                        payload[0] = numGtus;
                        int k = 1;
                        for (Gtu gtu : SingleLaneDemo.this.network.getGTUs())
                        {
                            Map<Lane, Length> positions = ((LaneBasedGtu) gtu).positions(gtu.getFront());
                            Length position = Length.POSITIVE_INFINITY;
                            String laneId = null;
                            for (Entry<Lane, Length> entry : positions.entrySet())
                            {
                                Length pos = entry.getValue();
                                if (pos.ge0() && pos.lt(position))
                                {
                                    position = pos;
                                    laneId = entry.getKey().getId();
                                }
                            }
                            int underscore = laneId.indexOf("_");
                            int lane = Integer.parseInt(underscore < 0 ? laneId : laneId.substring(underscore + 1));
                            LaneChange lc = lcInfo.get(gtu);
                            payload[k++] = lc == null ? lane : (lc.getDirection().isLeft() ? lane - 1 : lane + 1);
                            payload[k++] = Length.instantiateSI(gtu.getLocation().x);
                            payload[k++] = gtu.getSpeed();
                            payload[k++] = gtu.getAcceleration();
                            if (lc == null)
                            {
                                payload[k++] = 0;
                            }
                            else
                            {
                                double totalDesire;
                                try
                                {
                                    totalDesire =
                                            lc.getDirection().isLeft() ? gtu.getParameters().getParameter(LmrsParameters.DLEFT)
                                                    : gtu.getParameters().getParameter(LmrsParameters.DRIGHT);
                                }
                                catch (ParameterException ex)
                                {
                                    totalDesire = 1.0;
                                }
                                // 1 = for overtaking, 2 = for destination
                                payload[k++] = madatoryDesire.get(gtu) / totalDesire < 0.5 ? 1 : 2;
                                payload[k++] = lc.isChangingLeft();
                                payload[k++] = lc.getFraction();
                            }
                        }
                        this.responder.send(Sim0MQMessage.encodeUTF8(SingleLaneDemo.this.bigEndian,
                                SingleLaneDemo.this.federation, SingleLaneDemo.this.ots, SingleLaneDemo.this.fosim,
                                "VEHICLE_REPLY", this.messageId++, payload), 0);
                    }
                    else if ("STOP".equals(message.getMessageTypeId()))
                    {
                        System.out.println("Ots received STOP command at " + SingleLaneDemo.this.simulator.getSimulatorTime());
                        break;
                    }
                    else if ("PING".equals(message.getMessageTypeId()))
                    {
                        this.responder.send(Sim0MQMessage.encodeUTF8(SingleLaneDemo.this.bigEndian,
                                SingleLaneDemo.this.federation, SingleLaneDemo.this.ots, SingleLaneDemo.this.fosim, "PONG",
                                this.messageId++, new Object[0]), 0);
                    }
                }
            }
            catch (Sim0MQException | SerializationException | NumberFormatException | GtuException e)
            {
                e.printStackTrace();
            }
            this.responder.close();
            this.context.destroy();
            this.context.close();
            System.out.println("Ots terminated");
            System.exit(0);
        }

    }

}
