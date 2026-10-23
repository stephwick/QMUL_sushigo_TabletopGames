package groupB;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import games.sushigo.SGGameState;
import games.sushigo.SGParameters;
import games.sushigo.cards.SGCard.SGCardType;

/**
 * Version 2 of the Sushi Go heuristic.

 * Already in the game score (never added again): Tempura pairs, Sashimi triples,
 * Dumplings and Nigiri (including Wasabi multipliers), all scored when the card is revealed.

 * Added here (value the game has not paid out yet):
 *   - unfinished Tempura / Sashimi sets
 *   - Maki: most (6) and second most (3), split on ties, paid at round end
 *   - unused Wasabi (triples the next Nigiri)
 *   - unused Chopsticks (pick 2 cards in one turn)
 *   - Pudding: most (+6) and least (-6, only with 3+ players), paid at game end
 * Credits that can still be lost are scaled by how much of the round is left.
 */
public class SushiGoCustomHeuristicV2 implements IStateHeuristic {

    // Switches for ablation (recompile to change).
    private static final boolean USE_TIME_SCALING = true;
    private static final boolean USE_MAKI_SECOND = true;
    private static final boolean USE_WASABI = true;
    private static final boolean USE_CHOPSTICKS = true;
    private static final boolean USE_PUDDING = true;

    // Rough chances / discounts (not tuned).
    private static final double MAKI_CONFIDENCE = 0.5;    // a Maki lead can still be lost this round
    private static final double PUDDING_CONFIDENCE = 0.5; // a Pudding lead can still be lost before the end
    private static final double WASABI_CREDIT = 2.0;      // avg Nigiri 2 points, Wasabi adds 2x that, about half the time
    private static final double CHOPSTICKS_CREDIT = 1.0;  // one extra card (about 2 points), used about half the time

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        if (!gs.isNotTerminal()) {
            return gs.getPlayerResults()[playerId].value;
        }
        SGGameState state = (SGGameState) gs;
        SGParameters params = (SGParameters) state.getGameParameters();
        int n = state.getNPlayers();

        // 1.0 at the start of a round, close to 0 on the last pick.
        double remaining = 1.0;
        if (USE_TIME_SCALING) {
            int handSize = state.getPlayerHands().get(playerId).getSize();
            int startSize = params.nCards - n + 2;
            remaining = Math.max(0.0, Math.min(1.0, handSize / (double) startSize));
        }

        double opponents = 0;
        for (int p = 0; p < n; p++) {
            if (p != playerId) opponents += estimate(state, params, p, remaining);
        }
        opponents /= n - 1;

        return (estimate(state, params, playerId, remaining) - opponents) / 50.0;
    }

    private double estimate(SGGameState s, SGParameters params, int p, double remaining) {
        double score = s.getGameScore(p);
        double potential = 0; // value that can still be lost if the round runs out

        // Unfinished sets.
        if (count(s, SGCardType.Tempura, p) % 2 == 1) {
            potential += 0.5 * params.valueTempuraPair;
        }
        potential += (count(s, SGCardType.Sashimi, p) % 3) * 0.25 * params.valueSashimiTriple;

        potential += MAKI_CONFIDENCE * maki(s, params, p);

        if (USE_WASABI) {
            potential += WASABI_CREDIT * count(s, SGCardType.Wasabi, p);
        }
        if (USE_CHOPSTICKS) {
            potential += CHOPSTICKS_CREDIT * count(s, SGCardType.Chopsticks, p);
        }

        score += remaining * potential;

        if (USE_PUDDING) {
            score += PUDDING_CONFIDENCE * pudding(s, params, p);
        }
        return score;
    }

    // Maki points p would get if the round ended now (same rules as SGCard).
    private double maki(SGGameState s, SGParameters params, int p) {
        int mine = count(s, SGCardType.Maki, p);
        if (mine == 0) return 0;

        int n = s.getNPlayers();
        int most = 0;
        for (int i = 0; i < n; i++) most = Math.max(most, count(s, SGCardType.Maki, i));

        int tiedMost = 0, second = 0;
        for (int i = 0; i < n; i++) {
            int c = count(s, SGCardType.Maki, i);
            if (c == most) tiedMost++;
            else second = Math.max(second, c);
        }

        if (mine == most) return (double) params.valueMakiMost / tiedMost;

        // Second place only pays if nobody ties for most.
        if (USE_MAKI_SECOND && tiedMost == 1 && mine == second) {
            int tiedSecond = 0;
            for (int i = 0; i < n; i++) {
                if (count(s, SGCardType.Maki, i) == second) tiedSecond++;
            }
            return (double) params.valueMakiSecond / tiedSecond;
        }
        return 0;
    }

    // Pudding points p would get if the game ended now (same rules as SGCard),
    // weighted by game progress.
    private double pudding(SGGameState s, SGParameters params, int p) {
        int n = s.getNPlayers();
        int mine = count(s, SGCardType.Pudding, p);
        int best = 0, worst = Integer.MAX_VALUE;
        for (int i = 0; i < n; i++) {
            int c = count(s, SGCardType.Pudding, i);
            best = Math.max(best, c);
            worst = Math.min(worst, c);
        }
        if (best == worst) return 0; // nobody scores

        int tiedBest = 0, tiedWorst = 0;
        for (int i = 0; i < n; i++) {
            int c = count(s, SGCardType.Pudding, i);
            if (c == best) tiedBest++;
            if (c == worst) tiedWorst++;
        }
        double v = 0;
        if (mine == best) v += (double) params.valuePuddingMost / tiedBest;
        if (mine == worst && n > 2) v += (double) params.valuePuddingLeast / tiedWorst; // 2-player: ignored

        double progress = Math.min(1.0, (s.getRoundCounter() + 1.0) / params.nRounds);
        return progress * v;
    }

    private int count(SGGameState s, SGCardType type, int p) {
        return s.getPlayedCardTypes(type, p).getValue();
    }
}