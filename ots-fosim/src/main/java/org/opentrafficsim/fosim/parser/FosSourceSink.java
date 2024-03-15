package org.opentrafficsim.fosim.parser;

import org.opentrafficsim.core.network.Node;

/**
 * Parsed source or sink info (this is the same info).
 * <p>
 * Copyright (c) 2023-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
 */
class FosSourceSink
{
    /** Section index, counting from the end. */
    public final int sectionFromEnd;

    /** From lane index. */
    public final int fromLane;

    /** To lane index. */
    public final int toLane;

    /** Name. */
    public final String name;
    
    /** Node that is created for this source or sink. */
    public Node node;

    /**
     * Parses a single source or sink.
     * @param string String; value of a source or sink line.
     */
    public FosSourceSink(final String string)
    {
        String[] fields = FosParser.splitStringByBlank(string, 4);
        this.sectionFromEnd = Integer.parseInt(fields[0]);
        this.fromLane = Integer.parseInt(fields[1]);
        this.toLane = Integer.parseInt(fields[2]);
        this.name = fields[3];
    }

    /** {@inheritDoc} */
    @Override
    public String toString()
    {
        return "FosSourceSink [name=" + this.name + ", section=" + this.sectionFromEnd + ", fromLane=" + this.fromLane
                + ", toLane=" + this.toLane + "]";
    }
    
}