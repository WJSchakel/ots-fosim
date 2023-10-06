package org.opentrafficsim.fosim.sim0mq;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.djutils.serialization.SerializationException;
import org.sim0mq.Sim0MQException;
import org.sim0mq.message.Sim0MQMessage;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class Sim0mqTesterFosim
{

    public static void main(final String... args) throws Sim0MQException, SerializationException, InterruptedException
    {
        ZContext context = new ZContext(1);
        System.out.println("Connecting to server...");

        ZMQ.Socket requester = context.createSocket(SocketType.REQ);
        requester.connect("tcp://*:5556");

        int messageId = 0;
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

        for (int i = 0; i < 30; i++)
        {
            requester.send(Sim0MQMessage.encodeUTF8(true, "OTS_Fosim", "Fosim", "OTS", "STEP", messageId++, new Object[] {}),
                    0);

            byte[] reply = requester.recv(0);
            Object[] replyMessage = Sim0MQMessage.decode(reply).createObjectArray();
            System.out.println("Fosim received " + dateFormat.format(new Date()) + System.lineSeparator()
                    + Sim0MQMessage.print(replyMessage));
            
            Thread.sleep(500);
        }

        requester.close();
        context.destroy();
        context.close();
    }

}
