package com.pany.bad.service;

import java.awt.*;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;

public interface RiseOfEmpiresRobot {

    void detectPlayersCoordinatesRoutine(int state , boolean isNewSwipe, int startX, int startY) throws AWTException, IOException, URISyntaxException;

    void recognizeLatestPlayersNamesRoutineFromImageScreen() throws Exception;

    void recognizeLatestPlayersNamesRoutineByPlayerInfo(int pageToStart , int firstOffset,Integer ... instances) throws Exception;

    Integer runAsyncNamesRoutineOnPage( int pageToStart, int firstOffset, int pageNum, int windowIdAssignment) throws AWTException, IOException, UnsupportedFlavorException;
}
