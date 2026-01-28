package org.opentrafficsim.fosim.parser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.djunits.unit.SpeedUnit;
import org.djunits.value.vdouble.scalar.Duration;
import org.djunits.value.vdouble.scalar.Frequency;
import org.djunits.value.vdouble.scalar.Length;
import org.djunits.value.vdouble.scalar.Speed;
import org.djutils.draw.function.ContinuousPiecewiseLinearFunction;
import org.djutils.draw.line.PolyLine2d;
import org.djutils.draw.line.Polygon2d;
import org.djutils.draw.point.Point2d;
import org.opentrafficsim.base.geometry.OtsLine2d;
import org.opentrafficsim.core.definitions.DefaultsNl;
import org.opentrafficsim.core.network.NetworkException;
import org.opentrafficsim.core.network.Node;
import org.opentrafficsim.draw.graphs.GraphPath;
import org.opentrafficsim.draw.graphs.GraphPath.Section;
import org.opentrafficsim.kpi.sampling.SpaceTimeRegion;
import org.opentrafficsim.kpi.sampling.data.ExtendedDataType;
import org.opentrafficsim.kpi.sampling.filter.FilterDataType;
import org.opentrafficsim.road.definitions.DefaultsRoadNl;
import org.opentrafficsim.road.network.LaneKeepingPolicy;
import org.opentrafficsim.road.network.RoadNetwork;
import org.opentrafficsim.road.network.lane.CrossSectionGeometry;
import org.opentrafficsim.road.network.lane.CrossSectionLink;
import org.opentrafficsim.road.network.lane.Lane;
import org.opentrafficsim.road.network.sampling.GtuDataRoad;
import org.opentrafficsim.road.network.sampling.LaneDataRoad;
import org.opentrafficsim.road.network.sampling.RoadSampler;

/**
 * This class sets up a sampler for use in a FOSIM simulation.
 * @author wjschakel
 */
public class FosSampler
{

    /** Trajectory sampler. */
    private final RoadSampler sampler;

    /** Graph paths per lane. */
    private final GraphPath<LaneDataRoad>[] laneGraphPaths;

    /** Graph path for all lanes combined. */
    private final GraphPath<LaneDataRoad> allGraphPath;

    /** ID of dummy objects. */
    private int dummyId;

    /**
     * Sets up a sampler based on the network.
     * @param parser parser
     * @param network network
     * @throws NetworkException if dummy network element cannot be created
     */
    public FosSampler(final FosParser parser, RoadNetwork network, Frequency frequency) throws NetworkException
    {
        this(new LinkedHashSet<>(), new LinkedHashSet<>(), parser, network, frequency);
    }

    /**
     * Sets up a sampler based on the network.
     * @param extendedDataTypes extended data types
     * @param filterDataTypes filter data types
     * @param parser parser
     * @param network network
     * @throws NetworkException if dummy network element cannot be created
     */
    @SuppressWarnings("unchecked")
    public FosSampler(final Set<ExtendedDataType<?, ?, ?, ? super GtuDataRoad>> extendedDataTypes,
            final Set<FilterDataType<?, ? super GtuDataRoad>> filterDataTypes, final FosParser parser,
            final RoadNetwork network, final Frequency frequency) throws NetworkException
    {
        Duration endtime = Duration.ZERO.plus(network.getSimulator().getReplication().getEndTime());
        this.sampler = new RoadSampler(extendedDataTypes, filterDataTypes, network, frequency);

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
        Length[] lengths = new Length[nSections];
        LaneDataRoad[][] laneData = new LaneDataRoad[toLane + 1][nSections + 1];
        for (FosLink fosLink : parser.getLinks())
        {
            if (lengths[fosLink.sectionIndex] == null)
            {
                lengths[fosLink.sectionIndex] =
                        Length.ofSI(fosLink.lanes.get(0).getLane().getLink().getEndNode().getLocation().x
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
                            laneData[laneNum][fosLink.sectionIndex], Length.ZERO, lane.getLength(), Duration.ZERO, endtime));
                }
                laneNum++;
            }
        }

        // create graph paths, which ContourDataSource will use later to provide speed contour data
        this.laneGraphPaths = new GraphPath[toLane + 1];
        Map<Integer, List<LaneDataRoad>> lanesAllLanes = new LinkedHashMap<>(nSections);
        Speed speedLimit = new Speed(100.0, SpeedUnit.KM_PER_HOUR); // used for EGTF, which Fosim does not use
        for (int i = fromLane; i <= toLane; i++)
        {
            String pathName = "Lane " + (i + 1);
            List<Section<LaneDataRoad>> sections = new ArrayList<>();
            for (int j = 0; j < nSections; j++)
            {
                LaneDataRoad laneDataRoad = laneData[i][j] == null ? dummyLaneData(network, lengths[j]) : laneData[i][j];
                sections.add(new Section<>(laneDataRoad.getLength(), speedLimit, List.of(laneDataRoad)));
                lanesAllLanes.computeIfAbsent(j, (k) -> new ArrayList<>()).add(laneDataRoad);
            }
            this.laneGraphPaths[i] = new GraphPath<>(pathName, sections);
        }

        List<String> names = new ArrayList<>();
        List<Section<LaneDataRoad>> sectionsAllLanes = new ArrayList<>();
        for (int j = 0; j < nSections; j++)
        {
            names.add("Lane " + j);
            sectionsAllLanes.add(new Section<>(lengths[j], speedLimit, lanesAllLanes.get(j)));
        }
        this.allGraphPath = new GraphPath<>(names, sectionsAllLanes);
    }

    /**
     * Creates a dummy lane data in place of a gap within a lane (row), i.e. a grass section.
     * @param length length required
     * @return dummy lane data
     * @throws NetworkException
     */
    private LaneDataRoad dummyLaneData(final RoadNetwork network, final Length length) throws NetworkException
    {
        // 100m away laterally so no GTU can ever find its roaming position on these dummy links/lanes
        Point2d pointA = new Point2d(0.0, -100.0);
        Point2d pointB = new Point2d(length.si, -100.0);
        Node nodeA = new Node(network, "_Node " + this.dummyId++, pointA);
        Node nodeB = new Node(network, "_Node " + this.dummyId++, pointB);
        OtsLine2d line = new OtsLine2d(new PolyLine2d(pointA, pointB));
        CrossSectionLink link = new CrossSectionLink(network, "_Link " + this.dummyId++, nodeA, nodeB, DefaultsNl.FREEWAY, line,
                ContinuousPiecewiseLinearFunction.of(0.0, 0.0), LaneKeepingPolicy.KEEPRIGHT);
        ContinuousPiecewiseLinearFunction offset = ContinuousPiecewiseLinearFunction.of(0.0, 0.0, 1.0, 0.0);
        ContinuousPiecewiseLinearFunction width = ContinuousPiecewiseLinearFunction.of(0.0, 3.5, 1.0, 3.5);
        CrossSectionGeometry geometry = new CrossSectionGeometry(line, new Polygon2d(line.getPointList()), offset, width);
        Lane lane = new Lane(link, "_Lane " + this.dummyId++, geometry, DefaultsRoadNl.FREEWAY,
                Map.of(DefaultsNl.VEHICLE, new Speed(100.0, SpeedUnit.KM_PER_HOUR)));
        LaneDataRoad laneData = new LaneDataRoad(lane);
        this.sampler.registerSpaceTimeRegion(
                new SpaceTimeRegion<LaneDataRoad>(laneData, Length.ZERO, lane.getLength(), Duration.ZERO, Duration.ONE));
        return laneData;
    }

    /**
     * Get road sampler.
     * @return road sampler
     */
    public RoadSampler getSampler()
    {
        return this.sampler;
    }

    /**
     * Get graph paths per lane.
     * @return graph paths per lane
     */
    public GraphPath<LaneDataRoad>[] getLaneGraphPaths()
    {
        return this.laneGraphPaths;
    }

    /**
     * Get graph path of all lanes combined.
     * @return graph path of all lanes combined
     */
    public GraphPath<LaneDataRoad> getAllGraphPath()
    {
        return this.allGraphPath;
    }

}
