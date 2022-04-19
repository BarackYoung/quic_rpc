package microservice.quic_rpc.network.handler;

import com.google.protobuf.InvalidProtocolBufferException;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.incubator.codec.quic.QuicStreamChannel;
import microservice.quic_rpc.network.Executor;
import microservice.quic_rpc.network.RPC;
import microservice.quic_rpc.network.task.RpcRequestTask;

public class ServerInboundStreamHandler extends ChannelInitializer<QuicStreamChannel> {

    @Override
    protected void initChannel(QuicStreamChannel ch) throws Exception {
        ch.pipeline().addLast(new LineBasedFrameDecoder(1024))
                .addLast(new ChannelInboundHandlerAdapter() {
                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object msg) {
                        ByteBuf byteBuf = (ByteBuf) msg;
                        try {
                            RPC.RpcRequest request = getRequestFromByteBuf(byteBuf);
                            // 交给处理器处理
                            Executor.execute(new RpcRequestTask(request, ctx));

                        } catch (InvalidProtocolBufferException e) {
                            e.printStackTrace();
                        } finally {
                            byteBuf.release();
                        }
                    }
                });
    }

    private RPC.RpcRequest getRequestFromByteBuf(ByteBuf byteBuf) throws InvalidProtocolBufferException {
        int length = byteBuf.readInt();
        byte[] bytes = new byte[length];
        byteBuf.readBytes(bytes);
        return RPC.RpcRequest.parseFrom(bytes);
    }


}
