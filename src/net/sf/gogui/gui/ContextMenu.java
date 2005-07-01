//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package net.sf.gogui.gui;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.go.GoPoint;

//----------------------------------------------------------------------------

/** Context menu for fields on board. */
public class ContextMenu
    extends JPopupMenu
{
    public interface Listener
    {
        void editLabel(GoPoint point);

        void markSquare(GoPoint point, boolean markSquare);

        void setAnalyzeCommand(AnalyzeCommand command);
    }

    public ContextMenu(GoPoint point, boolean noProgram,
                       Vector supportedCommands, boolean markSquare,
                       Listener listener)
    {
        m_point = point;
        m_listener = listener;
        Vector commands = new Vector();
        Vector labels = new Vector();
        if (! noProgram)
        {
            try
            {
                AnalyzeCommand.read(commands, labels, supportedCommands);
            }
            catch (Exception e)
            {
            }
        }
        m_actionListener = new ActionListener()
            {
                public void actionPerformed(ActionEvent event)
                {
                    Listener listener = ContextMenu.this.m_listener;
                    if (listener == null)
                        return;
                    String actionCommand = event.getActionCommand();
                    if (actionCommand.equals("cancel"))
                    {
                        ContextMenu.this.setVisible(false);
                    }
                    else if (actionCommand.equals("mark-square"))
                    {                        
                        boolean markSquare
                            = ContextMenu.this.m_markSquare.isSelected();
                        listener.markSquare(m_point, markSquare);
                    }
                    else if (actionCommand.equals("edit-label"))
                    {                        
                        listener.editLabel(m_point);
                    }
                    else
                    {
                        int index = Integer.parseInt(actionCommand);
                        AnalyzeCommand command = getCommand(index);
                        command.setPointArg(m_point);
                        listener.setAnalyzeCommand(command);
                    }
                }
            };
        JLabel label = new JLabel("Point " + point);
        label.setBorder(GuiUtils.createSmallEmptyBorder());
        add(label);
        addSeparator();
        m_markSquare = createCheckBox("Mark Square", "mark-square");
        m_markSquare.setSelected(markSquare);
        add(m_markSquare);
        add(createItem("Edit Label", "edit-label"));
        addSeparator();
        if (! noProgram && commands.size() > 0)
        {
            for (int i = 0; i < commands.size(); ++i)
            {
                String line = (String)commands.get(i);
                AnalyzeCommand command = new AnalyzeCommand(line);
                if (command.needsOnlyPointArg())
                    addCommand(command);
                else if (command.needsOnlyPointAndColorArg())
                    addColorCommand(command);
            }
            addSeparator();
        }
        JMenuItem item = new JMenuItem("Cancel");
        item.addActionListener(m_actionListener);
        item.setActionCommand("cancel");
        add(item);
    }

    public GoPoint getPointArg()
    {
        return m_point;
    }

    public boolean isEmpty()
    {
        return (m_commands.size() == 0);
    }

    /** Serial version to suppress compiler warning.
        Contains a marker comment for serialver.sourceforge.net
    */
    private static final long serialVersionUID = 0L; // SUID

    private ActionListener m_actionListener;

    private GoPoint m_point;

    private JCheckBoxMenuItem m_markSquare;

    private JMenuItem m_editLabel;

    private Listener m_listener;

    /** Vector<AnalyzeCommand> */
    private Vector m_commands = new Vector();

    private void addColorCommand(AnalyzeCommand command)
    {
        String label = command.getLabel();
        JMenu menu = new JMenu(label);
        command.setColorArg(GoColor.BLACK);
        JMenuItem item = createItem(command, "Black");
        menu.add(item);
        command = command.cloneCommand();
        command.setColorArg(GoColor.WHITE);
        item = createItem(command, "White");
        menu.add(item);
        add(menu);
    }

    private void addCommand(AnalyzeCommand command)
    {        
        JMenuItem item = createItem(command, command.getLabel());
        add(item);
    }

    private JMenuItem createItem(AnalyzeCommand command, String label)
    {
        assert(! m_commands.contains(command));
        m_commands.add(command);
        return createItem(label, Integer.toString(m_commands.size() - 1));
    }

    private JCheckBoxMenuItem createCheckBox(String label,
                                             String actionCommand)
    {
        JCheckBoxMenuItem item = new JCheckBoxMenuItem(label);
        item.addActionListener(m_actionListener);
        item.setActionCommand(actionCommand);
        return item;
    }

    private JMenuItem createItem(String label, String actionCommand)
    {
        JMenuItem item = new JMenuItem(label);
        item.addActionListener(m_actionListener);
        item.setActionCommand(actionCommand);
        return item;
    }

    private AnalyzeCommand getCommand(int index)
    {
        return (AnalyzeCommand)m_commands.get(index);
    }
}

//----------------------------------------------------------------------------
