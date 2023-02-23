package demos;

import com.ibm.msg.client.jms.JmsConnectionFactory;
import com.ibm.msg.client.jms.JmsFactoryFactory;
import com.ibm.msg.client.wmq.WMQConstants;

import javax.jms.*;

public class JmsToIbm {


    public static void main(String[] args) throws JMSException {

        Connection connection;
        Session session;
        Destination destination;
        MessageProducer producer;

        JmsFactoryFactory ff = JmsFactoryFactory.getInstance(WMQConstants.WMQ_PROVIDER);
        JmsConnectionFactory cf = ff.createConnectionFactory();

        cf.setStringProperty(WMQConstants.WMQ_HOST_NAME, "localhost");
        cf.setIntProperty(WMQConstants.WMQ_PORT, 1414);
        cf.setStringProperty(WMQConstants.WMQ_CHANNEL, "DEV.APP.SVRCONN");
        cf.setIntProperty(WMQConstants.WMQ_CONNECTION_MODE, WMQConstants.WMQ_CM_CLIENT);
        cf.setStringProperty(WMQConstants.WMQ_QUEUE_MANAGER, "QMLOCAL");

        connection = cf.createConnection();
        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        destination = session.createQueue("queue:///TEST.QUEUE.LOCAL");
        producer = session.createProducer(destination);


        long uniqueNumber = System.currentTimeMillis() % 1000L;
        TextMessage message = session.createTextMessage("This is a test " + uniqueNumber);


        message.setJMSType("Test");
        message.setJMSReplyTo(destination);
        message.setJMSDestination(destination);
        message.setJMSDeliveryMode(DeliveryMode.NON_PERSISTENT);
        message.setJMSPriority(1);
        message.setJMSRedelivered(false);
        message.setJMSTimestamp(System.currentTimeMillis());

        connection.start();
        producer.send(message);
        System.out.println("Sent message to the queue  :\n" + message.getJMSMessageID());
    }


}
