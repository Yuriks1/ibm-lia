package backend;

import com.ibm.mq.*;
import com.ibm.mq.constants.MQConstants;

import java.util.Calendar;

public class BackendApp {

    private static final String QUEUE_MANAGER_NAME = "QMLOCAL";
    private static final String QUEUE_NAME = "TEST.QUEUE.LOCAL";
    private static final int RECEIVE_DELAY_SECONDS = 2;

    public static void main(String[] args) {
        try {

            MQEnvironment.hostname = "localhost";
            MQEnvironment.port = 1414;
            MQEnvironment.channel = "DEV.APP.SVRCONN";
            MQEnvironment.password = "passw0rd";
            MQEnvironment.userID = "app";


            MQQueueManager qmgr = new MQQueueManager(QUEUE_MANAGER_NAME);
            MQGetMessageOptions gmo = new MQGetMessageOptions();

            gmo.options = MQConstants.MQGMO_NO_SYNCPOINT | MQConstants.MQGMO_WAIT;
            gmo.waitInterval = RECEIVE_DELAY_SECONDS * 1000;


            MQQueue queue = qmgr.accessQueue(QUEUE_NAME, MQConstants.MQOO_INPUT_AS_Q_DEF | MQConstants.MQOO_FAIL_IF_QUIESCING | MQConstants.MQOO_INQUIRE);
            MQMessage message = new MQMessage();
            queue.get(message, gmo);

            System.out.println("Received message " + message.putDateTime.get(Calendar.HOUR_OF_DAY)
                    + ":" + message.putDateTime.get(Calendar.MINUTE)
                    + ":" + message.putDateTime.get(Calendar.SECOND)
                    + ":" + message.putDateTime.get(Calendar.MILLISECOND)
                    + "\nQueue Manager : " + QUEUE_MANAGER_NAME
                    + "\nFrom queue : " + QUEUE_NAME
                    + "\nMessage ReplyTo : " + message.getStringProperty("JMSReplyTo")
                    + "\nMessage id : " + message.getStringProperty("JMSMessageID")
                    + "\nMessage text : " + message.readStringOfByteLength(message.getMessageLength())
                    + "\nMessage type : " + message.getStringProperty("JMSType"));


            queue.close();
            Thread.sleep(RECEIVE_DELAY_SECONDS * 1000);


            qmgr.disconnect();
        } catch (MQException mqe) {
            System.err.println("MQException: " + mqe.getMessage());
        } catch (InterruptedException ie) {
            System.err.println("InterruptedException: " + ie.getMessage());
        } catch (Exception e) {
            System.err.println("Exception: " + e.getMessage());
        }
    }

}