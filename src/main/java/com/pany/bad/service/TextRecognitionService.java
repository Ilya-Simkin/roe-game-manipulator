package com.pany.bad.service;

import org.opencv.core.Mat;

public interface TextRecognitionService {

    String recognizeTextInImage(Mat sourceImage) throws Exception;
}
