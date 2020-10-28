package com.pany.bad.service.impl;

import com.pany.bad.domain.model.Player;
import com.pany.bad.domain.repository.CurrentScanCollection;
import com.pany.bad.domain.repository.FullServerScanCollection;
import com.pany.bad.service.ComputerVisionService;
import com.pany.bad.service.RiseOfEmpiresRobot;
import com.pany.bad.service.TextRecognitionService;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;


@Slf4j
@Service("ConcurrentRiseOfEmpiresRobotImpl")
public class ConcurrentRiseOfEmpiresRobotImpl implements RiseOfEmpiresRobot {

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

    @Value("${async.pool.num.of.executors:2}")
    private int numOfInstances;


    private final int delay = 155;
    private final int delayRecognition = 185;

    private final static Object lock = new Object();

    private static int focusId = 0;

    private final Date[] timers = new Date[2];

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

    @Lazy
    @Resource
    @Qualifier("ConcurrentRiseOfEmpiresRobotImpl")
    private RiseOfEmpiresRobot selfReference;


    @Override
    public void detectPlayersCoordinatesRoutine(int state, boolean isNewSwipe, int startX, int startY) throws AWTException, IOException, URISyntaxException {
        throw new NotImplementedException();
    }

    @Override
    public void recognizeLatestPlayersNamesRoutineFromImageScreen() throws Exception {
        throw new NotImplementedException();
    }


    @Override
    public void recognizeLatestPlayersNamesRoutineByPlayerInfo(int pageToStart , int firstOffset, Integer ... instances) throws Exception {
        log.info("> started player data extraction from last temp scan  ");

        Robot robot = new Robot();

        if(instances != null &&   instances[0] != null ){
            numOfInstances =  instances[0];
        }

        //retrieve the count of player need recognition
        long countOfElements = currentScanCollection.count();
        //doing a small pause before starting
        robot.delay(5000);
        int totalPages = (int) (Math.ceil(1.0 * countOfElements / pageSize));

        for (int pageNum = pageToStart; pageNum < totalPages; pageNum = pageNum + numOfInstances) {
            log.info("> doing the {} 'th  {} out of {}.", pageNum, pageSize, Math.ceil(1.0 * countOfElements / pageSize));
            int sizeOfPool = Math.min(numOfInstances, totalPages - pageNum);
            CompletableFuture<Integer>[] synchronizers = new CompletableFuture[sizeOfPool];
            //kick off x threads each time
            for (int j = 0; j < sizeOfPool; j++) {
                int finalPageNum = pageNum;
                int finalJ = j;
                synchronizers[j] = CompletableFuture.supplyAsync(() -> {
                    try {
                        return selfReference.runAsyncNamesRoutineOnPage( pageToStart, firstOffset,finalPageNum + finalJ, finalJ);
                    } catch (AWTException | UnsupportedFlavorException | IOException e) {
                        log.error("recived and exception.");
                        return -1;
                    }
                });
            }

            // wait for all threads
            CompletableFuture.allOf(synchronizers).join();
        }
        log.info("> Finished player data extraction from last temp scan ");
    }

    public Integer runAsyncNamesRoutineOnPage(int pageToStart, int firstOffset, int pageNum, int windowId) throws AWTException, IOException, UnsupportedFlavorException {
        Robot robot = new Robot();
        Random ran = new Random();

        //doing a pageable request to the last scan to get a chunk of players
        Page<Player> page = currentScanCollection.findAll(PageRequest.of(pageNum, pageSize, Sort.by("dateOfDetection")));// Sort.by("xxCoordinate").and(Sort.by("yyCoordinate"))));
        //a small brake in the beginning of the page
        robot.delay(3000);
        //getting the content of this page
        List<Player> usersChunk = page.getContent();
        //resetting clock
        timers[windowId] = new Date();
        //zooming in
        setNameDetectionZoomConcurrent(robot, windowId);


        //iterating trough the players retrieved
        int nothingFoundCounter = 0;
        int increaser = 0;
        int playerNumber = 0;
        if (pageNum == pageToStart) {
            playerNumber = firstOffset;
        }

        for (; playerNumber < pageSize; playerNumber++) {
            Player player = usersChunk.get(playerNumber);
            //getting the player name as string
            String result = "";

            //move to state and coordinatesy
            robot.delay(Math.max(1,  100+delayRecognition - (int) (new Date().getTime() - timers[windowId].getTime())));
           // log.info(">{} -  searching cords: {} , {}",windowId,player.getXxCoordinate().intValue(),player.getYyCoordinate().intValue());

            setCoordinatesConcurrent(robot, windowId, player.getXxCoordinate().intValue(), player.getYyCoordinate().intValue());
            //copy the player name out of menu

            result = getPlayerNameFromInfoConcurrent(robot, windowId);

            robot.delay(Math.max(1,  delay - (int) (new Date().getTime() - timers[windowId].getTime())));
            synchronized (lock) {
                checkAndSwitchWindow(robot, windowId);
                sendKeys(robot, "m");
                timers[windowId] = new Date();
            }
            if (StringUtils.isBlank(result)) {
                //try again
                //log.info("> would have retry again on executor : {}", windowId);
//            result = getPlayerNameFromInfoConcurrent(robot, windowID);
//            robot.delay(Math.max(1, delay - (int) (new Date().getTime() - timers[windowID].getTime())));
//            sendKeys(robot, "l");
//
//                if (StringUtils.isBlank(result)) {
//                    robot.delay(Math.max(1,  delay - (int) (new Date().getTime() - timers[windowId].getTime())));
//                    synchronized (lock) {
//                        checkAndSwitchWindow(robot, windowId);
//                        sendKeys(robot, "l");
//                        timers[windowId] = new Date();
//                    }
                    result = "Undefined";

            }

            if (result.equals("Undefined")) {
                nothingFoundCounter++;
            } else {
                nothingFoundCounter = 0;
                increaser = 0 ;
            }

            if (nothingFoundCounter == 6+increaser) {
                nothingFoundCounter = 0;


                log.info("> executor {} doing retry with random on the last 3 {} of the {}'th {}ed .",windowId,playerNumber, pageNum, pageSize);
                robot.delay(Math.max(1,  50+delayRecognition - (int) (new Date().getTime() - timers[windowId].getTime())));
                synchronized (lock) {
                    checkAndSwitchWindow(robot, windowId);
                    tryExitKeys(robot);

                    timers[windowId] = new Date();
                }
                increaser =+2 ;
                playerNumber = Math.max(0,playerNumber - 5 - increaser);
                robot.delay(Math.max(1,  delay - (int) (new Date().getTime() - timers[windowId].getTime())));
                setCoordinatesConcurrent(robot, windowId, ran.nextInt(mapLength - 10), ran.nextInt(mapWidth - 10));
                robot.delay(20);

            }
            //doing some text woodoo
            Map<String, String> textData = extractStringFeatures(result);
            transformNameAndSave(player, textData,playerNumber,windowId);
        }

        if (isDeleteFromScanCollection) {
            List<String> listOfIdsToRemove = usersChunk.stream().map(Player::getId).collect(Collectors.toList());
            currentScanCollection.deleteByIdIn(listOfIdsToRemove);
        }
        return windowId;
    }

    private void tryExitKeys(Robot robot) {
        List<String> mylist = Arrays.asList("n", "m","l");
        Collections.shuffle(mylist);
        sendKeys(robot, mylist.get(0));
        robot.delay(30);
        sendKeys(robot, mylist.get(1));
        robot.delay(30);
        sendKeys(robot, mylist.get(2));
        robot.delay(30);
    }


    private void transformNameAndSave(Player player, Map<String, String> textData,int indexNumber,int executorId) {
        if (textData.containsKey("playerName")) {
            log.info("> executor:{}, #{}, name:{}, coords:{}:{}", executorId,indexNumber, textData.get("fullPlayerName"), player.getXxCoordinate(), player.getYyCoordinate());
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


    private String getPlayerNameFromInfoConcurrent(Robot robot, int windowID) throws UnsupportedFlavorException, IOException {

        String result;
        //click the user castle
        robot.delay(Math.max(1, 3 * delayRecognition - (int) (new Date().getTime() - timers[windowID].getTime())));
        synchronized (lock) {
            checkAndSwitchWindow(robot, windowID);
            sendKeys(robot, "h");
            timers[windowID] = new Date();
        }
        //click the user info
        robot.delay(Math.max(1, 50+3 * delayRecognition - (int) (new Date().getTime() - timers[windowID].getTime())));
        synchronized (lock) {
            checkAndSwitchWindow(robot, windowID);
            sendKeys(robot, "j");
            timers[windowID] = new Date();
        }
        //copy username from user info
        robot.delay(Math.max(1, 100 + 2 * delayRecognition - (int) (new Date().getTime() - timers[windowID].getTime())));
        synchronized (lock) {
            checkAndSwitchWindow(robot, windowID);
            try {
                StringSelection stringSelection = new StringSelection("");
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stringSelection, null);
            }catch (IllegalStateException ex){
                log.error(" cannot open system clipboard");
                return "";
            }

            robot.delay(delayRecognition);
            result = "";
            //click the copy button
            sendKeys(robot, "k");
            robot.delay(2 * delayRecognition);
            try {
            Transferable t = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
            if (t != null && t.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                result = (String) t.getTransferData(DataFlavor.stringFlavor);
               // log.info(">executor: {} -  found name: {} ", windowID, result);
            }
            }catch (IllegalStateException ex){
                log.error(" cannot open system clipboard");
                return "";
            }
            timers[windowID] = new Date();
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

    private void setNameDetectionZoomConcurrent(Robot robot, int windowId) {

        synchronized (lock) {
            checkAndSwitchWindow(robot, windowId);
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
            timers[windowId] = new Date();
        }
    }

    private void setCoordinatesConcurrent(Robot robot, int windowID, int currentX, int currentY) {

        synchronized (lock) {
            checkAndSwitchWindow(robot, windowID);
            sendKeys(robot, "y");
            timers[windowID] = new Date();
        }
        robot.delay(Math.max(1, 2 * delay - (int) (new Date().getTime() - timers[windowID].getTime()))); //waiting the window to open
        synchronized (lock) {
            checkAndSwitchWindow(robot, windowID);
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
            timers[windowID] = new Date();
            //click end click
            robot.delay(30);
            sendKeys(robot, "o");
            timers[windowID] = new Date();
      }

//        robot.delay(Math.max(1, 2 * delay - (int) (new Date().getTime() - timers[windowID].getTime())));
//        synchronized (lock) {
//            checkAndSwitchWindow(robot, windowID);
//            sendKeys(robot, "o");
//            timers[windowID] = new Date();
//        }
    }

    @Synchronized
    private void sendKeys(Robot robot, String keys) {

        robot.delay(50);
        for (char c : keys.toCharArray()) {
            int keyCode = KeyEvent.getExtendedKeyCodeForChar(c);
            if (KeyEvent.CHAR_UNDEFINED == keyCode) {
                throw new RuntimeException(
                        "Key code not found for character '" + c + "'");
            }
           // robot.delay(30);
            robot.keyPress(keyCode);
            robot.delay(30);
            robot.keyRelease(keyCode);
            robot.delay(30);
        }
    }

    private void checkAndSwitchWindow(Robot robot, int listenersWindowId) {
        robot.delay(40);
        if (listenersWindowId != focusId) {

            focusId = listenersWindowId;

            robot.keyPress(KeyEvent.VK_WINDOWS);
            robot.delay(20);
            robot.keyPress(listenersWindowId+49); //Windows button is still pressed at this moment
            robot.delay(20);
            robot.keyRelease(listenersWindowId+49);
            robot.delay(20);
            robot.keyRelease(KeyEvent.VK_WINDOWS);
            robot.delay(50);

            //alt tabs
           /* robot.keyPress(KeyEvent.VK_ALT);
            robot.delay(30);

            robot.keyPress(KeyEvent.VK_TAB);
            robot.delay(30);

            robot.keyRelease(KeyEvent.VK_ALT);
            robot.delay(30);
            robot.keyRelease(KeyEvent.VK_TAB);*/
            //actually  button click
/*            robot.delay(50);

            if(focusId == 0){
                robot.delay(50);
                robot.mouseMove(225, 1054);
            }else{
                robot.delay(50);
                robot.mouseMove(270, 1054);
            }
            robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            robot.delay(50);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            robot.delay(2);
            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
            robot.delay(50);*/


        }
    }

}
