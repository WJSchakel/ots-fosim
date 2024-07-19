package org.opentrafficsim.fosim.sim0mq;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.WindowConstants;

import org.djunits.value.vdouble.scalar.Duration;
import org.djunits.value.vdouble.scalar.Length;
import org.djutils.cli.CliUtil;
import org.djutils.exceptions.Try;
import org.djutils.serialization.SerializationException;
import org.opentrafficsim.base.parameters.ParameterException;
import org.opentrafficsim.core.dsol.OtsAnimator;
import org.opentrafficsim.core.dsol.OtsSimulatorInterface;
import org.opentrafficsim.core.gtu.Gtu;
import org.opentrafficsim.core.gtu.GtuException;
import org.opentrafficsim.fosim.parameters.DefaultValue;
import org.opentrafficsim.fosim.parameters.DefaultValueAdapter;
import org.opentrafficsim.fosim.parameters.ParameterDefinitions;
import org.opentrafficsim.fosim.parameters.distributions.DistributionDefinitions;
import org.opentrafficsim.fosim.parser.FosParser;
import org.opentrafficsim.fosim.parser.ParserSetting;
import org.opentrafficsim.road.gtu.lane.LaneBasedGtu;
import org.opentrafficsim.road.gtu.lane.plan.operational.LaneChange;
import org.opentrafficsim.road.gtu.lane.tactical.lmrs.IncentiveRoute;
import org.opentrafficsim.road.gtu.lane.tactical.lmrs.Lmrs;
import org.opentrafficsim.road.gtu.lane.tactical.util.lmrs.LmrsParameters;
import org.opentrafficsim.road.network.RoadNetwork;
import org.opentrafficsim.road.network.lane.LanePosition;
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
    @Option(names = "--gui", description = "Whether to show GUI", defaultValue = "true")
    protected boolean showGUI;

    /** App when GUI is used. */
    protected OtsSimulationApplication<FosimModel> app;

    /** The simulator. */
    protected OtsSimulatorInterface simulator;

    /** The network. */
    protected RoadNetwork network;

    /** Step number. */
    private int stepNumber = 1;

    /**
     * Constructor.
     * @param args String[]; command line arguments.
     * @throws Exception on any exception during simulation.
     */
    protected OtsTransceiver(final String... args) throws Exception
    {
        CliUtil.execute(this, args);
    }

    /**
     * Main method.
     * @param args String[]; command line arguments.
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
     * Run a simulation step, where a 'step' is defined as a fixed time step. Note that within Ots usually a step is defined as
     * a single event in DSOL.
     */
    private synchronized void step()
    {
        Duration until = this.step.times(this.stepNumber++);
        if (!this.showGUI)
        {
            this.simulator.scheduleEventAbs(until, this, "showTime", null);
            while (this.simulator.getSimulatorTime().lt(until))
            {
                this.simulator.step();
            }
        }
        else
        {
            if (this.simulator.isStartingOrRunning())
            {
                this.simulator.stop();
            }
            this.simulator.runUpToAndIncluding(until);
            while (this.simulator.isStartingOrRunning())
            {
                try
                {
                    // In order to allow resources to go to other processes, we sleep before
                    // checking again
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
            this.responder.bind("tcp://*:" + OtsTransceiver.this.port);
            System.out.println("Server is running");

            Map<Gtu, Integer> targetLane = new LinkedHashMap<>();
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
                            // In order to allow resources to go to other processes, we sleep before
                            // checking again
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
                        OtsTransceiver.this.step();
                        this.responder.send(Sim0MQMessage.encodeUTF8(OtsTransceiver.this.bigEndian,
                                OtsTransceiver.this.federation, OtsTransceiver.this.ots, OtsTransceiver.this.fosim,
                                "STEP_REPLY", this.messageId++, new Object[0]), 0);
                    }
                    else if ("VEHICLES".equals(message.getMessageTypeId()))
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
                        for (Gtu gtu : OtsTransceiver.this.network.getGTUs())
                        {
                            LanePosition pos = ((LaneBasedGtu) gtu).getReferencePosition();
                            double front = gtu.getFront().getDx().si;
                            double laneStart = pos.getLane().getParentLink().getStartNode().getPoint().x;
                            Length position = Length.instantiateSI(pos.getPosition().si + front + laneStart);
                            String laneId = pos.getLane().getId();
                            int underscore = laneId.indexOf("_");
                            int lane = Integer.parseInt(underscore < 0 ? laneId : laneId.substring(underscore + 1));
                            LaneChange lc = lcInfo.get(gtu);
                            if (lc == null)
                            {
                                Integer target = targetLane.remove(gtu);
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
                                targetLane.put(gtu, lane);
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
                        this.responder.send(Sim0MQMessage.encodeUTF8(OtsTransceiver.this.bigEndian,
                                OtsTransceiver.this.federation, OtsTransceiver.this.ots, OtsTransceiver.this.fosim,
                                "VEHICLES_REPLY", this.messageId++, payload), 0);
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
                        String fosString = (String) message.createObjectArray()[8];
                        Map<ParserSetting, Boolean> settings = new LinkedHashMap<>();
                        settings.put(ParserSetting.GUI, OtsTransceiver.this.showGUI);
                        FosParser parser = new FosParser().setSettings(settings);
                        Try.execute(() -> parser.parseFromString(fosString), "Unable to setup OTS simulation.");
                        OtsTransceiver.this.network = parser.getNetwork();
                        OtsTransceiver.this.simulator = OtsTransceiver.this.network.getSimulator();
                        if (OtsTransceiver.this.showGUI)
                        {
                            OtsTransceiver.this.app = parser.getApplication();
                            OtsTransceiver.this.app.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
                            OtsTransceiver.this.app.getAnimationPanel().disableSimulationControlButtons();
                            ((OtsAnimator) OtsTransceiver.this.simulator).setSpeedFactor(Double.MAX_VALUE, false);
                        }
                        this.responder.send(Sim0MQMessage.encodeUTF8(OtsTransceiver.this.bigEndian,
                                OtsTransceiver.this.federation, OtsTransceiver.this.ots, OtsTransceiver.this.fosim,
                                "SETUP_REPLY", this.messageId++, new Object[0]), 0);
                    }
                    else if ("STOP".equals(message.getMessageTypeId()))
                    {
                        OtsTransceiver.this.simulator = null;
                        OtsTransceiver.this.network = null;
                        if (OtsTransceiver.this.app != null)
                        {
                            OtsTransceiver.this.app.setVisible(false);
                            OtsTransceiver.this.app.dispose();
                            OtsTransceiver.this.app = null;
                        }
                        OtsTransceiver.this.stepNumber = 1;
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
         * Transforms an object in to a JSON string.
         * @param object Object; object.
         * @return String; JSON string representation of object.
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

    }
}
