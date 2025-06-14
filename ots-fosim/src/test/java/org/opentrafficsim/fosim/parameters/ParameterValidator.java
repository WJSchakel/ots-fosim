package org.opentrafficsim.fosim.parameters;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.opentrafficsim.fosim.parameters.distributions.Distribution;
import org.opentrafficsim.fosim.parameters.distributions.DistributionDefinitions;
import org.opentrafficsim.fosim.parameters.distributions.DistributionParameter;
import org.opentrafficsim.fosim.parameters.distributions.DistributionType;

/**
 * Parameter validity checker.
 * <p>
 * Copyright (c) 2023-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
 */
public class ParameterValidator
{

    /** Ordinal list. */
    private static final List<String> ORDER = List.of("param.minimum", "min", "", "max", "param.maximum");

    /**
     * Test parameter validity. Parameters referred to in the limits exist, parameters must be unique by their id, default
     * values must be between the minimum and the maximum values.
     */
    @Test
    public void checkParameters()
    {
        List<ParameterGroup> list = ParameterDefinitions.getParameterGroups();

        // gather all by id, check unique
        Map<String, Parameter> map = new LinkedHashMap<>();
        for (ParameterGroup group : list)
        {
            for (Parameter parameter : group.parameters)
            {
                assertFalse(map.containsKey(parameter.id), "Parameter " + parameter.id + " is not unique.");
                map.put(parameter.id, parameter);
            }
        }

        // check limits
        for (ParameterGroup group : list)
        {
            for (Parameter parameter : group.parameters)
            {
                assertTrue(parameter.minimum != null && parameter.maximum != null,
                        "Parameter " + parameter.id + " is missing minimum or maximum.");
            }
            for (Parameter parameter : group.parameters)
            {
                if (parameter.minimum.value != null && parameter.maximum.value != null)
                {
                    assertTrue(parameter.minimum.value < parameter.maximum.value,
                            "Limits of parameter " + parameter.id + " do not comply to min < max.");
                }
                if (parameter.minimum.parameter != null)
                {
                    assertTrue(map.containsKey(parameter.minimum.parameter), "Parameter " + parameter.id
                            + " refers to parameter " + parameter.minimum.parameter + " (min) which does not exist.");
                    Limit otherLimit = map.get(parameter.minimum.parameter).maximum;
                    assertTrue(otherLimit.parameter != null && otherLimit.parameter.equals(parameter.id),
                            "Parameter " + parameter.id + " has a minimum of " + parameter.minimum.parameter
                                    + " but that does not have " + parameter.id + " as maximum.");
                }
                if (parameter.maximum.parameter != null)
                {
                    assertTrue(map.containsKey(parameter.maximum.parameter), "Parameter " + parameter.id
                            + " refers to parameter " + parameter.maximum.parameter + " (max) which does not exist.");
                    Limit otherLimit = map.get(parameter.maximum.parameter).minimum;
                    assertTrue(otherLimit.parameter != null && otherLimit.parameter.equals(parameter.id),
                            "Parameter " + parameter.id + " has a maximum of " + parameter.maximum.parameter
                                    + " but that does not have " + parameter.id + " as minimum.");
                }
                for (DefaultValue value : parameter.defaultValue)
                {
                    if (value instanceof Scalar)
                    {
                        double val = ((Scalar) value).value();
                        if (parameter.minimum.value != null)
                        {
                            assertTrue(parameter.minimum.value <= val,
                                    "Parameter " + parameter.id + " default value is smaller than its minimum.");
                        }
                        if (parameter.maximum.value != null)
                        {
                            assertTrue(val <= parameter.maximum.value,
                                    "Parameter " + parameter.id + " default value is larger than its maximum.");
                        }
                    }
                }
            }
            // Note: the above checks guarantee that any dynamic (chained) limit ends up at a value. Each parameter must have a
            // minimum and a maximum. Each dynamic limit must be an existing parameter. Only circular chains then allow no value
            // to be found, which is checked below.
            for (Parameter parameter : group.parameters)
            {
                Limit limit = parameter.minimum;
                while (limit.parameter != null)
                {
                    assertNotEquals(parameter.id, limit.parameter, "Circular limit definition for parameter " + parameter.id);
                    limit = map.get(limit.parameter).minimum;
                }
                // assertNotNull(limit.value, "Parameter " + parameter.id + " has a minimum that is never defined by a value.");
                assertTrue(limit.equals(parameter.minimum) || limit.value != 0.0, "Minimum bound for parameter " + parameter.id
                        + " ends up at a parameter that might not have a 'max' value when distributed.");

                limit = parameter.maximum;
                while (limit.parameter != null)
                {
                    assertNotEquals(parameter.id, limit.parameter, "Circular limit definition for parameter " + parameter.id);
                    limit = map.get(limit.parameter).maximum;
                }
                // assertNotNull(limit.value, "Parameter " + parameter.id + " has a maximum that is never defined by a value.");
                assertTrue(limit.equals(parameter.maximum) || limit.value != 0.0, "Maximum bound for parameter " + parameter.id
                        + " ends up at a parameter that might not have a 'min' value when distributed.");
            }
        }
    }

    /**
     * Test distribution validity. Distributions have unique types, parameters referred to in the limits exist (or are
     * param.minimum/maximum/defaultValue), parameters must be unique by their id within the distribution, default values must
     * be between the minimum and the maximum values (when given), parameters must obey ordinality: param.minimum &le; min &le;
     * any other with {@code .param} field &le; max &le; param.maximum.
     */
    @Test
    public void checkDistributions()
    {
        List<Distribution> list = DistributionDefinitions.getDistributions();

        // gather all by type, check unique
        Set<DistributionType> set = new LinkedHashSet<>();
        for (Distribution distribution : list)
        {
            assertFalse(set.contains(distribution.type), "Distribution " + distribution.type + " is not unique.");
            set.add(distribution.type);
        }

        // check individual distributions
        Set<String> allowedDefaultParams = Set.of("param.minimum", "param.maximum", "param.defaultValue");
        for (Distribution distribution : list)
        {
            // gather all by id, check unique
            Map<String, DistributionParameter> map = new LinkedHashMap<>();
            for (DistributionParameter parameter : distribution.parameters)
            {
                assertFalse(map.containsKey(parameter.id), "Parameter " + parameter.id + " of distribution " + distribution.type
                        + " is not unique within the distribution.");
                map.put(parameter.id, parameter);
            }

            // check limits
            for (DistributionParameter parameter : distribution.parameters)
            {
                int ordinal = getOrdinal(parameter.id);
                if (parameter.minimum != null && parameter.minimum.parameter != null)
                {
                    assertTrue(getOrdinal(parameter.minimum.parameter) < ordinal,
                            "Ordinallity between " + parameter.id + " and " + parameter.minimum.parameter + " of distribution "
                                    + distribution.type + " is incorrect.");
                    if (parameter.minimum.parameter.startsWith("param."))
                    {
                        assertTrue(allowedDefaultParams.contains(parameter.minimum.parameter), "Parameter " + parameter.id
                                + " of distribution " + distribution.type + " has invalid minimum value.");
                    }
                    else
                    {
                        assertTrue(map.containsKey(parameter.minimum.parameter),
                                "Parameter " + parameter.id + " of distribution " + distribution.type + " refers to parameter "
                                        + parameter.minimum.parameter + " which does not exist.");
                        assertTrue(map.get(parameter.minimum.parameter).maximum.parameter.equals(parameter.id),
                                "Parameter " + parameter.id + " of distribution " + distribution.type + " has a minimum of "
                                        + parameter.minimum.parameter + " but that does not have " + parameter.id
                                        + " as maximum.");
                    }
                }
                if (parameter.maximum != null && parameter.maximum.parameter != null)
                {
                    assertTrue(getOrdinal(parameter.maximum.parameter) > ordinal,
                            "Ordinallity between " + parameter.id + " and " + parameter.maximum.parameter + " of distribution "
                                    + distribution.type + " is incorrect.");
                    if (parameter.maximum.parameter.startsWith("param."))
                    {
                        assertTrue(allowedDefaultParams.contains(parameter.maximum.parameter), "Parameter " + parameter.id
                                + " of distribution " + distribution.type + " has invalid maximum value.");
                    }
                    else
                    {
                        assertTrue(map.containsKey(parameter.maximum.parameter),
                                "Parameter " + parameter.id + " of distribution " + distribution.type + " refers to parameter "
                                        + parameter.maximum.parameter + " which does not exist.");
                        assertTrue(map.get(parameter.maximum.parameter).minimum.parameter.equals(parameter.id),
                                "Parameter " + parameter.id + " of distribution " + distribution.type + " has a maximum of "
                                        + parameter.maximum.parameter + " but that does not have " + parameter.id
                                        + " as minimum.");
                    }
                }
                if (parameter.defaultValue != null)
                {
                    if (parameter.defaultValue.parameter != null)
                    {
                        assertTrue(allowedDefaultParams.contains(parameter.defaultValue.parameter), "Parameter " + parameter.id
                                + " of distribution " + distribution.type + " has invalid default value.");
                    }
                    else if (parameter.defaultValue.value != null)
                    {
                        if (parameter.minimum != null && parameter.minimum.value != null)
                        {
                            assertTrue(parameter.defaultValue.value >= parameter.minimum.value,
                                    "Parameter " + parameter.id + " of distribution " + distribution.type
                                            + " has a default value that is smaller than its minimum.");
                        }
                        if (parameter.maximum != null && parameter.maximum.value != null)
                        {
                            assertTrue(parameter.defaultValue.value <= parameter.maximum.value,
                                    "Parameter " + parameter.id + " of distribution " + distribution.type
                                            + " has a default value that is larger than its maximum.");
                        }
                    }
                }
            }
        }
    }

    /**
     * Return the ordinal of given distribution parameter name.
     * @param parameter distribution parameter name.
     * @return ordinal of given distribution parameter name.
     */
    private static int getOrdinal(final String parameter)
    {
        int ordinal = ORDER.indexOf(parameter);
        return ordinal < 0 ? 2 : ordinal;
    }

}
