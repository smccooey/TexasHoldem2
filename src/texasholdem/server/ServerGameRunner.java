/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package texasholdem.server;

import texasholdem.gamestate.GameState;
import texasholdem.gamestate.Player;

/**
 * This class handles networking required for game, and starting and finishing a game.
 *
 * @author al3x901
 */
public class ServerGameRunner {

   private ServerGameLogic game;
   private GameState gameState;

   public ServerGameRunner(GameState gameState) {
      // TODO code application logic here
      game = new ServerGameLogic();
      this.gameState = gameState;
      game.newGame(gameState.getPlayers());
      play();
   }

   private void play() {
      gameState.setCurrentPlayer(game.OP);
      game.deal();
      updateGameStateObject();

      main:
      while (gameState.getHandsDealt() <= 3) {
         game.printGameStatus();
         System.out.println("");
         System.out.println("");
         int nextPlayer = -1;
         gameState.setNumberOfturnsLeft(gameState.getPlayers().size());
         while (gameState.getNumberOfturnsLeft() > 0) {
            //TODO: Make players call if raised.
            nextPlayer++;
            int numberOfPlayers = gameState.getPlayers().size();
            int next = nextPlayer % numberOfPlayers;
            Player currentPlayer = game.getPlayers().get(next);
            gameState.setCurrentPlayer(currentPlayer);
            /*SGS with this player*/
            GameServer.getInstance().multicastGameState(gameState);
            synchronized(this) {
               try {
                  wait();
               }
               catch(InterruptedException ie) {
                  // Do nothing
               }
               catch(Exception e) {
                  e.printStackTrace();
                  System.exit(1);
               }
            }
            /*Happens in client*/
            currentPlayer.takeTurn();
            /*Server waits, and gest GS back updates currentPlayer and reacts, */
            // 0 fold , 1 game is finished, 2 player just raised
            switch (react(currentPlayer.getTurnOutCome(), currentPlayer)) {
               case 0:
                  nextPlayer--;
                  break;
               case 1:
                  break main;
               case 2:
                  switchCallModeOn(next, currentPlayer.getTurnOutCome());
                  break;
               case 3:
                  break;
               default:
                  break;
            }

            currentPlayer.resetTakeTurn();
            currentPlayer.setCallMode(false);
            /*USGS, move on to next player*/
            //game.printGameStatus();
            gameState.setNumberOfturnsLeft((gameState.getNumberOfturnsLeft() - 1));
            updateGameStateObject();
            currentPlayer.showState();
            System.out.println(gameState.getMessage() + " Pot : " + game.getPot());
         }
         gameState.setHandsDealt(gameState.getHandsDealt() + 1);
         resetMoneyOnTable();
         switch (gameState.getHandsDealt()) {
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
   }

   //CHECK 0, FOLD 1,RAISE 2, CALL 3
   // REACT_CODE: 0 folded player, 1 only one player left, 2 player raised
   public int react(int turnOutCome, Player p) {
      int REACT_CODE = -1;
      if (turnOutCome == 1) {
         // FOLD
         game.removePlayer(p);
         gameState.setMessage(p.getUsername() + " has folded");
         REACT_CODE = 0;
         if (gameState.getPlayers().size() == 1) {
            REACT_CODE = 1;
         }
      } else if (turnOutCome == 0) {
         //CHECKED
         gameState.setMessage(p.getUsername() + " checked");
      } else if (turnOutCome > 1) {
         // RAISE - call
         game.addToPot(turnOutCome);
         if (p.isCallMode()) {
            //Calling
            gameState.setMessage(p.getUsername() + " called " + turnOutCome);
            REACT_CODE = 3;
         } else {
            //Raising
            gameState.setMessage(p.getUsername() + " raised " + turnOutCome);
            gameState.setNumberOfturnsLeft(gameState.getPlayers().size() + 1);
            REACT_CODE = 2;
         }
      }
      return REACT_CODE;
   }

   public void updateGameStateObject() {
      gameState.setPlayers(game.getPlayers());
      gameState.setPot(game.getPot());
      gameState.setTableCards(game.getTableCards());
   }

   public void switchCallModeOn(int currentPlayer, int amountRaised) {
      // resetting values
      for (int i = 0; i < game.getPlayers().size(); i++) {
         game.getPlayers().get(i).setCallMode(false);
      }

      for (int i = 0; i < game.getPlayers().size(); i++) {
         if (i != currentPlayer) {
            game.getPlayers().get(i).setCallMode(true);
            game.getPlayers().get(i).setRaiseAmount(amountRaised);
         }
      }
   }

   public void resetMoneyOnTable() {
      // resetting values
      for (int i = 0; i < game.getPlayers().size(); i++) {
         game.getPlayers().get(i).setAmountOnTable(0);
         game.getPlayers().get(i).setRaiseAmount(0);
      }
   }
}