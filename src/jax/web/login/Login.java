package jax.web.login;

import jasonlib.Log;
import java.util.concurrent.TimeUnit;
import jax.auth.BattleNetAPI;
import jax.auth.BattleNetAPI.LoginData;
import jax.db.SessionDB;
import jax.db.UserDB;
import jax.model.User;
import bowser.Controller;
import bowser.Handler;

public class Login extends Controller {

  private final UserDB userDB = new UserDB();
  private final SessionDB sessionDB = new SessionDB();
  private BattleNetAPI battleNetAPI;

  public Login(BattleNetAPI battleNetAPI) {
    this.battleNetAPI = battleNetAPI;
  }

  @Override
  public void init() {
    route("GET", "/callback").to(callback);
    route("GET", "/login").to(login);
  }

  private final Handler login = (request, response) -> {
    response.redirect(battleNetAPI.getAuthURL());
  };

  private final Handler callback = (request, response) -> {
    String code = request.param("code");

    LoginData data = battleNetAPI.login(code);

    User user = userDB.get(data.accountId);

    if (user == null) {
      String battleTag = battleNetAPI.getBattleTag(data.token);

      user = new User(data.accountId, battleTag, false);
      userDB.createUser(user);

      Log.info("Created a new user: " + battleTag);
    }

    String token = sessionDB.newSession(user.battleId, data.token);

    response.cookie("token", token, 29, TimeUnit.DAYS);
    response.cookie("redirect", null);

    String target = request.cookie("redirect");
    if (target == null) {
      target = "/";
    }
    response.redirect(target);
  };

}
