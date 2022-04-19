package microservice.quic_rpc.client;

import microservice.quic_rpc.bean.DemoRespond;
import microservice.quic_rpc.network.ProtostuffUtils;
import microservice.quic_rpc.network.RPC;
import microservice.quic_rpc.network.task.RpcRespondHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DemoRespondHandler implements RpcRespondHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(DemoRespondHandler.class);

    @Override
    public void run(RPC.RpcRespond respond) {
        LOGGER.info("call back method!!");
        DemoRespond demoRespond = ProtostuffUtils.deserialize(respond.getResponse().toByteArray(), DemoRespond.class);
        LOGGER.info("return: " + demoRespond);
    }
}
