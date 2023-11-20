package org.opentrafficsim.fosim.parser;

import java.util.ArrayList;
import java.util.List;

/**
 * Class with link information.
 * <p>
 * Copyright (c) 2023-2023 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://dittlab.tudelft.nl">Wouter Schakel</a>
 */
class FosLink extends FosElement
{
    /** Section index. */
    final public int sectionIndex;

    /** From-lane index. */
    final public int fromLane;

    /** To-lane index. */
    final public int toLane;

    /** From-node. */
    public FosNode fromNode;

    /** To-node. */
    public FosNode toNode;

    /** Lanes. */
    final public List<FosLane> lanes = new ArrayList<>();

    /**
     * Constructor.
     * @param linkNumber int; unique link number.
     * @param sectionIndex int; section index.
     * @param fromLane int; from-lane index.
     * @param toLane int; to-lane index.
     * @param parser FosParser; parser to supply the lanes of this link.
     */
    public FosLink(final int linkNumber, final int sectionIndex, final int fromLane, final int toLane, final FosParser parser)
    {
        super(linkNumber);
        this.sectionIndex = sectionIndex;
        this.fromLane = fromLane;
        this.toLane = toLane;
        for (int laneIndex = fromLane; laneIndex <= toLane; laneIndex++)
        {
            this.lanes.add(parser.getLane(sectionIndex, laneIndex));
        }
    }
}
