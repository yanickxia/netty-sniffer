package demo.netty.sniffer;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.spec.InvalidKeySpecException;

/**
 * Hello world!
 */
public class App {

    private final static Logger logger = LoggerFactory.getLogger(App.class);

    private int port;

    public App(int port) {
        this.port = port;
    }

    public void run() throws Exception {

        EventLoopGroup bossGroup = new NioEventLoopGroup(); // (1)
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap(); // (2)
            b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class) // (3)
                    .childHandler(new ServerInitializer(buildSslContext())).option(ChannelOption.SO_BACKLOG, 128)          // (5)
                    .childOption(ChannelOption.SO_KEEPALIVE, true); // (6)

            // Bind and start to accept incoming connections.
            ChannelFuture f = b.bind(port).sync(); // (7)

            // Wait until the server socket is closed.
            // In this example, this does not happen, but you can do that to gracefully
            // shut down your server.
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        int port = 8080;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }
        logger.info("Starting at: " + port);
        new App(port).run();
    }

    private static class ServerInitializer extends ChannelInitializer<SocketChannel> {

        private final SslContext sslContext;

        public ServerInitializer(SslContext sslContext) {
            this.sslContext = sslContext;
        }

        @Override
        protected void initChannel(SocketChannel channel) {
            ChannelPipeline pipeline = channel.pipeline();
            pipeline.addLast(new TriableHandler(sslContext.newHandler(channel.alloc())))
                    .addLast(new HttpRequestDecoder())
                    .addLast(new HttpResponseEncoder())
                    .addLast(new CustomHttpServerHandler());
        }
    }

    private SslContext buildSslContext()
            throws IOException, InvalidKeySpecException, NoSuchAlgorithmException, CertificateException {
        return SslContextBuilder.forServer(
                        SecurityProvider.readPrivateKey(getClass().getClassLoader().getResourceAsStream("server-rsa.key")),
                        SecurityProvider.readCertificate(getClass().getClassLoader().getResourceAsStream("server-rsa.pem")))
                .protocols("TLSv1.3", "TLSv1.2")
                .build();
    }
}
