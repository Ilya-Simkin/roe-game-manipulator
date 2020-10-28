package com.pany.bad.service.impl;

import com.pany.bad.service.DataManipulationService;
import com.pany.bad.service.TextRecognitionService;
import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.commons.lang3.StringUtils;
import org.opencv.core.Mat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;

@Service
public class TextRecognitionServiceImpl implements TextRecognitionService {

    @Autowired
    private DataManipulationService dataManipulationService;

    public String recognizeTextInImage(Mat img) throws Exception {

        ITesseract tesseract = new Tesseract();
        tesseract.setTessVariable("user_defined_dpi", "70");
        tesseract.setLanguage("eng+rus");//chi_sim+ara
        String blackList = "”“,‚\\{°»‹、。^0.|\r;‘‘#\n!:`¢'\"?][}";
        tesseract.setTessVariable("tessedit_char_blacklist",blackList );
        tesseract.setDatapath("src/main/resources/tesseractData");

        BufferedImage imageFile = dataManipulationService.matToBufferedImage(img);
        try {
            String result = tesseract.doOCR(imageFile);
            result = StringUtils.trim(result);
            if (StringUtils.isEmpty(result)) {
                //System.out.println("name Detected: Undefined");
                return "Undefined";
            }
            //System.out.println("name Detected: " + result);
            return result;
        } catch (TesseractException e) {
            System.err.println(e.getMessage());
            return "Undefined";
        }
    }


}
