package microservice.quic_rpc.service;

import microservice.quic_rpc.bean.DemoRequest;
import microservice.quic_rpc.bean.DemoRespond;

public interface Demo {
    DemoRespond demoSynchronizedRpc(DemoRequest request);

    DemoRespond demoASynchronizedRpc(DemoRequest demoRequest);
}
