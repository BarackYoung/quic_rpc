package microservice.quic_rpc.network.proxy;

import com.google.protobuf.ByteString;
import microservice.quic_rpc.network.ProtostuffUtils;
import microservice.quic_rpc.network.QuicClient;
import microservice.quic_rpc.network.RPC;
import microservice.quic_rpc.network.annotation.Asynchronous;
import microservice.quic_rpc.network.task.RpcRespondHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

public class RpcClientInvocationHandler implements InvocationHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(RpcClientInvocationHandler.class);

    private static AtomicInteger requestId = new AtomicInteger(0);

    private String serviceName;

    private String serverIp;

    private int serverPort;

    public RpcClientInvocationHandler(String myServiceName, String myServerIp, int myServerPort) {
        this.serviceName = myServiceName;
        this.serverPort = myServerPort;
        this.serverIp = myServerIp;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        LOGGER.info(method.getName() + " called!!!");
        LOGGER.info("args number:" + args.length);
        RPC.RpcRequest request = RPC.RpcRequest.newBuilder()
                .setRequestId(getRequestId())
                .setService(this.serviceName)
                .setMethod(method.getName())
                .setRequest(ByteString.copyFrom(ProtostuffUtils.serialize(args[0])))
                .build();
        // 获取方法上的注解，判断是进行阻塞调用还是异步调用， 如果进行异步调用， 则请求后直接返回
        Asynchronous asynchronous = method.getAnnotation(Asynchronous.class);
        if (asynchronous != null) {
            String targetHandler = asynchronous.value();
            Class<?> claz = Class.forName(targetHandler);
            RpcRespondHandler rpcRespondHandler = (RpcRespondHandler) claz.newInstance();
            QuicClient.senRequest(serverIp, serverPort, request, rpcRespondHandler);
            return null;
        }
        RPC.RpcRespond rpcRespond = QuicClient.sendRequest(serverIp, serverPort, request);
        LOGGER.info("method returned!! ");
        if (rpcRespond == null) {
            return  null;
        }
        LOGGER.info("respond content:" + rpcRespond.toString());
        return ProtostuffUtils.deserialize(rpcRespond.getResponse().toByteArray(), method.getReturnType());
    }

    public static int getRequestId() {
        return requestId.addAndGet(1);
    }

}
