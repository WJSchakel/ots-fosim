package org.opentrafficsim.fosim.sim0mq;

import java.io.IOException;
import java.io.StringReader;

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
 * @author wjschakel
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

    /**
     * Main method.
     * @param args String[]; command line arguments.
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
                //System.out.println(Sim0MQMessage.print(payload));
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
                //System.out.println(Sim0MQMessage.print(payload));
            }
            else
            {
                throw new RuntimeException("Did not receive a PARAMETERS_REPLY on a PARAMETERS message.");
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
            }

            requester.send(Sim0MQMessage.encodeUTF8(BIG_ENDIAN, FEDERATION, FOSIM, OTS, "STOP", messageId++, new Object[] {}),
                    0);

            requester.close();
            context.destroy();
            context.close();

            System.out.println("Fosim terminated");
        }
    }

    /**
     * Read string in to object.
     * @param <T> type of object.
     * @param json String; JSON string.
     * @param clazz Class&lt;T&gt;; class of type to read.
     * @return T; object read from JSON string.
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
