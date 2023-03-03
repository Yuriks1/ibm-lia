package backend;

import com.ibm.mq.*;
import com.ibm.mq.constants.MQConstants;

import java.util.Calendar;

public class BackendApp {

    private static final String QUEUE_MANAGER_NAME = "QMLOCAL";
    private static final String QUEUE_NAME = "TEST.QUEUE.LOCAL";
    private static final String REPLYTO_QUEUE_NAME = "DEV.QUEUE.1";
    private static final int RECEIVE_DELAY_SECONDS = 5;

    private static final int DELAY_SECONDS = 30;



    public static void main(String[] args) {

        System.out.println((char) 27 + "[31m");

        try {

            MQEnvironment.hostname = "localhost";
            MQEnvironment.port = 1414;
            MQEnvironment.channel = "DEV.APP.SVRCONN";
            MQEnvironment.password = "passw0rd";
            MQEnvironment.userID = "app";


            MQQueueManager qmgr = new MQQueueManager(QUEUE_MANAGER_NAME);
            MQGetMessageOptions gmo = new MQGetMessageOptions();

            gmo.options = MQConstants.MQGMO_NO_SYNCPOINT | MQConstants.MQGMO_WAIT
                    | MQConstants.MQGMO_FAIL_IF_QUIESCING | MQConstants.MQGMO_CONVERT;
            gmo.waitInterval = DELAY_SECONDS * 1000;

            MQQueue queue = qmgr.accessQueue(QUEUE_NAME, MQConstants.MQOO_INPUT_AS_Q_DEF
                    | MQConstants.MQOO_FAIL_IF_QUIESCING | MQConstants.MQOO_INQUIRE | MQConstants.MQOO_OUTPUT
                    | MQConstants.MQOO_SET_IDENTITY_CONTEXT);


            long start = System.currentTimeMillis();
            long end = start + 30 * 1000;
            while (System.currentTimeMillis() < end) {

                Thread.sleep(RECEIVE_DELAY_SECONDS * 1000);
                MQMessage message = new MQMessage();
                MQMessage replyToMessage = new MQMessage();
                queue.get(message, gmo);
                String messageText = message.readStringOfByteLength(message.getMessageLength());


                //Get the message from the queue
                System.out.println("Waiting for a message from the queue " + QUEUE_NAME + "...");
                System.out.println("Received message " + message.putDateTime.get(Calendar.HOUR_OF_DAY) + ":"
                        + message.putDateTime.get(Calendar.MINUTE) + ":"
                        + message.putDateTime.get(Calendar.SECOND) + ":"
                        + message.putDateTime.get(Calendar.MILLISECOND) +
                        "\nQueue Manager : " + QUEUE_MANAGER_NAME
                        + "\nFrom queue : " + QUEUE_NAME
                        + "\nMessage ReplyTo : " + message.getStringProperty("JMSReplyTo")
                        + "\nMessage id : " + message.getStringProperty("JMSMessageID")
                        + "\nMessage text : " + messageText);
                System.out.println();
                System.out.println("---------------------------------------------------\n");
                System.out.println("Creating a reply message : ");

                // Create a reply message
                replyToMessage = new MQMessage();
                replyToMessage.format = MQConstants.MQFMT_STRING;
                replyToMessage.messageType = MQConstants.MQMT_REPLY;
                replyToMessage.correlationId = message.messageId;
                replyToMessage.replyToQueueManagerName = message.replyToQueueManagerName;
                replyToMessage.replyToQueueName = message.replyToQueueName;
                replyToMessage.writeString(messageText);
                System.out.println("ReplyToQueueManagerName : " + replyToMessage.replyToQueueManagerName);
                System.out.println("ReplyToQueueName : " + replyToMessage.replyToQueueName);
                System.out.println("CorrelationId : " + replyToMessage.getStringProperty("JMSCorrelationID"));
                System.out.println("Message text : " + messageText);

                // Send the reply message to the reply queue
                MQQueue replyQueue = qmgr.accessQueue(REPLYTO_QUEUE_NAME, MQConstants.MQOO_OUTPUT
                        | MQConstants.MQOO_FAIL_IF_QUIESCING | MQConstants.MQOO_INQUIRE | MQConstants.MQOO_SET_IDENTITY_CONTEXT);
                replyQueue.put(replyToMessage);
                System.out.println("Reply message sent to queue : " + REPLYTO_QUEUE_NAME);
                System.out.print("---------------------------------------------------\n");

            } // end while

            System.out.print("---------------------------------------------------");
            queue.close();
            qmgr.disconnect();
            System.out.println("Disconnected from queue manager " + QUEUE_MANAGER_NAME + " and closed queue " + QUEUE_NAME + ".");

        } catch (MQException mqe) {
            System.err.println("MQException: " + mqe.getMessage());
        } catch (Exception e) {
            System.err.println("Exception: " + e.getMessage());
        }
    }
}

