package org.opentrafficsim.fosim.parameters.data;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.Map.Entry;

import org.opentrafficsim.fosim.parameters.distributions.DistributionType;

import com.google.gson.Gson;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

/**
 * Adapter for default values, which are either {@code Scalar} or {@code Distribution}.
 * <p>
 * Copyright (c) 2023-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
 */
public class ValueAdapter implements JsonSerializer<ValueData>, JsonDeserializer<ValueData>
{

    /** Delegate Gson for default deserialization of a {@code DistributionValue}. */
    private final static Gson GSON = new Gson();

    /** {@inheritDoc} */
    @Override
    public JsonElement serialize(final ValueData src, final Type typeOfSrc, final JsonSerializationContext context)
    {
        if (src instanceof ScalarData)
        {
            return context.serialize(((ScalarData) src).value(), Double.class);
        }
        DistributionData distribution = (DistributionData) src;
        JsonObject obj = new JsonObject();
        obj.addProperty("type", distribution.type.toString());
        for (Entry<String, Double> entry : distribution.distributionParameters.entrySet())
        {
            obj.addProperty(entry.getKey(), entry.getValue());
        }
        return obj;
    }

    /** {@inheritDoc} */
    @Override
    public ValueData deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context)
            throws JsonParseException
    {
        if (json.isJsonPrimitive())
        {
            return new ScalarData(json.getAsDouble());
        }
        DistributionData distribution = new DistributionData();
        JsonObject obj = json.getAsJsonObject();
        distribution.type = GSON.fromJson(obj.get("type"), DistributionType.class);
        obj.remove("type");
        distribution.distributionParameters =
                GSON.fromJson(obj.get("values"), TypeToken.getParameterized(Map.class, String.class, Double.class).getType());
        return distribution;
    }

}
