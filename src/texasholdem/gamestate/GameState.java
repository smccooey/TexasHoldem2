/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package texasholdem.gamestate;

import java.io.Serializable;
import java.util.List;

/**
 *
 * @author al3x901
 */
public class GameState implements Serializable{

   private Player currentPlayer; // WHOSE TURN IS IT?
   private List<Player> players; // needed because when same gs object is sent to all players, playes need to be individually updated.
   private int sequenceNumber;
   private List<Card> tableCards;
   private int pot = 0;
   private int numberOfturnsLeft;
   private int handsDealt = 0;
   private String message;
   private int mode; // what mode is the server in

   public int getMode() {
      return mode;
   }

   public void setMode(int mode) {
      this.mode = mode;
   }

   public GameState(){

   }

   public String getMessage() {
      return message;
   }

   public void setMessage(String message) {
      this.message = message;
   }

   public int getHandsDealt() {
      return handsDealt;
   }

   public void setHandsDealt(int handsDealt) {
      this.handsDealt = handsDealt;
   }

   public int getNumberOfturnsLeft() {
      return numberOfturnsLeft;
   }

   public void setNumberOfturnsLeft(int numberOfturnsLeft) {
      this.numberOfturnsLeft = numberOfturnsLeft;
   }

   public Player getCurrentPlayer() {
      return currentPlayer;
   }

   public List<Card> getTableCards() {
      return tableCards;
   }

   public int getPot() {
      return pot;
   }

   public void setCurrentPlayer(Player currentPlayer) {
      this.currentPlayer = currentPlayer;
   }


   public void setTableCards(List<Card> tableCards) {
      this.tableCards = tableCards;
   }

   public void setPot(int pot) {
      this.pot = pot;
   }

   public List<Player> getPlayers() {
      return players;
   }

   public void setPlayers(List<Player> players) {
      this.players = players;
   }

   @Override
   public String toString() {
      return "GameState{" + "currentPlayer=" + currentPlayer + ", players=" + players.size() + ", tableCards=" + tableCards.size() + ", pot=" + pot + ", numberOfturnsLeft=" + numberOfturnsLeft + '}';
   }
}