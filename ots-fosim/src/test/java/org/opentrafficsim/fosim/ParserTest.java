package org.opentrafficsim.fosim;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.naming.NamingException;

import org.djunits.value.vdouble.scalar.Duration;
import org.djunits.value.vdouble.scalar.Time;
import org.junit.jupiter.api.Test;
import org.opentrafficsim.core.dsol.AbstractOTSModel;
import org.opentrafficsim.core.dsol.OTSSimulator;
import org.opentrafficsim.core.network.NetworkException;
import org.opentrafficsim.core.network.OTSNetwork;
import org.opentrafficsim.fosim.parameters.ParametersJsonTest;
import org.opentrafficsim.fosim.parser.FosParser;
import org.opentrafficsim.fosim.parser.ParserSetting;
import org.opentrafficsim.road.network.OTSRoadNetwork;

import nl.tudelft.simulation.dsol.SimRuntimeException;

/**
 * Tests for the parser.
 * <p>
 * Copyright (c) 2023-2023 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://dittlab.tudelft.nl">Wouter Schakel</a>
 */
public class ParserTest
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
        OTSSimulator simulator = new OTSSimulator("FOSIM parser test");
        OTSRoadNetwork network = new OTSRoadNetwork("FOSIM parser test", true, simulator);
        simulator.initialize(Time.ZERO, Duration.ZERO, Duration.instantiateSI(3600.0), new AbstractOTSModel(simulator)
        {
            private static final long serialVersionUID = 1L;

            /** {@inheritDoc} */
            @Override
            public OTSNetwork getNetwork()
            {
                return network;
            }

            /** {@inheritDoc} */
            @Override
            public void constructModel() throws SimRuntimeException
            {
                //
            }

            /** {@inheritDoc} */
            @Override
            public Serializable getSourceId()
            {
                return "ParserTest";
            }
        });
        Map<ParserSetting, Boolean> parserSettings = new LinkedHashMap<>();
        parserSettings.put(ParserSetting.STRIPED_AREAS, false);
        
        String fileName = "Terbregseplein_6.5_aangepast.fos";
        InputStream stream = ParametersJsonTest.class.getResourceAsStream("/" + fileName);
        String fileString = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        FosParser.parseFromString(network, parserSettings, fileString);
    }
    
}
