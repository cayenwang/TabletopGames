package games.cluedo;

import core.AbstractGameState;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.interfaces.IPlayerDecorator;
import games.cluedo.actions.GuessPartOfCaseFile;

import java.util.ArrayList;
import java.util.List;

// Decorator to only allow "sensible" actions
// (i.e. not guessing cards that they've already seen and not accusing unless certain)
public class CluedoActionDecorator implements IPlayerDecorator {

    @Override
    public List<AbstractAction> actionFilter(AbstractGameState state, List<AbstractAction> possibleActions) {
        CluedoGameState cgs = (CluedoGameState) state;

        List<Integer> hiddenCardsCount = cgs.getHiddenCardCount(cgs.getCurrentPlayer());
        int hiddenCardsTotal = hiddenCardsCount.get(0) + hiddenCardsCount.get(1) + hiddenCardsCount.get(2);

        List<AbstractAction> returnActions = new ArrayList<>();

        switch (cgs.getGamePhase().toString()) {
            case "chooseCharacter":
            case "revealCards":
                return possibleActions; // No need to restrict the actions

            case "makeSuggestion":
            case "makeAccusation":
                for (AbstractAction action : possibleActions) {
                    if (action instanceof DoNothing) {
                        if (hiddenCardsTotal != 3) {
                            returnActions.add(action);
                        }
                    } else if (action instanceof GuessPartOfCaseFile) {
                        if (hiddenCardsTotal == 3 || cgs.getGamePhase().equals(CluedoGameState.CluedoGamePhase.makeSuggestion)) {
                            String cardName = ((GuessPartOfCaseFile) action).getGuessName();
                            if (!cardIsVisible(cgs, cgs.getCurrentPlayer(), cardName)) {
                                returnActions.add(action);
                            }
                        }
                    } else {
                        throw new AssertionError("Action is not DoNothing nor GuessPartOfCaseFile");
                    }
                }
                if (returnActions.isEmpty()) {
                    returnActions.add(possibleActions.get(0));
                }
                return returnActions;
        }
        return List.of();
    }

    public Boolean cardIsVisible(CluedoGameState cgs, int playerId, String cardName) {
        for (int i = 0; i < cgs.getNPlayers(); i++) {
            for (int j = 0; j < cgs.getPlayerHandCards().get(i).getSize(); j++) {
                if (cgs.getPlayerHandCards().get(i).get(j).toString().equals(cardName)) {
                    return cgs.getPlayerHandCards().get(i).getVisibilityForPlayer(j, playerId);
                }
            }
        }
        return false;
    }
}
