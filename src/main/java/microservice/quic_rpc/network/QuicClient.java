package microservice.quic_rpc.network;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.incubator.codec.quic.*;
import io.netty.util.NetUtil;
import microservice.quic_rpc.network.handler.ClientInboundStreamHandler;
import microservice.quic_rpc.network.task.RpcRespondHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class QuicClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(QuicClient.class);

    private String serverIp;

    private int serverPort;

    private RPC.RpcRespond respond;

    private static final QuicSslContext context = QuicSslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).
            applicationProtocols("http/0.9").build();

    public static RPC.RpcRespond sendRequest(String serverIp, int serverPort, RPC.RpcRequest request) throws InterruptedException, ExecutionException {
        NioEventLoopGroup group = new NioEventLoopGroup(1);
        Bootstrap bs = new Bootstrap();
        ChannelHandler channelHandler = getChannelHandler();
        Channel channel = getChannel(bs, group, channelHandler);
        QuicChannel quicChannel = getQuicChannel(channel, serverIp, serverPort);
        List<RPC.RpcRespond> respondList = new ArrayList<>(1);
        Lock lock = new ReentrantLock();
        Condition condition = lock.newCondition();

        ClientInboundStreamHandler handler = new ClientInboundStreamHandler(respondList, lock, condition);

        QuicStreamChannel streamChannel = quicChannel.
                createStream(QuicStreamType.BIDIRECTIONAL,handler
                        ).sync().getNow();

        try {
            lock.lock();
            LOGGER.info("sending quic request! ip:" + serverIp + " , port:" +serverPort);
            streamChannel.writeAndFlush(wrapRpcRequest(request)).addListener(QuicStreamChannel.SHUTDOWN_OUTPUT);
            condition.await(3, TimeUnit.SECONDS);
            streamChannel.closeFuture().sync();
            quicChannel.closeFuture().sync();
            channel.close().sync();
            return respondList.size() > 0 ? respondList.get(0) : null;
        }finally {
            lock.unlock();
            group.shutdownGracefully();
        }

    }

    public static void senRequest(String serverIp, int serverPort, RPC.RpcRequest request, RpcRespondHandler rpcRespondHandler) throws InterruptedException, ExecutionException {
        NioEventLoopGroup group = new NioEventLoopGroup(1);
        Bootstrap bs = new Bootstrap();
        ChannelHandler channelHandler = getChannelHandler();
        Channel channel = getChannel(bs, group, channelHandler);

        QuicChannel quicChannel = getQuicChannel(channel, serverIp, serverPort);

        ClientInboundStreamHandler handler = new ClientInboundStreamHandler(rpcRespondHandler);

        QuicStreamChannel streamChannel = quicChannel.
                createStream(QuicStreamType.BIDIRECTIONAL,handler
                ).sync().getNow();

        try {
            LOGGER.info("sending quic request! ip:" + serverIp + " , port:" +serverPort);
            streamChannel.writeAndFlush(wrapRpcRequest(request)).addListener(QuicStreamChannel.SHUTDOWN_OUTPUT);
            streamChannel.closeFuture().sync();
            quicChannel.closeFuture().sync();
            channel.close().sync();
        }finally {
            group.shutdownGracefully();
        }

    }

    private static QuicChannel getQuicChannel(Channel channel, String serverIp, int serverPort) throws ExecutionException, InterruptedException {
        return QuicChannel.newBootstrap(channel)
                .streamHandler(new ChannelInboundHandlerAdapter() {
                    @Override
                    public void channelActive(ChannelHandlerContext ctx) {
                        // As we did not allow any remote initiated streams we will never see this method called.
                        // That said just let us keep it here to demonstrate that this handle would be called
                        // for each remote initiated stream.
                        ctx.close();
                    }
                })
                .remoteAddress(new InetSocketAddress(NetUtil.createInetAddressFromIpAddressString(serverIp), serverPort))
                .connect()
                .get();
    }

    private static ChannelHandler getChannelHandler() {
        return new QuicClientCodecBuilder()
                .sslContext(context)
                .maxIdleTimeout(5000, TimeUnit.MILLISECONDS)
                .initialMaxData(10000000)
                // As we don't want to support remote initiated streams just setup the limit for local initiated
                // streams in this example.
                .initialMaxStreamDataBidirectionalLocal(1000000)
                .build();
    }

    private static Channel getChannel(Bootstrap bs, NioEventLoopGroup group, ChannelHandler codec) throws InterruptedException {
        return bs.group(group)
                .channel(NioDatagramChannel.class)
                .handler(codec)
                .bind(0).sync().channel();
    }

    private static ByteBuf wrapRpcRequest(RPC.RpcRequest request) {
        ByteBuf byteBuf = PooledByteBufAllocator.DEFAULT.buffer();
        byte[] bytes = request.toByteArray();
        byteBuf.writeInt(bytes.length);
        byteBuf.writeBytes(bytes);
        byteBuf.writeBytes("\r\n".getBytes(StandardCharsets.UTF_8));
        return byteBuf;
    }



}
