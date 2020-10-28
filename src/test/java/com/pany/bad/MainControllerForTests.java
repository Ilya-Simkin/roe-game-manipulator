package com.pany.bad;


import com.pany.bad.service.ComputerVisionService;
import com.pany.bad.service.DataManipulationService;
import com.pany.bad.service.RiseOfEmpiresRobot;
import com.pany.bad.web.dto.OneImageDetectRequest;
import org.apache.commons.lang3.tuple.MutablePair;
import org.opencv.core.Mat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

@RestController
public class MainControllerForTests {

    @Autowired
    private DataManipulationService dataManipulationService;

    @Autowired
    @Qualifier("ConcurrentRiseOfEmpiresRobotImpl")
    private RiseOfEmpiresRobot ConcurrentRiseOfEmpiresRobotImpl;

    @Autowired
    @Qualifier("RiseOfEmpiresRobotImpl")
    private RiseOfEmpiresRobot riseOfEmpiresRobotImpl;


    @Autowired
    private ComputerVisionService computerVisionService;


    // @ApiOperation(value = "A method for retrieving all Host-Customer information.")
    @RequestMapping(value = "/matchOne/image", method = RequestMethod.POST)
    public void detectOneInImage(@RequestBody OneImageDetectRequest oneImageDetectRequest) throws IOException {

        Mat imageToLookOn = dataManipulationService.LoadImageFromFile(oneImageDetectRequest.getImagePath());
        Mat template = dataManipulationService.LoadImageFromFile(oneImageDetectRequest.getTemplatePath());
        Mat templateMask = dataManipulationService.LoadImageFromFile(oneImageDetectRequest.getTemplateMaskPath());
        Mat result = computerVisionService.matchOne(imageToLookOn, template, templateMask);

        dataManipulationService.saveImageToFile(oneImageDetectRequest.getResultPath(), result);

    }

    // @ApiOperation(value = "A method for retrieving all Host-Customer information.")
    @RequestMapping(value = "/matchMultiple/image", method = RequestMethod.POST)
    public void detectMultipleInImage(@RequestBody OneImageDetectRequest oneImageDetectRequest) throws IOException {

        Mat imageToLookOn = dataManipulationService.LoadImageFromFile(oneImageDetectRequest.getImagePath());
        Mat template = dataManipulationService.LoadImageFromFile(oneImageDetectRequest.getTemplatePath());
        Mat result;

        if (oneImageDetectRequest.getTemplateMaskPath() != null) {
            Mat templateMask = dataManipulationService.LoadImageFromFile(oneImageDetectRequest.getTemplateMaskPath());
            result = computerVisionService.matchMultiple(imageToLookOn, template, templateMask, oneImageDetectRequest.getTrash1(), oneImageDetectRequest.getTrash2());
        } else {
            result = computerVisionService.matchMultiple(imageToLookOn, template, oneImageDetectRequest.getTrash1(), oneImageDetectRequest.getTrash2());
        }

        dataManipulationService.saveImageToFile(oneImageDetectRequest.getResultPath(), result);
    }

    @RequestMapping(value = "/matchMultipleWithMultiple/image", method = RequestMethod.POST)
    public void detectMultipleInImageByMultiTemplate(@RequestBody OneImageDetectRequest oneImageDetectRequest) throws IOException {
        Mat result;
        Mat imageToLookOn = dataManipulationService.LoadImageFromFile(oneImageDetectRequest.getImagePath());
        Mat multiTemplate = dataManipulationService.LoadImageFromFile(oneImageDetectRequest.getTemplatePath());

        List<Mat> splatTemplates = dataManipulationService.splitImageToMultiImage(multiTemplate, new MutablePair<>(oneImageDetectRequest.getTemplateWidth(), oneImageDetectRequest.getTemplateHeight()),
                new MutablePair<>(6, 2));
        if (oneImageDetectRequest.getTemplateMaskPath() != null) {
            Mat templateMultiMask = dataManipulationService.LoadImageFromFile(oneImageDetectRequest.getTemplateMaskPath());
            List<Mat> splatMasks = dataManipulationService.splitImageToMultiImage(templateMultiMask, new MutablePair<>(oneImageDetectRequest.getTemplateWidth(), oneImageDetectRequest.getTemplateHeight()),
                    new MutablePair<>(6, 2));
            result = computerVisionService.matchMultipleWithListOfTemplates(imageToLookOn, splatTemplates, splatMasks, oneImageDetectRequest.getTrash1(), oneImageDetectRequest.getTrash2());
        } else {
            result = computerVisionService.matchMultipleWithListOfTemplates(imageToLookOn, splatTemplates, null, oneImageDetectRequest.getTrash1(), oneImageDetectRequest.getTrash2());
        }
        dataManipulationService.saveImageToFile(oneImageDetectRequest.getResultPath(), result);
    }


    @RequestMapping(value = "/activateRoutine/test", method = RequestMethod.GET)
    public void scanMapInZoomOut() throws Exception {

        riseOfEmpiresRobotImpl.detectPlayersCoordinatesRoutine(271, false, 0 , 0 );
    }


    @RequestMapping(value = "/activateRoutinePartTwo/test", method = RequestMethod.GET)
    public void detectAndAddUserNames() throws Exception {

        riseOfEmpiresRobotImpl.recognizeLatestPlayersNamesRoutineFromImageScreen();
    }



    @RequestMapping(value = "/activateRoutinePartTwo/test2", method = RequestMethod.GET)
    public void detectAndAddUserNames2() throws Exception {

        ConcurrentRiseOfEmpiresRobotImpl.recognizeLatestPlayersNamesRoutineByPlayerInfo(5,0);
    }


    @RequestMapping(value = "/activateRoutinePartTwo/test3", method = RequestMethod.GET)
    public void detectAndAddUserNames3() throws Exception {

        riseOfEmpiresRobotImpl.recognizeLatestPlayersNamesRoutineByPlayerInfo(0,0);
    }

}