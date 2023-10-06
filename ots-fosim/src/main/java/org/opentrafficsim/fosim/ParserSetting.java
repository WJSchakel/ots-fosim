package org.opentrafficsim.fosim;

/**
 * Parser settings.
 * @author wjschakel
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

    /** Whether to include the striped area's as valid lane change area's. */
    STRIPED_AREAS(false);

    /** Default value. */
    private final boolean defaultValue;

    /**
     * Constructor.
     * @param defaultValue boolean; default value.
     */
    ParserSetting(final boolean defaultValue)
    {
        this.defaultValue = defaultValue;
    }

    /**
     * Returns the default value.
     * @return boolean; the default value.
     */
    public boolean getDefaultValue()
    {
        return this.defaultValue;
    }
}
