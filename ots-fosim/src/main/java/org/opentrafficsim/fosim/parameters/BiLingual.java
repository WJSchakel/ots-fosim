package org.opentrafficsim.fosim.parameters;

/**
 * Stores a Dutch and an English name. 
 * <p>
 * Copyright (c) 2023-2023 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://dittlab.tudelft.nl">Wouter Schakel</a>
 */
public class BiLingual
{

    /** Dutch name. */
    public final String nameNl;

    /** English name. */
    public final String nameEn;

    /**
     * Constructor using same name for Dutch and English.
     * @param name String; parameter name.
     */
    public BiLingual(final String name)
    {
        this.nameNl = name;
        this.nameEn = name;
    }
    
    /**
     * Constructor with different names between Dutch and English.
     * @param nameNl String; Dutch parameter name.
     * @param nameEn String; English parameter name.
     */
    public BiLingual(final String nameNl, final String nameEn)
    {
        this.nameNl = nameNl;
        this.nameEn = nameEn;
    }
    
}
