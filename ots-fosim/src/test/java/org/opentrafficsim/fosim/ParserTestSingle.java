package org.opentrafficsim.fosim;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.naming.NamingException;

import org.djunits.value.vdouble.scalar.Duration;
import org.djunits.value.vdouble.scalar.Time;
import org.junit.jupiter.api.Test;
import org.opentrafficsim.core.dsol.AbstractOtsModel;
import org.opentrafficsim.core.dsol.OtsSimulator;
import org.opentrafficsim.core.network.Network;
import org.opentrafficsim.core.network.NetworkException;
import org.opentrafficsim.core.perception.HistoryManagerDevs;
import org.opentrafficsim.fosim.parameters.ParametersJsonTest;
import org.opentrafficsim.fosim.parser.FosParser;
import org.opentrafficsim.fosim.parser.ParserSetting;
import org.opentrafficsim.road.network.RoadNetwork;

import nl.tudelft.simulation.dsol.SimRuntimeException;

/**
 * Tests for the parser.
 * <p>
 * Copyright (c) 2023-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
 */
public class ParserTestSingle
{
    
    /**
     * Test parsing of a file.
     * @throws SimRuntimeException simulation exception
     * @throws NamingException naming exception
     * @throws InvalidPathException path exception
     * @throws IOException input-output exception
     * @throws NetworkException network exception
     */
    @Test
    public void testParser() throws SimRuntimeException, NamingException, InvalidPathException, IOException, NetworkException
    {
        OtsSimulator simulator = new OtsSimulator("FOSIM parser test");
        RoadNetwork network = new RoadNetwork("FOSIM parser test", simulator);
        simulator.initialize(Time.ZERO, Duration.ZERO, Duration.instantiateSI(3600.0), new AbstractOtsModel(simulator)
        {
            private static final long serialVersionUID = 1L;

            /** {@inheritDoc} */
            @Override
            public Network getNetwork()
            {
                return network;
            }

            /** {@inheritDoc} */
            @Override
            public void constructModel() throws SimRuntimeException
            {
                //
            }
        }, HistoryManagerDevs.noHistory(simulator));
        Map<ParserSetting, Boolean> parserSettings = new LinkedHashMap<>();
        parserSettings.put(ParserSetting.STRIPED_AREAS, false);

        String fileName = "Terbregseplein_6.5_aangepast.fos";
        FosParser parser = new FosParser();
        parser.setSettings(parserSettings).parseFromStream(ParametersJsonTest.class.getResourceAsStream("/" + fileName));
        // OtsSimulationApplication<FosimModel> app = parser.getApplication();
    }

}
