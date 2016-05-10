package texasholdem.server;

import java.io.*;
import java.net.*;

import texasholdem.TexasHoldemConstants;
import texasholdem.SharedUtilities;
import texasholdem.gamestate.GameState;
import texasholdem.gamestate.Player;

public class GameServerThread extends Thread implements TexasHoldemConstants{

   int DROP_TIMER = HEARTBEAT_INTERVAL * 10;
   byte[] packSize = new byte[MAX_PACKET_SIZE];
   byte[] info;
   DatagramSocket socket;
   boolean lobby, gameSession;

   DatagramPacket packet = new DatagramPacket(packSize, packSize.length);

   public GameServerThread(DatagramSocket sock) {
      super("ServerThread");
      socket = sock;
   }

   public void run() {
      GameState game = new GameState();
      Player player;
      while (lobby /* in the lobby gathering players */) {
         try {
            socket.receive(packet);
            info = SharedUtilities.toByteArray(packet.getData());
            if (new String(info).equals("start the game")) {
               lobby = false;
            } else {
               player = new Player(info);
               game.addPlayer(player);
            }

         } catch (IOException ex) {
            ex.printStackTrace();
         }
      }
      /* playing the game until only one player remains, then maybe terminate,
         or we could bounce back up to lobby to recruit more  players */
      while (gameSession) {
         try {
            socket.receive(packet);
            info = SharedUtilities.toByteArray(packet.getData());
            if (true /*Heartbeat*/) {

            } else if (true /*Action*/) {
               game.action(info);
            } else {
               /* tell the client it's dumb */
            }

         } catch (IOException ex) {
            ex.printStackTrace();
         }
      }
   }
}
