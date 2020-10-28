package com.pany.bad.service.impl;

import com.pany.bad.domain.model.Player;
import com.pany.bad.domain.repository.CurrentScanCollection;
import com.pany.bad.domain.repository.FullServerScanCollection;
import com.pany.bad.service.ComputerVisionService;
import com.pany.bad.service.RiseOfEmpiresRobot;
import com.pany.bad.service.TextRecognitionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;


@Slf4j
@Service("RiseOfEmpiresRobotImpl")
public class RiseOfEmpiresRobotImpl implements RiseOfEmpiresRobot {

    @Value("${map.lenght:50}")
    private int mapLength;

    @Value("${map.width:50}")
    private int mapWidth;

    @Value("${map.jump.interval.down.x:19}")
    private int jumpIntervalDownX;

    @Value("${map.jump.interval.down.y:19}")
    private int jumpIntervalDownY;

    @Value("${map.jump.interval.left.x:-7}")
    private int jumpIntervalLeftX;

    @Value("${map.jump.interval.left.y:7}")
    private int jumpIntervalLeftY;

    @Value("${map.jump.interval.right.x:7}")
    private int jumpIntervalRightX;

    @Value("${map.jump.interval.right.y:-7}")
    private int jumpIntervalRightY;

    @Value("${map.transformation.offset.x:17}")
    private double transformationOffsetX;

    @Value("${map.transformation.offset.y:3}")
    private double transformationOffsetY;

    @Value("${map.transformation.tile.size.x:35.0}")
    private double transformationTileSizeX;

    @Value("${map.transformation.tile.size.y:17.5}")
    private double transformationTileSizeY;

    @Value("${map.transformation.start.offset.x:12}")
    private int startOffsetX;

    @Value("${map.transformation.start.offset.y:12}")
    private int startOffsetY;

    @Value("${recognition.delete.from.scan.flag:false}")
    private boolean isDeleteFromScanCollection;

    @Value("${mongo.collection.names.current.scan}")
    private String currentCollectionName;

    @Value("${mongo.collection.names.full.info}")
    private String fullInfoCollection;

    @Value("${mongo.collection.page.size:50}")
    private int pageSize;

    @Value("${initial.retry.boundary:5}")
    private int retryBoundary;

    private final int delay = 120;
    private final int delayRecognition = 150;


    @Autowired
    private ComputerVisionService computerVisionService;

    @Autowired
    private CurrentScanCollection currentScanCollection;

    @Autowired
    private FullServerScanCollection fullServerScanCollection;

    @Autowired
    private TextRecognitionService textRecognitionService;

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
//    @Transactional
    public void detectPlayersCoordinatesRoutine(int state, boolean isNewSwipe, int startX, int startY) throws AWTException, IOException, URISyntaxException {
        log.info("> started player detection on state {}, from coordinates {},{}", state, startX, startY);
        Robot robot = new Robot();
        BufferedImage screenCapture;
        int currentXCenter = startX;
        int currentYCenter = startY;
        robot.delay(5000);
        //in game goes to map and set the optimal zoom out to start player coordinate search
        setSearchZoom(robot);
        //makes sure we are in the right state3
        //remove unneeded map legend elements
        removeElements(robot);
        //removing old temporary records if needed
        if (isNewSwipe) {
            log.debug("> removing all the old data from the temporary collection");
            currentScanCollection.deleteAll();
        }


        //going trough the map
        while (currentXCenter < mapWidth && currentYCenter < mapLength) {

            log.info("> moving to coordinates : {} , {}.", currentXCenter, currentYCenter);

            log.info("> doing left...{} images to scan.", Math.min(Math.ceil(currentXCenter / 7.0), Math.ceil((mapWidth - currentYCenter) / 7.0)));
            int retryCounter = doLeft(robot, state, currentXCenter, currentYCenter);

            if (retryCounter >= (retryBoundary * 3 - retryBoundary) / 3) {
                log.error(">something went terribly wrong , stopping run ");
                BufferedImage bufferedImage = captureGameScreen(robot);
                computerVisionService.showImageAndWait(bufferedImage);
                robot.delay(3 * delay);
            }

            log.info("> doing right...{} images to scan.", Math.min(Math.ceil(currentYCenter / 7.0), Math.ceil((mapLength - currentXCenter) / 7.0)));
            retryCounter = doRight(robot, state, currentXCenter, currentYCenter);
            if (retryCounter >= (retryBoundary * 3 - retryBoundary) / 3) {
                log.error(">something went terribly wrong , stopping run ");
                BufferedImage bufferedImage = captureGameScreen(robot);
                computerVisionService.showImageAndWait(bufferedImage);
                robot.delay(1000);
            }

            currentXCenter = currentXCenter + jumpIntervalDownX;
            currentYCenter = currentYCenter + jumpIntervalDownY;
        }
        log.info("> Finished player detection on state {}, from coordinates {},{}", state, startX, startY);
    }

    @Override
    public void recognizeLatestPlayersNamesRoutineFromImageScreen() throws Exception {
        log.info("> started player data extraction from last temp scan ");
        int pageSize = 100;

        Robot robot = new Robot();

        //retrieve the count of player need recognition
        long countOfElements = currentScanCollection.count();
        //doing a small pause between iterations
        robot.delay(3000);
        for (int i = 0; i < Math.ceil(1.0 * countOfElements / pageSize); i++) {

            //doing a pageable request to the last scan to get a chunk of players
            Page<Player> page = currentScanCollection.findAll(PageRequest.of(i, pageSize));
            //getting the content of this page
            List<Player> usersChunk = page.getContent();
            //zooming in
            setNameDetectionZoom(robot);
            //iterating trough the players retrieved
            for (Player player : usersChunk) {
                //getting the player name as string
                String name = recognizePlayerNameFromImage(robot, player.getState(), player.getXxCoordinate(), player.getYyCoordinate());
                //doing some text woodoo
                Map<String, String> textData = extractStringFeatures(name);

                transformNameAndSave(player, textData, 0);
            }

            if (isDeleteFromScanCollection) {
                List<String> listOfIdsToRemove = usersChunk.stream().map(Player::getId).collect(Collectors.toList());
                currentScanCollection.deleteByIdIn(listOfIdsToRemove);
            }
        }
        log.info("> Finished player data extraction from last temp scan ");
    }

    @Override
    public void recognizeLatestPlayersNamesRoutineByPlayerInfo(int pageToStart, int firstOffset, Integer... instances) throws Exception {

        log.info("> started player data extraction from last temp scan  ");
        int pageSize = 100;

        Robot robot = new Robot();

        //retrieve the count of player need recognition
        long countOfElements = currentScanCollection.count();
        //doing a small pause between iterations
        robot.delay(5000);
        Random ran = new Random();

        for (int pageNum = pageToStart; pageNum < Math.ceil(1.0 * countOfElements / pageSize); pageNum++) {
            log.info("> doing the {} 'th  {} out of {}.", pageNum, pageSize, Math.ceil(1.0 * countOfElements / pageSize));
            //doing a pageable request to the last scan to get a chunk of players
            Page<Player> page = currentScanCollection.findAll(PageRequest.of(pageNum, pageSize, Sort.by("xxCoordinate").and(Sort.by("yyCoordinate"))));
            //getting the content of this page
            List<Player> usersChunk = page.getContent();
            //zooming in
            setNameDetectionZoom(robot);
            //iterating trough the players retrieved
            int nothingFoundCounter = 0;
            int playerNumber = 0;
            int increaser = 0;
            if (pageNum == pageToStart) {
                playerNumber = firstOffset;
            }
            for (; playerNumber < pageSize; playerNumber++) {
                Player player = usersChunk.get(playerNumber);
                //getting the player name as string
                try {
                    StringSelection stringSelection = new StringSelection("");
                    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
                } catch (IllegalStateException ex) {
                    log.error(" cannot open system clipboard");
                    continue;
                }
                String result = "";
                //move to state and coordinates
                robot.delay(2 * delayRecognition);
                setCoordinates(robot, player.getXxCoordinate().intValue(), player.getYyCoordinate().intValue());

                result = getPlayerNameFromInfo(robot);
                robot.delay(delay);
                sendKeys(robot, "m");
                if (StringUtils.isBlank(result)) {
                    nothingFoundCounter++;

                    result = "Undefined";


                } else {
                    nothingFoundCounter = 0;
                    increaser = 0;
                }
                if (nothingFoundCounter == 6 + increaser) {
                    nothingFoundCounter = 0;

                    log.info("> doing retry with random on the last x from {} of the {}'th {}ed .", playerNumber, pageNum, pageSize);
                    robot.delay(50 + delayRecognition);
                    tryExitKeys(robot);

                }
                increaser = +2;
                playerNumber = Math.max(0, playerNumber - 5 - increaser);
                robot.delay(delay);
                setCoordinates(robot, ran.nextInt(mapLength - 10), ran.nextInt(mapWidth - 10));
                robot.delay(20);

                //doing some text woodoo
                Map<String, String> textData = extractStringFeatures(result);
                transformNameAndSave(player, textData, playerNumber);
            }
            if (isDeleteFromScanCollection) {
                List<String> listOfIdsToRemove = usersChunk.stream().map(Player::getId).collect(Collectors.toList());
                currentScanCollection.deleteByIdIn(listOfIdsToRemove);
            }
        }
        log.info("> Finished player data extraction from last temp scan ");

    }

    @Override
    public Integer runAsyncNamesRoutineOnPage(int pageToStart, int firstOffset, int pageNum, int windowIdAssignment) throws AWTException, IOException, UnsupportedFlavorException {
        return null;
    }

    private void transformNameAndSave(Player player, Map<String, String> textData, int f) {
        if (textData.containsKey("playerName")) {

            log.info(">no:{}, detected name: {} ,coords: {}:{}", f, textData.get("fullPlayerName"), player.getXxCoordinate(), player.getYyCoordinate());
            Player playerToInsert = Player.builder()
                    .fullPlayerName(textData.get("fullPlayerName"))
                    .playerName(textData.get("playerName"))
                    .dateOfDetection(player.getDateOfDetection())
                    .dateOfRecognition(LocalDateTime.now())
                    .state(player.getState())
                    .xxCoordinate(player.getXxCoordinate())
                    .yyCoordinate(player.getYyCoordinate())
                    .build();

            if (textData.containsKey("allianceName")) {
                playerToInsert.setAllianceName(textData.get("allianceName"));
            }

            Optional<Player> fromFullData = fullServerScanCollection.findByPlayerNameAndState(textData.get("playerName"), player.getState());
            if (fromFullData.isPresent()) {
                Player playerFromDb = fromFullData.get();
                //will change the save operation to an update
                playerToInsert.setId(playerFromDb.getId());
            }
            mongoTemplate.save(playerToInsert, fullInfoCollection);

        }
    }

    private String getPlayerNameFromInfo(Robot robot) throws UnsupportedFlavorException, IOException {
        int delayRecognition = 190;
        String result = "";
        //open the user info
        robot.delay(3 * delayRecognition);
        sendKeys(robot, "h");
        robot.delay(50 + 3 * delayRecognition);
        sendKeys(robot, "j");
        //copy username from user info
        robot.delay(3 * delayRecognition);
        sendKeys(robot, "k");
        robot.delay(3 * delayRecognition);
        try {
            Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
            if (t != null && t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                result = (String) t.getTransferData(DataFlavor.stringFlavor);
                // log.info(">executor: {} -  found name: {} ", windowID, result);
            }
        } catch (IllegalStateException ex) {
            log.error(" cannot open system clipboard");
            return "";
        }
        return result;
    }

    private Map<String, String> extractStringFeatures(String name) {
        Map<String, String> result = new HashMap<>();
        if (StringUtils.isBlank(name) || name.equals("Undefined")) {
            return result;
        }

        if (name.contains("(")) {
            name = name.substring(name.indexOf("("));
            if (name.length() < 7) {
                return result;
            }
            name = name.substring(0, 4) + ')' + name.substring(5);
        }

        String trimmedName = name.trim();

        while (trimmedName.startsWith("-") || trimmedName.startsWith("_")) {
            trimmedName = trimmedName.substring(1).trim();
        }
        if (name.trim().length() < 3) {
            return result;
        }


        result.put("fullPlayerName", trimmedName);
        //either the name is with (cln)name or just name
        if (trimmedName.startsWith("(") && trimmedName.length() > 4 && trimmedName.charAt(4) == ')') {
            result.put("allianceName", trimmedName.substring(1, 4));
            result.put("playerName", trimmedName.substring(5));
        } else {
            result.put("playerName", trimmedName);
        }
        return result;
    }

    private String recognizePlayerNameFromImage(Robot robot, Long state, Long xxCoordinate, Long yyCoordinate) throws Exception {
        //move to state and coordinates
        setStateAndCoordinates(robot, state.intValue(), xxCoordinate.intValue(), yyCoordinate.intValue());
        robot.delay(600);
        //cut the name out of the image
        BufferedImage textArea = capturePlayerNameFromScreen(robot);
        //doing image manipulation to make text recognition easier
        Mat readyForRecognitionMat = computerVisionService.prepareImageForTextDetection(textArea);
        //the actual text recognition process
        String resultOfTextRecognition = textRecognitionService.recognizeTextInImage(readyForRecognitionMat);

        return resultOfTextRecognition;
    }

    private BufferedImage capturePlayerNameFromScreen(Robot robot) {
        robot.delay(350);
        //Rectangle rect = new Rectangle(814, 186, 170, 25);
        Rectangle rect = new Rectangle(865, 542, 190, 30);
        return robot.createScreenCapture(rect);
    }


    private void setNameDetectionZoom(Robot robot) {
        robot.delay(160);
        for (int i = 0; i < 5; i++) {
            robot.keyPress(17);
            robot.delay(60);
            robot.keyPress(107);
            robot.delay(200);
            robot.keyRelease(107);
            robot.delay(60);
            robot.keyRelease(17);
            robot.delay(60);
        }
    }

    private int doRight(Robot robot, int state, int currentXCenter, int currentYCenter) throws IOException, URISyntaxException {
        int nothingFoundBoundary = retryBoundary;
        int retryCounter = 0;
        int nothingFoundCounter = 0;

        int movingX = currentXCenter;
        int movingY = currentYCenter;
        //so we don't do the center twice
        movingX = movingX + jumpIntervalRightX;
        movingY = movingY + jumpIntervalRightY;

        while (movingY >= 0 && movingX <= mapWidth) {
            int foundCount = detectPlayersOnMapRoutine(robot, state, movingX, movingY);
            if (foundCount == 0) {
                nothingFoundCounter++;
                if (nothingFoundCounter == retryBoundary * 3) {
                    return retryCounter;
                }
                if (nothingFoundCounter >= nothingFoundBoundary) {

                    robot.delay(delay);
                    tryExitKeys(robot);
                    robot.delay(2 * delay);
                    setSearchZoom(robot);

                    movingX = movingX - nothingFoundBoundary * jumpIntervalLeftX;
                    movingY = movingY - nothingFoundBoundary * jumpIntervalLeftY;
                    retryCounter++;
                    nothingFoundCounter = 0;
                    log.info("> had to redo last {} turns from Right side with re-zoom on coordinates {} , {} ", nothingFoundBoundary, movingX, movingY);
                }

            } else {
                nothingFoundCounter = 0;
                retryCounter = 0;
                nothingFoundBoundary = retryBoundary;
            }
            movingX = movingX + jumpIntervalRightX;
            movingY = movingY + jumpIntervalRightY;
        }
        return retryCounter;
    }

    private int doLeft(Robot robot, int state, int currentXCenter, int currentYCenter) throws IOException, URISyntaxException {
        int nothingFoundBoundary = retryBoundary;
        int retryCounter = 0;
        int nothingFoundCounter = 0;

        int movingX = currentXCenter;
        int movingY = currentYCenter;

        while (movingX >= 0 && movingY <= mapLength) {
            int foundCount = detectPlayersOnMapRoutine(robot, state, movingX, movingY);
            if (foundCount == 0) {
                nothingFoundCounter++;
                if (nothingFoundCounter == retryBoundary * 3) {
                    return retryCounter;
                }
                if (nothingFoundCounter >= nothingFoundBoundary) {

                    robot.delay(delay);
                    tryExitKeys(robot);
                    robot.delay(2 * delay);
                    setSearchZoom(robot);

                    movingX = movingX - nothingFoundBoundary * jumpIntervalLeftX;
                    movingY = movingY - nothingFoundBoundary * jumpIntervalLeftY;

                    retryCounter++;
                    nothingFoundCounter = 0;
                    log.info("> had to redo last {} turns from left side with re-zoom on coordinates {} , {} ", nothingFoundBoundary, movingX, movingY);

                    nothingFoundBoundary = Math.min(retryBoundary * 3, nothingFoundBoundary + 3);
                }

            } else {
                nothingFoundCounter = 0;
                retryCounter = 0;
                nothingFoundBoundary = retryBoundary;

            }
            movingX = movingX + jumpIntervalLeftX;
            movingY = movingY + jumpIntervalLeftY;
        }
        return retryCounter;
    }

    private int detectPlayersOnMapRoutine(Robot robot, int state, int movingX, int movingY) throws IOException, URISyntaxException {
        //move to next Coordinates
        setCoordinates(robot, movingX, movingY);
        //trigger screen capture
        BufferedImage screenCapture = captureGameScreen(robot);
        //get list of players Coordinates for given screen capture
        List<Point> pixelCoordinates = computerVisionService.getPlayerCoordinates(screenCapture);

        List<Point> relativePlayerCoordinates = transformToGameCoordinates(pixelCoordinates);

        //change the coordinates to absolute
        List<Point> absolutePlayerCoordinates = fixPlayerCoordinates(relativePlayerCoordinates, movingX, movingY);

        //adding the unrecognized players to the unrecognized players temp collection
        if (absolutePlayerCoordinates != null) {

            absolutePlayerCoordinates.forEach(point -> addUnrecognizedPlayer(point, (long) state));
        }
        return absolutePlayerCoordinates.size();
    }

    private void tryExitKeys(Robot robot) {
        List<String> mylist = Arrays.asList("n", "m", "l");
        Collections.shuffle(mylist);
        sendKeys(robot, mylist.get(0));
        robot.delay(20);
        sendKeys(robot, mylist.get(1));
        robot.delay(20);

    }

    private List<Point> transformToGameCoordinates(List<Point> pixelCoordinates) {
        List<Point> transformedPoints = pixelCoordinates.stream().map(
                point -> {
                    double x = ((point.x - transformationOffsetX) / transformationTileSizeX) - 8;
                    double y = (point.y - transformationOffsetY) / transformationTileSizeY;

                    x = Math.round(x);
                    y = Math.round(y);

                    double transformedX = (x + y) / 2;
                    double transformedY = (y - x) / 2;

                    transformedX--;
                    transformedY--;

                    return new Point(transformedX, transformedY);
                }
        ).collect(Collectors.toList());
        log.debug("transformed {} points", transformedPoints.size());
        return transformedPoints;
    }

    private void removeElements(Robot robot) {
        robot.delay(2 * delay);
        //open menu
//        robot.mouseMove(705, 705);
//        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
//        robot.delay(30);
//        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        robot.keyPress(90);
        robot.delay(30);
        robot.keyRelease(90);
        robot.delay(30);
        //cancel slots
        robot.delay(60);
        robot.keyPress(88);
        robot.delay(30);
        robot.keyRelease(88);
        robot.delay(30);
//        robot.mouseMove(715, 530);
//        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
//        robot.delay(30);
//        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        //cancel monsters
        robot.delay(60);
        robot.keyPress(67);
        robot.delay(30);
        robot.keyRelease(67);
        robot.delay(30);
//        robot.mouseMove(715, 570);
//        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
//        robot.delay(30);
//        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        //cancel farms
        robot.delay(60);
        robot.keyPress(86);
        robot.delay(30);
        robot.keyRelease(86);
        robot.delay(30);
//        robot.mouseMove(715, 650);
//        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
//        robot.delay(30);
//        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        //close menu
        robot.delay(60);
        robot.keyPress(90);
        robot.delay(30);
        robot.keyRelease(90);
        robot.delay(30);
//        robot.mouseMove(705, 705);
//        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
//        robot.delay(30);
//        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }

    private void setCoordinates(Robot robot, int currentX, int currentY) {
    /*    //open menu
//        robot.delay(200);
//        robot.mouseMove(900, 840);
//        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
//        robot.delay(150);
//        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        robot.delay(2*delay);

        sendKeys(robot, "y");
        //set start coordinates of x
        robot.delay(2*delay); //waiting the window to open

//        robot.mouseMove(850, 570);
//        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
//        robot.delay(150);
//        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        sendKeys(robot,  "u");
        robot.delay(delay);
        sendKeys(robot, currentX + "");
        //press enter
        robot.delay(50);
        robot.keyPress(java.awt.event.KeyEvent.VK_ENTER);
        robot.delay(30);
        robot.keyRelease(java.awt.event.KeyEvent.VK_ENTER);
        robot.delay(30);

        //set start coordinates of y
        robot.delay(delay);
        sendKeys(robot, "i");
//        robot.mouseMove(1050, 570);
//        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
//        robot.delay(150);
//        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
//        robot.delay(150);
//        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
//        robot.delay(150);
//        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
//        robot.delay(150);
        robot.delay(delay);
        sendKeys(robot, currentY + "");

        //press enter
        robot.delay(50);
        robot.keyPress(java.awt.event.KeyEvent.VK_ENTER);
        robot.delay(30);
        robot.keyRelease(java.awt.event.KeyEvent.VK_ENTER);
        robot.delay(30);

        //click closing click
        robot.delay(2*delay);
        sendKeys(robot, "o");
//        robot.delay(400);
//        robot.mouseMove(975, 630);
//        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
//        robot.delay(150);
//        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
//        robot.delay(150);
//        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
//        robot.delay(150);
//        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);*/


        sendKeys(robot, "y");
        robot.delay(2 * delay); //waiting the window to open
        sendKeys(robot, "u");
        robot.delay(80);
        sendKeys(robot, currentX + "");
        //press enter
        robot.delay(50);
        robot.keyPress(KeyEvent.VK_ENTER);
        robot.delay(30);
        robot.keyRelease(KeyEvent.VK_ENTER);
        robot.delay(30);
        //set coordinates of y
        sendKeys(robot, "i");
        robot.delay(delay);
        sendKeys(robot, currentY + "");
        //press enter
        robot.delay(50);
        robot.keyPress(KeyEvent.VK_ENTER);
        robot.delay(30);
        robot.keyRelease(KeyEvent.VK_ENTER);
        robot.delay(30);
        //click end click
        robot.delay(30);
        sendKeys(robot, "o");

    }

    private void addUnrecognizedPlayer(Point point, Long state) {
        Long xCoord = Math.round(point.x);
        Long yCoord = Math.round(point.y);
        if (!currentScanCollection.existsByStateAndXxCoordinateAndYyCoordinate(state, xCoord, yCoord)) {
            String tempName = "Unknown_" + state + "_" + xCoord + "-" + yCoord;
            Player toAdd = Player.builder()
                    .fullPlayerName(tempName)
                    .playerName(tempName)
                    .dateOfDetection(LocalDateTime.now())
                    .state(state)
                    .xxCoordinate(xCoord)
                    .yyCoordinate(yCoord)
                    .build();
            mongoTemplate.save(toAdd, currentCollectionName);
        }
    }

    private List<Point> fixPlayerCoordinates(List<Point> relativePlayerCoordinates, int currentX, int currentY) {

        return relativePlayerCoordinates.stream().map(point -> {
            double absX = point.x + currentX - startOffsetX;
            double absY = point.y + currentY - startOffsetY;
            return new Point(absX, absY);
        }).collect(Collectors.toList());
    }

    private BufferedImage captureGameScreen(Robot robot) throws IOException {
        robot.delay(2 * delay);
        Rectangle rect = new Rectangle(680, 105, 560, 735);
        return robot.createScreenCapture(rect);
    }

    private void setStateAndCoordinates(Robot robot, int state, int startX, int startY) {
        //open menu
        robot.delay(300);
        robot.mouseMove(900, 840);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.delay(100);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        //click province window
        robot.delay(150);
        robot.mouseMove(850, 505);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.delay(40);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        //set state number
        robot.delay(300);
        sendKeys(robot, state + "");
        //set start coordinates of x
        robot.delay(120);
        robot.mouseMove(850, 570);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.delay(40);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        robot.delay(70);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.delay(40);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        robot.delay(200);
        sendKeys(robot, startX + "");
        //set start coordinates of y
        robot.delay(60);
        robot.mouseMove(1050, 570);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.delay(30);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        robot.delay(70);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.delay(40);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        robot.delay(200);
        sendKeys(robot, startY + "");
        //click end click
        robot.delay(400);
        robot.mouseMove(975, 630);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.delay(50);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        robot.delay(50);
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.delay(50);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);

    }

    private void setSearchZoom(Robot robot) {
        robot.delay(160);
        for (int i = 0; i < 7; i++) {
            robot.keyPress(17);
            robot.delay(60);
            robot.keyPress(107);
            robot.delay(200);
            robot.keyRelease(107);
            robot.delay(60);
            robot.keyRelease(17);
            robot.delay(60);
        }
        for (int i = 0; i < 4; i++) {
            robot.keyPress(17);
            robot.delay(70);
            robot.keyPress(109);
            robot.delay(200);
            robot.keyRelease(109);
            robot.delay(60);
            robot.keyRelease(17);
            robot.delay(60);
        }
        robot.delay(300);

    }

    private void sendKeys(Robot robot, String keys) {
        robot.delay(50);
        for (char c : keys.toCharArray()) {
            int keyCode = KeyEvent.getExtendedKeyCodeForChar(c);
            if (KeyEvent.CHAR_UNDEFINED == keyCode) {
                throw new RuntimeException(
                        "Key code not found for character '" + c + "'");
            }

            robot.keyPress(keyCode);
            robot.delay(30);
            robot.keyRelease(keyCode);
            robot.delay(30);
        }
    }

}
