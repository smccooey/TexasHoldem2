package texasholdem.server;

import texasholdem.gamestate.Card;
import texasholdem.gamestate.Deck;
import texasholdem.gamestate.IDeck;
import texasholdem.gamestate.Player;
import texasholdem.gamestate.RankingUtil;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

//SERVER SIDE GAME LOGIC!
public class ServerGameLogic implements Serializable {

   private static final long serialVersionUID = 967261359515323981L;
   private static final int BLIND = 50;
   private IDeck deck;
   private List<Player> players;
   private List<Card> tableCards;
   private int pot = 0;
   public Player OP;

   public void newGame(Player op, Player... _players) {
      deck = new Deck();
      tableCards = new ArrayList<Card>();
      players = new ArrayList<Player>();
      //the game needs at least one player
      ((Player) op).setAsOp();
      OP = op;
      players.add(op);
      players.addAll(Arrays.asList(_players));
   }

   public void addToPot(int p){
      pot += p;
   }

   //To abandon the game
   public void removePlayer(Player player) {
      players.remove(player);
   }

   public void deal() {
      for (Player player : players) {
         player.getCards()[0] = deck.pop();
         player.getCards()[1] = deck.pop();
         // take blinds as well
         pot += player.takeMoney(BLIND);
      }
      checkPlayersRanking();
   }

   /**
    * double initial bet
    */
   public void callFlop() {
      deck.pop();
      tableCards.add(deck.pop());
      tableCards.add(deck.pop());
      tableCards.add(deck.pop());
      checkPlayersRanking();
   }

   public void betTurn() {
      deck.pop();
      tableCards.add(deck.pop());
      checkPlayersRanking();
   }

   public void betRiver() {
      deck.pop();
      tableCards.add(deck.pop());
      checkPlayersRanking();
   }

   public List<Player> getWinner() {
      checkPlayersRanking();
      List<Player> winnerList = new ArrayList<Player>();
      Player winner = players.get(0);
      Integer winnerRank = RankingUtil.getRankingToInt(winner);
      winnerList.add(winner);
      for (int i = 1; i < players.size(); i++) {
         Player player = players.get(i);
         Integer playerRank = RankingUtil.getRankingToInt(player);
         //Draw game
         if (winnerRank == playerRank) {
            Player highHandPlayer = checkHighSequence(winner, player);
            //Draw checkHighSequence
            if (highHandPlayer == null) {
               highHandPlayer = checkHighCardWinner(winner, player);
            }
            //Not draw in checkHighSequence or checkHighCardWinner
            if (highHandPlayer != null && !winner.equals(highHandPlayer)) {
               winner = highHandPlayer;
               winnerList.clear();
               winnerList.add(winner);
            } else if (highHandPlayer == null) {
               //Draw in checkHighSequence and checkHighCardWinner
               winnerList.add(winner);
            }
         } else if (winnerRank < playerRank) {
            winner = player;
            winnerList.clear();
            winnerList.add(winner);
         }
         winnerRank = RankingUtil.getRankingToInt(winner);
      }

      return winnerList;
   }

   private Player checkHighSequence(Player player1, Player player2) {
      Integer player1Rank = sumRankingList(player1);
      Integer player2Rank = sumRankingList(player2);
      if (player1Rank > player2Rank) {
         return player1;
      } else if (player1Rank < player2Rank) {
         return player2;
      }
      return null;
   }

   @SuppressWarnings("unchecked")
   private Player checkHighCardWinner(Player player1, Player player2) {
      Player winner = compareHighCard(player1, player1.getHighCard(),
            player2, player2.getHighCard());
      if (winner == null) {
         Card player1Card = RankingUtil.getHighCard(player1,
               Collections.EMPTY_LIST);
         Card player2Card = RankingUtil.getHighCard(player2,
               Collections.EMPTY_LIST);
         winner = compareHighCard(player1, player1Card, player2, player2Card);
         if (winner != null) {
            player1.setHighCard(player1Card);
            player2.setHighCard(player2Card);
         } else if (winner == null) {
            player1Card = getSecondHighCard(player1, player1Card);
            player2Card = getSecondHighCard(player2, player2Card);
            winner = compareHighCard(player1, player1Card, player2,
                  player2Card);
            if (winner != null) {
               player1.setHighCard(player1Card);
               player2.setHighCard(player2Card);
            }
         }
      }
      return winner;
   }

   private Player compareHighCard(Player player1, Card player1HighCard,
         Player player2, Card player2HighCard) {
      if (player1HighCard.getRankToInt() > player2HighCard.getRankToInt()) {
         return player1;
      } else if (player1HighCard.getRankToInt() < player2HighCard
            .getRankToInt()) {
         return player2;
      }
      return null;
   }

   /*
    * TODO This method must be moved to RankingUtil
    */
   private Card getSecondHighCard(Player player, Card card) {
      if (player.getCards()[0].equals(card)) {
         return player.getCards()[1];
      }
      return player.getCards()[0];
   }

   public List<Card> getTableCards() {
      return tableCards;
   }

   /*
    * TODO This method must be moved to RankingUtil
    */
   private Integer sumRankingList(Player player) {
      Integer sum = 0;
      for (Card card : player.getRankingList()) {
         sum += card.getRankToInt();
      }
      return sum;
   }

   private void checkPlayersRanking() {
      for (Player player : players) {
         RankingUtil.checkRanking(player, tableCards);
      }
   }

   public List<Player> getPlayers() {
      return players;
   }

   public void printGameStatus(){
      System.out.println("Cards on table");
      for(Card card : tableCards){
         System.out.print(card.toString()+":");
      }
      System.out.println("\nPOT: " + pot);

      System.out.println("Players");
      for(Player p : players){
         System.out.print(p.getUsername()+":"+p.getAmountOfMoney()+" ");
      }
      System.out.println();
   }

   public int getPot() {
      return pot;
   }

   public void setPot(int pot) {
      this.pot = pot;
   }
}