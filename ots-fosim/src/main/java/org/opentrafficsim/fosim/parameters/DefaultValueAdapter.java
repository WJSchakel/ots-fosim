package org.opentrafficsim.fosim.parameters;

import java.lang.reflect.Type;

import org.opentrafficsim.fosim.parameters.distributions.DistributionValue;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * Adapter for default values, which are either {@code Scalar} or {@code Distribution}.
 * <p>
 * Copyright (c) 2023-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
 */
@Deprecated
public class DefaultValueAdapter implements JsonSerializer<DefaultValue>, JsonDeserializer<DefaultValue>
{

    /** Delegate Gson for default deserialization of a {@code DistributionValue}. */
    private final static Gson GSON = new Gson();

    /** {@inheritDoc} */
    @Override
    public JsonElement serialize(final DefaultValue src, final Type typeOfSrc, final JsonSerializationContext context)
    {
        if (src instanceof Scalar)
        {
            return context.serialize(((Scalar) src).value(), Double.class);
        }
        return context.serialize(src, DistributionValue.class);
    }

    /** {@inheritDoc} */
    @Override
    public DefaultValue deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context)
            throws JsonParseException
    {
        if (json.isJsonPrimitive())
        {
            return new Scalar(json.getAsDouble());
        }
        return GSON.fromJson(json, DistributionValue.class);
    }

}
