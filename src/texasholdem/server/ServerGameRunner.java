/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package texasholdem.server;

import csc445.Serializer;
import texasholdem.gamestate.Player;
import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class handles networking required for game, and starting and finishing a
 * game.
 *
 * @author al3x901
 */
public class ServerGameRunner {

   private ServerGameLogic game;
   private GameState gameState;

   public ServerGameRunner() {
      // TODO code application logic here
      game = new ServerGameLogic();
      gameState = new GameState();
      Player homer = new Player("homer");
      Player flanders = new Player("flanders");
      Player andres = new Player("andres");
      game.newGame(homer, flanders, andres);
      play();
   }

   private void play() {
      gameState.setCurrentPlayer(game.OP);
      game.deal();
      updateGameStateObject();

      while (gameState.getHandsDealt() <= 3) {
         game.printGameStatus();
         System.out.println("");
         System.out.println("");
         int nextPlayer = -1;
         gameState.setNumberOfturnsLeft(gameState.getPlayers().size());
         while (gameState.getNumberOfturnsLeft() > 0) {
            //TODO: Make players call if raised.
            nextPlayer++;
            int next = nextPlayer % gameState.getPlayers().size();
            Player currentPlayer = game.getPlayers().get(next);
            gameState.setCurrentPlayer(currentPlayer);
            /*SGS with this player*/

            /*Happens in client*/
            currentPlayer.takeTurn();
            /*Server waits, and gest GS back updates currentPlayer and reacts, */

            react(currentPlayer.getTurnOutCome(), currentPlayer);
            currentPlayer.resetTakeTurn();
            /*USGS, move on to next player*/
            //game.printGameStatus();
            gameState.setNumberOfturnsLeft((gameState.getNumberOfturnsLeft() - 1));
            updateGameStateObject();
            System.out.println(gameState.getMessage()+":"+game.getPot());
         }
         gameState.setHandsDealt(gameState.getHandsDealt()+1);

         switch(gameState.getHandsDealt()){
            case 1:
               game.callFlop();
               updateGameStateObject();
               break;
            case 2:
               game.betTurn();
               updateGameStateObject();
               break;
            case 3:
               game.betRiver();
               updateGameStateObject();
               break;
            default:
               break;
         }

      }
      Serializer s = new Serializer();
      try {
         System.out.println(s.serialize(gameState).length);
         //TODO: USGS = update and send gameStateObject
         // We have to wait for all players to say what they want to do.

         /* TODO: USGS,setting currentPlayer in GS to player at i and client is going to call takeTurn.
         * client will update GS with it's own player object. Send back to server.Wait for a response with updated GS object. Check currentPlayers takeTurn field, react accordingly,reset its value.
         *  USGS. Move on to next player.
         */
      } catch (IOException ex) {
         Logger.getLogger(ServerGameRunner.class.getName()).log(Level.SEVERE, null, ex);
      }
   }

   //CHECK 0, FOLD 1,RAISE 2
   public void react(int turnOutCome, Player p) {
      if (turnOutCome == 1) {
         // FOLD
         game.removePlayer(p);
         gameState.setMessage(p.getUsername() + " has folded");
      } else if (turnOutCome > 1) {
         // RAISE
         game.addToPot(turnOutCome);
         gameState.setMessage(p.getUsername() + " raised " + turnOutCome);
         gameState.setNumberOfturnsLeft(gameState.getPlayers().size()+1);
      } else if (turnOutCome == 0) {
         gameState.setMessage(p.getUsername() + " checked");
      }
      p.setTurnOutCome(-1);
   }

   public void updateGameStateObject() {
      gameState.setPlayers(game.getPlayers());
      gameState.setPot(game.getPot());
      gameState.setTableCards(game.getTableCards());
   }

}