import com.ibm.msg.client.jms.JmsConnectionFactory;
import com.ibm.msg.client.jms.JmsFactoryFactory;
import com.ibm.msg.client.wmq.WMQConstants;

import javax.jms.*;


public class JmsPutGet {

    private static int status = 1;

    private static final String HOST = "localhost";
    private static final int PORT = 1414;
    private static final String CHANNEL = "DEV.APP.SVRCONN";
    private static final String QMGR = "QMLOCAL";
    private static final String APP_USER = "app";
    private static final String APP_PASSWORD = "passw0rd";
    private static final String QUEUE_NAME = "TEST.QUEUE.LOCAL";
    private static final String REPLY_TO = "DEV.QUEUE.1";


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


            long uniqueNumber = System.currentTimeMillis() % 1000;
            TextMessage message = context.createTextMessage("My unique number " + uniqueNumber);


            producer = context.createProducer();
            producer.setJMSType("SuperSecretMessage");
            producer.setJMSReplyTo(replyTo);
            producer.send(destination, message);
            System.out.println("Sent message to the queue : " + message.getText()
                    + "\nMessage Id " + message.getJMSMessageID()
                    + "\nDestination " + message.getJMSDestination()
                    + "\nReplyTo " + message.getJMSReplyTo());
            System.out.print("---------------------------------------------------");


           /* consumer = context.createConsumer(destination);
            TextMessage receivedMessage = (TextMessage) consumer.receive();


            if (receivedMessage != null && receivedMessage.getJMSMessageID().equals(message.getJMSMessageID())) {

                System.out.println("\nReceived same message from the queue:\n"
                        + receivedMessage.getJMSMessageID());
            } else {
                assert receivedMessage != null;
                System.out.println("\nReceived different message from the queue:\n"
                        + receivedMessage.getJMSMessageID());
            }
*/






/*            // Uncomment the following to compare the received message with the sent message(unique number)
            if (receivedMessage != null && receivedMessage.equals(message.getText())) {

                System.out.println("\nReceived same message:\n" + receivedMessage);
            } else {
                System.out.println("\nReceived different message:\n" + receivedMessage);
            }*/

            context.close();

        } catch (
                JMSException jmsex) {
            recordFailure(jmsex);
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
        return;
    }

}