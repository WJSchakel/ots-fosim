package org.opentrafficsim.fosim.parser;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.opentrafficsim.core.network.NetworkException;

/**
 * This is a simple demo that runs Terbregseplein_6.5_aangepast.fos.
 * <p>
 * Copyright (c) 2023-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
 */
public class ParserDemo
{

    /**
     * Runs the demo.
     * @param args command line arguments.
     * @throws NetworkException; network exception
     * @throws IOException; file reading exception
     */
    public static void main(final String... args) throws NetworkException, IOException
    {
        Map<ParserSetting, Boolean> parserSettings = new LinkedHashMap<>();
        parserSettings.put(ParserSetting.STRIPED_AREAS, false);

        String fileName = "Terbregseplein_6.5_aangepast.fos";
        new FosParser().setSettings(parserSettings).parseFromStream(ParserDemo.class.getResourceAsStream("/" + fileName));
    }

}
