package jax;

import jasonlib.swing.component.GButton;
import jasonlib.swing.component.GFrame;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import javax.swing.JPanel;
import com.google.common.base.Splitter;

public class PasteHelper {

  public static void main(String[] args) {
    String ss = "/announce <b>Lord Jaraxxus: </b>Greetings mortals.\n"
        +
        "/announce <b>Lord Jaraxxus: </b>You have entered the domain of Jaraxxus, eredar lord of the Burning Legion.\n"
        +
        "/announce <b>Lord Jaraxxus: </b>Today, you will fight to the death until only one worthy champion remains.\n"
        +
        "/announce <b>Lord Jaraxxus: </b>It will not be an easy victory. Rexxar's vermin have been spreading like a plague. Valeera's daily oil bath is beginning to make her reek.\n"
        +
        "/announce <b>Lord Jaraxxus: </b>Loyal followers know there is only one true way to win. Summon me, and I will show you true power.\n"
        +
        "/sound //wowimg.zamimg.com/hearthhead/sounds/VO_EX1_323_Attack_02.ogg\n" +
        "/announce <b>Lord Jaraxxus: </b>Let the games begin!";

    JPanel content = new JPanel();
    for (String s : Splitter.on("\n").split(ss)) {
      GButton button = new GButton(s.substring(32, 42));
      content.add(button);
      button.click(() -> {
        StringSelection selection = new StringSelection(s);
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(selection, selection);
      });
    }

    GFrame frame = GFrame.create().content(content).size(400, 200).start();
    frame.setAlwaysOnTop(true);

  }

}
