package org.opentrafficsim.fosim.sim0mq;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import org.djunits.value.vdouble.scalar.Length;
import org.djutils.serialization.SerializationException;
import org.sim0mq.Sim0MQException;
import org.sim0mq.message.Sim0MQMessage;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class Sim0mqTesterOts
{

    /** */
    private ZContext context;

    /** the socket. */
    private ZMQ.Socket responder;

    /** */
    public Sim0mqTesterOts()
    {
        this.context = new ZContext(1);

        // Socket to talk to clients
        this.responder = this.context.createSocket(SocketType.REP);
        this.responder.bind("tcp://*:5556");

        new Worker().start();
    }

    public static void main(final String... args)
    {
        new Sim0mqTesterOts();
    }

    /** */
    private class Worker extends Thread
    {

        /**
         * Constructor.
         */
        public Worker()
        {
        }

        /** {@inheritDoc} */
        @Override
        public void run()
        {
            int messageId = 0;
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
            while (!Thread.currentThread().isInterrupted())
            {
                try
                {
                    // Wait for next request from the client
                    byte[] request = Sim0mqTesterOts.this.responder.recv(0);
                    Object[] message = Sim0MQMessage.decode(request).createObjectArray();
                    System.out.println("OTS received " + dateFormat.format(new Date()) + System.lineSeparator()
                            + Sim0MQMessage.print(message));

                    Object[] payload = new Object[1]; 
                    if (Math.random() > 0.5)
                    {
                        payload[0] = UUID.randomUUID().toString();
                    }
                    else
                    {
                        payload[0] = Length.instantiateSI(Math.random() * 10.0);
                    }
                    Sim0mqTesterOts.this.responder.send(Sim0MQMessage.encodeUTF8(true, "OTS_Fosim", "OTS", "Fosim",
                            "STEP_REPLY", messageId++, payload), 0);

                }
                catch (Sim0MQException | SerializationException e)
                {
                    e.printStackTrace();
                }
            }
            Sim0mqTesterOts.this.responder.close();
            Sim0mqTesterOts.this.context.destroy();
            Sim0mqTesterOts.this.context.close();
        }

    }

}
