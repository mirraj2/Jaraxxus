package jax.auth;

import jasonlib.IO;
import jasonlib.IO.Input;
import jasonlib.Json;
import jasonlib.Log;
import java.util.Map;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public class BattleNetAPI {

  private final String apiKey, apiSecret, callback;

  public BattleNetAPI(String apiKey, String apiSecret, String callback) {
    this.apiKey = apiKey;
    this.apiSecret = apiSecret;
    this.callback = callback;
  }

  public String getBattleTag(String token) {
    Json json = get("https://us.api.battle.net/account/user/battletag", false,
        ImmutableMap.of("access_token", token));
    return json.get("battletag");
  }

  public String getAuthURL() {
    return String.format("https://us.battle.net/oauth/authorize?client_id=%s"
        + "&redirect_uri=%s&response_type=code", apiKey, callback);
  }

  public LoginData login(String code) {
    String url = "https://us.battle.net/oauth/token";

    Map<String, String> params = Maps.newLinkedHashMap();
    params.put("grant_type", "authorization_code");
    params.put("client_id", apiKey);
    params.put("client_secret", apiSecret);
    params.put("code", code);
    params.put("redirect_uri", callback);

    Json json = get(url, true, params);

    return new LoginData(json.get("access_token"), json.getInt("accountId"));
  }

  private Json get(String url, boolean post, Map<String, String> params) {
    StringBuilder sb = new StringBuilder();
    sb.append(url).append("?");
    params.forEach((key, value) -> {
      sb.append(key).append("=").append(value).append("&");
    });
    sb.setLength(sb.length() - 1);

    String fullURL = sb.toString();

    Exception e = null;
    for (int i = 0; i < 5; i++) {
      try {
        Input input = IO.fromURL(fullURL);
        if (post) {
          input.httpPost();
        }
        return input.toJson();
      } catch (Exception ee) {
        e = ee;
        if (i < 4) {
          Log.error("Problem contacting Battle.net. Trying again...");
        }
      }
    }
    throw Throwables.propagate(e);
  }

  public static class LoginData {
    public String token;
    public int accountId;

    public LoginData(String token, int accountId) {
      this.token = token;
      this.accountId = accountId;
    }
  }

}
