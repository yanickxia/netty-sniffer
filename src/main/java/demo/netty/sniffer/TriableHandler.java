package demo.netty.sniffer;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.ssl.SslHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLEngine;
import java.util.Optional;

public class TriableHandler extends SslHandler {
    private Optional<Boolean> isSSL = Optional.empty();
    private final static Logger logger = LoggerFactory.getLogger(TriableHandler.class);

    public TriableHandler(SslHandler sslHandler) {
        super(sslHandler.engine());
    }

    private TriableHandler(SSLEngine engine) {
        super(engine);
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (isSSL.isPresent()) {
            if (isSSL.get()) {
                super.channelRead(ctx, msg);
            } else {
                ctx.fireChannelRead(msg);
            }
        } else {
            ByteBuf byteBuf = (ByteBuf) msg;
            ByteBuf retained = byteBuf.retain();
            try {
                isSSL = Optional.of(true);
                super.channelRead(ctx, byteBuf);
            } catch (Exception e) {
                logger.warn("Failed to read by ssl handler, try http");
                retained.resetReaderIndex();
                isSSL = Optional.of(false);
                ctx.fireChannelRead(retained);
            }
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (isSSL.isPresent()) {
            if (isSSL.get()) {
                super.write(ctx, msg, promise);
            } else {
                ctx.write(msg, promise);
            }
        } else {
            try {
                super.write(ctx, msg, promise);
            } catch (Exception e) {
                logger.warn("Failed to write by ssl handler", e);
                ctx.write(msg, promise);
            }
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        super.exceptionCaught(ctx, cause);
    }
}
