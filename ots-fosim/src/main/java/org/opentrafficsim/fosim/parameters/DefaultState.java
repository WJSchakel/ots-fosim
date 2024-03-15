package org.opentrafficsim.fosim.parameters;

/**
 * Default state of parameter group. 
 * <p>
 * Copyright (c) 2023-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
 */
public enum DefaultState
{
    
    /** Always on; mandatory parameters and model components. */
    ALWAYS,
    
    /** Model component and parameters on by default. */
    ON,
    
    /** Model component and parameters off by default. */
    OFF;
    
}