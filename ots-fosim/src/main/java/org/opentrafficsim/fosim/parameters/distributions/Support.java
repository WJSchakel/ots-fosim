package org.opentrafficsim.fosim.parameters.distributions;

/**
 * Valid domain of a parameter to which a distribution can apply.
 * <p>
 * Copyright (c) 2023-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
 */
public enum Support
{

    /** Distribution applicable to all domains. */
    all,
    
    /** Distribution applicable if param.minimum == 0. */ 
    from_zero,
    
    /** Distribution applicable if param.minimum >= 0. */ 
    positive;
    
}
