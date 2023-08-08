package demo.netty.sniffer;

import io.netty.channel.Channel;
import io.netty.util.concurrent.DefaultPromise;
import io.netty.util.concurrent.EventExecutor;

public class LazyChannelPromise extends DefaultPromise<Channel> {

    @Override
    public boolean tryFailure(Throwable cause) {
        return false;
    }

    @Override
    protected EventExecutor executor() {
        if (ctx == null) {
            throw new IllegalStateException();
        }
        return ctx.executor();
    }

    @Override
    protected void checkDeadLock() {
        if (ctx == null) {
            // If ctx is null the handlerAdded(...) callback was not called, in this case the checkDeadLock()
            // method was called from another Thread then the one that is used by ctx.executor(). We need to
            // guard against this as a user can see a race if handshakeFuture().sync() is called but the
            // handlerAdded(..) method was not yet as it is called from the EventExecutor of the
            // ChannelHandlerContext. If we not guard against this super.checkDeadLock() would cause an
            // IllegalStateException when trying to call executor().
            return;
        }
        super.checkDeadLock();
    }
}