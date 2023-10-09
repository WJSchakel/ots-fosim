package org.opentrafficsim.fosim.parameters;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

/**
 * Parameter validity checker. 
 * <p>
 * Copyright (c) 2023-2023 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://dittlab.tudelft.nl">Wouter Schakel</a>
 */
public class ParameterValidator
{

    /**
     * Test parameter validity. Parameters referred to in the limits exist, parameters must be unique by their id, default
     * values must be between the minimum and the maximum values. 
     */
    @Test
    public void checkParameters()
    {
        List<ParameterGroup> list = ParameterDefinitions.getParameterGroups();
        Map<String, Parameter> map = new LinkedHashMap<>();

        // gather all by id, check unique
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
                if (parameter.minimum.value != null && parameter.maximum.value != null)
                {
                    assertTrue(parameter.minimum.value < parameter.maximum.value,
                            "Limits of parameter " + parameter.id + " do not comply to min < max.");
                }
                if (parameter.minimum.parameter != null)
                {
                    assertTrue(map.containsKey(parameter.minimum.parameter), "Parameter " + parameter.id
                            + " refers to parameter " + parameter.minimum.parameter + " (min) which does not exist.");
                    assertTrue(
                            map.get(parameter.minimum.parameter).maximum != null
                                    && map.get(parameter.minimum.parameter).maximum.parameter.equals(parameter.id),
                            "Parameter " + parameter.id + " has a minimum of " + parameter.minimum.parameter
                                    + " but that does not have " + parameter.id + " as maximum.");
                }
                if (parameter.maximum.parameter != null)
                {
                    assertTrue(map.containsKey(parameter.maximum.parameter), "Parameter " + parameter.id
                            + " refers to parameter " + parameter.maximum.parameter + " (max) which does not exist.");
                    assertTrue(
                            map.get(parameter.maximum.parameter).minimum != null
                                    && map.get(parameter.maximum.parameter).minimum.parameter.equals(parameter.id),
                            "Parameter " + parameter.id + " has a maximum of " + parameter.maximum.parameter
                                    + " but that does not have " + parameter.id + " as minimum.");
                }
                for (DefaultValue value : parameter.defaultValue)
                {
                    if (value instanceof Scalar)
                    {
                        double val = ((Scalar) value).value();
                        if (parameter.minimum != null && parameter.minimum.value != null)
                        {
                            assertTrue(parameter.minimum.value <= val,
                                    "Parameter " + parameter.id + " default value is smaller than its minimum.");
                        }
                        if (parameter.maximum != null && parameter.maximum.value != null)
                        {
                            assertTrue(val <= parameter.maximum.value,
                                    "Parameter " + parameter.id + " default value is larger than its maximum.");
                        }
                    }
                }
            }
        }
    }

}
