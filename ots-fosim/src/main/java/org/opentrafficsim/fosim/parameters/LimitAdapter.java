package org.opentrafficsim.fosim.parameters;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * Adapter for parameter limits of type {@code Limit}.
 * <p>
 * Copyright (c) 2023-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
 */
public class LimitAdapter implements JsonSerializer<Limit>, JsonDeserializer<Limit>
{

    @Override
    public JsonElement serialize(final Limit src, final  Type typeOfSrc, final JsonSerializationContext context)
    {
        if (src.parameter == null)
        {
            return context.serialize(src.value, Double.class);
        }
        return context.serialize(src.parameter, String.class);
    }
   
    @Override
    public Limit deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context)
            throws JsonParseException
    {
        Limit limit = new Limit();
        if (json.getAsJsonPrimitive().isString())
        {
            limit.parameter = json.getAsString();
        }
        else
        {
            limit.value = json.getAsDouble();
        }
        return limit;
    }

}
