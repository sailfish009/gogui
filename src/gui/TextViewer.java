//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import utils.GuiUtils;

//----------------------------------------------------------------------------

public class TextViewer
    extends JDialog
{
    static public interface Listener
    {
        /** Callback if some text is selected.
            If text is unselected again this function will be called
            with the complete text content of the window.
        */
        public void textSelected(String text);
    }

    public TextViewer(Frame owner, String title, String text,
                      boolean highlight, Listener listener)
    {
        super(owner, title);
        //setLocationRelativeTo(owner);
        m_listener = listener;
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(GuiUtils.createSmallEmptyBorder());
        Container contentPane = getContentPane();
        contentPane.add(panel, BorderLayout.CENTER);
        JLabel label = new JLabel(title);
        panel.add(label, BorderLayout.NORTH);
        m_textPane = new JTextPane();
        StyledDocument doc = m_textPane.getStyledDocument();
        try
        {
            doc.insertString(0, text, null);
        }
        catch (BadLocationException e)
        {
            assert(false);
        }
        int fontSize = GuiUtils.getDefaultMonoFontSize();
        m_textPane.setFont(new Font("Monospaced", Font.PLAIN, fontSize));
        JScrollPane scrollPane = new JScrollPane(m_textPane);
        panel.add(scrollPane, BorderLayout.CENTER);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        KeyListener keyListener = new KeyAdapter()
            {
                public void keyReleased(KeyEvent e) 
                {
                    int c = e.getKeyCode();        
                    if (c == KeyEvent.VK_ESCAPE)
                        dispose();
                }
            };
        m_textPane.addKeyListener(keyListener);
        CaretListener caretListener = new CaretListener()
            {
                public void caretUpdate(CaretEvent event)
                {
                    if (m_listener == null)
                        return;
                    int start = m_textPane.getSelectionStart();
                    int end = m_textPane.getSelectionEnd();
                    StyledDocument doc = m_textPane.getStyledDocument();
                    try
                    {
                        if (start == end)
                        {
                            String text = doc.getText(0, doc.getLength());
                            m_listener.textSelected(text);
                            return;
                        }
                        String text = doc.getText(start, end - start);
                        m_listener.textSelected(text);
                    }
                    catch (BadLocationException e)
                    {
                        assert(false);
                    }   
                }
            };
        m_textPane.addCaretListener(caretListener);
        if (highlight)
            doSyntaxHighlight();
        m_textPane.setCaretPosition(0);
        m_textPane.setEditable(false);
        pack();
        // Workaround for problems with oversized windows on some platforms
        Dimension size = getSize();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int maxHeight = (int)(0.9 * screenSize.height);
        int maxWidth = (int)(0.9 * screenSize.width);
        if (size.height > maxHeight || size.width > maxWidth)
        {
            size.width = Math.min(size.width, maxWidth);
            size.height = Math.min(size.height, maxHeight);
            setSize(size);
        }
    }

    private JTextPane m_textPane;

    private Listener m_listener;

    private void doSyntaxHighlight()
    {
        StyledDocument doc = m_textPane.getStyledDocument();
        StyleContext context = StyleContext.getDefaultStyleContext();
        Style def = context.getStyle(StyleContext.DEFAULT_STYLE);
        Style styleTitle = doc.addStyle("title", def);
        StyleConstants.setBold(styleTitle, true);
        Style stylePoint = doc.addStyle("point", def);
        Color colorPoint = new Color(0.25f, 0.5f, 0.7f);
        StyleConstants.setForeground(stylePoint, colorPoint);
        Style styleNumber = doc.addStyle("number", def);
        Color colorNumber = new Color(0f, 0.54f, 0f);
        StyleConstants.setForeground(styleNumber, colorNumber);
        Style styleConst = doc.addStyle("const", def);
        Color colorConst = new Color(0.8f, 0f, 0f);
        StyleConstants.setForeground(styleConst, colorConst);
        Style styleColor = doc.addStyle("color", def);
        Color colorColor = new Color(0.54f, 0f, 0.54f);
        StyleConstants.setForeground(styleColor, colorColor);
        m_textPane.setEditable(true);
        highlight("number", "\\b-?\\d+\\.?\\d*([Ee][+-]\\d+)?\\b");
        highlight("const", "\\b[A-Z_][A-Z_]+[A-Z]\\b");
        highlight("color",
                  "\\b([Bb][Ll][Aa][Cc][Kk]|[Ww][Hh][Ii][Tt][Ee])\\b");
        highlight("point", "\\b([Pp][Aa][Ss][Ss]|[A-Ta-t](1\\d|[1-9]))\\b");
        highlight("title", "^\\S+:(\\s|$)");
        m_textPane.setEditable(false);
    }

    private void highlight(String styleName, String regex)
    {
        StyledDocument doc = m_textPane.getStyledDocument();
        Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
        try
        {
            CharSequence text = doc.getText(0, doc.getLength());
            Matcher matcher = pattern.matcher(text);
            while (matcher.find())
            {
                int start = matcher.start();
                int end = matcher.end();
                Style style = doc.getStyle(styleName);
                doc.setCharacterAttributes(start, end - start, style, true);
            }
        }
        catch (BadLocationException e)
        {
            assert(false);
        }
    }
}

//----------------------------------------------------------------------------
