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
        // Sample the screen
        while (i<9){
            arr.append(screenSample(i));
            i++;
        }

        // MQTT process starts here
        String topic        = "esp32/output";
        String content      = arr.toString();
        int qos             = 0;
        //TODO: update your MQTT server IP address here
        String broker       = "tcp://0.0.0.0:1883";
        String clientId     = "ScreenSample";
        MemoryPersistence persistence = new MemoryPersistence();

        try {
            MqttClient sampleClient = new MqttClient(broker, clientId, persistence);
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(true);
            System.out.println("Connecting to broker: "+broker);
            sampleClient.connect(connOpts);
            System.out.println("Connected");
            // Use infinite loop so the screen sampling code runs until it is stopped
            while(true){
                MqttMessage message = new MqttMessage(content.getBytes());
                message.setQos(qos);
                sampleClient.publish(topic, message);
                // empty out the array for the next screen sampling result
                arr.delete(0, arr.length());
                i = 0;
                while (i<9){
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
        Image src = SwingFXUtils.toFXImage(img, null);

        // TODO set grid size
        // for our booth we will use a 3x3 grid so there are 9 sections
        int gridWidth = 3;
        int gridHeight = 3;

        // TODO set ledRow and ledColumn size
        // for ledRows, the number of rows of LEDs should be the same for all panels because the
        // height of the panels are all the same
        int ledRows = 3;
        // for ledColumns, the number of columns of LEDs are different for each panel as the panel
        // size is different for each side
        int ledColumns;

        if (panel % 3 == 0){
            ledColumns = 52;
        }
        else if (panel % 3 == 1){
            ledColumns = 32;
        }
        else{
            ledColumns = 21;
        }


        // get screen pixels
        PixelReader reader = src.getPixelReader();
        int width = (int)src.getWidth();
        int height = (int)src.getHeight();

        // initialize the LED matrix
        StringBuilder ledMatrix = new StringBuilder();

        // width/gridWidth because we are sampling 1 grid at a time
        // divide by the ledColumns to calculate how many pixels to skip
        int gapWidth = (width/gridWidth)/(ledColumns);
        // same like calculating gapWidth
        int gapHeight= (height/gridHeight)/(ledRows);

        int height_init;
        int height_limit;
        int width_init;
        int width_limit;

        // panel 0 is the first panel the data line goes through. panel 1 is the next panel, etc.
        if (panel == 0){
            width_init = 0;
            width_limit = width/gridWidth;
            height_init = 0;
            height_limit = height/gridHeight;
        }
        else if (panel == 1){
            width_init = width/gridWidth;
            width_limit = 2*(width/gridWidth);
            height_init = 0;
            height_limit = height/gridHeight;
        }
        else if (panel == 2){
            width_init = 2*(width/gridWidth);
            width_limit = 3*(width/gridWidth);
            height_init = 0;
            height_limit = height/gridHeight;
        }
        else if (panel == 3){
            width_init = 0;
            width_limit = width/gridWidth;
            height_init = height/gridHeight;
            height_limit = 2*(height/gridHeight);
        }
        else if (panel == 4){
            width_init = width/gridWidth;
            width_limit = 2*(width/gridWidth);
            height_init = height/gridHeight;
            height_limit = 2*(height/gridHeight);
        }
        else if (panel == 5){
            width_init = 2*(width/gridWidth);
            width_limit = 3*(width/gridWidth);
            height_init = height/gridHeight;
            height_limit = 2*(height/gridHeight);
        }
        else if (panel == 6){
            width_init = 0;
            width_limit = width/gridWidth;
            height_init = 2*(height/gridHeight);
            height_limit = 3*(height/gridHeight);
        }
        else if (panel == 7){
            width_init = width/gridWidth;
            width_limit = 2*(width/gridWidth);
            height_init = 2*(height/gridHeight);
            height_limit = 3*(height/gridHeight);
        }
        else{
            width_init = 2*(width/gridWidth);
            width_limit = 3*(width/gridWidth);
            height_init = 2*(height/gridHeight);
            height_limit = 3*(height/gridHeight);
        }

        for (int y = height_init; y < height_limit; y+=gapHeight) {
            StringBuilder ledStrip = new StringBuilder();
            for (int x = width_init; x < width_limit; x+=gapWidth) {
                // read pixel from source image
                Color color = reader.getColor(x, y);
                double red = color.getRed();
                double blue = color.getBlue();
                double green = color.getGreen();

                // Color adjustment here
                if (red < 0.35 && blue < 0.35 && green < 0.35){
                    // means that it is dark color
                    red = 0;
                    blue = 0;
                    green = 0;
                }
                else if (red > 0.8 && blue > 0.8 && green > 0.8){
                    // means that it is light color (white)
                    red = 1;
                    blue = 1;
                    green = 1;
                }
                else if (red >= blue && red >= green){
                    // means its red so decrease the G and B values
                    blue *= 0.15;
                    green *= 0.15;
                }
                else if (blue >= red && blue >= green){
                    red *= 0.15;
                    green *= 0.15;
                }
                else if (green >= blue && green >= red){
                    red *= 0.15;
                    blue *= 0.15;
                }

                // convert values to 0-255 range
                int f_red = (int) Math.round((red * 255));
                int f_blue = (int) Math.round((blue * 255));
                int f_green= (int) Math.round((green * 255));

                try {
                    // The commented lines below are for our prototype booth. If you will be using the prototype booth
                    // uncomment these lines
//                    if (panel == 1 || panel == 2) {
//                        int number = f_blue;
//                        int f_blue_new = 0;
//                        while (number != 0) {
//                            int remainder = number % 10;
//                            f_blue_new = f_blue_new * 10 + remainder;
//                            number = number / 10;
//                        }
//                        number = f_red;
//                        int f_red_new = 0;
//                        while (number != 0) {
//                            int remainder = number % 10;
//                            f_red_new = f_red_new * 10 + remainder;
//                            number = number / 10;
//                        }
//                        number = f_green;
//                        int f_green_new = 0;
//                        while (number != 0) {
//                            int remainder = number % 10;
//                            f_green_new = f_green_new * 10 + remainder;
//                            number = number / 10;
//                        }
//                        ledStrip.append(f_blue_new);
//                        ledStrip.append(",");
//                        ledStrip.append(f_green_new);
//                        ledStrip.append(",");
//                        ledStrip.append(f_red_new);
//                        ledStrip.append(":");
//                    }
//                    else{
//                        ledStrip.append(":");
//                        ledStrip.append(f_red);
//                        ledStrip.append(",");
//                        ledStrip.append(f_green);
//                        ledStrip.append(",");
//                        ledStrip.append(f_blue);
//                    }
                    // If you will be using the above code, comment the append lines below
                    ledStrip.append(":");
                    ledStrip.append(f_red);
                    ledStrip.append(",");
                    ledStrip.append(f_green);
                    ledStrip.append(",");
                    ledStrip.append(f_blue);
                }

                catch(IndexOutOfBoundsException e) {
                    break;
                }
            }
            // for prototype panels
//            if (panel == 1 || panel == 2) {
//                ledMatrix.append(ledStrip.reverse());
//            }
//            else{
//                ledMatrix.append(ledStrip);
//            }
            // for non-prototype panels
            ledMatrix.append(ledStrip);
        }
        // UNCOMMENT THE BELOW LINE FOR DEBUGGING
        // System.out.println(ledMatrix.toString());
        return ledMatrix.toString();
    }
}
