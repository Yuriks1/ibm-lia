package demos;

import com.ibm.mq.*;
import com.ibm.mq.constants.MQConstants;

import java.util.Calendar;

public class IbmMqSendAndReceiveWithInterval {

    private static final String QUEUE_MANAGER_NAME = "QMLOCAL";
    private static final String QUEUE_NAME = "TEST.QUEUE.LOCAL";
    private static final int MESSAGE_COUNT = 5;
    private static final String MESSAGE_TEXT = "Unique number";
    private static final int SEND_DELAY_SECONDS = 2;
    private static final int RECEIVE_DELAY_SECONDS = 2;

    public static void main(String[] args) {
        try {

            MQEnvironment.hostname = "localhost";
            MQEnvironment.port = 1414;
            MQEnvironment.channel = "DEV.APP.SVRCONN";
            MQEnvironment.password = "passw0rd";
            MQEnvironment.userID = "app";


            MQQueueManager qmgr = new MQQueueManager(QUEUE_MANAGER_NAME);

            int count = 0;

            // Send messages to the queue
            MQPutMessageOptions pmo = new MQPutMessageOptions();
            pmo.options = MQConstants.MQPMO_NO_SYNCPOINT | MQConstants.MQPMO_NEW_MSG_ID | MQConstants.MQPMO_NEW_CORREL_ID;

            for (int i = 0; i < MESSAGE_COUNT; i++) {

                MQMessage message = new MQMessage();
                long uniqueNumber = System.currentTimeMillis() % 1000;
                count++;

                message.writeString(MESSAGE_TEXT+ " " + uniqueNumber + " sequence " + count);
                MQQueue queue = qmgr.accessQueue(QUEUE_NAME, MQConstants.MQOO_OUTPUT | MQConstants.MQOO_FAIL_IF_QUIESCING);
                queue.put(message, pmo);

                System.out.println("Sent message " + count + " " + message.putDateTime.get(Calendar.HOUR_OF_DAY)
                        + ":" + message.putDateTime.get(Calendar.MINUTE)
                        + ":" + message.putDateTime.get(Calendar.SECOND));
                Thread.sleep(SEND_DELAY_SECONDS * 1000);
            }

            // Receive messages from the queue
            MQGetMessageOptions gmo = new MQGetMessageOptions();
            gmo.options = MQConstants.MQGMO_NO_SYNCPOINT | MQConstants.MQGMO_WAIT;
            gmo.waitInterval = RECEIVE_DELAY_SECONDS * 1000;

            for (int i = 0; i < MESSAGE_COUNT; i++) {

                MQQueue queue = qmgr.accessQueue(QUEUE_NAME, MQConstants.MQOO_INPUT_AS_Q_DEF | MQConstants.MQOO_FAIL_IF_QUIESCING);
                MQMessage message = new MQMessage();
                queue.get(message, gmo);
                System.out.println("Received message " + message.putDateTime.get(Calendar.HOUR_OF_DAY)
                        + ":" + message.putDateTime.get(Calendar.MINUTE)
                        + ":" + message.putDateTime.get(Calendar.SECOND));
                queue.close();
                Thread.sleep(RECEIVE_DELAY_SECONDS * 1000);

            }

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
