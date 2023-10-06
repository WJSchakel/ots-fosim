package org.opentrafficsim.fosim.sim0mq;

import java.util.Arrays;

import org.djutils.serialization.SerializationException;
import org.sim0mq.Sim0MQException;
import org.sim0mq.message.Sim0MQMessage;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

/**
 * Class acting as if it is Fosim for testing.
 * @author wjschakel
 */
public class TechnicalDemoFosim
{

    /** Federation id to receive/sent messages. */
    private static final String FEDERATION = "OTS_Fosim";

    /** OTS id to receive/sent messages. */
    private static final String OTS = "OTS";

    /** Fosim id to receive/sent messages. */
    private static final String FOSIM = "Fosim";

    /** Endianness. */
    private static final Boolean BIG_ENDIAN = true;

    /** Port number. */
    private static final int PORT = 5556;

    /**
     * Main method.
     * @param args String[]; command line arguments.
     * @throws SerializationException
     * @throws Sim0MQException
     */
    public static void main(String... args) throws Sim0MQException, SerializationException
    {
        ZContext context = new ZContext(1);
        ZMQ.Socket requester = context.createSocket(SocketType.REQ);
        requester.connect("tcp://*:" + PORT);
        System.out.println("Client is running");

        int messageId = 0;
        for (int i = 0; i < 7200; i++)
        {
            byte[] encodedMessage =
                    Sim0MQMessage.encodeUTF8(BIG_ENDIAN, FEDERATION, FOSIM, OTS, "STEP", messageId++, new Object[] {});
            System.out.println("Encoded Sim0MQMessage: " + Arrays.toString(encodedMessage));
            requester.send(encodedMessage, 0);

            byte[] reply = requester.recv(0);
            Sim0MQMessage message = Sim0MQMessage.decode(reply);
            if ("STEP_REPLY".equals(message.getMessageTypeId()))
            {
                // Object[] payload = message.createObjectArray();
                // System.out.println(Sim0MQMessage.print(payload));
            }
        }

        requester.send(Sim0MQMessage.encodeUTF8(BIG_ENDIAN, FEDERATION, FOSIM, OTS, "STOP", messageId++, new Object[] {}), 0);

        requester.close();
        context.destroy();
        context.close();

        System.out.println("Fosim terminated");
    }

}
