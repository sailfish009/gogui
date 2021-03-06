// Clock.java

package net.sf.gogui.game;

import java.util.TimerTask;
import java.util.Timer;
import net.sf.gogui.go.BlackWhiteSet;
import net.sf.gogui.go.GoColor;
import net.sf.gogui.gogui.GoGui;
import static net.sf.gogui.go.GoColor.BLACK;
import static net.sf.gogui.go.GoColor.WHITE;
import static net.sf.gogui.gogui.GoGui.BACKWARD_CLICKED;
import static net.sf.gogui.gogui.GoGui.FISCHER_RULE;
import static net.sf.gogui.gogui.GoGui.TOGGLE_BEEP;
import net.sf.gogui.util.StringUtil;

/** Time control for a Go game.
    If the clock is not initialized with Clock.setTimeSettings, the clock
    will count upwards, otherwise the time settings with main and/or
    byoyomi time are used. The time unit is milliseconds. */
public final class Clock
    implements ConstClock
{
    /** Provides the time for a clock. */
    public interface TimeSource
    {
        long currentTimeMillis();
    }

    /** Time source using the system time. */
    public static final class SystemTimeSource
        implements TimeSource
    {
        public long currentTimeMillis()
        {
            return System.currentTimeMillis();
        }
    }

    /** Listener to clock changes.
        This function will be called from a different thread at regular
        intervals. */
    public interface Listener
    {
        void clockChanged();
    }

    public Clock()
    {
        this(new SystemTimeSource());
    }

    public Clock(TimeSource timeSource)
    {
        m_timeSource = timeSource;
        reset();
    }

    /** Get moves left.
        Requires: getUseByoyomi() and isInByoyomi(color) */
    public int getMovesLeft(GoColor color)
    {
        assert getUseByoyomi() && isInByoyomi(color);
        return getRecord(color).m_movesLeft;
    }

    /** Get time left.
        Requires: isInitialized() */
    public long getTimeLeft(GoColor color)
    {
        assert isInitialized();
        TimeRecord record = getRecord(color);
        long time = record.m_time;
        if (getUseByoyomi() && isInByoyomi(color))
            return (getByoyomi() - time);
        else
            return (getPreByoyomi() - time);
    }

    public TimeSettings getTimeSettings()
    {
        return m_timeSettings;
    }

    private boolean run_once_white = true;
    private boolean run_once_black = true;
    private boolean pretime_white = false;
    private boolean pretime_black = false;
    private long pre_time_black = 0;
    private long pre_time_white = 0;

    // fischer rule
    private boolean add_once_white = false;
    private boolean add_once_black = false;
    private long total_time_white = 0;
    private long total_time_black = 0;

    private boolean first_once_black = true;
    private boolean first_once_white = true;

    public String getTimeString(GoColor color)
    {
        assert color.isBlackWhite();
        TimeRecord record = getRecord(color);
        long time = record.m_time;

        if (m_isRunning == false)
        {
            if(FISCHER_RULE)
            {
                add_once_white = add_once_black =
                    run_once_white = run_once_black = false;
            }
            pre_time_black = pre_time_white =
                total_time_black = total_time_white = PRE_TIME_COUNT;
           return getTimeString((double)PRE_TIME_COUNT, -1);
        }

        if(FISCHER_RULE)
        {
            if (color.equals(m_toMove))
            {
                switch(color)
                {
                    case BLACK:
                    {
                        if(add_once_black)
                        {
                            add_once_black = false;
                            m_startTime = currentTimeMillis();
                        }

                        time += currentTimeMillis() - m_startTime;
                        time = total_time_black - time/1000L;
                        pre_time_black = time;

                        if (time >=0 && time < 11)
                        {
                            java.awt.Toolkit.getDefaultToolkit().beep();
                        }
                        else if(time < 0)
                        {
                            m_lost_time_black = true;
                            System.out.println(" black time out ");
                        }
                        return getTimeString((double)time, -1);
                    }

                    case WHITE:
                    {
                        if(add_once_white)
                        {
                            add_once_white = false;
                            m_startTime = currentTimeMillis();
                        }

                        time += currentTimeMillis() - m_startTime;
                        time = total_time_white - time/1000L;
                        pre_time_white = time;

                        if (time >=0 && time < 11)
                        {
                            java.awt.Toolkit.getDefaultToolkit().beep();
                        }
                        else if(time < 0)
                        {
                            m_lost_time_white = true;
                            System.out.println(" white time out ");
                        }
                        return getTimeString((double)time, -1);
                    }
                }
            }
            else
            {
                switch(color)
                {
                    case BLACK:
                    {
                        if(first_once_black)
                        {
                            first_once_black = false;
                            return getTimeString((double)total_time_black, -1);
                        }

                        if(run_once_black)
                        {
                            run_once_black = false;
                            pre_time_black += TIME_COUNT;
                            TimeRecord b_record = getRecord(color);
                            b_record.m_time = 0;
                            total_time_black = pre_time_black;
                            add_once_black = true;
                        }
                        return getTimeString((double)total_time_black, -1);
                    }

                    case WHITE:
                    {
                        if(first_once_white)
                        {
                            first_once_white = false;
                            return getTimeString((double)total_time_white, -1);
                        }

                        if(run_once_white)
                        {
                            run_once_white = false;
                            pre_time_white += TIME_COUNT;
                            TimeRecord w_record = getRecord(color);
                            w_record.m_time = 0;
                            total_time_white = pre_time_white;
                            add_once_white = true;
                        }
                        return getTimeString((double)total_time_white, -1);
                    }
                }
            }

        }
        else
        {
            if (color.equals(m_toMove))
            {
                switch(color)
                {
                    case BLACK:
                        if(pretime_black == false)
                        {
                            time += currentTimeMillis() - m_startTime;
                            time = time / 1000L;
                            time = PRE_TIME_COUNT - time;
                            if (time == 0)
                                pretime_black = true;
                            pre_time_black = time;
                            return getTimeString((double)time, -1);
                        }
                        else
                        {
                            if (run_once_black && m_isRunning)
                            {
                                run_once_black = false;
                                m_prevTime_black = currentTimeMillis();
                            }

                            time = currentTimeMillis() - m_prevTime_black;
                            time = time / 1000L;
                            time = TIME_COUNT - time;
                            if (time >=0 && time < 11)
                            {
                                if(TOGGLE_BEEP)
                                    java.awt.Toolkit.getDefaultToolkit().beep();
                                else if( m_lost_time_white == false && m_lost_time_black == false)
                                    java.awt.Toolkit.getDefaultToolkit().beep();
                            }
                            else if (time < 0)
                            {
                                if( m_lost_time_white == false && m_lost_time_black == false)
                                    java.awt.Toolkit.getDefaultToolkit().beep();

                                time = TIME_COUNT;
                                m_prevTime_black = currentTimeMillis();
                                if(m_chance_black != 0)
                                    m_chance_black -= 1;
                                if(m_chance_black == 0)
                                {
                                    m_lost_time_black = true;
                                    System.out.println(" black time out ");
                                }
                            }
                            pre_time_black = time;
                            return getTimeString((double)time, m_chance_black);
                        }

                    case WHITE:
                        if(pretime_white == false)
                        {
                            time += currentTimeMillis() - m_startTime;
                            time = time / 1000L;
                            time = PRE_TIME_COUNT - time;
                            if (time == 0)
                                pretime_white = true;
                            pre_time_white = time;
                            return getTimeString((double)time, -1);
                        }
                        else
                        {
                            if (run_once_white && m_isRunning)
                            {
                                run_once_white = false;
                                m_prevTime_white = currentTimeMillis();
                            }

                            time = currentTimeMillis() - m_prevTime_white;
                            time = time / 1000L;
                            time = TIME_COUNT - time;
                            if (time >=0 && time < 11)
                            {
                                if( m_lost_time_white == false && m_lost_time_black == false)
                                    java.awt.Toolkit.getDefaultToolkit().beep();
                            }
                            else if (time < 0)
                            {

                                if( m_lost_time_white == false && m_lost_time_black == false)
                                    java.awt.Toolkit.getDefaultToolkit().beep();

                                time = TIME_COUNT;
                                m_prevTime_white = currentTimeMillis();
                                if(m_chance_white != 0)
                                    m_chance_white -= 1;
                                if(m_chance_white == 0)
                                {
                                    m_lost_time_white = true;
                                    System.out.println(" white time out ");
                                }
                            }
                            pre_time_white = time;
                            return getTimeString((double)time, m_chance_white);
                        }

                }
            }
            else
            {
                switch(color)
                {
                    case BLACK:
                        {
                            if(pretime_black == false)
                            {
                                time = pre_time_black; 
                                m_prevTime_black = currentTimeMillis();
                                return getTimeString((double)time, -1);
                            }
                            else
                            {
                                time = TIME_COUNT; 
                                m_prevTime_black = currentTimeMillis();

                                return getTimeString((double)time, m_chance_black);
                            }
                        }

                    case WHITE:
                        {
                            if(pretime_white == false)
                            {
                                time = pre_time_white; 
                                m_prevTime_white = currentTimeMillis();
                                return getTimeString((double)time, -1);
                            }
                            else
                            {
                                time = TIME_COUNT; 
                                m_prevTime_white = currentTimeMillis();

                                return getTimeString((double)time, m_chance_white);
                            }
                        }
                }
            }

        }

        return "00:00";
    }


    // orig
    // public String getTimeString(GoColor color)
    // {
    //     assert color.isBlackWhite();
    //     TimeRecord record = getRecord(color);
    //     long time = record.m_time;
    //     if (color.equals(m_toMove))
    //         time += currentTimeMillis() - m_startTime;
    //     if (isInitialized())
    //     {
    //         if (record.m_isInByoyomi)
    //             time = getByoyomi() - time;
    //         else
    //             time = getPreByoyomi() - time;
    //     }
    //     int movesLeft = -1;
    //     if (isInitialized() && record.m_isInByoyomi)
    //     {
    //         movesLeft = record.m_movesLeft;
    //     }
    //     // Round time to seconds
    //     time = time / 1000L;
    //     return getTimeString((double)time, movesLeft);
    // }

    /** Format time left to a string.
        If movesLeft &lt; 0, only the time will be returned, otherwise
        after the time string, a slash and the number of moves left will be
        appended. */
    public static String getTimeString(double timeLeft, int movesLeft)
    {
        StringBuilder buffer = new StringBuilder(8);
        buffer.append(StringUtil.formatTime((long)timeLeft));
        if (movesLeft > 0)
        {
            buffer.append('/');
            buffer.append(movesLeft);
        }
        else if (movesLeft == 0)
        {
            buffer.append('/');
            buffer.append('X');
        }

        return buffer.toString();
    }

    /** Return color the clock is currently measuring the time for.
        Returns null, if clock is between a #stopMove and #startMove. */
    public GoColor getToMove()
    {
        return m_toMove;
    }

    public boolean getUseByoyomi()
    {
        return m_timeSettings.getUseByoyomi();
    }

    public void halt()
    {
        if (! m_isRunning)
            return;

        TimeRecord record = getRecord(m_toMove);
        long currentTime = currentTimeMillis();
        long time = currentTime - m_startTime;
        m_startTime = currentTime;
        record.m_time += time;
        m_isRunning = false;
        updateListener();
        stopTimer();

        pre_time_black = pre_time_white = PRE_TIME_COUNT;

        if (BACKWARD_CLICKED)
        {
            BACKWARD_CLICKED = false;

            run_once_white = run_once_black = true;

            if(m_lost_time_white)
                m_chance_white = 1;

            if(m_lost_time_black)
                m_chance_black = 1;

            m_lost_time_white = m_lost_time_black = false;
        }
        else
        {
            m_chance_white = m_chance_black = LEFT_COUNT;
            run_once_white = run_once_black = true;
            m_lost_time_white = m_lost_time_black = 
                pretime_black = pretime_white = false;
        }

        if(FISCHER_RULE)
        {
            add_once_white = add_once_black = false;
            total_time_white = total_time_black = 0;
            first_once_black = first_once_white = true;
        }

    }

    public boolean isInitialized()
    {
        return (m_timeSettings != null);
    }

    public boolean isInByoyomi(GoColor color)
    {
        return getUseByoyomi() && getRecord(color).m_isInByoyomi;
    }

    public boolean isRunning()
    {
        return m_isRunning;
    }

    private boolean m_lost_time_white = false;
    private boolean m_lost_time_black = false;

    public boolean lostOnTime(GoColor color)
    {
        switch(color)
        {
            case BLACK: return m_lost_time_black;
            case WHITE: return m_lost_time_white;
        }
        return false;
    }


    // orig
    // public boolean lostOnTime(GoColor color)
    // {
    //     if (! isInitialized())
    //         return false;
    //     TimeRecord record = getRecord(color);
    //     long time = record.m_time;
    //     if (getUseByoyomi())
    //         return record.m_byoyomiExceeded;
    //     else
    //         return (time > getPreByoyomi());
    // }

    /** Parses a time string.
        The expected format is <tt>[[H:]MM:]SS</tt>.
        @return The time in milliseconds or -1, if the time string is not
        valid. */
    public static long parseTimeString(String s)
    {
        String a[] = s.split(":");
        if (a.length == 0 || a.length > 3)
            return -1;
        int hours = 0;
        int minutes = 0;
        int seconds = 0;
        try
        {
            if (a.length == 3)
            {
                hours = Integer.parseInt(a[0]);
                minutes = Integer.parseInt(a[1]);
                seconds = Integer.parseInt(a[2]);
            }
            else if (a.length == 2)
            {
                minutes = Integer.parseInt(a[0]);
                seconds = Integer.parseInt(a[1]);
            }
            else
            {
                assert a.length == 1;
                seconds = Integer.parseInt(a[0]);
            }
        }
        catch (NumberFormatException e)
        {
            return -1;
        }
        if (minutes < 0 || minutes > 60 || seconds < 0 ||seconds > 60)
            return -1;
        return 1000L * (seconds + minutes * 60L + hours * 3600L);
    }

    public void reset()
    {
        reset(BLACK);
        reset(WHITE);
        m_toMove = null;
        m_isRunning = false;
        updateListener();
    }

    public void reset(GoColor color)
    {
        TimeRecord timeRecord = getRecord(color);
        timeRecord.m_time = 0;
        timeRecord.m_movesLeft = 0;
        timeRecord.m_isInByoyomi = false;
        timeRecord.m_byoyomiExceeded = false;
        if (isInitialized() && getPreByoyomi() == 0)
        {
            assert getByoyomiMoves() > 0;
            timeRecord.m_movesLeft = getByoyomiMoves();
            timeRecord.m_isInByoyomi = true;
        }
        updateListener();
    }

    /** Resume clock, if it was halted during a player's move time. */
    public void resume()
    {
        if (m_isRunning)
            return;
        assert m_toMove != null;
        m_startTime = currentTimeMillis();
        m_isRunning = true;
        startTimer();
    }

    /** Register listener for clock changes.
        Only one listener supported at the moment.
        If the clock has a listener, the clock should be stopped with halt()
        if it is no longer used, otherwise the timer thread can keep an
        application from terminating. */
    public void setListener(Listener listener)
    {
        m_listener = listener;
    }

    /** Set time settings.
        Changing the time settings does not change the current state of the
        clock. The time settings are only used when the clock is reset or
        the next byoyomi period is initialized. */
    public void setTimeSettings(TimeSettings settings)
    {
        try
        {
            PRE_TIME_COUNT = settings.getPreByoyomi() / 1000L;
            TIME_COUNT = settings.getByoyomi() / 1000L;
            LEFT_COUNT = settings.getByoyomiMoves();

            m_chance_black = m_chance_white = LEFT_COUNT;
        }
        catch(Exception e)
        {}
    }

    // orig
    // public void setTimeSettings(TimeSettings settings)
    // {
    //     m_timeSettings = settings;
    // }

    /** Set time left.
        @param color Color to set the time for.
        @param time New value for time left.
        @param movesLeft -1, if not in byoyomi. */
    public void setTimeLeft(GoColor color, long time, int movesLeft)
    {
        halt();
        boolean isInByoyomi = (movesLeft >= 0);
        TimeRecord record = getRecord(color);
        if (isInByoyomi)
        {
            // We cannot handle setting the time left in overtime if we don't
            // know the overtime settings (e.g. if an SGF file was loaded
            // that has TM,OT and BL/WL/OB/OW properties but we couldn't parse
            // the value of OT, which is not standardized in SGF, or could
            // use an overtime system not supported by GoGui (GoGui supports
            // only the Canadian overtime system as used by the time_settings
            // GTP command
            if (! m_timeSettings.getUseByoyomi())
                return;
            record.m_isInByoyomi = isInByoyomi;
            record.m_time = getByoyomi() - time;
            record.m_movesLeft = movesLeft;
            record.m_byoyomiExceeded = time > 0;
        }
        else
        {
            record.m_time = getPreByoyomi() - time;
            record.m_movesLeft = -1;
            record.m_byoyomiExceeded = false;
        }
        if (m_toMove != null)
            startMove(m_toMove);
        updateListener();
    }

    /** Start time for a move.
        If the clock was already running, the passed time for the current move
        is discarded. */
    public void startMove(GoColor color)
    {
        assert color.isBlackWhite();
        m_toMove = color;
        m_isRunning = true;
        m_startTime = currentTimeMillis();
        startTimer();
        if (color == BLACK)
        {
            // System.out.println("WHITE : "+ m_prevTime_white);
            m_prevTime_white = m_startTime;
            if(FISCHER_RULE)
            {
                // first_once_black = first_once_white = false;
                run_once_white = true;
            }
        }
        else
        {
            // System.out.println("BLACK : "+ m_prevTime_black);
            m_prevTime_black = m_startTime;
            if(FISCHER_RULE)
            {
                // first_once_black = first_once_white = false;
                run_once_black = true;
            }
        }
    }

    /** Stop time for a move.
        If the clock was running, the time for the move is added to the
        total time for the color the clock was running for; otherwise
        this function does nothing. */
    public void stopMove()
    {
        if (! m_isRunning)
            return;
        TimeRecord record = getRecord(m_toMove);
        long time = currentTimeMillis() - m_startTime;
        record.m_time += time;
        if (isInitialized() && getUseByoyomi())
        {
            if (! record.m_isInByoyomi
                && record.m_time > getPreByoyomi())
            {
                record.m_isInByoyomi = true;
                record.m_time -= getPreByoyomi();
                assert getByoyomiMoves() > 0;
                record.m_movesLeft = getByoyomiMoves();
            }
            if (record.m_isInByoyomi)
            {
                if (record.m_time > getByoyomi())
                    record.m_byoyomiExceeded = true;
                assert record.m_movesLeft > 0;
                --record.m_movesLeft;
                if (record.m_movesLeft == 0)
                {
                    record.m_time = 0;
                    assert getByoyomiMoves() > 0;
                    record.m_movesLeft = getByoyomiMoves();
                }
            }
        }
        m_toMove = null;
        m_isRunning = false;
        updateListener();
    }

    private static class TimeRecord
    {
        public boolean m_isInByoyomi;

        public boolean m_byoyomiExceeded;

        public int m_movesLeft;

        public long m_time;
    }

    private boolean m_isRunning = false;

    private long m_startTime;

    // prebyoyomi
    private long  PRE_TIME_COUNT = 60; // 60s x 1 (1 min)
    // byoyomi
    private long  TIME_COUNT = 30;

    private int  LEFT_COUNT = 3;

    private long m_prevTime_white;
    private long m_prevTime_black;
    private int  m_chance_white = 3;
    private int  m_chance_black = 3;

    private GoColor m_toMove;

    private final BlackWhiteSet<TimeRecord> m_timeRecord
        = new BlackWhiteSet<TimeRecord>(new TimeRecord(), new TimeRecord());

    private TimeSettings m_timeSettings;

    private Listener m_listener;

    private Timer m_timer;

    private final TimeSource m_timeSource;

    private long currentTimeMillis()
    {
        return m_timeSource.currentTimeMillis();
    }

    private TimeRecord getRecord(GoColor c)
    {
        return m_timeRecord.get(c);
    }

    private long getByoyomi()
    {
        return m_timeSettings.getByoyomi();
    }

    private int getByoyomiMoves()
    {
        return m_timeSettings.getByoyomiMoves();
    }

    private long getPreByoyomi()
    {
        return m_timeSettings.getPreByoyomi();
    }

    private void startTimer()
    {
        if (m_timer == null && m_listener != null)
        {
            m_timer = new Timer();
            TimerTask task = new TimerTask() {
                    public void run() {
                        updateListener();
                    }
                };
            m_timer.scheduleAtFixedRate(task, 1000, 1000);
        }
    }

    private void stopTimer()
    {
        if (m_timer != null)
        {
            m_timer.cancel();
            m_timer = null;
        }
    }

    private void updateListener()
    {
        if (m_listener != null)
            m_listener.clockChanged();
    }
}
