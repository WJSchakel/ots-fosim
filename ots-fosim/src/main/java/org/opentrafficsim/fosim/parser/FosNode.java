package org.opentrafficsim.fosim.parser;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Class with node information.
 * <p>
 * Copyright (c) 2023-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
 */
class FosNode extends FosElement
{
    /** Links in to the node. */
    public final Set<FosLink> inLinks = new LinkedHashSet<>();

    /** Links out of the node. */
    public final Set<FosLink> outLinks = new LinkedHashSet<>();

    /** Node name. */
    String name;
    
    /** Source at this node. */
    public FosSourceSink source;
    
    /** Sink at this node. */
    public FosSourceSink sink;

    /**
     * Constructor.
     * @param nodeNumber int; unique node number.
     */
    public FosNode(final int nodeNumber)
    {
        super(nodeNumber);
        //System.out.println("Created node " + this.number + " with name " + getName());
    }

    /**
     * Returns a name for the node. If no name is given, the number is translated as 1&gt;A, 2&gt;B, ..., 27&gt;AA,
     * 28&gt;AB, etc.
     * @return String; name of the node.
     */
    public String getName()
    {
        if (this.name == null)
        {
            int num = this.number;
            this.name = "";
            while (num > 0)
            {
                int remainder = (num - 1) % 26;
                num = (num - remainder) / 26;
                this.name = (char) (remainder + 'A') + this.name;
            }
        }
        return this.name;
    }
    
    
}