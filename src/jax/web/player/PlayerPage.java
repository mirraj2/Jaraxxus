package jax.web.player;

import bowser.Controller;

public class PlayerPage extends Controller {

  @Override
  public void init() {
    route("GET", "/players/*").to("player.html");
  }

}
