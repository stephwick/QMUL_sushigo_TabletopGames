package groupB;

import core.AbstractGameState;
import core.interfaces.IStateHeuristic;
import core.components.Counter;
import games.sushigo.SGGameState;
import games.sushigo.cards.SGCard;

@SuppressWarnings("unused")
public class SushiGoCustomHeuristic implements IStateHeuristic {

    @Override
    public double evaluateState(AbstractGameState gs, int playerId) {
        SGGameState state = (SGGameState) gs;

        double score = state.getGameScore(playerId);

        try {
            // Dumpling (scale: 1=1, 2=3, 3=6, 4=10, 5+=15)
            Counter dumplings = state.getPlayedCardTypes(
                    SGCard.SGCardType.Dumpling, playerId
            );
            if (dumplings != null) {
                score += dumplings.getValue() * 1.5;
            }

            // Pudding (end game value
            Counter puddings = state.getPlayedCardTypes(
                    SGCard.SGCardType.Pudding, playerId
            );
            if (puddings != null) {
                score += puddings.getValue() * 2.0;
            }

            Counter maki = state.getPlayedCardTypes(
                    SGCard.SGCardType.Maki, playerId
            );
            if (maki != null) {
                score += maki.getValue() * 1.0;
            }

            // Tempura (a set of 2 is worth 5 points
            Counter tempura = state.getPlayedCardTypes(SGCard.SGCardType.Tempura, playerId);
            if (tempura != null) {
                int count = tempura.getValue();
                int completeSets = count / 2;
                int remainder = count % 2;
                score += (completeSets * 5.0) + (remainder * 2.0);
            }

            // Sashimi (a set of 3 is worth 10 points)
            Counter sashimi = state.getPlayedCardTypes(SGCard.SGCardType.Sashimi, playerId);
            if (sashimi != null) {
                int count = sashimi.getValue();
                int completeSets = count / 3;
                int remainder = count % 3;
                score += (completeSets * 10.0) + (remainder * 3.0);
            }
        } catch (Exception e) {
            // Error handling
        }
        return score;
    }
}
