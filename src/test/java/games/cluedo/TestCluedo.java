package games.cluedo;

import core.AbstractParameters;
import core.AbstractPlayer;
import core.CoreConstants;
import core.Game;
import core.actions.AbstractAction;
import core.actions.DoNothing;
import core.components.BoardNode;
import games.GameType;
import games.cluedo.actions.ChooseCharacter;
import games.cluedo.actions.GuessPartOfCaseFile;
import games.cluedo.actions.ShowHintCard;
import org.junit.Before;
import org.junit.Test;
import players.simple.RandomPlayer;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class TestCluedo {
    Game cluedo;
    CluedoForwardModel cfm = new CluedoForwardModel();
    List<AbstractPlayer> players;
    AbstractParameters gameParameters;

    @Before
    public void setup() {
        // Feel free to change how many characters this game is set up with
        // The tests should all work with different numbers of players
        players = List.of(
                new RandomPlayer(),
                new RandomPlayer(),
                new RandomPlayer(),
                new RandomPlayer(),
                new RandomPlayer(),
                new RandomPlayer()
        );
        gameParameters = new CluedoParameters();
        cluedo = GameType.Cluedo.createGameInstance(players.size(),100, gameParameters);
        cluedo.reset(players);
    }

    // ========== setup ==========

    @Test
    public void testSetupCaseFile() {
        CluedoGameState cgs = (CluedoGameState) cluedo.getGameState();

        // Assert caseFile has 3 cards: 1 Room card, 1 Character card, 1 Weapon card
        assertEquals(3, cgs.caseFile.getSize());
        CluedoConstants.Weapon.valueOf(cgs.caseFile.get(0).toString()); // Will throw an exception if cgs.caseFile.get(0) is not in the enum of Weapons
        CluedoConstants.Character.valueOf(cgs.caseFile.get(1).toString());
        CluedoConstants.Room.valueOf(cgs.caseFile.get(2).toString());
    }

    @Test
    public void testSetupPlayerHands() {
        CluedoGameState cgs = (CluedoGameState) cluedo.getGameState();

        // Assert players have been dealt the expected number of cards
        // (assuming there are 21 cards per the original game)
        List<Integer> actualPlayerHandSize = new ArrayList<>();
        for (int i=0; i<cgs.getNPlayers(); i++) {
            actualPlayerHandSize.add(cgs.playerHandCards.get(i).getSize());
        }
        List<Integer> expectedPlayerHandSize = switch (cgs.getNPlayers()) {
            case 2 -> List.of(9, 9);
            case 3 -> List.of(6, 6, 6);
            case 4 -> List.of(5, 5, 4, 4);
            case 5 -> List.of(4, 4, 4, 3, 3);
            case 6 -> List.of(3, 3, 3, 3, 3, 3);
            default -> List.of();
        };
        assertEquals(expectedPlayerHandSize, actualPlayerHandSize);
    }

    @Test
    public void testSetupGameBoard() {
        CluedoGameState cgs = (CluedoGameState) cluedo.getGameState();

        // Assert gameBoard is a fully connected graph with (#ofRooms + 1) nodes
        assert(cgs.gameBoard.getBoardNodes().size() == CluedoConstants.Room.values().length + 1);
        for (BoardNode node : cgs.gameBoard.getBoardNodes()) {
            // Correct number of neighbours
            assert(node.getNeighbours().size() == CluedoConstants.Room.values().length);
            // No node is a neighbour to itself
            assert(!(node.getNeighbours().contains(node)));
            // All neighbours are distinct
            assertEquals(node.getNeighbours().stream().distinct().collect(Collectors.toList()), node.getNeighbours().stream().toList());
        }
    }

    @Test
    public void testSetupCharacterLocation() {
        CluedoGameState cgs = (CluedoGameState) cluedo.getGameState();

        // Assert all characters start in the START node
        List<String> expectedCharacterLocations = List.of("START", "START", "START", "START", "START", "START");
        assertEquals(expectedCharacterLocations, cgs.characterLocations);
    }

    @Test
    public void testSetupGamePhase() {
        CluedoGameState cgs = (CluedoGameState) cluedo.getGameState();
        CluedoParameters cp = (CluedoParameters) cgs.getGameParameters();

        if (cp.getChooseCharacters()) {
            assertEquals(CluedoGameState.CluedoGamePhase.chooseCharacter, cgs.getGamePhase());
        } else {
            assertEquals(CluedoGameState.CluedoGamePhase.makeSuggestion, cgs.getGamePhase());
        }
    }

    // ========== computeAvailableActions ==========

    @Test
    public void testComputeAvailableActionsChooseCharactersPhase() {
        CluedoGameState cgs = (CluedoGameState) cluedo.getGameState();
        CluedoParameters cp = (CluedoParameters) cgs.getGameParameters();

        if (cp.getChooseCharacters()) {
            // Assert the first player can choose any character
            List<AbstractAction> actions0 = cfm.computeAvailableActions(cgs);
            assertEquals(List.of(0,1,2,3,4,5), computeAvailableCharacterIndexes(actions0));

            // Populate characterToPlayerMap as if some players have chosen their character
            for (int i=0;i< cgs.getNPlayers()-1; i++) {
                cgs.characterToPlayerMap.put(i,i);
            }

            // Assert the next player can choose any character not previously chosen
            List<AbstractAction> actions1 = cfm.computeAvailableActions(cgs);
            List<Integer> expectedList = new ArrayList<>();
            for (int i=cgs.getNPlayers()-1; i<6; i++) {
                expectedList.add(i);
            }
            assertEquals(expectedList, computeAvailableCharacterIndexes(actions1));
        }
    }

    @Test
    public void testComputeAvailableActionsMakeGuessPhase() { // either makeSuggestion or makeAccusation
        CluedoGameState cgs = (CluedoGameState) cluedo.getGameState();

        for (CluedoGameState.CluedoGamePhase phase : new CluedoGameState.CluedoGamePhase[]{
                CluedoGameState.CluedoGamePhase.makeSuggestion, CluedoGameState.CluedoGamePhase.makeAccusation }) {

            cgs.setGamePhase(phase);
            cgs.currentGuess.clear();

            // Assert player can first choose any room
            List<AbstractAction> actions0 = cfm.computeAvailableActions(cgs);
            for (CluedoConstants.Room room : CluedoConstants.Room.values()) {
                assert(computeAvailableGuessNames(actions0).contains(room.name()));
            }
            // Populate the caseFile as if the player chose a room
            cgs.currentGuess.add(new CluedoCard(CluedoConstants.Room.LOUNGE));

            // Assert player can next choose any character
            List<AbstractAction> actions1 = cfm.computeAvailableActions(cgs);
            for (CluedoConstants.Character character : CluedoConstants.Character.values()) {
                assert(computeAvailableGuessNames(actions1).contains(character.name()));
            }
            // Populate the caseFile as if the player chose a character
            cgs.currentGuess.add(new CluedoCard(CluedoConstants.Character.DR_ORCHID));

            // Assert player can next choose any weapon
            List<AbstractAction> actions2 = cfm.computeAvailableActions(cgs);
            for (CluedoConstants.Weapon weapon : CluedoConstants.Weapon.values()) {
                assert(computeAvailableGuessNames(actions2).contains(weapon.name()));
            }
        }
    }

    @Test
    public void testComputeAvailableActionsRevealCardsPhase() {
        CluedoGameState cgs = (CluedoGameState) cluedo.getGameState();

        cgs.setGamePhase(CluedoGameState.CluedoGamePhase.revealCards);
        cgs.currentTurnPlayerId = 1;

        // Populate the currentGuess as if player 1 had made a suggestion
        cgs.currentGuess.add(cgs.allCards.get(0));
        cgs.currentGuess.add(cgs.allCards.get(1));
        cgs.currentGuess.add(cgs.allCards.get(2));

        cgs.playerHandCards.get(0).clear();

        // Assert player 0 cannot show any cards to player 1
        List<AbstractAction> actions0 = cfm.computeAvailableActions(cgs);
        assert(actions0.size() == 1);
        assert(actions0.get(0) instanceof DoNothing);

        for (int i=0; i<3; i++) {
            // Suppose player 0 had i+1 cards in their hand that were in the currentGuess
            cgs.playerHandCards.get(0).add(cgs.allCards.get(i));
            // Assert player 0 has i+1 options for which card to show player 1
            List<AbstractAction> actions1 = cfm.computeAvailableActions(cgs);
            assert(actions1.size() == i+1);
        }
    }

    @Test
    public void testComputeAvailableActionsMakeAccusationPhase() {
        CluedoGameState cgs = (CluedoGameState) cluedo.getGameState();

        cgs.setGamePhase(CluedoGameState.CluedoGamePhase.makeAccusation);

        // Assert player has the option to not make an accusation
        List<AbstractAction> actions0 = cfm.computeAvailableActions(cgs);
        assert(actions0.get(actions0.size()-1) instanceof DoNothing);

    }

    private List<Integer> computeAvailableCharacterIndexes(List<AbstractAction> actions) {
        List<Integer> availableCharacterIndexes = new ArrayList<>();
        for (AbstractAction availableAction : actions) {
            availableCharacterIndexes.add(((ChooseCharacter) availableAction).getCharacter());
        }
        return availableCharacterIndexes;
    }

    private List<String> computeAvailableGuessNames(List<AbstractAction> actions) {
        List<String> availableGuessNames = new ArrayList<>();
        for (AbstractAction availableAction : actions) {
            if (!(availableAction instanceof DoNothing)) {
                availableGuessNames.add(((GuessPartOfCaseFile) availableAction).getGuessName());
            }
        }
        return availableGuessNames;
    }

    // ========== afterAction ==========

    @Test
    public void testAfterActionChooseCharactersPhase() {
        CluedoGameState cgs = (CluedoGameState) cluedo.getGameState();
        CluedoParameters cp = (CluedoParameters) cgs.getGameParameters();

        if (cp.getChooseCharacters()) {
            for (int i=0; i<cgs.getNPlayers()-1; i++) {
                cfm.next(cgs, new ChooseCharacter(i, 5-i));
                // Assert we have moved to the next player
                assertEquals(i+1, cgs.getCurrentPlayer());
            }

            cfm.next(cgs, new ChooseCharacter(cgs.getNPlayers()-1, 6-cgs.getNPlayers()));
            // After the final player has chosen a character, assert that the gamePhase has changed, and turn order is now by character index
            assertEquals(CluedoGameState.CluedoGamePhase.makeSuggestion, cgs.getGamePhase());

            Queue<Integer> expectedTurnOrder = new LinkedList<>();
            for (int i=1; i<cgs.getNPlayers(); i++) {
                expectedTurnOrder.add(cgs.getNPlayers()-i-1);
            }
            expectedTurnOrder.add(cgs.getNPlayers()-1);
            assertEquals(expectedTurnOrder, cgs.getTurnOrderQueue());
            assertEquals(cgs.getNPlayers()-1, cgs.getCurrentPlayer());
        }
    }

    @Test
    public void testAfterActionMakingSuggestionPhase() {
        CluedoGameState cgs = (CluedoGameState) cluedo.getGameState();

        for (int i=0; i<cgs.getNPlayers(); i++) {
            cgs.characterToPlayerMap.put(i,i);
        }
        cfm.setPlayerOrder(cgs, cgs.characterToPlayerMap);
        cgs.setGamePhase(CluedoGameState.CluedoGamePhase.makeSuggestion);

        cfm.next(cgs, new GuessPartOfCaseFile(cgs, CluedoConstants.Room.KITCHEN));
        // Assert player 0 has moved to the kitchen
        assertEquals("KITCHEN", cgs.characterLocations.get(0));

        cfm.next(cgs, new GuessPartOfCaseFile(cgs, CluedoConstants.Character.DR_ORCHID));
        // Assert Dr Orchid has moved to the kitchen as well
        assertEquals("KITCHEN", cgs.characterLocations.get(2));

        cfm.next(cgs, new GuessPartOfCaseFile(cgs, CluedoConstants.Weapon.ROPE));
        // After the final suggestion has been made, assert that the gamePhase has changed and all players have been added to reactivePlayers
        assertEquals(CluedoGameState.CluedoGamePhase.revealCards, cgs.getGamePhase());

        Queue<Integer> expectedReactivePlayers = new LinkedList<>();
        for (int i=1; i<cgs.getNPlayers(); i++) {
            expectedReactivePlayers.add(i);
        }
        assertEquals(expectedReactivePlayers, cgs.reactivePlayers);
    }

    @Test
    public void testAfterActionRevealingCardsPhase() {
        CluedoGameState cgs = (CluedoGameState) cluedo.getGameState();

        cgs.setGamePhase(CluedoGameState.CluedoGamePhase.revealCards);
        // Suppose player 1 is making a suggestion currently
        cgs.currentTurnPlayerId = 1;
        cfm.next(cgs, new DoNothing());
        // If the player cannot show a card, assert the current player moves to the next player
        assertEquals(1, cgs.getCurrentPlayer());

        // Suppose player 0 is making a suggestion currently
        cgs.currentTurnPlayerId = 0;
        cfm.next(cgs, new ShowHintCard(1,0, 0));
        // If the player can show a card, assert the game phase has changed, the current player moves back to the player that made the suggestion and currentGuess is empty
        assertEquals(CluedoGameState.CluedoGamePhase.makeAccusation, cgs.getGamePhase());
        assertEquals(0, cgs.getCurrentPlayer());
        assertEquals(0, cgs.currentGuess.getSize());
    }

    @Test
    public void testAfterActionMakingAccusationPhaseCorrectGuess() {
        CluedoGameState cgs = (CluedoGameState) cluedo.getGameState();

        cgs.setGamePhase(CluedoGameState.CluedoGamePhase.makeAccusation);
        cfm.next(cgs, new GuessPartOfCaseFile(cgs, CluedoConstants.Room.valueOf(cgs.caseFile.get(2).getComponentName())));
        cfm.next(cgs, new GuessPartOfCaseFile(cgs, CluedoConstants.Character.valueOf(cgs.caseFile.get(1).getComponentName())));
        cfm.next(cgs, new GuessPartOfCaseFile(cgs, CluedoConstants.Weapon.valueOf(cgs.caseFile.get(0).getComponentName())));

        // Assert the game ends and the current player wins
        assertEquals(CoreConstants.GameResult.GAME_END, cgs.getGameStatus());
        assertEquals(CoreConstants.GameResult.WIN_GAME, cgs.getPlayerResults()[0]);
        for (int i=1; i<cgs.getNPlayers(); i++) {
            assertEquals(CoreConstants.GameResult.LOSE_GAME, cgs.getPlayerResults()[i]);
        }
    }

    @Test
    public void testAfterActionMakingAccusationPhaseOneIncorrectGuess() {
        CluedoGameState cgs = (CluedoGameState) cluedo.getGameState();

        cgs.setGamePhase(CluedoGameState.CluedoGamePhase.makeAccusation);
        // Assumes the seed doesn't set the case file to be DR_ORCHID, ROPE, KITCHEN (0.3% chance)
        cfm.next(cgs, new GuessPartOfCaseFile(cgs, CluedoConstants.Room.KITCHEN));
        cfm.next(cgs, new GuessPartOfCaseFile(cgs, CluedoConstants.Character.DR_ORCHID));
        cfm.next(cgs, new GuessPartOfCaseFile(cgs, CluedoConstants.Weapon.ROPE));

        // Assert the current player is set to be no longer alive and the game continues
        assertEquals(false, cgs.playerAliveStatus.get(0));
        assertEquals(CluedoGameState.CluedoGamePhase.makeSuggestion, cgs.getGamePhase());
        assertEquals(1, cgs.getCurrentPlayer());
    }

    @Test
    public void testAfterActionMakingAccusationPhaseAllIncorrectGuesses() {
        CluedoGameState cgs = (CluedoGameState) cluedo.getGameState();

        cgs.setGamePhase(CluedoGameState.CluedoGamePhase.makeAccusation);
        // Assume all other players have made incorrect guesses
        for (int i=1; i<cgs.getNPlayers(); i++) {
            cgs.playerAliveStatus.set(i, false);
        }
        // Assumes the seed doesn't set the case file to be DR_ORCHID, ROPE, KITCHEN (0.3% chance)
        cfm.next(cgs, new GuessPartOfCaseFile(cgs, CluedoConstants.Room.KITCHEN));
        cfm.next(cgs, new GuessPartOfCaseFile(cgs, CluedoConstants.Character.DR_ORCHID));
        cfm.next(cgs, new GuessPartOfCaseFile(cgs, CluedoConstants.Weapon.ROPE));

        // Assert the game ends and everyone loses
        assertEquals(CoreConstants.GameResult.GAME_END, cgs.getGameStatus());
        for (int i=0; i<cgs.getNPlayers(); i++) {
            assertEquals(CoreConstants.GameResult.LOSE_GAME, cgs.getPlayerResults()[i]);
        }
    }

    // ========== Other Tests ==========

    @Test
    public void testGetNextPlayerWithReactivePlayers() {
        CluedoGameState cgs = (CluedoGameState) cluedo.getGameState();

        cgs.turnOrderQueue.clear();
        cgs.turnOrderQueue.addAll(List.of(2, 1, 3, 0));
        cgs.reactivePlayers.addAll(List.of(3,0));

        assertEquals(3, cfm.nextPlayer(cgs));
    }

    @Test
    public void testGetNextPlayerWithoutReactivePlayers() {
        CluedoGameState cgs = (CluedoGameState) cluedo.getGameState();

        cgs.turnOrderQueue.clear();
        cgs.turnOrderQueue.addAll(List.of(2, 1, 3, 0));

        assertEquals(2, cfm.nextPlayer(cgs));

        Queue<Integer> expectedTurnOrderQueue = new LinkedList<>(List.of(1, 3, 0, 2));
        assertEquals(expectedTurnOrderQueue, cgs.turnOrderQueue);
    }

}
