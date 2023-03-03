import com.ibm.mq.*;
import com.ibm.mq.constants.MQConstants;
import com.ibm.msg.client.jms.JmsConnectionFactory;
import com.ibm.msg.client.jms.JmsFactoryFactory;
import com.ibm.msg.client.wmq.WMQConstants;

import javax.jms.Destination;
import javax.jms.JMSContext;
import javax.jms.JMSProducer;
import javax.jms.TextMessage;
import java.util.Scanner;


public class JmsPutGet {
    static Scanner scanner = new Scanner(System.in);
    private static final String HOST = "localhost";
    private static final int PORT = 1414;
    private static final String CHANNEL = "DEV.APP.SVRCONN";
    private static final String QMGR = "QMLOCAL";
    private static final String APP_USER = "app";
    private static final String APP_PASSWORD = "passw0rd";
    private static final String QUEUE_NAME = "TEST.QUEUE.LOCAL";
    private static final String REPLY_TO = "DEV.QUEUE.1";
    private static final int SEND_DELAY_SECONDS = 5;
    private static final int RECEIVE_DELAY_SECONDS = 30;


    public static void main(String[] args) {

        System.out.println((char) (27) + "[34m");

        JMSContext context;
        Destination destination;
        JMSProducer producer;
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
            int MAX_MESSAGES;




            while (true) {

                System.out.println("Enter number of messages to send (1-10) or 0 to exit");
                try {
                    MAX_MESSAGES = scanner.nextInt();
                    if (MAX_MESSAGES > 0 && MAX_MESSAGES <= 10) {
                        break;
                    } else if (MAX_MESSAGES == 0) {
                        System.exit(0);
                    } else {
                        System.out.println("Invalid number of messages");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid number of messages");
                }
            }


            for (int i = 0; i < MAX_MESSAGES; i++) {
                long uniqueNumber = System.currentTimeMillis() % 1000; // Unique number to identify each message
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


            System.out.println("Waiting " + RECEIVE_DELAY_SECONDS + " seconds for receiving message!\n");
            MQQueueManager qmgr = new MQQueueManager(QMGR);
            MQGetMessageOptions gmo = new MQGetMessageOptions();
            gmo.waitInterval = RECEIVE_DELAY_SECONDS * 1000;

            MQQueue queue = qmgr.accessQueue(REPLY_TO, MQConstants.MQOO_INPUT_AS_Q_DEF );


            long start = System.currentTimeMillis();
            long end = start + 30 * 1000;
            while (System.currentTimeMillis() < end) {
                Thread.sleep(SEND_DELAY_SECONDS * 1000);
                MQMessage mqMessage = new MQMessage();
                queue.get(mqMessage, gmo);
                String messageText = mqMessage.readStringOfByteLength(mqMessage.getMessageLength());

                if (mqMessage.getStringProperty("JMSCorrelationID").equals(message.getJMSMessageID())) {

                    System.out.println("\nReceived same message from the queue " + REPLY_TO
                            + "\nWith CorrelationId" + mqMessage.getStringProperty("JMSCorrelationID")
                            + "\nMessage text : " + messageText);

                } else if (!mqMessage.getStringProperty("JMSCorrelationID").equals(message.getJMSMessageID())) {
                    System.out.println("\nReceived different message from the queue " + REPLY_TO
                            + "\nWith CorrelationId" + mqMessage.getStringProperty("JMSCorrelationID")
                            + "\nMessage text : " + messageText);

                } else {
                    System.out.println("\nNo message received from the queue "
                            + REPLY_TO + " within " + RECEIVE_DELAY_SECONDS + " seconds");
                }
            }






            Thread.sleep(RECEIVE_DELAY_SECONDS * 1000);
            queue.close();
            qmgr.disconnect();
            System.out.println("Disconnected from IBM MQ");

        } catch (MQException mqe) {
            System.err.println("MQException: " + mqe.getMessage());
        } catch (InterruptedException ie) {
            System.err.println("InterruptedException: " + ie.getMessage());
        } catch (Exception e) {
            System.err.println("Exception: " + e.getMessage());
        }
    }


}