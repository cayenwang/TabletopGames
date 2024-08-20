package games.cluedo.heuristics;

import core.AbstractGameState;
import core.AbstractParameters;
import core.interfaces.IStateHeuristic;
import evaluation.optimisation.TunableParameters;
import games.cluedo.CluedoGameState;
import players.simple.OSLAPlayer;

import java.util.List;


// This agent ignores re-guessing cards it's already seen, but otherwise guesses according to the gameState heuristic, only accusing when certain
public class OSLAPlusHeuristic extends TunableParameters {

    private IStateHeuristic heuristic = this::evaluateState;

    public OSLAPlusHeuristic() {
        addTunableParameter("heuristic", (IStateHeuristic) this::evaluateState);
        _reset();
    }

    @Override
    protected AbstractParameters _copy() {
        OSLAPlusHeuristic retValue = new OSLAPlusHeuristic();
        retValue.heuristic = heuristic;
        return retValue;
    }

    @Override
    public void _reset() {
        if (heuristic instanceof TunableParameters) {
            TunableParameters tunableHeuristic = (TunableParameters) heuristic;
            for (String name : tunableHeuristic.getParameterNames()) {
                tunableHeuristic.setParameterValue(name, this.getParameterValue("heuristic." + name));
            }
        }
    }

    @Override
    protected boolean _equals(Object o) {
        if (o instanceof OSLAPlusHeuristic) {
           OSLAPlusHeuristic other = (OSLAPlusHeuristic) o;
           return other.heuristic.equals(heuristic);
        }
        return false;
    }

    @Override
    public Object instantiate() {
        return new OSLAPlayer(heuristic);
    }

    public double evaluateState(AbstractGameState gs, int playerId) {
        CluedoGameState cgs = (CluedoGameState) gs;

        List<Integer> hiddenCardsCount = cgs.getHiddenCardCount(playerId);
        int hiddenCardsTotal = hiddenCardsCount.get(0) + hiddenCardsCount.get(1) + hiddenCardsCount.get(2);

        // If the agent makes an accusation without being certain, return -1
        if (cgs.getGamePhase() == CluedoGameState.CluedoGamePhase.makeAccusation
                && hiddenCardsTotal != 3) {
            if (cgs.currentGuess.getSize() == 0) return 1; // If the agent skips accusing, return 1
            else return -1;
        }

        // If the agent guesses a card it's seen before, return -1, else return the gameState heuristic value
        if ((cgs.getGamePhase() == CluedoGameState.CluedoGamePhase.makeSuggestion
            || cgs.getGamePhase() == CluedoGameState.CluedoGamePhase.makeAccusation)
            && cgs.currentGuess.getSize() != 0) {

            for (int i = 0; i < cgs.getNPlayers(); i++) {
                for (int j = 0; j < cgs.getPlayerHandCards().get(i).getSize(); j++) {
                    if (cgs.currentGuess.contains(cgs.getPlayerHandCards().get(i).get(j))) {
                        if (cgs.getPlayerHandCards().get(i).getVisibilityForPlayer(j, playerId)) return -1;
                    }
                }
            }
            return cgs.getHeuristicScore(playerId);
        }

        return 0;
    }

}
