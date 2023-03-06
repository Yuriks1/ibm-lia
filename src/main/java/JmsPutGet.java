import com.ibm.mq.MQGetMessageOptions;
import com.ibm.mq.MQMessage;
import com.ibm.mq.MQQueue;
import com.ibm.mq.MQQueueManager;
import com.ibm.mq.constants.MQConstants;
import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.msg.client.jms.JmsConnectionFactory;
import com.ibm.msg.client.jms.JmsFactoryFactory;
import com.ibm.msg.client.wmq.WMQConstants;
import com.ibm.msg.client.wmq.compat.jms.internal.JMSC;

import javax.jms.*;
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
    private static final int SEND_DELAY_SECONDS = 3;
    private static final int RECEIVE_DELAY_SECONDS = 30;

    private static final String QUEUE_MANAGER = "QMLOCAL";
    private static final int MESSAGE_COUNT = 10;


    public static void main(String[] args) {

        System.out.println((char) (27) + "[34m");

        JMSContext context;
        Destination destination;
        JMSProducer producer;
        Destination replyTo;


        try {
            // Create a connection factory
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


            MQConnectionFactory factory = new MQConnectionFactory();
            factory.setHostName(HOST);
            factory.setPort(PORT);
            factory.setTransportType(JMSC.MQJMS_TP_CLIENT_MQ_TCPIP);
            factory.setQueueManager(QUEUE_MANAGER);
            factory.setChannel(CHANNEL);

            Connection connection = factory.createConnection();
            connection.start();

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);


            TextMessage message = null;
            int MAX_MESSAGES;

            Queue recvQueue = session.createQueue(REPLY_TO);
            MessageConsumer consumer = session.createConsumer(recvQueue);


            // Get the number of messages to send from the user
            while (true) {
                System.out.println("Enter number of messages to send (0-10) or 11 to exit");
                try {
                    MAX_MESSAGES = scanner.nextInt();
                    if (MAX_MESSAGES >= 0 && MAX_MESSAGES <= 10) {
                        break;
                    } else if (MAX_MESSAGES == 11) {
                        System.exit(0);
                    } else {
                        System.out.println("Invalid number of messages");
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid number of messages");
                }
            }

            // Create a number of messages as defined by the user
            for (int i = 1; i <= MAX_MESSAGES; i++) {

                // Create a unique number to identify each message
                //long uniqueNumber = System.currentTimeMillis() % 1000; // Unique number to identify each message
                message = context.createTextMessage("Message " + i);
                producer = context.createProducer();
                producer.setJMSReplyTo(replyTo);

                // Sends the message
                producer.send(destination, message);
                System.out.println("Sent message to the queue : " + message.getText()
                        + "\nMessage Id " + message.getJMSMessageID()
                        + "\nDestination " + message.getJMSDestination()
                        + "\nReplyTo " + message.getJMSReplyTo());
                System.out.print("\n---------------------------------------------------\n");
                Thread.sleep(SEND_DELAY_SECONDS * 1000);
            }

          /*  // receive and check messages

            long start = System.currentTimeMillis();
            long end = start + 30 * 1000;

            while (System.currentTimeMillis() < end) {


                    TextMessage receivedMessage = (TextMessage) consumer.receive(5000);

                    if (!receivedMessage.getText().equals(message.getText())) {
                        System.out.println("Mismatch between sent and received message at  " + message.getText() + " : " + receivedMessage.getText());
                    } else if (receivedMessage.getText().equals(message.getText())) {
                        System.out.println("Received number is same as sent  " + message.getText()+ " : " + receivedMessage.getText());
                    }
                    else {
                        System.out.println("No message received");
                    }
                }
*/


            // Gets the message from the queue


            System.out.println("Waiting " + RECEIVE_DELAY_SECONDS + " seconds to receive messages!\n");
            MQQueueManager qmgr = new MQQueueManager(QMGR);
            MQGetMessageOptions gmo = new MQGetMessageOptions();
            gmo.options = MQConstants.MQGMO_WAIT | MQConstants.MQGMO_FAIL_IF_QUIESCING
                    | MQConstants.MQGMO_CONVERT
                    | MQConstants.MQGMO_SYNCPOINT;
            gmo.matchOptions = MQConstants.MQMO_MATCH_CORREL_ID;
            gmo.waitInterval = RECEIVE_DELAY_SECONDS * 1000;
            MQQueue queue = qmgr.accessQueue(REPLY_TO, MQConstants.MQOO_INPUT_AS_Q_DEF
                    | MQConstants.MQOO_FAIL_IF_QUIESCING);

            // Wait for 30 seconds for the messages to arrive
            long start = System.currentTimeMillis();
            long end = start + 30 * 1000;

            // Loop until the time is up
            while (System.currentTimeMillis() < end) {
                Thread.sleep(SEND_DELAY_SECONDS * 1000);

                MQMessage mqMessage = new MQMessage();
                mqMessage.clearMessage();
                queue.get(mqMessage, gmo);
                String messageText = mqMessage.readStringOfByteLength(mqMessage.getMessageLength());

                if (mqMessage == null) {
                    System.out.println("No message received within timeout period");
                    continue;
                }

                // Check if the message received is the same as the one sent
                if (mqMessage.getStringProperty("JMSCorrelationID").equals(message.getJMSMessageID())) {

                    System.out.println("\nReceived same message from the queue " + REPLY_TO
                            + "\nWith CorrelationId" + mqMessage.getStringProperty("JMSCorrelationID")
                            + "\nMessage text : " + messageText);

                } else if (!mqMessage.getStringProperty("JMSCorrelationID").equals(message.getJMSMessageID())) {
                    System.out.println("\nReceived different message from the queue " + REPLY_TO
                            + "\nWith CorrelationId" + mqMessage.getStringProperty("JMSCorrelationID")
                            + "\nMessage text : " + messageText);

                } else {
                    System.out.println("Something went wrong");
                }
            }

            // Close the queue and the queue manager
            queue.close();
            qmgr.disconnect();
            System.out.println("Disconnected from IBM MQ");


            connection.close();

            System.out.println("Connection closed");

            // Catch any exceptions that may have occurred
        } catch (InterruptedException ie) {
            System.err.println("InterruptedException: " + ie.getMessage());
        } catch (Exception e) {
            System.err.println("Exception: " + e.getMessage());

        } catch (Throwable t) {
            System.err.println("Throwable: " + t.getMessage());
        }
    }


}