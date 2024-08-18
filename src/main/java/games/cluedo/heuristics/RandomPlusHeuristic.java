package games.cluedo.heuristics;

import core.AbstractGameState;
import core.AbstractParameters;
import core.interfaces.IStateHeuristic;
import evaluation.optimisation.TunableParameters;
import games.cluedo.CluedoCard;
import games.cluedo.CluedoGameState;
import players.simple.OSLAPlayer;


/**
 * This is a wrapper to use any parameterisable heuristic with the OSLA player
 */
public class RandomPlusHeuristic extends TunableParameters {

    int plyDepth = 1;
    private IStateHeuristic heuristic = this::evaluateState;

    public RandomPlusHeuristic() {
        addTunableParameter("heuristic", (IStateHeuristic) this::evaluateState);
        _reset();
    }

    @Override
    protected AbstractParameters _copy() {
        RandomPlusHeuristic retValue = new RandomPlusHeuristic();
        retValue.plyDepth = plyDepth;
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
        if (o instanceof RandomPlusHeuristic) {
           RandomPlusHeuristic other = (RandomPlusHeuristic) o;
           return other.plyDepth == plyDepth && other.heuristic.equals(heuristic);
        }
        return false;
    }

    @Override
    public Object instantiate() {
        return new OSLAPlayer(heuristic);
    }

    public double evaluateState(AbstractGameState gs, int playerId) {
        CluedoGameState cgs = (CluedoGameState) gs;

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

            return 1;
        }
        return 0;
    }

}
