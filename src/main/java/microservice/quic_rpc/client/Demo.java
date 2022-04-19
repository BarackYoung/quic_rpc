package microservice.quic_rpc.client;

import microservice.quic_rpc.bean.DemoRequest;
import microservice.quic_rpc.bean.DemoRespond;
import microservice.quic_rpc.network.annotation.Asynchronous;
import microservice.quic_rpc.network.annotation.RpcClient;
import org.springframework.stereotype.Component;

@RpcClient("demo")
@Component
public interface Demo {
    DemoRespond demoSynchronizedRpc(DemoRequest request);

    @Asynchronous("microservice.quic_rpc.client.DemoRespondHandler")
    DemoRespond demoASynchronizedRpc(DemoRequest demoRequest);
}
