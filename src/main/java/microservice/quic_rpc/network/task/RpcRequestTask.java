package microservice.quic_rpc.network.task;

import com.google.protobuf.ByteString;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelHandlerContext;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import microservice.quic_rpc.network.ProtostuffUtils;
import microservice.quic_rpc.network.RPC;
import microservice.quic_rpc.network.servicegovernance.ServiceGovernance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Map;


public class RpcRequestTask implements Runnable{

    Logger logger = LoggerFactory.getLogger(RpcRequestTask.class);

    private RPC.RpcRequest request;

    private ChannelHandlerContext context;

    public RpcRequestTask(RPC.RpcRequest request, ChannelHandlerContext ctx) {
        this.request = request;
        this.context = ctx;
    }

    @Override
    public void run() {
        // 根据request找到对应的服务, 调用相应方法， 包装返回结果
        logger.info("server received a request, requestId =" + request.getRequestId());
        logger.info(request.toString());
        Map<String, Method> methodMap = ServiceGovernance.methodMap.get(request.getService());
        Class<?> claz = ServiceGovernance.serviceNameMap.get(request.getService());
        Method method = methodMap.get(request.getMethod());
        Class<?> parameterType = method.getParameterTypes()[0];
        try {
          Object res =  method.invoke(claz.newInstance(), ProtostuffUtils.deserialize(request.getRequest().toByteArray(), parameterType));
          RPC.RpcRespond respond = RPC.RpcRespond.newBuilder()
                    .setRespondId(request.getRequestId())
                    .setService(request.getService())
                    .setMethod(request.getMethod())
                    .setResponse(ByteString.copyFrom(ProtostuffUtils.serialize(res)))
                    .build();
          context.writeAndFlush(wrapRespondToByteBuf(respond)).addListener(QuicStreamChannel.SHUTDOWN_OUTPUT);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    private ByteBuf wrapRespondToByteBuf(RPC.RpcRespond respond) {
        byte[] respondBytes = respond.toByteArray();
        ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.buffer();
        byteBuf.writeInt(respondBytes.length);
        byteBuf.writeBytes(respondBytes);
        byteBuf.writeBytes("\r\n".getBytes(StandardCharsets.UTF_8));
        return byteBuf;
    }
}
