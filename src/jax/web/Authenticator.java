package jax.web;

import jax.db.SessionDB;
import jax.db.UserDB;
import jax.model.User;
import bowser.Request;
import bowser.RequestHandler;
import bowser.Response;

public class Authenticator implements RequestHandler {

  private final SessionDB sessionDB = new SessionDB();
  private final UserDB userDB = new UserDB();

  @Override
  public boolean process(Request request, Response response) {
    if (request.isStaticResource()) {
      return false;
    }

    String session = request.cookie("token");

    if (session != null) {
      Integer battleId = sessionDB.getBattleId(session);
      if (battleId != null) {
        User user = userDB.get(battleId);
        request.put("user", user);
        return false;
      }
    }

    return false;
  }

}
