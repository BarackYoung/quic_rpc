package microservice.quic_rpc.network.task;

import microservice.quic_rpc.network.RPC;

public interface RpcRespondHandler {
    void run(RPC.RpcRespond respond);
}
