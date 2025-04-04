package org.opentrafficsim.fosim.parameters.distributions;

import static org.opentrafficsim.fosim.parameters.ParameterDefinitions.it;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * This class defines the distributions, their parameters, etc. It can also return a JSON string that represents all this
 * information.
 * <p>
 * Copyright (c) 2023-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
 */
@Deprecated
public class DistributionDefinitions
{

    /** Version. */
    @SuppressWarnings("unused") // used to parse to json
    private final String version;
    
    /** Distributions. */
    @SuppressWarnings("unused") // used to parse to json
    private final List<Distribution> distributions = getDistributions();
    
    /**
     * Constructor.
     * @param version version.
     */
    public DistributionDefinitions(final String version)
    {
        this.version = version;
    }
    
    /**
     * Return JSON string distribution definitions.
     * @param prettyString whether to use new lines and indentation.
     * @param htmlEscaping whether to escape html characters.
     * @param version version
     * @return JSON string of distribution definitions.
     */
    public static String getDistributionsJson(final boolean prettyString, final boolean htmlEscaping, final String version)
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
        // builder.registerTypeAdapter(DefaultValue.class, new DefaultValueAdapter());
        Gson gson = builder.create();
        return gson.toJson(new DistributionDefinitions(version));
    }

    /**
     * This method defines the distributions, their parameters, etc.
     * @return list of distributions, in order.
     */
    public static List<Distribution> getDistributions()
    {
        List<Distribution> list = new ArrayList<>();

        // Exponential
        Distribution exponential = new Distribution(DistributionType.Exponential, "Exponentieel", "Exponential")
                .setValidRange(ValidRange.positive_inclusive);
        DistributionParameter lambda =
                new DistributionParameter("lambda", it("λ")).setMin(0.0).setMax("param.max").setDefault("param.default");
        exponential.addParameter(lambda);
        list.add(exponential);

        // Triangular
        Distribution triangular = new Distribution(DistributionType.Triangular, "Driehoek", "Triangular");
        DistributionParameter triMin =
                new DistributionParameter("min", it("min")).setMin("param.min").setMax("mode").setDefault("param.min");
        DistributionParameter triMode =
                new DistributionParameter("mode", it("mode")).setMin("min").setMax("max").setDefault("param.default");
        DistributionParameter triMax =
                new DistributionParameter("max", it("max")).setMin("mode").setMax("param.max").setDefault("param.max");
        triangular.addParameter(triMin);
        triangular.addParameter(triMode);
        triangular.addParameter(triMax);
        list.add(triangular);

        // Normal
        Distribution normal = new Distribution(DistributionType.Normal, "Normaal", "Normal");
        DistributionParameter normMu =
                new DistributionParameter("mu", it("μ")).setMin("min").setMax("max").setDefault("param.default");
        DistributionParameter normSigma = new DistributionParameter("sigma", it("σ")).setMin(0.0).setDefault(1.0);
        DistributionParameter normMin =
                new DistributionParameter("min", it("min")).setMin("param.min").setMax("mu").setDefault("param.min");
        DistributionParameter normMax =
                new DistributionParameter("max", it("max")).setMin("mu").setMax("param.max").setDefault("param.max");
        normal.addParameter(normMu);
        normal.addParameter(normSigma);
        normal.addParameter(normMin);
        normal.addParameter(normMax);
        list.add(normal);

        // LogNormal
        Distribution logNormal =
                new Distribution(DistributionType.LogNormal, "LogNormaal", "LogNormal").setValidRange(ValidRange.positive);
        DistributionParameter logMean = new DistributionParameter("mean", it("gemiddeld"), it("mean")).setMin("min")
                .setMax("max").setDefault("param.default");
        DistributionParameter logStd = new DistributionParameter("std", it("std")).setMin(0.0).setDefault(1.0);
        DistributionParameter logMin =
                new DistributionParameter("min", it("min")).setMin("param.min").setMax("mean").setDefault("param.min");
        DistributionParameter logMax =
                new DistributionParameter("max", it("max")).setMin("mean").setMax("param.max").setDefault("param.max");
        logNormal.addParameter(logMean);
        logNormal.addParameter(logStd);
        logNormal.addParameter(logMin);
        logNormal.addParameter(logMax);
        list.add(logNormal);

        // Uniform
        Distribution uniform =
                new Distribution(DistributionType.Uniform, "Uniform", "Uniform");
        DistributionParameter uniMin =
                new DistributionParameter("min", it("min")).setMin("param.min").setMax("max").setDefault("param.min");
        DistributionParameter uniMax =
                new DistributionParameter("max", it("max")).setMin("min").setMax("param.max").setDefault("param.max");
        uniform.addParameter(uniMin);
        uniform.addParameter(uniMax);
        list.add(uniform);

        return list;
    }

}
