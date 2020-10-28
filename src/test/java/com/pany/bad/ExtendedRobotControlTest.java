package com.pany.bad;

import java.awt.AWTException;
import java.awt.GridLayout;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class ExtendedRobotControlTest
{
    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                createAndShowGUI();
            }
        });
    }

    private static void createAndShowGUI()
    {
        JFrame f = new JFrame();
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        final RobotControl robotControl = new RobotControl();

        f.getContentPane().setLayout(new GridLayout(1,2));

        final JButton startButton = new JButton("Start");
        f.getContentPane().add(startButton);

        final JButton stopButton = new JButton("Stop");
        stopButton.setEnabled(false);
        f.getContentPane().add(stopButton);

        startButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                startButton.setEnabled(false);
                stopButton.setEnabled(true);
                robotControl.startControl();
            }
        });

        stopButton.addActionListener(new ActionListener()
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                robotControl.stopControl();
                startButton.setEnabled(true);
                stopButton.setEnabled(false);
            }
        });

        f.pack();
        f.setLocationRelativeTo(null);
        f.setVisible(true);
    }
}

class RobotControl
{
    private final Random random = new Random();
    private final Robot robot;

    private Thread clickingThread;
    private Thread observingThread;

    private long lastMovementMillis = -1;
    private Point lastMousePosition = null;

    private volatile boolean running = false;

    RobotControl()
    {
        try
        {
            robot = new Robot();
        }
        catch (AWTException e)
        {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    void startObserver()
    {
        observingThread = new Thread(new Runnable(){

            @Override
            public void run()
            {
                observeMovement();
            }
        });
        observingThread.start();
    }

    private void observeMovement()
    {
        while (running)
        {
            Point p = MouseInfo.getPointerInfo().getLocation();
            if (!p.equals(lastMousePosition))
            {
                System.out.println("Movement detected");
                lastMovementMillis = System.currentTimeMillis();
                lastMousePosition = p;
            }
            try
            {
                Thread.sleep(50);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
                Thread.currentThread().interrupt();
                return;
            }
        }
    }


    void startControl()
    {
        stopControl();
        System.out.println("Starting");
        lastMovementMillis = System.currentTimeMillis();
        lastMousePosition = MouseInfo.getPointerInfo().getLocation();
        running = true;
        startClicking();
        startObserver();
    }

    private void startClicking()
    {
        clickingThread = new Thread(new Runnable()
        {
            @Override
            public void run()
            {
                performClicks();
            }
        });
        clickingThread.start();
    }

    void stopControl()
    {
        if (running)
        {
            System.out.println("Stopping");
            running = false;
            try
            {
                clickingThread.join(5000);
                observingThread.join(5000);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }
    }

    private void performClicks()
    {
        System.out.println("Starting");
        while (running)
        {
            long t = System.currentTimeMillis();
            if (t > lastMovementMillis + 1000)
            {
                leftClick();
                System.out.println("Clicked");
            }
            else
            {
                System.out.println("Waiting before clicks...");
                try
                {
                    Thread.sleep(50);
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
    }

    private void leftClick()
    {
        int no = random.nextInt(6) + 1;
        robot.mousePress(InputEvent.BUTTON1_MASK);
        robot.delay(50 * no);
        robot.mouseRelease(InputEvent.BUTTON1_MASK);
        robot.delay(220 * no);
    }
}