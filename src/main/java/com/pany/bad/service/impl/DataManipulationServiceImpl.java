package com.pany.bad.service.impl;

import com.pany.bad.service.DataManipulationService;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.highgui.HighGui;
import org.opencv.imgcodecs.Imgcodecs;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
public class DataManipulationServiceImpl implements DataManipulationService {

    public Mat LoadImageFromFile(String imagePath) {
        return Imgcodecs.imread(imagePath);
    }

    public void saveImageToFile(String outFile, Mat imageToSave) {
        // Save the visualized detection.
        System.out.println("Writing " + outFile);
        Imgcodecs.imwrite(outFile, imageToSave);
    }

    public BufferedImage matToBufferedImage(Mat matrix) throws IOException {
        MatOfByte mob = new MatOfByte();
        Imgcodecs.imencode(".jpg", matrix, mob);
        byte[] ba = mob.toArray();

        BufferedImage bi = ImageIO.read(new ByteArrayInputStream(ba));
        return bi;
    }

    public List<Mat> splitImageToMultiImage(Mat LotsOfTemplates, Pair<Integer, Integer> templateSize, Pair<Integer, Integer> structure) {
        List<Mat> splattedResults = new ArrayList<>();
        for (int x = 0; x < structure.getKey(); x++) {
            for (int y = 0; y < structure.getValue(); y++) {
                Mat template = LotsOfTemplates.submat(y * templateSize.getValue(), y * templateSize.getValue() + templateSize.getValue(),
                        x * templateSize.getKey(), x * templateSize.getKey() + templateSize.getKey()
                );

                splattedResults.add(template);
                //showWaitDestroy("splat result ", template);
            }
        }
        return splattedResults;
    }

    public void show(String winname, Mat img) {
        HighGui.imshow(winname, img);
        HighGui.moveWindow(winname, 500, 50);
        HighGui.waitKey(500);
        HighGui.destroyWindow(winname);
    }

    public void showWaitDestroy(String winname, Mat img) {
        HighGui.imshow(winname, img);
        HighGui.moveWindow(winname, 500, 50);
        HighGui.waitKey(0);
        HighGui.destroyWindow(winname);
    }

    /*  public Mat dataBufferIntToMat( BufferedImage source) {

          BufferedImage b = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
          Graphics g = b.getGraphics();
          g.drawImage(source, 0, 0, null);
          g.dispose();

          ColorModel cm = source.getColorModel();
          boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
          WritableRaster raster = source.copyData(null);
           new BufferedImage(cm, raster, isAlphaPremultiplied, null);



          // Convert INT to BYTE
          BufferedImage im = new BufferedImage(intImag.getWidth(), intImag.getHeight(),BufferedImage.TYPE_3BYTE_BGR);

          // Convert bufferedimage to byte array
          byte[] pixels = ((DataBufferByte) im.getRaster().getDataBuffer()).getData();

          // Create a Matrix the same size of image
          Mat image = new Mat(im.getHeight(), im.getWidth(), CvType.CV_8UC3);
          // Fill Matrix with image values
          image.put(0, 0, pixels);

          return  image;
      }

      */
    public Mat dataBufferIntToMat(BufferedImage imgBuffer) throws IOException {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(imgBuffer, "jpg", byteArrayOutputStream);
        byteArrayOutputStream.flush();
        return Imgcodecs.imdecode(new MatOfByte(byteArrayOutputStream.toByteArray()), Imgcodecs.IMREAD_UNCHANGED);
    }

    @Override
    public String getAbsolutePath(String relativePath) throws URISyntaxException {
        URL res = getClass().getClassLoader().getResource(relativePath);
        File file = Paths.get(res.toURI()).toFile();
        String absolutePath = file.getAbsolutePath();
        return absolutePath;
    }

    public Mat dataBufferByteToMat(BufferedImage bi) {
        Mat mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC3);
        byte[] data = ((DataBufferByte) bi.getRaster().getDataBuffer()).getData();
        mat.put(0, 0, data);
        return mat;
    }
}
