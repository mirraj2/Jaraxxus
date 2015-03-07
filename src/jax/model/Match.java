package jax.model;

import static com.google.common.base.Preconditions.checkState;
import static jax.model.TournamentManager.BYE;

public class Match {

  public Long id;
  public final long event;
  public final int round;
  public final String playerA, playerB;
  public final String playerANick, playerBNick;

  public Match(Long id, long event, int round, String playerA, String playerB) {
    this.id = id;
    this.event = event;
    this.round = round;
    this.playerA = playerA;
    this.playerB = playerB;
    this.playerANick = User.prettify(playerA);
    this.playerBNick = User.prettify(playerB);

    checkState(!BYE.equals(playerA), "We assume that BYEs are in the playerB position");
  }

  public boolean isBye() {
    return BYE.equals(playerB);
  }

}
