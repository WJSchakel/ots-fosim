package org.opentrafficsim.fosim.parser;

import static org.opentrafficsim.road.gtu.lane.tactical.util.lmrs.Synchronization.gentleUrgency;
import static org.opentrafficsim.road.gtu.lane.tactical.util.lmrs.Synchronization.getMergeDistance;

import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.Function;

import javax.naming.NamingException;

import org.djunits.unit.AccelerationUnit;
import org.djunits.unit.SpeedUnit;
import org.djunits.unit.TimeUnit;
import org.djunits.value.storage.StorageType;
import org.djunits.value.vdouble.scalar.Acceleration;
import org.djunits.value.vdouble.scalar.Direction;
import org.djunits.value.vdouble.scalar.Duration;
import org.djunits.value.vdouble.scalar.Length;
import org.djunits.value.vdouble.scalar.Speed;
import org.djunits.value.vdouble.scalar.Time;
import org.djunits.value.vdouble.vector.TimeVector;
import org.djunits.value.vdouble.vector.data.DoubleVectorData;
import org.djutils.draw.line.Polygon2d;
import org.djutils.draw.point.Point2d;
import org.djutils.exceptions.Throw;
import org.djutils.exceptions.Try;
import org.djutils.immutablecollections.ImmutableHashSet;
import org.djutils.immutablecollections.ImmutableSet;
import org.opentrafficsim.animation.gtu.colorer.GtuColorer;
import org.opentrafficsim.base.geometry.OtsGeometryException;
import org.opentrafficsim.base.geometry.OtsLine2d;
import org.opentrafficsim.base.parameters.ParameterException;
import org.opentrafficsim.base.parameters.ParameterSet;
import org.opentrafficsim.base.parameters.ParameterTypes;
import org.opentrafficsim.base.parameters.Parameters;
import org.opentrafficsim.core.definitions.DefaultsNl;
import org.opentrafficsim.core.distributions.Generator;
import org.opentrafficsim.core.dsol.OtsAnimator;
import org.opentrafficsim.core.dsol.OtsSimulatorInterface;
import org.opentrafficsim.core.geometry.FractionalLengthData;
import org.opentrafficsim.core.gtu.GtuCharacteristics;
import org.opentrafficsim.core.gtu.GtuTemplate;
import org.opentrafficsim.core.gtu.GtuType;
import org.opentrafficsim.core.gtu.perception.DirectEgoPerception;
import org.opentrafficsim.core.gtu.perception.EgoPerception;
import org.opentrafficsim.core.gtu.plan.operational.OperationalPlanException;
import org.opentrafficsim.core.network.LateralDirectionality;
import org.opentrafficsim.core.network.Link;
import org.opentrafficsim.core.network.NetworkException;
import org.opentrafficsim.core.network.Node;
import org.opentrafficsim.core.parameters.ParameterFactory;
import org.opentrafficsim.core.parameters.ParameterFactoryByType;
import org.opentrafficsim.core.perception.HistoryManagerDevs;
import org.opentrafficsim.core.units.distributions.ContinuousDistSpeed;
import org.opentrafficsim.draw.gtu.DefaultCarAnimation.GtuData.GtuMarker;
import org.opentrafficsim.fosim.FosDetector;
import org.opentrafficsim.fosim.model.CarFollowingTask;
import org.opentrafficsim.fosim.model.LaneChangeTask;
import org.opentrafficsim.fosim.model.TaskManagerAr;
import org.opentrafficsim.fosim.parameters.ParameterDefinitions;
import org.opentrafficsim.fosim.parameters.data.ParameterDataDefinition;
import org.opentrafficsim.fosim.parameters.data.ParameterDataGroup;
import org.opentrafficsim.fosim.parameters.data.ValueAdapter;
import org.opentrafficsim.fosim.parameters.data.ValueData;
import org.opentrafficsim.fosim.sim0mq.FosimModel;
import org.opentrafficsim.fosim.simulator.OtsAnimatorStep;
import org.opentrafficsim.fosim.simulator.OtsSimulatorStep;
import org.opentrafficsim.road.definitions.DefaultsRoadNl;
import org.opentrafficsim.road.gtu.generator.GeneratorPositions.LaneBias;
import org.opentrafficsim.road.gtu.generator.GeneratorPositions.LaneBiases;
import org.opentrafficsim.road.gtu.generator.characteristics.LaneBasedGtuCharacteristics;
import org.opentrafficsim.road.gtu.generator.characteristics.LaneBasedGtuCharacteristicsGeneratorOd;
import org.opentrafficsim.road.gtu.lane.LaneBasedGtu;
import org.opentrafficsim.road.gtu.lane.VehicleModel;
import org.opentrafficsim.road.gtu.lane.perception.CategoricalLanePerception;
import org.opentrafficsim.road.gtu.lane.perception.LanePerception;
import org.opentrafficsim.road.gtu.lane.perception.PerceptionCollectable;
import org.opentrafficsim.road.gtu.lane.perception.PerceptionFactory;
import org.opentrafficsim.road.gtu.lane.perception.RelativeLane;
import org.opentrafficsim.road.gtu.lane.perception.categories.AnticipationTrafficPerception;
import org.opentrafficsim.road.gtu.lane.perception.categories.DirectInfrastructurePerception;
import org.opentrafficsim.road.gtu.lane.perception.categories.DirectIntersectionPerception;
import org.opentrafficsim.road.gtu.lane.perception.categories.InfrastructurePerception;
import org.opentrafficsim.road.gtu.lane.perception.categories.neighbors.Anticipation;
import org.opentrafficsim.road.gtu.lane.perception.categories.neighbors.DirectNeighborsPerception;
import org.opentrafficsim.road.gtu.lane.perception.categories.neighbors.Estimation;
import org.opentrafficsim.road.gtu.lane.perception.categories.neighbors.HeadwayGtuType;
import org.opentrafficsim.road.gtu.lane.perception.categories.neighbors.HeadwayGtuType.PerceivedHeadwayGtuType;
import org.opentrafficsim.road.gtu.lane.perception.categories.neighbors.NeighborsPerception;
import org.opentrafficsim.road.gtu.lane.perception.headway.HeadwayGtu;
import org.opentrafficsim.road.gtu.lane.perception.mental.AdaptationHeadway;
import org.opentrafficsim.road.gtu.lane.perception.mental.AdaptationSituationalAwareness;
import org.opentrafficsim.road.gtu.lane.perception.mental.AdaptationSpeed;
import org.opentrafficsim.road.gtu.lane.perception.mental.Fuller;
import org.opentrafficsim.road.gtu.lane.perception.mental.Fuller.BehavioralAdaptation;
import org.opentrafficsim.road.gtu.lane.perception.mental.Task;
import org.opentrafficsim.road.gtu.lane.perception.mental.TaskManager;
import org.opentrafficsim.road.gtu.lane.plan.operational.LaneChange;
import org.opentrafficsim.road.gtu.lane.tactical.following.AbstractIdm;
import org.opentrafficsim.road.gtu.lane.tactical.following.CarFollowingModel;
import org.opentrafficsim.road.gtu.lane.tactical.following.CarFollowingModelFactory;
import org.opentrafficsim.road.gtu.lane.tactical.following.IdmPlus;
import org.opentrafficsim.road.gtu.lane.tactical.following.IdmPlusFactory;
import org.opentrafficsim.road.gtu.lane.tactical.lmrs.AccelerationIncentive;
import org.opentrafficsim.road.gtu.lane.tactical.lmrs.AccelerationTrafficLights;
import org.opentrafficsim.road.gtu.lane.tactical.lmrs.DefaultLmrsPerceptionFactory;
import org.opentrafficsim.road.gtu.lane.tactical.lmrs.IncentiveCourtesy;
import org.opentrafficsim.road.gtu.lane.tactical.lmrs.IncentiveKeep;
import org.opentrafficsim.road.gtu.lane.tactical.lmrs.IncentiveSocioSpeed;
import org.opentrafficsim.road.gtu.lane.tactical.lmrs.IncentiveSpeedWithCourtesy;
import org.opentrafficsim.road.gtu.lane.tactical.lmrs.LmrsFactory;
import org.opentrafficsim.road.gtu.lane.tactical.lmrs.SocioDesiredSpeed;
import org.opentrafficsim.road.gtu.lane.tactical.util.CarFollowingUtil;
import org.opentrafficsim.road.gtu.lane.tactical.util.lmrs.Cooperation;
import org.opentrafficsim.road.gtu.lane.tactical.util.lmrs.Desire;
import org.opentrafficsim.road.gtu.lane.tactical.util.lmrs.GapAcceptance;
import org.opentrafficsim.road.gtu.lane.tactical.util.lmrs.LmrsData;
import org.opentrafficsim.road.gtu.lane.tactical.util.lmrs.LmrsParameters;
import org.opentrafficsim.road.gtu.lane.tactical.util.lmrs.LmrsUtil;
import org.opentrafficsim.road.gtu.lane.tactical.util.lmrs.MandatoryIncentive;
import org.opentrafficsim.road.gtu.lane.tactical.util.lmrs.Synchronization;
import org.opentrafficsim.road.gtu.lane.tactical.util.lmrs.Tailgating;
import org.opentrafficsim.road.gtu.lane.tactical.util.lmrs.VoluntaryIncentive;
import org.opentrafficsim.road.gtu.strategical.LaneBasedStrategicalRoutePlannerFactory;
import org.opentrafficsim.road.network.LaneChangeInfo;
import org.opentrafficsim.road.network.RoadNetwork;
import org.opentrafficsim.road.network.lane.CrossSectionElement;
import org.opentrafficsim.road.network.lane.CrossSectionGeometry;
import org.opentrafficsim.road.network.lane.CrossSectionLink;
import org.opentrafficsim.road.network.lane.Lane;
import org.opentrafficsim.road.network.lane.LaneGeometryUtil;
import org.opentrafficsim.road.network.lane.LanePosition;
import org.opentrafficsim.road.network.lane.LaneType;
import org.opentrafficsim.road.network.lane.Shoulder;
import org.opentrafficsim.road.network.lane.Stripe;
import org.opentrafficsim.road.network.lane.StripeData;
import org.opentrafficsim.road.network.lane.changing.LaneKeepingPolicy;
import org.opentrafficsim.road.network.lane.object.detector.LoopDetector;
import org.opentrafficsim.road.network.lane.object.trafficlight.TrafficLight;
import org.opentrafficsim.road.network.speed.SpeedLimitInfo;
import org.opentrafficsim.road.od.Categorization;
import org.opentrafficsim.road.od.Category;
import org.opentrafficsim.road.od.Interpolation;
import org.opentrafficsim.road.od.OdApplier;
import org.opentrafficsim.road.od.OdMatrix;
import org.opentrafficsim.road.od.OdOptions;
import org.opentrafficsim.swing.gui.OtsAnimationPanel;
import org.opentrafficsim.swing.gui.OtsSimulationApplication;
import org.opentrafficsim.swing.gui.OtsSwingApplication;
import org.opentrafficsim.trafficcontrol.FixedTimeController;
import org.opentrafficsim.trafficcontrol.FixedTimeController.SignalGroup;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import nl.tudelft.simulation.dsol.SimRuntimeException;
import nl.tudelft.simulation.jstats.distributions.DistContinuous;
import nl.tudelft.simulation.jstats.distributions.DistNormal;
import nl.tudelft.simulation.jstats.streams.StreamInterface;
import nl.tudelft.simulation.language.DsolException;

/**
 * Parser of .fos files from FOSIM.
 * <p>
 * Copyright (c) 2023-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
 */
public class FosParser
{

    /** Offset for stripes that are at the left or right edge. This separates them between links. */
    private static final Length EDGE_STRIPE_GAP = Length.instantiateSI(0.2);

    /** Length above which vehicles types are considered a truck (deprecated versions). */
    private static final Length TRUCK_THRESHOLD = Length.instantiateSI(7.0);

    /** Shoulder lane type without any compatibility. */
    private static final LaneType SHOULDER = new LaneType("Shoulder");

    /** Simulator. */
    private OtsSimulatorInterface simulator = null;

    /** Network. */
    private RoadNetwork network;

    /** Model. */
    private FosimModel model;

    /** Application. */
    private OtsSimulationApplication<FosimModel> app;

    /** Parser settings. */
    private Map<ParserSetting, Boolean> parserSettings = new LinkedHashMap<>();

    /** Parameter supplier. */
    private ParameterSupplier parameterSupplier = new ParameterSupplier();

    // parsed info

    /** Version. */
    @SuppressWarnings("unused")
    private String version;

    /** Section lengths. */
    private List<Length> sections = new ArrayList<>();

    /** Lanes, per lane a list of objects for each section. */
    private FosList<List<FosLane>> lane = new FosList<>();

    /** Sources. */
    private FosList<FosSourceSink> source = new FosList<>();

    /** Sinks. */
    private FosList<FosSourceSink> sink = new FosList<>();

    /** Traffic lights. */
    private FosList<FosTrafficLight> trafficLight = new FosList<>();

    /** Detector times [step#] containing two values: first detector output time and interval after that. */
    private List<Integer> detectorTimes = new ArrayList<>();

    /** Detector positions. */
    private List<Length> detectorPositions = new ArrayList<>();

    /** Vehicle types. */
    private List<String> vehicleTypeNames = new ArrayList<>();

    /** Whether vehicle types are truck. */
    private List<Boolean> isTruck = new ArrayList<>();

    /** General parameters. */
    private FosList<FosParameter> generalParameter = new FosList<>();

    /** Specific parameters per vehicle type. */
    private FosList<FosList<FosParameter>> specificParameter = new FosList<>();

    /** Parameter definitions. */
    private ParameterDataDefinition otsParameters;

    /** List of flows, index same as source index. */
    private FosList<FosFlow> flow = new FosList<>();

    /** Vehicle probabilities. */
    private FosList<List<Double>> vehicleProbabilities = new FosList<>();

    /** Source to sink. */
    private FosList<FosList<List<Double>>> sourceToSink = new FosList<>();

    /** Switched areas. */
    private FosList<FosSwitchedArea> switchedAreaTimes = new FosList<>();

    /** Temporary blockage. */
    @SuppressWarnings("unused")
    private FosTemporaryBlockage temporaryBlockage;

    /** Seed. */
    private int seed;

    /** Time step. */
    private Duration timeStep;

    /** Maximum simulation time [#steps]. */
    private int maximumSimulationTime;

    /** End of file reached. */
    private boolean endOfFile;

    // temporary information while parsing

    /** Maximum width of each lane. */
    private Length[] maxLaneWidth;

    /** Number of last mapped link. */
    private int lastMappedLink = 1;

    /** Link numbers, stored for reference when building and finding nodes. */
    private int[][] mappedLinks;

    /** Link object per link number. */
    private Map<Integer, FosLink> links = new LinkedHashMap<>();

    /** Number of last mapped node. */
    private int lastMappedNode = 1;

    /** Nodes. */
    private Set<FosNode> nodes = new LinkedHashSet<>();

    /** Forbidden node names, i.e. reserved for sources and sinks. */
    private LinkedHashSet<String> forbiddenNodeNames;

    /** GTU types. */
    private List<GtuType> gtuTypes = new ArrayList<>();

    /** OD node name mappings (OTS names are the keys, Fosim numbers the fields). */
    private Map<String, Integer> odNumbers = new LinkedHashMap<>();

    /**
     * Sets the settings.
     * @param parserSettings parse settings. Missing settings are assumed default.
     * @return this parser for method chaining.
     */
    public FosParser setSettings(final Map<ParserSetting, Boolean> parserSettings)
    {
        this.parserSettings = parserSettings;
        return this;
    }

    /**
     * Sets the parameter supplier.
     * @param parameterSupplier parameter supplier.
     * @return this parser for method chaining.
     */
    public FosParser setParameterSupplier(final ParameterSupplier parameterSupplier)
    {
        this.parameterSupplier = parameterSupplier;
        return this;
    }

    /**
     * Sets the simulator. If none is set, one will be created.
     * @param simulator simulator.
     * @return this parser for method chaining.
     */
    public FosParser setSimulator(final OtsSimulatorInterface simulator)
    {
        this.simulator = simulator;
        return this;
    }

    /**
     * Parses a .fos file.
     * @param file location of a .pos file.
     * @throws InvalidPathException if the path is invalid.
     * @throws IOException if the file could not be read.
     * @throws NetworkException if anything fails critically during parsing.
     */
    public void parseFromFile(final String file) throws InvalidPathException, IOException, NetworkException
    {
        parseFromString(Files.readString(Path.of(file)));
    }

    /**
     * Parses a string of the contents typically in a .fos file.
     * @param stream stream of the contents typically in a .fos file.
     * @throws NetworkException if anything fails critically during parsing.
     * @throws IOException if the stream cannot be read
     */
    public void parseFromStream(final InputStream stream) throws NetworkException, IOException
    {
        parseFromString(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
    }

    /**
     * Parses a string of the contents typically in a .fos file.
     * @param fosString string of the contents typically in a .fos file.
     * @throws NetworkException if anything fails critically during parsing.
     */
    public void parseFromString(final String fosString) throws NetworkException
    {
        new BufferedReader(new StringReader(fosString)).lines().forEach(this::parseLine);
        build();
    }

    // TODO: parse to Xml, similar to XsdTreeNode.saveXmlNodes() in the editor

    /**
     * Returns the network after parsing.
     * @return network.
     * @throws IllegalStateException when no parsing was yet performed.
     */
    public RoadNetwork getNetwork()
    {
        Throw.when(this.network == null, IllegalStateException.class, "No fos information was parsed.");
        return this.network;
    }

    /**
     * Returns the model after parsing.
     * @return model.
     * @throws IllegalStateException when no parsing was yet performed.
     */
    public FosimModel getModel()
    {
        Throw.when(this.model == null, IllegalStateException.class, "No fos information was parsed.");
        return this.model;
    }

    /**
     * Returns the application after parsing.
     * @return application.
     * @throws IllegalStateException when no parsing was yet performed.
     */
    public OtsSimulationApplication<FosimModel> getApplication()
    {
        Throw.when(this.app == null, IllegalStateException.class, "No fos information was parsed.");
        return this.app;
    }

    /**
     * Returns the duration of the first detector period.
     * @return duration of first detector period.
     */
    public Duration getFirstPeriod()
    {
        return this.timeStep.times(this.detectorTimes.get(0));
    }

    /**
     * Returns the duration of detector periods after the first.
     * @return duration of detector periods after the first.
     */
    public Duration getNextPeriods()
    {
        return this.timeStep.times(this.detectorTimes.get(1));
    }

    /**
     * Returns the links.
     * @return links
     */
    public ImmutableSet<FosLink> getLinks()
    {
        return new ImmutableHashSet<>(this.links.values());
    }

    /**
     * Parses a line. This method should treat all missing parser settings as true, by using
     * {@code Map.getOrDefault(ParserSettings, true)}.
     * @param line line to parse.
     */
    private void parseLine(final String line)
    {
        Throw.when(this.endOfFile, IllegalStateException.class, "Lines beyond end of file marker.");
        if (line.startsWith("#"))
        {
            return; // comment line
        }
        if (line.startsWith("version"))
        {
            this.version = fieldValue(line);
            return;
        }
        if (line.startsWith("sections"))
        {
            parseValueList(this.sections, fieldValue(line), v -> Length.instantiateSI(Double.parseDouble(v)));
            return;
        }
        // beware of overlap between "lane" and "lane change" as line starts
        if (line.startsWith("lane") && !line.startsWith("lane change"))
        {
            List<FosLane> list = new ArrayList<>();
            int from = line.indexOf(":") + 1;
            String lane = nextLaneString(line, from);
            while (!lane.isEmpty())
            {
                // FosLane does the actual interpreting of the string
                list.add(new FosLane(lane.trim()));
                from += lane.length();
                lane = nextLaneString(line, from);
            }
            this.lane.set(fieldIndex(line), list);
            return;
        }
        // beware of overlap between "source" and "source to sink" as line starts
        if (line.startsWith("source") && !line.startsWith("source to sink"))
        {
            this.source.set(fieldIndex(line), new FosSourceSink(fieldValue(line)));
            return;
        }
        if (line.startsWith("sink"))
        {
            this.sink.set(fieldIndex(line), new FosSourceSink(fieldValue(line)));
            return;
        }
        if (line.startsWith("traffic light"))
        {
            if (getSetting(ParserSetting.TRAFFIC_LIGHTS))
            {
                this.trafficLight.set(fieldIndex(line), new FosTrafficLight(fieldValue(line)));
            }
            return;
        }
        if (line.startsWith("detector times"))
        {
            String value = fieldValue(line);
            if (value.contains("s"))
            {
                parseValueList(this.detectorTimes, value, v -> (int) (Duration.valueOf(v).si / 0.5));
            }
            else
            {
                parseValueList(this.detectorTimes, value, v -> (int) Double.parseDouble(v));
            }
            return;
        }
        if (line.startsWith("detector positions"))
        {
            if (getSetting(ParserSetting.DETECTORS))
            {
                parseValueList(this.detectorPositions, fieldValue(line), v -> Length.instantiateSI(Double.parseDouble(v)));
            }
            return;
        }
        if (line.startsWith("vehicle types"))
        {
            if (line.startsWith("vehicle types:"))
            {
                // old definition: just a number
                int numberOfVehicleTypes = Integer.parseInt(fieldValue(line));
                for (int i = 0; i < numberOfVehicleTypes; i++)
                {
                    this.vehicleTypeNames.add((i + 1) + "");
                }
            }
            else
            {
                int index = fieldIndex(line);
                List<String> values = new ArrayList<>();
                parseValueList(values, fieldValue(line), v -> v);
                while (this.vehicleTypeNames.size() <= index)
                {
                    this.isTruck.add(false);
                    this.vehicleTypeNames.add("");
                }
                this.isTruck.set(index, "1".equals(values.get(0)));
                this.vehicleTypeNames.set(index, values.get(1));
            }
            return;
        }
        if (line.startsWith("vehicle general param"))
        {
            this.generalParameter.set(fieldIndex(line), new FosParameter(fieldValue(line)));
            return;
        }
        if (line.startsWith("vehicle specific param"))
        {
            int[] indices = fieldIndices(line);
            getSubList(this.specificParameter, indices[1]).set(indices[0], new FosParameter(fieldValue(line)));
            return;
        }
        if (line.startsWith("ots param"))
        {
            GsonBuilder builder = new GsonBuilder();
            builder.registerTypeAdapter(ValueData.class, new ValueAdapter());
            Gson gson = builder.create();
            JsonReader reader = new JsonReader(new StringReader(fieldValue(line)));
            this.otsParameters = (ParameterDataDefinition) gson.fromJson(reader, ParameterDataDefinition.class);
            return;
        }
        if (line.startsWith("flow"))
        {
            if (getSetting(ParserSetting.DEMAND))
            {
                this.flow.set(fieldIndex(line), new FosFlow(fieldValue(line)));
            }
            return;
        }
        if (line.startsWith("vehicle probabilities"))
        {
            this.vehicleProbabilities.set(fieldIndex(line),
                    parseValueList(new ArrayList<>(), fieldValue(line), v -> Double.parseDouble(v)));
            return;
        }
        if (line.startsWith("source to sink"))
        {
            int[] indices = fieldIndices(line);
            getSubList(this.sourceToSink, indices[0]).set(indices[1],
                    parseValueList(new ArrayList<>(), fieldValue(line), v -> Double.parseDouble(v)));
            return;
        }
        if (line.startsWith("lane change"))
        {
            // this information is ignored
            return;
        }
        if (line.startsWith("switched area times"))
        {
            this.switchedAreaTimes.set(fieldIndex(line), new FosSwitchedArea(fieldValue(line)));
            return;
        }
        if (line.startsWith("temporary blockage"))
        {
            if (getSetting(ParserSetting.TEMPORARY_BLOCKAGE))
            {
                this.temporaryBlockage = new FosTemporaryBlockage(fieldValue(line));
            }
            return;
        }
        if (line.startsWith("random seed"))
        {
            this.seed = Integer.parseInt(fieldValue(line));
            return;
        }
        if (line.startsWith("time step size"))
        {
            this.timeStep = Duration.instantiateSI(Double.parseDouble(fieldValue(line)));
            setFinalDemand();
            return;
        }
        if (line.startsWith("maximum simulation time"))
        {
            String value = fieldValue(line);
            int blank = value.indexOf(" ");
            if (blank > 0)
            {
                // second value is time of day of start simulation, this can be ignored by OTS
                value = value.substring(0, blank);
            }
            if (value.contains("s"))
            {
                this.maximumSimulationTime = (int) (Duration.valueOf(value).si / 0.5);
                this.timeStep = Duration.instantiateSI(0.5);
            }
            else
            {
                this.maximumSimulationTime = Integer.parseInt(value);
            }
            setFinalDemand();
            return;
        }
        if (line.startsWith("end of file"))
        {
            this.endOfFile = true;
            return;
        }
        // TODO: this anticipates a future change in Fos files, might be called differently
        if (line.startsWith("start of simulation time"))
        {
            // this can be ignored
            return;
        }
        // all supported line variants did not take the line
        throw new RuntimeException("Unable to parse line: " + line);
    }

    /**
     * Sets the final demand in each flow object, if the final simulation time is known.
     */
    private void setFinalDemand()
    {
        if (this.timeStep != null && this.maximumSimulationTime > 0)
        {
            Time endTime = Time.instantiateSI(this.timeStep.times(this.maximumSimulationTime).si);
            this.flow.forEach((flow) -> flow.setEndTime(endTime));
        }
    }

    /**
     * Returns a sub-list from a list. The list is created if required.
     * @param list list to take the sub-list from, or to put a create one in.
     * @param index index of the list to obtain.
     * @return list to take a list from.
     * @param <T> type in the returned list.
     */
    private synchronized <T> FosList<T> getSubList(final FosList<FosList<T>> list, final int index)
    {
        if (index >= list.size() || list.get(index) == null)
        {
            FosList<T> subList = new FosList<>();
            list.set(index, subList);
            return subList;
        }
        return list.get(index);
    }

    /**
     * Returns the value for the setting. It will be the default value of the setting if the setting was not set.
     * @param setting parser setting.
     * @return value for the setting.
     */
    private boolean getSetting(final ParserSetting setting)
    {
        return this.parserSettings.getOrDefault(setting, setting.getDefaultValue());
    }

    /**
     * Builds the network and anything on it as indicated by the settings.
     * @throws NetworkException if anything fails critically during parsing.
     */
    private void build() throws NetworkException
    {
        // test
        validityTest();

        try
        {
            // dealing with older versions
            if (this.isTruck.isEmpty())
            {
                for (int vehicleTypeNumber = 0; vehicleTypeNumber < this.vehicleTypeNames.size(); vehicleTypeNumber++)
                {
                    Length length = Length.instantiateSI(getParameterValue(vehicleTypeNumber, "length"));
                    this.isTruck.add(length.gt(TRUCK_THRESHOLD));
                }
            }

            // simulator and network
            boolean gui;
            if (this.simulator == null)
            {
                gui = getSetting(ParserSetting.GUI);
                this.simulator = gui ? new OtsAnimatorStep("Ots-Fosim") : new OtsSimulatorStep("Ots-Fosim");
            }
            else
            {
                gui = false;
            }
            this.network = new RoadNetwork("Ots-Fosim", this.simulator);
            this.model = new FosimModel(this.simulator, this.seed);
            this.model.setNetwork(this.network);

            this.simulator.initialize(Time.ZERO, Duration.ZERO, this.timeStep.times(this.maximumSimulationTime), this.model,
                    new HistoryManagerDevs(this.simulator, Duration.instantiateSI(5.0), Duration.instantiateSI(10.0)));
            // TODO replace this to constructor of replication (OTS issue #184)
            // this.simulator.getReplication().setHistoryManager(
            // new HistoryManagerDevs(this.simulator, Duration.instantiateSI(5.0), Duration.instantiateSI(10.0)));

            // map out network
            for (int sectionIndex = 0; sectionIndex < this.sections.size(); sectionIndex++)
            {
                int fromLane = 0;
                while (fromLane < this.lane.size())
                {
                    fromLane = mapOutLink(sectionIndex, fromLane);
                }
            }
            mapOutNodes();

            // build network
            for (FosNode node : this.nodes)
            {
                buildNode(node);
            }
            for (FosLink link : this.links.values())
            {
                buildLink(link);
            }

            // GTU types
            buildGtuTypes();

            // generators
            buildGenerators();

            // detectors
            buildDetecors();

            // traffic lights
            buildTrafficLights();

            // printMappings(); // TODO: remove test code

            if (gui)
            {
                List<GtuColorer> colorers = OtsSwingApplication.DEFAULT_GTU_COLORERS;
                Map<GtuType, GtuMarker> markerMap = Map.of(DefaultsNl.TRUCK, GtuMarker.SQUARE);
                OtsAnimationPanel animationPanel = new OtsAnimationPanel(this.network.getExtent(), new Dimension(100, 100),
                        (OtsAnimator) this.network.getSimulator(), this.model, colorers, this.network);
                animationPanel.enableSimulationControlButtons();
                this.app = new OtsSimulationApplication<FosimModel>(this.model, animationPanel, markerMap);
            }

        }
        catch (SimRuntimeException | NamingException | RemoteException | DsolException | ParameterException e)
        {
            throw new RuntimeException(e);
        }

    }

    /**
     * Test whether the parsed information is valid.
     * @throws NetworkException if anything fails critically during parsing.
     */
    private void validityTest() throws NetworkException
    {
        // end of file
        Throw.when(!this.endOfFile, NetworkException.class,
                "No end of file information was found, parsing information likely incomplete.");

        // specific parameters
        for (int vehicleType = 0; vehicleType < this.vehicleTypeNames.size(); vehicleType++)
        {
            Throw.when(vehicleType >= this.specificParameter.size() || this.specificParameter.get(vehicleType) == null,
                    NetworkException.class, "No parameters for vehicle type " + vehicleType);
        }

        // vehicle probabilities
        for (int sourceIndex = 0; sourceIndex < this.vehicleProbabilities.size(); sourceIndex++)
        {
            Throw.when(sourceIndex >= this.source.size(), NetworkException.class,
                    "Source " + sourceIndex + " as specified for vehicle probabilities does not exist.");
            Throw.when(this.vehicleProbabilities.get(sourceIndex).size() != this.vehicleTypeNames.size(),
                    NetworkException.class, "Wrong number of vehicle probabilities for source " + sourceIndex + ".");
        }

        // sink to source
        for (int sourceIndex = 0; sourceIndex < this.sourceToSink.size(); sourceIndex++)
        {
            Throw.when(sourceIndex >= this.source.size(), NetworkException.class,
                    "Source " + sourceIndex + " as specified for sink to source does not exist.");
            for (int sinkIndex = 0; sinkIndex < this.sourceToSink.get(sourceIndex).size(); sinkIndex++)
            {
                Throw.when(sinkIndex >= this.sink.size(), NetworkException.class,
                        "Sink " + sinkIndex + " as specified for sink to source does not exist.");
                Throw.when(this.sourceToSink.get(sourceIndex).get(sinkIndex).size() != this.vehicleTypeNames.size(),
                        NetworkException.class, "Wrong number of vehicle probabilities for source " + sourceIndex + ".");
            }
        }

        // all FosLists contain no null (except SwitchedAreaTimes at index 0)?
        Throw.when(!this.lane.isDefined(), NetworkException.class, "Not all lanes are defined.");
        Throw.when(!this.source.isDefined(), NetworkException.class, "Not all sources are defined.");
        Throw.when(!this.sink.isDefined(), NetworkException.class, "Not all sinks are defined.");
        Throw.when(!this.trafficLight.isDefined(), NetworkException.class, "Not all traffic lights are defined.");
        Throw.when(!this.generalParameter.isDefined(), NetworkException.class, "Not all general parameters are defined.");
        Throw.when(!this.specificParameter.isDefined(), NetworkException.class, "Not all specific parameters are defined.");
        for (FosList<FosParameter> parameters : this.specificParameter)
        {
            Throw.when(!parameters.isDefined(), NetworkException.class, "Not all specific parameters are defined.");
        }
        Throw.when(!this.flow.isDefined(), NetworkException.class, "Not all flow is defined.");
        Throw.when(!this.vehicleProbabilities.isDefined(), NetworkException.class,
                "Not all vehicle probabilities are defined.");
        Throw.when(!this.sourceToSink.isDefined(), NetworkException.class, "Not all source to sinks are defined.");
        for (FosList<List<Double>> parameters : this.sourceToSink)
        {
            Throw.when(!parameters.isDefined(), NetworkException.class, "Not all source to sinks are defined.");
        }
        // switch areas are never defined at index 0 (as that would make it impossible to distinct positive and negative values,
        // pertaining to a distinction between rush-hour and plus lanes, in the lines that define lanes)
        for (int i = 1; i < this.switchedAreaTimes.size(); i++)
        {
            Throw.when(this.switchedAreaTimes.get(i) == null, NetworkException.class, "Not all switch area times are defined.");
        }
    }

    /**
     * Maps out a single link in the {@code mappedLinks} grid. This moves from a first, left-most, lane to the right. This
     * happens for as long as lane changes are possible to the next lane, or from the next lane to the current lane. No link is
     * mapped if the specified section and lane are not a valid traffic lane. In either case the returned index is where the
     * next link may be found, and should be used for the next call to this method (if it is in the overall lane bounds).
     * @param sectionIndex section index.
     * @param fromLane from lane index (i.e. the left-most lane of the potential link).
     * @return lane index at which the next left-most lane of the next link on the same section may be found.
     */
    private int mapOutLink(final int sectionIndex, final int fromLane)
    {
        // lane is grass, striped area to ignore, or beyond striped area; skip
        if (this.lane.get(fromLane).get(sectionIndex).type.equals("u"))
        {
            return fromLane + 1;
        }

        // initialize mapping when needed
        if (this.mappedLinks == null)
        {
            this.mappedLinks = new int[this.lane.size()][this.sections.size()];
        }

        // find until what lane we are on the same link
        int toLane = fromLane;
        this.mappedLinks[toLane][sectionIndex] = this.lastMappedLink;
        boolean stripedAreas = getSetting(ParserSetting.STRIPED_AREAS);
        while (toLane < this.lane.size() - 1 && (this.lane.get(toLane).get(sectionIndex).canChangeRight(stripedAreas)
                || (this.lane.get(toLane).get(sectionIndex).isShoulder(stripedAreas) && toLane == fromLane)
                || this.lane.get(toLane + 1).get(sectionIndex).canChangeLeft(stripedAreas)
                || this.lane.get(toLane + 1).get(sectionIndex).isShoulder(stripedAreas)))
        {
            toLane++;
            this.mappedLinks[toLane][sectionIndex] = this.lastMappedLink;
        }

        // remember information
        this.links.put(this.lastMappedLink, new FosLink(this.lastMappedLink++, sectionIndex, fromLane, toLane, this));

        // return possible next from-lane
        return toLane + 1;
    }

    /**
     * Returns a lane.
     * @param sectionIndex section index.
     * @param laneIndex lane index.
     * @return lane at given section and lane index.
     */
    FosLane getLane(final int sectionIndex, final int laneIndex)
    {
        return this.lane.get(laneIndex).get(sectionIndex);
    }

    /**
     * Maps out all nodes, including sources, sinks and intermediate nodes. It gives sources their intended name. Sinks may
     * receive a default name based on their number to prevent a name clash with sources, or with other sinks that may happen to
     * have an intended name similar to a default name.
     */
    private void mapOutNodes()
    {
        // intermediate nodes
        for (int sectionIndex = 0; sectionIndex < this.sections.size() - 1; sectionIndex++)
        {
            for (int laneIndex = 0; laneIndex < this.lane.size(); laneIndex++)
            {
                int fromLink = this.mappedLinks[laneIndex][sectionIndex];
                int toLink = this.mappedLinks[this.lane.get(laneIndex).get(sectionIndex).laneOut][sectionIndex + 1];
                if (fromLink > 0 && toLink > 0)
                {
                    FosNode newNode = getOrCreateNode(fromLink, toLink);
                    newNode.inLinks.add(this.links.get(fromLink));
                    this.links.get(fromLink).toNode = newNode;
                    newNode.outLinks.add(this.links.get(toLink));
                    this.links.get(toLink).fromNode = newNode;
                }
            }
        }

        // sources
        Set<String> sourceNames = new LinkedHashSet<>();
        for (FosSourceSink source : this.source)
        {
            FosLink link = getSourceSinkLink(source);
            FosNode sourceNode = new FosNode(this.lastMappedNode++);
            sourceNode.source = source;
            sourceNode.name = source.name;
            this.odNumbers.put(sourceNode.name, this.source.indexOf(source));
            sourceNode.outLinks.add(link);
            link.fromNode = sourceNode;
            this.nodes.add(sourceNode);
            sourceNames.add(sourceNode.getName());
        }

        // sinks
        Set<String> sinkNames = new LinkedHashSet<>();
        for (FosSourceSink sink : this.sink)
        {
            FosLink link = getSourceSinkLink(sink);
            FosNode sinkNode = new FosNode(this.lastMappedNode++);
            sinkNode.sink = sink;
            String name = sink.name;
            // sources and sinks can have the same name, in that case create a new node
            if (sourceNames.contains(name) || sinkNames.contains(name))
            {
                name = sinkNode.getName() + " (" + sink.name + ")"; // number to "AY" and sink name
            }
            while (sourceNames.contains(name) || sinkNames.contains(name))
            {
                // even as "AY (Amsterdam)" it's duplicate, increase number until the name is unique
                sinkNode = new FosNode(this.lastMappedNode++);
                name = sinkNode.getName() + " (" + sink.name + ")"; // number to "AZ" and sink name
            }
            sinkNode.name = name;
            this.odNumbers.put(sinkNode.name, this.sink.indexOf(sink));
            sinkNode.inLinks.add(link);
            link.toNode = sinkNode;
            this.nodes.add(sinkNode);
            sinkNames.add(sinkNode.getName());
        }
    }

    /**
     * Returns the link connected to the source or sink.
     * @param sourceSink source or sink.
     * @return link connected to the source or sink.
     */
    private FosLink getSourceSinkLink(final FosSourceSink sourceSink)
    {
        int sectionIndexFromStart = this.sections.size() - sourceSink.sectionFromEnd - 1;
        FosLink link = this.links.get(this.mappedLinks[sourceSink.fromLane][sectionIndexFromStart]);
        if (link == null)
        {
            // attaching link to a sink must be a diagonal lane, find it
            for (int i = 0; i < this.lane.size(); i++)
            {
                FosLane lane = this.lane.get(i).get(sectionIndexFromStart);
                if (!"u".equals(lane.type) && lane.laneOut >= sourceSink.fromLane && lane.laneOut <= sourceSink.toLane)
                {
                    link = this.links.get(this.mappedLinks[i][sectionIndexFromStart]);
                    break;
                }
            }
        }
        return link;
    }

    /**
     * Tries to find a node as it either flows towards {@code toLink} or from {@code fromLink}. In such cases links must share a
     * node due to a merge or diverge. If no such node exists, a new node is created.
     * @param fromLink from link, flowing in to the node.
     * @param toLink to link, flowing out of the node.
     * @return node to connect both links.
     */
    private FosNode getOrCreateNode(final int fromLink, final int toLink)
    {
        for (FosNode node : this.nodes)
        {
            if (node.outLinks.contains(this.links.get(toLink)) || node.inLinks.contains(this.links.get(fromLink)))
            {
                return node;
            }
        }
        FosNode newNode = new FosNode(this.lastMappedNode++);
        // check we are not using a forbidden name (same as source or sink)
        while (hasForbiddenName(newNode))
        {
            newNode = new FosNode(this.lastMappedNode++);
        }
        this.nodes.add(newNode);
        return newNode;
    }

    /**
     * Returns whether this node has a forbidden name, i.e. equal to desired name of a source or sink. In that case, a new node
     * should be generated in that case.
     * @param node node.
     * @return whether this node has a forbidden name.
     */
    private boolean hasForbiddenName(final FosNode node)
    {
        if (this.forbiddenNodeNames == null)
        {
            this.forbiddenNodeNames = new LinkedHashSet<>();
            this.source.forEach((s) -> this.forbiddenNodeNames.add(s.name));
            this.sink.forEach((s) -> this.forbiddenNodeNames.add(s.name));
        }
        return this.forbiddenNodeNames.contains(node.getName());
    }

    /**
     * Prints debugging information.
     */
    @SuppressWarnings("unused") // can be used for testing
    private void printMappings()
    {
        System.out.println("To-nodes:");
        for (int i = 0; i < this.lane.size(); i++)
        {
            System.out.print("    ");
            for (int j = 0; j < this.sections.size(); j++)
            {
                int link = this.mappedLinks[i][j];
                String node = link > 0 && this.links.get(link).toNode != null ? this.links.get(link).toNode.getName() : "";
                System.out.print(String.format("%2d %3.3s ", link, node).replace(" 0", "--"));
            }
            System.out.print("\r");
        }
        System.out.print("\r");
        System.out.println("From-nodes:");
        for (int i = 0; i < this.lane.size(); i++)
        {
            for (int j = 0; j < this.sections.size(); j++)
            {
                int link = this.mappedLinks[i][j];
                String node = link > 0 && this.links.get(link).fromNode != null ? this.links.get(link).fromNode.getName() : "";
                System.out.print(String.format("%3.3s %2d ", node, link).replace(" 0", "--"));
            }
            System.out.print("\r");
        }
    }

    /**
     * Builds the nodes.
     * @param node node.
     * @throws NetworkException; on exception when creating a node
     */
    private void buildNode(final FosNode node) throws NetworkException
    {
        int deltaSection;
        Set<FosLink> links;
        boolean inLinks;
        if (node.outLinks.isEmpty())
        {
            deltaSection = 0;
            links = node.inLinks;
            inLinks = true;
        }
        else
        {
            deltaSection = -1;
            links = node.outLinks;
            inLinks = false;
        }

        int sectionIndex = links.iterator().next().sectionIndex + deltaSection;
        Length x = sectionIndex == -1 ? Length.ZERO : this.sections.get(sectionIndex);
        Length y = Length.POSITIVE_INFINITY;
        for (FosLink link : links)
        {
            int from;
            int to;
            if (inLinks)
            {
                from = Integer.MAX_VALUE;
                to = Integer.MIN_VALUE;
                for (FosLane lane : link.lanes)
                {
                    from = from < lane.laneOut ? from : lane.laneOut;
                    to = to > lane.laneOut ? to : lane.laneOut;
                }
            }
            else
            {
                from = link.fromLane;
                to = link.toLane;
            }
            y = Length.min(y, getLeftLinkEdge(link.sectionIndex, from, to));
        }
        Node n = new Node(this.network, node.getName(), new Point2d(x.si, y.si), Direction.ZERO);
        // store node in sink and/or source object
        if (node.source != null)
        {
            node.source.node = n;
        }
        if (node.sink != null)
        {
            node.sink.node = n;
        }
    }

    /**
     * Builds a link, including the lanes on it.
     * @param link parsed link information.
     * @throws NetworkException on exceptions creating the link or lane objects.
     * @throws OtsGeometryException when lane or stripe geometry is not correct
     */
    private void buildLink(final FosLink link) throws NetworkException, OtsGeometryException
    {
        // create the link
        String name = String.format("Link %d", link.number);
        Node startNode = (Node) this.network.getNode(link.fromNode.getName());
        Node endNode = (Node) this.network.getNode(link.toNode.getName());
        Point2d startPoint = startNode.getPoint();
        Point2d endPoint = endNode.getPoint();
        OtsLine2d designLine = new OtsLine2d(startPoint, new Point2d(endPoint.x, startPoint.y));// ,
        // NetworkException.class, "Design line could not be generated for link at lane %s, section %s.", link.fromLane,
        // link.sectionIndex);
        CrossSectionLink otsLink = new CrossSectionLink(this.network, name, startNode, endNode, DefaultsNl.FREEWAY, designLine,
                FractionalLengthData.of(0.0, 0.0), LaneKeepingPolicy.KEEPRIGHT);

        // calculate offsets
        List<Length> lateralOffsetAtStarts = new ArrayList<>();
        List<Length> lateralOffsetAtEnds = new ArrayList<>();
        // initialize relative to nodes
        Length leftLinkEdge = getLeftLinkEdge(link.sectionIndex, link.fromLane, link.toLane);
        Length leftEdgeOffsetStart = leftLinkEdge.minus(Length.instantiateSI(startNode.getPoint().y));
        Length leftEdgeOffsetEnd = leftLinkEdge.minus(Length.instantiateSI(startNode.getPoint().y));
        double offsetEnd = 0; // to detect change in the number of lanes a lane shifts, relative to left-hand lanes
        for (int i = 0; i < link.lanes.size(); i++)
        {
            if (link.lanes.get(i).taper.equals("<"))
            {
                // at taper, do not shift left edge
                lateralOffsetAtStarts.add(leftEdgeOffsetStart);
            }
            else
            {
                // otherwise, add halve the lane width to the left edge, and increment the left edge for the next lane
                lateralOffsetAtStarts.add(leftEdgeOffsetStart.minus(link.lanes.get(i).laneWidth.times(0.5)));
                leftEdgeOffsetStart = leftEdgeOffsetStart.minus(link.lanes.get(i).laneWidth);
            }

            double currentOffsetEnd = link.lanes.get(i).laneOut - (link.fromLane + i); // in # of lanes
            if (link.lanes.get(i).taper.equals(">"))
            {
                currentOffsetEnd = currentOffsetEnd - 0.5;
            }
            if (currentOffsetEnd == offsetEnd)
            {
                lateralOffsetAtEnds.add(leftEdgeOffsetEnd.minus(link.lanes.get(i).laneWidth.times(0.5)));
                leftEdgeOffsetEnd = leftEdgeOffsetEnd.minus(link.lanes.get(i).laneWidth);
            }
            else if (currentOffsetEnd > offsetEnd || i == 0)
            {
                // a shift to the right; this must be the first lane
                // (note that for a lane adjacent to a diverge taper, this affects the start offset, not the end offset)

                // or...

                // a shift to the left, on the first lane

                // margin between actual left edge and left edge in grid assuming maximum lane widths
                Length leftEdgeMargin =
                        getLeftEdgeMax(link.fromLane).minus(getLeftLinkEdge(link.sectionIndex, link.fromLane, link.toLane));

                // add this margin to the left edge assuming maximum lane widths on the output lane
                Length actualLeftEdge = getLeftEdgeMax(link.lanes.get(i).laneOut).minus(leftEdgeMargin);
                leftEdgeOffsetEnd = actualLeftEdge.minus(Length.instantiateSI(startNode.getPoint().y)); // relative to line end

                // build from that left edge onwards
                lateralOffsetAtEnds.add(leftEdgeOffsetEnd.minus(link.lanes.get(i).laneWidth.times(0.5)));
                leftEdgeOffsetEnd = leftEdgeOffsetEnd.minus(link.lanes.get(i).laneWidth);
            }
            else
            {
                // a shift to the left, relative to the previous lane
                double stepsBack = offsetEnd - currentOffsetEnd;
                lateralOffsetAtEnds.add(lateralOffsetAtEnds.get(lateralOffsetAtEnds.size() - 1)
                        .minus(link.lanes.get(i).laneWidth.times(stepsBack)));
            }
            offsetEnd = currentOffsetEnd;

        }

        // build the lanes
        FosLane prevLane = null;
        boolean stripedAreas = getSetting(ParserSetting.STRIPED_AREAS);
        int stripeNum = 1;
        for (int laneNum = 0; laneNum < link.lanes.size(); laneNum++)
        {
            FosLane lane = link.lanes.get(laneNum);
            String id = String.format("Lane %d_%d", laneNum + 1, laneNum + link.fromLane);
            // calculate offset as y-distance between start/end node and from/to point, and add halve the lane width
            Length lateralOffsetAtStart = lateralOffsetAtStarts.get(laneNum);
            Length lateralOffsetAtEnd = lateralOffsetAtEnds.get(laneNum);
            Length startWidth = lane.taper.equals("<") ? Length.ZERO : lane.laneWidth;
            Length endWidth = lane.taper.equals(">") ? Length.ZERO : lane.laneWidth;

            OtsLine2d centerLine = designLine.offsetLine(lateralOffsetAtStart.si, lateralOffsetAtEnd.si);
            Polygon2d contour = LaneGeometryUtil.getContour(centerLine.offsetLine(startWidth.si * .5, endWidth.si * .5),
                    centerLine.offsetLine(-startWidth.si * .5, -endWidth.si * .5));
            // List<CrossSectionSlice> crossSectionSlices =
            // List.of(new CrossSectionSlice(Length.ZERO, lateralOffsetAtStart, startWidth),
            // new CrossSectionSlice(centerLine.getLength(), lateralOffsetAtEnd, endWidth));
            CrossSectionGeometry geometry = new CrossSectionGeometry(centerLine, contour,
                    FractionalLengthData.of(0.0, lateralOffsetAtStart.si, 1.0, lateralOffsetAtEnd.si),
                    FractionalLengthData.of(0.0, startWidth.si, 1.0, endWidth.si));

            CrossSectionElement otsLane;
            if (!lane.isShoulder(stripedAreas))
            {
                otsLane =
                        new Lane(otsLink, id, geometry, DefaultsRoadNl.HIGHWAY, Map.of(DefaultsNl.ROAD_USER, lane.speedLimit));
            }
            else
            {
                otsLane = new Shoulder(otsLink, id, geometry, SHOULDER); // OTS 175: , SHOULDER
            }
            lane.setLane(otsLane);

            // Type type = null;
            StripeData stripeData;
            // CrossSectionSlice start;
            // CrossSectionSlice end;

            double startStripeOffset;
            double endStripeOffset;
            double startStripeWidth;
            double endStripeWidth;

            if (laneNum == 0)
            {
                startStripeOffset = lateralOffsetAtStart.minus(EDGE_STRIPE_GAP).plus(startWidth.times(0.5)).si;
                endStripeOffset = lateralOffsetAtEnd.minus(EDGE_STRIPE_GAP).plus(endWidth.times(0.5)).si;
                startStripeWidth = 0.2;
                endStripeWidth = 0.2;
                stripeData = DefaultsRoadNl.SOLID;
                // start = new CrossSectionSlice(Length.ZERO,
                // lateralOffsetAtStart.minus(EDGE_STRIPE_GAP).plus(startWidth.times(0.5)), Length.instantiateSI(0.2));
                // end = new CrossSectionSlice(otsLink.getLength(),
                // lateralOffsetAtEnd.minus(EDGE_STRIPE_GAP).plus(endWidth.times(0.5)), Length.instantiateSI(0.2));
                // type = Type.SOLID;
            }
            else
            {
                Length width = null;
                if (lane.taper.equals(">"))
                {
                    stripeData = DefaultsRoadNl.LEFT;
                    // type = Type.LEFT;
                    width = Length.instantiateSI(0.6);
                }
                else if (lane.taper.equals("<") || prevLane.taper.equals(">") || prevLane.taper.equals("<"))
                {
                    stripeData = DefaultsRoadNl.RIGHT;
                    // type = Type.RIGHT;
                    width = Length.instantiateSI(0.6);
                }
                else if (prevLane.isShoulder(stripedAreas) || lane.isShoulder(stripedAreas))
                {
                    stripeData = DefaultsRoadNl.SOLID;
                    // type = Type.SOLID;
                    width = Length.instantiateSI(0.2);
                }
                else if (lane.canChangeLeft(stripedAreas) && prevLane.canChangeRight(stripedAreas))
                {
                    stripeData = DefaultsRoadNl.DASHED;
                    // type = Type.DASHED;
                    width = Length.instantiateSI(0.2);
                }
                else if (lane.canChangeLeft(stripedAreas))
                {
                    stripeData = DefaultsRoadNl.LEFT;
                    // type = Type.LEFT;
                    width = Length.instantiateSI(0.6);
                }
                else
                {
                    stripeData = DefaultsRoadNl.RIGHT;
                    // type = Type.RIGHT;
                    width = Length.instantiateSI(0.6);
                }
                startStripeOffset = lateralOffsetAtStart.plus(startWidth.times(0.5)).si;
                endStripeOffset = lateralOffsetAtEnd.plus(endWidth.times(0.5)).si;
                startStripeWidth = width.si;
                endStripeWidth = width.si;
                // start = new CrossSectionSlice(Length.ZERO, lateralOffsetAtStart.plus(startWidth.times(0.5)), width);
                // end = new CrossSectionSlice(otsLink.getLength(), lateralOffsetAtEnd.plus(endWidth.times(0.5)), width);
            }
            centerLine = designLine.offsetLine(startStripeOffset, endStripeOffset);
            contour = LaneGeometryUtil.getContour(centerLine.offsetLine(startStripeWidth * .5, endStripeWidth * .5),
                    centerLine.offsetLine(-startStripeWidth * .5, -endStripeWidth * .5));
            CrossSectionGeometry stripeGeometry = new CrossSectionGeometry(centerLine, contour,
                    FractionalLengthData.of(0.0, startStripeOffset, 1.0, endStripeOffset),
                    FractionalLengthData.of(0.0, startStripeWidth, 1.0, endStripeWidth));
            new Stripe("" + stripeNum++, stripeData, otsLink, stripeGeometry);

            if (laneNum == link.lanes.size() - 1)
            {
                startStripeOffset = lateralOffsetAtStart.plus(EDGE_STRIPE_GAP).minus(startWidth.times(0.5)).si;
                endStripeOffset = lateralOffsetAtEnd.plus(EDGE_STRIPE_GAP).minus(endWidth.times(0.5)).si;
                startStripeWidth = 0.2;
                endStripeWidth = 0.2;
                stripeData = DefaultsRoadNl.SOLID;
                // start = new CrossSectionSlice(Length.ZERO,
                // lateralOffsetAtStart.plus(EDGE_STRIPE_GAP).minus(startWidth.times(0.5)), Length.instantiateSI(0.2));
                // end = new CrossSectionSlice(otsLink.getLength(),
                // lateralOffsetAtEnd.plus(EDGE_STRIPE_GAP).minus(endWidth.times(0.5)), Length.instantiateSI(0.2));
                // type = Type.SOLID;
                centerLine = designLine.offsetLine(startStripeOffset, endStripeOffset);
                contour = LaneGeometryUtil.getContour(centerLine.offsetLine(startStripeWidth * .5, endStripeWidth * .5),
                        centerLine.offsetLine(-startStripeWidth * .5, -endStripeWidth * .5));
                stripeGeometry = new CrossSectionGeometry(centerLine, contour,
                        FractionalLengthData.of(0.0, startStripeOffset, 1.0, endStripeOffset),
                        FractionalLengthData.of(0.0, startStripeWidth, 1.0, endStripeWidth));
                new Stripe("" + stripeNum++, stripeData, otsLink, stripeGeometry);
                // new Stripe(type, otsLink, centerLine, contour, List.of(start, end));
            }

            prevLane = lane;
        }
    }

    /**
     * Returns the lateral coordinate of the left edge. The section is centered by its actual width, in the space assuming a
     * grid of maximum lane widths.
     * @param sectionIndex section index.
     * @param fromLane from lane.
     * @param toLane to lane.
     * @return lateral coordinate of the left edge for a link.
     */
    private Length getLeftLinkEdge(final int sectionIndex, final int fromLane, final int toLane)
    {
        // derive max lane widths, if we haven't already
        if (this.maxLaneWidth == null)
        {
            this.maxLaneWidth = new Length[this.lane.size()];
            for (int laneIndex = 0; laneIndex < this.lane.size(); laneIndex++)
            {
                this.maxLaneWidth[laneIndex] = Length.ZERO;
            }
            for (int laneIndex = 0; laneIndex < this.lane.size(); laneIndex++)
            {
                // for each lane, loop all sections and store the maximum width
                for (FosLane lane : this.lane.get(laneIndex))
                {
                    this.maxLaneWidth[laneIndex] = Length.max(this.maxLaneWidth[laneIndex], lane.laneWidth);
                    if (lane.laneOut != laneIndex)
                    {
                        // also maximize out lane for diagonal lanes
                        this.maxLaneWidth[lane.laneOut] = Length.max(this.maxLaneWidth[lane.laneOut], lane.laneWidth);
                    }
                }
            }
        }

        // get left edge assuming all lanes are at maximum width
        Length leftEdgeMax = getLeftEdgeMax(fromLane);

        // get width if all lanes are at maximum width, and actual width
        Length linkWidthMax = Length.ZERO;
        Length linkWidth = Length.ZERO;
        for (int laneIndex = fromLane; laneIndex <= toLane; laneIndex++)
        {
            linkWidthMax = linkWidthMax.plus(this.maxLaneWidth[laneIndex]);
            linkWidth = linkWidth.plus(this.lane.get(laneIndex).get(sectionIndex).laneWidth);
        }

        // add halve of space to left edge
        return leftEdgeMax.minus(linkWidthMax.minus(linkWidth).times(.5));
    }

    /**
     * Returns the left-edge coordinate of a lane, assuming maximum width of all lanes to the left.
     * @param lane lane index.
     * @return left-edge coordinate of a lane, assuming maximum width of all lanes to the left.
     */
    private Length getLeftEdgeMax(final int lane)
    {
        Length leftEdgeMax = Length.ZERO;
        for (int laneIndex = 0; laneIndex < lane; laneIndex++)
        {
            leftEdgeMax = leftEdgeMax.minus(this.maxLaneWidth[laneIndex]);
        }
        return leftEdgeMax;
    }

    /**
     * Builds the GTU types.
     */
    private void buildGtuTypes()
    {
        for (int vehicleTypeNumber = 0; vehicleTypeNumber < this.vehicleTypeNames.size(); vehicleTypeNumber++)
        {
            this.gtuTypes.add(new GtuType(this.vehicleTypeNames.get(vehicleTypeNumber), DefaultsNl.VEHICLE));
        }
    }

    /**
     * Build the vehicle generators.
     * @throws ParameterException if a parameter is missing
     * @throws SimRuntimeException if this method is called after simulation time 0
     */
    private void buildGenerators() throws SimRuntimeException, ParameterException
    {
        List<Node> origins = new ArrayList<>();
        this.source.forEach((s) -> origins.add(s.node));
        List<Node> destinations = new ArrayList<>();
        this.sink.forEach((s) -> destinations.add(s.node));
        Categorization categorization = new Categorization("GTU type", GtuType.class);
        TimeVector globalTimeVector = new TimeVector(DoubleVectorData.instantiate(
                new Time[] {Time.ZERO, Time.instantiateSI(this.timeStep.times(this.maximumSimulationTime).si)},
                StorageType.DENSE), TimeUnit.BASE_SECOND);
        Interpolation globalInterpolation = Interpolation.LINEAR;
        OdMatrix od = new OdMatrix("Fosim OD", origins, destinations, categorization, globalTimeVector, globalInterpolation);

        // prepare categories
        List<Category> categories = new ArrayList<>();
        for (GtuType gtuType : this.gtuTypes)
        {
            categories.add(new Category(categorization, gtuType));
        }

        // prepare options
        StreamInterface stream = this.network.getSimulator().getModel().getStream("generation");
        OdOptions options = new OdOptions();
        options.set(OdOptions.INSTANT_LC, getSetting(ParserSetting.INSTANT_LC));
        options.set(OdOptions.NO_LC_DIST, Length.instantiateSI(50.0));

        // lane biases
        LaneBiases laneBiases = new LaneBiases();
        for (int vehicleTypeNumber = 0; vehicleTypeNumber < this.vehicleTypeNames.size(); vehicleTypeNumber++)
        {
            if (this.isTruck.get(vehicleTypeNumber))
            {
                laneBiases.addBias(this.gtuTypes.get(vehicleTypeNumber), LaneBias.TRUCK_RIGHT);
            }
            else
            {
                laneBiases.addBias(this.gtuTypes.get(vehicleTypeNumber), LaneBias.WEAK_LEFT);
            }
        }
        options.set(OdOptions.LANE_BIAS, laneBiases);

        // templates and parameters
        ParameterFactoryByType parameterFactory = new ParameterFactoryByType();

        this.parameterSupplier.setAllInParameterFactory(this.gtuTypes, parameterFactory);
        Map<GtuType, GtuTemplate> templates = new LinkedHashMap<>();
        if (this.otsParameters == null)
        {
            parameterFactory.addParameter(ParameterTypes.FSPEED, new DistNormal(stream, 123.7 / 120.0, 0.1));
            for (int vehicleTypeNumber = 0; vehicleTypeNumber < this.vehicleTypeNames.size(); vehicleTypeNumber++)
            {
                Length length = Length.instantiateSI(getParameterValue(vehicleTypeNumber, "length"));
                Length width = Length.instantiateSI(getParameterValue(vehicleTypeNumber, "vehicle width"));
                Generator<Speed> speed;
                if (this.isTruck.get(vehicleTypeNumber))
                {
                    speed = new ContinuousDistSpeed(new DistNormal(stream, 85.0, 2.5), SpeedUnit.KM_PER_HOUR);
                }
                else
                {
                    Speed vMax = new Speed(180.0, SpeedUnit.KM_PER_HOUR);
                    speed = () -> vMax;
                }
                GtuType gtuType = this.gtuTypes.get(vehicleTypeNumber);
                templates.put(gtuType, new GtuTemplate(gtuType, () -> length, () -> width, speed));
            }
        }
        else
        {
            OtsParametersParser.parse(this.gtuTypes, this.otsParameters, templates, parameterFactory, stream);
            if (this.getSetting(ParserSetting.INSTANT_LC))
            {
                for (GtuType gtuType : this.gtuTypes)
                {
                    // TODO: This is a temporary fix to effectively disable LaneChange.checkRoom(...)
                    parameterFactory.addParameter(gtuType, ParameterTypes.LCDUR, Duration.instantiateSI(1e-12));
                }
            }
        }

        // factories
        List<LaneBasedStrategicalRoutePlannerFactory> factories = new ArrayList<>();
        for (int vehicleTypeNumber = 0; vehicleTypeNumber < this.vehicleTypeNames.size(); vehicleTypeNumber++)
        {
            factories.add(getStrategicalPlannerFactory(parameterFactory, stream, vehicleTypeNumber));
        }
        LaneBasedGtuCharacteristicsGeneratorOd characteristicsGenerator = (origin, destination, category, randomStream) ->
        {
            GtuType gtuType = category.get(GtuType.class);
            GtuCharacteristics gtuCharacteristics =
                    Try.assign(() -> templates.get(gtuType).draw(), "Unable to draw GTU characteristics");
            VehicleModel vehicleModel = VehicleModel.MINMAX;
            return new LaneBasedGtuCharacteristics(gtuCharacteristics, factories.get(FosParser.this.gtuTypes.indexOf(gtuType)),
                    null, origin, destination, vehicleModel);
        };
        options.set(OdOptions.GTU_TYPE, characteristicsGenerator);

        // demand
        for (int sourceIndex = 0; sourceIndex < this.flow.size(); sourceIndex++)
        {
            FosFlow flow = this.flow.get(sourceIndex);
            Node sourceNode = origins.get(sourceIndex);
            FosList<List<Double>> sinks = this.sourceToSink.get(sourceIndex);
            for (int sinkIndex = 0; sinkIndex < sinks.size(); sinkIndex++)
            {
                Node sinkNode = destinations.get(sinkIndex);
                List<Double> sinkFractions = sinks.get(sinkIndex);
                for (int vehicleTypeIndex = 0; vehicleTypeIndex < sinkFractions.size(); vehicleTypeIndex++)
                {
                    double sinkFraction = sinkFractions.get(vehicleTypeIndex);
                    double vehicleFraction = this.vehicleProbabilities.get(sourceIndex).get(vehicleTypeIndex);
                    if (flow.getFrequencyVector().size() == 1)
                    {
                        flow.flow.add(flow.flow.get(0));
                        flow.time.add(Time.instantiateSI(this.maximumSimulationTime * this.timeStep.si));
                    }
                    od.putDemandVector(sourceNode, sinkNode, categories.get(vehicleTypeIndex), flow.getFrequencyVector(),
                            flow.getTimeVector(), Interpolation.LINEAR, sinkFraction * vehicleFraction);
                }
            }
        }

        // apply
        OdApplier.applyOd(this.network, od, options, DefaultsNl.ROAD_USERS);
    }

    /**
     * Creates a factory with all required model components, including optional social interactions and perception.
     * @param parameterFactory parameter factory.
     * @param stream stream of random numbers.
     * @param vehicleTypeNumber vehicle type number.
     * @return factory.
     */
    private LaneBasedStrategicalRoutePlannerFactory getStrategicalPlannerFactory(final ParameterFactory parameterFactory,
            final StreamInterface stream, final int vehicleTypeNumber)
    {
        // figure out which components to use
        boolean social = false;
        boolean courtesy = false;
        boolean perception = false;
        boolean estimation = false;
        boolean anticipation = false;
        if (this.otsParameters != null)
        {
            for (ParameterDataGroup group : this.otsParameters.parameterGroups)
            {
                if (group.id.equals(ParameterDefinitions.SOCIAL_GROUP_ID) && group.state != null && group.state.isActive())
                {
                    social = true;
                }
                else if (group.id.equals(ParameterDefinitions.COURTESY_GROUP_ID) && group.state != null
                        && group.state.isActive())
                {
                    courtesy = true;
                }
                else if (group.id.equals(ParameterDefinitions.PERCEPTION_GROUP_ID) && group.state != null
                        && group.state.isActive())
                {
                    perception = true;
                }
                else if (group.id.equals(ParameterDefinitions.ESTIMATION_GROUP_ID) && group.state != null
                        && group.state.isActive())
                {
                    estimation = true;
                }
                else if (group.id.equals(ParameterDefinitions.ANTICIPATION_GROUP_ID) && group.state != null
                        && group.state.isActive())
                {
                    anticipation = true;
                }
            }
        }
        boolean isTruck = this.isTruck.get(vehicleTypeNumber);
        Estimation estimation2 = perception && estimation ? Estimation.FACTOR_ESTIMATION : Estimation.NONE;
        Anticipation anticipation2 = perception && anticipation ? Anticipation.CONSTANT_SPEED : Anticipation.NONE;

        // car-following
        DistContinuous fSpeed = new DistNormal(stream, 123.7 / 120.0, 0.1);
        CarFollowingModelFactory<IdmPlus> cfModelFactory =
                !social ? new IdmPlusFactory(stream) : new CarFollowingModelFactory<>()
                {
                    @Override
                    public Parameters getParameters() throws ParameterException
                    {
                        ParameterSet parameters = new ParameterSet();
                        parameters.setDefaultParameters(AbstractIdm.class);
                        parameters.setDefaultParameters(SocioDesiredSpeed.class);
                        parameters.setParameter(ParameterTypes.FSPEED, fSpeed.draw());
                        return parameters;
                    }

                    @Override
                    public IdmPlus generateCarFollowingModel()
                    {
                        return new IdmPlus(AbstractIdm.HEADWAY, new SocioDesiredSpeed(AbstractIdm.DESIRED_SPEED));
                    }
                };
        // new AbstractIdmFactory<>(
        // new IdmPlus(AbstractIdm.HEADWAY, new SocioDesiredSpeed(AbstractIdm.DESIRED_SPEED)), stream)

        // perception
        PerceptionFactory perceptionFactory = perception ? new PerceptionFactory()
        {
            /** {@inheritDoc} */
            @Override
            public Parameters getParameters() throws ParameterException
            {
                return new ParameterSet(); // all parameters are given by FOSIM and parsed in to the ParameterFactoryByType
            }

            /** {@inheritDoc} */
            @Override
            public LanePerception generatePerception(final LaneBasedGtu gtu)
            {
                Set<Task> tasks = new LinkedHashSet<>();
                tasks.add(new CarFollowingTask());
                tasks.add(new LaneChangeTask());
                Set<BehavioralAdaptation> behavioralAdapatations = new LinkedHashSet<>();
                behavioralAdapatations.add(new AdaptationSituationalAwareness()); // sets SA and reaction time
                behavioralAdapatations.add(new AdaptationHeadway());
                behavioralAdapatations.add(new AdaptationSpeed());
                TaskManager taskManager = new TaskManagerAr("lane-changing");
                CategoricalLanePerception perception =
                        new CategoricalLanePerception(gtu, new Fuller(tasks, behavioralAdapatations, taskManager));
                HeadwayGtuType headwayGtuType = new PerceivedHeadwayGtuType(estimation2, anticipation2);
                perception.addPerceptionCategory(new DirectEgoPerception<>(perception));
                perception.addPerceptionCategory(new DirectInfrastructurePerception(perception));
                perception.addPerceptionCategory(new DirectNeighborsPerception(perception, headwayGtuType));
                perception.addPerceptionCategory(new AnticipationTrafficPerception(perception));
                perception.addPerceptionCategory(new DirectIntersectionPerception(perception, HeadwayGtuType.WRAP));
                return perception;
            }
        } : new DefaultLmrsPerceptionFactory();

        // tailgating
        Tailgating tailgating = social ? Tailgating.PRESSURE : Tailgating.NONE;

        // incentives: voluntary, mandatory, acceleration
        Set<MandatoryIncentive> mandatoryIncentives = new LinkedHashSet<>();
        mandatoryIncentives.add(new FosIncentiveRoute());
        Set<VoluntaryIncentive> voluntaryIncentives = new LinkedHashSet<>();
        voluntaryIncentives.add(new IncentiveSpeedWithCourtesy());
        voluntaryIncentives.add(new IncentiveKeep());
        if (social)
        {
            voluntaryIncentives.add(new IncentiveSocioSpeed());
            if (courtesy)
            {
                voluntaryIncentives.add(new IncentiveCourtesy());
            }
        }
        if (isTruck)
        {
            voluntaryIncentives.add(new IncentiveStayRight());
        }
        Set<AccelerationIncentive> accelerationIncentives = new LinkedHashSet<>();
        accelerationIncentives.add(new AccelerationTrafficLights());

        // create the factories
        LmrsFactory lmrsFactory = new LmrsFactory(cfModelFactory, perceptionFactory, Synchronization.PASSIVE_MOVING,
                Cooperation.PASSIVE, GapAcceptance.INFORMED, tailgating, () -> mandatoryIncentives, () -> voluntaryIncentives,
                () -> accelerationIncentives);
        return new LaneBasedStrategicalRoutePlannerFactory(lmrsFactory, parameterFactory);
    }

    /**
     * Returns the FOSIM parameter value for given vehicle type number, and parameter name.
     * @param vehicleTypeNumber vehicle type number.
     * @param parameterName parameter name as in FOSIM specification.
     * @return parameter value.
     * @throws ParameterException if the parameter is not found for the vehicle type.
     */
    private double getParameterValue(final int vehicleTypeNumber, final String parameterName) throws ParameterException
    {
        for (FosParameter param : this.specificParameter.get(vehicleTypeNumber))
        {
            if (param.name.equals(parameterName))
            {
                return param.value;
            }
        }
        throw new ParameterException("No parameter " + parameterName + " for vehicle type " + vehicleTypeNumber);
    }

    /**
     * Build the detectors.
     * @throws NetworkException; on network exception
     */
    private void buildDetecors() throws NetworkException
    {
        Time firstAggregation = Time.instantiateSI(this.timeStep.si * this.detectorTimes.get(0));
        Duration aggregationTime = this.timeStep.times(this.detectorTimes.get(1));

        Map<String, Time> prevTime = new LinkedHashMap<>();
        Map<String, Time> thisTime = new LinkedHashMap<>();
        for (int detectorCrossSection = 0; detectorCrossSection < this.detectorPositions.size(); detectorCrossSection++)
        {
            Length position = this.detectorPositions.get(detectorCrossSection);
            for (Link link : this.network.getLinkMap().values())
            {
                if (link.getStartNode().getLocation().x <= position.si && position.si < link.getEndNode().getLocation().x)
                {
                    double fraction = (position.si - link.getStartNode().getLocation().x)
                            / (link.getEndNode().getLocation().x - link.getStartNode().getLocation().x);
                    CrossSectionLink crossSectionLink = (CrossSectionLink) link;
                    int firstLane = -1;
                    for (int i = 0; i < this.mappedLinks.length; i++)
                    {
                        for (int j = 0; j < this.mappedLinks[i].length; j++)
                        {
                            int linkNum = this.mappedLinks[i][j];
                            if (linkNum > 0 && ("Link " + linkNum).equals(link.getId()))
                            {
                                firstLane = i;
                                break;
                            }
                        }
                        if (firstLane >= 0)
                        {
                            break;
                        }
                    }
                    for (Lane lane : crossSectionLink.getLanes())
                    {
                        Length longitudinalPosition = lane.getLength().times(fraction);
                        int underscore = lane.getId().indexOf("_");
                        int laneNum = Integer.valueOf(lane.getId().substring(5, underscore)) + (firstLane - 1);
                        // Id's are "1_2" where 1=detector cross-section, and 2=lane 2 (both start counting at 0)
                        String id = (this.detectorPositions.size() - detectorCrossSection - 1) + "_" + laneNum;
                        if (getSetting(ParserSetting.FOS_DETECTORS))
                        {
                            new FosDetector(id, lane, longitudinalPosition, this.network.getSimulator(), prevTime, thisTime,
                                    firstAggregation, aggregationTime);
                        }
                        else
                        {
                            new LoopDetector(id, new LanePosition(lane, longitudinalPosition), Length.instantiateSI(1.5),
                                    DefaultsNl.ROAD_USERS, firstAggregation, aggregationTime, LoopDetector.MEAN_SPEED);
                        }
                    }
                }
            }
            prevTime = thisTime;
            thisTime = new LinkedHashMap<>();
        }
    }

    /**
     * Build the traffic lights.
     * @throws NetworkException; on network exception
     */
    private void buildTrafficLights() throws NetworkException
    {
        int lightNum = 0;
        for (FosTrafficLight light : this.trafficLight)
        {
            for (int section = 0; section < this.sections.size() - 1; section++)
            {
                Length to = this.sections.get(section + 1);
                if (to.gt(light.position))
                {
                    FosLane lane = this.lane.get(light.lane).get(section + 1);
                    Lane otsLane = (Lane) lane.getLane();
                    if (otsLane == null)
                    {
                        System.out.println("Traffic light refers to a lane that was not created.");
                    }
                    else
                    {
                        Length from = this.sections.get(section);
                        double f = (light.position.si - from.si) / (to.si - from.si);
                        Length position = otsLane.getLength().times(f);
                        new TrafficLight("" + lightNum, otsLane, position);
                        SignalGroup group = new SignalGroup(("" + lightNum), Set.of(otsLane.getFullId() + "." + lightNum),
                                light.startOffset, light.greenTime, light.yellowTime);
                        new FixedTimeController("" + lightNum, this.network.getSimulator(), this.network, light.cycleTime,
                                Duration.ZERO, Set.of(group)); // offset already in group
                        lightNum++;
                    }
                    break;
                }
            }
        }
    }

    /**
     * Returns the part of a line after the ":", with leading and trailing white spaces trimmed.
     * @param line line.
     * @return part of a line after the ":", with leading and trailing white spaces trimmed.
     */
    private static String fieldValue(final String line)
    {
        return line.substring(line.indexOf(":") + 1).trim();
    }

    /**
     * Returns the index of the field described in the line. This is "0" in the line "lane 0: ...".
     * @param line line.
     * @return index of the field described in the line.
     */
    private static int fieldIndex(final String line)
    {
        // get string until ":" and trim any spaces
        String field = line.substring(0, line.indexOf(":")).trim();
        // parse string from last blank till end as int
        return Integer.parseInt(field.substring(field.lastIndexOf(" ") + 1));
    }

    /**
     * Returns two indices of the field described in the line. This is "[1 0]" for the line "source to sink 1 0: ...".
     * @param line line.
     * @return indices of the field described in the line.
     */
    private static int[] fieldIndices(final String line)
    {
        int[] out = new int[2];
        // get string until ":" and trim any spaces
        String field = line.substring(0, line.indexOf(":")).trim();
        // parse string from last blank till end as int
        int blank = field.lastIndexOf(" ");
        out[1] = Integer.parseInt(field.substring(blank + 1));
        // get string until last blank (cutting the parsed index above) and trim any spaces
        field = field.substring(0, blank).trim();
        // parse string from last blank till end as int
        out[0] = Integer.parseInt(field.substring(field.lastIndexOf(" ") + 1));
        return out;
    }

    /**
     * Reads a list of values from a blank-separated string. This method can deal with consecutive blanks.
     * @param list list to place the parsed values in.
     * @param string blank-separated list of integers.
     * @param converter value converter.
     * @param <T> type to convert the integers in to.
     * @return the input list.
     */
    private static <T> List<T> parseValueList(final List<T> list, final String string,
            final Function<String, ? extends T> converter)
    {
        for (String numString : splitStringByBlank(string, 0))
        {
            list.add(converter.apply(numString));
        }
        return list;
    }

    /**
     * Returns the next part of a line that describes a single section of a lane.
     * @param line line.
     * @param from index to start, should be incremented with the length of each returned lane string.
     * @return next part of a line that describes a single section of a lane.
     */
    private static String nextLaneString(final String line, final int from)
    {
        int to = from + 9; // in the first bit per lane, there are blanks, we need the blank after all that
        to = line.indexOf(" ", to);
        if (to == -1)
        {
            // last lane has no blank behind it
            return line.substring(from);
        }
        return line.substring(from, to);
    }

    /**
     * Splits a string, and trims the resulting fields.
     * @param string string to split.
     * @param delimiter delimiter.
     * @return trimmed fields.
     */
    static String[] splitAndTrimString(final String string, final String delimiter)
    {
        String[] fields = string.split(delimiter);
        for (int i = 0; i < fields.length; i++)
        {
            fields[i] = fields[i].trim();
        }
        return fields;
    }

    /**
     * Splits a string by blanks. Consecutive blanks are considered as one blank. If a number of fields is specified, the last
     * field may contain an overflow of blanks in the line. For example when 4 fields are requested, the string "0 5 3 The
     * Hague" results in a 4th field "The Hague".
     * @param string string.
     * @param numFields Number of fields to return. Use 0 for as many as required.
     * @return split strings.
     */
    static String[] splitStringByBlank(final String string, final int numFields)
    {
        if (string.isEmpty())
        {
            return new String[0];
        }
        String trimmedString = string.trim();
        List<String> fields = new ArrayList<>(numFields);
        int from = 0;
        while (true)
        {
            // skip any number of blanks between fields
            while (trimmedString.substring(from, from + 1).isBlank())
            {
                from++;
            }
            // find next blank
            int to = trimmedString.indexOf(" ", from);
            // stop on last field, as specified by numFields, or because there is no further blank
            if (fields.size() == numFields - 1 || to < 0)
            {
                // add all that remains as the final field
                fields.add(trimmedString.substring(from));
                return fields.toArray(new String[fields.size()]);
            }
            // store field and move to next
            fields.add(trimmedString.substring(from, to));
            from = to + 1;
        }
    }

    // TODO: remove this after new OTS version; we get lanes from the root cross section, and those are available in
    // getLegalLaneChangeInfo
    private static class IncentiveStayRight implements VoluntaryIncentive
    {
        public Desire determineDesire(final Parameters parameters, final LanePerception perception,
                final CarFollowingModel carFollowingModel, final Desire mandatoryDesire, final Desire voluntaryDesire)
                throws ParameterException, OperationalPlanException
        {
            InfrastructurePerception infra = perception.getPerceptionCategory(InfrastructurePerception.class);
            // start at left-most lane
            SortedSet<RelativeLane> rootCrossSection = perception.getLaneStructure().getRootCrossSection();
            RelativeLane lane = rootCrossSection.first();
            // move right until we find 'the right-hand lane', which is defined by the last lane where the urgency does not
            // increase
            Speed speed = perception.getPerceptionCategory(EgoPerception.class).getSpeed();
            double curUrgency = !perception.getLaneStructure().exists(lane) ? Double.MAX_VALUE
                    : urgency(infra.getLegalLaneChangeInfo(lane), parameters, speed);
            double rightUrgency;
            RelativeLane right;
            while (rootCrossSection.contains(right = lane.getRight())
                    && (rightUrgency = !perception.getLaneStructure().exists(right) ? Double.MAX_VALUE
                            : urgency(infra.getLegalLaneChangeInfo(right), parameters, speed)) <= curUrgency)
            {
                curUrgency = rightUrgency;
                lane = right;
            }
            boolean legalLeft = infra.getLegalLaneChangePossibility(RelativeLane.CURRENT, LateralDirectionality.LEFT).ge0();
            if (lane.getLateralDirectionality().isRight() && lane.getNumLanes() > 1)
            {
                // must change right
                return new Desire(legalLeft ? -1.0 : 0.0, parameters.getParameter(LmrsParameters.DSYNC));
            }
            if (lane.isRight())
            {
                // must not change left
                return new Desire(legalLeft ? -1.0 : 0.0, 0.0);
            }
            return new Desire(0.0, 0.0);
        }

        /**
         * Returns the urgency to leave a lane.
         * @param laneChangeInfo SortedSet&lt;InfrastructureLaneChangeInfo&gt;; lane change info on the lane
         * @param parameters Parameters; parameters
         * @param speed Speed; current speed
         * @return double; urgency to leave the lane
         * @throws ParameterException if parameter is not given
         */
        private double urgency(final SortedSet<LaneChangeInfo> laneChangeInfo, final Parameters parameters, final Speed speed)
                throws ParameterException
        {
            double urgency = 0.0;
            for (LaneChangeInfo info : laneChangeInfo)
            {
                double nextUrgency = FosIncentiveRoute.getDesireToLeave(parameters, info.remainingDistance(),
                        info.numberOfLaneChanges(), speed);
                urgency = urgency > nextUrgency ? urgency : nextUrgency;
            }
            return urgency;
        }

        // ABOVE: OTS 1.7.5
        // BELOW: OTS 1.7.4

        // /** {@inheritDoc} */
        // @Override
        // public Desire determineDesire(final Parameters parameters, final LanePerception perception,
        // final CarFollowingModel carFollowingModel, final Desire mandatoryDesire, final Desire voluntaryDesire)
        // throws ParameterException, OperationalPlanException
        // {
        // InfrastructurePerception infra = perception.getPerceptionCategory(InfrastructurePerception.class);
        // LaneStructureRecord root = perception.getLaneStructure().getRootRecord();
        // LaneStructureRecord record = root;
        // RelativeLane lane = RelativeLane.CURRENT;
        // Route route = Try.assign(() -> perception.getGtu().getStrategicalPlanner().getRoute(), "");
        // GtuType gtuType = Try.assign(() -> perception.getGtu().getType(), "");
        // Speed speed = perception.getPerceptionCategory(EgoPerception.class).getSpeed();
        // // move all the way left
        // while (record.physicalLeft())
        // {
        // lane = lane.getLeft();
        // record = record.getLeft();
        // }
        // // move right until we find 'the right-hand lane',
        // double curUrgency = urgency(infra.getInfrastructureLaneChangeInfo(lane), parameters, speed);
        // while (true)
        // {
        // try
        // {
        // if (!record.physicalRight() || !record.getRight().allowsRoute(route, gtuType))
        // {
        // // next lane considered not there, we've found 'the right-hand lane'
        // break;
        // }
        // }
        // catch (NetworkException exception)
        // {
        // throw new RuntimeException(exception);
        // }
        // double rightUrgency = urgency(infra.getInfrastructureLaneChangeInfo(lane.getRight()), parameters, speed);
        // if (rightUrgency > curUrgency)
        // {
        // // next lane is worse for the route, current lane is allowable for the route
        // break;
        // }
        // lane = lane.getRight();
        // record = record.getRight();
        // curUrgency = rightUrgency;
        // }
        // if (lane.getLateralDirectionality().isRight() && lane.getNumLanes() > 1)
        // {
        // // must change right
        // return new Desire(root.legalLeft() ? -1.0 : 0.0, parameters.getParameter(LmrsParameters.DSYNC));
        // }
        // if (lane.isRight())
        // {
        // // must not change left
        // return new Desire(root.legalLeft() ? -1.0 : 0.0, 0.0);
        // }
        // return new Desire(0.0, 0.0);
        // }

        // /**
        // * Returns the urgency to leave a lane.
        // * @param laneChangeInfo SortedSet&lt;InfrastructureLaneChangeInfo&gt;; lane change info on the lane
        // * @param parameters Parameters; parameters
        // * @param speed Speed; current speed
        // * @return double; urgency to leave the lane
        // * @throws ParameterException if parameter is not given
        // */
        // private double urgency(final SortedSet<InfrastructureLaneChangeInfo> laneChangeInfo, final Parameters parameters,
        // final Speed speed) throws ParameterException
        // {
        // double urgency = 0.0;
        // for (InfrastructureLaneChangeInfo info : laneChangeInfo)
        // {
        // double nextUrgency = FosIncentiveRoute.getDesireToLeave(parameters, info.getRemainingDistance(),
        // info.getRequiredNumberOfLaneChanges(), speed);
        // urgency = urgency > nextUrgency ? urgency : nextUrgency;
        // }
        // return urgency;
        // }

    }

    private static Synchronization ALIGN_GAP = new Synchronization()
    {
        /** {@inheritDoc} */
        @Override
        public Acceleration synchronize(final LanePerception perception, final Parameters params, final SpeedLimitInfo sli,
                final CarFollowingModel cfm, final double desire, final LateralDirectionality lat, final LmrsData lmrsData,
                final LaneChange laneChange, final LateralDirectionality initiatedLaneChange)
                throws ParameterException, OperationalPlanException
        {
            Acceleration a = Acceleration.POSITIVE_INFINITY;
            EgoPerception<?, ?> ego = perception.getPerceptionCategory(EgoPerception.class);
            Speed ownSpeed = ego.getSpeed();

            // skip synchronization at low speed if follower will not provide cooperation
            Speed threshold = params.getParameter(ParameterTypes.LOOKAHEAD).divide(params.getParameter(ParameterTypes.T0));
            if (desire < params.getParameter(LmrsParameters.DCOOP) && ownSpeed.lt(threshold))
            {
                return a;
            }

            RelativeLane relativeLane = new RelativeLane(lat, 1);
            PerceptionCollectable<HeadwayGtu, LaneBasedGtu> leaders =
                    perception.getPerceptionCategory(NeighborsPerception.class).getLeaders(relativeLane);
            if (!leaders.isEmpty())
            {
                HeadwayGtu leader = leaders.first();
                Length gap = leader.getDistance();
                setDesiredHeadway(params, desire);
                PerceptionCollectable<HeadwayGtu, LaneBasedGtu> followers =
                        perception.getPerceptionCategory(NeighborsPerception.class).getFollowers(relativeLane);
                if (!followers.isEmpty())
                {
                    HeadwayGtu follower = followers.first();
                    Length netGap = leader.getDistance().plus(follower.getDistance()).times(0.5);
                    gap = Length.max(gap, leader.getDistance().minus(netGap).plus(cfm.desiredHeadway(params, ownSpeed)));
                }
                a = CarFollowingUtil.followSingleLeader(cfm, params, ownSpeed, sli, gap, leader.getSpeed());
                params.resetParameter(ParameterTypes.T);
                // limit deceleration based on desire
                a = gentleUrgency(a, desire, params);
            }
            a = Acceleration.min(a,
                    DEAD_END.synchronize(perception, params, sli, cfm, desire, lat, lmrsData, laneChange, initiatedLaneChange));
            // never stop before we can actually merge
            Length xMerge = getMergeDistance(perception, lat);
            if (xMerge.gt0())
            {
                Acceleration aMerge = LmrsUtil.singleAcceleration(xMerge, ownSpeed, Speed.ZERO, desire, params, sli, cfm);
                a = Acceleration.max(a, aMerge);
            }
            return a;
        }

        /** {@inheritDoc} */
        @Override
        public String toString()
        {
            return "ALIGN_GAP";
        }

    };

    private static Synchronization DEAD_END = new Synchronization()
    {
        /** {@inheritDoc} */
        @Override
        public Acceleration synchronize(final LanePerception perception, final Parameters params, final SpeedLimitInfo sli,
                final CarFollowingModel cfm, final double desire, final LateralDirectionality lat, final LmrsData lmrsData,
                final LaneChange laneChange, final LateralDirectionality initiatedLaneChange)
                throws ParameterException, OperationalPlanException
        {
            Acceleration a = Acceleration.POSITIVE_INFINITY;
            // stop for end
            Length remainingDist = Length.POSITIVE_INFINITY;
            int dn = laneChange.isChangingLane() ? -1 : 0;
            Length lcLength = laneChange.getMinimumLaneChangeDistance();
            for (LaneChangeInfo lcInfo : perception.getPerceptionCategory(InfrastructurePerception.class)
                    .getPhysicalLaneChangeInfo(RelativeLane.CURRENT))
            {
                // TODO replace this hack with something that properly accounts for overshoot this method also
                // introduces very strong deceleration at low speeds, as the time step makes bMin go from 3.4 (ignored,
                // so maybe 1.25 acceleration applied) to >10
                remainingDist = Length.min(remainingDist,
                        lcInfo.remainingDistance().minus(lcLength.times(lcInfo.numberOfLaneChanges() + dn)));
            }
            Speed speed = perception.getPerceptionCategory(EgoPerception.class).getSpeed();
            Acceleration bCrit = params.getParameter(ParameterTypes.BCRIT);
            remainingDist = remainingDist.minus(params.getParameter(ParameterTypes.S0));
            if (remainingDist.le0())
            {
                if (speed.gt0())
                {
                    a = Acceleration.min(a, bCrit.neg());
                }
                else
                {
                    a = Acceleration.ONE; // prevent dead-lock
                }
            }
            else
            {
                Acceleration bMin = new Acceleration(.5 * speed.si * speed.si / remainingDist.si, AccelerationUnit.SI);
                if (bMin.ge(bCrit))
                {
                    a = Acceleration.min(a, bMin.neg());
                }
            }
            return a;
        }

        /** {@inheritDoc} */
        @Override
        public String toString()
        {
            return "DEADEND";
        }
    };

    /**
     * Sets value for T depending on level of lane change desire.
     * @param params Parameters; parameters
     * @param desire double; lane change desire
     * @throws ParameterException if T, TMIN or TMAX is not in the parameters
     */
    private static void setDesiredHeadway(final Parameters params, final double desire) throws ParameterException
    {
        double limitedDesire = desire < 0 ? 0 : desire > 1 ? 1 : desire;
        double tDes = limitedDesire * params.getParameter(ParameterTypes.TMIN).si
                + (1 - limitedDesire) * params.getParameter(ParameterTypes.TMAX).si;
        double t = params.getParameter(ParameterTypes.T).si;
        params.setParameterResettable(ParameterTypes.T, Duration.instantiateSI(tDes < t ? tDes : t));
    }

    /**
     * Return the GTU types.
     * @return GTU types (safe copy)
     */
    public List<GtuType> getGtuTypes()
    {
        return new ArrayList<>(this.gtuTypes);
    }

    /**
     * Returns the mapped OD names.
     * @return mapped OD names (safe copy, OTS names are the keys, Fosim numbers the fields)
     */
    public Map<String, Integer> getOdNameMappings()
    {
        return new LinkedHashMap<>(this.odNumbers);
    }

}
