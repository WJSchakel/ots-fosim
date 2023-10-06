package org.opentrafficsim.fosim.sim0mq;

import java.awt.Dimension;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
import org.opentrafficsim.core.animation.gtu.colorer.GTUColorer;
import org.opentrafficsim.core.dsol.AbstractOTSModel;
import org.opentrafficsim.core.dsol.OTSAnimator;
import org.opentrafficsim.core.dsol.OTSSimulator;
import org.opentrafficsim.core.dsol.OTSSimulatorInterface;
import org.opentrafficsim.core.geometry.OTSGeometryException;
import org.opentrafficsim.core.geometry.OTSLine3D;
import org.opentrafficsim.core.geometry.OTSPoint3D;
import org.opentrafficsim.core.gtu.GTU;
import org.opentrafficsim.core.gtu.GTUDirectionality;
import org.opentrafficsim.core.gtu.GTUException;
import org.opentrafficsim.core.network.LinkType;
import org.opentrafficsim.core.network.NetworkException;
import org.opentrafficsim.draw.core.OTSDrawingException;
import org.opentrafficsim.road.gtu.generator.od.ODApplier;
import org.opentrafficsim.road.gtu.generator.od.ODOptions;
import org.opentrafficsim.road.gtu.lane.LaneBasedGTU;
import org.opentrafficsim.road.gtu.strategical.od.Categorization;
import org.opentrafficsim.road.gtu.strategical.od.Category;
import org.opentrafficsim.road.gtu.strategical.od.Interpolation;
import org.opentrafficsim.road.gtu.strategical.od.ODMatrix;
import org.opentrafficsim.road.network.OTSRoadNetwork;
import org.opentrafficsim.road.network.lane.CrossSectionLink;
import org.opentrafficsim.road.network.lane.Lane;
import org.opentrafficsim.road.network.lane.LaneType;
import org.opentrafficsim.road.network.lane.OTSRoadNode;
import org.opentrafficsim.road.network.lane.changing.LaneKeepingPolicy;
import org.opentrafficsim.road.network.lane.object.sensor.SinkSensor;
import org.opentrafficsim.swing.gui.OTSAnimationPanel;
import org.opentrafficsim.swing.gui.OTSSimulationApplication;
import org.opentrafficsim.swing.gui.OTSSwingApplication;
import org.sim0mq.Sim0MQException;
import org.sim0mq.message.Sim0MQMessage;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import nl.tudelft.simulation.dsol.SimRuntimeException;
import nl.tudelft.simulation.jstats.streams.MersenneTwister;
import nl.tudelft.simulation.jstats.streams.StreamInterface;
import nl.tudelft.simulation.language.DSOLException;
import picocli.CommandLine.Option;

public class TechnicalDemo
{

    /** Federation id to receive/sent messages. */
    @Option(names = "--federationId", description = "Federation id to receive/sent messages", defaultValue = "OTS_Fosim")
    private String federation;

    /** OTS id to receive/sent messages. */
    @Option(names = "--otsId", description = "OTS id to receive/sent messages", defaultValue = "OTS")
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
    @Option(names = "--step", description = "Simulation step", defaultValue = "0.5s")
    private Duration step;

    /** Show GUI. */
    @Option(names = "--gui", description = "Whether to show GUI", defaultValue = "false")
    private boolean showGUI;

    /** The simulator. */
    private OTSSimulatorInterface simulator;

    /** The network. */
    private OTSRoadNetwork network;

    /** Application screen. */
    private OTSSimulationApplication<FosimModel> app;

    /** Step number. */
    private int stepNumber = 1;

    /**
     * Main method.
     * @param args String[]; command line arguments.
     * @throws Exception on any exception during simulation.
     */
    public static void main(String... args) throws Exception
    {
        TechnicalDemo demo = new TechnicalDemo();
        CliUtil.execute(demo, args);
        demo.setupSimulator();
    }

    /**
     * Starts a simulator/animator
     * @throws SimRuntimeException
     * @throws NamingException
     * @throws RemoteException
     * @throws DSOLException
     * @throws OTSDrawingException
     */
    private void setupSimulator()
            throws SimRuntimeException, NamingException, RemoteException, DSOLException, OTSDrawingException
    {
        Duration simulationTime = Duration.instantiateSI(3600.0);
        if (!this.showGUI)
        {
            this.simulator = new OTSSimulator("OTS-Fosim");
            final FosimModel fosimModel = new FosimModel(this.simulator);
            this.simulator.initialize(Time.ZERO, Duration.ZERO, simulationTime, fosimModel);
        }
        else
        {
            OTSAnimator animator = new OTSAnimator("OTS-Fosim");
            this.simulator = animator;
            final FosimModel fosimModel = new FosimModel(this.simulator);
            this.simulator.initialize(Time.ZERO, Duration.ZERO, simulationTime, fosimModel);
            GTUColorer colorer = OTSSwingApplication.DEFAULT_COLORER;
            OTSAnimationPanel animationPanel = new OTSAnimationPanel(fosimModel.getNetwork().getExtent(),
                    new Dimension(800, 600), (OTSAnimator) this.simulator, fosimModel, colorer, fosimModel.getNetwork());
            this.app = new OTSSimulationApplication<FosimModel>(fosimModel, animationPanel);
            this.app.setExitOnClose(true);
            animator.setAnimation(false);
            animator.setSpeedFactor(Double.MAX_VALUE, false);
            // animationPanel.enableSimulationControlButtons();
        }
        new Worker().start();
    }

    /**
     * Builds the network and demand.
     * @param sim OTSSimulatorInterface; simulator.
     * @return OTSRoadNetwork; network.
     * @throws NetworkException
     * @throws OTSGeometryException
     * @throws ParameterException
     * @throws SimRuntimeException
     */
    private OTSRoadNetwork setupSimulation(final OTSSimulatorInterface sim)
            throws NetworkException, OTSGeometryException, SimRuntimeException, ParameterException
    {
        OTSRoadNetwork network = new OTSRoadNetwork("OTS-Fosim", true, sim);

        OTSPoint3D pointFrom = new OTSPoint3D(0.0, -1.75, 0.0);
        OTSPoint3D pointTo = new OTSPoint3D(2000.0, -1.75, 0.0);

        OTSRoadNode nodeFrom = new OTSRoadNode(network, "From", pointFrom, Direction.ZERO);
        OTSRoadNode nodeTo = new OTSRoadNode(network, "To", pointTo, Direction.ZERO);

        OTSLine3D designLine = new OTSLine3D(pointFrom, pointTo);
        LinkType linkType = network.getLinkType(LinkType.DEFAULTS.FREEWAY);
        CrossSectionLink link =
                new CrossSectionLink(network, "Link", nodeFrom, nodeTo, linkType, designLine, LaneKeepingPolicy.KEEPRIGHT);

        LaneType laneType = network.getLaneType(LaneType.DEFAULTS.FREEWAY);
        Lane lane =
                new Lane(link, "1", Length.ZERO, Length.instantiateSI(3.5), laneType, new Speed(100.0, SpeedUnit.KM_PER_HOUR));

        DoubleVectorData timeData =
                DoubleVectorData.instantiate(new double[] {0.0, 3600.0}, TimeUnit.BASE_SECOND.getScale(), StorageType.DENSE);
        TimeVector timeVector = new TimeVector(timeData, TimeUnit.BASE_SECOND);
        List<OTSRoadNode> origins = new ArrayList<>();
        origins.add(nodeFrom);
        List<OTSRoadNode> destinations = new ArrayList<>();
        destinations.add(nodeTo);
        ODMatrix od = new ODMatrix("OD", origins, destinations, Categorization.UNCATEGORIZED, timeVector, Interpolation.LINEAR);
        DoubleVectorData flowData = DoubleVectorData.instantiate(new double[] {1500.0, 1500.0},
                FrequencyUnit.PER_HOUR.getScale(), StorageType.DENSE);
        FrequencyVector flowVector = new FrequencyVector(flowData, FrequencyUnit.PER_HOUR);
        od.putDemandVector(nodeFrom, nodeTo, Category.UNCATEGORIZED, flowVector);

        ODOptions odOptions = new ODOptions();
        ODApplier.applyOD(network, od, odOptions);

        new SinkSensor(lane, Length.instantiateSI(1950.0), GTUDirectionality.DIR_PLUS, sim);

        return network;
    }

    /**
     * Run a simulation step, where a 'step' is defined as a fixed time step. Note that within OTS usually a step is defined as
     * a single event in DSOL.
     */
    private void step()
    {
        Duration until = this.step.times(this.stepNumber++);
        if (this.simulator.isStartingOrRunning())
        {
            this.simulator.stop();
        }
        this.simulator.runUpToAndIncluding(until);
        while (this.simulator.isStartingOrRunning())
        {
            Thread.onSpinWait();
        }
    }

    /**
     * Model.
     * @author wjschakel
     */
    private class FosimModel extends AbstractOTSModel
    {
        /** */
        private static final long serialVersionUID = 20180409L;

        /**
         * Constructor.
         * @param simulator OTSSimulatorInterface; the simulator
         */
        FosimModel(final OTSSimulatorInterface simulator)
        {
            super(simulator);
            TechnicalDemo.this.simulator = simulator;
        }

        /** {@inheritDoc} */
        @Override
        public void constructModel() throws SimRuntimeException
        {
            Map<String, StreamInterface> streams = new LinkedHashMap<>();
            long seed = 1L;
            StreamInterface stream = new MersenneTwister(seed);
            streams.put("generation", stream);
            stream = new MersenneTwister(seed + 1);
            streams.put("default", stream);
            TechnicalDemo.this.simulator.getModel().getStreams().putAll(streams);
            TechnicalDemo.this.network = Try.assign(() -> TechnicalDemo.this.setupSimulation(TechnicalDemo.this.simulator),
                    RuntimeException.class, "Exception while setting up simulation.");
        }

        /** {@inheritDoc} */
        @Override
        public OTSRoadNetwork getNetwork()
        {
            return TechnicalDemo.this.network;
        }

        /** {@inheritDoc} */
        @Override
        public Serializable getSourceId()
        {
            return getShortName();
        }
    }

    /**
     * Worker thread to listen to messages and respond.
     * @author wjschakel
     */
    private class Worker extends Thread
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
            this.responder.bind("tcp://*:" + TechnicalDemo.this.port);
            System.out.println("Server is running");

            try
            {
                while (!Thread.currentThread().isInterrupted())
                {
                    // Wait for next request from the client
                    byte[] request = this.responder.recv(0);
                    Sim0MQMessage message = Sim0MQMessage.decode(request);
                    if ("STEP".equals(message.getMessageTypeId()))
                    {
                        // System.out.println("Performing STEP");
                        TechnicalDemo.this.step(); // Performance of this line is terrible when using an OTSAnimator
                        int numGtus = TechnicalDemo.this.network.getGTUs().size();
                        Object[] payload = new Object[1 + 4 * numGtus];
                        payload[0] = numGtus;
                        int k = 1;
                        for (GTU gtu : TechnicalDemo.this.network.getGTUs())
                        {
                            payload[k++] = Integer.parseInt(((LaneBasedGTU) gtu).getReferencePosition().getLane().getId());
                            payload[k++] = Length.instantiateSI(gtu.getLocation().x);
                            payload[k++] = gtu.getSpeed();
                            payload[k++] = gtu.getAcceleration();
                        }
                        // System.out.println("OTS replies STEP command with " + numGtus + " GTUs");
                        this.responder.send(
                                Sim0MQMessage.encodeUTF8(TechnicalDemo.this.bigEndian, TechnicalDemo.this.federation,
                                        TechnicalDemo.this.ots, TechnicalDemo.this.fosim, "STEP_REPLY", messageId++, payload),
                                0);
                    }
                    else if ("STOP".equals(message.getMessageTypeId()))
                    {
                        System.out.println("OTS received STOP command at " + TechnicalDemo.this.simulator.getSimulatorTime());
                        break;
                    }
                }
            }
            catch (Sim0MQException | SerializationException | NumberFormatException | GTUException e)
            {
                e.printStackTrace();
            }
            this.responder.close();
            this.context.destroy();
            this.context.close();
            System.out.println("OTS terminated");
            System.exit(0);
        }

    }

}
