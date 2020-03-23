package tr.com.busoft.openfire.multiplexer;

import org.jivesoftware.openfire.interceptor.PacketInterceptor;
import org.jivesoftware.openfire.interceptor.PacketRejectedException;
import org.jivesoftware.openfire.session.Session;
import org.xmpp.packet.Packet;
import org.jivesoftware.openfire.XMPPServer;
import org.jivesoftware.openfire.carbons.Sent;
import org.jivesoftware.openfire.forward.Forwarded;
import org.jivesoftware.openfire.MessageRouter;
import org.xmpp.packet.Message;
import org.xmpp.packet.JID;
import org.dom4j.Element;
import org.dom4j.Namespace;
import java.util.*;
import org.jivesoftware.util.XMPPDateTimeFormat;
import org.jivesoftware.database.DbConnectionManager;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MultiplexerInterceptor implements PacketInterceptor
{
    private static final Logger Log = LoggerFactory.getLogger( MultiplexerInterceptor.class );

    private MessageRouter messageRouter;

    MultiplexerInterceptor()
   {
      this.messageRouter = XMPPServer.getInstance().getMessageRouter();
   }

    @Override
    public void interceptPacket( final Packet packet, final Session session, final boolean incoming, final boolean processed ) throws PacketRejectedException
    {
        // Ignore any packets that haven't already been processed by interceptors.
        // Ignore any outgoing messages (we'll catch them when they're incoming).
        if (processed || !incoming) {
            return;
        }

        if (packet instanceof Message) {

            Message message = (Message) packet;

            JID receiver = message.getTo();
            String receiverUsername = receiver.getNode();
            String receiverDomain = receiver.getDomain();
            String receiverResource = receiver.getResource();

            JID sender = message.getFrom();
            String senderUsername = sender.getNode();
            String senderDomain = sender.getDomain();
            String senderResource = sender.getResource();

            if (receiverResource == null) {

                boolean chatState = isChatState(message);
                if(!chatState) {
                    Date timestamp = new Date();
                    Element delayInformation = message.addChildElement("delay", "urn:xmpp:delay");
                    delayInformation.addAttribute("stamp", XMPPDateTimeFormat.format(timestamp));
                }

                ArrayList<String> receiverResources = getResources(receiverUsername);

                for (String resource: receiverResources)
                {
                    JID newReceiver = new JID(receiverUsername, receiverDomain, resource);

                    message.setTo(newReceiver);

                    messageRouter.route(message);
                }

                if (!chatState && !isReceipt(message))
                {
                    ArrayList<String> senderResources = getResources(senderUsername);

                    for (String resource: senderResources)
                    {
                        if (!resource.equals(senderResource))
                        {
                            JID newReceiver = new JID(senderUsername, senderDomain, resource);

                            Message newMessage = new Message();
                            newMessage.setTo(newReceiver);
                            newMessage.setFrom(sender);
                            newMessage.setType(message.getType());

                            Forwarded forwarded = new Forwarded(message);

                            newMessage.addExtension(new Sent(forwarded));
                            messageRouter.route(newMessage);
                        }
                    }
                }

                PacketRejectedException rejected = new PacketRejectedException("Packet rejected with disallowed content!");

                throw rejected;
            } else {
                // Log.info("Packet has resource {}", packet);
            } 

            // make a copy of the original packet only if required,
            // as it's an expensive operation
            // original = packet.createCopy();
        }
    }

    private static boolean isChatState(final Message message) {
        Iterator<?> it = message.getElement().elementIterator();

        while (it.hasNext()) {
            Object item = it.next();

            if (item instanceof Element) {
                Element el = (Element) item;
                if (Namespace.NO_NAMESPACE.equals(el.getNamespace())) {
                    continue;
                }
                if (el.getNamespaceURI().equals("http://jabber.org/protocol/chatstates") && !(el.getQualifiedName().equals("active"))) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean isReceipt(final Message message) {
        if ((message.getExtension("received", "urn:xmpp:receipts") != null) || (message.getExtension("seen", "urn:xmpp:receipts")) != null) {
            return true;
        }

        return false;
    }

    private static ArrayList<String> getResources(String username) {
        ArrayList<String> resources = new ArrayList<String>();

        Connection connection = null;
        Statement statement = null;
        
        try
        {
            String sql = String.format("SELECT resource FROM ofMultiplexer WHERE username = '%s'", username);

            connection = DbConnectionManager.getConnection();
			statement = connection.createStatement();
            ResultSet set = statement.executeQuery(sql);
            
            while (set.next()) { 
                resources.add(set.getString("resource"));
            }
            
            return resources;
        } catch (SQLException e) {
			Log.error(e.toString());
			return null;
        }
        finally {
            DbConnectionManager.closeConnection(statement, connection);
        }
    }
}
