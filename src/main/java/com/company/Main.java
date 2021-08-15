package com.company;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.paint.Color;

import java.awt.image.BufferedImage;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class Main {
    public static void main(String args[])
    {
        StringBuilder arr = new StringBuilder();
        int i = 0;
        while (i<3){
            arr.append(screenSample(i));
            i++;
        }

        // MQTT process starts here
        String topic        = "esp32/output";
        String content      = arr.toString();
        int qos             = 0;
        //TODO: update your MQTT server IP address here
        String broker       = "tcp://192.168.255.240:1883";
        String clientId     = "ScreenSample";
        MemoryPersistence persistence = new MemoryPersistence();

        try {
            MqttClient sampleClient = new MqttClient(broker, clientId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            System.out.println("Connecting to broker: "+broker);
            sampleClient.connect(connOpts);
            System.out.println("Connected");
            // for now use infinite loop
            while(true){
//                System.out.println(content);
                MqttMessage message = new MqttMessage(content.getBytes());
                message.setQos(qos);
                sampleClient.publish(topic, message);
                // empty out the array
                arr.delete(0, arr.length());
                i = 0;
                while (i<3){
                    arr.append(screenSample(i));
                    i++;
                };
                content = arr.toString();
            }
        } catch(MqttException me) {
            System.out.println("reason "+me.getReasonCode());
            System.out.println("msg "+me.getMessage());
            System.out.println("loc "+me.getLocalizedMessage());
            System.out.println("cause "+me.getCause());
            System.out.println("excep "+me);
            me.printStackTrace();
        }
    }

    private static String screenSample(int panel){
        BufferedImage img = null;
        try {
            Robot robot = new Robot();

            Rectangle rectangle = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            img = robot.createScreenCapture(rectangle);

        } catch (AWTException ex) {
            System.err.println(ex);
        }
//        BufferedImage img = null;
//        File f;
//        try
//        {
//            f = new File("C:\\Users\\Sesilia Fenina G\\Desktop\\checkered.jpg");
//            img = ImageIO.read(f);
//        }
//        catch(IOException e)
//        {
//            System.out.println(e);
//        }
        Image src = SwingFXUtils.toFXImage(img, null);

        // TODO set grid size
        int gridWidth = 3;
        int gridHeight = 3;

        int ledRows = 3;
        int ledColumns;

        if (panel == 0){
            ledColumns = 21;
        }
        else if (panel == 1){
            ledColumns = 32;
        }
        else{
            ledColumns = 52;
        }


        // get screen pixels
        PixelReader reader = src.getPixelReader();
        int width = (int)src.getWidth();
        int height = (int)src.getHeight();

        // initialize the LED matrix
        StringBuilder ledMatrix = new StringBuilder();

        // width/gridWidth because we are sampling 1 grid at a time
        // divide by the ledColumns to calculate how many pixels to skip
        // add 1 to account for the last LED
        int gapWidth = (width/gridWidth)/(ledColumns);
        // same like calculating gapWidth
        int gapHeight= (height/gridHeight)/(ledRows);

//        int counterY = 0;
        // middle grid
        for (int y = height/gridHeight; y < 2*(height/gridHeight); y+=gapHeight) {
//            int counterX = 0;
            int init;
            int limit;

            if (panel == 2){
                init = 0;
                limit = width/gridWidth;
            }
            else if (panel == 1){
                init = width/gridWidth;
                limit = 2*(width/gridWidth);
            }
            else{
                init = 2*(width/gridWidth);
                limit = 3*(width/gridWidth);
            }
            StringBuilder ledStrip = new StringBuilder();
            for (int x = init; x < limit; x+=gapWidth) {
                // read pixel from source image
                Color color = reader.getColor(x, y);
                double red = color.getRed();
                double blue = color.getBlue();
                double green = color.getGreen();
//                System.out.print(red + " ");
//                System.out.print(blue + " ");
//                System.out.print(green + "\n");

                if (red < 0.35 && blue < 0.35 && green < 0.35){
                    // means that it is dark color
                    red = 0;
                    blue = 0;
                    green = 0;
                }
                else if (red > 0.8 && blue > 0.8 && green > 0.8){
                    red = 1;
                    blue = 1;
                    green = 1;
                }
                else if (red >= blue && red >= green){
//                    red *= 0.7;
                    blue *= 0.15;
                    green *= 0.15;
                }
                else if (blue >= red && blue >= green){
//                    blue *= 0.7;
                    red *= 0.15;
                    green *= 0.15;
                }
                else if (green >= blue && green >= red){
//                    green *= 2;
                    red *= 0.15;
                    blue *= 0.15;
                }
                // convert values to 0-255 range
                int f_red = (int) Math.round((red * 255));
                int f_blue = (int) Math.round((blue * 255));
                int f_green= (int) Math.round((green * 255));

                try {
                    if (panel == 1 || panel == 2) {
                        int number = f_blue;
                        int f_blue_new = 0;
                        while (number != 0) {
                            int remainder = number % 10;
                            f_blue_new = f_blue_new * 10 + remainder;
                            number = number / 10;
                        }
                        number = f_red;
                        int f_red_new = 0;
                        while (number != 0) {
                            int remainder = number % 10;
                            f_red_new = f_red_new * 10 + remainder;
                            number = number / 10;
                        }
                        number = f_green;
                        int f_green_new = 0;
                        while (number != 0) {
                            int remainder = number % 10;
                            f_green_new = f_green_new * 10 + remainder;
                            number = number / 10;
                        }
                        ledStrip.append(f_blue_new);
                        ledStrip.append(",");
                        ledStrip.append(f_green_new);
                        ledStrip.append(",");
                        ledStrip.append(f_red_new);
                        ledStrip.append(":");
                    }
                    else{
                        ledStrip.append(":");
                        ledStrip.append(f_red);
                        ledStrip.append(",");
                        ledStrip.append(f_green);
                        ledStrip.append(",");
                        ledStrip.append(f_blue);
                    }
                }

                catch(IndexOutOfBoundsException e) {
                    break;
                }
            }
            if (panel == 1 || panel == 2) {
                ledMatrix.append(ledStrip.reverse());
            }
            else{
                ledMatrix.append(ledStrip);
            }
        }
//        System.out.println(ledMatrix.toString());
        return ledMatrix.toString();
    }
}
