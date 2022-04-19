package microservice.quic_rpc.network.handler;

import com.google.protobuf.InvalidProtocolBufferException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.ChannelInputShutdownReadComplete;
import io.netty.incubator.codec.quic.QuicChannel;
import microservice.quic_rpc.network.RPC;
import microservice.quic_rpc.network.task.RpcRespondHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class ClientInboundStreamHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientInboundStreamHandler.class);

    private List<RPC.RpcRespond> respond;

    private Lock lock;

    private Condition condition;

    private RpcRespondHandler handler;

    public ClientInboundStreamHandler(List<RPC.RpcRespond> myRespond, Lock lock, Condition condition) {
        this.respond = myRespond;
        this.lock = lock;
        this.condition = condition;
    }

    public ClientInboundStreamHandler(RpcRespondHandler handler) {
        this.handler = handler;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws InvalidProtocolBufferException {
        LOGGER.info("response received!!!");

        ByteBuf byteBuf = (ByteBuf) msg;

        int length = byteBuf.readInt();

        byte[] bytes = new byte[length];
        byteBuf.readBytes(bytes);

        RPC.RpcRespond myRespond = RPC.RpcRespond.parseFrom(bytes);

        if (handler != null) {
            handler.run(myRespond);
            return;
        }

        try {
            lock.lock();
            respond.add(myRespond);
            condition.signal();
        }finally {
            lock.unlock();
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt == ChannelInputShutdownReadComplete.INSTANCE) {
            // Close the connection once the remote peer did send the FIN for this stream.
            ((QuicChannel) ctx.channel().parent()).close(true, 0,
                    ctx.alloc().directBuffer(16)
                            .writeBytes(new byte[]{'k', 't', 'h', 'x', 'b', 'y', 'e'}));
        }
    }
}
