package jax.web;

import jax.auth.BattleNetAPI;
import jax.model.User;
import bowser.Request;
import bowser.RequestHandler;
import bowser.Response;

public class Redirector implements RequestHandler {

  private final BattleNetAPI battleNetAPI;

  public Redirector(BattleNetAPI battleNetAPI) {
    this.battleNetAPI = battleNetAPI;
  }

  @Override
  public boolean process(Request request, Response response) {
    if (request.isStaticResource()) {
      return false;
    }

    User user = request.get("user");

    if (user == null) {
      response.cookie("redirect", request.path);
      response.redirect(battleNetAPI.getAuthURL());
      return true;
    }

    return false;
  }

}
