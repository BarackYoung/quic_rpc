package microservice.quic_rpc.network;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.netty.incubator.codec.quic.*;
import microservice.quic_rpc.network.handler.ServerInboundStreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.security.cert.CertificateException;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component
@Lazy(value = false)
public class QuicServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(QuicServer.class);

    private String port;

    public void startService() throws InterruptedException, CertificateException {
        ResourceBundle resourceBundle = ResourceBundle.getBundle("application");
        this.port = resourceBundle.getString("rpc.service.port");
        LOGGER.info("QUIC server run at port:" + this.port);
        SelfSignedCertificate selfSignedCertificate = new SelfSignedCertificate();
        QuicSslContext context = QuicSslContextBuilder.forServer(
                        selfSignedCertificate.privateKey(), null, selfSignedCertificate.certificate())
                .applicationProtocols("http/0.9").build();
        NioEventLoopGroup group = new NioEventLoopGroup(1);
        ChannelHandler codec = new QuicServerCodecBuilder().sslContext(context)
                .maxIdleTimeout(5000, TimeUnit.MILLISECONDS)
                // Configure some limits for the maximal number of streams (and the data) that we want to handle.
                .initialMaxData(10000000)
                .initialMaxStreamDataBidirectionalLocal(1000000)
                .initialMaxStreamDataBidirectionalRemote(1000000)
                .initialMaxStreamsBidirectional(100)
                .initialMaxStreamsUnidirectional(100)

                // Setup a token handler. In a production system you would want to implement and provide your custom
                // one.
                .tokenHandler(InsecureQuicTokenHandler.INSTANCE)
                // ChannelHandler that is added into QuicChannel pipeline.
                .handler(new ChannelInboundHandlerAdapter() {
                    @Override
                    public void channelActive(ChannelHandlerContext ctx) {
                        QuicChannel channel = (QuicChannel) ctx.channel();
                        // Create streams etc..
                    }

                    public void channelInactive(ChannelHandlerContext ctx) {
                        ((QuicChannel) ctx.channel()).collectStats().addListener(f -> {
                            if (f.isSuccess()) {
                                LOGGER.info("Connection closed: {}", f.getNow());
                            }
                        });
                    }

                    @Override
                    public boolean isSharable() {
                        return true;
                    }
                })
                .streamHandler(new ServerInboundStreamHandler()).build();
        Bootstrap bs = new Bootstrap();
        try {
            Channel channel = bs.group(group)
                    .channel(NioDatagramChannel.class)
                    .handler(codec)
                    .bind(new InetSocketAddress(Integer.parseInt(port))).sync().channel();
            channel.closeFuture().sync();
        }finally {
            group.shutdownGracefully();
        }
    }

    public static void run() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    new QuicServer().startService();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (CertificateException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


}
