package microservice.quic_rpc.service;

import microservice.quic_rpc.bean.DemoRequest;
import microservice.quic_rpc.bean.DemoRespond;
import microservice.quic_rpc.network.annotation.RpcService;
import org.springframework.stereotype.Component;

@RpcService
@Component
public class DemoImpl implements Demo{


    @Override
    public DemoRespond demoSynchronizedRpc(DemoRequest request) {
        DemoRespond respond = new DemoRespond();
        respond.setRespondId(respond.respondId);
        respond.setContent(request.getContent());
        return respond;
    }

    @Override
    public DemoRespond demoASynchronizedRpc(DemoRequest request) {
        DemoRespond respond = new DemoRespond();
        respond.setRespondId(respond.respondId);
        respond.setContent(request.getContent());
        return respond;
    }
}
