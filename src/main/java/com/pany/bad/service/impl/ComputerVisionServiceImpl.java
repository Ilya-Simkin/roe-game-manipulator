package com.pany.bad.service.impl;

import com.pany.bad.service.ComputerVisionService;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.MutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.opencv.core.*;
import org.opencv.core.Point;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.awt.*;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.opencv.imgproc.Imgproc.MORPH_RECT;

@Service
public class ComputerVisionServiceImpl implements ComputerVisionService {

    @Autowired
    private DataManipulationServiceImpl dataManipulationService;

    @Value("${search.template.path}")
    private String templateFilePath;

    @Value("${search.mask.path}")
    private String maskFilePath;

    @Value("${search.results.limit:60}")
    private int resultsLimit;

    @Value("${search.threshold.size:0.94}")
    private double threshold;

    @Value("${search.filter.threshold.size:0.94}")
    private double filterThreshold;

    private Mat searchMask = null;

    private Mat searchTemplate = null;

    public Mat matchOne(Mat sourceImage, Mat templateToSearch, Mat templateMask) {
        int match_method = Imgproc.TM_CCOEFF;


        int result_cols = sourceImage.cols() - templateToSearch.cols() + 1;
        int result_rows = sourceImage.rows() - templateToSearch.rows() + 1;
        Mat result = new Mat(result_rows, result_cols, CvType.CV_32FC1);

        // / Do the Matching and Normalize
        if (templateMask == null) {
            Imgproc.matchTemplate(sourceImage, templateToSearch, result, match_method);
        } else {
            Imgproc.matchTemplate(sourceImage, templateToSearch, result, match_method, templateMask);
        }
        Core.normalize(result, result, 0, 1, Core.NORM_MINMAX, -1, new Mat());

        // / Localizing the best match with minMaxLoc
        Core.MinMaxLocResult mmr = Core.minMaxLoc(result);

        Point matchLoc;
        if (match_method == Imgproc.TM_SQDIFF
                || match_method == Imgproc.TM_SQDIFF_NORMED) {
            matchLoc = mmr.minLoc;
        } else {
            matchLoc = mmr.maxLoc;
        }

        // / Show me what you got
        Imgproc.rectangle(sourceImage, matchLoc, new Point(matchLoc.x + templateToSearch.cols(),
                matchLoc.y + templateToSearch.rows()), new Scalar(0, 255, 0));

        return sourceImage;
    }

    public Mat matchOne(Mat sourceImage, Mat templateToSearch) {
        return matchOne(sourceImage, templateToSearch, null);
    }

    public Mat matchMultiple(Mat sourceImage, Mat templateToSearch, double trash1, double trash2) {
        return matchMultiple(sourceImage, templateToSearch, null, trash1, trash2);
    }

    public Mat matchMultiple(Mat sourceImage, Mat templateToSearch, Mat templateMask, double trash1, double trash2) {
        //Matching method
        int match_method = Imgproc.TM_CCORR_NORMED;//Imgproc.TM_SQDIFF_NORMED;//Imgproc.TM_CCOEFF_NORMED; //Imgproc.TM_CCORR_NORMED; //

        Mat mWhere = new Mat();
        Imgproc.cvtColor(sourceImage, mWhere, Imgproc.COLOR_BGR2GRAY);

        Mat templ = new Mat();
        Mat mask = new Mat();
        Imgproc.cvtColor(sourceImage, mWhere, Imgproc.COLOR_RGB2GRAY);
        Imgproc.cvtColor(templateToSearch, templ, Imgproc.COLOR_RGB2GRAY);
        if (templateMask != null) {
            Imgproc.cvtColor(templateMask, mask, Imgproc.COLOR_RGB2GRAY);
            //Imgproc.threshold(mask, mask, 150, 255, Imgproc.THRESH_BINARY);

        }
        //prepare skelton for resultant image
        int result_cols = mWhere.cols() - templ.cols() + 1;
        int result_rows = mWhere.rows() - templ.rows() + 1;
        //Mat result = new Mat(result_rows, result_cols, CvType.CV_32FC1);
        Mat result = new Mat(result_rows, result_cols, CvType.CV_32FC1);
        Mat resultThresh = new Mat(result_rows, result_cols, CvType.CV_32FC1);
        // do the matching
        if (templateMask == null) {
            Imgproc.matchTemplate(mWhere, templ, result, match_method);
        } else {
            Imgproc.matchTemplate(mWhere, templ, result, match_method, mask);
        }
        //Core.normalize(result, result, 0.0, 1.0, Core.NORM_MINMAX);
        // use threshold to restrict the number of results, mine is very poor just .3
        Mat temp = new Mat();
        Core.normalize(result, temp, 0, 255, Core.NORM_MINMAX, CvType.CV_32F);
        Imgcodecs.imwrite("C:\\gitRepository\\my-projects\\coe-map-scanner\\tempfile.jpg", temp);

        Imgproc.threshold(result, resultThresh, trash1, 1.0, Imgproc.THRESH_TOZERO);


        Core.normalize(resultThresh, temp, 0, 255, Core.NORM_MINMAX, CvType.CV_32F);
        Imgcodecs.imwrite("C:\\gitRepository\\my-projects\\coe-map-scanner\\tempfile.jpg", temp);
        Point matchLoc;
        Point maxLoc;
        Point minLoc;

        Core.MinMaxLocResult mmr;
        List<Point> listOfResults = new LinkedList<>();
        //Iterate through all results
        while (true) {
            mmr = Core.minMaxLoc(resultThresh);
            matchLoc = mmr.maxLoc;
            if (mmr.maxVal >= trash2) {
                //Drawing the rectangle in the resultant image
                Point point = new Point(matchLoc.x + templ.cols(), matchLoc.y + templ.rows());
                Imgproc.rectangle(sourceImage, matchLoc, point, new Scalar(0, 0, 255), 3);

                //Removing the already drawn result using that -1
                Imgproc.rectangle(resultThresh, matchLoc, point, new Scalar(0, 0, 0), -1);
                Imgproc.rectangle(resultThresh, matchLoc, point, new Scalar(0, 0, 0), 3);

                Core.normalize(resultThresh, temp, 0, 255, Core.NORM_MINMAX, CvType.CV_8U);
                Imgcodecs.imwrite("C:\\gitRepository\\my-projects\\coe-map-scanner\\tempfile.jpg", temp);
                listOfResults.add(point);
                //break;
            } else {
                break; //No more results within tolerance, break search
            }
        }
        return sourceImage;
    }

    public Mat matchMultipleWithListOfTemplates(Mat sourceImage, List<Mat> templatesToSearch, List<Mat> templatesMask, double trash1, double trash2) {
        //Matching method
        int match_method = Imgproc.TM_CCORR_NORMED;

        Mat monoSourceImage = new Mat();
        Imgproc.cvtColor(sourceImage, monoSourceImage, Imgproc.COLOR_BGR2GRAY);
        List<Mat> monoTemplateToSearch = new ArrayList<>();
        if (!CollectionUtils.isEmpty(templatesToSearch)) {
            templatesToSearch.forEach(template -> {
                Mat monoTemplate = new Mat();
                Imgproc.cvtColor(template, monoTemplate, Imgproc.COLOR_RGB2GRAY);
                monoTemplateToSearch.add(monoTemplate);
            });
        }
        List<Mat> monoTemplateMask = new ArrayList<>();
        if (!CollectionUtils.isEmpty(templatesMask)) {
            templatesMask.forEach(mask -> {
                Mat monoMask = new Mat();
                Imgproc.cvtColor(mask, monoMask, Imgproc.COLOR_RGB2GRAY);
                monoTemplateMask.add(monoMask);
            });
        }

        int result_cols = monoSourceImage.cols() - monoTemplateToSearch.get(0).cols() + 1;
        int result_rows = monoSourceImage.rows() - monoTemplateToSearch.get(0).rows() + 1;
        Mat result = new Mat(result_rows, result_cols, CvType.CV_32FC1);
        Mat resultThresh = new Mat(result_rows, result_cols, CvType.CV_32FC1);

        Map<Integer, List<Pair<Point, Point>>> mapOfResults = new HashMap<>();

        for (int i = 0; i < monoTemplateToSearch.size(); i++) {

            // do the matching
            if (CollectionUtils.isEmpty(monoTemplateMask)) {
                Imgproc.matchTemplate(monoSourceImage, monoTemplateToSearch.get(i), result, match_method);
            } else {
                Imgproc.matchTemplate(monoSourceImage, monoTemplateToSearch.get(i), result, match_method, monoTemplateMask.get(i));
            }
            // use threshold to restrict the number of results, mine is very poor just .3
            Mat temp = new Mat();
            Core.normalize(result, temp, 0, 255, Core.NORM_MINMAX, CvType.CV_32F);
            // temp.convertTo(temp, CvType.CV_8U, 0,255);
            // showWaitDestroy("Normalized", temp);
            Imgproc.threshold(result, resultThresh, trash1, 1.0, Imgproc.THRESH_TOZERO);
            // resultThresh.convertTo(resultThresh, CvType.CV_8U, 0,255);
            //showWaitDestroy("Threshold ed", resultThresh);

            Point matchLoc;
            Point maxLoc;
            Point minLoc;

            Core.MinMaxLocResult mmr;

            //Iterate through first 15 results
            for (int j = 0; j < 15; j++) {
                mmr = Core.minMaxLoc(resultThresh);
                matchLoc = mmr.maxLoc;
                if (mmr.maxVal >= trash2) {
                    //Drawing the rectangle in the resultant image
                    Point point2 = new Point(matchLoc.x + monoTemplateToSearch.get(i).cols(), matchLoc.y + monoTemplateToSearch.get(i).rows());
                    if (!mapOfResults.containsKey(i)) {
                        mapOfResults.put(i, new ArrayList<>());
                    }
                    mapOfResults.get(i).add(new MutablePair<>(matchLoc, point2));

                    //Removing the already drawn result using that -1
                    Imgproc.rectangle(resultThresh, matchLoc, point2, new Scalar(0, 0, 0), -1);
                    Imgproc.rectangle(resultThresh, matchLoc, point2, new Scalar(0, 0, 0), 3);

                    Core.normalize(resultThresh, temp, 0, 255, Core.NORM_MINMAX, CvType.CV_8U);

                    //break;
                } else {

                    break; //No more results within tolerance, break search
                }
                // showWaitDestroy("resultsMasks-"+i+"_"+j, resultThresh);
            }
        }

        Random random = new Random();

        for (Map.Entry<Integer, List<Pair<Point, Point>>> entry : mapOfResults.entrySet()) {
            int r = random.nextInt(255);
            int b = random.nextInt(255);
            int g = random.nextInt(255);
            List<Pair<Point, Point>> pointsOfGivenTemplate = entry.getValue();
            pointsOfGivenTemplate.forEach(pair -> {
                Imgproc.rectangle(sourceImage, pair.getKey(), pair.getValue(), new Scalar(r, b, g), 3);
                Point textLocation = new Point(pair.getKey().x + (pair.getValue().x) / 2, pair.getKey().y + (pair.getValue().y) / 2);
                Imgproc.putText(sourceImage, "# " + entry.getKey(), textLocation, 1, 1, new Scalar(r, b, g));
            });
        }

        return sourceImage;
    }

    public void showImageAndWait(BufferedImage img) throws IOException {
        Mat capturedImage = dataManipulationService.dataBufferIntToMat(img);
        dataManipulationService.showWaitDestroy("Image", capturedImage);

    }

    @Override
    public List<Point> getPlayerCoordinates(BufferedImage screenCapture) throws IOException, URISyntaxException {
        Mat capturedImage = dataManipulationService.dataBufferIntToMat(screenCapture);
        //Matching method
        int match_method = Imgproc.TM_CCORR_NORMED;
        Mat mWhere = new Mat();
        Imgproc.cvtColor(capturedImage, mWhere, Imgproc.COLOR_BGR2GRAY);

        Mat templ = new Mat();
        Mat mask = new Mat();
        Imgproc.cvtColor(capturedImage, mWhere, Imgproc.COLOR_RGB2GRAY);

        if (searchTemplate == null) {
            String maskAbsolutePath = dataManipulationService.getAbsolutePath(templateFilePath);
            searchTemplate = dataManipulationService.LoadImageFromFile(maskAbsolutePath);
        }

        if (searchMask == null) {
            String maskAbsolutePath = dataManipulationService.getAbsolutePath(maskFilePath);
            searchMask = dataManipulationService.LoadImageFromFile(maskAbsolutePath);
        }

        Imgproc.cvtColor(searchTemplate, templ, Imgproc.COLOR_RGB2GRAY);
        if (searchMask != null) {
            Imgproc.cvtColor(searchMask, mask, Imgproc.COLOR_RGB2GRAY);
            //Imgproc.threshold(mask, mask, 150, 255, Imgproc.THRESH_BINARY);
        }
        //prepare skeleton for resultant image
        int result_cols = mWhere.cols() - templ.cols() + 1;
        int result_rows = mWhere.rows() - templ.rows() + 1;
        //Mat result = new Mat(result_rows, result_cols, CvType.CV_32FC1);
        Mat result = new Mat(result_rows, result_cols, CvType.CV_32FC1);
        Mat resultThresh = new Mat(result_rows, result_cols, CvType.CV_32FC1);
        // do the matching
        if (searchMask == null) {
            Imgproc.matchTemplate(mWhere, templ, result, match_method);
        } else {
            Imgproc.matchTemplate(mWhere, templ, result, match_method, mask);
        }

        // use threshold to restrict the number of results
        Imgproc.threshold(result, resultThresh, threshold, 1.0, Imgproc.THRESH_TOZERO);

        Point matchLoc;

        Core.MinMaxLocResult mmr;
        List<Point> coordinatesPixels = new LinkedList<>();
        //Iterate through all results
        int counter = 0;
        while (counter < resultsLimit) {
            counter++;
            mmr = Core.minMaxLoc(resultThresh);
            matchLoc = mmr.maxLoc;
            if (mmr.maxVal >= filterThreshold) {
                //Drawing the rectangle in the resultant image
                Point point = new Point(matchLoc.x + templ.cols(), matchLoc.y + templ.rows());
                Imgproc.rectangle(capturedImage, matchLoc, point, new Scalar(0, 0, 255), 3);

                //Removing the already drawn result using that -1
                Imgproc.rectangle(resultThresh, matchLoc, point, new Scalar(0, 0, 0), -1);
                Imgproc.rectangle(resultThresh, matchLoc, point, new Scalar(0, 0, 0), 3);

                coordinatesPixels.add(point);
                //break;
            } else {
                break; //No more results within tolerance, break search
            }
        }
        //dataManipulationService.showWaitDestroy("crap", capturedImage);

        return coordinatesPixels;
    }

    private Mat convertToZeroUpperAndLowerEdges(Mat m) {
        Mat result = m.clone();

        for (int i = 0; i < 3; i++) {
            byte[] values = new byte[m.cols()];
            byte[] values2 = new byte[m.cols()];
            for (int j = 0; j < m.cols(); j++) {
                values[j] = (byte) 255;
                values2[j] = (byte) 255;
            }

            result.put(i, 0, values);
            result.put(m.rows() - 1 - i, 0, values2);
        }
        return result;
    }


    private Mat invertAndBlur(Mat srcMat) {
        Mat imgGray = new Mat();
        Mat imageBlackAndWhite = new Mat();
        Mat imgGaussianBlur = new Mat();

        Imgproc.cvtColor(srcMat, imgGray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.threshold(imgGray, imageBlackAndWhite, 0, 120, Imgproc.THRESH_TOZERO);

        Mat invertcolormatrix = new Mat(imageBlackAndWhite.rows(), imageBlackAndWhite.cols(), imageBlackAndWhite.type(), new Scalar(255, 255, 255));
        Core.subtract(invertcolormatrix, imageBlackAndWhite, imageBlackAndWhite);

        Imgproc.GaussianBlur(imageBlackAndWhite, imgGaussianBlur, new Size(0, 0), 0.5);
        return imgGaussianBlur;
    }

    private Mat addBorder(Mat dilatedMat) {
        Mat removedBoarder = new Mat();
        Scalar value = new Scalar(0, 0, 0);
        Core.copyMakeBorder(dilatedMat, removedBoarder, 3, 3, 1, 1, Core.BORDER_CONSTANT, value);
        return removedBoarder;
    }

    public Mat prepareImageForTextDetection(BufferedImage sourceImageBuffered) throws Exception {
        Mat sourceImage = dataManipulationService.dataBufferIntToMat(sourceImageBuffered);
        //we are expecting here a white text so we can help the program and extract only the text our of its background
        Mat whiteExtractedImage = extractWhitePixels(sourceImage);
        //Mat dilatedMat = dilateMat(whiteExtractedImage);
        Mat monoAndBlur = invertAndBlur(whiteExtractedImage);
        Mat result = convertToZeroUpperAndLowerEdges(monoAndBlur);

       // dataManipulationService.show("name", result);
//
        return result;
    }

    private Mat dilateMat(Mat matImgDst) {
        Mat result = new Mat();
        double dilation_size = 2;
        Mat element1 = Imgproc.getStructuringElement(MORPH_RECT, new Size(dilation_size, dilation_size));
        Imgproc.dilate(matImgDst, result, element1);
        return result;
    }

    private Mat extractWhitePixels(Mat originalImage) {
        Mat result = new Mat();
        final Mat hsvMat = new Mat();
        Imgproc.cvtColor(originalImage, hsvMat, Imgproc.COLOR_BGR2HSV);
        //the values here are received by the test class of hsv
        Scalar lowerWhite = new Scalar(0, 0, 200);
        Scalar upperWhite = new Scalar(172, 65, 255);

        Mat mask = new Mat();
        Core.inRange(hsvMat, lowerWhite, upperWhite, mask);
        Core.bitwise_and(originalImage, originalImage, result, mask);

        return result;
    }
}
