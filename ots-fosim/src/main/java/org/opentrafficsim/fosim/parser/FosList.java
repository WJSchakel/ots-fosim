package org.opentrafficsim.fosim.parser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This list wrapper allows multiple threads to set data in the array using the {@code set(index, element)} method. This
 * method also assures the array will be of sufficient size, filling it with {@code null} as required. Using this, multiple
 * threads can fill the array in parallel.
 * <p>
 * Copyright (c) 2023-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
 * @param <T> element type.
 */
class FosList<T> implements Iterable<T>
{
    /** Wrapped list. */
    private final List<T> list = new ArrayList<>();

    /**
     * Set the element at the specified index. Increase size of array if required. This will fill the array with
     * {@code null}.
     * @param index int; index to set the element.
     * @param t T; element to set.
     * @return T; the element previously at the specified position.
     */
    public synchronized T set(final int index, final T t)
    {
        while (index >= size())
        {
            this.list.add(null);
        }
        return this.list.set(index, t);
    }

    /**
     * Returns the size;
     * @return int; size.
     */
    public int size()
    {
        return this.list.size();
    }

    /**
     * Returns the value at index.
     * @param index int; index.
     * @return T; value at index.
     */
    public T get(final int index)
    {
        return this.list.get(index);
    }

    /** {@inhertitDoc} */
    @Override
    public Iterator<T> iterator()
    {
        return this.list.iterator();
    }

    /**
     * Returns whether the list is fully defined, i.e. contains no {@code null}.
     * @return boolean; whether the list is fully defined.
     */
    public boolean isDefined()
    {
        return !this.list.contains(null);
    }

    /** {@inheritDoc} */
    @Override
    public String toString()
    {
        return this.list.toString();
    }
}