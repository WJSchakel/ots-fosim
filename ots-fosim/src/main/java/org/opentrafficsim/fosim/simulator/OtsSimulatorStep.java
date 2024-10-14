package org.opentrafficsim.fosim.simulator;

import java.io.Serializable;

import org.opentrafficsim.core.dsol.OtsSimulator;

/**
 * Extends OtsSimulator by also implementing OtsSimulatorInterfaceStep.
 * @author wjschakel
 */
public class OtsSimulatorStep extends OtsSimulator implements OtsSimulatorInterfaceStep
{

    /** */
    private static final long serialVersionUID = 20241014L;

    /**
     * Constructor.
     * @param simulatorId simulator id
     */
    public OtsSimulatorStep(final Serializable simulatorId)
    {
        super(simulatorId);
    }

    /** {@inheritDoc} */
    @Override
    public Thread getWorkerThread()
    {
        return this.worker;
    }

}
