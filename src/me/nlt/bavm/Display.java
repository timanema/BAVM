package me.nlt.bavm;

import javax.swing.*;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.DefaultCaret;
import java.awt.*;
import java.awt.event.*;

public class Display
{
    private int width, height;
    public JTextArea textArea;
    public JTextField jTextField;

    private Object lockObject;

    /**
     * Display constructor
     *
     * @param width  Width
     * @param height Height
     */
    public Display(int width, int height, Object lockObject)
    {
        this.width = width;
        this.height = height;
        this.lockObject = lockObject;

        this.initDisplay();
    }

    /**
     * Hiermee wordt de display 'opgestart'
     */
    private void initDisplay()
    {
        JFrame jFrame = new JFrame();
        JPanel jPanel = new JPanel();

        this.textArea = new JTextArea(width, height);
        this.jTextField = new JTextField();

        JScrollPane jScrollPane = new JScrollPane(textArea);
        DefaultCaret defaultCaret = (DefaultCaret) this.textArea.getCaret();

        // JPanel, JTextArea en JScrollPane 'configureren'
        jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.Y_AXIS));
        jScrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        defaultCaret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);

        // JLabel en textArea 'configureren'
        this.textArea.setEditable(false);
        this.jTextField.setEnabled(false);
        this.jTextField.setFont(new Font("Verdana", Font.ITALIC, 12));
        this.jTextField.setText("Input box");
        this.jTextField.setPreferredSize(new Dimension(500, 30));

        // Focuslistener voor jTextField voor de input hint
        jTextField.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusLost(FocusEvent e)
            {
                jTextField.setText("Input box");
            }

            @Override
            public void focusGained(FocusEvent e)
            {
                jTextField.setText("");
            }
        });

        // Borders zetten
        jPanel.setBorder(new TitledBorder(new EtchedBorder(), "Actie venster"));

        // Componenten toevoegen
        jPanel.add(jScrollPane);
        jPanel.add(jTextField);

        // Hier zorgen we dat onClose() wordt aangeroepen als het programma stopt
        jFrame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        jFrame.addWindowListener(new WindowAdapter()
        {
            @Override
            public void windowClosing(WindowEvent e)
            {
                // Op deze manier kunnen we data opslaan voordat het programma stopt
                onClose();
            }
        });

        // Wat standaard dingen opzetten
        jFrame.setTitle("BAVM");
        jFrame.add(jPanel);
        jFrame.pack();
        jFrame.setLocationRelativeTo(null);
        jFrame.setVisible(true);

        // Object unlocken
        this.resumeLoading();
    }

    /**
     * Unlocked het lockObject zodat de main thread verder gaat met laden
     */
    private void resumeLoading()
    {
        // Object unlocken in een synchronized block zodat we de juiste MonitorState krijgen
        synchronized (lockObject)
        {
            lockObject.notify();
        }
    }

    /**
     * Wordt aangeroepen als de gebruiker het scherm wegklikt zodat we dingen kunnen opslaan
     */
    private void onClose()
    {
        System.out.println("Afsluiten ..");
        System.exit(1);
    }

    /**
     * Hiermee kan tekst worden toegevoegd aan de JTextArea en dus worden getoond
     *
     * @param strings De strings die toegevoegd moeten worden
     */
    public void appendText(String... strings)
    {
       this.appendText(true, strings);
    }

    /**
     * Hiermee kan je ook strings toevoegen alleen heb je hier ook nog de keuze of de string op een nieuwe regel moet
     * @param newLine De boolean die aangeeft of het op een nieuwe regel moet of niet
     * @param strings De strings die toegevoegd moeten worden
     */
    public void appendText(boolean newLine, String... strings)
    {
        // Op EDT uitvoeren
        EventQueue.invokeLater(() -> {
            // Door alle strings lopen en die printen in het actie venster
            for (String string : strings)
            {
                textArea.append((newLine ? "\n" : "") + string);
            }
        });
    }

    /**
     * Hiermee kan je de input van de gebruiker lezen zonder eerst een vraag te stellen
     * @return De input van de gebruiker
     */
    public String readLine()
    {
        return this.readLine("");
    }

    /**
     * Hiermee kan je de input van de gebruiker lezen
     * @param questionString De vraag die wordt gesteld aan de gebruiker
     * @return De input van de gebruiker
     */
    public String readLine(String questionString)
    {
        final String[] inputLine = {null};

        // Zet de textField aan zodat mensen dingen kunnen typen
        jTextField.setEnabled(true);

        // Stel de vraag mits die er is
        if (!questionString.trim().equals(""))
        {
            appendText(questionString);
        }

        // Action listener maken
        ActionListener actionListener = e -> {
            // Even checken of de gebruiker wel iets heeft getypt en stuur zo nodig een bericht waarin gevraagd wordt of hij/zij dat wilt doen
            if (!jTextField.getText().trim().equals(""))
            {
                // Zet de tekst in de textField in een array (variable moeten constant zijn om in een lambda te kunnen worden gebruikt)
                inputLine[0] = jTextField.getText();

                // De input van de gebruiker ook even 'printen'
                appendText(false, "   --> " + inputLine[0]);
                // Unlock het lockObject weer zodat de Thread die deze methode aanvroeg weer verder kan met het resultaat
                synchronized (inputLine)
                {
                    inputLine.notify();
                }
            }
            else
            {
                appendText("Je dient wel iets in te typen!");
            }
        };

        // Ga verder op EDT en wacht op het punt dat de gebruiker de 'enter' toets indrukt
        EventQueue.invokeLater(() -> jTextField.addActionListener(actionListener));

        // Loch het lockObject zodat deze moet wachten tot de gebruiker iets heeft ingetypt
        synchronized (inputLine)
        {
            try
            {
                inputLine.wait();
            } catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }

        // Reset de waarden weer
        jTextField.removeActionListener(actionListener);
        jTextField.setText("Input box");
        jTextField.setEnabled(false);

        return inputLine[0];
    }

    /**
     * Hiermee kan je om een nummer vragen van de gebruiker zonder een vraag te stellen
     * @return Het resultaat in de vorm van een double
     */
    public double readDouble()
    {
        return this.readDouble("");
    }

    /**
     * Hiermee kan je om een nummer vragen van de gebruiker
     * Het nummer wordt automatisch geparsed en mocht dat niet kunnen wordt er een bericht gestuurd
     * @param questionString De vraag die er aan de gebruiker wordt gevraagd
     * @return Het resultaat in de vorm van een double
     */
    public double readDouble(String questionString)
    {
        double result = Double.MIN_VALUE;

        // Net zo lang blijven vragen totdat de gebruiker een waarde heeft gegeven waar we iets mee kunnen
        while (result == Double.MIN_VALUE)
        {
            // Even kijken of de input wel een nummer is
            try
            {
                result = Double.parseDouble(readLine(questionString));
            } catch (NumberFormatException e)
            {
                appendText("Er wordt om een nummer gevraagd in het patroon '1.0' of '1'!");
            }
        }

        return result;
    }
}
