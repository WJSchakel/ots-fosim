package org.opentrafficsim.fosim.simulator;

import org.opentrafficsim.core.dsol.OtsSimulatorInterface;

/**
 * Implementations return the worker thread synchronization.
 * @author wjschakel
 */
public interface OtsSimulatorInterfaceStep extends OtsSimulatorInterface
{

    /**
     * Returns the worker thread.
     * @return worker thread
     */
    public Thread getWorkerThread();
    
}
