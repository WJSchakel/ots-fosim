package org.opentrafficsim.fosim;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.DoubleFunction;

import javax.naming.NamingException;

import org.djunits.unit.FrequencyUnit;
import org.djunits.value.vdouble.scalar.Direction;
import org.djunits.value.vdouble.scalar.Duration;
import org.djunits.value.vdouble.scalar.Frequency;
import org.djunits.value.vdouble.scalar.Length;
import org.djunits.value.vdouble.scalar.Speed;
import org.djunits.value.vdouble.scalar.Time;
import org.djutils.exceptions.Throw;
import org.djutils.exceptions.Try;
import org.opentrafficsim.core.dsol.AbstractOTSModel;
import org.opentrafficsim.core.dsol.OTSSimulator;
import org.opentrafficsim.core.geometry.OTSLine3D;
import org.opentrafficsim.core.geometry.OTSPoint3D;
import org.opentrafficsim.core.network.LinkType;
import org.opentrafficsim.core.network.LinkType.DEFAULTS;
import org.opentrafficsim.core.network.NetworkException;
import org.opentrafficsim.core.network.OTSNetwork;
import org.opentrafficsim.road.network.OTSRoadNetwork;
import org.opentrafficsim.road.network.lane.CrossSectionLink;
import org.opentrafficsim.road.network.lane.Lane;
import org.opentrafficsim.road.network.lane.LaneType;
import org.opentrafficsim.road.network.lane.OTSRoadNode;
import org.opentrafficsim.road.network.lane.changing.LaneKeepingPolicy;

import nl.tudelft.simulation.dsol.SimRuntimeException;

public class FosParser
{

    /** Network. */
    private final OTSRoadNetwork network;

    /** Parser settings. */
    private final Map<ParserSetting, Boolean> parserSettings;

    // parsed info

    /** Version. */
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
    // TODO: we do not support an irregular first interval, I think
    private List<Integer> detectorTimes = new ArrayList<>();

    /** Detector positions. */
    private List<Length> detectorPositions = new ArrayList<>();

    /** Number of vehicle types. */
    private int vehicleTypes;

    /** General parameters. */
    private FosList<FosParameter> generalParameter = new FosList<>();

    /** Specific parameters per vehicle type. */
    private FosList<FosList<FosParameter>> specificParameter = new FosList<>();

    /** List of flows, index same as source index. */
    private FosList<FosFlow> flow = new FosList<>();

    /** Vehicle probabilities. */
    private FosList<List<Double>> vehicleProbabilities = new FosList<>();

    /** Source to sink. */
    private FosList<FosList<List<Double>>> sourceToSink = new FosList<>();

    /** Switched areas. */
    private FosList<FosSwitchedArea> switchedAreaTimes = new FosList<>();

    /** Temporary blockage. */
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

    /** Links per number. */
    private Map<Integer, FosLink> links = new LinkedHashMap<>();

    /** Number of last mapped node. */
    private int lastMappedNode = 1;

    /** Nodes per number. */
    private Set<FosNode> nodes = new LinkedHashSet<>();

    /** Forbidden node names, i.e. reserved for sources and sinks. */
    private LinkedHashSet<String> forbiddenNodeNames;

    /**
     * Constructor. For parser settings that are not specified the default value is used.
     * @param network OTSRoadNetwork; network.
     * @param parserSettings Map&lt;ParserSettings, Boolean&gt;; parse settings. Missing settings are assumed default.
     */
    private FosParser(final OTSRoadNetwork network, final Map<ParserSetting, Boolean> parserSettings)
    {
        this.network = Throw.whenNull(network, "Network may not be null.");
        this.parserSettings = parserSettings;
    }

    /**
     * Parses a .fos file. All parser settings are default.
     * @param network OTSRoadNetwork; network to build the fos information in.
     * @param file String; location of a .pos file.
     * @throws InvalidPathException if the path is invalid.
     * @throws IOException if the file could not be read.
     * @throws NetworkException if anything fails critically during parsing.
     */
    public static void parseFromFile(final OTSRoadNetwork network, final String file)
            throws InvalidPathException, IOException, NetworkException
    {
        parseFromString(network, new EnumMap<>(ParserSetting.class), Files.readString(Path.of(file)));
    }

    /**
     * Parses a .fos file.
     * @param network OTSRoadNetwork; network to build the fos information in.
     * @param parserSettings Map&lt;ParserSettings, Boolean&gt;; parse settings. Missing settings are assumed default.
     * @param file String; location of a .pos file.
     * @throws InvalidPathException if the path is invalid.
     * @throws IOException if the file could not be read.
     * @throws NetworkException if anything fails critically during parsing.
     */
    public static void parseFromFile(final OTSRoadNetwork network, final Map<ParserSetting, Boolean> parserSettings,
            final String file) throws InvalidPathException, IOException, NetworkException
    {
        parseFromString(network, parserSettings, Files.readString(Path.of(file)));
    }

    /**
     * Parses a string of the contents typically in a .fos file. All parser settings are default.
     * @param network OTSRoadNetwork; network to build the fos information in.
     * @param fosString String; string of the contents typically in a .fos file.
     * @throws NetworkException if anything fails critically during parsing.
     */
    public static void parseFromString(final OTSRoadNetwork network, final String fosString) throws NetworkException
    {
        parseFromString(network, new EnumMap<>(ParserSetting.class), fosString);
    }

    /**
     * Parses a string of the contents typically in a .fos file.
     * @param network OTSRoadNetwork; network to build the fos information in.
     * @param parserSettings Map&lt;ParserSettings, Boolean&gt;; parse settings. Missing settings are assumed default.
     * @param fosString String; string of the contents typically in a .fos file.
     * @throws NetworkException if anything fails critically during parsing.
     */
    public static void parseFromString(final OTSRoadNetwork network, final Map<ParserSetting, Boolean> parserSettings,
            final String fosString) throws NetworkException
    {
        FosParser parser = new FosParser(network, parserSettings);
        new BufferedReader(new StringReader(fosString)).lines().forEach(parser::parseLine);
        parser.build();
    }

    /**
     * Parses a line. This method should treat all missing parser settings as true, by using
     * {@code Map.getOrDefault(ParserSettings, true)}.
     * @param line String; line to parse.
     */
    private void parseLine(final String line)
    {
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
            parseValueList(this.sections, fieldValue(line), v -> Length.instantiateSI(v));
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
            parseValueList(this.detectorTimes, fieldValue(line), v -> (int) v);
            return;
        }
        if (line.startsWith("detector positions"))
        {
            if (getSetting(ParserSetting.DETECTORS))
            {
                parseValueList(this.detectorPositions, fieldValue(line), v -> Length.instantiateSI(v));
            }
            return;
        }
        if (line.startsWith("vehicle types"))
        {
            this.vehicleTypes = Integer.parseInt(fieldValue(line));
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
            getSubList(this.specificParameter, indices[0]).set(indices[1], new FosParameter(fieldValue(line)));
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
            this.vehicleProbabilities.set(fieldIndex(line), parseValueList(new ArrayList<>(), fieldValue(line), v -> v));
            return;
        }
        if (line.startsWith("source to sink"))
        {
            int[] indices = fieldIndices(line);
            getSubList(this.sourceToSink, indices[0]).set(indices[1],
                    parseValueList(new ArrayList<>(), fieldValue(line), v -> v));
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
            return;
        }
        if (line.startsWith("maximum simulation time"))
        {
            this.maximumSimulationTime = Integer.parseInt(fieldValue(line));
            return;
        }
        if (line.startsWith("end of file"))
        {
            this.endOfFile = true;
            return;
        }
        // all supported line variants did not take the line
        throw new RuntimeException("Unable to parse line: " + line);
    }

    /**
     * Returns a sub-list from a list. The list is created if required.
     * @param list FosList&lt;FosList&lt;T&gt;&gt;; list to take the sub-list from, or to put a create one in.
     * @param index int; index of the list to obtain.
     * @return FosList&lt;FosList&lt;T&gt;&gt;; list to take a list from.
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
     * @param setting ParserSetting; parser setting.
     * @return boolean; value for the setting.
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
        
        // build generators

        printMappings(); // TODO remove test code

        double q = 8;

        // TODO
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
        for (int vehicleType = 0; vehicleType < this.vehicleTypes; vehicleType++)
        {
            Throw.when(vehicleType >= this.specificParameter.size() || this.specificParameter.get(vehicleType) == null,
                    NetworkException.class, "No parameters for vehicle type " + vehicleType);
        }

        // vehicle probabilities
        for (int sourceIndex = 0; sourceIndex < this.vehicleProbabilities.size(); sourceIndex++)
        {
            Throw.when(sourceIndex >= this.source.size(), NetworkException.class,
                    "Source " + sourceIndex + " as specified for vehicle probabilities does not exist.");
            Throw.when(this.vehicleProbabilities.get(sourceIndex).size() != this.vehicleTypes, NetworkException.class,
                    "Wrong number of vehicle probabilities for source " + sourceIndex + ".");
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
                Throw.when(this.sourceToSink.get(sourceIndex).get(sinkIndex).size() != this.vehicleTypes,
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
     * Maps out a single link in the {@code mappedLinks} grid. No link is mapped if the specified section and lane are not a
     * valid traffic lane. In either case the returned index is where the next link may be found, and should be used for the
     * next call to this method (if it is in the lane bounds).
     * @param sectionIndex int; section index.
     * @param fromLane int; from lane index (i.e. the left-most lane of the potential link).
     * @return int; lane index at which the next left-most lane of the next link on the same section may be found.
     */
    private int mapOutLink(final int sectionIndex, final int fromLane)
    {
        // lane is grass, striped area to ignore, or beyond striped area; skip
        if (this.lane.get(fromLane).get(sectionIndex).type.equals("u")
                || (this.lane.get(fromLane).get(sectionIndex).type.equals("R") && !getSetting(ParserSetting.STRIPED_AREAS))
                || (this.lane.get(fromLane).get(sectionIndex).type.equals("L") && !getSetting(ParserSetting.STRIPED_AREAS))
                || this.lane.get(fromLane).get(sectionIndex).type.equals("X"))
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
        while (toLane < this.lane.size() - 1 && (this.lane.get(toLane).get(sectionIndex).canChangeRight()
                || this.lane.get(toLane + 1).get(sectionIndex).canChangeLeft()))
        {
            toLane++;
            this.mappedLinks[toLane][sectionIndex] = this.lastMappedLink;
        }

        // remember information, the FosLink constructor increases the lastMappedLink index
        this.links.put(this.lastMappedLink, new FosLink(sectionIndex, fromLane, toLane));

        // return possible next from-lane
        return toLane + 1;
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
            FosLink link = source.getConnectedLink();
            FosNode sourceNode = new FosNode();
            sourceNode.name = source.name;
            sourceNode.outLinks.add(link);
            link.fromNode = sourceNode;
            this.nodes.add(sourceNode);
            sourceNames.add(sourceNode.getName());
        }

        // sinks
        Set<String> sinkNames = new LinkedHashSet<>();
        for (FosSourceSink sink : this.sink)
        {
            FosLink link = sink.getConnectedLink();
            FosNode sinkNode = new FosNode();
            sinkNode.name = sink.name;
            // sources and sinks can have the same name, in that case create a new node
            while (sourceNames.contains(sinkNode.getName()) || sinkNames.contains(sinkNode.getName()))
            {
                /**
                 * Why we also check for sinkNames.contains(sinkNode.getName()), example:<br>
                 * 1. We have a source named "A20".<br>
                 * 2. We have a sink named "AZ" (e.g. the soccer stadium).<br>
                 * 3. We have another sink generated with a number that gives default name "AY", but name it "A20".<br>
                 * 4. That name is already taken by a source, so we create a new node for the sink.<br>
                 * 5. That new node has a number that gives default name "AZ" (i.e. "AY" + 1).<br>
                 * 6. A sink "AZ" was already there, so we need to create a new node again.<br>
                 * Note that a source could also have a name like "AZ".<br>
                 * Note also that we can't use sinkNode.hasForbiddenName(), as that checks all desired source and sink names,
                 * including the one we are trying to create, and hence then sinks would never be allowed to have their own
                 * actual name.
                 */
                sinkNode = new FosNode();
            }
            sinkNode.inLinks.add(link);
            link.toNode = sinkNode;
            this.nodes.add(sinkNode);
            sinkNames.add(sinkNode.getName());
        }
    }

    /**
     * Tries to find a node as it either flows towards {@code toLink} or from {@code fromLink}. In such cases links must share a
     * node due to a merge or diverge. If no such node exists, a new node is created.
     * @param fromLink int; from link, flowing in to the node.
     * @param toLink int; to link, flowing out of the node.
     * @return FosNode; node to connect both links.
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
        FosNode newNode = new FosNode();
        // check we are not using a forbidden name (same as source or sink)
        while (newNode.hasForbiddenName())
        {
            newNode = new FosNode(); // constructor increases the lastMappedNode counter
        }
        this.nodes.add(newNode);
        return newNode;
    }

    /**
     * Prints debugging information.
     */
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
     * @param node FosNode; node.
     * @throws NetworkException
     */
    private void buildNode(final FosNode node) throws NetworkException
    {
        int deltaSection;
        Set<FosLink> links;
        if (node.outLinks.isEmpty())
        {
            deltaSection = 0;
            links = node.inLinks;
        }
        else
        {
            deltaSection = -1;
            links = node.outLinks;
        }

        int sectionIndex = links.iterator().next().sectionIndex + deltaSection;
        Length x = sectionIndex == -1 ? Length.ZERO : this.sections.get(sectionIndex);
        Length y = Length.POSITIVE_INFINITY;
        for (FosLink link : links)
        {
            y = Length.min(y, link.getLeftLinkEdge());
        }
        new OTSRoadNode(this.network, node.getName(), new OTSPoint3D(x.si, y.si, 0.0), Direction.ZERO);
    }

    private void buildLink(final FosLink link) throws NetworkException
    {
        // create the link
        String name = String.format("Link %d", link.number);
        OTSRoadNode startNode = (OTSRoadNode) this.network.getNode(link.fromNode.getName());
        OTSRoadNode endNode = (OTSRoadNode) this.network.getNode(link.toNode.getName());
        LinkType linkType = network.getLinkType(DEFAULTS.FREEWAY);
        // TODO: Use Bezier if any of the lanes makes a shift?
        OTSLine3D designLine = Try.assign(() -> new OTSLine3D(startNode.getPoint(), endNode.getPoint()), NetworkException.class,
                "Design line could not be generated for link at lane %s, section %s.", link.fromLane, link.sectionIndex);
        CrossSectionLink otsLink =
                new CrossSectionLink(this.network, name, startNode, endNode, linkType, designLine, LaneKeepingPolicy.KEEPRIGHT);

        // TODO: we must do this all relative to the y-coordinates of the nodes, as these may be anywhere in case of multiple
        // links
        // we should maybe work with absolute y-values, and subtract the node value

        // calculate offsets
        List<Length> lateralOffsetAtStarts = new ArrayList<>();
        List<Length> lateralOffsetAtEnds = new ArrayList<>();
        // initialize relative to nodes
        Length leftEdgeOffsetStart = link.getLeftLinkEdge().minus(Length.instantiateSI(startNode.getPoint().y));
        Length leftEdgeOffsetEnd = link.getLeftLinkEdge().minus(Length.instantiateSI(endNode.getPoint().y));
        int offsetEnd = 0; // to detect change in the number of lanes a lane shifts, relative to left-hand lanes
        for (int i = 0; i < link.lanes.size(); i++)
        {
            if (link.lanes.get(i).taper.equals("\\"))
            {
                // for the lane adjacent to a diverge taper, re-use the same origin point as the previous lane
                Throw.when(lateralOffsetAtStarts.isEmpty(), NetworkException.class,
                        "Lane adjacent to diverge taper (i.e. \\) is not to the right of another lane in the same link.");
                lateralOffsetAtStarts.add(lateralOffsetAtStarts.get(lateralOffsetAtStarts.size() - 1));
            }
            else
            {
                // otherwise, add halve the lane width to the left edge, and increment the left edge for the next lane
                lateralOffsetAtStarts.add(leftEdgeOffsetStart.plus(link.lanes.get(i).laneWidth.times(0.5)));
                leftEdgeOffsetStart = leftEdgeOffsetStart.plus(link.lanes.get(i).laneWidth);
            }

            int currentOffsetEnd = link.lanes.get(i).laneOut - (link.fromLane + i); // in # of lanes
            if (currentOffsetEnd == offsetEnd)
            {
                // same offset as previous lane, build from there, and increment the left edge for the next lane
                lateralOffsetAtEnds.add(leftEdgeOffsetEnd.plus(link.lanes.get(i).laneWidth.times(0.5)));
                leftEdgeOffsetEnd = leftEdgeOffsetEnd.plus(link.lanes.get(i).laneWidth);
            }
            else if (currentOffsetEnd > offsetEnd || i == 0)
            {
                // a shift to the right; this must be the first lane
                // (note that for a lane adjacent to a diverge taper, this affects the start offset, not the end offset)

                // or...

                // a shift to the left, on the first lane

                // margin between actual left edge and left edge in grid assuming maximum lane widths
                Length leftEdgeMargin = link.getLeftLinkEdge().minus(getLeftEdgeMax(link.fromLane));

                // add this margin to the left edge assuming maximum lane widths on the output lane
                Length actualLeftEdge = getLeftEdgeMax(link.lanes.get(i).laneOut).plus(leftEdgeMargin);
                leftEdgeOffsetEnd = actualLeftEdge.minus(Length.instantiateSI(endNode.getPoint().y)); // relative to node

                // build from that left edge onwards
                lateralOffsetAtEnds.add(leftEdgeOffsetEnd.plus(link.lanes.get(i).laneWidth.times(0.5)));
                leftEdgeOffsetEnd = leftEdgeOffsetEnd.plus(link.lanes.get(i).laneWidth);
            }
            else
            {
                // a shift to the left, relative to the previous lane
                int stepsBack = offsetEnd - currentOffsetEnd;
                Throw.when(stepsBack != 1, NetworkException.class,
                        "Lane makes a shift to the left, relative to its left lane, by some other number than 1."
                                + " I.e. it does not merge with its left lane.");

                // copy last value, i.e. of the left lane
                lateralOffsetAtEnds.add(lateralOffsetAtEnds.get(lateralOffsetAtEnds.size() - 1));
            }
            offsetEnd = currentOffsetEnd;

        }

        // build the lanes
        LaneType laneType = this.network.getLaneType(LaneType.DEFAULTS.HIGHWAY);
        int laneNum = link.fromLane;
        for (FosLane lane : link.lanes)
        {
            String id = String.format("Lane %d", laneNum + 1);
            // calculate offset as y-distance between start/end node and from/to point, and add halve the lane width
            Length lateralOffsetAtStart = lateralOffsetAtStarts.get(laneNum);
            Length lateralOffsetAtEnd = lateralOffsetAtEnds.get(laneNum);
            Lane otsLane = Try.assign(
                    () -> new Lane(otsLink, id, lateralOffsetAtStart, lateralOffsetAtEnd, lane.laneWidth, lane.laneWidth,
                            laneType, lane.speedLimit),
                    NetworkException.class, "Geometry failed for lane %s at section %s.", laneNum,
                    link.sectionIndex);
            laneNum++;
        }

        // Bezier.cubic(from.getLocation(), to.getLocation());

        // TODO

    }

    /**
     * Returns the left-edge coordinate of a lane, assuming maximum width of all lanes to the left.
     * @param lane int; lane index.
     * @return Length; left-edge coordinate of a lane, assuming maximum width of all lanes to the left.
     */
    private Length getLeftEdgeMax(final int lane)
    {
        Length leftEdgeMax = Length.ZERO;
        for (int laneIndex = 0; laneIndex < lane; laneIndex++)
        {
            leftEdgeMax = leftEdgeMax.plus(this.maxLaneWidth[laneIndex]);
        }
        return leftEdgeMax;
    }

    // TODO: test parsing, and remove this
    public static void main(final String... args)
            throws InvalidPathException, IOException, NetworkException, SimRuntimeException, NamingException
    {

        OTSSimulator simulator = new OTSSimulator("FOSIM parser test");
        OTSRoadNetwork network = new OTSRoadNetwork("FOSIM parser test", true, simulator);
        simulator.initialize(Time.ZERO, Duration.ZERO, Duration.instantiateSI(3600.0), new AbstractOTSModel(simulator)
        {
            private static final long serialVersionUID = 1L;

            @Override
            public OTSNetwork getNetwork()
            {
                return network;
            }

            @Override
            public void constructModel() throws SimRuntimeException
            {
                //
            }

            @Override
            public Serializable getSourceId()
            {
                return "ParserTest";
            }
        });
        Map<ParserSetting, Boolean> parserSettings = new LinkedHashMap<>();
        parserSettings.put(ParserSetting.STRIPED_AREAS, false);
        // Format_test Terbregseplein_6.5_aangepast
        parseFromFile(network, parserSettings, "C:\\TUDelft\\2020\\Projects\\2022_FOSIM_OTS\\Fosim_files\\Format_test.fos");

    }

    /**
     * Returns the part of a line after the ":", with leading and trailing white spaces trimmed.
     * @param line String; line.
     * @return String; part of a line after the ":", with leading and trailing white spaces trimmed.
     */
    private static String fieldValue(final String line)
    {
        return line.substring(line.indexOf(":") + 1).trim();
    }

    /**
     * Returns the index of the field described in the line. This is "0" in the line "lane 0: ...".
     * @param line String; line.
     * @return int; index of the field described in the line.
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
     * @param line String; line.
     * @return int[]; indices of the field described in the line.
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
     * @param list List&lt;T&gt;; list to place the parsed values in.
     * @param string String; blank-separated list of integers.
     * @param converter DoubleFunction&lt;? extends T&gt;; value converter.
     * @param <T> type to convert the integers in to.
     * @return List&lt;T&gt;; the input list.
     */
    private static <T> List<T> parseValueList(final List<T> list, final String string,
            final DoubleFunction<? extends T> converter)
    {
        for (String numString : splitStringByBlank(string, 0))
        {
            list.add(converter.apply(Double.parseDouble(numString)));
        }
        return list;
    }

    /**
     * Returns the next part of a line that describes a single section of a lane.
     * @param line String; line.
     * @param from int; index to start, should be incremented with the length of each returned lane string.
     * @return String; next part of a line that describes a single section of a lane.
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
     * @param string String; string to split.
     * @param delimiter String; delimiter.
     * @return String[]; trimmed fields.
     */
    private static String[] splitAndTrimString(final String string, final String delimiter)
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
     * @param string String; string.
     * @param numFields Number of fields to return. Use 0 for as many as required.
     * @return String[]; split strings.
     */
    private static String[] splitStringByBlank(final String string, final int numFields)
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

    /**
     * This list wrapper allows multiple threads to set data in the array using the {@code set(index, element)} method. This
     * method also assures the array will be of sufficient size, filling it with {@code null} as required. Using this, multiple
     * threads can fill the array in parallel.
     * @author wjschakel
     * @param <T> element type.
     */
    private static class FosList<T> implements Iterable<T>
    {
        /** Wrapped list. */
        private final List<T> list = new ArrayList<>();

        /**
         * Set the element at the specified index. Increase size of array if required. This will fill the array with
         * {@code null}.
         * @param index int; index to set the element.
         * @param t T; element to set.
         */
        public synchronized T set(final int index, final T t)
        {
            while (index >= size())
            {
                this.list.add(null);
            }
            return this.list.set(index, t);
        }

        /**
         * Returns the size;
         * @return int; size.
         */
        public int size()
        {
            return this.list.size();
        }

        /**
         * Returns the value at index.
         * @param index int; index.
         * @return T; value at index.
         */
        public T get(final int index)
        {
            return this.list.get(index);
        }

        /** {@inhertitDoc} */
        public Iterator<T> iterator()
        {
            return this.list.iterator();
        }

        /**
         * Returns whether the list is fully defined, i.e. contains no {@code null}.
         * @return boolean; whether the list is fully defined.
         */
        public boolean isDefined()
        {
            return !this.list.contains(null);
        }

        /** {@inheritDoc} */
        @Override
        public String toString()
        {
            return this.list.toString();
        }
    }

    /**
     * Parsed lane info. The interpretation if this has been reverse-engineered by setting various properties in a .fos file.
     * @author wjschakel
     */
    private class FosLane
    {
        /**
         * Type:
         * <ul>
         * <li>u: unused</li>
         * <li>l: can only change left</li>
         * <li>r: can only change right</li>
         * <li>c: can change in both directions</li>
         * <li>s: single lane (no lane change in either direction</li>
         * <li>L: must go left (striped area)</li>
         * <li>R: must go right (striped area)</li>
         * <li>X: should not be here (beyond striped area)</li>
         * </ul>
         */
        public final String type;

        /**
         * Taper:
         * <ul>
         * <li>- no</li>
         * <li>&gt; merge taper</li>
         * <li>&lt; diverge taper</li>
         * <li>/ merge taper adjacent</li>
         * <li>\ diverge taper adjacent</li>
         */
        public final String taper;

        /**
         * Lane number out; for diagonal sections. For both a merge taper and its adjacent lane, this is the same lane as the
         * merge taper. For diverge tapers, these values are not affected.
         */
        public final int laneOut;

        /** No overtaking trucks. */
        public final boolean noOvertakingTrucks;

        /** Speed suppression. */
        public final double speedSuppression;

        /** Speed limit. */
        public final Speed speedLimit;

        /** Slope (unit unknown, not used in FOSIM). */
        public final double slope;

        /** All lane change required (function unknown, not used in FOSIM). */
        public final boolean allLaneChangeRequired;

        /** Lane width. */
        public final Length laneWidth;

        /** Road works. */
        public final boolean roadWorks;

        /** Trajectory control. */
        public final boolean trajectoryControl;

        /**
         * Switched lane.
         * <ul>
         * <li>0: none</li>
         * <li>&lt;0: rush-hour lane, must change left</li>
         * <li>&gt;0: plus lane, must change left</li>
         * <li>abs value: index of area times</li>
         * </ul>
         * area time).
         */
        private final int switchedLane;

        /**
         * Parses the information of a single lane in a single section.
         * @param string String; string describing a single lane in a single section.
         */
        public FosLane(final String string)
        {
            // example string: r,-, 0, 0,1.0,100.0,0.000,0,3.50,0,0[,-1]
            // documented comma-separated fields do not match actual fields, nor in type, nor in number
            String[] fields = splitAndTrimString(string, ",");
            this.type = fields[0];
            this.taper = fields[1];
            this.laneOut = Integer.parseInt(fields[2]);
            this.noOvertakingTrucks = fields[3].equals("1"); // "0" otherwise
            this.speedSuppression = Double.parseDouble(fields[4]);
            this.speedLimit = Speed.of(Double.parseDouble(fields[5]), "km/h");
            this.slope = Double.parseDouble(fields[6]);
            this.allLaneChangeRequired = fields[7].equals("1"); // "0" otherwise
            this.laneWidth = Length.instantiateSI(Double.parseDouble(fields[8]));
            this.roadWorks = fields[9].equals("1"); // "0" otherwise
            this.trajectoryControl = fields[10].equals("1"); // "0" otherwise
            this.switchedLane = fields.length > 11 ? Integer.parseInt(fields[11]) : 0;
        }

        /**
         * Whether vehicles may change left.
         * @return boolean; whether vehicles may change left.
         */
        public boolean canChangeLeft()
        {
            return (this.type.equals("l") || this.type.equals("c")
                    || (this.type.equals("L") && getSetting(ParserSetting.STRIPED_AREAS)));
        }

        /**
         * Whether vehicles may change right.
         * @return boolean; whether vehicles may change right.
         */
        public boolean canChangeRight()
        {
            return (this.type.equals("r") || this.type.equals("c")
                    || (this.type.equals("R") && getSetting(ParserSetting.STRIPED_AREAS)));
        }

        /**
         * Whether this lane is switched (rush-hour lane or plus lane).
         * @return boolean; whether this lane is switched (rush-hour lane or plus lane).
         */
        public boolean isSwitched()
        {
            return this.switchedLane != 0;
        }

        /**
         * Returns the number of the switched are times for this lane, if it is switched.
         * @return int; number of the switched are times for this lane.
         */
        public int switchedAreaTimesNumber()
        {
            Throw.when(!isSwitched(), RuntimeException.class, "Requesting switch times of lane that is not switched.");
            return Math.abs(this.switchedLane);
        }

        /** {@inheritDoc} */
        @Override
        public String toString()
        {
            return "FosLane " + type + taper + ", " + speedLimit;
        }
    }

    /**
     * Parsed source or sink info (this is the same info).
     * @author wjschakel
     */
    private class FosSourceSink
    {
        /** Section index, counting from the end. */
        public final int sectionFromEnd;

        /** From lane index. */
        public final int fromLane;

        /** To lane index. */
        public final int toLane;

        /** Name. */
        public final String name;

        /**
         * Parses a single source or sink.
         * @param string String; value of a source or sink line.
         */
        public FosSourceSink(final String string)
        {
            String[] fields = splitStringByBlank(string, 4);
            this.sectionFromEnd = Integer.parseInt(fields[0]);
            this.fromLane = Integer.parseInt(fields[1]);
            this.toLane = Integer.parseInt(fields[2]);
            this.name = fields[3];
        }

        /**
         * Returns the link connected to the source or sink.
         * @return FosLink; link connected to the source or sink.
         */
        public FosLink getConnectedLink()
        {
            int sectionIndexFromStart = FosParser.this.sections.size() - this.sectionFromEnd - 1;
            return FosParser.this.links.get(FosParser.this.mappedLinks[this.fromLane][sectionIndexFromStart]);
        }

        /** {@inheritDoc} */
        @Override
        public String toString()
        {
            return "FosSourceSink [name=" + name + ", section=" + sectionFromEnd + ", fromLane=" + fromLane + ", toLane="
                    + toLane + "]";
        }
    }

    /**
     * Parsed traffic light info.
     * @author wjschakel
     */
    private class FosTrafficLight
    {
        /** Controller (function unknown, not used in FOSIM). */
        public final String controller;

        /** Position. */
        public final Length position;

        /** Lane. */
        public final int lane;

        /** Number (function unknown, not used in FOSIM). */
        public final int number;

        /** Cycle time. */
        public final Duration cycleTime;

        /** Green time. */
        public final Duration greenTime;

        /** Yellow time. */
        public final Duration yellowTime;

        /** Start offset. */
        public final Duration startOffset;

        /**
         * Parses a single traffic light.
         * @param string String; value of a traffic light line.
         */
        public FosTrafficLight(final String string)
        {
            String[] fields = splitStringByBlank(string, 8);
            this.controller = fields[0];
            this.position = Length.instantiateSI(Double.parseDouble(fields[1]));
            this.lane = Integer.parseInt(fields[2]);
            this.number = Integer.parseInt(fields[3]);
            this.cycleTime = Duration.instantiateSI(Double.parseDouble(fields[4]));
            this.greenTime = Duration.instantiateSI(Double.parseDouble(fields[5]));
            this.yellowTime = Duration.instantiateSI(Double.parseDouble(fields[6]));
            this.startOffset = Duration.instantiateSI(Double.parseDouble(fields[7]));
        }

        /** {@inheritDoc} */
        @Override
        public String toString()
        {
            return "FosTrafficLight [position=" + position + ", lane=" + lane + ", cycleTime=" + cycleTime + ", greenTime="
                    + greenTime + ", yellowTime=" + yellowTime + ", startOffset=" + startOffset + "]";
        }
    }

    /**
     * Parsed parameter info.
     * @author wjschakel
     */
    private class FosParameter
    {
        /** Value. */
        public final double value;

        /** Name. */
        public final String name;

        /**
         * Parses a single parameter.
         * @param string String; value of a parameter line.
         */
        public FosParameter(final String string)
        {
            String[] fields = splitStringByBlank(string, 2);
            this.value = Double.parseDouble(fields[0]);
            this.name = fields[1];
        }

        /** {@inheritDoc} */
        @Override
        public String toString()
        {
            return "FosParameter " + name + " = " + value + "]";
        }
    }

    /**
     * Parsed flow info.
     * @author wjschakel
     */
    private class FosFlow
    {
        /** Time. */
        public final List<Duration> time = new ArrayList<>();

        /** Flow. */
        public final List<Frequency> flow = new ArrayList<>();

        /**
         * Parses a single flow source.
         * @param string String; value of a flow line.
         */
        public FosFlow(final String string)
        {
            String[] fields = splitStringByBlank(string, 0);
            for (String subString : fields)
            {
                String[] valueStrings = splitAndTrimString(subString, "\\|"); // pipe is a meta character in regex
                this.time.add(Duration.instantiateSI(Double.parseDouble(valueStrings[0])));
                this.flow.add(new Frequency(Double.parseDouble(valueStrings[1]), FrequencyUnit.PER_HOUR));
            }
        }

        /** {@inheritDoc} */
        @Override
        public String toString()
        {
            return "FosFlow [time=" + time + ", flow=" + flow + "]";
        }
    }

    /**
     * Parsed switched area info.
     * @author wjschakel
     */
    private class FosSwitchedArea
    {
        /** Open time. */
        public final Duration openTime;

        /** Close time. */
        public final Duration closeTime;

        /** Open speed (unit unknown, not used in FOSIM). */
        public final Speed openSpeed;

        /** Close speed (unit unknown, not used in FOSIM). */
        public final Speed closeSpeed;

        /** Open intensity (unit unknown, not used in FOSIM). */
        public final Frequency openIntensity;

        /** Open intensity (unit unknown, not used in FOSIM). */
        public final Frequency closeIntensity;

        /** Open mode (function unknown, not used in FOSIM). */
        public final int openMode;

        /** Close mode (function unknown, not used in FOSIM). */
        public final int closeMode;

        /** Detector index (not used in FOSIM). */
        public final int detectorIndex;

        /**
         * Parses a single switch area.
         * @param string String; value of a switch area line.
         */
        public FosSwitchedArea(final String string)
        {
            String[] fields = splitStringByBlank(string, 9);
            this.openTime = Duration.instantiateSI(Double.parseDouble(fields[0]));
            this.closeTime = Duration.instantiateSI(Double.parseDouble(fields[1]));
            this.openSpeed = Speed.instantiateSI(Double.parseDouble(fields[2])); // unit unknown
            this.closeSpeed = Speed.instantiateSI(Double.parseDouble(fields[3])); // unit unknown
            this.openIntensity = new Frequency(Double.parseDouble(fields[4]), FrequencyUnit.PER_HOUR); // unit unknown
            this.closeIntensity = new Frequency(Double.parseDouble(fields[5]), FrequencyUnit.PER_HOUR); // unit unknown
            this.openMode = Integer.parseInt(fields[6]); // function unknown
            this.closeMode = Integer.parseInt(fields[7]); // function unknown
            this.detectorIndex = Integer.parseInt(fields[8]);
        }

        /** {@inheritDoc} */
        @Override
        public String toString()
        {
            return "FosSwitchedArea [openTime=" + openTime + ", closeTime=" + closeTime + "]";
        }
    }

    /**
     * Parsed temporary blockage info.
     * @author wjschakel
     */
    private class FosTemporaryBlockage
    {
        /** Position. */
        public final Length position;

        /** From lane index. */
        public final int fromLane;

        /** To lane index. */
        public final int toLane;

        /** From time. */
        public final Duration fromTime;

        /** To time. */
        public final Duration toTime;

        /**
         * Parses a single switch area.
         * @param string String; value of a switch area line.
         */
        public FosTemporaryBlockage(final String string)
        {
            String[] fields = splitStringByBlank(string, 5);
            this.position = Length.instantiateSI(Double.parseDouble(fields[0]));
            this.fromLane = Integer.parseInt(fields[1]);
            this.toLane = Integer.parseInt(fields[2]);
            this.fromTime = Duration.instantiateSI(Double.parseDouble(fields[3]));
            this.toTime = Duration.instantiateSI(Double.parseDouble(fields[4]));
        }

        /** {@inheritDoc} */
        @Override
        public String toString()
        {
            return "FosTemporaryBlockage [position=" + position + ", fromLane=" + fromLane + ", toLane=" + toLane
                    + ", fromTime=" + fromTime + ", toTime=" + toTime + "]";
        }
    }

    /**
     * Class for common functionality of both FOSIM links and nodes.
     * @author wjschakel
     */
    private abstract class FosElement
    {
        /** Unique number. */
        final public int number;

        /**
         * Contructor.
         * @param number int; unique number.
         */
        public FosElement(final int number)
        {
            this.number = number;
        }

        /** {@inheritDoc} */
        @Override
        public final int hashCode()
        {
            return Objects.hash(this.number);
        }

        /** {@inheritDoc} */
        @Override
        public final boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            FosElement other = (FosElement) obj;
            return this.number == other.number;
        }

        /** {@inheritDoc} */
        @Override
        public final String toString()
        {
            return getClass().getSimpleName() + " " + number;
        }
    }

    /**
     * Class with link information.
     * @author wjschakel
     */
    private class FosLink extends FosElement
    {
        /** Section index. */
        final public int sectionIndex;

        /** From-lane index. */
        final public int fromLane;

        /** To-lane index. */
        final public int toLane;

        /** From-node. */
        public FosNode fromNode;

        /** To-node. */
        public FosNode toNode;

        /** Lanes. */
        final public List<FosLane> lanes = new ArrayList<>();

        /**
         * Constructor.
         * @param sectionIndex int; section index.
         * @param fromLane int; from-lane index.
         * @param toLane int; to-lane index.
         */
        public FosLink(final int sectionIndex, final int fromLane, final int toLane)
        {
            super(FosParser.this.lastMappedLink++);
            this.sectionIndex = sectionIndex;
            this.fromLane = fromLane;
            this.toLane = toLane;
            for (int laneIndex = fromLane; laneIndex <= toLane; laneIndex++)
            {
                this.lanes.add(FosParser.this.lane.get(laneIndex).get(sectionIndex));
            }
        }

        /**
         * Returns the lateral coordinate of the left edge. The section is centered by its actual width, in the space assuming a
         * grid of maximum lane widths.
         * @return Length; lateral coordinate of the left edge for a link.
         */
        public Length getLeftLinkEdge()
        {
            // derive max lane widths, if we haven't already
            if (FosParser.this.maxLaneWidth == null)
            {
                FosParser.this.maxLaneWidth = new Length[FosParser.this.lane.size()];
                for (int laneIndex = 0; laneIndex < FosParser.this.lane.size(); laneIndex++)
                {
                    // for each lane, loop all sections and store the maximum width
                    FosParser.this.maxLaneWidth[laneIndex] = Length.ZERO;
                    for (FosLane lane : FosParser.this.lane.get(laneIndex))
                    {
                        FosParser.this.maxLaneWidth[laneIndex] =
                                Length.max(FosParser.this.maxLaneWidth[laneIndex], lane.laneWidth);
                    }
                }
            }

            // get left edge assuming all lanes are at maximum width
            Length leftEdgeMax = getLeftEdgeMax(this.fromLane);

            // get width if all lanes are at maximum width, and actual width
            Length linkWidthMax = Length.ZERO;
            Length linkWidth = Length.ZERO;
            for (int laneIndex = this.fromLane; laneIndex <= this.toLane; laneIndex++)
            {
                linkWidthMax = linkWidthMax.plus(FosParser.this.maxLaneWidth[laneIndex]);
                linkWidth = linkWidth.plus(FosParser.this.lane.get(laneIndex).get(this.sectionIndex).laneWidth);
            }

            // add halve of space to left edge
            return leftEdgeMax.plus(linkWidthMax.minus(linkWidth).times(.5));
        }
    }

    /**
     * Class with node information.
     * @author wjschakel
     */
    private class FosNode extends FosElement
    {
        /** Links in to the node. */
        public final Set<FosLink> inLinks = new LinkedHashSet<>();

        /** Links out of the node. */
        public final Set<FosLink> outLinks = new LinkedHashSet<>();

        /** Node name. */
        private String name;

        /**
         * Constructor.
         */
        public FosNode()
        {
            super(FosParser.this.lastMappedNode++);
            System.out.println("Created node " + this.number + " with name " + getName());
        }

        /**
         * Returns a name for the node. If no name is given, the number is translated as 1&gt;A, 2&gt;B, ..., 27&gt;AA,
         * 28&gt;AB, etc.
         * @return String; name of the node.
         */
        public String getName()
        {
            if (this.name == null)
            {
                int num = this.number;
                this.name = "";
                while (num > 0)
                {
                    int remainder = (num - 1) % 26;
                    num = (num - remainder) / 26;
                    this.name = (char) (remainder + 'A') + this.name;
                }
            }
            return this.name;
        }

        /**
         * Returns whether this node has a forbidden name, i.e. equal to desired name of a source or sink. In that case, a new
         * node should be generated in that case.
         * @return boolean; whether this node has a forbidden name.
         */
        public boolean hasForbiddenName()
        {
            if (FosParser.this.forbiddenNodeNames == null)
            {
                FosParser.this.forbiddenNodeNames = new LinkedHashSet<>();
                FosParser.this.source.forEach((s) -> FosParser.this.forbiddenNodeNames.add(s.name));
                FosParser.this.sink.forEach((s) -> FosParser.this.forbiddenNodeNames.add(s.name));
            }
            return FosParser.this.forbiddenNodeNames.contains(getName());
        }
    }
}