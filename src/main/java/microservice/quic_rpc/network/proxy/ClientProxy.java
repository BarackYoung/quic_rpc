package microservice.quic_rpc.network.proxy;

import microservice.quic_rpc.network.annotation.RpcClient;
import microservice.quic_rpc.network.exception.AnnotationException;
import microservice.quic_rpc.network.servicegovernance.LoadBalance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Proxy;
import java.util.Map;

@Component
public class ClientProxy {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientProxy.class);


    public static <T> T getService(Class<T> interfaceClaz, LoadBalance loadBalance) {
        RpcClient rpcClient = interfaceClaz.getDeclaredAnnotation(RpcClient.class);
        if (rpcClient == null) {
            throw new AnnotationException();
        }
        String[] classNames = interfaceClaz.getName().split("\\.");
        String className = classNames[classNames.length-1];
        String applicationName = rpcClient.value();
        // 通过serviceName到注册中心查找实例列表并进行负载均衡调用
        LOGGER.info("loadBalance:" + loadBalance);
        Map<String, Object> instanceInfo = loadBalance.getInstance(applicationName);
        String serverIp = instanceInfo.get("ipAddr").toString();
        int serverPort = Integer.parseInt(instanceInfo.get("port").toString());
        LOGGER.info("class: " +className);
        LOGGER.info("ip:" + serverIp + ", port:" + serverPort);
        return (T) Proxy.newProxyInstance(interfaceClaz.getClassLoader(), new Class<?>[]{interfaceClaz}, new RpcClientInvocationHandler(className, serverIp, serverPort));
    }
}
