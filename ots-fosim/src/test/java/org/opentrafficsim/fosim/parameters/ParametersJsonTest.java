package org.opentrafficsim.fosim.parameters;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;

import org.opentrafficsim.fosim.parameters.distributions.Distribution;
import org.opentrafficsim.fosim.parameters.distributions.DistributionDefinitions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

/**
 * Test code to serialize and deserialize parameter groups and distributions.
 * <p>
 * Copyright (c) 2023-2023 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://dittlab.tudelft.nl">Wouter Schakel</a>
 */
public class ParametersJsonTest
{

    /** File name for parameters JSON file. */
    private static final String PARAMETERS_FILENAME = "parameters.json";

    /** File name for distributions JSON file. */
    private static final String DISTRIBUTIONS_FILENAME = "distributions.json";

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
            writeParametersFile(ParameterDefinitions.getParameterGroups(), PARAMETERS_FILENAME, true, false);
        }
        catch (NullPointerException ex)
        {
            System.err.println("Unable to save file. Make sure \"src/test/resources/" + PARAMETERS_FILENAME
                    + "\" exists so it can be found to be overwritten.");
        }
        @SuppressWarnings("unused")
        Object t;
        try
        {
            TypeToken<ArrayList<ParameterGroup>> typeToken = new TypeToken<>()
            {
            };
            t = loadFile(PARAMETERS_FILENAME, typeToken);
        }
        catch (Exception ex)
        {
            System.err.println("Unable to load " + PARAMETERS_FILENAME + ".");
        }
        
        try
        {
            writeParametersFile(DistributionDefinitions.getDistributions(), DISTRIBUTIONS_FILENAME, true, false);
        }
        catch (NullPointerException ex)
        {
            System.err.println("Unable to save file. Make sure \"src/test/resources/" + DISTRIBUTIONS_FILENAME
                    + "\" exists so it can be found to be overwritten.");
        }
        try
        {
            TypeToken<ArrayList<Distribution>> typeToken = new TypeToken<>()
            {
            };
            t = loadFile(DISTRIBUTIONS_FILENAME, typeToken);
        }
        catch (Exception ex)
        {
            System.err.println("Unable to load " + DISTRIBUTIONS_FILENAME + ".");
            ex.printStackTrace();
        }
    }

    /**
     * This method writes the 'parameters.json' file.
     * @param data Object; data to write.
     * @param fileName String; file name.
     * @param prettyString boolean; whether to use new lines and indentation.
     * @param htmlEscaping boolean; whether to escape html characters.
     * @throws IOException when the file cannot be written.
     */
    private static void writeParametersFile(final Object data, final String fileName, final boolean prettyString,
            final boolean htmlEscaping) throws IOException
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
        String path = ParametersJsonTest.class.getResource("/" + fileName).getPath();
        if (path.endsWith("target/test-classes/" + fileName))
        {
            // hard-coded overule to save to src/test/resources
            path = path.replace("target/test-classes/" + fileName, "src/test/resources/" + fileName);
        }
        PrintWriter writer = new PrintWriter(new File(path));
        gson.toJson(data, writer);
        writer.close();
        System.out.println("Data written to " + path + ".");
    }

    /**
     * Returns parameter groups as loaded from file.
     * @param <T> type of the output.
     * @param fileName String; file name.
     * @param typeToken TypeToken&lt;T&gt;; type token.
     * @return T; list of parameter groups.
     * @throws IOException when the file cannot be loaded.
     */
    @SuppressWarnings("unchecked")
    public static <T> T loadFile(final String fileName, final TypeToken<T> typeToken) throws IOException
    {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(DefaultValue.class, new DefaultValueAdapter());
        Gson gson = builder.create();
        InputStream stream = ParametersJsonTest.class.getResourceAsStream("/" + fileName);
        Reader streamReader = new InputStreamReader(stream, "UTF-8");
        JsonReader reader = new JsonReader(streamReader);
        System.out.println("Data read from " + fileName + ".");
        return (T) gson.fromJson(reader, typeToken.getType());
    }

}
