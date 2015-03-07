package jax.web;

import static com.google.common.base.Preconditions.checkNotNull;
import jasonlib.Config;
import jasonlib.IO;
import jasonlib.Log;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import jax.auth.BattleNetAPI;
import jax.web.chat.ChatController;
import jax.web.chat.JaraxxusSocketServer;
import jax.web.events.EventSignupAPI;
import jax.web.events.EventsController;
import jax.web.faq.FAQController;
import jax.web.home.Home;
import jax.web.ladder.LadderController;
import jax.web.login.Login;
import jax.web.match.MatchController;
import jax.web.player.PlayerPage;
import bowser.WebServer;
import com.google.common.base.Throwables;

public class JaxServer {

  private final Config config = Config.load("jax");

  private void run() {
    boolean devMode = config.getBoolean("dev_mode", false);

    int httpPort = config.getInt("port", devMode ? 8001 : 80);
    int sslPort = devMode ? 8000 : 443;
    int websocketPort = config.getInt("websocket_port", 39141);

    Log.info("Starting web server on port " + httpPort);

    String apiKey = config.get("apiKey");
    String apiSecret = config.get("apiSecret");

    checkNotNull(apiKey, "You need to set apiKey in the config file.");
    checkNotNull(apiSecret, "you need to set apiSecret in the config file.");

    String domain = devMode ? "localhost" : "jaraxxus.com";
    BattleNetAPI battleNetAPI = new BattleNetAPI(apiKey, apiSecret, "https://" + domain + ":" + sslPort + "/callback");

    new WebServer("Jaraxxus", httpPort, devMode)
        .shortcut("jquery", "//cdnjs.cloudflare.com/ajax/libs/jquery/2.1.1/jquery.min.js")
        .shortcut("bootstrap", "//maxcdn.bootstrapcdn.com/bootstrap/3.3.1/css/bootstrap.min.css")
        .shortcut("bootstrap", "//cdnjs.cloudflare.com/ajax/libs/twitter-bootstrap/3.3.2/js/bootstrap.min.js")
        .shortcut("cookie", "//cdnjs.cloudflare.com/ajax/libs/jquery-cookie/1.4.1/jquery.cookie.min.js")
        .shortcut("buzz", "//cdnjs.cloudflare.com/ajax/libs/buzz/1.1.8/buzz.min.js")
        .add(new Authenticator())
        .controller(new ChatController(domain, websocketPort))
        .controller(new Login(battleNetAPI))
        .controller(new Home())
        .controller(new EventsController())
        .controller(new LadderController())
        .controller(new FAQController())
        .controller(new MatchController())
        .controller(new PlayerPage())
        .add(new Redirector(battleNetAPI))
        .controller(new EventSignupAPI())
        .start();

    new WebServer("Jaraxxus", sslPort, devMode)
        .controller(new Login(battleNetAPI))
        .add(new SSLRedirect(domain + ":" + httpPort))
        .ssl(getSSLContext())
        .start();

    new JaraxxusSocketServer(websocketPort).start();

    Log.info("Server started.");
  }

  private SSLContext getSSLContext() {
    try {
      KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
      keystore.load(IO.from(getClass(), "cert/keystore.jks").asStream(), "nimrod".toCharArray());

      KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
      keyManagerFactory.init(keystore, "nimrod".toCharArray());

      SSLContext ret = SSLContext.getInstance("SSLv3");
      // sslContext.init(keyManagerFactory.getKeyManagers(), new TrustManager[] { new NaiveX509TrustManager() },
      ret.init(keyManagerFactory.getKeyManagers(), null, null);

      return ret;
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  public static void main(String[] args) {
    new JaxServer().run();
  }

}
