package microservice.quic_rpc.controller;

import microservice.quic_rpc.bean.DemoRequest;
import microservice.quic_rpc.bean.DemoRespond;
import microservice.quic_rpc.client.Demo;
import microservice.quic_rpc.network.proxy.ClientProxy;
import microservice.quic_rpc.network.servicegovernance.LoadBalanceFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/rpcSynchronizedDemo")
    public ResponseEntity<?> rpcSynchronizedDemo() {
        DemoRequest request = new DemoRequest();
        request.setRequestId(1);
        request.setContent("content");
        Demo demo = ClientProxy.getService(Demo.class, LoadBalanceFactory.getRandomLoadBalance());
        DemoRespond respond = demo.demoSynchronizedRpc(request);
        return ResponseEntity.ok(respond);
    }

    @GetMapping("/rpcAsynchronizedDemo")
    public ResponseEntity<?> rpcAsynchronizedDemo() {
        DemoRequest request = new DemoRequest();
        request.setRequestId(1);
        request.setContent("content");
        Demo demo = ClientProxy.getService(Demo.class, LoadBalanceFactory.getRandomLoadBalance());
        demo.demoASynchronizedRpc(request);
        return ResponseEntity.ok("success");
    }

}
