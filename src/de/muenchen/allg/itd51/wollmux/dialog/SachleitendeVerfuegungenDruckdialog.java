/* 
 * Dateiname: SachleitendeVerfuegungenDruckdialog.java
 * Projekt  : WollMux
 * Funktion : Implementiert den Dialog zum Drucken von Sachleitenden Verf�gungen
 * 
 * Copyright: Landeshauptstadt M�nchen
 *
 * �nderungshistorie:
 * Datum      | Wer | �nderungsgrund
 * -------------------------------------------------------------------
 * 09.10.2006 | LUT | Erstellung (basierend auf AbsenderAuswaehlen.java)
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * @version 1.0
 * 
 */
package de.muenchen.allg.itd51.wollmux.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import de.muenchen.allg.itd51.parser.ConfigThingy;
import de.muenchen.allg.itd51.parser.NodeNotFoundException;
import de.muenchen.allg.itd51.wollmux.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.Logger;
import de.muenchen.allg.itd51.wollmux.SachleitendeVerfuegung.Verfuegungspunkt;
import de.muenchen.allg.itd51.wollmux.db.DatasourceJoiner;

/**
 * Diese Klasse baut anhand einer als ConfigThingy �bergebenen
 * Dialogbeschreibung einen Dialog zum Drucken von Sachleitenden Verf�gungen.
 * Die private-Funktionen d�rfen NUR aus dem Event-Dispatching Thread heraus
 * aufgerufen werden.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1), Christoph Lutz (D-III-ITD 5.1)
 */
public class SachleitendeVerfuegungenDruckdialog
{
  /**
   * Rand um Textfelder (wird auch f�r ein paar andere R�nder verwendet) in
   * Pixeln.
   */
  private final static int TF_BORDER = 4;

  /**
   * Rand �ber und unter einem horizontalen Separator (in Pixeln).
   */
  private final static int SEP_BORDER = 7;

  /**
   * Rand um Buttons (in Pixeln).
   */
  private final static int BUTTON_BORDER = 2;

  /**
   * ActionListener f�r Buttons mit der ACTION "printElement".
   */
  private ActionListener actionListener_printElement = new ActionListener()
  {
    public void actionPerformed(ActionEvent e)
    {
      printElement();
    }
  };

  /**
   * ActionListener f�r Buttons mit der ACTION "printAll".
   */
  private ActionListener actionListener_printAll = new ActionListener()
  {
    public void actionPerformed(ActionEvent e)
    {
      printAll();
    }
  };

  /**
   * ActionListener f�r Buttons mit der ACTION "printSettings".
   */
  private ActionListener actionListener_printSettings = new ActionListener()
  {
    public void actionPerformed(ActionEvent e)
    {
      printSettings();
    }
  };

  /**
   * ActionListener f�r Buttons mit der ACTION "back".
   */
  private ActionListener actionListener_back = new ActionListener()
  {
    public void actionPerformed(ActionEvent e)
    {
      back();
    }
  };

  /**
   * ActionListener f�r Buttons mit der ACTION "abort".
   */
  private ActionListener actionListener_abort = new ActionListener()
  {
    public void actionPerformed(ActionEvent e)
    {
      abort();
    }
  };

  /**
   * wird getriggert bei windowClosing() Event.
   */
  private ActionListener closeAction = actionListener_abort;

  /**
   * Der Rahmen des gesamten Dialogs.
   */
  private JFrame myFrame;

  /**
   * Das JPanel der obersten Hierarchiestufe.
   */
  private JPanel mainPanel;

  /**
   * Die Array mit allen comboBoxen, die elementCount beinhalten.
   */
  private JSpinner[] elementCountSpinner;

  /**
   * Die Array mit allen comboBoxen, die verf�gungspunkte+zuleitungszeilen
   * beinhalten.
   */
  private JComboBox[] elementComboBoxes;

  /**
   * Die Array mit allen buttons auf printElement-Actions
   */
  private JButton[] printElementButtons;

  /**
   * Der dem
   * {@link #AbsenderAuswaehlen(ConfigThingy, ConfigThingy, DatasourceJoiner, ActionListener) Konstruktor}
   * �bergebene dialogEndListener.
   */
  private ActionListener dialogEndListener;

  /**
   * Vector of Verfuegungspunkt, der die Beschreibungen der Verf�gungspunkte
   * enth�lt.
   */
  private Vector verfuegungspunkte;

  /**
   * Erzeugt einen neuen Dialog.
   * 
   * @param conf
   *          das ConfigThingy, das den Dialog beschreibt (der Vater des
   *          "Fenster"-Knotens.
   * @param dialogEndListener
   *          falls nicht null, wird die
   *          {@link ActionListener#actionPerformed(java.awt.event.ActionEvent)}
   *          Methode aufgerufen (im Event Dispatching Thread), nachdem der
   *          Dialog geschlossen wurde. Das actionCommand des ActionEvents gibt
   *          die Aktion an, die das Beenden des Dialogs veranlasst hat.
   * @param verfuegungspunkte
   *          Vector of Verfuegungspunkt, der die Beschreibungen der
   *          Verf�gungspunkte enth�lt.
   * @throws ConfigurationErrorException
   *           im Falle eines schwerwiegenden Konfigurationsfehlers, der es dem
   *           Dialog unm�glich macht, zu funktionieren (z.B. dass der "Fenster"
   *           Schl�ssel fehlt.
   */
  public SachleitendeVerfuegungenDruckdialog(ConfigThingy conf,
      Vector /* of Verfuegungspunkt */verfuegungspunkte,
      ActionListener dialogEndListener) throws ConfigurationErrorException
  {
    this.verfuegungspunkte = verfuegungspunkte;
    this.dialogEndListener = dialogEndListener;

    ConfigThingy fensterDesc1 = conf.query("Fenster");
    if (fensterDesc1.count() == 0)
      throw new ConfigurationErrorException("Schl�ssel 'Fenster' fehlt in "
                                            + conf.getName());

    final ConfigThingy fensterDesc = fensterDesc1.query("Drucken");
    if (fensterDesc.count() == 0)
      throw new ConfigurationErrorException("Schl�ssel 'Drucken' fehlt in "
                                            + conf.getName());

    // GUI im Event-Dispatching Thread erzeugen wg. Thread-Safety.
    try
    {
      javax.swing.SwingUtilities.invokeLater(new Runnable()
      {
        public void run()
        {
          try
          {
            createGUI(fensterDesc.getLastChild());
          }
          catch (Exception x)
          {
          }
        }
      });
    }
    catch (Exception x)
    {
      Logger.error(x);
    }
  }

  /**
   * Erzeugt das GUI.
   * 
   * @param fensterDesc
   *          die Spezifikation dieses Dialogs.
   * @author Matthias Benkmann (D-III-ITD 5.1), Christoph Lutz (D-III-ITD 5.1)
   */
  private void createGUI(ConfigThingy fensterDesc)
  {
    Common.setLookAndFeelOnce();

    int size = verfuegungspunkte.size();

    // element
    elementComboBoxes = new JComboBox[size];
    elementCountSpinner = new JSpinner[size];
    printElementButtons = new JButton[size];

    for (int i = 0; i < size; ++i)
    {
      Verfuegungspunkt verfPunkt = (Verfuegungspunkt) verfuegungspunkte.get(i);
      Vector zuleitungszeilen = verfPunkt.getZuleitungszeilen();

      // elementComboBoxes vorbelegen:
      Vector content = new Vector();
      content.add(verfPunkt.getHeading());
      Iterator iter = verfPunkt.getZuleitungszeilen().iterator();
      while (iter.hasNext())
      {
        String zuleitung = (String) iter.next();
        content.add(zuleitung);
      }
      elementComboBoxes[i] = new JComboBox(content);

      // elementCountComboBoxes vorbelegen:
      SpinnerNumberModel model = new SpinnerNumberModel(
          zuleitungszeilen.size(), 0, 50, 1);
      elementCountSpinner[i] = new JSpinner(model);

      // printElementButtons vorbelegen:
      printElementButtons[i] = new JButton();
    }

    String title = "TITLE fehlt f�r Fenster Drucken";
    try
    {
      title = fensterDesc.get("TITLE").toString();
    }
    catch (Exception x)
    {
    }

    try
    {
      closeAction = getAction(fensterDesc.get("CLOSEACTION").toString());
    }
    catch (Exception x)
    {
    }

    // Create and set up the window.
    myFrame = new JFrame(title);
    // leave handling of close request to WindowListener.windowClosing
    myFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    myFrame.addWindowListener(new MyWindowListener());

    mainPanel = new JPanel(new BorderLayout());
    mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    myFrame.getContentPane().add(mainPanel);

    JPanel verfPunktPanel = new JPanel(new GridBagLayout());
    JPanel buttons = new JPanel(new GridBagLayout());

    mainPanel.add(verfPunktPanel, BorderLayout.CENTER);
    mainPanel.add(buttons, BorderLayout.PAGE_END);

    for (int i = 0; i < size; i++)
    {
      addUIElements(fensterDesc, "Verfuegungspunkt", i, verfPunktPanel, 1, 0);
    }

    // separator zwischen Verf�gungspunkte und Summenzeile hinzuf�gen
    GridBagConstraints gbcSeparator = new GridBagConstraints(0, 0,
        GridBagConstraints.REMAINDER, 1, 1.0, 0.0, GridBagConstraints.CENTER,
        GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);
    JPanel uiElement = new JPanel(new GridLayout(1,1));
    uiElement.add(new JSeparator(SwingConstants.HORIZONTAL));
    uiElement.setBorder(BorderFactory.createEmptyBorder(SEP_BORDER,0,SEP_BORDER,0));
    gbcSeparator.gridy = size;
    verfPunktPanel.add(uiElement, gbcSeparator);

    addUIElements(fensterDesc, "AllElements", size+1, verfPunktPanel, 1, 0);
    addUIElements(fensterDesc, "Buttons", 0, buttons, 1, 0);

    myFrame.pack();
    int frameWidth = myFrame.getWidth();
    int frameHeight = myFrame.getHeight();
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    int x = screenSize.width / 2 - frameWidth / 2;
    int y = screenSize.height / 2 - frameHeight / 2;
    myFrame.setLocation(x, y);
    myFrame.setResizable(false);
    myFrame.setVisible(true);
    myFrame.requestFocus();
  }

  /**
   * F�gt compo UI Elemente gem�ss den Kindern von conf.query(key) hinzu. compo
   * muss ein GridBagLayout haben. stepx und stepy geben an um wieviel mit jedem
   * UI Element die x und die y Koordinate der Zelle erh�ht werden soll.
   * Wirklich sinnvoll sind hier nur (0,1) und (1,0).
   */
  private void addUIElements(ConfigThingy conf, String key, int verfPunktNr,
      JComponent compo, int stepx, int stepy)
  {
    // int gridx, int gridy, int gridwidth, int gridheight, double weightx,
    // double weighty, int anchor, int fill, Insets insets, int ipadx, int
    // ipady)
    // GridBagConstraints gbcTextfield = new GridBagConstraints(0, 0, 1, 1, 1.0,
    // 0.0, GridBagConstraints.LINE_START, GridBagConstraints.HORIZONTAL, new
    // Insets(TF_BORDER,TF_BORDER,TF_BORDER,TF_BORDER),0,0);
    GridBagConstraints gbcLabel = new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
        GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(
            TF_BORDER, TF_BORDER, TF_BORDER, TF_BORDER), 0, 0);
    GridBagConstraints gbcGlue = new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0,
        GridBagConstraints.LINE_START, GridBagConstraints.BOTH, new Insets(0,
            0, 0, 0), 0, 0);
    GridBagConstraints gbcButton = new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0,
        GridBagConstraints.LINE_START, GridBagConstraints.NONE, new Insets(
            BUTTON_BORDER, BUTTON_BORDER, BUTTON_BORDER, BUTTON_BORDER), 0, 0);
    GridBagConstraints gbcComboBox = new GridBagConstraints(0, 0, 1, 1, 1.0,
        1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(
            TF_BORDER, TF_BORDER, TF_BORDER, TF_BORDER), 0, 0);
    GridBagConstraints gbcTextField = new GridBagConstraints(0, 0, 1, 1, 1.0,
        1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(
            TF_BORDER, TF_BORDER, TF_BORDER, TF_BORDER), 0, 0);
    GridBagConstraints gbcSpinner = new GridBagConstraints(0, 0, 1, 1, 1.0,
        1.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(
            TF_BORDER, TF_BORDER, TF_BORDER, TF_BORDER), 0, 0);

    ConfigThingy felderParent = conf.query(key);
    int y = -stepy + verfPunktNr;
    int x = -stepx;

    Iterator piter = felderParent.iterator();
    while (piter.hasNext())
    {
      Iterator iter = ((ConfigThingy) piter.next()).iterator();
      while (iter.hasNext())
      {
        y += stepy;
        x += stepx;

        ConfigThingy uiElementDesc = (ConfigThingy) iter.next();
        try
        {
          /*
           * ACHTUNG! DER FOLGENDE CODE SOLLTE SO GESCHRIEBEN WERDEN, DASS DER
           * ZUSTAND AUCH IM FALLE EINES GESCHEITERTEN GET() UND EINER EVTL.
           * DARAUS RESULTIERENDEN NULLPOINTEREXCEPTION NOCH KONSISTENT IST!
           */

          // boolean readonly = false;
          String id = "";
          try
          {
            id = uiElementDesc.get("ID").toString();
          }
          catch (NodeNotFoundException e)
          {
          }
          // try{ if (uiElementDesc.get("READONLY").toString().equals("true"))
          // readonly = true; }catch(NodeNotFoundException e){}
          String type = uiElementDesc.get("TYPE").toString();

          if (type.equals("label"))
          {
            JLabel uiElement = new JLabel();
            gbcLabel.gridx = x;
            gbcLabel.gridy = y;
            compo.add(uiElement, gbcLabel);
            uiElement.setText(uiElementDesc.get("LABEL").toString());
          }

          else if (type.equals("glue"))
          {
            Box uiElement = Box.createHorizontalBox();
            try
            {
              int minsize = Integer.parseInt(uiElementDesc.get("MINSIZE")
                  .toString());
              uiElement.add(Box.createHorizontalStrut(minsize));
            }
            catch (Exception e)
            {
            }
            uiElement.add(Box.createHorizontalGlue());

            gbcGlue.gridx = x;
            gbcGlue.gridy = y;
            compo.add(uiElement, gbcGlue);
          }

          else if (type.equals("spinner"))
          {
            JSpinner spinner;
            if (id.equals("elementCount")
                && verfPunktNr < elementCountSpinner.length)
              spinner = elementCountSpinner[verfPunktNr];
            else
              spinner = new JSpinner(new SpinnerNumberModel(0, 0, 0, 0));

            // comboBox.addListSelectionListener(myListSelectionListener);

            // String action = "";
            // try{ action = uiElementDesc.get("ACTION").toString();
            // }catch(NodeNotFoundException e){}
            //            
            // ActionListener actionL = getAction(action);
            // if (actionL != null) comboBox.addMouseListener(new
            // MyActionMouseListener(comboBox, actionL));

            gbcSpinner.gridx = x;
            gbcSpinner.gridy = y;
            compo.add(spinner, gbcSpinner);
          }

          else if (type.equals("combobox"))
          {
            JComboBox comboBox;
            if (id.equals("element") && verfPunktNr < elementComboBoxes.length)
              comboBox = elementComboBoxes[verfPunktNr];
            else
              comboBox = new JComboBox();

            // comboBox.addListSelectionListener(myListSelectionListener);

            gbcComboBox.gridx = x;
            gbcComboBox.gridy = y;
            compo.add(comboBox, gbcComboBox);
          }

          else if (type.equals("textfield"))
          {
            JTextField textField;
            if (id.equals("allElementCount"))
            {
              textField = new JTextField("" + getAllElementCount());
              textField.setEditable(false);
            }
            else
              textField = new JTextField();

            gbcTextField.gridx = x;
            gbcTextField.gridy = y;
            compo.add(textField, gbcTextField);
          }

          else if (type.equals("button"))
          {
            String action = "";
            try
            {
              action = uiElementDesc.get("ACTION").toString();
            }
            catch (NodeNotFoundException e)
            {
            }

            String label = uiElementDesc.get("LABEL").toString();

            char hotkey = 0;
            try
            {
              hotkey = uiElementDesc.get("HOTKEY").toString().charAt(0);
            }
            catch (Exception e)
            {
            }

            JButton button = new JButton(label);
            button.setMnemonic(hotkey);

            gbcButton.gridx = x;
            gbcButton.gridy = y;
            compo.add(button, gbcButton);

            ActionListener actionL = getAction(action);
            if (actionL != null) button.addActionListener(actionL);

          }
          else
          {
            Logger.error("Ununterst�tzter TYPE f�r User Interface Element: "
                         + type);
          }
        }
        catch (NodeNotFoundException e)
        {
          Logger.error(e);
        }
      }
    }
  }

  /**
   * TODO: berechnung von allElementCount
   * 
   * @return
   */
  private int getAllElementCount()
  {
    int count = 0;
    for (int i = 0; i < elementCountSpinner.length; i++)
    {
      count += new Integer(elementCountSpinner[i].getValue().toString())
          .intValue();
    }
    return count;
  }

//  /**
//   * Wartet auf Doppelklick und f�hrt dann die actionPerformed() Methode eines
//   * ActionListeners aus.
//   * 
//   * @author Matthias Benkmann (D-III-ITD 5.1)
//   */
//  private class MyActionMouseListener extends MouseAdapter
//  {
//    private JList list;
//
//    private ActionListener action;
//
//    public MyActionMouseListener(JList list, ActionListener action)
//    {
//      this.list = list;
//      this.action = action;
//    }
//
//    public void mouseClicked(MouseEvent e)
//    {
//      if (e.getClickCount() == 2)
//      {
//        Point location = e.getPoint();
//        int index = list.locationToIndex(location);
//        if (index < 0) return;
//        Rectangle bounds = list.getCellBounds(index, index);
//        if (!bounds.contains(location)) return;
//        action.actionPerformed(null);
//      }
//    }
//  }

  /**
   * �bersetzt den Namen einer ACTION in eine Referenz auf das passende
   * actionListener_... Objekt.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1), Christoph Lutz (D-III-ITD 5.1)
   */
  private ActionListener getAction(String action)
  {
    if (action.equals("abort"))
    {
      return actionListener_abort;
    }
    else if (action.equals("back"))
    {
      return actionListener_back;
    }
    else if (action.equals("printElement"))
    {
      return actionListener_printElement;
    }
    else if (action.equals("printAll"))
    {
      return actionListener_printAll;
    }
    else if (action.equals("printSettings"))
    {
      return actionListener_printSettings;
    }
    else if (action.equals(""))
    {
      return null;
    }
    else
      Logger.error("Ununterst�tzte ACTION: " + action);

    return null;
  }

  /**
   * Implementiert die gleichnamige ACTION.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void abort()
  {
    dialogEnd("abort");
  }

  /**
   * Implementiert die gleichnamige ACTION.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void back()
  {
    dialogEnd("back");
  }

  /**
   * Beendet den Dialog und liefer actionCommand an den dialogEndHandler zur�ck
   * (falls er nicht null ist).
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void dialogEnd(String actionCommand)
  {
    myFrame.dispose();
    if (dialogEndListener != null)
      dialogEndListener
          .actionPerformed(new ActionEvent(this, 0, actionCommand));
  }

  /**
   * TODO: doc printSettings
   * 
   * @author christoph.lutz
   */
  private void printSettings()
  {
    // TODO Auto-generated method stub

  }

  /**
   * TODO: doc printAll
   * 
   * @author christoph.lutz
   */
  private void printAll()
  {
    // TODO Auto-generated method stub

  }

  /**
   * TODO: doc printElement()
   * 
   * @author christoph.lutz
   */
  private void printElement()
  {
    // TODO Auto-generated method stub

  }

  /**
   * Ein WindowListener, der auf den JFrame registriert wird, damit als Reaktion
   * auf den Schliessen-Knopf auch die ACTION "abort" ausgef�hrt wird.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private class MyWindowListener implements WindowListener
  {
    public MyWindowListener()
    {
    }

    public void windowActivated(WindowEvent e)
    {
    }

    public void windowClosed(WindowEvent e)
    {
    }

    public void windowClosing(WindowEvent e)
    {
      closeAction.actionPerformed(null);
    }

    public void windowDeactivated(WindowEvent e)
    {
    }

    public void windowDeiconified(WindowEvent e)
    {
    }

    public void windowIconified(WindowEvent e)
    {
    }

    public void windowOpened(WindowEvent e)
    {
    }
  }

  /**
   * Zerst�rt den Dialog. Nach Aufruf dieser Funktion d�rfen keine weiteren
   * Aufrufe von Methoden des Dialogs erfolgen. Die Verarbeitung erfolgt
   * asynchron. Wurde dem Konstruktor ein entsprechender ActionListener
   * �bergeben, so wird seine actionPerformed() Funktion aufgerufen.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  public void dispose()
  {
    // GUI im Event-Dispatching Thread zerst�ren wg. Thread-Safety.
    try
    {
      javax.swing.SwingUtilities.invokeLater(new Runnable()
      {
        public void run()
        {
          abort();
        }
      });
    }
    catch (Exception x)
    {/* Hope for the best */
    }
  }

  // /**
  // * Sorgt f�r das dauernde Neustarten des Dialogs.
  // * @author Matthias Benkmann (D-III-ITD 5.1)
  // */
  // private static class RunTest implements ActionListener
  // {
  // private DatasourceJoiner dj;
  // private ConfigThingy conf;
  // private ConfigThingy verConf;
  // private ConfigThingy abConf;
  //    
  // public RunTest(ConfigThingy conf, ConfigThingy verConf, ConfigThingy
  // abConf, DatasourceJoiner dj)
  // {
  // this.dj = dj;
  // this.conf = conf;
  // this.abConf = abConf;
  // this.verConf = verConf;
  // }
  //    
  // public void actionPerformed(ActionEvent e)
  // {
  // try{
  // try{
  // if (e.getActionCommand().equals("abort")) System.exit(0);
  // }catch(Exception x){}
  // new SachleitendeVerfuegungenDruckdialog(conf, this);
  // } catch(ConfigurationErrorException x)
  // {
  // Logger.error(x);
  // }
  // }
  // }
  //  
  // public static void main(String[] args) throws Exception
  // {
  // LookAndFeelInfo[] lf = UIManager.getInstalledLookAndFeels();
  // for (int i = 0; i < lf.length; ++i)
  // System.out.println(lf[i].getClassName());
  // System.out.println("Default L&F:
  // "+UIManager.getSystemLookAndFeelClassName());
  // String confFile = "testdata/WhoAmI.conf";
  // String verConfFile = "testdata/PAL.conf";
  // String abConfFile = "testdata/AbsenderdatenBearbeiten.conf";
  // ConfigThingy conf = new ConfigThingy("",new URL(new
  // File(System.getProperty("user.dir")).toURL(),confFile));
  // ConfigThingy verConf = new ConfigThingy("",new URL(new
  // File(System.getProperty("user.dir")).toURL(),verConfFile));
  // ConfigThingy abConf = new ConfigThingy("",new URL(new
  // File(System.getProperty("user.dir")).toURL(),abConfFile));
  // TestDatasourceJoiner dj = new TestDatasourceJoiner();
  // RunTest test = new RunTest(conf.get("AbsenderAuswaehlen"),
  // verConf.get("PersoenlicheAbsenderliste"),
  // abConf.get("AbsenderdatenBearbeiten"), dj);
  // test.actionPerformed(null);
  // Thread.sleep(600000);
  // System.exit(0);
  // }

}