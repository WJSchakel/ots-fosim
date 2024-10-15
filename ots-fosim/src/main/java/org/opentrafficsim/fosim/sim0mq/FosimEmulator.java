package org.opentrafficsim.fosim.sim0mq;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import org.djunits.unit.SpeedUnit;
import org.djunits.value.vdouble.scalar.Duration;
import org.djunits.value.vdouble.scalar.Speed;
import org.djutils.serialization.SerializationException;
import org.opentrafficsim.fosim.parameters.DefaultValue;
import org.opentrafficsim.fosim.parameters.DefaultValueAdapter;
import org.opentrafficsim.fosim.parameters.ParameterDefinitions;
import org.opentrafficsim.fosim.parameters.distributions.DistributionDefinitions;
import org.sim0mq.Sim0MQException;
import org.sim0mq.message.Sim0MQMessage;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

/**
 * Class acting as if it is Fosim for testing.
 * <p>
 * Copyright (c) 2023-2024 Delft University of Technology, PO Box 5, 2600 AA, Delft, the Netherlands. All rights reserved. <br>
 * BSD-style license. See <a href="https://opentrafficsim.org/docs/license.html">OpenTrafficSim License</a>.
 * </p>
 * @author <a href="https://github.com/wjschakel">Wouter Schakel</a>
 */
public class FosimEmulator
{

    /** Federation id to receive/sent messages. */
    private static final String FEDERATION = "Ots_Fosim";

    /** OTS id to receive/sent messages. */
    private static final String OTS = "Ots";

    /** Fosim id to receive/sent messages. */
    private static final String FOSIM = "Fosim";

    /** Endianness. */
    private static final Boolean BIG_ENDIAN = true;

    /** Port number. */
    private static final int PORT = 5556;

    /** Simulation speed. */
    private static final double SPEED = 20;

    /** Batch test (or normal). */
    private static final boolean BATCH = true;

    /**
     * Main method.
     * @param args command line arguments.
     * @throws SerializationException serialization exception
     * @throws Sim0MQException sim0mq exception
     * @throws IOException if received JSON could not be loaded as object
     */
    public static void main(final String... args) throws Sim0MQException, SerializationException, IOException
    {
        try (ZContext context = new ZContext(1))
        {
            ZMQ.Socket requester = context.createSocket(SocketType.REQ);
            requester.connect("tcp://*:" + PORT);
            System.out.println("Client is running");
            int messageId = 0;

            // Setup messages
            byte[] encodedMessage =
                    Sim0MQMessage.encodeUTF8(BIG_ENDIAN, FEDERATION, FOSIM, OTS, "DISTRIBUTIONS", messageId++, new Object[] {});
            requester.send(encodedMessage, 0);
            byte[] reply = requester.recv(0);
            Sim0MQMessage message = Sim0MQMessage.decode(reply);
            if ("DISTRIBUTIONS_REPLY".equals(message.getMessageTypeId()))
            {
                Object[] payload = message.createObjectArray();
                DistributionDefinitions distributions = loadString((String) payload[8], DistributionDefinitions.class);
                // System.out.println(Sim0MQMessage.print(payload));
            }
            else
            {
                throw new RuntimeException("Did not receive a DISTRIBUTIONS_REPLY on a DISTRIBUTIONS message.");
            }

            encodedMessage =
                    Sim0MQMessage.encodeUTF8(BIG_ENDIAN, FEDERATION, FOSIM, OTS, "PARAMETERS", messageId++, new Object[] {});
            requester.send(encodedMessage, 0);
            reply = requester.recv(0);
            message = Sim0MQMessage.decode(reply);
            if ("PARAMETERS_REPLY".equals(message.getMessageTypeId()))
            {
                Object[] payload = message.createObjectArray();
                ParameterDefinitions parameters = loadString((String) payload[8], ParameterDefinitions.class);
                // System.out.println(Sim0MQMessage.print(payload));
            }
            else
            {
                throw new RuntimeException("Did not receive a PARAMETERS_REPLY on a PARAMETERS message.");
            }

            String fosString = new String(
                    FosimEmulator.class.getResourceAsStream("/Terbregseplein_6.5_aangepast_param.fos").readAllBytes(),
                    StandardCharsets.UTF_8);
            encodedMessage = Sim0MQMessage.encodeUTF8(BIG_ENDIAN, FEDERATION, FOSIM, OTS, "SETUP", messageId++,
                    new Object[] {fosString});
            requester.send(encodedMessage, 0);
            reply = requester.recv(0);
            message = Sim0MQMessage.decode(reply);
            if ("SETUP_REPLY".equals(message.getMessageTypeId()))
            {
                // System.out.println("SETUP_REPLY received");
            }
            else
            {
                throw new RuntimeException("Did not receive a SETUP_REPLY on a SETUP message.");
            }

            if (BATCH)
            {
                int fromLane = 0;
                int toLane = 12;
                int detector = -1;
                Duration additionalTime = Duration.instantiateSI(600.0);
                encodedMessage =
                        Sim0MQMessage.encodeUTF8(BIG_ENDIAN, FEDERATION, FOSIM, OTS, "BATCH", messageId++, new Object[] {"PLM",
                                fromLane, toLane, detector, new Speed(50.0, SpeedUnit.KM_PER_HOUR), additionalTime});
                requester.send(encodedMessage, 0);
                reply = requester.recv(0);
                message = Sim0MQMessage.decode(reply);
                if ("BATCH_REPLY".equals(message.getMessageTypeId()))
                {
                    System.out.println("BATCH_REPLY received");
                }
                else
                {
                    throw new RuntimeException("Did not receive a SETUP_REPLY on a SETUP message.");
                }
                return;
            }

            long step = (long) (500 / SPEED);
            long t0 = -1;
            for (int i = 0; i < 7200; i++)
            {
                if (t0 > 0)
                {
                    long wait = t0 + (long) (i * step) - System.currentTimeMillis();
                    // System.out.println("wait: " + wait + "ms");
                    if (wait > 0)
                    {
                        try
                        {
                            Thread.sleep(wait);
                        }
                        catch (InterruptedException e)
                        {
                        }
                    }
                }

                // Step
                encodedMessage =
                        Sim0MQMessage.encodeUTF8(BIG_ENDIAN, FEDERATION, FOSIM, OTS, "STEP", messageId++, new Object[] {});
                // System.out.println("Encoded Sim0MQMessage: " + Arrays.toString(encodedMessage));
                requester.send(encodedMessage, 0);
                reply = requester.recv(0);
                message = Sim0MQMessage.decode(reply);
                if ("STEP_REPLY".equals(message.getMessageTypeId()))
                {
                    if (t0 < 0)
                    {
                        // set t0 after the first time step, to skip over initial setup time
                        t0 = System.currentTimeMillis() - step;
                    }
                    // System.out.println("STEP_REPLY received");
                    // Object[] payload = message.createObjectArray();
                    // System.out.println(Sim0MQMessage.print(payload));
                }

                // Vehicles info
                encodedMessage =
                        Sim0MQMessage.encodeUTF8(BIG_ENDIAN, FEDERATION, FOSIM, OTS, "VEHICLES", messageId++, new Object[] {});
                // System.out.println("Encoded Sim0MQMessage: " + Arrays.toString(encodedMessage));
                requester.send(encodedMessage, 0);

                reply = requester.recv(0);
                message = Sim0MQMessage.decode(reply);
                if ("VEHICLE_REPLY".equals(message.getMessageTypeId()))
                {
                    // System.out.println("VEHICLE_REPLY received");
                    // Object[] payload = message.createObjectArray();
                    // System.out.println(payload[8] + " vehicles");
                }

                if (i == 602)
                {
                    encodedMessage = Sim0MQMessage.encodeUTF8(BIG_ENDIAN, FEDERATION, FOSIM, OTS, "DETECTOR", messageId++,
                            new Object[] {2, 3, 1, "COUNT"});
                    requester.send(encodedMessage, 0);
                    reply = requester.recv(0);
                    message = Sim0MQMessage.decode(reply);
                    if ("DETECTOR_REPLY".equals(message.getMessageTypeId()))
                    {
                        Object[] payload = message.createObjectArray();
                        System.out.println("Count at cross-section 2, lane 3, period 1 is " + payload[8] + " vehicles");
                    }

                    encodedMessage = Sim0MQMessage.encodeUTF8(BIG_ENDIAN, FEDERATION, FOSIM, OTS, "DETECTOR", messageId++,
                            new Object[] {2, 3, 1, "SUM_RECIPROCAL_SPEED"});
                    requester.send(encodedMessage, 0);
                    reply = requester.recv(0);
                    message = Sim0MQMessage.decode(reply);
                    if ("DETECTOR_REPLY".equals(message.getMessageTypeId()))
                    {
                        Object[] payload = message.createObjectArray();
                        System.out.println("Reciprocal speed at cross-section 2, lane 3, period 1 is " + payload[8] + " s/m");
                    }

                    encodedMessage = Sim0MQMessage.encodeUTF8(BIG_ENDIAN, FEDERATION, FOSIM, OTS, "DETECTOR", messageId++,
                            new Object[] {2, 3, 1, "SUM_TRAVEL_TIME"});
                    requester.send(encodedMessage, 0);
                    reply = requester.recv(0);
                    message = Sim0MQMessage.decode(reply);
                    if ("DETECTOR_REPLY".equals(message.getMessageTypeId()))
                    {
                        Object[] payload = message.createObjectArray();
                        System.out.println("Sum of travel time at cross-section 2, lane 3, period 1 is " + payload[8] + " s");
                    }
                }
            }

            requester.send(Sim0MQMessage.encodeUTF8(BIG_ENDIAN, FEDERATION, FOSIM, OTS, "STOP", messageId++, new Object[] {}),
                    0);
            reply = requester.recv(0);
            message = Sim0MQMessage.decode(reply);
            if ("STOP_REPLY".equals(message.getMessageTypeId()))
            {

            }
            else
            {
                throw new RuntimeException("Did not receive a STOP_REPLY on a STOP message.");
            }

            requester.send(
                    Sim0MQMessage.encodeUTF8(BIG_ENDIAN, FEDERATION, FOSIM, OTS, "TERMINATE", messageId++, new Object[] {}), 0);
            reply = requester.recv(0);
            message = Sim0MQMessage.decode(reply);
            if ("TERMINATE_REPLY".equals(message.getMessageTypeId()))
            {

            }
            else
            {
                throw new RuntimeException("Did not receive a TERMINATE_REPLY on a TERMINATE message.");
            }

            requester.close();
            context.destroy();
            context.close();

            System.out.println("Fosim terminated");
        }
    }

    /**
     * Read string in to object.
     * @param <T> type of object.
     * @param json JSON string.
     * @param clazz class of type to read.
     * @return object read from JSON string.
     * @throws IOException when unable to read from string.
     */
    @SuppressWarnings("unchecked")
    public static <T> T loadString(final String json, final Class<T> clazz) throws IOException
    {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(DefaultValue.class, new DefaultValueAdapter());
        Gson gson = builder.create();
        JsonReader reader = new JsonReader(new StringReader(json));
        return (T) gson.fromJson(reader, clazz);
    }

}
