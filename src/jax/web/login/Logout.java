package jax.web.login;

import bowser.Controller;
import bowser.RequestHandler;

public class Logout extends Controller {

  @Override
  public void init() {
    route("GET", "/logout").first(logout).to("logout.html");
  }
  
  private final RequestHandler logout = (request, response) -> {
    response.cookie("token", null);
    request.put("user", null);

    return false;
  };

}
