/*
 * Projekt  : WollMux
 * Funktion : Implementiert den Hinzufügen/Entfernen Dialog des BKS
 * 
 * Copyright (c) 2010-2015 Landeshauptstadt München
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the European Union Public Licence (EUPL),
 * version 1.0 (or any later version).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * European Union Public Licence for more details.
 *
 * You should have received a copy of the European Union Public Licence
 * along with this program. If not, see
 * http://ec.europa.eu/idabc/en/document/7330
 *
 * Änderungshistorie:
 * Datum      | Wer | Änderungsgrund
 * -------------------------------------------------------------------
 * 17.10.2005 | BNK | Erstellung
 * 18.10.2005 | BNK | PAL Verwalten GUI großteils implementiert (aber funktionslos)
 * 19.10.2005 | BNK | Suche implementiert
 * 20.10.2005 | BNK | Suche getestet
 * 24.10.2005 | BNK | Restliche ACTIONS implementiert
 *                  | Doppelklickbehandlung
 *                  | Sortierung
 *                  | Gegenseitiger Ausschluss der Selektierung
 * 25.10.2005 | BNK | besser kommentiert
 * 27.10.2005 | BNK | back + CLOSEACTION
 * 31.10.2005 | BNK | Behandlung von TimeoutException bei find()
 * 02.11.2005 | BNK | +editNewPALEntry()
 * 10.11.2005 | BNK | +DEFAULT_* Konstanten
 * 14.11.2005 | BNK | Exakter Match "Nachname" entfernt aus 1-Wort-Fall
 * 22.11.2005 | BNK | Common.setLookAndFeel() verwenden
 * 22.11.2005 | BNK | Bei Initialisierung ist der selectedDataset auch in der Liste 
 *                  | selektiert.
 * 20.01.2006 | BNK | Default-Anrede für Tinchen WollMux ist "Frau"
 * 19.10.2006 | BNK | Credits
 * 23.10.2006 | BNK | Bugfix: Bei credits an wurden Personen ohne Mail nicht dargestellt.
 * 06.11.2006 | BNK | auf AlwaysOnTop gesetzt.
 * 26.02.2010 | BED | WollMux-Icon für Frame; Löschen aus PAL-Liste mit ENTF-Taste 
 * 11.03.2010 | BED | Einsatz von FrameWorker für Suche + Meldung bei Timeout
 *                  | Credits-Bild für BED
 * -------------------------------------------------------------------
 *
 * @author Matthias Benkmann (D-III-ITD 5.1)
 * 
 */
package de.muenchen.allg.itd51.wollmux.dialog;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import de.muenchen.allg.itd51.wollmux.WollMuxFiles;
import de.muenchen.allg.itd51.wollmux.core.db.ColumnNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.db.DJDataset;
import de.muenchen.allg.itd51.wollmux.core.db.DJDatasetListElement;
import de.muenchen.allg.itd51.wollmux.core.db.Dataset;
import de.muenchen.allg.itd51.wollmux.core.db.DatasetNotFoundException;
import de.muenchen.allg.itd51.wollmux.core.db.DatasourceJoiner;
import de.muenchen.allg.itd51.wollmux.core.db.QueryResults;
import de.muenchen.allg.itd51.wollmux.core.db.Search;
import de.muenchen.allg.itd51.wollmux.core.db.SearchStrategy;
import de.muenchen.allg.itd51.wollmux.core.db.TimeoutException;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigThingy;
import de.muenchen.allg.itd51.wollmux.core.parser.ConfigurationErrorException;
import de.muenchen.allg.itd51.wollmux.core.util.L;
import de.muenchen.allg.itd51.wollmux.core.util.Logger;

/**
 * Diese Klasse baut anhand einer als ConfigThingy übergebenen Dialogbeschreibung
 * einen Dialog zum Hinzufügen/Entfernen von Einträgen der Persönlichen Absenderliste
 * auf. Die private-Funktionen dagegen NUR aus dem Event-Dispatching Thread heraus
 * aufgerufen werden.
 * 
 * @author Matthias Benkmann (D-III-ITD 5.1)
 */
public class PersoenlicheAbsenderlisteVerwalten
{
  public static final String DEFAULT_ROLLE = "D-WOLL-MUX-5.1";

  public static final String DEFAULT_NACHNAME = "Wollmux";

  public static final String DEFAULT_VORNAME = "Tinchen";

  public static final String DEFAULT_ANREDE = "Frau";

  /**
   * Gibt an, wie die Personen in den Listen angezeigt werden sollen, wenn es nicht
   * explizit in der Konfiguration über das DISPLAY-Attribut für eine listbox
   * festgelegt ist. %{Spalte}-Syntax um entsprechenden Wert des Datensatzes
   * einzufügen, z.B. "%{Nachname}, %{Vorname}" für die Anzeige "Meier, Hans" etc.
   * 
   * An dieser Stelle einen Default-Wert hardzucodieren (der noch dazu LHM-spezifisch
   * ist!) ist sehr unschön und wurde nur gemacht um abwärtskompatibel zu alten
   * WollMux-Konfigurationen zu bleiben. Sobald sichergestellt ist, dass überall auf
   * eine neue WollMux-Konfiguration geupdatet wurde, sollte man diesen Fallback
   * wieder entfernen.
   */
  private static final String DEFAULT_DISPLAYTEMPLATE =
    "%{Nachname}, %{Vorname} (%{Rolle})";

  private final URL MB_URL =
    this.getClass().getClassLoader().getResource("data/mb.png");

  private final URL CL_URL =
    this.getClass().getClassLoader().getResource("data/cl.png");

  private final URL DB_URL =
    this.getClass().getClassLoader().getResource("data/db.png");

  /**
   * Standardbreite für Textfelder
   */
  private final static int TEXTFIELD_DEFAULT_WIDTH = 22;

  /**
   * ActionListener für Buttons mit der ACTION "abort".
   */
  private ActionListener actionListener_abort = new ActionListener()
  {
    public void actionPerformed(ActionEvent e)
    {
      abort();
    }
  };

  /**
   * ActionListener für Buttons mit der ACTION "search".
   */
  private ActionListener actionListener_search = new ActionListener()
  {
    public void actionPerformed(ActionEvent e)
    {
      search();
    }
  };

  /**
   * ActionListener für Buttons mit der ACTION "addToPAL".
   */
  private ActionListener actionListener_addToPAL = new ActionListener()
  {
    public void actionPerformed(ActionEvent e)
    {
      addToPAL();
    }
  };

  /**
   * ActionListener für Buttons mit der ACTION "removeFromPAL".
   */
  private ActionListener actionListener_removeFromPAL = new ActionListener()
  {
    public void actionPerformed(ActionEvent e)
    {
      removeFromPAL();
    }
  };

  /**
   * ActionListener für Buttons mit der ACTION "editEntry".
   */
  private ActionListener actionListener_editEntry = new ActionListener()
  {
    public void actionPerformed(ActionEvent e)
    {
      editEntry();
    }
  };

  /**
   * ActionListener für Buttons mit der ACTION "copyEntry".
   */
  private ActionListener actionListener_copyEntry = new ActionListener()
  {
    public void actionPerformed(ActionEvent e)
    {
      copyEntry();
    }
  };

  /**
   * ActionListener für Buttons mit der ACTION "editNewPALEntry".
   */
  private ActionListener actionListener_editNewPALEntry = new ActionListener()
  {
    public void actionPerformed(ActionEvent e)
    {
      editNewPALEntry();
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
  private JComponent mainPanel;

  /**
   * Der DatasourceJoiner, den dieser Dialog anspricht.
   */
  private DatasourceJoiner dj;

  /**
   * Speichert Referenzen auf die JButtons, die zu deaktivieren sind, wenn kein
   * Eintrag in einer Liste selektiert ist.
   */
  private List<JButton> buttonsToGreyOutIfNothingSelected = new Vector<JButton>();

  /**
   * Die Listbox mit den Suchresultaten.
   */
  private JList<DJDatasetListElement> resultsJList;

  /**
   * Die Listbox mit der persönlichen Absenderliste.
   */
  private JList<DJDatasetListElement> palJList;

  /**
   * Gibt an, wie die Suchresultate in der {@link #resultsJList} angezeigt werden
   * sollen. Der Wert wird in der Konfiguration dieses Dialogs bei der "listbox" mit
   * ID "suchanfrage" durch Angeben des DISPLAY-Attributs konfiguriert.
   * %{Spalte}-Syntax um entsprechenden Wert des Datensatzes einzufügen, z.B.
   * "%{Nachname}, %{Vorname}" für die Anzeige "Meier, Hans" etc.
   */
  private String resultsDisplayTemplate;

  /**
   * Gibt an, wie die Suchresultate in der {@link #palJList} angezeigt werden sollen.
   * Der Wert wird in der Konfiguration bei der "listbox" mit ID "suchanfrage" durch
   * Angeben des DISPLAY-Attributs konfiguriert. %{Spalte}-Syntax um entsprechenden
   * Wert des Datensatzes einzufügen, z.B. "%{Nachname}, %{Vorname}" für die Anzeige
   * "Meier, Hans" etc.
   */
  private String palDisplayTemplate;

  /**
   * Das Textfeld in dem der Benutzer seine Suchanfrage eintippt.
   */
  private JTextField query;

  /**
   * Die Suchstrategie für Suchanfragen.
   */
  private SearchStrategy searchStrategy;

  /**
   * URL der Konfiguration der Fallback-Suchstrategie, falls kein
   * Suchstrategie-Abschnitt für den PAL-Dialog definiert wurde.
   * 
   * Dieser Fallback wurde eingebaut, um mit alten WollMux-Konfigurationen kompatibel
   * zu bleiben, sollte nach ausreichend Zeit aber wieder entfernt werden!
   */
  private final URL DEFAULT_SUCHSTRATEGIE_URL =
    this.getClass().getClassLoader().getResource("data/PAL_suchstrategie.conf");

  /**
   * Der dem
   * {@link #PersoenlicheAbsenderlisteVerwalten(ConfigThingy, ConfigThingy, DatasourceJoiner, ActionListener)
   * Konstruktor} übergebene dialogEndListener.
   */
  private ActionListener dialogEndListener;

  /**
   * Das ConfigThingy, das diesen Dialog spezifiziert.
   */
  private ConfigThingy myConf;

  /**
   * Das ConfigThingy, das den Dialog Datensatz Bearbeiten für das Bearbeiten eines
   * Datensatzes der PAL spezifiziert.
   */
  private ConfigThingy abConf;

  /**
   * Sorgt dafür, dass jeweils nur in einer der beiden Listboxen ein Eintrag
   * selektiert sein kann und dass die entsprechenden Buttons ausgegraut werden wenn
   * kein Eintrag selektiert ist.
   */
  private MyListSelectionListener myListSelectionListener =
    new MyListSelectionListener();

  /**
   * Erzeugt einen neuen Dialog.
   * 
   * @param conf
   *          das ConfigThingy, das den Dialog beschreibt (der Vater des
   *          "Fenster"-Knotens.
   * @param abConf
   *          das ConfigThingy, das den Absenderdaten Bearbeiten Dialog beschreibt.
   * @param dj
   *          der DatasourceJoiner, der die zu bearbeitende Liste verwaltet.
   * @param dialogEndListener
   *          falls nicht null, wird die
   *          {@link ActionListener#actionPerformed(java.awt.event.ActionEvent)}
   *          Methode aufgerufen (im Event Dispatching Thread), nachdem der Dialog
   *          geschlossen wurde. Das actionCommand des ActionEvents gibt die Aktion
   *          an, die das Speichern des Dialogs veranlasst hat.
   * @throws ConfigurationErrorException
   *           im Falle eines schwerwiegenden Konfigurationsfehlers, der es dem
   *           Dialog unmöglich macht, zu funktionieren (z.B. dass der "Fenster"
   *           Schlüssel fehlt.
   */
  public PersoenlicheAbsenderlisteVerwalten(ConfigThingy conf, ConfigThingy abConf,
      DatasourceJoiner dj, ActionListener dialogEndListener)
      throws ConfigurationErrorException
  {
    this.dj = dj;
    this.myConf = conf;
    this.abConf = abConf;
    this.dialogEndListener = dialogEndListener;
    this.resultsDisplayTemplate = DEFAULT_DISPLAYTEMPLATE;
    this.palDisplayTemplate = DEFAULT_DISPLAYTEMPLATE;

    // Falls in der Konfiguration ein Suchstrategie-Abschnitt existiert, parsen wir
    // diesen um eine SearchStrategy zu erhalten
    if (conf.query("Suchstrategie").count() != 0)
    {
      this.searchStrategy = SearchStrategy.parse(conf);
    }
    else
    {
      Logger.log(L.m("Kein Suchstrategie-Abschnitt für den PersoenlicheAbsenderliste-Dialog "
        + "angegeben! Verwende Default-Suchstrategie."));
      // Es gibt keinen Suchstrategie-Abschnitt in der Konfiguration, also verwenden
      // wir die Default-Suchstrategie
      // Eigentlich sollte der Suchstrategie-Abschnitt aber verpflichtend sein und
      // wir sollten an dieser Stelle einen echten Error loggen bzw. eine
      // Meldung in der GUI ausgeben und evtl. sogar abbrechen. Wir tun
      // dies allerdings nicht, da das Konfigurieren der Suchstrategie erst mit
      // WollMux 6.3.2 eingeführt wurde und wir abwärtskompatibel zu alten
      // WollMux-Konfigurationen bleiben müssen und Benutzer alter
      // Konfigurationen nicht mit Error-Meldungen irritieren wollen.
      // Dies ist allerdings nur eine Übergangslösung. Die obige Meldung
      // sollte nach ausreichend Zeit genauso wie DEFAULT_SUCHSTRATEGIE_URL
      // entfernt werden (bzw. wie oben gesagt überarbeitet).
      try
      {
        this.searchStrategy =
          SearchStrategy.parse(new ConfigThingy("Default", DEFAULT_SUCHSTRATEGIE_URL));
      }
      catch (Exception e)
      {
        Logger.error(e);
      }
    }

    ConfigThingy fensterDesc1 = conf.query("Fenster");
    if (fensterDesc1.count() == 0)
      throw new ConfigurationErrorException(L.m("Schlüssel 'Fenster' fehlt in %1",
        conf.getName()));

    final ConfigThingy fensterDesc = fensterDesc1.query("Verwalten");
    if (fensterDesc.count() == 0)
      throw new ConfigurationErrorException(L.m("Schlüssel 'Verwalten' fehlt in ",
        conf.getName()));

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
            Logger.error(x);
          }
          ;
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
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void createGUI(ConfigThingy fensterDesc)
  {
    Common.setLookAndFeelOnce();

    resultsJList = new JList<DJDatasetListElement>(new DefaultListModel<DJDatasetListElement>());
    ListCellRenderer<Object> myRenderer = new MyListCellRenderer();
    resultsJList.setCellRenderer(myRenderer);
    palJList = new JList<DJDatasetListElement>(new DefaultListModel<DJDatasetListElement>());

    // KeyListener hinzufügen, damit Einträge in der PAL-Liste durch Drücken der
    // ENTF-Taste gelöscht werden können
    palJList.addKeyListener(new KeyAdapter()
    {
      public void keyPressed(KeyEvent e)
      {
        super.keyPressed(e);
        if (e.getKeyCode() == KeyEvent.VK_DELETE)
        {
          removeFromPAL();
        }
      }
    });

    palJList.setCellRenderer(myRenderer);
    query = new JTextField(TEXTFIELD_DEFAULT_WIDTH);

    String title = L.m("Absenderliste Verwalten (WollMux)");
    closeAction = actionListener_abort;

    // Create and set up the window.
    myFrame = new JFrame(title);
    // leave handling of close request to WindowListener.windowClosing
    myFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    myFrame.addWindowListener(new MyWindowListener());
    myFrame.setAlwaysOnTop(true);
    // WollMux-Icon für PAL-Frame
    Common.setWollMuxIcon(myFrame);

    mainPanel = Box.createVerticalBox();
    mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    mainPanel.setPreferredSize(new Dimension(831, 357));
    myFrame.getContentPane().add(mainPanel);

    addUIElements();

    Dataset dsToSelect = null;
    try
    {
      dsToSelect = dj.getSelectedDataset();
    }
    catch (DatasetNotFoundException x)
    {}
    setListElements(palJList, dj.getLOS(), palDisplayTemplate, false, dsToSelect);

    updateButtonStates();

    myFrame.pack();

    /**
     * Beschränkung der Höhe erst nach pack() aufheben, damit Fenster nicht unnötig
     * groß wird.
     */
    palJList.setFixedCellHeight(-1);
    resultsJList.setFixedCellHeight(-1);

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

  
  private void addUIElements() {
    Box panelIntro = new Box(BoxLayout.X_AXIS);

    JLabel label = new JLabel(L.m("Sie können nach Vorname, Nachname, Email und Orga-Einheit suchen"));
    panelIntro.add(label);

    Box glue = Box.createHorizontalBox();
    glue.add(Box.createHorizontalGlue());
    panelIntro.add(glue);

    mainPanel.add(panelIntro);
    mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));

    Box panelSuche = Box.createHorizontalBox();
    panelSuche.setMaximumSize(new Dimension(900, 28));
    
    JTextField textField = new JTextField(22);
    panelSuche.add(textField);
    
    panelSuche.add(Box.createRigidArea(new Dimension(5, 0)));

    JButton button = new JButton(L.m("Suchen"));
    button.setMnemonic('S');
    button.addActionListener(actionListener_search);
    panelSuche.add(button);

    mainPanel.add(panelSuche);
    mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));
    
    Box panelSelection = Box.createHorizontalBox();
    mainPanel.add(panelSelection);
    mainPanel.add(Box.createRigidArea(new Dimension(0, 5)));

    Box panelSuchergebnis = new Box(BoxLayout.Y_AXIS);

    Box label1Box = Box.createHorizontalBox();
    JLabel label1 = new JLabel(L.m("Suchergebnis"));
    label1Box.add(label1);
    label1Box.add(Box.createHorizontalGlue());
    panelSuchergebnis.add(label1Box);

    JList<DJDatasetListElement> list = new JList<DJDatasetListElement>();
    list.setModel(new DefaultListModel<DJDatasetListElement>());
    list.setVisibleRowCount(10);
    list.addMouseListener(new MyActionMouseListener(list, actionListener_addToPAL));
    list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    list.setLayoutOrientation(JList.VERTICAL);
    list.setFixedCellWidth((int) new JLabel(
      "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX").getPreferredSize().getWidth());
    list.addListSelectionListener(myListSelectionListener);

    resultsJList = list;

    JScrollPane scrollPane = new JScrollPane(list);
    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

    panelSuchergebnis.add(scrollPane);

    panelSelection.add(panelSuchergebnis);
    panelSelection.add(Box.createRigidArea(new Dimension(5, 0)));

    Box panelHinUndHer = new Box(BoxLayout.Y_AXIS);

    JButton button1 = new JButton(L.m("→"));
    button1.addActionListener(actionListener_addToPAL);
    buttonsToGreyOutIfNothingSelected.add(button1);

    panelHinUndHer.add(button1);
    panelHinUndHer.add(Box.createRigidArea(new Dimension(0, 5)));

    JButton button2 = new JButton(L.m("←"));
    button2.addActionListener(actionListener_removeFromPAL);
    buttonsToGreyOutIfNothingSelected.add(button2);
    panelHinUndHer.add(button2);

    panelSelection.add(panelHinUndHer);
    panelSelection.add(Box.createRigidArea(new Dimension(5, 0)));

    Box panelAbsenderliste = new Box(BoxLayout.Y_AXIS);

    JLabel label2 = new JLabel(L.m("Persönliche Absenderliste"));
    
    Box label2Box = Box.createHorizontalBox();
    label2Box.add(label2);
    label2Box.add(Box.createHorizontalGlue());
    panelAbsenderliste.add(label2Box);


    JList<DJDatasetListElement> list1 = new JList<DJDatasetListElement>();
    list1.setModel(new DefaultListModel<DJDatasetListElement>());
    list1.setVisibleRowCount(10);
    list1.addMouseListener(new MyActionMouseListener(list, actionListener_editEntry));
    list1.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    list1.setLayoutOrientation(JList.VERTICAL);
    list1.setFixedCellWidth((int) new JLabel(
      "XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX").getPreferredSize().getWidth());
    list1.addListSelectionListener(myListSelectionListener);
    palJList = list1;
    
    JScrollPane scrollPane1 = new JScrollPane(list1);
    scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);

    panelAbsenderliste.add(scrollPane1);

    panelSelection.add(panelAbsenderliste);

    Box panelFussbereich = Box.createHorizontalBox();

    JButton button3 = new JButton(L.m("Löschen"));
    button3.setMnemonic('L');
    button3.addActionListener(actionListener_removeFromPAL);
    buttonsToGreyOutIfNothingSelected.add(button3);
    panelFussbereich.add(button3);

    JButton button4 = new JButton(L.m("Bearbeiten..."));
    button4.setMnemonic('B');
    button4.addActionListener(actionListener_editEntry);
    buttonsToGreyOutIfNothingSelected.add(button4);
    panelFussbereich.add(button4);

    JButton button5 = new JButton(L.m("Kopieren"));
    button5.setMnemonic('K');
    button5.addActionListener(actionListener_copyEntry);
    buttonsToGreyOutIfNothingSelected.add(button5);
    panelFussbereich.add(button5);

    JButton button6 = new JButton(L.m("Neu"));
    button6.setMnemonic('N');
    button6.addActionListener(actionListener_editNewPALEntry);
    panelFussbereich.add(button6);

    JButton button7 = new JButton(L.m("Schließen"));
    button7.setMnemonic('C');
    button7.addActionListener(actionListener_abort);
    panelFussbereich.add(button7);

    mainPanel.add(panelFussbereich);
  }

  /**
   * Wartet auf Doppelklick und führt dann die actionPerformed() Methode eines
   * ActionListeners aus.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private static class MyActionMouseListener extends MouseAdapter
  {
    private JList<DJDatasetListElement> list;

    private ActionListener action;

    public MyActionMouseListener(JList<DJDatasetListElement> list, ActionListener action)
    {
      this.list = list;
      this.action = action;
    }

    public void mouseClicked(MouseEvent e)
    {
      if (e.getClickCount() == 2)
      {
        Point location = e.getPoint();
        int index = list.locationToIndex(location);
        if (index < 0) return;
        Rectangle bounds = list.getCellBounds(index, index);
        if (!bounds.contains(location)) return;
        action.actionPerformed(null);
      }
    }
  }

  /**
   * Nimmt eine JList list, die ein DefaultListModel haben muss und ändert ihre
   * Wertliste so, dass sie data entspricht. Die Datasets aus data werden nicht
   * direkt als Werte verwendet, sondern in {@link DJDatasetListElement}-Objekte
   * gewrappt, deren Inhalt entsprechend des übergebenen displayTemplates angezeigt
   * wird. data == null wird interpretiert als leere Liste. Wenn datasetToSelect !=
   * null ist, so wird der entsprechende Datensatz in der Liste selektiert, wenn er
   * darin vorhanden ist.
   * 
   * @param list
   *          die Liste deren Wertliste geändert werden soll
   * @param data
   *          enthält die Datensätze, mit denen die Liste gefüllt werden soll
   * @param displayTemplate
   *          gibt an wie die Datensätze in der Liste als Strings repräsentiert
   *          werden sollen, siehe z.B. {@link #DEFAULT_DISPLAYTEMPLATE}.
   * @param append
   *          Falls true, werden die Elemente an die Liste angehängt anstatt sie zu
   *          ersetzen.
   * @param datasetToSelect
   *          gibt den Datensatz an, der selektiert werden soll. Wenn
   *          <code>null</code> übergeben wird, wird entsprechend kein Datensatz
   *          ausgewählt.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void setListElements(JList<DJDatasetListElement> list, QueryResults data,
      String displayTemplate, boolean append, Dataset datasetToSelect)
  {
    int selectedIndex = -1;
    DJDatasetListElement[] elements;
    if (data == null)
      elements = new DJDatasetListElement[] {};
    else
    {
      elements = new DJDatasetListElement[data.size()];
      Iterator<Dataset> iter = data.iterator();
      int i = 0;
      while (iter.hasNext())
      {
        DJDataset ds = (DJDataset) iter.next();
        Icon icon = null;
        String mail = null;
        try
        {
          mail = ds.get("Mail"); // liefert null, wenn nicht belegt.
        }
        catch (ColumnNotFoundException x)
        {}

        if (mail == null) mail = "";

        if (WollMuxFiles.showCredits())
        {
          if (mail.equals("matthias.benkmann@muenchen.de"))
            icon = new ImageIcon(MB_URL);
          else if (mail.equals("christoph.lutz@muenchen.de"))
            icon = new ImageIcon(CL_URL);
          else if (mail.equals("daniel.benkmann@muenchen.de"))
            icon = new ImageIcon(DB_URL);
        }

        elements[i++] = new DJDatasetListElement(ds, displayTemplate, icon);
      }
      Arrays.sort(elements);
    }

    DefaultListModel<DJDatasetListElement> listModel = (DefaultListModel<DJDatasetListElement>) list.getModel();
    if (!append) listModel.clear();
    int oldSize = listModel.size();
    for (int i = 0; i < elements.length; ++i)
    {
      listModel.addElement(elements[i]);
      if (datasetToSelect != null
        && elements[i].getDataset().getKey().equals(datasetToSelect.getKey()))
        selectedIndex = i;
    }

    if (selectedIndex >= 0) list.setSelectedIndex(selectedIndex + oldSize);
  }

  /**
   * Wie {@link #setListElements(JList, QueryResults, String, boolean, Dataset)},
   * aber es wird kein Datensatz selektiert.
   * 
   * @param list
   *          die Liste deren Wertliste geändert werden soll
   * @param data
   *          enthält die Datensätze, mit denen die Liste gefüllt werden soll
   * @param displayTemplate
   *          gibt an wie die Datensätze in der Liste als Strings repräsentiert
   *          werden sollen, siehe z.B. {@link #DEFAULT_DISPLAYTEMPLATE}.
   * @param append
   *          Falls true, werden die Elemente an die Liste angehängt anstatt sie zu
   *          ersetzen.
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void setListElements(JList<DJDatasetListElement> list, QueryResults data,
      String displayTemplate, boolean append)
  {
    setListElements(list, data, displayTemplate, append, null);
  }

  /**
   * Aktiviert oder Deaktiviert die {@link #buttonsToGreyOutIfNothingSelected} gemäss
   * der Selektion oder nicht Selektion von Werten in den Listboxen.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void updateButtonStates()
  {
    boolean enabled = false;
    try
    {
      enabled =
        (resultsJList.getSelectedIndex() >= 0) || (palJList.getSelectedIndex() >= 0);
    }
    catch (NullPointerException x)
    {
      Logger.error(L.m("Listbox mit ID \"suchergebnisse\" oder \"pal\" fehlt"));
    }

    Iterator<JButton> iter = buttonsToGreyOutIfNothingSelected.iterator();
    while (iter.hasNext())
      iter.next().setEnabled(enabled);
  }

  private static class MyListCellRenderer extends DefaultListCellRenderer
  {
    private static final long serialVersionUID = -540148680826568290L;

    public Component getListCellRendererComponent(
        @SuppressWarnings("rawtypes") JList list, Object value, int index,
        boolean isSelected, boolean cellHasFocus)
    {
      try
      {
        DJDatasetListElement ele = (DJDatasetListElement) value;

        Icon icon = ele.getIcon();
        if (icon != null)
          value = icon;
        else
          value = ele.toString();
      }
      catch (ClassCastException x)
      {}

      return super.getListCellRendererComponent(list, value, index, isSelected,
        cellHasFocus);
    }
  }

  /**
   * Sorgt dafür, dass jeweils nur in einer der beiden Listboxen ein Eintrag
   * selektiert sein kann und dass die entsprechenden Buttons ausgegraut werden wenn
   * kein Eintrag selektiert ist.
   */
  private class MyListSelectionListener implements ListSelectionListener
  {
    public void valueChanged(ListSelectionEvent e)
    {
      @SuppressWarnings("unchecked")
      JList<DJDatasetListElement> list = (JList<DJDatasetListElement>) e.getSource();
      if (list != palJList && list != resultsJList) return;

      /*
       * Dafür sorgen, dass nie in beiden Listen ein Element selektiert ist.
       */
      JList<DJDatasetListElement> otherlist = (list == palJList) ? resultsJList : palJList;
      if (list.getSelectedIndex() >= 0) otherlist.clearSelection();

      /*
       * Buttons ausgrauen, falls nichts selektiert, einschwarzen sonst.
       */
      updateButtonStates();
    }
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
   * Beendet den Dialog und ruft falls nötig den dialogEndListener auf wobei das
   * gegebene actionCommand übergeben wird.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void dialogEnd(String actionCommand)
  {
    myFrame.dispose();
    if (dialogEndListener != null)
      dialogEndListener.actionPerformed(new ActionEvent(this, 0, actionCommand));
  }

  /**
   * Implementiert die gleichnamige ACTION.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void addToPAL()
  {
    List<DJDatasetListElement> sel = resultsJList.getSelectedValuesList();
    addEntries: for (DJDatasetListElement e : sel)
    {
      DJDataset ds = e.getDataset();
      String eStr = e.toString();
      ListModel<DJDatasetListElement> model = palJList.getModel();
      for (int j = model.getSize() - 1; j >= 0; --j)
      {
        DJDatasetListElement e2 = model.getElementAt(j);
        if (e2.toString().equals(eStr)) continue addEntries;
      }
      ds.copy();
    }

    listsHaveChanged();
  }

  /**
   * Aktualisiert die Werte in der PAL Listbox, löscht die Selektionen in beiden
   * Listboxen und passt den Ausgegraut-Status der Buttons entsprechend an.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void listsHaveChanged()
  {
    setListElements(palJList, dj.getLOS(), palDisplayTemplate, false);
    palJList.clearSelection();
    resultsJList.clearSelection();
    updateButtonStates();
  }

  /**
   * Implementiert die gleichnamige ACTION.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void removeFromPAL()
  {
    List<DJDatasetListElement> sel = palJList.getSelectedValuesList();
    for (DJDatasetListElement e : sel)
    {
      e.getDataset().remove();
    }

    listsHaveChanged();
  }

  /**
   * Implementiert die gleichnamige ACTION.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void editEntry()
  {
    DJDatasetListElement e = (DJDatasetListElement) palJList.getSelectedValue();
    DJDataset ds;
    if (e == null)
    {
      e = (DJDatasetListElement) resultsJList.getSelectedValue();
      if (e == null) return;
      ds = e.getDataset().copy();
    }
    else
      ds = e.getDataset();

    editDataset(ds);
  }

  private void editDataset(DJDataset ds)
  {
    ActionListener del =
      new MyDialogEndListener(this, myConf, abConf, dj, dialogEndListener, null);
    dialogEndListener = null;
    abort();
    try
    {
      new DatensatzBearbeiten(abConf, ds, del);
    }
    catch (ConfigurationErrorException x)
    {
      Logger.error(x);
    }
  }

  /**
   * Erzeugt eine Kopue von orig und ändert ihre Rolle auf "Kopie".
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void copyDJDataset(DJDataset orig)
  {
    DJDataset newDS = orig.copy();
    try
    {
      newDS.set("Rolle", L.m("Kopie"));
    }
    catch (Exception e)
    {
      Logger.error(e);
    }
  }

  /**
   * Implementiert die gleichnamige ACTION.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void copyEntry()
  {
    List<DJDatasetListElement> sel = resultsJList.getSelectedValuesList();
    for (DJDatasetListElement e : sel)
    {
      copyDJDataset(e.getDataset());
    }

    sel = palJList.getSelectedValuesList();
    for (DJDatasetListElement e : sel)
    {
      copyDJDataset(e.getDataset());
    }

    listsHaveChanged();
  }

  /**
   * Implementiert die gleichnamige ACTION. Liefert den neuen Datensatz zurück.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private DJDataset newPALEntry()
  {
    DJDataset ds = dj.newDataset();
    try
    {
      ds.set("Vorname", DEFAULT_VORNAME);
      ds.set("Nachname", DEFAULT_NACHNAME);
      ds.set("Rolle", DEFAULT_ROLLE);
      ds.set("Anrede", DEFAULT_ANREDE);
    }
    catch (Exception x)
    {
      Logger.error(x);
    }
    listsHaveChanged();
    return ds;
  }

  /**
   * Implementiert die gleichnamige ACTION.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void editNewPALEntry()
  {
    editDataset(newPALEntry());
  }

  /**
   * Implementiert die gleichnamige ACTION. Hier stecken die ganzen komplexen
   * Heuristiken drinnen zur Auswertung der Suchanfrage.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private void search()
  {
    // Erzeugen eines Runnable-Objekts, das die Geschäftslogik enthält und nachher an
    // FrameWorker.disableFrameAndWork übergeben werden kann.
    Runnable r = new Runnable()
    {
      public void run()
      {
        String queryString = query.getText();
        if (queryString.equalsIgnoreCase("credits"))
        {
          credits();
          return;
        }

        QueryResults results = null;
        try
        {
          results = Search.search(query.getText(), searchStrategy, dj, true);
        }
        catch (TimeoutException x)
        {
          JOptionPane.showMessageDialog(
            myFrame,
            L.m("Das Bearbeiten Ihrer Suchanfrage hat zu lange gedauert und wurde deshalb abgebrochen.\n"
              + "Grund hierfür könnte ein Problem mit der Datenquelle sein oder mit dem verwendeten\n"
              + "Suchbegriff, der auf zu viele Ergebnisse zutrifft.\n"
              + "Bitte versuchen Sie eine andere, präzisere Suchanfrage."),
            L.m("Timeout bei Suchanfrage"), JOptionPane.WARNING_MESSAGE);
          Logger.error(x);
        }
        catch (IllegalArgumentException x)
        { // wird bei illegalen Suchanfragen geworfen
          Logger.error(x);
        }

        // Wir benötigen finalResults, da eine nicht-finale Variable nicht in der
        // unten definierten anonymen Runnable-Klasse referenziert werden darf.
        final QueryResults finalResults = results;

        // Folgendes muss im Event Dispatch Thread ausgeführt werden
        SwingUtilities.invokeLater(new Runnable()
        {
          public void run()
          {
            // kann mit finalResults == null umgehen
            setListElements(resultsJList, finalResults, resultsDisplayTemplate,
              false);
            updateButtonStates();
          }
        });
      }
    }; // Ende des Erzeugens des Runnable-Objekts r

    // Frame disablen und Suche in eigenem Thread starten
    FrameWorker.disableFrameAndWork(myFrame, r, true);
  }

  private void credits()
  {
    WollMuxFiles.showCredits(true);
    QueryResults results = null;
    try
    {
      results = dj.find("Mail", "matthias.benkmann@muenchen.de");
      setListElements(resultsJList, results, resultsDisplayTemplate, false);
      results = dj.find("Mail", "christoph.lutz@muenchen.de");
      setListElements(resultsJList, results, resultsDisplayTemplate, true);
      results = dj.find("Mail", "daniel.benkmann@muenchen.de");
      setListElements(resultsJList, results, resultsDisplayTemplate, true);
    }
    catch (TimeoutException x)
    {
      Logger.error(x);
    }

    updateButtonStates();
  }

  private static class MyDialogEndListener implements ActionListener
  {
    private ConfigThingy conf;

    private ConfigThingy abConf;

    private DatasourceJoiner dj;

    private ActionListener dialogEndListener;

    private String actionCommand;

    private PersoenlicheAbsenderlisteVerwalten mySource;

    /**
     * Falls actionPerformed() mit getActionCommand().equals("back") aufgerufen wird,
     * wird ein neuer AbsenderAuswaehlen Dialog mit den übergebenen Parametern
     * erzeugt. Ansonsten wird der dialogEndListener mit actionCommand aufgerufen.
     * Falls actionCommand null ist wird das actioncommand und die source des
     * ActionEvents weitergereicht, der actionPerformed() übergeben wird, ansonsten
     * werden die übergebenen Werte für actionCommand und source verwendet.
     */
    public MyDialogEndListener(PersoenlicheAbsenderlisteVerwalten source,
        ConfigThingy conf, ConfigThingy abConf, DatasourceJoiner dj,
        ActionListener dialogEndListener, String actionCommand)
    {
      this.conf = conf;
      this.abConf = abConf;
      this.dj = dj;
      this.dialogEndListener = dialogEndListener;
      this.actionCommand = actionCommand;
      this.mySource = source;
    }

    public void actionPerformed(ActionEvent e)
    {
      if (e.getActionCommand().equals("back"))
        try
        {
          new PersoenlicheAbsenderlisteVerwalten(conf, abConf, dj, dialogEndListener);
        }
        catch (Exception x)
        {
          Logger.error(x);
        }
      else
      {
        Object source = mySource;
        if (actionCommand == null)
        {
          actionCommand = e.getActionCommand();
          source = e.getSource();
        }
        if (dialogEndListener != null)
          dialogEndListener.actionPerformed(new ActionEvent(source, 0, actionCommand));
      }
    }
  }

  /**
   * Ein WindowListener, der auf den JFrame registriert wird, damit als Reaktion auf
   * den Schliessen-Knopf auch die ACTION "abort" ausgeführt wird.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  private class MyWindowListener extends WindowAdapter
  {
    public void windowClosing(WindowEvent e)
    {
      closeAction.actionPerformed(null);
    }
  }

  /**
   * Zerstört den Dialog. Nach Aufruf dieser Funktion dürfen keine weiteren Aufrufe
   * von Methoden des Dialogs erfolgen. Die Verarbeitung erfolgt asynchron. Wurde dem
   * Konstruktor ein entsprechender ActionListener übergeben, so wird seine
   * actionPerformed() Funktion aufgerufen.
   * 
   * @author Matthias Benkmann (D-III-ITD 5.1)
   */
  @Override
  protected void finalize() throws Throwable
  {
    // GUI im Event-Dispatching Thread zerstören wg. Thread-Safety.
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
    {/* Hope for the best */}
  }
}
