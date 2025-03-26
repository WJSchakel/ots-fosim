package org.opentrafficsim.fosim.parameters;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * This class defines the parameters, their groups, descriptions, etc. It can also return a JSON string that represents all this
 * information.
 * <p>
 * Copyright (c) 2023-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
 */
public class ParameterDefinitions
{

    /** Vehicle group id. */
    public static final String VEHICLE_GROUP_ID = "Vehicle";

    /** Driver group id. */
    public static final String DRIVER_GROUP_ID = "Driver";

    /** Car-following group id. */
    public static final String FOLLOWING_GROUP_ID = "Car-following model";

    /** Lane-change group id. */
    public static final String LC_GROUP_ID = "Lane-change model";

    /** Social interactions group id. */
    public static final String SOCIAL_GROUP_ID = "Social interactions";

    /** Perception group id. */
    public static final String PERCEPTION_GROUP_ID = "Perception";

    /** Version. */
    @SuppressWarnings("unused") // used to parse to json
    private final String version;

    /** Parameter groups. */
    @SuppressWarnings("unused") // used to parse to json
    private final List<ParameterGroup> parameterGroups = getParameterGroups();

    /**
     * Constructor.
     * @param version version.
     */
    public ParameterDefinitions(final String version)
    {
        this.version = version;
    }

    /**
     * Return JSON string parameter definitions.
     * @param prettyString whether to use new lines and indentation.
     * @param htmlEscaping whether to escape html characters.
     * @param version version
     * @return JSON string of parameter definitions.
     */
    public static String getParametersJson(final boolean prettyString, final boolean htmlEscaping, final String version)
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
        return gson.toJson(new ParameterDefinitions(version));
    }

    /**
     * This method defines the parameters, their groups, descriptions, etc.
     * @return list of parameter groups, in order.
     */
    public static List<ParameterGroup> getParameterGroups()
    {
        List<ParameterGroup> list = new ArrayList<>();
        ParameterGroup group;

        // Vehicle
        group = new ParameterGroup("Voertuig", VEHICLE_GROUP_ID, DefaultState.ALWAYS).setDescriptionNl("Voertuig parameters.")
                .setDescriptionEn("Vehicle parameters.");
        group.addParameter(new Parameter("l", it("lengte"), it("length"), "m")// .setMin(3.0).setMax(26.0)
                .setDefault(4.19, 12.0).setDescriptionNl("Voertuiglengte.").setDescriptionEn("Vehicle length."));
        group.addParameter(new Parameter("w", it("breedte"), it("width"), "m")// .setMin(1.5).setMax(2.6)
                .setDefault(1.7, 2.55).setDescriptionNl("Voertuigbreedte.").setDescriptionEn("Vehicle width."));
        group.addParameter(new Parameter("vMax", it("v_max"), "km/h")// .setMin(50.0).setMax(200.0)
                // .setDefault(180.0, DistributionValue.normal(85.0, 2.5, 60.0, 100.0))
                .setDefault(180.0, 85.0).setDescriptionNl("Maximale voertuigsnelheid.")
                .setDescriptionEn("Maximum vehicle speed."));
        list.add(group);

        // Driver
        group = new ParameterGroup("Bestuurder", DRIVER_GROUP_ID, DefaultState.ALWAYS)
                .setDescriptionNl("Algemene bestuurder parameters.").setDescriptionEn("General driver parameters.");
        group.addParameter(new Parameter("Tmax", it("T_max"), "s")// .setMin("Tmin").setMax(10.0)
                .setDefault(1.2, 1.2).setDescriptionNl("Normale volgtijd.").setDescriptionEn("Regular desired headway."));
        group.addParameter(new Parameter("Tmin", it("T_min"), "s")// .setMin(0.1).setMax("Tmax")
                .setDefault(0.56, 0.56).setDescriptionNl("Minimale volgtijd bij rijstrookwisselen.")
                .setDescriptionEn("Minimum desired headway when changing lanes."));
        group.addParameter(new Parameter("fSpeed", it("f_speed"), "-")// .setMin(0.1).setMax(2.0)
                // .setDefault(DistributionValue.normal(1.0308, 0.1, 0.5, 2.0), DistributionValue.normal(1.0308, 0.1, 0.5, 2.0))
                .setDefault(1.0308, 1.0308).setDescriptionNl("Factor gewenste snelheid/maximum snelheid.")
                .setDescriptionEn("Factor desired speed/legal speed limit."));
        list.add(group);

        // Car-following model
        group = new ParameterGroup("Voertuig-volg model", FOLLOWING_GROUP_ID, DefaultState.ALWAYS)
                .setDescriptionNl("Parameters van het IDM+ voertuig-volg model. Dit wordt ook gebruikt voor het accepteren van "
                        + "gaten bij rijstrookwisselen en het remmen voor verkeerslichten.")
                .setDescriptionEn("Parameters of the IDM+ car-following model. This is also used for gap-acceptance when "
                        + "changing lane, and for braking for traffic lights.");
        group.addParameter(new Parameter("a", it("a"), sc("m/s^2"))// .setMin(0.0).setMax(8.0)
                .setDefault(1.25, 0.4).setDescriptionNl("Maximale acceleratie.").setDescriptionEn("Maximum acceleration."));
        group.addParameter(new Parameter("b0", it("b_0"), sc("m/s^2"))// .setMin(0.0).setMax("b")
                .setDefault(0.5, 0.5).setDescriptionNl("Aanpassingsdeceleratie, bv. bij verlaging van maximum snelheid.")
                .setDescriptionEn("Adjustment deceleration, e.g. at reduction of legal speed limit."));
        group.addParameter(new Parameter("b", it("b"), sc("m/s^2"))// .setMin("b0").setMax("bCrit")
                .setDefault(2.09, 2.09).setDescriptionNl("Maximale gewenste deceleratie.")
                .setDescriptionEn("Maximum desired deceleration."));
        group.addParameter(new Parameter("bCrit", it("b_crit"), sc("m/s^2"))// .setMin("b").setMax(8.0)
                .setDefault(3.5, 3.5).setDescriptionNl("Kritieke deceleratie, bv. bij verkeerslichten.")
                .setDescriptionEn("Critical deceleration, e.g. at traffic lights."));
        group.addParameter(new Parameter("s0", it("s_0"), "m")// .setMin(0.5).setMax(8.0)
                .setDefault(3.0, 3.0).setDescriptionNl("Stopafstand tussen voertuigen.")
                .setDescriptionEn("Stopping distance between vehicles."));
        group.addParameter(new Parameter("delta", it("δ"), "-")// .setMin(0).setMax(1000)
                .setDefault(4.0, 4.0).setDescriptionNl("Persistentie maximale acceleratie bij toenemen snelheid.")
                .setDescriptionEn("Persistence of maximum acceleration as speed increases."));
        list.add(group);

        // Lane-change model
        group = new ParameterGroup("Rijstrookwisselmodel", LC_GROUP_ID, DefaultState.ALWAYS)
                .setDescriptionNl("Parameters van het 'Lane-change Model with Relaxation and Synchronization' (LMRS) gebaseerd "
                        + "op rijstrookwisselwens.")
                .setDescriptionEn("Parameters of the Lane-change Model with Relaxation and Synchronization (LMRS) based on lane"
                        + " change desire.");
        group.addParameter(new Parameter("dFree", it("d_free"), "-")// .setMin(0.0).setMax("dSync")
                .setDefault(0.365, 0.365).setDescriptionNl("Drempelwaarde rijstrookwisselwens vrije wisseling.")
                .setDescriptionEn("Lane change desire threshold for free lane changes."));
        group.addParameter(new Parameter("dSync", it("d_sync"), "-")// .setMin("dFree").setMax("dCoop")
                .setDefault(0.577, 0.577).setDescriptionNl("Drempelwaarde rijstrookwisselwens synchroniseren snelheid.")
                .setDescriptionEn("Lane change desire threshold for synchronizing speed."));
        group.addParameter(new Parameter("dCoop", it("d_coop"), "-")// .setMin("dSync").setMax(1.0)
                .setDefault(0.788, 0.788).setDescriptionNl("Drempelwaarde rijstrookwisselwens cooperatie.")
                .setDescriptionEn("Lane change desire threshold for cooperation."));
        group.addParameter(new Parameter("tau", it("τ"), "s")// .setMin(1.0).setMax(120.0)
                .setDefault(25.0, 25.0).setDescriptionNl("Relaxatietijd naar normale volgtijd.")
                .setDescriptionEn("Relaxation time to regular headway."));
        group.addParameter(new Parameter("x0", it("x_0"), "m")// .setMin(100.0).setMax(1000.0)
                .setDefault(295.0, 295.0).setDescriptionNl("Anticipatie afstand rijstrookwisselen per strook.")
                .setDescriptionEn("Lane change anticipation distance per lane change."));
        group.addParameter(new Parameter("t0", it("t_0"), "s")// .setMin(10.0).setMax(120.0)
                .setDefault(43.0, 43.0).setDescriptionNl("Anticipatie tijd rijstrookwisselen per strook.")
                .setDescriptionEn("Lane change anticipation time per lane change."));
        group.addParameter(new Parameter("vGain", it("v_gain"), "km/h")// .setMin(1.0).setMax(130.0)
                .setDefault(69.6, 69.6).setDescriptionNl("Snelheidswinst voor maximaal gewenste rijstrookwisseling.")
                .setDescriptionEn("Speed gain for maximally desired lane change."));
        group.addParameter(new Parameter("vCong", it("v_cong"), "km/h")// .setMin(1.0).setMax(130.0)
                .setDefault(60.0, 60.0).setDescriptionNl("File drempelsnelheid.")
                .setDescriptionEn("Threshold speed for congestion."));
        group.addParameter(new Parameter("LCdur", it("LC_dur"), "s")// .setMin(1.0).setMax(5.0)
                .setDefault(3.0, 3.0).setDescriptionNl("Duur van een strookwisseling.")
                .setDescriptionEn("Duration of a lane change."));
        list.add(group);

        // Social interactions
        group = new ParameterGroup("Sociale interacties", SOCIAL_GROUP_ID, DefaultState.ON)
                .setDescriptionNl("Endogene beinvloeding van gewenste snelheid, volgtijd en rijstrookwisselwens vanuit sociale "
                        + "druk. Dit beinvloedt met name het aantal rijstrookwisselingen en de verdeling van volgtijden. "
                        + "Hierbij is het van belang dat er andere basis waarden worden gebruikt: <i>T<sub>max</sub></i> = 1.6s"
                        + ", <i>v<sub>gain</sub></i> = <i>LogNormaal</i>(3.3789, 0.4) (cars) en 50 (trucks) km/h.")
                .setDescriptionEn("Endogenous influence on desired speed, desired headway and lane change desire from social "
                        + "pressure. This mostly influences the number of lane changes and the headway distribution. It is "
                        + "important to use a different base value: <i>T<sub>max</sub></i> = 1.6s, <i>v<sub>gain</sub></i> = "
                        + "<i>LogNormaal</i>(3.3789, 0.4) (cars) and 50 (trucks) km/h.");
        group.addParameter(new Parameter("sigma", it("σ"), "-")// .setMin(0.0).setMax(1.0)
                // .setDefault(DistributionValue.triangular(0.0, 0.25, 1.0), 1.0)
                .setDefault(0.25, 1.0).setDescriptionNl("Gevoeligheid voor gewenste snelheid anderen.")
                .setDescriptionEn("Socio-speed sensitivity."));
        group.addParameter(new Parameter("courtesy", it("courtesy"), "0|1")// .setMin(0.0).setMax(1.0)
                .setDefault(1.0, 0.0).setDescriptionNl("Bereid rijstrook te wisselen voor anderen.")
                .setDescriptionEn("Willing to change lane for others."));
        list.add(group);

        // Perception
        group = new ParameterGroup("Perceptie", PERCEPTION_GROUP_ID, DefaultState.OFF)
                .setDescriptionNl("Endogene processen voor imperfecte perceptie, afhankelijk van mentale druk door rijtaken.")
                .setDescriptionEn(
                        "Endogenous processes of imperfect perception, depending on mental demand due to driving tasks.");
        group.addParameter(new Parameter("TC", it("TC"), "-")// .setMin(0.0).setMax(2.0)
                .setDefault(1.0, 1.0).setDescriptionNl("Taak capaciteit.").setDescriptionEn("Task capacity."));
        group.addParameter(new Parameter("TScrit", it("TS_crit"), "-")// .setMin(0.0).setMax("TSmax")
                .setDefault(0.8, 0.8)
                .setDescriptionNl(
                        "Kritische taak saturatie, waarboven situationele aandacht afneemt en de reactietijd toeneemt.")
                .setDescriptionEn(
                        "Critical task saturation, above which situational awareness reduces and reaction time increases."));
        group.addParameter(new Parameter("TSmax", it("TS_max"), "-")// .setMin("TScrit").setMax(3.0)
                .setDefault(2.0, 2.0).setDescriptionNl("Maximale taak saturatie.")
                .setDescriptionEn("Maximum task saturation."));
        group.addParameter(new Parameter("SAmin", it("SA_min"), "-")// .setMin(0).setMax("SAmax")
                .setDefault(0.5, 0.5).setDescriptionNl("Minimale situationele aandacht.")
                .setDescriptionEn("Minimum situational awareness."));
        group.addParameter(new Parameter("SAmax", it("SA_max"), "-")// .setMin("SAmin").setMax(1.0)
                .setDefault(1.0, 1.0).setDescriptionNl("Maximale situationele aandacht.")
                .setDescriptionEn("Maximum situational awareness."));
        group.addParameter(new Parameter("Trmax", it("T_r,max"), "s")// .setMin(0.0).setMax(3.0)
                .setDefault(2.0, 2.0).setDescriptionNl("Maximale reactietijd.")
                .setDescriptionEn("Maximum reaction time."));
        group.addParameter(new Parameter("hexp", it("h_exp"), "s")// .setMin(1.0).setMax(10.0)
                .setDefault(4.0, 4.0).setDescriptionNl("Helling van afname volg-taak last bij toenemen volgtijd.")
                .setDescriptionEn("Slope of reduction of car-following task demand as headway increases."));
        group.addParameter(new Parameter("betaT", it("β_T"), "-")// .setMin(0.0).setMax(2.0)
                .setDefault(1.0, 1.0).setDescriptionNl("Gevoeligheid aanpassing volgtijd.")
                .setDescriptionEn("Sensitivity behavioural adaptation of headway."));
        group.addParameter(new Parameter("betav0", it("β_v0"), "-")// .setMin(0.0).setMax(2.0)
                .setDefault(1.0, 1.0).setDescriptionNl("Gevoeligheid aanpassing snelheid.")
                .setDescriptionEn("Sensitivity behavioural adaptation of speed."));
        group.addParameter(new Parameter("est", it("estimation"), "-1|0|1")// .setMin(-1.0).setMax(1.0)
                .setDefault(1.0, 1.0).setDescriptionNl("Onder- of overschatting afstand en relatieve snelheid.")
                .setDescriptionEn("Under- or overestimation of distance and relative speed."));
        group.addParameter(new Parameter("ant", it("anticipation"), "0|1|2")// .setMin(0.0).setMax(2.0)
                .setDefault(1.0, 1.0).setDescriptionNl("Anticipatie (0=geen, 1=constante snelheid, 2=constante acceleratie).")
                .setDescriptionEn("Anticipation (0=none, 1=constant speed, 2=constant acceleration)."));
        group.addParameter(new Parameter("alpha", it("α"), "-")// .setMin(0.0).setMax(1.0)
                .setDefault(0.8, 0.8).setDescriptionNl("Maximale afname primaire taak last door anticipatie.")
                .setDescriptionEn("Maximum reduction of primary task load due to anticipation."));
        group.addParameter(new Parameter("beta", it("β"), "-")// .setMin(0.0).setMax(1.0)
                .setDefault(0.6, 0.6).setDescriptionNl("Maximale afname secundaire taak last door anticipatie.")
                .setDescriptionEn("Maximum reduction of secondary task load due to anticipation."));
        list.add(group);

        return list;
    }

    /**
     * Helper method to add html tags for italics, subscript and superscript. This method essentially makes everything italic,
     * except subscripts that are a single character "0". The format of the input string is "base_sub^sup", where "_sub" and
     * "^sup" are optional.
     * @param string string to add html tags to.
     * @return string with html tags.
     */
    public static String it(final String string)
    {
        String str = string;
        String suffix = "";
        if (str.contains("^"))
        {
            int index = str.indexOf("^");
            suffix = "<sup>" + str.substring(index + 1) + "</sup>";
            str = str.substring(0, index);
        }
        if (str.contains("_"))
        {
            int index = str.indexOf("_");
            String sub = str.substring(index + 1);
            if ("0".equals(sub))
            {
                return "<i>" + str.substring(0, index) + "</i><sub>0</sub>"
                        + (suffix.isEmpty() ? "" : ("<i>" + suffix + "</i>"));
            }
            return "<i>" + str.substring(0, index) + "<sub>" + sub + "</sub>" + suffix + "</i>";
        }
        return "<i>" + str + suffix + "</i>";
    }

    /**
     * Helper method to add html tags for subscript and superscript. The format of the input string is "base_sub^sup", where
     * "_sub" and "^sup" are optional.
     * @param string string to add html tags to.
     * @return string with html tags.
     */
    protected static String sc(final String string)
    {
        String str = string;
        String suffix = "";
        if (str.contains("^"))
        {
            int index = str.indexOf("^");
            suffix = "<sup>" + str.substring(index + 1) + "</sup>";
            str = str.substring(0, index);
        }
        if (str.contains("_"))
        {
            int index = str.indexOf("_");
            String sub = str.substring(index + 1);
            str = str.substring(0, index) + "<sub>" + sub + "</sub>";
        }
        return str + suffix;
    }

}
