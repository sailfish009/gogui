//----------------------------------------------------------------------------
// $Id$
// $Source$
//----------------------------------------------------------------------------

package gtpdummy;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import go.Point;
import gtp.GtpServer;
import utils.StringUtils;
import version.Version;

//----------------------------------------------------------------------------

/** Dummy Go program for testing GTP controlling programs. */
public class GtpDummy
    extends GtpServer
{
    public GtpDummy(InputStream in, OutputStream out, PrintStream log)
        throws Exception
    {
        super(in, out, log);
        initSize(19);
        m_thread = Thread.currentThread();
    }

    public boolean handleCommand(String cmdLine, StringBuffer response)
    {
        String[] cmdArray = StringUtils.tokenize(cmdLine);
        String cmd = cmdArray[0];
        boolean status = true;
        if (m_nextResponseFixed
            && ! (cmd.equals("dummy_next_failure")
                  || cmd.equals("dummy_next_success")))
        {
            status = m_nextStatus;
            response.append(m_nextResponse);
            m_nextResponseFixed = false;
        }
        else if (cmd.equals("boardsize"))
            status = cmdBoardsize(cmdArray, response);
        else if (cmd.equals("clear_board"))
            status = cmdClearBoard(response);
        else if (cmd.equals("dummy_bwboard"))
            bwBoard(response);
        else if (cmd.equals("dummy_delay"))
            status = cmdDelay(cmdArray, response);
        else if (cmd.equals("dummy_invalid"))
            cmdInvalid();
        else if (cmd.equals("dummy_long_response"))
            status = cmdLongResponse(cmdArray, response);
        else if (cmd.equals("dummy_crash"))
            crash();
        else if (cmd.equals("dummy_next_failure"))
            nextResponseFixed(cmd, cmdLine, false);
        else if (cmd.equals("dummy_next_success"))
            nextResponseFixed(cmd, cmdLine, true);
        else if (cmd.equals("dummy_sleep"))
            status = sleep(cmdArray, response);
        else if (cmd.equals("echo"))
            echo(cmdLine, response);
        else if (cmd.equals("echo_err"))
            echoErr(cmdLine);
        else if (cmd.equals("genmove"))
            status = cmdGenmove(response);
        else if (cmd.equals("gogui_interrupt"))
            ;
        else if (cmd.equals("name"))
            response.append("GtpDummy");
        else if (cmd.equals("play"))
            status = cmdPlay(cmdArray, response);
        else if (cmd.equals("protocol_version"))
            response.append("2");
        else if (cmd.equals("list_commands"))
            response.append("boardsize\n" +
                            "clear_board\n" +
                            "dummy_bwboard\n" +
                            "dummy_crash\n" +
                            "dummy_delay\n" +
                            "dummy_invalid\n" +
                            "dummy_long_response\n" +
                            "dummy_next_success\n" +
                            "dummy_next_failure\n" +
                            "dummy_sleep\n" +
                            "echo\n" +
                            "echo_err\n" +
                            "genmove\n" +
                            "gogui_interrupt\n" +
                            "list_commands\n" +
                            "name\n" +
                            "play\n" +
                            "protocol_version\n" +
                            "quit\n" +
                            "version\n");
        else if (cmd.equals("version"))
            response.append(Version.get());
        else if (cmd.equals("quit"))
            ;
        else
        {
            response.append("unknown command");
            status = false;
        }
        if (m_delay > 0 && ! cmd.equals("dummy_delay"))
        {
            try
            {
                Thread.sleep(1000L * m_delay);
            }
            catch (InterruptedException e)
            {
            }
        }
        return status;
    }

    public void interruptCommand()
    {
        m_thread.interrupt();
    }

    private boolean m_nextResponseFixed;

    private boolean m_nextStatus;

    /** Delay every command (seconds) */
    private int m_delay;

    private int m_size;

    private boolean[][] m_alreadyPlayed;

    private String m_nextResponse;

    private Thread m_thread;

    private void bwBoard(StringBuffer response)
    {        
        response.append("\n");
        for (int x = 0; x < m_size; ++x)
        {
            for (int y = 0; y < m_size; ++y)
                response.append(Math.random() > 0.5 ? "B " : "W ");
            response.append("\n");
        }                    
    }

    private boolean cmdBoardsize(String[] cmdArray, StringBuffer response)
    {
        IntegerArgument argument = parseIntegerArgument(cmdArray, response);
        if (argument == null)
            return false;
        if (argument.m_integer < 1 || argument.m_integer > 1000)
        {
            response.append("Invalid size");
            return false;
        }
        initSize(argument.m_integer);
        return true;
    }

    private boolean cmdClearBoard(StringBuffer response)
    {
        initSize(m_size);
        return true;
    }

    private boolean cmdDelay(String[] cmdArray, StringBuffer response)
    {
        IntegerArgument argument = parseIntegerArgument(cmdArray, response);
        if (argument == null)
        {
            response.delete(0, response.length());
            response.append(m_delay);
            return true;
        }
        if (argument.m_integer < 0)
        {
            response.append("Argument must be positive");
            return false;
        }
        m_delay = argument.m_integer;
        return true;
    }

    private boolean cmdGenmove(StringBuffer response)
    {
        int numberPossibleMoves = 0;
        for (int x = 0; x < m_size; ++x)
            for (int y = 0; y < m_size; ++y)
                if (! m_alreadyPlayed[x][y])
                    ++numberPossibleMoves;
        Point point = null;
        if (numberPossibleMoves > 0)
        {
            int rand = (int)(Math.random() * numberPossibleMoves);
            int index = 0;
            for (int x = 0; x < m_size && point == null; ++x)
                for (int y = 0; y < m_size && point == null; ++y)
                    if (! m_alreadyPlayed[x][y])
                    {
                        if (index == rand)
                            point = new Point(x, y);
                        ++index;
                    }
        }
        response.append(Point.toString(point));
        if (point != null)
            m_alreadyPlayed[point.getX()][point.getY()] = true;
        return true;
    }

    private void cmdInvalid()
    {        
        printInvalidResponse("This is an invalid GTP response.\n" +
                             "It does not start with a status character.\n");
    }

    private boolean cmdLongResponse(String[] cmdArray, StringBuffer response)
    {        
        IntegerArgument argument = parseIntegerArgument(cmdArray, response);
        if (argument == null)
            return false;
        for (int i = 1; i <= argument.m_integer; ++i)
        {
            response.append(i);
            response.append("\n");
        }
        return true;
    }

    private boolean cmdPlay(String[] cmdArray, StringBuffer response)
    {
        ColorPointArgument argument =
            parseColorPointArgument(cmdArray, response, m_size);
        if (argument == null)
            return false;
        Point point = argument.m_point;
        if (point != null)
            m_alreadyPlayed[point.getX()][point.getY()] = true;
        return true;
    }

    private void crash()
    {        
        System.err.println("Aborting GtpDummy");
        System.exit(-1);
    }

    private void echo(String cmdLine, StringBuffer response)
    {
        int index = cmdLine.indexOf(" ");
        if (index < 0)
            return;
        response.append(cmdLine.substring(index + 1));
    }

    private void echoErr(String cmdLine)
    {
        int index = cmdLine.indexOf(" ");
        if (index < 0)
            return;
        System.err.println(cmdLine.substring(index + 1));
    }

    private void initSize(int size)
    {
        m_alreadyPlayed = new boolean[size][size];
        m_size = size;
    }

    private void nextResponseFixed(String cmd, String cmdLine,
                                   boolean nextStatus)
    {
        m_nextResponseFixed = true;
        m_nextStatus = nextStatus;
        m_nextResponse = cmdLine.substring(cmd.length()).trim();
    }

    private boolean sleep(String[] cmdArray, StringBuffer response)
    {
        long millis = 20000;
        if (cmdArray.length > 1)
        {
            try
            {
                millis = (long)(Double.parseDouble(cmdArray[1]) * 1000.0);
            }
            catch (NumberFormatException e)
            {
                response.append("Invalid argument");
                return false;
            }
        }
        try
        {
            Thread.sleep(millis);
        }
        catch (InterruptedException e)
        {
            response.append("Interrupted");
            return false;
        }
        return true;
    }
}

//----------------------------------------------------------------------------
