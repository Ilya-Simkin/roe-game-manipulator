package com.pany.bad.service;

import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.Mat;
import org.opencv.core.Point;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

public interface ComputerVisionService {

    Mat matchOne(Mat sourceImage, Mat templateToSearch,Mat mask) ;
    Mat matchOne(Mat sourceImage, Mat templateToSearch) ;
    Mat matchMultiple(Mat sourceImage, Mat templateToSearch,Mat mask, double trash1, double trash2);
    Mat matchMultiple(Mat sourceImage, Mat templateToSearch, double trash1, double trash2);

    void showImageAndWait(BufferedImage img) throws IOException;

    Mat matchMultipleWithListOfTemplates(Mat sourceImage, List<Mat> templatesToSearch, List<Mat> templatesMask, double trash1, double trash2);

    Mat prepareImageForTextDetection(BufferedImage sourceImage) throws Exception;

    List<Point> getPlayerCoordinates(BufferedImage screenCapture) throws IOException, URISyntaxException;
}