package demos;

import javax.jms.*;
import com.ibm.mq.jms.MQConnectionFactory;
import com.ibm.msg.client.wmq.compat.jms.internal.JMSC;

public class DemoEtt {

    private static final String HOSTNAME = "localhost";
    private static final int PORT = 1414;
    private static final String CHANNEL = "DEV.APP.SVRCONN";
    private static final String QUEUE_MANAGER = "QMLOCAL";
    private static final String QUEUE_NAME = "TEST.QUEUE.LOCAL";
    private static final int MESSAGE_COUNT = 10;

    public static void main(String[] args) {

        try {
            MQConnectionFactory factory = new MQConnectionFactory();
            factory.setHostName(HOSTNAME);
            factory.setPort(PORT);
            factory.setTransportType(JMSC.MQJMS_TP_CLIENT_MQ_TCPIP);
            factory.setQueueManager(QUEUE_MANAGER);
            factory.setChannel(CHANNEL);

            Connection connection = factory.createConnection();
            connection.start();

            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            Queue sendQueue = session.createQueue(QUEUE_NAME);
            Queue recvQueue = session.createQueue(QUEUE_NAME );

            MessageProducer producer = session.createProducer(sendQueue);
            MessageConsumer consumer = session.createConsumer(recvQueue);

            // send messages
            for (int i = 0; i < MESSAGE_COUNT; i++) {
                TextMessage sendMessage = session.createTextMessage("Message " + i);
                producer.send(sendMessage);
                System.out.println("Sent message " + i + ": " + sendMessage.getText());
            }

            // receive and check messages
            for (int i = 0; i < MESSAGE_COUNT; i++) {
                TextMessage receivedMessage = (TextMessage) consumer.receive(5000);
                if (receivedMessage == null) {
                    System.out.println("No message received within timeout period");
                    continue;
                }
                System.out.println("Received message " + i + ": " + receivedMessage.getText());
                if (!receivedMessage.getText().equals("Message " + i)) {
                    System.out.println("Mismatch between sent and received message at index " + i);
                }
            }

            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
