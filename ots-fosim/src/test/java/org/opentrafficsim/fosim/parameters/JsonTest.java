package org.opentrafficsim.fosim.parameters;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

/**
 * Test code to serialize and deserialize parameter groups.
 * <p>
 * Copyright (c) 2023-2023 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://dittlab.tudelft.nl">Wouter Schakel</a>
 */
public class JsonTest
{

    /** File name for parameters JSON file. */
    private static final String FILENAME = "parameters.json";

    /**
     * Runs to write parameters file. Note this file is not required for OTS to communicate to FOSIM as the JSON string is sent
     * directly to FOSIM over Sim0mq.
     * @param args String[]; arguments.
     * @throws IOException when the file cannot be written.
     */
    public static void main(final String[] args) throws IOException
    {
        try
        {
            writeParametersFile(true, false);
        }
        catch (NullPointerException ex)
        {
            System.err.println("Unable to save file. Make sure \"src/test/resources/" + FILENAME
                    + "\" exists so it can be found to be overwritten.");
        }
    }

    /**
     * This method writes the 'parameters.json' file.
     * @param prettyString boolean; whether to use new lines and indentation.
     * @param htmlEscaping boolean; whether to escape html characters.
     * @throws IOException when the file cannot be written.
     */
    public static void writeParametersFile(final boolean prettyString, final boolean htmlEscaping) throws IOException
    {
        GsonBuilder builder = new GsonBuilder();
        if (prettyString)
        {
            builder.setPrettyPrinting();
        }
        if (!htmlEscaping)
        {
            builder.disableHtmlEscaping();
        }
        builder.registerTypeAdapter(DefaultValue.class, new DefaultValueAdapter());
        Gson gson = builder.create();

        // Write to resources (should not work in a Jar file)
        String path = JsonTest.class.getResource("/" + FILENAME).getPath();
        if (path.endsWith("target/test-classes/" + FILENAME))
        {
            // hard-coded overule to save to src/test/resources
            path = path.replace("target/test-classes/" + FILENAME, "src/test/resources/" + FILENAME);
        }
        PrintWriter writer = new PrintWriter(new File(path));
        gson.toJson(ParameterDefinitions.getParameterGroups(), writer);
        writer.close();
        System.out.println("Parameters written to " + path + ".");
    }

    /**
     * Returns parameter groups as loaded from file.
     * @return List&lt;ParameterGroup&gt;; list of parameter groups.
     * @throws IOException when the file cannot be loaded.
     */
    public static List<ParameterGroup> loadParametersFile() throws IOException
    {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(DefaultValue.class, new DefaultValueAdapter());
        Gson gson = builder.create();

        InputStream stream = JsonTest.class.getResourceAsStream("/parameters.json");
        Reader streamReader = new InputStreamReader(stream, "UTF-8");
        JsonReader reader = new JsonReader(streamReader);
        Type listOfMyClassObject = new TypeToken<ArrayList<ParameterGroup>>()
        {
        }.getType();
        return gson.fromJson(reader, listOfMyClassObject);
    }

}
