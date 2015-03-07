package jax.web.faq;

import bowser.Controller;

public class FAQController extends Controller {

  @Override
  public void init() {
    route("GET", "/faq").to("faq.html");
  }

}
