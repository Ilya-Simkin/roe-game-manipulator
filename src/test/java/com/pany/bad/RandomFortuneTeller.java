package com.pany.bad;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.Random;
import java.util.Scanner;

@RunWith(SpringRunner.class)
@ActiveProfiles("manual-activation")
public class RandomFortuneTeller {

    @Test
    public void main() {
        System.out.println("I am the genie...i will tell you your fortune...");
        Scanner userInput = new Scanner(System.in);

        boolean fPressed = false;

        while (!fPressed) {
            System.out.println("Press f to hear your fortune");

            String enteredByUser = userInput.next();

            if (enteredByUser.equals("f")) {
                System.out.println("I'm looking at your future");
                break;
            }

        }
        Random randomObject = new Random();
        int randomNumber = randomObject.nextInt(10) + 1;
        System.out.println(randomNumber);

        switch (randomNumber) {

            case 1:
                System.out.println("You will come into money soon...");
                break;

            case 2:
                System.out.println("A new life awaits you...");
                break;

            case 3:
                System.out.println("Somebody new will enter your life...");
                break;

            case 4:
                System.out.println("A foreign country is calling your name...");
                break;

            case 5:
                System.out.println("A pleasant surprise is in store for you...");
                break;

            case 6:
                System.out.println("Good news is travelling to you now...");
                break;

            case 7:
                System.out.println("You will have very good luck soon...");
                break;

            case 8:
                System.out.println("Your future is very cloudy...");
                break;

            case 9:
                System.out.println("There is light at the end of the tunnel...");
                break;

            case 10:
                System.out.println("A new opportunity will present itself...");
                break;

            default:
                System.out.println("I cant see your future...sorry!!!");


        }

        System.out.println("Thank you for your participation...");
    }


    @Test
    public void testAltTab() throws AWTException {
        Robot robot = new Robot();

        robot.delay(30000); //time to set windows

        for (int i = 0; i < 10; i++) {

            robot.keyPress(KeyEvent.VK_ALT);
            robot.delay(30);

            robot.keyPress(KeyEvent.VK_TAB);
            robot.delay(30);

            robot.keyRelease(KeyEvent.VK_ALT);
            robot.delay(30);
            robot.keyRelease(KeyEvent.VK_TAB);
            robot.delay(30);

            sendKeys(robot, "u");

            robot.delay(1000);

        }
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

