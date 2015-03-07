package jax.web.chat;

import jasonlib.IO;
import java.util.concurrent.TimeUnit;
import bowser.Controller;
import bowser.Handler;

public class ChatController extends Controller {

  private String domain;
  private int webSocketPort;

  public ChatController(String domain, int webSocketPort) {
    this.domain = domain;
    this.webSocketPort = webSocketPort;
  }

  @Override
  public void init() {
    route("GET", "/chat2.js").to(chatScript);
  }

  private Handler chatScript = (request, response) -> {
    response.contentType("text/javascript");

    if (!getServer().developerMode) {
      response.cacheFor(20, TimeUnit.MINUTES);
    }

    String js = new String(getServer().getResourceLoader().getData(request.path));
    js = js.replace("$WEBSOCKET_IP", domain);
    js = js.replace("$WEBSOCKET_PORT", webSocketPort + "");

    IO.from(js).to(response.getOutputStream());
  };

}
