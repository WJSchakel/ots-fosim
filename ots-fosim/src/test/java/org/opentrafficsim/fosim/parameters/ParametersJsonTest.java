package org.opentrafficsim.fosim.parameters;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;

import org.opentrafficsim.fosim.parameters.distributions.DistributionDefinitions;
import org.opentrafficsim.fosim.sim0mq.OtsTransceiver;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;

/**
 * Test code to serialize and deserialize parameter groups and distributions. By running this class the parameters.json and
 * distributions.json files are created in the test resources.
 * <p>
 * Copyright (c) 2023-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
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
     * @param args arguments.
     * @throws IOException when the file cannot be written.
     */
    public static void main(final String[] args) throws IOException
    {
        writeToFile(new ParameterDefinitions(OtsTransceiver.VERSION), PARAMETERS_FILENAME, true, false);
        try
        {
            TypeToken<ParameterDefinitions> typeToken = new TypeToken<>()
            {
            };
            @SuppressWarnings("unused")
            ParameterDefinitions param = loadFile(PARAMETERS_FILENAME, typeToken);
        }
        catch (Exception ex)
        {
            System.err.println("Unable to load " + PARAMETERS_FILENAME + ".");
        }

        writeToFile(new DistributionDefinitions(OtsTransceiver.VERSION), DISTRIBUTIONS_FILENAME, true, false);
        try
        {
            TypeToken<DistributionDefinitions> typeToken = new TypeToken<>()
            {
            };
            @SuppressWarnings("unused")
            DistributionDefinitions dist = loadFile(DISTRIBUTIONS_FILENAME, typeToken);
        }
        catch (Exception ex)
        {
            System.err.println("Unable to load " + DISTRIBUTIONS_FILENAME + ".");
            ex.printStackTrace();
        }
    }

    /**
     * This method writes the data to file.
     * @param data data to write.
     * @param fileName file name.
     * @param prettyString whether to use new lines and indentation.
     * @param htmlEscaping whether to escape html characters.
     * @throws IOException when the file cannot be written.
     */
    private static void writeToFile(final Object data, final String fileName, final boolean prettyString,
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
        builder.registerTypeAdapter(Limit.class, new LimitAdapter());
        Gson gson = builder.create();

        // Write to resources (should not work in a Jar file)
        String path = ParametersJsonTest.class.getResource("/").getPath();
        if (path.endsWith("target/test-classes/"))
        {
            // hard-coded overule to save to src/test/resources
            path = path.replace("target/test-classes/", "src/test/resources/");
        }
        path += fileName;
        PrintWriter writer = new PrintWriter(new File(path));
        gson.toJson(data, writer);
        writer.close();
        System.out.println("Data written to " + path + ".");
    }

    /**
     * Returns parameter groups as loaded from file.
     * @param <T> type of the output.
     * @param fileName file name.
     * @param typeToken type token.
     * @return list of parameter groups.
     * @throws IOException when the file cannot be loaded.
     */
    @SuppressWarnings("unchecked")
    public static <T> T loadFile(final String fileName, final TypeToken<T> typeToken) throws IOException
    {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(DefaultValue.class, new DefaultValueAdapter());
        builder.registerTypeAdapter(Limit.class, new LimitAdapter());
        Gson gson = builder.create();
        InputStream stream = ParametersJsonTest.class.getResourceAsStream("/" + fileName);
        Reader streamReader = new InputStreamReader(stream, "UTF-8");
        JsonReader reader = new JsonReader(streamReader);
        System.out.println("Data read from " + fileName + ".");
        return (T) gson.fromJson(reader, typeToken.getType());
    }

}
