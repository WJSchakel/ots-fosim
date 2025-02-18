package org.opentrafficsim.fosim.sim0mq;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.WindowConstants;

import org.djunits.unit.SpeedUnit;
import org.djunits.value.vdouble.scalar.Duration;
import org.djunits.value.vdouble.scalar.Frequency;
import org.djunits.value.vdouble.scalar.Length;
import org.djunits.value.vdouble.scalar.Speed;
import org.djunits.value.vdouble.scalar.Time;
import org.djunits.value.vfloat.matrix.FloatDurationMatrix;
import org.djunits.value.vfloat.matrix.FloatLengthMatrix;
import org.djunits.value.vfloat.vector.FloatDurationVector;
import org.djunits.value.vfloat.vector.FloatLengthVector;
import org.djutils.cli.CliUtil;
import org.djutils.draw.line.PolyLine2d;
import org.djutils.draw.line.Polygon2d;
import org.djutils.draw.point.Point2d;
import org.djutils.event.Event;
import org.djutils.event.EventListener;
import org.djutils.exceptions.Throw;
import org.djutils.exceptions.Try;
import org.djutils.serialization.SerializationException;
import org.opentrafficsim.base.geometry.OtsLine2d;
import org.opentrafficsim.base.parameters.ParameterException;
import org.opentrafficsim.core.definitions.DefaultsNl;
import org.opentrafficsim.core.dsol.OtsAnimator;
import org.opentrafficsim.core.geometry.FractionalLengthData;
import org.opentrafficsim.core.gtu.Gtu;
import org.opentrafficsim.core.gtu.GtuException;
import org.opentrafficsim.core.network.LateralDirectionality;
import org.opentrafficsim.core.network.Network;
import org.opentrafficsim.core.network.NetworkException;
import org.opentrafficsim.core.network.Node;
import org.opentrafficsim.draw.graphs.GraphPath;
import org.opentrafficsim.draw.graphs.GraphPath.Section;
import org.opentrafficsim.draw.graphs.GraphUtil;
import org.opentrafficsim.fosim.FosDetector;
import org.opentrafficsim.fosim.parameters.DefaultValue;
import org.opentrafficsim.fosim.parameters.DefaultValueAdapter;
import org.opentrafficsim.fosim.parameters.ParameterDefinitions;
import org.opentrafficsim.fosim.parameters.distributions.DistributionDefinitions;
import org.opentrafficsim.fosim.parser.FosIncentiveRoute;
import org.opentrafficsim.fosim.parser.FosLane;
import org.opentrafficsim.fosim.parser.FosLink;
import org.opentrafficsim.fosim.parser.FosParser;
import org.opentrafficsim.fosim.parser.ParserSetting;
import org.opentrafficsim.fosim.sim0mq.StopCriterion.BatchStatus;
import org.opentrafficsim.fosim.sim0mq.StopCriterion.DetectionType;
import org.opentrafficsim.fosim.simulator.OtsSimulatorInterfaceStep;
import org.opentrafficsim.kpi.interfaces.LaneData;
import org.opentrafficsim.kpi.sampling.SpaceTimeRegion;
import org.opentrafficsim.kpi.sampling.Trajectory;
import org.opentrafficsim.road.definitions.DefaultsRoadNl;
import org.opentrafficsim.road.gtu.lane.LaneBasedGtu;
import org.opentrafficsim.road.gtu.lane.plan.operational.LaneChange;
import org.opentrafficsim.road.gtu.lane.tactical.lmrs.Lmrs;
import org.opentrafficsim.road.gtu.lane.tactical.util.lmrs.LmrsParameters;
import org.opentrafficsim.road.network.RoadNetwork;
import org.opentrafficsim.road.network.lane.CrossSectionGeometry;
import org.opentrafficsim.road.network.lane.CrossSectionLink;
import org.opentrafficsim.road.network.lane.Lane;
import org.opentrafficsim.road.network.lane.LanePosition;
import org.opentrafficsim.road.network.lane.changing.LaneKeepingPolicy;
import org.opentrafficsim.road.network.sampling.LaneDataRoad;
import org.opentrafficsim.road.network.sampling.RoadSampler;
import org.opentrafficsim.swing.gui.OtsSimulationApplication;
import org.sim0mq.Sim0MQException;
import org.sim0mq.message.Sim0MQMessage;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import picocli.CommandLine.Option;

/**
 * This class is the core transceiver between OTS and Fosim from the side of OTS. It handles all messages. Sub classes only
 * define the network etc., which is useful for demos.
 * <p>
 * Copyright (c) 2023-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
 */
public class OtsTransceiver
{

    /** Parameter and distribution version so Fosim can check versions. */
    public static final String VERSION = "v0.0.1";

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
    @Option(names = "--step", description = "Simulation step") // Locale bug: , defaultValue = "0,5s"
    private Duration step = Duration.instantiateSI(0.5);

    /** Show GUI. */
    @Option(names = "--gui", description = "Whether to show GUI", defaultValue = "false")
    private boolean showGui;

    /** Show GUI. */
    @Option(names = "--instantLc", description = "Instant lane changes", defaultValue = "true")
    private boolean instantLc;

    /** The simulator. */
    private OtsSimulatorInterfaceStep simulator;

    /** The network. */
    private RoadNetwork network;

    /** Map of lane change start times per GTU. */
    private Map<String, VirtualLaneChange> laneChanges = new LinkedHashMap<>();

    /** Duration of virtual lane change. */
    public Duration virtualLcDuration = Duration.instantiateSI(3.0);

    /**
     * Constructor.
     * @param args command line arguments.
     * @throws Exception on any exception during simulation.
     */
    protected OtsTransceiver(final String... args) throws Exception
    {
        CliUtil.execute(this, args);
    }

    /**
     * Main method.
     * @param args command line arguments.
     * @throws Exception on any exception during simulation.
     */
    public static void main(String[] args) throws Exception
    {
        new OtsTransceiver(args).start();
    }

    /**
     * Starts worker thread.
     */
    private void start()
    {
        new Worker().start();
    }

    /**
     * Returns whether the GUI is shown.
     * @return whether the GUI is shown
     */
    public boolean isShowGui()
    {
        return this.showGui;
    }

    /**
     * Returns the simulator.
     * @return simulator
     */
    public OtsSimulatorInterfaceStep getSimulator()
    {
        return this.simulator;
    }

    /**
     * Sets the simulator.
     * @param simulator simulator
     */
    protected void setSimulator(final OtsSimulatorInterfaceStep simulator)
    {
        this.simulator = simulator;
    }

    /**
     * Returns the network.
     * @return network
     */
    public RoadNetwork getNetwork()
    {
        return this.network;
    }

    /**
     * Sets the network.
     * @param network network
     */
    protected void setNetwork(final RoadNetwork network)
    {
        this.network = network;
    }

    /**
     * Worker thread to listen to messages and respond.
     * <p>
     * Copyright (c) 2023-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved.
     * <br>
     * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
     * </p>
     * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
     */
    protected class Worker extends Thread
    {

        /** */
        private ZContext context;

        /** the socket. */
        private ZMQ.Socket responder;

        /** Next message id. */
        private int messageId = 0;

        /** App when GUI is used. */
        private OtsSimulationApplication<FosimModel> app;

        /** First detector period. */
        private Duration firstPeriod;

        /** Duration of detector periods after first. */
        private Duration nextPeriods;

        /** Detectors. */
        protected Map<String, FosDetector> detectors = new LinkedHashMap<>();

        /** Step number. */
        private int stepNumber = 1;

        /** Trajectory sampler. */
        private RoadSampler sampler;

        /** ID of dummy objects. */
        private int dummyId;

        /** Target lane of lane changes. */
        private Map<Gtu, Integer> targetLane = new LinkedHashMap<>();

        /** Stop criterion during a batch run. */
        private StopCriterion stopCriterion;

        /** Graph paths. */
        private GraphPath<LaneDataRoad>[] graphPaths;

        /** Minute when time was shown. */
        private int shownMinute = -1;

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
            this.responder.bind("tcp://*:" + port);
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
                        step();
                        this.responder.send(Sim0MQMessage.encodeUTF8(OtsTransceiver.this.bigEndian,
                                OtsTransceiver.this.federation, OtsTransceiver.this.ots, OtsTransceiver.this.fosim,
                                "STEP_REPLY", this.messageId++, new Object[0]), 0);
                    }
                    else if ("VEHICLES".equals(message.getMessageTypeId()))
                    {
                        Object[] payload = OtsTransceiver.this.instantLc ? getVehiclePayloadInstant() : getVehiclePayload();
                        this.responder.send(Sim0MQMessage.encodeUTF8(OtsTransceiver.this.bigEndian,
                                OtsTransceiver.this.federation, OtsTransceiver.this.ots, OtsTransceiver.this.fosim,
                                "VEHICLES_REPLY", this.messageId++, payload), 0);
                    }
                    else if ("DETECTOR".equals(message.getMessageTypeId()))
                    {
                        float value = getDetectorValue(message);
                        this.responder.send(Sim0MQMessage.encodeUTF8(OtsTransceiver.this.bigEndian,
                                OtsTransceiver.this.federation, OtsTransceiver.this.ots, OtsTransceiver.this.fosim,
                                "DETECTOR_REPLY", this.messageId++, value), 0);
                    }
                    else if ("BATCH".equals(message.getMessageTypeId()))
                    {
                        batch(message);
                        this.responder.send(Sim0MQMessage.encodeUTF8(OtsTransceiver.this.bigEndian,
                                OtsTransceiver.this.federation, OtsTransceiver.this.ots, OtsTransceiver.this.fosim,
                                "BATCH_REPLY", this.messageId++, new Object[0]), 0);
                    }
                    else if ("BATCH_STEP".equals(message.getMessageTypeId()))
                    {
                        // TODO: we could perform the next step asynchronously
                        BatchStatus triggered = batchStep();
                        this.responder.send(Sim0MQMessage.encodeUTF8(OtsTransceiver.this.bigEndian,
                                OtsTransceiver.this.federation, OtsTransceiver.this.ots, OtsTransceiver.this.fosim,
                                "BATCH_STEP_REPLY", this.messageId++, new Object[] {triggered.name()}), 0);
                    }
                    else if ("DISTRIBUTIONS".equals(message.getMessageTypeId()))
                    {
                        String distributions = asJsonString(new DistributionDefinitions(OtsTransceiver.VERSION));
                        this.responder.send(Sim0MQMessage.encodeUTF8(OtsTransceiver.this.bigEndian,
                                OtsTransceiver.this.federation, OtsTransceiver.this.ots, OtsTransceiver.this.fosim,
                                "DISTRIBUTIONS_REPLY", this.messageId++, distributions), 0);
                    }
                    else if ("PARAMETERS".equals(message.getMessageTypeId()))
                    {
                        String parameters = asJsonString(new ParameterDefinitions(OtsTransceiver.VERSION));
                        this.responder.send(Sim0MQMessage.encodeUTF8(OtsTransceiver.this.bigEndian,
                                OtsTransceiver.this.federation, OtsTransceiver.this.ots, OtsTransceiver.this.fosim,
                                "PARAMETERS_REPLY", this.messageId++, parameters), 0);
                    }
                    else if ("SETUP".equals(message.getMessageTypeId()))
                    {
                        String exceptionMessage = setup(message);
                        this.responder.send(Sim0MQMessage.encodeUTF8(OtsTransceiver.this.bigEndian,
                                OtsTransceiver.this.federation, OtsTransceiver.this.ots, OtsTransceiver.this.fosim,
                                "SETUP_REPLY", this.messageId++, exceptionMessage), 0);
                    }
                    else if ("TRAJECTORIES".equals(message.getMessageTypeId()))
                    {
                        Object[] payload = getTrajectoriesPayload(message);
                        this.responder.send(Sim0MQMessage.encodeUTF8(OtsTransceiver.this.bigEndian,
                                OtsTransceiver.this.federation, OtsTransceiver.this.ots, OtsTransceiver.this.fosim,
                                "TRAJECTORIES_REPLY", this.messageId++, payload), 0);
                    }
                    else if ("CONTOUR".equals(message.getMessageTypeId()))
                    {
                        Object[] payload = getSpeedContourPayload(message);
                        this.responder.send(Sim0MQMessage.encodeUTF8(OtsTransceiver.this.bigEndian,
                                OtsTransceiver.this.federation, OtsTransceiver.this.ots, OtsTransceiver.this.fosim,
                                "CONTOUR_REPLY", this.messageId++, payload), 0);
                    }
                    else if ("STOP".equals(message.getMessageTypeId()))
                    {
                        stopSimulation();
                        this.responder.send(Sim0MQMessage.encodeUTF8(OtsTransceiver.this.bigEndian,
                                OtsTransceiver.this.federation, OtsTransceiver.this.ots, OtsTransceiver.this.fosim,
                                "STOP_REPLY", this.messageId++, new Object[0]), 0);
                    }
                    else if ("TERMINATE".equals(message.getMessageTypeId()))
                    {
                        this.responder.send(Sim0MQMessage.encodeUTF8(OtsTransceiver.this.bigEndian,
                                OtsTransceiver.this.federation, OtsTransceiver.this.ots, OtsTransceiver.this.fosim,
                                "TERMINATE_REPLY", this.messageId++, new Object[0]), 0);
                        break;
                    }
                    else if ("PING".equals(message.getMessageTypeId()))
                    {
                        this.responder.send(Sim0MQMessage.encodeUTF8(OtsTransceiver.this.bigEndian,
                                OtsTransceiver.this.federation, OtsTransceiver.this.ots, OtsTransceiver.this.fosim, "PONG",
                                this.messageId++, new Object[0]), 0);
                    }
                }
            }
            catch (Sim0MQException | SerializationException | NumberFormatException | GtuException | IOException e)
            {
                e.printStackTrace();
            }
            this.responder.close();
            this.context.destroy();
            this.context.close();
            System.out.println("Ots terminated");
            System.exit(0);
        }

        /**
         * Obtains vehicle message payload in case of virtual lane changes.
         * @return payload
         * @throws GtuException
         */
        private Object[] getVehiclePayloadInstant() throws GtuException
        {
            int numGtus = OtsTransceiver.this.network.getGTUs().size();
            Time now = OtsTransceiver.this.simulator.getSimulatorAbsTime();
            for (Gtu gtu : OtsTransceiver.this.network.getGTUs())
            {
                String gtuId = gtu.getId();
                if (OtsTransceiver.this.laneChanges.containsKey(gtuId))
                {
                    VirtualLaneChange lc = OtsTransceiver.this.laneChanges.get(gtuId);
                    if (now.si - lc.time.si >= OtsTransceiver.this.virtualLcDuration.si)
                    {
                        OtsTransceiver.this.laneChanges.remove(gtuId);
                    }
                }
            }
            int lcGtus = OtsTransceiver.this.laneChanges.size();
            Object[] payload = new Object[1 + 5 * (numGtus - lcGtus) + 7 * lcGtus];
            payload[0] = numGtus;
            int k = 1;
            for (Gtu gtu : OtsTransceiver.this.network.getGTUs())
            {
                LanePosition pos = ((LaneBasedGtu) gtu).getReferencePosition();
                double front = gtu.getFront().dx().si;
                double laneStart = pos.lane().getLink().getStartNode().getPoint().x;
                Length position = Length.instantiateSI(pos.position().si + front + laneStart);
                int lane = getLane(pos.lane().getId());

                payload[k++] = lane;
                payload[k++] = position;
                payload[k++] = gtu.getSpeed();
                payload[k++] = gtu.getAcceleration();

                String gtuId = gtu.getId();
                if (OtsTransceiver.this.laneChanges.containsKey(gtuId))
                {
                    VirtualLaneChange lc = OtsTransceiver.this.laneChanges.get(gtuId);
                    // 1 = for overtaking, 2 = for destination
                    payload[k++] = lc.overtaking ? 1 : 2;
                    payload[k++] = lc.left;
                    payload[k++] = (now.si - lc.time.si) / OtsTransceiver.this.virtualLcDuration.si;
                }
                else
                {
                    payload[k++] = 0;
                }
            }
            return payload;
        }

        /**
         * Obtains vehicle message payload.
         * @return payload
         * @throws GtuException
         */
        private Object[] getVehiclePayload() throws GtuException
        {
            int numGtus = OtsTransceiver.this.network.getGTUs().size();
            Map<Gtu, LaneChange> lcInfo = new LinkedHashMap<>();
            Map<Gtu, Double> madatoryDesire = new LinkedHashMap<>();
            for (Gtu gtu : OtsTransceiver.this.network.getGTUs())
            {
                Lmrs lmrs = (Lmrs) ((LaneBasedGtu) gtu).getTacticalPlanner();
                // need to use reflection to access certain information
                try
                {
                    Field lcField = Lmrs.class.getDeclaredField("laneChange");
                    lcField.setAccessible(true);
                    LaneChange lc = (LaneChange) lcField.get(lmrs);
                    // note: lc may already have finalized the lane change, while finalizeLaneChange has not yet
                    // been called on the GTU, i.e. the reference lane is not the correct lane at the end of an lc
                    if (lc.isChangingLane())
                    {
                        madatoryDesire.put(gtu, lmrs.getLatestDesire(FosIncentiveRoute.class).get(lc.getDirection()));
                        lcInfo.put(gtu, lc);
                    }
                }
                catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex)
                {
                    // ignore; no LC will be shown in FOSIM
                }
            }
            int lcGtus = lcInfo.size();
            Object[] payload = new Object[1 + 5 * (numGtus - lcGtus) + 7 * lcGtus];
            payload[0] = numGtus;
            int k = 1;
            for (Gtu gtu : OtsTransceiver.this.network.getGTUs())
            {
                LanePosition pos = ((LaneBasedGtu) gtu).getReferencePosition();
                double front = gtu.getFront().dx().si;
                double laneStart = pos.lane().getLink().getStartNode().getPoint().x;
                Length position = Length.instantiateSI(pos.position().si + front + laneStart);
                int lane = getLane(pos.lane().getId());
                LaneChange lc = lcInfo.get(gtu);
                if (lc == null)
                {
                    Integer target = this.targetLane.remove(gtu);
                    if (target != null)
                    {
                        // first time step LaneChange indicated no lane change; it is just finished,
                        // finalizeLaneChange() is not yet called so reference lane is not valid this time step
                        lane = target;
                    }
                }
                else if (lc.getDirection().isRight())
                {
                    lane++;
                }
                else if (lc.getDirection().isLeft())
                {
                    lane--;
                }

                payload[k++] = lane;
                payload[k++] = position;
                payload[k++] = gtu.getSpeed();
                payload[k++] = gtu.getAcceleration();
                if (lc == null)
                {
                    payload[k++] = 0;
                }
                else
                {
                    this.targetLane.put(gtu, lane);
                    double totalDesire;
                    try
                    {
                        totalDesire = lc.getDirection().isLeft() ? gtu.getParameters().getParameter(LmrsParameters.DLEFT)
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
            return payload;
        }

        /**
         * Returns the lane number.
         * @param laneId lane id
         * @return lane number
         */
        private int getLane(final String laneId)
        {
            int underscore = laneId.indexOf("_");
            return Integer.parseInt(underscore < 0 ? laneId : laneId.substring(underscore + 1));
        }

        /**
         * Setup a new simulation.
         * @param message message through Sim0MQ
         * @return possible exception message, empty when ok
         */
        private String setup(final Sim0MQMessage message)
        {
            String exceptionMessage = "";
            try
            {
                this.dummyId = 0;
                String fosString = (String) message.createObjectArray()[8];
                Map<ParserSetting, Boolean> settings = new LinkedHashMap<>();
                settings.put(ParserSetting.GUI, OtsTransceiver.this.showGui);
                settings.put(ParserSetting.FOS_DETECTORS, true);
                settings.put(ParserSetting.INSTANT_LC, OtsTransceiver.this.instantLc);
                FosParser parser = new FosParser().setSettings(settings);
                parser.parseFromString(fosString);
                OtsTransceiver.this.network = parser.getNetwork();
                OtsTransceiver.this.simulator = (OtsSimulatorInterfaceStep) OtsTransceiver.this.network.getSimulator();
                if (OtsTransceiver.this.showGui)
                {
                    this.app = parser.getApplication();
                    this.app.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
                    this.app.getAnimationPanel().disableSimulationControlButtons();
                    ((OtsAnimator) OtsTransceiver.this.simulator).setSpeedFactor(Double.MAX_VALUE, false);
                }
                this.firstPeriod = parser.getFirstPeriod();
                this.nextPeriods = parser.getNextPeriods();
                // Map all detectors by their non-full id (i.e. ignoring link and lane)
                this.detectors.clear();
                for (FosDetector detector : OtsTransceiver.this.network.getObjectMap(FosDetector.class).values())
                {
                    this.detectors.put(detector.getId(), detector);
                }
                setupSampler(parser);
                if (OtsTransceiver.this.instantLc)
                {
                    LaneChange.MIN_LC_LENGTH_FACTOR = 0;
                    setupVirtualLaneChanges();
                }
            }
            catch (Exception ex)
            {
                exceptionMessage = ex.getMessage() == null ? "Exception occured without message." : ex.getMessage();
            }
            return exceptionMessage;
        }

        /**
         * Sets up a sampler based on the network.
         * @param parser parser
         * @throws NetworkException if dummy network element cannot be created
         */
        @SuppressWarnings("unchecked")
        private void setupSampler(final FosParser parser) throws NetworkException
        {
            Time endtime = Time.ZERO.plus(OtsTransceiver.this.simulator.getReplication().getEndTime());
            this.sampler =
                    new RoadSampler(OtsTransceiver.this.network, Frequency.instantiateSI(1.0 / OtsTransceiver.this.step.si));

            // determine grid
            int nSections = 0;
            int fromLane = Integer.MAX_VALUE;
            int toLane = 0;
            for (FosLink fosLink : parser.getLinks())
            {
                nSections = Math.max(nSections, fosLink.sectionIndex);
                fromLane = Math.min(fromLane, fosLink.fromLane);
                toLane = Math.max(toLane, fosLink.toLane);
            }
            nSections++; // zero-indexed

            // create grid of LaneDataRoad and register regions for recording
            Length[] lengths = new Length[nSections + 1];
            LaneDataRoad[][] laneData = new LaneDataRoad[toLane + 1][nSections + 1];
            for (FosLink fosLink : parser.getLinks())
            {
                if (lengths[fosLink.sectionIndex] == null)
                {
                    lengths[fosLink.sectionIndex] =
                            Length.instantiateSI(fosLink.lanes.get(0).getLane().getLink().getEndNode().getLocation().x
                                    - fosLink.lanes.get(0).getLane().getLink().getStartNode().getLocation().x);
                }
                int laneNum = fosLink.fromLane;
                for (FosLane fosLane : fosLink.lanes)
                {
                    if (fosLane.getLane() instanceof Lane)
                    {
                        Lane lane = (Lane) fosLane.getLane();
                        laneData[laneNum][fosLink.sectionIndex] = new LaneDataRoad(lane);
                        this.sampler.registerSpaceTimeRegion(new SpaceTimeRegion<LaneDataRoad>(
                                laneData[laneNum][fosLink.sectionIndex], Length.ZERO, lane.getLength(), Time.ZERO, endtime));
                    }
                    laneNum++;
                }
            }

            // create graph paths, which ContourDataSource will use later to provide speed contour data
            this.graphPaths = new GraphPath[toLane + 1];
            Speed speedLimit = new Speed(100.0, SpeedUnit.KM_PER_HOUR); // used for EGTF, which Fosim does not use
            for (int i = fromLane; i <= toLane; i++)
            {
                String pathName = "Lane " + i;
                List<Section<LaneDataRoad>> sections = new ArrayList<>();
                for (int j = 0; j < nSections; j++)
                {
                    LaneDataRoad laneDataRoad = laneData[i][j] == null ? dummyLaneData(lengths[j]) : laneData[i][j];
                    sections.add(new Section<>(laneDataRoad.getLength(), speedLimit, List.of(laneDataRoad)));
                }
                this.graphPaths[i] = new GraphPath<>(pathName, sections);
            }
        }

        private void setupVirtualLaneChanges()
        {
            EventListener listener = new EventListener()
            {
                /** */
                private static final long serialVersionUID = 20250114L;

                @Override
                public void notify(final Event event) throws RemoteException
                {
                    if (event.getType().equals(LaneBasedGtu.LANE_CHANGE_EVENT))
                    {
                        Object[] content = (Object[]) event.getContent();
                        String gtuId = (String) content[0];
                        Gtu gtu = OtsTransceiver.this.network.getGTU(gtuId);
                        LateralDirectionality dir = LateralDirectionality.valueOf((String) content[1]);

                        double totalDesire;
                        try
                        {
                            totalDesire = dir.isLeft() ? gtu.getParameters().getParameter(LmrsParameters.DLEFT)
                                    : gtu.getParameters().getParameter(LmrsParameters.DRIGHT);
                        }
                        catch (ParameterException ex)
                        {
                            totalDesire = 1.0;
                        }
                        Lmrs lmrs = (Lmrs) ((LaneBasedGtu) gtu).getTacticalPlanner();
                        double mandatoryDesire = lmrs.getLatestDesire(FosIncentiveRoute.class).get(dir);
                        boolean overtaking = mandatoryDesire / totalDesire < 0.5;

                        OtsTransceiver.this.laneChanges.put(gtuId, new VirtualLaneChange(dir.isLeft(),
                                OtsTransceiver.this.simulator.getSimulatorAbsTime(), overtaking));
                    }
                    else if (event.getType().equals(Network.GTU_ADD_EVENT))
                    {
                        String id = (String) event.getContent();
                        Gtu gtu = OtsTransceiver.this.network.getGTU(id);
                        gtu.addListener(this, LaneBasedGtu.LANE_CHANGE_EVENT);
                    }
                    else if (event.getType().equals(Network.GTU_REMOVE_EVENT))
                    {
                        String id = (String) event.getContent();
                        Gtu gtu = OtsTransceiver.this.network.getGTU(id);
                        gtu.removeListener(this, LaneBasedGtu.LANE_CHANGE_EVENT);
                        OtsTransceiver.this.laneChanges.remove(id);
                    }
                }
            };
            OtsTransceiver.this.network.addListener(listener, Network.GTU_ADD_EVENT);
            OtsTransceiver.this.network.addListener(listener, Network.GTU_REMOVE_EVENT);
        }

        /**
         * Creates a dummy lane data in place of a gap within a lane (row), i.e. a grass section.
         * @param length length required
         * @return dummy lane data
         * @throws NetworkException
         */
        private LaneDataRoad dummyLaneData(final Length length) throws NetworkException
        {
            Point2d pointA = new Point2d(0.0, -10.0);
            Point2d pointB = new Point2d(length.si, -10.0);
            Node nodeA = new Node(OtsTransceiver.this.network, "_Node " + this.dummyId++, pointA);
            Node nodeB = new Node(OtsTransceiver.this.network, "_Node " + this.dummyId++, pointB);
            OtsLine2d line = new OtsLine2d(new PolyLine2d(pointA, pointB));
            CrossSectionLink link = new CrossSectionLink(OtsTransceiver.this.network, "_Link " + this.dummyId++, nodeA, nodeB,
                    DefaultsNl.FREEWAY, line, FractionalLengthData.of(0.0, 0.0), LaneKeepingPolicy.KEEPRIGHT);
            // List<CrossSectionSlice> slices =
            // List.of(new CrossSectionSlice(Length.ZERO, Length.ZERO, Length.instantiateSI(3.5)));
            FractionalLengthData offset = FractionalLengthData.of(0.0, 0.0, 1.0, 0.0);
            FractionalLengthData width = FractionalLengthData.of(0.0, 3.5, 1.0, 3.5);
            CrossSectionGeometry geometry = new CrossSectionGeometry(line, new Polygon2d(line.getPoints()), offset, width);
            Lane lane = new Lane(link, "_Lane " + this.dummyId++, geometry, DefaultsRoadNl.FREEWAY,
                    Map.of(DefaultsNl.VEHICLE, new Speed(100.0, SpeedUnit.KM_PER_HOUR)));
            return new LaneDataRoad(lane);
        }

        /**
         * Returns a detector value.
         * @param message message through Sim0MQ
         * @return detector value
         */
        private float getDetectorValue(final Sim0MQMessage message)
        {
            Object[] payload = message.createObjectArray();
            int crossSection = (int) payload[8];
            int lane = (int) payload[9];
            int period = (int) payload[10];
            float value;
            double tEnd = this.firstPeriod.si + this.nextPeriods.si * period;
            double tNow = OtsTransceiver.this.simulator.getSimulatorAbsTime().si;
            if (tNow < tEnd)
            {
                value = -1.0f;
            }
            else
            {
                String measurement = (String) payload[11];
                String detectorId = crossSection + "_" + lane;
                FosDetector detector = this.detectors.get(detectorId);
                try
                {
                    switch (measurement)
                    {
                        case "COUNT":
                            value = detector.getCount(period);
                            break;
                        case "SUM_RECIPROCAL_SPEED":
                            value = (float) detector.getSumReciprocalSpeed(period);
                            break;
                        case "TRAVEL_TIME_COUNT":
                            value = detector.getTravelTimeCount(period);
                            break;
                        case "SUM_TRAVEL_TIME":
                            value = (float) detector.getSumTravelTime(period);
                            break;
                        default:
                            value = -1;
                    }
                }
                catch (Exception ex)
                {
                    value = -1;
                }
            }
            return value;
        }

        /**
         * Setup stop criterion for batch simulation.
         * @param message message through Sim0MQ
         */
        private void batch(final Sim0MQMessage message)
        {
            Object[] payload = message.createObjectArray();
            DetectionType detectionType = DetectionType.valueOf((String) payload[8]);
            int fromLane = (int) payload[9];
            int toLane = (int) payload[10];
            int detector = (int) payload[11];
            Speed threshold = (Speed) payload[12];
            // TODO: remove after bypass: "at any detector" gives detector = 0, should be -1
            if (detector == 0)
            {
                detector = -1;
            }
            this.stopCriterion =
                    new StopCriterion(OtsTransceiver.this.network, detectionType, fromLane, toLane, detector, threshold);
        }

        /**
         * Performs a simulation step and returns the stop criterion batch status.
         * @return batch status
         */
        private BatchStatus batchStep()
        {
            Throw.when(this.stopCriterion == null, IllegalStateException.class,
                    "Call BATCH before calling BATCH_STEP, and stop BATCH calls when TRIGGERED or STOPPED is returned.");
            BatchStatus out;
            if (OtsTransceiver.this.network.getSimulator().getSimulatorAbsTime().si >= OtsTransceiver.this.network
                    .getSimulator().getReplication().getEndTime().si)
            {
                out = BatchStatus.STOPPED;
            }
            else
            {
                step();
                out = this.stopCriterion.canStop();
            }
            if (!BatchStatus.RUNNING.equals(out))
            {
                this.stopCriterion = null;
            }
            return out;
        }

        /**
         * Returns trajectories payload.
         * @param message message through Sim0MQ
         * @return speed trajectories payload
         */
        private Object[] getTrajectoriesPayload(final Sim0MQMessage message)
        {
            Object[] payloadIn = message.createObjectArray();
            final Duration startDuration = (Duration) payloadIn[8];
            final Duration finishDuration = (Duration) payloadIn[9];
            final Length startPosition = (Length) payloadIn[10];
            final Length finishPosition = (Length) payloadIn[11];
            final int granularity = (int) payloadIn[12]; // number of time steps

            final Time startTime = Time.instantiateSI(startDuration.si);
            final Time finishTime = Time.instantiateSI(finishDuration.si);
            final float stepSize = (float) (OtsTransceiver.this.step.si * granularity);

            final Map<String, SortedSet<Trajectory<?>>> trajectoriesPerGtu = new LinkedHashMap<>();
            final Map<Trajectory<?>, Lane> laneOfTrajectory = new LinkedHashMap<>();
            final Comparator<Trajectory<?>> trajectoryComp = new Comparator<>()
            {
                @Override
                public int compare(final Trajectory<?> o1, final Trajectory<?> o2)
                {
                    if (o1.size() == 0)
                    {
                        if (o2.size() == 0)
                        {
                            return 0;
                        }
                        return -1;
                    }
                    return Float.compare(o1.getT(0), o2.getT(0));
                }
            };

            for (LaneData<?> laneData : this.sampler.getSamplerData().getLanes())
            {
                Lane lane = ((LaneDataRoad) laneData).getLane();
                double x0 = lane.getCenterLine().getFirst().x;
                double x1 = lane.getCenterLine().getLast().x;
                Length startPositionLane = Length.instantiateSI(startPosition.si - x0);
                Length finishPositionLane = Length.instantiateSI(finishPosition.si - x0);
                if (x0 < finishPosition.si && x1 > startPosition.si)
                {
                    for (Trajectory<?> trajectory : this.sampler.getSamplerData().getTrajectoryGroup(laneData))
                    {
                        // TODO: remove Try.assign after update to later OTS version
                        boolean spatialOverlap = Try.assign(() -> trajectory.getX(0) < finishPositionLane.si
                                && trajectory.getX(trajectory.size() - 1) > startPositionLane.si, "");
                        if (spatialOverlap && GraphUtil.considerTrajectory(trajectory, startTime, finishTime))
                        {
                            trajectoriesPerGtu.computeIfAbsent(trajectory.getGtuId(), (id) -> new TreeSet<>(trajectoryComp))
                                    .add(trajectory);
                            laneOfTrajectory.put(trajectory, lane);
                        }
                    }
                }
            }

            float tMin = (float) startDuration.si;
            float tMax = (float) finishDuration.si;
            int n = trajectoriesPerGtu.size();
            Object[] payload = new Object[1 + 3 * n];
            int k = 0;
            payload[k++] = n;
            for (SortedSet<Trajectory<?>> trajectories : trajectoriesPerGtu.values())
            {
                List<Float> time = new ArrayList<>();
                List<Float> position = new ArrayList<>();
                List<Integer> lane = new ArrayList<>();

                float tPrev = Float.NEGATIVE_INFINITY;
                for (Trajectory<?> trajectory : trajectories)
                {
                    float[] t = trajectory.getT();
                    float[] x = trajectory.getX();
                    Lane laneRoad = laneOfTrajectory.get(trajectory);
                    int underscore = laneRoad.getId().lastIndexOf("_");
                    int laneNum = Integer.valueOf(laneRoad.getId().substring(underscore + 1));
                    float laneStart = (float) laneRoad.getCenterLine().getFirst().x;
                    float laneLength = (float) laneRoad.getLength().si;
                    float xMin = Math.max(0.0f, (float) startPosition.si - laneStart);
                    float xMax = Math.min(laneLength, (float) finishPosition.si - laneStart);
                    for (int i = 0; i < t.length; i++)
                    {
                        if (x[i] >= xMin && x[i] < xMax && t[i] >= tMin && t[i] < tMax && t[i] >= tPrev + stepSize - 0.001)
                        {
                            time.add(t[i]);
                            position.add(laneStart + x[i]);
                            lane.add(laneNum);
                            tPrev = t[i];
                        }
                    }
                }

                payload[k++] = new FloatDurationVector(time);
                payload[k++] = new FloatLengthVector(position);
                payload[k++] = lane.toArray(new Integer[lane.size()]);
            }

            return payload;
        }

        /**
         * Returns speed contour payload.
         * @param message message through Sim0MQ
         * @return speed contour payload
         */
        private Object[] getSpeedContourPayload(final Sim0MQMessage message)
        {
            Object[] payloadIn = message.createObjectArray();
            final Duration startTime = (Duration) payloadIn[8];
            final Duration dt = (Duration) payloadIn[9];
            final Duration finishTime = (Duration) payloadIn[10];
            final Length startPosition = (Length) payloadIn[11];
            final Length dx = (Length) payloadIn[12];
            final Length finishPosition = (Length) payloadIn[13];

            List<Integer> lanes = new ArrayList<>();
            for (int i = 0; i < this.graphPaths.length; i++)
            {
                if (this.graphPaths[i] != null)
                {
                    lanes.add(i);
                }
            }

            Object[] payloadOut = new Object[1 + 2 * lanes.size()];
            int[] laneNums = lanes.stream().mapToInt(i -> i).toArray();
            payloadOut[0] = laneNums;
            int index = 0;
            for (int i = 0; i < this.graphPaths.length; i++)
            {
                if (this.graphPaths[i] != null)
                {
                    GraphPath<LaneDataRoad> graphPath = this.graphPaths[i];
                    FosContourDataSource dataSource = new FosContourDataSource(this.sampler.getSamplerData(), graphPath);
                    dataSource.update(startTime, dt, finishTime, startPosition, dx, finishPosition);
                    payloadOut[1 + index * 2] = new FloatLengthMatrix(dataSource.getTotalDistance());
                    payloadOut[2 + index * 2] = new FloatDurationMatrix(dataSource.getTotalTime());
                    index++;
                }
            }
            return payloadOut;
        }

        /**
         * Stops simulation.
         */
        private void stopSimulation()
        {
            OtsTransceiver.this.simulator = null;
            OtsTransceiver.this.network = null;
            if (this.app != null)
            {
                this.app.setVisible(false);
                this.app.dispose();
                this.app = null;
            }
            this.firstPeriod = null;
            this.nextPeriods = null;
            this.detectors.clear();
            this.stepNumber = 1;
            this.sampler = null;
            this.dummyId = 0;
            this.targetLane.clear();
            this.graphPaths = null;
        }

        /**
         * Run a simulation step, where a 'step' is defined as a fixed time step. Note that within Ots usually a step is defined
         * as a single event in DSOL.
         */
        private synchronized void step()
        {
            Duration until = OtsTransceiver.this.step.times(this.stepNumber++);
            if (!OtsTransceiver.this.showGui)
            {
                OtsTransceiver.this.simulator.scheduleEventAbs(until, this, "showTime", null);
                while (OtsTransceiver.this.simulator.getSimulatorTime().lt(until))
                {
                    OtsTransceiver.this.simulator.step();
                }
            }
            else
            {
                if (OtsTransceiver.this.simulator.isStartingOrRunning())
                {
                    OtsTransceiver.this.simulator.stop();
                }
                OtsTransceiver.this.simulator.runUpToAndIncluding(until);
                while (OtsTransceiver.this.simulator.isStartingOrRunning())
                {
                    synchronized (OtsTransceiver.this.simulator.getWorkerThread())
                    {
                        //
                    }
                }
            }
        }

        /**
         * Transforms an object in to a JSON string.
         * @param object object.
         * @return JSON string representation of object.
         * @throws IOException if string writer could not be closed.
         */
        private String asJsonString(final Object object) throws IOException
        {
            GsonBuilder builder = new GsonBuilder();
            builder.disableHtmlEscaping();
            builder.registerTypeAdapter(DefaultValue.class, new DefaultValueAdapter());
            Gson gson = builder.create();

            StringWriter writer = new StringWriter();
            gson.toJson(object, writer);
            writer.close();
            return writer.toString();
        }

        /**
         * Shows the time. This is mostly a dummy method scheduled at the 'run until' time such that the simulator stops at this
         * time.
         */
        @SuppressWarnings("unused") // used through scheduling
        private void showTime()
        {
            Duration time = OtsTransceiver.this.simulator.getSimulatorTime();
            int thisMinute = (int) (time.si / 60.0);
            if ((this.shownMinute > 0 && thisMinute == 0) || this.shownMinute < thisMinute)
            {
                this.shownMinute = thisMinute;
                System.out.println(time);
            }
        }

    }

    /**
     * Record of lane change direction and time.
     */
    public record VirtualLaneChange(boolean left, Time time, boolean overtaking)
    {
    }
}
