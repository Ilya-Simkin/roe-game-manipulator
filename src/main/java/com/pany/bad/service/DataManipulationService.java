package com.pany.bad.service;


import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Mat;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

public interface DataManipulationService {

    Mat LoadImageFromFile(String imagePath);

    void saveImageToFile(String outFile, Mat imageToSave);

    BufferedImage matToBufferedImage(Mat matrix) throws IOException;

    Mat dataBufferByteToMat(BufferedImage bi);

    Mat dataBufferIntToMat( BufferedImage  bi) throws IOException;

    String getAbsolutePath(String relativePath) throws URISyntaxException;

    List<Mat> splitImageToMultiImage(Mat LotsOfTemplates, Pair<Integer, Integer> templateSize, Pair<Integer, Integer> structure);

    void showWaitDestroy(String winname, Mat img);

}
