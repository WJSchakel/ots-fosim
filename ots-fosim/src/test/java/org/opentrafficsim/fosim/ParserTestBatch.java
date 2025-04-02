package org.opentrafficsim.fosim;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.naming.NamingException;
import javax.swing.JFrame;

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
import org.opentrafficsim.fosim.sim0mq.FosimModel;
import org.opentrafficsim.road.network.RoadNetwork;
import org.opentrafficsim.swing.gui.OtsSimulationApplication;

import nl.tudelft.simulation.dsol.SimRuntimeException;

/**
 * Tests for the parser based on a library of test fos files.
 * <p>
 * Copyright (c) 2023-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
 */
public class ParserTestBatch
{

    /** Manual test or not. Should be false, unless you are performing a manual test. */
    private final static boolean MANUAL = true;

    /** Skips all fos files up to this number. Should be 0, unless you are performing a manual test. */
    private final static int FIRST = 5;
    
    /** Skips all fos files after this number. Should be Integer.MAX_VALUE, unless you are performing a manual test. */
    private final static int LAST = Integer.MAX_VALUE;

    /**
     * Test parsing of a file.
     * @throws SimRuntimeException simulation exception
     * @throws NamingException naming exception
     * @throws InvalidPathException path exception
     * @throws IOException input-output exception
     * @throws NetworkException network exception
     * @throws InterruptedException interrupted exception
     */
    @Test
    public void testParser() throws SimRuntimeException, NamingException, InvalidPathException, IOException, NetworkException,
            InterruptedException
    {
        List<String> files = getResourceFiles("/fos/");
        int i = 0;
        for (String file : files)
        {
            if (i < FIRST)
            {
                i++;
                continue;
            }
            if (i > LAST)
            {
                return;
            }
            System.out.println(i + ": " + file);
            i++;
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
            parserSettings.put(ParserSetting.STRIPED_AREAS, true);
            parserSettings.put(ParserSetting.GUI, MANUAL);
            parserSettings.put(ParserSetting.INSTANT_LC, true);
            parserSettings.put(ParserSetting.FOS_DETECTORS, true);

            String fileName = "/fos/" + file;
            FosParser parser = new FosParser();
            parser.setSettings(parserSettings).parseFromStream(ParametersJsonTest.class.getResourceAsStream(fileName));
            if (MANUAL)
            {
                OtsSimulationApplication<FosimModel> app = parser.getApplication();
                app.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                while (!app.isClosed())
                {
                    Thread.sleep(3);
                }
            }
        }

    }

    /**
     * Obtain list of files in folder.
     * @param path folder path.
     * @return list of file names.
     * @throws IOException IO exception
     */
    private List<String> getResourceFiles(final String path) throws IOException
    {
        List<String> filenames = new ArrayList<>();
        try (InputStream in = getClass().getResourceAsStream(path);
                BufferedReader br = new BufferedReader(new InputStreamReader(in)))
        {
            String resource;
            while ((resource = br.readLine()) != null)
            {
                filenames.add(resource);
            }
        }
        return filenames;
    }
}
