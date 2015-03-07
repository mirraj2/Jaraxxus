package jax.web;

import jasonlib.Log;
import bowser.Request;
import bowser.RequestHandler;
import bowser.Response;

public class SSLRedirect implements RequestHandler {

  private final String domain;

  public SSLRedirect(String domain) {
    this.domain = domain;
  }

  @Override
  public boolean process(Request request, Response response) {
    String path = request.path;
    Log.debug("redirecting from SSL: " + path);

    response.redirect("http://" + domain + path);
    
    return true;
  }

}
