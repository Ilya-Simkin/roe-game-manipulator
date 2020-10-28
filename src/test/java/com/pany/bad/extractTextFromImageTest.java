package com.pany.bad;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;

@RunWith(SpringRunner.class)
@ActiveProfiles("manual-activation")
public class extractTextFromImageTest {


    @Test
    public void runTheTest() throws InvocationTargetException, InterruptedException {

        // Load the native OpenCV library
        nu.pattern.OpenCV.loadShared();
        // Schedule a job for the event dispatch thread:
        // creating and showing this application's GUI.
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new runTextDetectionTestScreen();
            }
        });
       while(true){
           Thread.sleep(10000);
       }
    }



    public class runTextDetectionTestScreen {

        private String[] slideBarNames = new String[]{"HMin", "SMin", "VMin", "HMax", "SMax", "VMax"};
        private int[] slideBarValues = new int[]{0, 0, 200, 172, 65, 255};
        private int[] maxValues = new int[]{172, 255, 255, 172, 255, 255};

        private int erosion_size = 1;
        private int dilation_size = 2;

        private Mat matImgSrc1;
        private Mat matImgDst = new Mat();
        private JFrame frame;
        private JLabel imgLabel;

        public runTextDetectionTestScreen() {
            String imagePath1 = "C:\\gitRepository\\my-projects\\roe-map-scanner\\src\\main\\resources\\images\\textExample3.jpg";

            matImgSrc1 = Imgcodecs.imread(imagePath1);

            if (matImgSrc1.empty()) {
                System.out.println("Empty image: " + imagePath1);
                Assert.assertEquals(1,1);

                System.exit(0);

            }

            // Create and set up the window.
            frame = new JFrame("HSV Graph");
            frame.setSize(500,500);


            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            // Set up the content pane.
            Image img = HighGui.toBufferedImage(matImgSrc1);
            addComponentsToPane(frame.getContentPane(), img);

            // Use the content pane's default BorderLayout. No need for
            // setLayout(new BorderLayout());
            // Display the window.
            frame.pack();
            frame.setVisible(true);
        }

        private void addComponentsToPane(Container pane, Image img) {
            if (!(pane.getLayout() instanceof BorderLayout)) {
                pane.add(new JLabel("Container doesn't use BorderLayout!"));
                return;
            }

            JPanel mainPanel = new JPanel(); // main panel
            mainPanel.setLayout(new GridLayout(9, 1));
            JLabel label1 = new JLabel("HSV and Erosion Test"
            );
            pane.add(label1, BorderLayout.NORTH);
            for (int i = 0; i < 6; i++) {
                createSlider(i, mainPanel);
            }
            createErodeSlider(mainPanel);
            createDilationSlider(mainPanel);


            pane.add(mainPanel, BorderLayout.NORTH);
            imgLabel = new JLabel(new ImageIcon(img));
            pane.add(imgLabel, BorderLayout.CENTER);
        }

        private void createSlider(int index, Container pane) {

            JPanel sliderPane = new JPanel();
            sliderPane.setLayout(new BoxLayout(sliderPane, BoxLayout.Y_AXIS));
            sliderPane.add(new JLabel(String.format(slideBarNames[index] + " x %d", maxValues[index])));

            JSlider slider2 = new JSlider(0, maxValues[index], slideBarValues[index]);
            slider2.setMajorTickSpacing(20);
            slider2.setMinorTickSpacing(5);
            slider2.setPaintTicks(true);
            slider2.setPaintLabels(true);
            slider2.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    JSlider source = (JSlider) e.getSource();
                    slideBarValues[index] = source.getValue();
                    updateErosion();
                }
            });
            sliderPane.add(slider2);
            pane.add(sliderPane, BorderLayout.CENTER);
            pane.revalidate();
        }

        private void createErodeSlider(Container pane) {
            JPanel sliderPane = new JPanel();
            sliderPane.setLayout(new BoxLayout(sliderPane, BoxLayout.Y_AXIS));
            sliderPane.add(new JLabel(String.format("Erode x %d", 6)));

            JSlider slider2 = new JSlider(0, 6, 0);
            slider2.setMajorTickSpacing(1);
            slider2.setMinorTickSpacing(1);
            slider2.setPaintTicks(true);
            slider2.setPaintLabels(true);
            slider2.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    JSlider source = (JSlider) e.getSource();
                    erosion_size = source.getValue();
                    updateErosion();
                }
            });
            sliderPane.add(slider2);
            pane.add(sliderPane, BorderLayout.CENTER);
            pane.revalidate();
        }

        private void createDilationSlider(Container pane) {
            JPanel sliderPane = new JPanel();
            sliderPane.setLayout(new BoxLayout(sliderPane, BoxLayout.Y_AXIS));
            sliderPane.add(new JLabel(String.format("Dilation x %d", 6)));

            JSlider slider2 = new JSlider(0, 6, 2);
            slider2.setMajorTickSpacing(1);
            slider2.setMinorTickSpacing(1);
            slider2.setPaintTicks(true);
            slider2.setPaintLabels(true);
            slider2.addChangeListener(new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    JSlider source = (JSlider) e.getSource();
                    dilation_size = source.getValue();
                    updateErosion();
                }
            });
            sliderPane.add(slider2);
            pane.add(sliderPane, BorderLayout.CENTER);
            pane.revalidate();
        }

        private void updateErosion() {
            update();

            if (erosion_size >= 1) {
                Mat element = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(erosion_size, erosion_size));
                Imgproc.erode(matImgDst, matImgDst, element);
            }
            if (dilation_size >= 1) {
                Mat element1 = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(dilation_size, dilation_size));
                Imgproc.dilate(matImgDst, matImgDst, element1);
            }
            Image img = HighGui.toBufferedImage(matImgDst);
            imgLabel.setIcon(new ImageIcon(img));
            frame.repaint();

        }

        private void update() {
            final Mat hsvMat = new Mat();
            // originalImage.copyTo(hsvMat);
            matImgDst = new Mat();
            // convert mat to HSV format for Core.inRange()
            Imgproc.cvtColor(matImgSrc1, hsvMat, Imgproc.COLOR_BGR2HSV);

            //Scalar lowerWhite = new Scalar(0, 0, 200);
            Scalar lowerWhite = new Scalar(slideBarValues[0], slideBarValues[1], slideBarValues[2]);
            //Scalar upperWhite = new Scalar(145, 60, 255);
            Scalar upperWhite = new Scalar(slideBarValues[3], slideBarValues[4], slideBarValues[5]);
            System.out.println(slideBarValues[0] + "_" + slideBarValues[1] + "_" + slideBarValues[2] + "_" + slideBarValues[3] + "_" + slideBarValues[4] + "_" + slideBarValues[5]);

            Mat mask = new Mat();
            Core.inRange(hsvMat, lowerWhite, upperWhite, mask);
            Core.bitwise_and(matImgSrc1, matImgSrc1, matImgDst, mask);

            Image img = HighGui.toBufferedImage(matImgDst);
            imgLabel.setIcon(new ImageIcon(img));
            frame.repaint();
        }

    }
}