package org.opentrafficsim.fosim.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.DoubleFunction;

import javax.naming.NamingException;

import org.djunits.value.vdouble.scalar.Direction;
import org.djunits.value.vdouble.scalar.Duration;
import org.djunits.value.vdouble.scalar.Length;
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
        while (toLane < this.lane.size() - 1
                && (this.lane.get(toLane).get(sectionIndex).canChangeRight(getSetting(ParserSetting.STRIPED_AREAS))
                        || this.lane.get(toLane + 1).get(sectionIndex).canChangeLeft(getSetting(ParserSetting.STRIPED_AREAS))))
        {
            toLane++;
            this.mappedLinks[toLane][sectionIndex] = this.lastMappedLink;
        }

        // remember information, the FosLink constructor increases the lastMappedLink index
        this.links.put(this.lastMappedLink, new FosLink(this.lastMappedLink++, sectionIndex, fromLane, toLane, this));

        // return possible next from-lane
        return toLane + 1;
    }
    
    /**
     * Returns a lane.
     * @param sectionIndex int; section index.
     * @param laneIndex int; lane index.
     * @return FosLane; lane at given section and lane index.
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
            FosLink link = getSourceSinkLink(sink);
            FosNode sinkNode = new FosNode(this.lastMappedNode++);
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
                sinkNode = new FosNode(this.lastMappedNode++);
            }
            sinkNode.inLinks.add(link);
            link.toNode = sinkNode;
            this.nodes.add(sinkNode);
            sinkNames.add(sinkNode.getName());
        }
    }

    /**
     * Returns the link connected to the source or sink.
     * @param sourceSink FosSourceSink; source or sink.
     * @return FosLink; link connected to the source or sink.
     */
    private FosLink getSourceSinkLink(final FosSourceSink sourceSink)
    {
        int sectionIndexFromStart = this.sections.size() - sourceSink.sectionFromEnd - 1;
        return this.links.get(this.mappedLinks[sourceSink.fromLane][sectionIndexFromStart]);
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
     * Returns whether this node has a forbidden name, i.e. equal to desired name of a source or sink. In that case, a new
     * node should be generated in that case.
     * @param node FosNode; node.
     * @return boolean; whether this node has a forbidden name.
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
            y = Length.min(y, getLeftLinkEdge(link.sectionIndex, link.fromLane, link.toLane));
        }
        new OTSRoadNode(this.network, node.getName(), new OTSPoint3D(x.si, y.si, 0.0), Direction.ZERO);
    }

    private void buildLink(final FosLink link) throws NetworkException
    {
        // create the link
        String name = String.format("Link %d", link.number);
        OTSRoadNode startNode = (OTSRoadNode) this.network.getNode(link.fromNode.getName());
        OTSRoadNode endNode = (OTSRoadNode) this.network.getNode(link.toNode.getName());
        LinkType linkType = this.network.getLinkType(DEFAULTS.FREEWAY);
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
        Length leftEdgeOffsetStart = getLeftLinkEdge(link.sectionIndex, link.fromLane, link.toLane)
                .minus(Length.instantiateSI(startNode.getPoint().y));
        Length leftEdgeOffsetEnd = getLeftLinkEdge(link.sectionIndex, link.fromLane, link.toLane)
                .minus(Length.instantiateSI(endNode.getPoint().y));
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
                Length leftEdgeMargin =
                        getLeftLinkEdge(link.sectionIndex, link.fromLane, link.toLane).minus(getLeftEdgeMax(link.fromLane));

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
                    NetworkException.class, "Geometry failed for lane %s at section %s.", laneNum, link.sectionIndex);
            laneNum++;
        }

        // Bezier.cubic(from.getLocation(), to.getLocation());

        // TODO

    }

    /**
     * Returns the lateral coordinate of the left edge. The section is centered by its actual width, in the space assuming a
     * grid of maximum lane widths.
     * @param sectionIndex int; section index.
     * @param fromLane int; from lane.
     * @param toLane int; to lane.
     * @return Length; lateral coordinate of the left edge for a link.
     */
    private Length getLeftLinkEdge(final int sectionIndex, final int fromLane, final int toLane)
    {
        // derive max lane widths, if we haven't already
        if (this.maxLaneWidth == null)
        {
            this.maxLaneWidth = new Length[this.lane.size()];
            for (int laneIndex = 0; laneIndex < this.lane.size(); laneIndex++)
            {
                // for each lane, loop all sections and store the maximum width
                this.maxLaneWidth[laneIndex] = Length.ZERO;
                for (FosLane lane : this.lane.get(laneIndex))
                {
                    this.maxLaneWidth[laneIndex] = Length.max(this.maxLaneWidth[laneIndex], lane.laneWidth);
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
        return leftEdgeMax.plus(linkWidthMax.minus(linkWidth).times(.5));
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
     * @param string String; string.
     * @param numFields Number of fields to return. Use 0 for as many as required.
     * @return String[]; split strings.
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
}
