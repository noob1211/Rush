package net.rush.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.ReadTimeoutHandler;
import net.rush.Server;
import net.rush.netty.pipeline.KickStringWriter;
import net.rush.netty.pipeline.PacketDecoder;
import net.rush.netty.pipeline.PacketEncoder;
import net.rush.netty.pipeline.Varint21Decoder;
import net.rush.netty.pipeline.Varint21Encoder;
import net.rush.protocol.Protocol;

public class NettyInitializer extends Thread {

	private final Server server;
	private final EventLoopGroup bossgroup = new NioEventLoopGroup();
	private final EventLoopGroup workergroup = new NioEventLoopGroup();

	public NettyInitializer(Server server) {
		this.server = server;
	}

	@Override
	public void run() {

		ServerBootstrap bootstrap = new ServerBootstrap()
				.group(bossgroup, workergroup)
				.channel(NioServerSocketChannel.class)
				.childHandler(new ChannelInitializer<SocketChannel>() {
					@Override
					protected void initChannel(SocketChannel ch) throws Exception {
						ch.pipeline()

						.addLast("timer", new ReadTimeoutHandler(30))
						.addLast("kickwriter", new KickStringWriter())

						.addLast("varintdecoder", new Varint21Decoder())
						.addLast("decoder", new PacketDecoder(Protocol.HANDSHAKE))

						.addLast("varintencoder", new Varint21Encoder())
						.addLast("encoder", new PacketEncoder(Protocol.HANDSHAKE))

						.addLast("handler", new RushChannelHandler(server));
					}
				});

		try {
			bootstrap.bind(server.getPort()).sync().channel().closeFuture().sync();
		} catch (Throwable t) {
			server.getLogger().severe("** FAILED TO BIND TO THE PORT! Make sure that");
			server.getLogger().severe("another server is not running on that port. **");
			server.getLogger().severe("The exception was: " + t.getMessage());
			System.exit(0);
		}	}

	public void shutdown() {
		System.out.println("Closing Netty");

		bossgroup.shutdownGracefully();
		workergroup.shutdownGracefully();		
	}
}