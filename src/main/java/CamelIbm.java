import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.camel.CamelContext;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jms.JmsComponent;
import org.apache.camel.impl.DefaultCamelContext;


public final class CamelIbm {



    private CamelIbm() {
    }



    public static void main(String[] args) {
        try (CamelContext context = new DefaultCamelContext()) {

            ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory();
            context.addComponent("jms", JmsComponent.jmsComponentAutoAcknowledge(connectionFactory));


            try {
                context.addRoutes(new MyRouteBuilder());
                context.start();
                Thread.sleep(60000);
                context.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    static class MyRouteBuilder extends RouteBuilder {

        @Override
        public void configure() {
            from("ibmmq:queue:TEST.QUEUE.LOCAL?username=app&password=passw0rd&transportType=CLIENT&brokerURI=tcp://localhost:1414&queueManager=QMLOCAL&dataFormat=string")

                    .to("file:C:\\Users\\juris\\out")
                    .log("File Name: ${header.CamelFileName}, Body:${body} ")
                    .to("activemq:queue:foo.bar");
        }
    }
}