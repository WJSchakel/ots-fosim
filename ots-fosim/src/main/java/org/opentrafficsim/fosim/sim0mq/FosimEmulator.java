package org.opentrafficsim.fosim.sim0mq;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

import org.djunits.unit.SpeedUnit;
import org.djunits.value.vdouble.scalar.Duration;
import org.djunits.value.vdouble.scalar.Length;
import org.djunits.value.vdouble.scalar.Speed;
import org.djunits.value.vfloat.matrix.FloatDurationMatrix;
import org.djunits.value.vfloat.matrix.FloatLengthMatrix;
import org.djunits.value.vfloat.vector.FloatDurationVector;
import org.djunits.value.vfloat.vector.FloatLengthVector;
import org.djunits.value.vfloat.vector.base.FloatVector;
import org.djutils.serialization.SerializationException;
import org.opentrafficsim.fosim.parameters.DefaultValue;
import org.opentrafficsim.fosim.parameters.DefaultValueAdapter;
import org.opentrafficsim.fosim.parameters.Limit;
import org.opentrafficsim.fosim.parameters.LimitAdapter;
import org.opentrafficsim.fosim.parameters.distributions.DistributionDefinitions;
import org.opentrafficsim.fosim.sim0mq.StopCriterion.BatchStatus;
import org.opentrafficsim.fosim.sim0mq.trace.Trace;
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
    private static final boolean BATCH = false;

    /** Configuration file. */
    private static final String CONFIGURATION = "Config 2.fos";

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
                // Object[] payload = message.createObjectArray();
                // ParameterDefinitions parameters = loadString((String) payload[8], ParameterDefinitions.class);
                // System.out.println(Sim0MQMessage.print(payload));
            }
            else
            {
                throw new RuntimeException("Did not receive a PARAMETERS_REPLY on a PARAMETERS message.");
            }

            // Trace
            encodedMessage =
                    Sim0MQMessage.encodeUTF8(BIG_ENDIAN, FEDERATION, FOSIM, OTS, "TRACE_FILES", messageId++, new Object[] {});
            requester.send(encodedMessage, 0);
            reply = requester.recv(0);
            message = Sim0MQMessage.decode(reply);
            if ("TRACE_FILES_REPLY".equals(message.getMessageTypeId()))
            {
                Object[] payload = message.createObjectArray();
                System.out.println("TRACE_FILES_REPLY with " + (payload.length - 8) + " fields.");
            }
            else
            {
                throw new RuntimeException("Did not receive a TRACE_FILES_REPLY on a TRACE_FILES message.");
            }
            for (Trace trace : Trace.values())
            {
                encodedMessage = Sim0MQMessage.encodeUTF8(BIG_ENDIAN, FEDERATION, FOSIM, OTS, "TRACE_ACTIVE", messageId++,
                        new Object[] {trace.getInfo().id(), true});
                requester.send(encodedMessage, 0);
                reply = requester.recv(0);
                message = Sim0MQMessage.decode(reply);
                if ("TRACE_ACTIVE_REPLY".equals(message.getMessageTypeId()))
                {
                    System.out.println("TRACE_ACTIVE_REPLY for " + trace.getInfo().id() + " received.");
                }
                else
                {
                    throw new RuntimeException("Did not receive a TRACE_ACTIVE_REPLY on a TRACE_ACTIVE message.");
                }
            }
            encodedMessage = Sim0MQMessage.encodeUTF8(BIG_ENDIAN, FEDERATION, FOSIM, OTS, "TRACE_VEHICLES_STEP", messageId++,
                    new Object[] {Duration.ofSI(0.5)});
            requester.send(encodedMessage, 0);
            reply = requester.recv(0);
            message = Sim0MQMessage.decode(reply);
            if ("TRACE_VEHICLES_STEP_REPLY".equals(message.getMessageTypeId()))
            {
                System.out.println("TRACE_VEHICLES_STEP_REPLY received.");
            }
            else
            {
                throw new RuntimeException("Did not receive a TRACE_VEHICLES_STEP_REPLY on a TRACE_VEHICLES_STEP message.");
            }

            String fosString = new String(FosimEmulator.class.getResourceAsStream("/" + CONFIGURATION).readAllBytes(),
                    StandardCharsets.UTF_8);
            encodedMessage = Sim0MQMessage.encodeUTF8(BIG_ENDIAN, FEDERATION, FOSIM, OTS, "SETUP", messageId++,
                    new Object[] {fosString});
            requester.send(encodedMessage, 0);
            reply = requester.recv(0);
            message = Sim0MQMessage.decode(reply);
            if ("SETUP_REPLY".equals(message.getMessageTypeId()))
            {
                // System.out.println("SETUP_REPLY received");
                String exception = (String) message.createObjectArray()[8];
                if (exception != null)
                {
                    System.err.println(exception);
                }
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
                Duration additionalTime = Duration.ofSI(600.0);
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

                BatchStatus batchStatus = BatchStatus.RUNNING;
                while (BatchStatus.RUNNING.equals(batchStatus))
                {
                    // Batch step
                    encodedMessage = Sim0MQMessage.encodeUTF8(BIG_ENDIAN, FEDERATION, FOSIM, OTS, "BATCH_STEP", messageId++,
                            new Object[] {});
                    // System.out.println("Encoded Sim0MQMessage: " + Arrays.toString(encodedMessage));
                    requester.send(encodedMessage, 0);
                    reply = requester.recv(0);
                    message = Sim0MQMessage.decode(reply);
                    if ("BATCH_STEP_REPLY".equals(message.getMessageTypeId()))
                    {
                        Object[] payload = message.createObjectArray();
                        batchStatus = BatchStatus.valueOf((String) payload[8]);
                    }
                }
                System.out.println("Stopped with batch status " + batchStatus);
                return;
            }

            long step = (long) (500 / SPEED);
            long t0 = -1;
            for (int i = 0; i < 3600 * 4; i++)
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
                if ("VEHICLES_REPLY".equals(message.getMessageTypeId()))
                {
                    // System.out.println("VEHICLE_REPLY received");
                    // Object[] payload = message.createObjectArray();
                    // System.out.println(payload[8] + " vehicles");
                }

                // Check various data messages
                if (i == 301)
                {
                    // Manual test: comment out this line & check that at 601 the same amount of data is returned as at 301+601
                    // messageId = getTraceData(requester, messageId);
                }
                else if (i == 601)
                {
                    messageId = getTraceData(requester, messageId);

                    encodedMessage = Sim0MQMessage.encodeUTF8(BIG_ENDIAN, FEDERATION, FOSIM, OTS, "CONTOUR", messageId++,
                            new Object[] {Duration.ZERO, Duration.ofSI(60.0), Duration.ofSI(300.0),
                                    Length.ZERO, Length.ofSI(100.0), Length.ofSI(3500.0)});
                    long t = System.currentTimeMillis();
                    requester.send(encodedMessage, 0);
                    reply = requester.recv(0);
                    message = Sim0MQMessage.decode(reply);
                    t = System.currentTimeMillis() - t;
                    if ("CONTOUR_REPLY".equals(message.getMessageTypeId()))
                    {
                        Object[] payload = message.createObjectArray();
                        int[] lanes = Arrays.asList((Integer[]) payload[8]).stream().mapToInt(k -> k).toArray();
                        System.out.println("Received contour data for lanes " + Arrays.toString(lanes) + " in " + t + "ms");
                        for (int j = 0; j < lanes.length; j++)
                        {
                            FloatLengthMatrix distance = (FloatLengthMatrix) payload[9 + j * 2];
                            FloatDurationMatrix time = (FloatDurationMatrix) payload[10 + j * 2];
                            System.out.println("Lane " + lanes[j] + ": distance=" + distance.rows() + "x" + distance.cols()
                                    + ", time=" + time.rows() + "x" + time.cols());
                        }
                    }

                    encodedMessage = Sim0MQMessage.encodeUTF8(BIG_ENDIAN, FEDERATION, FOSIM, OTS, "TRAJECTORIES", messageId++,
                            new Object[] {Duration.ZERO, Duration.ofSI(541.0), Length.ZERO,
                                    Length.ofSI(3500.0), 20});
                    t = System.currentTimeMillis();
                    requester.send(encodedMessage, 0);
                    reply = requester.recv(0);
                    message = Sim0MQMessage.decode(reply);
                    t = System.currentTimeMillis() - t;
                    if ("TRAJECTORIES_REPLY".equals(message.getMessageTypeId()))
                    {
                        Object[] payload = message.createObjectArray();
                        int n = (int) payload[8];
                        System.out.println("Received trajectory data for " + n + " GTUs in " + t + "ms");
                        for (int j = 0; j < n; j++)
                        {
                            FloatDurationVector distance = (FloatDurationVector) payload[9 + j * 3];
                            FloatLengthVector time = (FloatLengthVector) payload[10 + j * 3];
                            Integer[] lane = (Integer[]) payload[11 + j * 3];
                            System.out.println("GTU " + j + ": distance=" + distance.size() + ", time=" + time.size()
                                    + ", lane=" + lane.length);
                        }
                    }
                }
                else if (i == 1201)
                {
                    for (int crossSection = 0; crossSection < 20; crossSection++)
                    {
                        for (int lane = 0; lane < 4; lane++)
                        {
                            for (int period = 0; period < 2; period++)
                            {
                                encodedMessage = Sim0MQMessage.encodeUTF8(BIG_ENDIAN, FEDERATION, FOSIM, OTS, "DETECTOR",
                                        messageId++, new Object[] {crossSection, lane, period, "COUNT"});
                                requester.send(encodedMessage, 0);
                                reply = requester.recv(0);
                                message = Sim0MQMessage.decode(reply);
                                if ("DETECTOR_REPLY".equals(message.getMessageTypeId()))
                                {
                                    Object[] payload = message.createObjectArray();
                                    System.out.println("Count at cross-section " + crossSection + ", lane " + lane + ", period "
                                            + period + " is " + payload[8] + " vehicles");
                                }

                                encodedMessage = Sim0MQMessage.encodeUTF8(BIG_ENDIAN, FEDERATION, FOSIM, OTS, "DETECTOR",
                                        messageId++, new Object[] {crossSection, lane, period, "SUM_RECIPROCAL_SPEED"});
                                requester.send(encodedMessage, 0);
                                reply = requester.recv(0);
                                message = Sim0MQMessage.decode(reply);
                                if ("DETECTOR_REPLY".equals(message.getMessageTypeId()))
                                {
                                    Object[] payload = message.createObjectArray();
                                    System.out.println("Reciprocal speed at cross-section " + crossSection + ", lane " + lane
                                            + ", period " + period + " is " + payload[8] + " s/m");
                                }

                                encodedMessage = Sim0MQMessage.encodeUTF8(BIG_ENDIAN, FEDERATION, FOSIM, OTS, "DETECTOR",
                                        messageId++, new Object[] {crossSection, lane, period, "SUM_TRAVEL_TIME"});
                                requester.send(encodedMessage, 0);
                                reply = requester.recv(0);
                                message = Sim0MQMessage.decode(reply);
                                if ("DETECTOR_REPLY".equals(message.getMessageTypeId()))
                                {
                                    Object[] payload = message.createObjectArray();
                                    System.out.println("Sum of travel time at cross-section " + crossSection + ", lane " + lane
                                            + ", period " + period + " is " + payload[8] + " s");
                                }
                            }
                        }
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
        }
        System.out.println("Fosim terminated");
    }

    /**
     * Displays information on currently received trace data.
     * @param requester socket
     * @param messageId message id
     * @return increased message id
     * @throws Sim0MQException exception
     * @throws SerializationException exception
     */
    private static int getTraceData(final ZMQ.Socket requester, final int messageId)
            throws Sim0MQException, SerializationException
    {
        int msgId = messageId;
        for (Trace trace : Trace.values())
        {
            byte[] encodedMessage = Sim0MQMessage.encodeUTF8(BIG_ENDIAN, FEDERATION, FOSIM, OTS, "TRACE_GET", msgId++,
                    new Object[] {trace.getInfo().id()});
            long t = System.currentTimeMillis();
            requester.send(encodedMessage, 0);
            byte[] reply = requester.recv(0);
            Sim0MQMessage message = Sim0MQMessage.decode(reply);
            t = System.currentTimeMillis() - t;
            if ("TRACE_GET_REPLY".equals(message.getMessageTypeId()))
            {
                Object[] payload = message.createObjectArray();
                short m = (short) ((short) payload[7] - 1);
                System.out.println("Received trace data for " + payload[8] + " with " + m + " fields in " + t + "ms");
                for (int j = 0; j < m; j++)
                {
                    Object obj = payload[9 + j];
                    int length;
                    if (obj instanceof int[] arr)
                    {
                        length = arr.length;
                    }
                    else if (obj instanceof double[] arr)
                    {
                        length = arr.length;
                    }
                    if (obj instanceof Integer[] arr)
                    {
                        length = arr.length;
                    }
                    else if (obj instanceof Double[] arr)
                    {
                        length = arr.length;
                    }
                    else
                    {
                        FloatVector<?, ?, ?> vector = (FloatVector<?, ?, ?>) obj;
                        if ((trace.equals(Trace.ACCELERATION_CHANGE) && j == 0) || (trace.equals(Trace.DETECTION) && j == 2)
                                || (trace.equals(Trace.TRAVEL_TIME) && j == 2) || (trace.equals(Trace.LANE_CHANGE) && j == 0)
                                || (trace.equals(Trace.OD_TRAVEL_TIME) && j == 0) || (trace.equals(Trace.VEHICLES) && j == 0))
                        {
                            // Print min and max time in data
                            float tMin = Float.POSITIVE_INFINITY;
                            float tMax = Float.NEGATIVE_INFINITY;
                            for (int k = 0; k < vector.size(); k++)
                            {
                                tMin = Math.min(tMin, vector.getSI(k));
                                tMax = Math.max(tMax, vector.getSI(k));
                            }
                            System.out.println(String.format(Locale.US, " Time in data: [%.2f, %.2f]", tMin, tMax));
                        }
                        length = vector.size();
                    }
                    System.out.println(" Payload field " + (j + 1) + " has length " + length);
                }
            }
        }
        return msgId;
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
        builder.registerTypeAdapter(Limit.class, new LimitAdapter());
        Gson gson = builder.create();
        JsonReader reader = new JsonReader(new StringReader(json));
        return (T) gson.fromJson(reader, clazz);
    }

}
