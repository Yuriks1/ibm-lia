import backend.BackendApp;
import com.ibm.mq.*;
import com.ibm.mq.constants.MQConstants;
import com.ibm.msg.client.jms.JmsConnectionFactory;
import com.ibm.msg.client.jms.JmsFactoryFactory;
import com.ibm.msg.client.wmq.WMQConstants;

import javax.jms.*;
import java.io.IOException;


public class JmsPutGet {

    private static final int RECEIVE_DELAY_SECONDS = 2;
    private static int status = 1;
    private static final String HOST = "localhost";
    private static final int PORT = 1414;
    private static final String CHANNEL = "DEV.APP.SVRCONN";
    private static final String QMGR = "QMLOCAL";
    private static final String APP_USER = "app";
    private static final String APP_PASSWORD = "passw0rd";
    private static final String QUEUE_NAME = "TEST.QUEUE.LOCAL";
    private static final String REPLY_TO = "DEV.QUEUE.1";

    private static final int SEND_DELAY_SECONDS = 2;


    public static void main(String[] args) {

        JMSContext context;
        Destination destination;
        JMSProducer producer;
        JMSConsumer consumer;
        Destination replyTo;

        try {
            JmsFactoryFactory ff = JmsFactoryFactory.getInstance(WMQConstants.WMQ_PROVIDER);
            JmsConnectionFactory cf = ff.createConnectionFactory();

            cf.setStringProperty(WMQConstants.WMQ_HOST_NAME, HOST);
            cf.setIntProperty(WMQConstants.WMQ_PORT, PORT);
            cf.setStringProperty(WMQConstants.WMQ_CHANNEL, CHANNEL);
            cf.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT);
            cf.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, QMGR);
            cf.setStringProperty(WMQConstants.WMQ_APPLICATIONNAME, "JmsPutGet (JMS)");
            cf.setBooleanProperty(WMQConstants.USER_AUTHENTICATION_MQCSP, true);
            cf.setStringProperty(WMQConstants.USERID, APP_USER);
            cf.setStringProperty(WMQConstants.PASSWORD, APP_PASSWORD);

            context = cf.createContext();
            destination = context.createQueue("queue:///" + QUEUE_NAME);
            replyTo = context.createQueue("queue:///" + REPLY_TO);

            TextMessage message = null;
            for (int i = 0; i < 1; i++) {

                long uniqueNumber = System.currentTimeMillis() % 1000;
                message = context.createTextMessage("My unique number " + uniqueNumber);
                producer = context.createProducer();
                producer.setJMSReplyTo(replyTo);
                producer.send(destination, message);
                System.out.println("Sent message to the queue : " + message.getText()
                        + "\nMessage Id " + message.getJMSMessageID()
                        + "\nDestination " + message.getJMSDestination()
                        + "\nReplyTo " + message.getJMSReplyTo());
                System.out.print("---------------------------------------------------");
                System.out.println();
                //System.out.println("Waiting " + SEND_DELAY_SECONDS + " seconds before sending next message");
                System.out.println();
                Thread.sleep(SEND_DELAY_SECONDS * 1000);
            }

            BackendApp.main(args);


            MQQueueManager qmgr = new MQQueueManager(QMGR);
            MQGetMessageOptions gmo = new MQGetMessageOptions();
            gmo.waitInterval = RECEIVE_DELAY_SECONDS * 1000;


            gmo.options = MQConstants.MQGMO_NO_SYNCPOINT | MQConstants.MQGMO_WAIT;


            MQQueue queue = qmgr.accessQueue(REPLY_TO, MQConstants.MQOO_INPUT_AS_Q_DEF
                    | MQConstants.MQOO_FAIL_IF_QUIESCING | MQConstants.MQOO_INQUIRE);
            MQMessage mqMessage = new MQMessage();
            queue.get(mqMessage, gmo);

            if (mqMessage.getStringProperty("JMSCorrelationID").equals(message.getJMSMessageID())) {

                System.out.println("\nReceived same message from the queue " + REPLY_TO
                        + " :\n" + mqMessage.getStringProperty("JMSCorrelationID")
                        + "\nMessage text : " + mqMessage.readStringOfByteLength(mqMessage.getMessageLength()));
            } else {
                System.out.println("\nReceived different message from the queue " + REPLY_TO + " :\n"
                        + mqMessage.getStringProperty("JMSCorrelationID"));
            }
            queue.close();
            Thread.sleep(RECEIVE_DELAY_SECONDS * 1000);


            qmgr.disconnect();
            System.out.println("Disconnected from IBM MQ");

        } catch (JMSException | InterruptedException jmsex) {
            recordFailure(jmsex);
        } catch (MQException | IOException e) {
            throw new RuntimeException(e);
        }

        System.exit(status);

    }


    private static void recordFailure(Exception ex) {
        if (ex != null) {
            if (ex instanceof JMSException) {
                processJMSException((JMSException) ex);
            } else {
                System.out.println(ex);
            }
        }
        System.out.println("FAILURE");
        status = -1;
    }


    private static void processJMSException(JMSException jmsex) {
        System.out.println(jmsex);
        Throwable innerException = jmsex.getLinkedException();
        if (innerException != null) {
            System.out.println("Inner exception(s):");
        }
        while (innerException != null) {
            System.out.println(innerException);
            innerException = innerException.getCause();
        }
    }

}