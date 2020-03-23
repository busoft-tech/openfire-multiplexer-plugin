package tr.com.busoft.openfire.multiplexer;

import org.jivesoftware.openfire.container.Plugin;
import org.jivesoftware.openfire.container.PluginManager;
import org.jivesoftware.openfire.event.SessionEventDispatcher;
import org.jivesoftware.openfire.interceptor.InterceptorManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.File;

public class MultiplexerPlugin implements Plugin {
    private static final Logger Log = LoggerFactory.getLogger( MultiplexerPlugin.class );

    private final MultiplexerInterceptor interceptor = new MultiplexerInterceptor();
    private final MultiplexerSession session = new MultiplexerSession();

    @Override
    public synchronized void initializePlugin( final PluginManager manager, final File pluginDirectory ) {
        Log.debug( "Initializing..." );

        SessionEventDispatcher.addListener( session );
        InterceptorManager.getInstance().addInterceptor( interceptor );

        Log.debug( "Initialized." );
    }

    @Override
    public synchronized void destroyPlugin() {
        Log.debug( "Destroying..." );

        SessionEventDispatcher.removeListener( session );
        InterceptorManager.getInstance().removeInterceptor( interceptor );

        Log.debug( "Destroyed." );
    }
}
