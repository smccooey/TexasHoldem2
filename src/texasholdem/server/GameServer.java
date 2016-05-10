package texasholdem.server;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import texasholdem.TexasHoldemConstants;
import texasholdem.gamestate.GameState;
import texasholdem.gamestate.Player;

public class GameServer implements TexasHoldemConstants {

   /**
    * Socket for all communication with clients
    */
   private DatagramSocket socket;

   /**
    * Thread to listen for incoming packets
    */
   private ServerListener listener;

   /**
    * Scheduled timeouts for client acks for gamestate multicasts
    */
   private ConcurrentHashMap<Long, ScheduledFuture<?>> clientAckTimeouts;

   /**
    * Periodically multicasts heartbeats to clients
    */
   private HeartbeatSender hbSender;

   /**
    * Group address
    */
   private final SocketAddress group;

   /**
    * Current game mode
    */
   private int mode;

   /**
    * true if the game has been canceled
    */
   private volatile boolean cancel;

   /**
    * Main method.
    * @param args Command-line arguments
    */
   public static void main(String[] args) {
      new GameServer();
   }

   /**
    * Constructs a new game server.
    */
   private GameServer() {
      // Always start in waiting mode
      mode = WAITING_MODE;
      cancel = false;
      InetAddress groupAddress = null;
      try {
         groupAddress = InetAddress.getByAddress(MULTICAST_ADDRESS);
      }
      catch(UnknownHostException uhe) {
         uhe.printStackTrace();
         System.exit(1);
      }
      group = new InetSocketAddress(groupAddress, PORT);

      try {
         socket = new DatagramSocket(PORT);
      }
      catch(SocketException se) {
         se.printStackTrace();
         System.exit(1);
      }
      clientAckTimeouts = new ConcurrentHashMap<>();
      listener = new ServerListener(this, socket);
      listener.start();
      hbSender = new HeartbeatSender(group, socket);
      hbSender.start();
   }


/*
      Object request;
      byte[] recPacket = new byte[MAX_PACKET_SIZE];
      byte[] sendPacket;
      DatagramPacket packet = new DatagramPacket(recPacket, recPacket.length);
      InetAddress address;
      String instructions = "Send your MAC address";
      while (true) {
         try {
            socket = new MulticastSocket(PORT);
            while (true) {
               socket.receive(packet);
               request = SharedUtilities.toObject(packet.getData());
               address = packet.getAddress();
               sendPacket = SharedUtilities.toByteArray(instructions);
               if (request == "sick indicator to start game") {
                  packet = new DatagramPacket(sendPacket, sendPacket.length,
                        address, PORT);
                  socket.send(packet);
                  new GameServerThread(socket).start();
                  new HeartbeatSender(address, socket).start();
               }
               socket.close();
            }
         } catch (IOException ex) {
            ex.printStackTrace();
         } catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
         }
      }
   }
*/


   /**
    * Drops the specified player from the game.
    * @param id The id of the player to be dropped
    */
   void dropPlayer(long id) {

   }

   /**
    * Invoked when the listener receives an object (other than a heartbeat)
    * from a client.
    * @param obj The object received
    */
   void receiveObject(Object obj) {
      if(obj == null) {
         throw new RuntimeException("Null received by " +
               "GameServer#receiveObject");
      }
      else if(obj instanceof Player) {
         if(mode == WAITING_MODE) {
            // This is the first player. Start new game
         }
         else if(mode == PREGAME_MODE) {
            // Add player to game
         }
         else {
            // Game already underway; reject player
         }

      }
      else if(obj instanceof GameState) {
         GameState newGameState = (GameState)obj;
      }
      else {
         throw new RuntimeException("Unexpected " + obj.getClass().getName() +
               "received.");
      }
   }

   /**
    * Cancels the server and its associated classes.
    */
   private void cancel()  {
      hbSender.cancel();
      listener.cancel();
      cancel = true;
   }
}
