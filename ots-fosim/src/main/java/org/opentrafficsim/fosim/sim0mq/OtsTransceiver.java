package org.opentrafficsim.fosim.sim0mq;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import javax.swing.JFileChooser;
import javax.swing.WindowConstants;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.djunits.value.vdouble.scalar.Duration;
import org.djunits.value.vdouble.scalar.Frequency;
import org.djunits.value.vdouble.scalar.Length;
import org.djunits.value.vdouble.scalar.Speed;
import org.djunits.value.vfloat.matrix.FloatDurationMatrix;
import org.djunits.value.vfloat.matrix.FloatLengthMatrix;
import org.djunits.value.vfloat.scalar.FloatDuration;
import org.djunits.value.vfloat.scalar.FloatLength;
import org.djunits.value.vfloat.scalar.FloatSpeed;
import org.djunits.value.vfloat.vector.FloatDurationVector;
import org.djunits.value.vfloat.vector.FloatLengthVector;
import org.djutils.cli.CliUtil;
import org.djutils.data.Column;
import org.djutils.data.Row;
import org.djutils.event.Event;
import org.djutils.event.EventListener;
import org.djutils.exceptions.Throw;
import org.djutils.serialization.SerializationException;
import org.opentrafficsim.base.OtsRuntimeException;
import org.opentrafficsim.base.parameters.ParameterException;
import org.opentrafficsim.core.dsol.OtsAnimator;
import org.opentrafficsim.core.gtu.Gtu;
import org.opentrafficsim.core.gtu.GtuException;
import org.opentrafficsim.core.gtu.GtuType;
import org.opentrafficsim.core.network.LateralDirectionality;
import org.opentrafficsim.core.network.Network;
import org.opentrafficsim.core.network.NetworkException;
import org.opentrafficsim.draw.graphs.GraphPath;
import org.opentrafficsim.draw.graphs.GraphUtil;
import org.opentrafficsim.fosim.FosDetector;
import org.opentrafficsim.fosim.FosDetector.Passing;
import org.opentrafficsim.fosim.parameters.DefaultValue;
import org.opentrafficsim.fosim.parameters.DefaultValueAdapter;
import org.opentrafficsim.fosim.parameters.Limit;
import org.opentrafficsim.fosim.parameters.LimitAdapter;
import org.opentrafficsim.fosim.parameters.ParameterDefinitions;
import org.opentrafficsim.fosim.parameters.distributions.DistributionDefinitions;
import org.opentrafficsim.fosim.parser.FosIncentiveRoute;
import org.opentrafficsim.fosim.parser.FosParser;
import org.opentrafficsim.fosim.parser.FosSampler;
import org.opentrafficsim.fosim.parser.ParserSetting;
import org.opentrafficsim.fosim.sim0mq.StopCriterion.BatchStatus;
import org.opentrafficsim.fosim.sim0mq.StopCriterion.DetectionType;
import org.opentrafficsim.fosim.sim0mq.trace.AccelerationChangeListener;
import org.opentrafficsim.fosim.sim0mq.trace.LaneChangeListener;
import org.opentrafficsim.fosim.sim0mq.trace.OdTravelTimeListener;
import org.opentrafficsim.fosim.sim0mq.trace.StaticVehiclesTraceDataListener;
import org.opentrafficsim.fosim.sim0mq.trace.Trace;
import org.opentrafficsim.fosim.sim0mq.trace.TraceData;
import org.opentrafficsim.fosim.simulator.OtsSimulatorInterfaceStep;
import org.opentrafficsim.kpi.interfaces.LaneData;
import org.opentrafficsim.kpi.sampling.Trajectory;
import org.opentrafficsim.road.gtu.lane.LaneBasedGtu;
import org.opentrafficsim.road.gtu.lane.tactical.lmrs.Lmrs;
import org.opentrafficsim.road.gtu.lane.tactical.util.lmrs.LmrsParameters;
import org.opentrafficsim.road.network.RoadNetwork;
import org.opentrafficsim.road.network.lane.Lane;
import org.opentrafficsim.road.network.lane.LanePosition;
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
    public static final String VERSION = "v0.1.0";

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
    private Duration step = Duration.ofSI(0.5);

    /** Show GUI. */
    @Option(names = "--gui", description = "Whether to show GUI", defaultValue = "false")
    private boolean showGui;

    // -------- OTS-only settings --------
    // To use the transceiver in OTS only mode, use (with possible relevant paths):
    // java.exe -jar OtsTransceiver.jar --otsOnly=true --gui=false --fosFile=network.fos --detectorOutput=true --seed=12

    /** Run simulation from FOSIM file in OTS directly. */
    @Option(names = "--otsOnly", description = "Run simulation in OTS from FOSIM file", defaultValue = "false")
    private boolean otsOnly;

    /** FOSIM file to run with. */
    @Option(names = "--fosFile", description = "FOSIM file to run with (if not given a file dialog will appear)")
    private String fosFile = null;

    /** Write detector output. */
    @Option(names = "--detectorOutput", description = "Write detector output", defaultValue = "false")
    private boolean detectorOutput;

    /** Seed. */
    @Option(names = "--seed", description = "Seed to override file seed with")
    private Integer seed;

    /** The simulator. */
    private OtsSimulatorInterfaceStep simulator;

    /** The network. */
    private RoadNetwork network;

    /** Duration of virtual lane change. */
    private final static Duration VIRTUAL_LC_DURATION = Duration.ofSI(3.0);

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
        OtsTransceiver otsTransceiver = new OtsTransceiver(args);
        if (otsTransceiver.otsOnly)
        {
            File file;
            if (otsTransceiver.fosFile != null && !otsTransceiver.fosFile.isBlank())
            {
                file = new File(otsTransceiver.fosFile);
            }
            else
            {
                JFileChooser chooser = new JFileChooser();
                FileNameExtensionFilter filter = new FileNameExtensionFilter("FOSIM file", "fos");
                chooser.setFileFilter(filter);
                int returnVal = chooser.showOpenDialog(null);
                if (returnVal == JFileChooser.APPROVE_OPTION)
                {
                    file = chooser.getSelectedFile();
                }
                else
                {
                    return;
                }
            }
            OtsRunner.run(file, otsTransceiver.showGui, otsTransceiver.detectorOutput, otsTransceiver.seed);
        }
        else
        {
            otsTransceiver.start();
        }
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
     * Returns the lane number.
     * @param laneId lane id
     * @return lane number
     */
    public static int getLaneRowFromId(final String laneId)
    {
        int underscore = laneId.indexOf("_");
        try
        {
            return Integer.parseInt(underscore < 0 ? laneId : laneId.substring(underscore + 1));
        }
        catch (NumberFormatException ex)
        {
            throw new OtsRuntimeException("Could not find row from lane. Is the GTU mapped to a dummy lane starting with '_'?",
                    ex);
        }
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
    protected class Worker extends Thread implements EventListener
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

        /** GTU types from parser. */
        private List<GtuType> gtuTypes;

        /** Detectors. */
        protected Map<String, FosDetector> detectors = new LinkedHashMap<>();

        /** Step number. */
        private int stepNumber = 1;

        /** Trajectory sampler. */
        private RoadSampler sampler;

        /** Target lane of lane changes. */
        private Map<Gtu, Integer> targetLane = new LinkedHashMap<>();

        /** Stop criterion during a batch run. */
        private StopCriterion stopCriterion;

        /** Graph paths. */
        private GraphPath<LaneDataRoad>[] graphPaths;

        /** Minute when time was shown. */
        private int shownMinute = -1;

        /** Map of lane change start times per GTU. */
        private Map<String, VirtualLaneChange> laneChanges = new LinkedHashMap<>();

        /** Trace files activated, with optional data storage. */
        private Map<Trace, TraceData> traceFiles = new EnumMap<>(Trace.class);

        /** Step in vehicle trace file. */
        private Duration vehiclesTraceStep = Duration.ofSI(0.5);

        /** Time until which detections data was returned. */
        private Duration lastDetectionTime;

        /** Time until which travel time data was returned. */
        private Duration lastTravelTimeTime;

        /** Time until which vehicle data was returned. */
        private Duration lastVehiclesTime;

        /** Listener to static vehicles trace data. */
        private StaticVehiclesTraceDataListener staticVehiclesTraceDataListener;

        /** Time column in sampler data for Vehicles trace data. */
        private Column<FloatDuration> tColumn;

        /** GTU id column in sampler data for Vehicles trace data. */
        private Column<String> gtuIdColumn;

        /** Link id column in sampler data for Vehicles trace data. */
        private Column<String> linkColumn;

        /** Lane id column in sampler data for Vehicles trace data. */
        private Column<String> laneColumn;

        /** Lane location column in sampler data for Vehicles trace data. */
        private Column<FloatLength> xColumn;

        /** Speed column in sampler data for Vehicles trace data. */
        private Column<FloatSpeed> vColumn;

        /** OD node name mappings (OTS names are the keys, Fosim names the fields). */
        private Map<String, Integer> odNumbers;

        /**
         * Constructor.
         */
        public Worker()
        {
        }

        @Override
        public void run()
        {
            this.context = new ZContext(1);
            this.responder = this.context.createSocket(SocketType.REP);
            this.responder.bind("tcp://*:" + port);
            this.responder.setReceiveTimeOut(5000);
            System.out.println("Server is running");

            try
            {
                while (!Thread.currentThread().isInterrupted())
                {
                    // Wait for next request from the client
                    byte[] request = this.responder.recv(); // ZMQ.DONTWAIT
                    while (request == null)
                    {
                        /*
                         * This code is left as a comment to later check how much speed this gains. As of 2026 jan-25 the
                         * simulation seems to be slow mostly due to excess logging. THis makes a benchmark impossible.
                         */
                        // try
                        // {
                        // // In order to allow resources to go to other processes, we sleep before checking again
                        // Thread.sleep(3);
                        // }
                        // catch (InterruptedException e)
                        // {
                        // }
                        request = this.responder.recv(); // ZMQ.DONTWAIT
                    }
                    /*
                     * We have transitioned from SIM02 to SIM03. SIM03 deals differently with little or big endianness. In
                     * particular all the little endian data types were removed and the endianness in the main message
                     * determines all endianness in individual payload objects. This is already how we used sim0mq under SIM02.
                     * Therefore we can safely change the 2 in to a 3 when receiving the message.
                     */
                    request[9] = (byte) 51; // makes the 10th byte represent "3"
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
                        Object[] payload = getVehiclePayload();
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
                        // TODO: We could perform the next step asynchronously?
                        BatchStatus triggered = batchStep();
                        this.responder.send(Sim0MQMessage.encodeUTF8(OtsTransceiver.this.bigEndian,
                                OtsTransceiver.this.federation, OtsTransceiver.this.ots, OtsTransceiver.this.fosim,
                                "BATCH_STEP_REPLY", this.messageId++, new Object[] {triggered.name()}), 0);
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
                        if (exceptionMessage.isEmpty())
                        {
                            setupTraceData();
                        }
                        this.responder.send(Sim0MQMessage.encodeUTF8(OtsTransceiver.this.bigEndian,
                                OtsTransceiver.this.federation, OtsTransceiver.this.ots, OtsTransceiver.this.fosim,
                                "SETUP_REPLY", this.messageId++, exceptionMessage), 0);
                    }
                    else if ("TRACE_FILES".equals(message.getMessageTypeId()))
                    {
                        Object[] payload = getTraceFilesPayload();
                        this.responder.send(Sim0MQMessage.encodeUTF8(OtsTransceiver.this.bigEndian,
                                OtsTransceiver.this.federation, OtsTransceiver.this.ots, OtsTransceiver.this.fosim,
                                "TRACE_FILES_REPLY", this.messageId++, payload), 0);
                    }
                    else if ("TRACE_ACTIVE".equals(message.getMessageTypeId()))
                    {
                        setTraceActive(message);
                        this.responder.send(Sim0MQMessage.encodeUTF8(OtsTransceiver.this.bigEndian,
                                OtsTransceiver.this.federation, OtsTransceiver.this.ots, OtsTransceiver.this.fosim,
                                "TRACE_ACTIVE_REPLY", this.messageId++, new Object[0]), 0);
                    }
                    else if ("TRACE_VEHICLES_STEP".equals(message.getMessageTypeId()))
                    {
                        Object[] payload = message.createObjectArray();
                        this.vehiclesTraceStep = (Duration) payload[8];
                        this.responder.send(Sim0MQMessage.encodeUTF8(OtsTransceiver.this.bigEndian,
                                OtsTransceiver.this.federation, OtsTransceiver.this.ots, OtsTransceiver.this.fosim,
                                "TRACE_VEHICLES_STEP_REPLY", this.messageId++, new Object[0]), 0);
                    }
                    else if ("TRACE_GET".equals(message.getMessageTypeId()))
                    {
                        Object[] payload = getTracePayload((String) message.createObjectArray()[8]);
                        this.responder.send(Sim0MQMessage.encodeUTF8(OtsTransceiver.this.bigEndian,
                                OtsTransceiver.this.federation, OtsTransceiver.this.ots, OtsTransceiver.this.fosim,
                                "TRACE_GET_REPLY", this.messageId++, payload), 0);
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
         * Returns payload on the information of supported trace files. This is based on the {@code Trace} class.
         * @return payload on the information of supported trace files
         */
        private Object[] getTraceFilesPayload()
        {
            Object[] payload = new Object[Trace.values().length * 4];
            int i = 0;
            for (Trace trace : Trace.values())
            {
                payload[i++] = trace.getInfo().id();
                payload[i++] = trace.getInfo().dutchName();
                payload[i++] = trace.getInfo().englishName();
                // TODO Header should become a String[]
                StringBuilder str = new StringBuilder();
                String sep = "";
                for (String col : trace.getInfo().header())
                {
                    str.append(sep).append(col);
                    sep = ", ";
                }
                payload[i++] = sep.toString();
            }
            return payload;
        }

        /**
         * Sets trace file active or not.
         * @param message message
         */
        private void setTraceActive(final Sim0MQMessage message)
        {
            Object[] payload = message.createObjectArray();
            Trace trace = Trace.byId((String) payload[8]);
            boolean enable = (Boolean) payload[9];
            if (enable)
            {
                this.traceFiles.put(trace, new TraceData(trace.getInfo().header().length));
            }
            else
            {
                this.traceFiles.remove(trace);
            }
        }

        /**
         * Sets up active trace file data recording.
         */
        @SuppressWarnings("unchecked")
        private void setupTraceData()
        {
            this.traceFiles.values().forEach((td) -> td.clear());
            if (this.traceFiles.containsKey(Trace.ACCELERATION_CHANGE))
            {
                new AccelerationChangeListener(OtsTransceiver.this.network, this.gtuTypes,
                        this.traceFiles.get(Trace.ACCELERATION_CHANGE));
            }
            if (this.traceFiles.containsKey(Trace.LANE_CHANGE))
            {
                new LaneChangeListener(OtsTransceiver.this.network, this.gtuTypes, this.traceFiles.get(Trace.LANE_CHANGE));
            }
            if (this.traceFiles.containsKey(Trace.OD_TRAVEL_TIME))
            {
                new OdTravelTimeListener(OtsTransceiver.this.network, this.gtuTypes, this.odNumbers,
                        this.traceFiles.get(Trace.OD_TRAVEL_TIME));
            }
            if (this.traceFiles.containsKey(Trace.VEHICLES))
            {
                this.staticVehiclesTraceDataListener = new StaticVehiclesTraceDataListener(OtsTransceiver.this.network);
                this.tColumn = (Column<FloatDuration>) this.sampler.getSamplerData()
                        .getColumn(this.sampler.getSamplerData().getColumnNumber("t"));
                this.gtuIdColumn = (Column<String>) this.sampler.getSamplerData()
                        .getColumn(this.sampler.getSamplerData().getColumnNumber("gtuId"));
                this.linkColumn = (Column<String>) this.sampler.getSamplerData()
                        .getColumn(this.sampler.getSamplerData().getColumnNumber("linkId"));
                this.laneColumn = (Column<String>) this.sampler.getSamplerData()
                        .getColumn(this.sampler.getSamplerData().getColumnNumber("laneId"));
                this.xColumn = (Column<FloatLength>) this.sampler.getSamplerData()
                        .getColumn(this.sampler.getSamplerData().getColumnNumber("x"));
                this.vColumn = (Column<FloatSpeed>) this.sampler.getSamplerData()
                        .getColumn(this.sampler.getSamplerData().getColumnNumber("v"));
            }
            // Detection, TravelTime and Vehicles are based on detectors and the sampler
        }

        /**
         * Returns the payload of a trace file.
         * @param traceId id of the trace file
         * @return
         */
        private Object[] getTracePayload(final String traceId)
        {
            switch (traceId)
            {
                case Trace.Info.ACCELERATION_CHANGE_ID:
                {
                    TraceData data = this.traceFiles.get(Trace.ACCELERATION_CHANGE);
                    Object[] payload = new Object[10];
                    int index = 0;
                    payload[++index] = data.asDuration(index - 1); // t
                    payload[++index] = data.asInteger(index - 1); // fromln
                    payload[++index] = data.asInteger(index - 1); // tolane
                    payload[++index] = data.asAcceleration(index - 1); // from a
                    payload[++index] = data.asAcceleration(index - 1); // to a
                    payload[++index] = data.asLength(index - 1); // pos
                    payload[++index] = data.asSpeed(index - 1); // v
                    payload[++index] = data.asInteger(index - 1); // type
                    payload[++index] = data.asInteger(index - 1); // id
                    payload[0] = traceId;
                    data.clear();
                    return payload;
                }
                case Trace.Info.DETECTION_ID:
                case Trace.Info.TRAVEL_TIME_ID:
                {
                    // pos, lane, t, ___ v, type, id, dest
                    // pos, lane, t, dt, v, type, id, dest
                    boolean addTravelTime = traceId.equals(Trace.Info.TRAVEL_TIME_ID);
                    Duration time = addTravelTime ? this.lastTravelTimeTime : this.lastDetectionTime;
                    int numberOfColumns = addTravelTime ? 8 : 7;
                    TraceData data = new TraceData(numberOfColumns);
                    this.detectors.values().forEach((d) -> addTraceDataFromPassings(d, time, data, addTravelTime));
                    Object[] payload = new Object[numberOfColumns + 1];
                    int index = 0;
                    payload[++index] = data.asLength(index - 1); // pos
                    payload[++index] = data.asInteger(index - 1); // lane
                    payload[++index] = data.asDuration(index - 1); // t
                    if (addTravelTime)
                    {
                        payload[++index] = data.asDuration(index - 1); // dt
                    }
                    payload[++index] = data.asSpeed(index - 1); // v
                    payload[++index] = data.asInteger(index - 1); // type
                    payload[++index] = data.asInteger(index - 1); // id
                    payload[++index] = data.asInteger(index - 1); // dest
                    payload[0] = traceId;
                    if (addTravelTime)
                    {
                        this.lastTravelTimeTime = OtsTransceiver.this.simulator.getSimulatorTime();
                    }
                    else
                    {
                        this.lastDetectionTime = OtsTransceiver.this.simulator.getSimulatorTime();
                    }
                    return payload;
                }
                case Trace.Info.LANE_CHANGE_ID:
                {
                    TraceData data = this.traceFiles.get(Trace.LANE_CHANGE);
                    Object[] payload = new Object[7];
                    int index = 0;
                    payload[++index] = data.asDuration(index - 1); // t
                    payload[++index] = data.asInteger(index - 1); // fromln
                    payload[++index] = data.asInteger(index - 1); // tolane
                    payload[++index] = data.asLength(index - 1); // pos
                    payload[++index] = data.asInteger(index - 1); // type
                    payload[++index] = data.asInteger(index - 1); // id
                    payload[0] = traceId;
                    data.clear();
                    return payload;
                }
                case Trace.Info.OD_TRAVEL_TIME_ID:
                {
                    TraceData data = this.traceFiles.get(Trace.OD_TRAVEL_TIME);
                    Object[] payload = new Object[8];
                    int index = 0;
                    payload[++index] = data.asDuration(index - 1); // t
                    payload[++index] = data.asInteger(index - 1); // origin
                    payload[++index] = data.asInteger(index - 1); // dest
                    payload[++index] = data.asDuration(index - 1); // tt
                    payload[++index] = data.asSpeed(index - 1); // v
                    payload[++index] = data.asInteger(index - 1); // type
                    payload[++index] = data.asInteger(index - 1); // id
                    payload[0] = traceId;
                    data.clear();
                    return payload;
                }
                case Trace.Info.VEHICLES_ID:
                {
                    // t, id, type, origin, dest, lane, pos, v
                    TraceData data = new TraceData(8);
                    this.sampler.getSamplerData().forEach((row) -> addVehiclesTraceRow(row, data));
                    Object[] payload = new Object[9];
                    int index = 0;
                    payload[++index] = data.asDuration(index - 1); // t
                    payload[++index] = data.asInteger(index - 1); // id
                    payload[++index] = data.asInteger(index - 1); // type
                    payload[++index] = data.asInteger(index - 1); // origin
                    payload[++index] = data.asInteger(index - 1); // dest
                    payload[++index] = data.asInteger(index - 1); // lane
                    payload[++index] = data.asLength(index - 1); // pos
                    payload[++index] = data.asSpeed(index - 1); // v
                    payload[0] = traceId;
                    this.lastVehiclesTime = OtsTransceiver.this.simulator.getSimulatorTime();
                    return payload;
                }
                default:
                {
                    return new Object[0];
                }
            }
        }

        /**
         * Add passing data to trace data for a single detector.
         * @param detector detector
         * @param since time since last data was sent
         * @param data object to add data to
         * @param addTravelTime whether to include the travel time since last detector data
         */
        private void addTraceDataFromPassings(final FosDetector detector, final Duration since, final TraceData data,
                final boolean addTravelTime)
        {
            FloatLength pos = FloatLength.ofSI((float) detector.getLocation().x);
            Integer lane = getLaneRowFromId(detector.getLane().getId());
            for (Passing passing : detector.getPassings(since))
            {
                if (!addTravelTime || passing.travelTimeSinceDetector() != null)
                {
                    Object[] row = new Object[addTravelTime ? 8 : 7];
                    int index = 0;
                    row[index++] = pos; // pos
                    row[index++] = lane; // lane
                    row[index++] = FloatDuration.ofSI((float) passing.time().si); // t
                    if (addTravelTime)
                    {
                        row[index++] = FloatDuration.ofSI((float) passing.travelTimeSinceDetector().si); // dt
                    }
                    row[index++] = FloatSpeed.ofSI((float) passing.speed().si); // v
                    row[index++] = this.gtuTypes.indexOf(passing.gtuType()); // type
                    row[index++] = Integer.valueOf(passing.id()); // id
                    row[index++] = this.odNumbers.get(passing.destination()); // dest
                    data.append(row);
                }
            }
            detector.clearPassingsUpTo(Duration.min(this.lastDetectionTime, this.lastTravelTimeTime));
        }

        /**
         * Adds sampler row to vehicles trace data, if it is at the right vehicles trace step in time.
         * @param row sampler row
         * @param data vehicles trace data
         */
        private void addVehiclesTraceRow(final Row row, final TraceData data)
        {
            FloatDuration t = row.getValue(this.tColumn);
            if (t.si % this.vehiclesTraceStep.si < 1e-6 && t.si > this.lastVehiclesTime.si - 1e-6)
            {
                // t, id, type, origin, dest, lane, pos, v
                String id = row.getValue(this.gtuIdColumn);
                int type = this.gtuTypes.indexOf(this.staticVehiclesTraceDataListener.getGtuType(id));
                int origin = this.odNumbers.get(this.staticVehiclesTraceDataListener.getOrigin(id));
                int dest = this.odNumbers.get(this.staticVehiclesTraceDataListener.getDestination(id));
                double x0 =
                        OtsTransceiver.this.network.getLink(row.getValue(this.linkColumn)).get().getStartNode().getPoint().x;
                int lane = getLaneRowFromId(row.getValue(this.laneColumn));
                FloatLength pos = FloatLength.ofSI((float) (x0 + row.getValue(this.xColumn).si));
                FloatSpeed v = FloatSpeed.ofSI((float) row.getValue(this.vColumn).si);
                data.append(t, Integer.valueOf(id), type, origin, dest, lane, pos, v);
            }
        }

        /**
         * Obtains vehicle message payload in case of virtual lane changes.
         * @return payload
         * @throws GtuException
         */
        private Object[] getVehiclePayload() throws GtuException
        {
            int numGtus = OtsTransceiver.this.network.getGTUs().size();
            Duration now = OtsTransceiver.this.simulator.getSimulatorTime();
            for (Gtu gtu : OtsTransceiver.this.network.getGTUs())
            {
                String gtuId = gtu.getId();
                if (this.laneChanges.containsKey(gtuId))
                {
                    VirtualLaneChange lc = this.laneChanges.get(gtuId);
                    if (now.si - lc.time.si >= VIRTUAL_LC_DURATION.si)
                    {
                        this.laneChanges.remove(gtuId);
                    }
                }
            }
            int lcGtus = this.laneChanges.size();
            Object[] payload = new Object[1 + 5 * (numGtus - lcGtus) + 7 * lcGtus];
            payload[0] = numGtus;
            int k = 1;
            for (Gtu gtu : OtsTransceiver.this.network.getGTUs())
            {
                LanePosition pos = ((LaneBasedGtu) gtu).getPosition();
                double front = gtu.getFront().dx().si;
                double laneStart = pos.lane().getLink().getStartNode().getPoint().x;
                Length position = Length.ofSI(pos.position().si + front + laneStart);
                int lane = getLaneRowFromId(pos.lane().getId());

                payload[k++] = lane;
                payload[k++] = position;
                payload[k++] = gtu.getSpeed();
                payload[k++] = gtu.getAcceleration();

                String gtuId = gtu.getId();
                if (this.laneChanges.containsKey(gtuId))
                {
                    VirtualLaneChange lc = this.laneChanges.get(gtuId);
                    // 1 = for overtaking, 2 = for destination
                    payload[k++] = lc.overtaking ? 1 : 2;
                    payload[k++] = lc.left;
                    payload[k++] = (now.si - lc.time.si) / VIRTUAL_LC_DURATION.si;
                }
                else
                {
                    payload[k++] = 0;
                }
            }
            return payload;
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
                stopSimulation();
                String fosString = (String) message.createObjectArray()[8];
                Map<ParserSetting, Boolean> settings = new LinkedHashMap<>();
                settings.put(ParserSetting.GUI, OtsTransceiver.this.showGui);
                settings.put(ParserSetting.FOS_DETECTORS, true);
                FosParser parser = new FosParser().setSettings(settings);
                parser.parseFromString(fosString);
                this.gtuTypes = parser.getGtuTypes();
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
                this.gtuTypes = parser.getGtuTypes();
                this.lastDetectionTime = Duration.ofSI(-1.0);
                this.lastTravelTimeTime = Duration.ofSI(-1.0);
                this.lastVehiclesTime = Duration.ofSI(-1.0);
                this.odNumbers = parser.getOdNameMappings();
                setupSampler(parser);
                setupVirtualLaneChanges();
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
        private void setupSampler(final FosParser parser) throws NetworkException
        {
            FosSampler fosSampler = new FosSampler(parser, network, Frequency.ofSI(1.0 / OtsTransceiver.this.step.si));
            this.sampler = fosSampler.getSampler();
            this.graphPaths = fosSampler.getLaneGraphPaths();
        }

        /**
         * Listen to lane change events to maintain 3s bookkeeping.
         */
        private void setupVirtualLaneChanges()
        {
            OtsTransceiver.this.network.addListener(this, Network.GTU_ADD_EVENT);
            OtsTransceiver.this.network.addListener(this, Network.GTU_REMOVE_EVENT);
        }

        @Override
        public void notify(final Event event)
        {
            if (event.getType().equals(LaneBasedGtu.LANE_CHANGE_EVENT))
            {
                Object[] content = (Object[]) event.getContent();
                String gtuId = (String) content[0];
                Gtu gtu = OtsTransceiver.this.network.getGTU(gtuId).get();
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
                double mandatoryDesire = lmrs.getLatestDesire(FosIncentiveRoute.class).get().get(dir);
                boolean overtaking = mandatoryDesire / totalDesire < 0.5;

                this.laneChanges.put(gtuId,
                        new VirtualLaneChange(dir.isLeft(), OtsTransceiver.this.simulator.getSimulatorTime(), overtaking));
            }
            else if (event.getType().equals(Network.GTU_ADD_EVENT))
            {
                String id = (String) event.getContent();
                Gtu gtu = OtsTransceiver.this.network.getGTU(id).get();
                gtu.addListener(this, LaneBasedGtu.LANE_CHANGE_EVENT);
            }
            else if (event.getType().equals(Network.GTU_REMOVE_EVENT))
            {
                String id = (String) event.getContent();
                Gtu gtu = OtsTransceiver.this.network.getGTU(id).get();
                gtu.removeListener(this, LaneBasedGtu.LANE_CHANGE_EVENT);
                this.laneChanges.remove(id);
            }
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
            double tNow = OtsTransceiver.this.simulator.getSimulatorTime().si;
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
            if (OtsTransceiver.this.network.getSimulator().getSimulatorTime().si >= OtsTransceiver.this.network.getSimulator()
                    .getReplication().getEndTime().si)
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
            final Duration startTime = (Duration) payloadIn[8];
            final Duration finishTime = (Duration) payloadIn[9];
            final Length startPosition = (Length) payloadIn[10];
            final Length finishPosition = (Length) payloadIn[11];
            final int granularity = (int) payloadIn[12]; // number of time steps

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
                Length startPositionLane = Length.ofSI(startPosition.si - x0);
                Length finishPositionLane = Length.ofSI(finishPosition.si - x0);
                if (x0 < finishPosition.si && x1 > startPosition.si)
                {
                    for (Trajectory<?> trajectory : this.sampler.getSamplerData().getTrajectoryGroup(laneData).get())
                    {
                        boolean spatialOverlap = trajectory.getX(0) < finishPositionLane.si
                                && trajectory.getX(trajectory.size() - 1) > startPositionLane.si;
                        if (spatialOverlap && GraphUtil.considerTrajectory(trajectory, startTime, finishTime))
                        {
                            trajectoriesPerGtu.computeIfAbsent(trajectory.getGtuId(), (id) -> new TreeSet<>(trajectoryComp))
                                    .add(trajectory);
                            laneOfTrajectory.put(trajectory, lane);
                        }
                    }
                }
            }

            float tMin = (float) startTime.si;
            float tMax = (float) finishTime.si;
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
                    int laneNum = getLaneRowFromId(laneRoad.getId());
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
            this.targetLane.clear();
            this.graphPaths = null;
            this.laneChanges.clear();
            this.lastDetectionTime = null;
            this.lastTravelTimeTime = null;
            this.lastVehiclesTime = null;
            this.staticVehiclesTraceDataListener = null;
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
            builder.registerTypeAdapter(Limit.class, new LimitAdapter());
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
    public record VirtualLaneChange(boolean left, Duration time, boolean overtaking)
    {
    }
}
