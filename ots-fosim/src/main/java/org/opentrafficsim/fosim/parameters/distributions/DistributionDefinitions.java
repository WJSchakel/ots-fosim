package org.opentrafficsim.fosim.parameters.distributions;

import static org.opentrafficsim.fosim.parameters.ParameterDefinitions.it;

import java.util.ArrayList;
import java.util.List;

import org.opentrafficsim.fosim.parameters.DefaultValue;
import org.opentrafficsim.fosim.parameters.DefaultValueAdapter;
import org.opentrafficsim.fosim.parameters.Limit;
import org.opentrafficsim.fosim.parameters.LimitAdapter;

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
        builder.registerTypeAdapter(DefaultValue.class, new DefaultValueAdapter());
        builder.registerTypeAdapter(Limit.class, new LimitAdapter());
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
        Distribution exponential = new Distribution(DistributionType.Exponential, it("E"), "Exponentieel", "Exponential")
                .setSupport(Support.from_zero);
        DistributionParameter lambda = new DistributionParameter("lambda", it("λ")).setMin(0.0).setMax("param.maximum")
                .setDefault("param.defaultValue");
        exponential.addParameter(lambda);
        list.add(exponential);

        // Triangular
        Distribution triangular = new Distribution(DistributionType.Triangular, it("T"), "Driehoek", "Triangular");
        DistributionParameter triMin =
                new DistributionParameter("min", it("min")).setMin("param.minimum").setMax("mode").setDefault("param.minimum");
        DistributionParameter triMode =
                new DistributionParameter("mode", it("mode")).setMin("min").setMax("max").setDefault("param.defaultValue");
        DistributionParameter triMax =
                new DistributionParameter("max", it("max")).setMin("mode").setMax("param.maximum").setDefault("param.maximum");
        triangular.addParameter(triMin);
        triangular.addParameter(triMode);
        triangular.addParameter(triMax);
        list.add(triangular);

        // Normal
        Distribution normal = new Distribution(DistributionType.Normal, it("N"), "Normaal", "Normal");
        DistributionParameter normMu =
                new DistributionParameter("mu", it("μ")).setMin("min").setMax("max").setDefault("param.defaultValue");
        DistributionParameter normSigma = new DistributionParameter("sigma", it("σ")).setMin(0.0).setDefault(1.0);
        DistributionParameter normMin =
                new DistributionParameter("min", it("min")).setMin("param.minimum").setMax("mu").setDefault("param.minimum");
        DistributionParameter normMax =
                new DistributionParameter("max", it("max")).setMin("mu").setMax("param.maximum").setDefault("param.maximum");
        normal.addParameter(normMu);
        normal.addParameter(normSigma);
        normal.addParameter(normMin);
        normal.addParameter(normMax);
        list.add(normal);

        // LogNormal
        Distribution logNormal = new Distribution(DistributionType.LogNormal, it("LN"), "LogNormaal", "LogNormal")
                .setSupport(Support.positive);
        DistributionParameter logMu = new DistributionParameter("mu", it("μ")).setDefault(1.0);
        DistributionParameter logSigma = new DistributionParameter("sigma", it("σ")).setMin(0.0).setDefault(1.0);
        DistributionParameter logMin =
                new DistributionParameter("min", it("min")).setMin("param.minimum").setMax("max").setDefault("param.minimum");
        DistributionParameter logMax =
                new DistributionParameter("max", it("max")).setMin("min").setMax("param.maximum").setDefault("param.maximum");
        logNormal.addParameter(logMu);
        logNormal.addParameter(logSigma);
        logNormal.addParameter(logMin);
        logNormal.addParameter(logMax);
        list.add(logNormal);

        // Uniform
        Distribution uniform = new Distribution(DistributionType.Uniform, it("U"), "Uniform", "Uniform");
        DistributionParameter uniMin =
                new DistributionParameter("min", it("min")).setMin("param.minimum").setMax("max").setDefault("param.minimum");
        DistributionParameter uniMax =
                new DistributionParameter("max", it("max")).setMin("min").setMax("param.maximum").setDefault("param.maximum");
        uniform.addParameter(uniMin);
        uniform.addParameter(uniMax);
        list.add(uniform);

        return list;
    }

}
