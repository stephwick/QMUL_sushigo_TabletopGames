
package groupB;

import core.AbstractGameState;
import core.components.Counter;
import core.interfaces.IStateHeuristic;
import games.sushigo.SGGameState;
import games.sushigo.SGParameters;
import games.sushigo.cards.SGCard.SGCardType;

public class SushiGoCustomHeuristic implements IStateHeuristic {

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {

        // Use the actual result when the game has ended.
        if (!gs.isNotTerminal()) {
            return gs.getPlayerResults()[playerId].value;
        }

        SGGameState state = (SGGameState) gs;

        // Calculate the average estimated score of the opponents.
        double opponentTotal = 0.0;
        int nPlayers = state.getNPlayers();

        for (int p = 0; p < nPlayers; p++) {
            if (p != playerId) {
                opponentTotal += estimate(state, p);
            }
        }

        double averageOpponent = nPlayers > 1
                ? opponentTotal / (nPlayers - 1)
                : 0.0;

        // Return a normalised relative score.
        return (estimate(state, playerId) - averageOpponent) / 50.0;
    }

    /**
     * Estimates a player's current score plus potential points
     * from incomplete sets and their current Maki lead.
     */
    private double estimate(SGGameState state, int playerId) {

        SGParameters params =
                (SGParameters) state.getGameParameters();

        // Points already awarded by the framework.
        double score = state.getGameScore(playerId);

        // Estimate progress towards completing a Tempura pair.
        int tempura = count(state, SGCardType.Tempura, playerId);

        if (tempura % 2 == 1) {
            score += 0.5 * params.valueTempuraPair;
        }

        // Estimate progress towards completing a Sashimi triple.
        int sashimi = count(state, SGCardType.Sashimi, playerId) % 3;

        if (sashimi == 1) {
            score += 0.25 * params.valueSashimiTriple;
        } else if (sashimi == 2) {
            score += 0.5 * params.valueSashimiTriple;
        }

        // Estimate a discounted Maki bonus if currently leading.
        int mine = count(state, SGCardType.Maki, playerId);
        int tiedMost = 0;
        int highest = 0;

        for (int p = 0; p < state.getNPlayers(); p++) {
            int maki = count(state, SGCardType.Maki, p);
            highest = Math.max(highest, maki);
        }

        if (mine > 0 && mine == highest) {
            for (int p = 0; p < state.getNPlayers(); p++) {
                if (count(state, SGCardType.Maki, p) == highest) {
                    tiedMost++;
                }
            }

            if (tiedMost > 0) {
                score += 0.5 * params.valueMakiMost / tiedMost;
            }
        }

        return score;
    }
    /**
     * Returns the number of cards of a type played by a player.
     * A missing counter is treated as zero.
     */
    private int count(SGGameState state, SGCardType type, int playerId) {

        Counter counter = state.getPlayedCardTypes(type, playerId);

        return counter == null ? 0 : counter.getValue();
    }
}
