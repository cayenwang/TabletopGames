package games.uno;

import core.AbstractForwardModel;
import core.actions.AbstractAction;
import core.components.Deck;
import core.AbstractGameState;
import core.AbstractGameParameters;
import games.uno.cards.*;
import core.interfaces.IObservation;
import games.uno.actions.NoCards;
import games.uno.actions.PlayCard;
import games.uno.actions.PlayWild;
import utilities.Utils;

import java.util.ArrayList;
import java.util.List;


public class UnoGameState extends AbstractGameState {
    List<Deck<UnoCard>>  playerDecks;
    Deck<UnoCard>        drawDeck;
    Deck<UnoCard>        discardDeck;
    UnoCard              currentCard;
    UnoCard.UnoCardColor currentColor;

    public UnoGameState(AbstractGameParameters gameParameters, AbstractForwardModel model, int nPlayers){
        super(gameParameters, model, new UnoTurnOrder(nPlayers));
    }

    @Override
    public void addAllComponents() {
        allComponents.putComponent(drawDeck);
        allComponents.putComponent(discardDeck);
        allComponents.putComponent(currentCard);
        allComponents.putComponents(drawDeck.getComponents());
        allComponents.putComponents(discardDeck.getComponents());
        allComponents.putComponents(playerDecks);
        for (Deck<UnoCard> d: playerDecks) {
            allComponents.putComponents(d.getComponents());
        }
    }

    boolean isWildCard(UnoCard card) {
        return card instanceof UnoWildCard || card instanceof UnoWildDrawFourCard;
    }

    boolean isNumberCard(UnoCard card) {
        return card instanceof UnoNumberCard;
    }

    @Override
    public void endGame() {
        System.out.println("Game Results:");
        for (int playerID = 0; playerID < getNPlayers(); playerID++) {
            if (playerResults[playerID] == Utils.GameResult.GAME_WIN) {
                System.out.println("The winner is the player : " + playerID);
                break;
            }
        }
    }

    @Override
    public List<AbstractAction> computeAvailableActions() {
        ArrayList<AbstractAction> actions = new ArrayList<>();
        int player = getCurrentPlayerID();

        Deck<UnoCard> playerHand = playerDecks.get(player);
        for (UnoCard card : playerHand.getComponents()) {
            int cardIdx = playerHand.getComponents().indexOf(card);
            if (card.isPlayable(this)) {
                if (isWildCard(card)) {
                    for (UnoCard.UnoCardColor color : UnoCard.UnoCardColor.values()) {
                        actions.add(new PlayWild(playerHand.getComponentID(), discardDeck.getComponentID(), cardIdx, color));
                    }
                }
                else {
                    actions.add(new PlayCard(playerHand.getComponentID(), discardDeck.getComponentID(), cardIdx));
                }
            }
        }

        if (actions.isEmpty())
            actions.add(new NoCards());

        return actions;
    }

    @Override
    public IObservation getObservation(int playerID) {
        Deck<UnoCard> playerHand = playerDecks.get(playerID);
        ArrayList<Integer> cardsLeft = new ArrayList<>();
        for( int i = 0; i < getNPlayers(); i++) {
            int nCards = playerDecks.get(playerID).getComponents().size();
            cardsLeft.add(nCards);
        }
        return new UnoObservation(currentCard, currentColor, playerHand, discardDeck, playerID, cardsLeft);
    }

    public int getCurrentPlayerID() {
        return turnOrder.getTurnOwner();
    }

    public void updateCurrentCard(UnoCard card) {
        currentCard  = card;
        currentColor = card.color;
    }

    public void updateCurrentCard(UnoCard card, UnoCard.UnoCardColor color) {
        currentCard  = card;
        currentColor = color;
    }

    public Deck<UnoCard> getDrawDeck() {
        return drawDeck;
    }

    public Deck<UnoCard> getDiscardDeck() {
        return discardDeck;
    }

    public List<Deck<UnoCard>> getPlayerDecks() {
        return playerDecks;
    }

    public UnoCard getCurrentCard() {
        return currentCard;
    }

    public UnoCard.UnoCardColor getCurrentColor() {
        return currentColor;
    }
}

