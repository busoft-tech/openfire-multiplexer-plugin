package tr.com.busoft.openfire.multiplexer;

import org.jivesoftware.openfire.event.SessionEventListener;
import org.jivesoftware.openfire.session.Session;
import org.jivesoftware.database.DbConnectionManager;
import java.sql.Connection;
import java.sql.Statement;
import org.xmpp.packet.JID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiplexerSession implements SessionEventListener {

	private static final Logger Log = LoggerFactory.getLogger(MultiplexerSession.class);

	@Override
	public void sessionCreated(Session session)
	{
		Connection connection = null;
		Statement statement = null;
		try
		{
			connection = DbConnectionManager.getConnection();
			statement = connection.createStatement();

			JID user = session.getAddress();
			String username = user.getNode();
			String resource = user.getResource();

			String sql = String.format("INSERT IGNORE INTO ofMultiplexer (username, resource) VALUES ('%s', '%s')", username, resource);
			statement.executeUpdate(sql);
		}
		catch (Exception exception)
		{
			Log.error("Error while inserting " + exception.getMessage());
		}
		finally
		{
			try
			{
				DbConnectionManager.closeConnection(statement, connection);
			}
			catch (Exception exception)
			{
				Log.error("Error while closing dbconnections " + exception.getMessage());
			}
		}
	}

	@Override
	public void sessionDestroyed(Session session) {
		// TODO Auto-generated method stub

	}

	@Override
	public void anonymousSessionCreated(Session session) {
		// TODO Auto-generated method stub

	}

	@Override
	public void anonymousSessionDestroyed(Session session) {
		// TODO Auto-generated method stub

	}

	@Override
	public void resourceBound(Session session) {
		// TODO Auto-generated method stub

	}
	
}