package microservice.quic_rpc;

import microservice.quic_rpc.network.QuicServer;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class QuicRpcApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuicRpcApplication.class, args);
        QuicServer.run();
    }

}
