package org.opentrafficsim.fosim.parser;

/**
 * Parser settings.
 * <p>
 * Copyright (c) 2023-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
 */
public enum ParserSetting
{
    /** Parse demand. */
    DEMAND(true),

    /** Parse traffic lights. */
    TRAFFIC_LIGHTS(true),

    /** Parse temporary blockage. */
    TEMPORARY_BLOCKAGE(true),

    /** Parse detectors. */
    DETECTORS(true),
    
    /** Detector type. */
    FOS_DETECTORS(false),

    /** Whether to include the striped area's as valid lane change area's. */
    STRIPED_AREAS(false),
    
    /** Whether to build a GUI, or just a simulator. Ignored when a simulator is set in the parser. */
    GUI(true),
    
    /** Instant lane changes. */
    INSTANT_LC(false);

    /** Default value. */
    private final boolean defaultValue;

    /**
     * Constructor.
     * @param defaultValue default value.
     */
    ParserSetting(final boolean defaultValue)
    {
        this.defaultValue = defaultValue;
    }

    /**
     * Returns the default value.
     * @return the default value.
     */
    public boolean getDefaultValue()
    {
        return this.defaultValue;
    }
}
