package org.opentrafficsim.fosim.sim0mq;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.djunits.unit.FrequencyUnit;
import org.djunits.unit.SpeedUnit;
import org.djunits.unit.TimeUnit;
import org.djunits.value.storage.StorageType;
import org.djunits.value.vdouble.scalar.Direction;
import org.djunits.value.vdouble.scalar.Length;
import org.djunits.value.vdouble.scalar.Speed;
import org.djunits.value.vdouble.vector.FrequencyVector;
import org.djunits.value.vdouble.vector.TimeVector;
import org.djunits.value.vdouble.vector.data.DoubleVectorData;
import org.opentrafficsim.base.parameters.ParameterException;
import org.opentrafficsim.core.definitions.Defaults;
import org.opentrafficsim.core.definitions.DefaultsNl;
import org.opentrafficsim.core.dsol.OtsSimulatorInterface;
import org.opentrafficsim.core.geometry.OtsGeometryException;
import org.opentrafficsim.core.geometry.OtsLine3d;
import org.opentrafficsim.core.geometry.OtsPoint3d;
import org.opentrafficsim.core.gtu.GtuType;
import org.opentrafficsim.core.network.NetworkException;
import org.opentrafficsim.core.network.Node;
import org.opentrafficsim.road.definitions.DefaultsRoadNl;
import org.opentrafficsim.road.gtu.generator.characteristics.DefaultLaneBasedGtuCharacteristicsGeneratorOd;
import org.opentrafficsim.road.gtu.generator.characteristics.DefaultLaneBasedGtuCharacteristicsGeneratorOd.Factory;
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

import nl.tudelft.simulation.dsol.SimRuntimeException;
import nl.tudelft.simulation.jstats.streams.StreamInterface;

/**
 * Simple technical demo of a single straight lane.
 * <p>
 * Copyright (c) 2023-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
 */
public class SingleLaneDemo extends OtsTransceiver
{

    /**
     * Constructor.
     * @param args String[]; command line arguments.
     * @throws Exception on any exception during simulation.
     */
    protected SingleLaneDemo(final String[] args) throws Exception
    {
        super(args);
    }

    /**
     * Main method.
     * @param args String[]; command line arguments.
     * @throws Exception on any exception during simulation.
     */
    public static void main(final String... args) throws Exception
    {
        new SingleLaneDemo(args);
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
    @Override
    protected RoadNetwork setupSimulation(final OtsSimulatorInterface sim)
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

}
