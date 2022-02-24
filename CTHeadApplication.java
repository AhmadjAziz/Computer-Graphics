import java.io.FileInputStream;
import java.io.FileNotFoundException;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.FlowPane;
import javafx.scene.control.Button;
import javafx.scene.control.Slider;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.image.PixelWriter;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.io.*;

/**
 * This class reads the information of a CTHead and displays an interface in java fx that can be interacted with.
 * @version 1.0
 * @author Ahmad
 * @date 02/03/2021
 */
public class CTHeadApplication extends Application {
    short[][][] ctHead; // store the 3D volume data set
    short min, max; // min/max value in the 3D volume data set
    int ct_X_Axis = 256;
    int ct_Y_Axis = 256;
    int ct_Z_Axis = 113;
    final int SLICE_76 = 76;
    final double BEST_OPACITY = 0.12;
    double opacityVal = 0.12;

    /**
     * This method reads the cTHead file it then create a java fx interface for the user
     * to interact with which calls all the other methods from within the class.
     */
    @Override
    public void start(Stage stage) throws FileNotFoundException, IOException {
        stage.setTitle("CTHead Viewer");

        readData();

        int topWidth = ct_X_Axis;
        int topHeight = ct_Y_Axis;

        int frontWidth = ct_X_Axis;
        int frontHeight = ct_Z_Axis;

        int sideWidth = ct_Y_Axis;
        int sideHeight = ct_Z_Axis;

        // We need 3 things to see an image
        // 1. We create an image we can write to
        WritableImage topImage = new WritableImage(topWidth, topHeight);
        WritableImage frontImage = new WritableImage(frontWidth, frontHeight);
        WritableImage sideImage = new WritableImage(sideWidth, sideHeight);

        // 2. We create a view of that image
        ImageView topView = new ImageView(topImage);
        ImageView frontView = new ImageView(frontImage);
        ImageView sideView = new ImageView(sideImage);

        // sliders to step through the slices
        Slider topSlider = new Slider(0, ct_Z_Axis - 1, 0);
        topSlider.setShowTickMarks(true);
        topSlider.setShowTickLabels(true);

        Slider frontSlider = new Slider(0, ct_Y_Axis - 1, 0);
        frontSlider.setShowTickMarks(true);
        frontSlider.setShowTickLabels(true);

        Slider sideSlider = new Slider(0, ct_X_Axis - 1, 0);
        sideSlider.setShowTickMarks(true);
        sideSlider.setShowTickLabels(true);

        Slider skinOpacitySlider = new Slider(0, 1, 0);

        // buttons that show us the best view of the images.
        Button sliceButton = new Button("Slice 76");
        Button volumeRenderButton = new Button("Volume Render");

        sliceButton.setOnAction(event -> {
            /* sets the slider value to 76 which further opens slice 76. The slider then
           runs then calls the methods to print images on screen */
            topSlider.setValue(SLICE_76);
            frontSlider.setValue(SLICE_76);
            sideSlider.setValue(SLICE_76);
            topDownSlices(topImage,SLICE_76);
            frontBackSlices(frontImage,SLICE_76);
            sideToSideSlices(sideImage, SLICE_76);
        });

        volumeRenderButton.setOnAction(event -> {
             /* sets the slider value to 0.12 which further opens slice 76. The slider then
           runs then calls the methods to print images on screen */
            skinOpacitySlider.setValue(BEST_OPACITY);
            topDownRender(topImage,BEST_OPACITY );
            frontBackRender(frontImage,BEST_OPACITY);
            sideToSideRender(sideImage, BEST_OPACITY);

        });

        // listeners for the slider
        topSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            topDownSlices(topImage,newValue.intValue());
        });

        frontSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            frontBackSlices(frontImage,newValue.intValue());
        });

        sideSlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            sideToSideSlices(sideImage, newValue.intValue());
        });

        skinOpacitySlider.valueProperty().addListener((observable, oldValue, newValue) -> {
            topDownRender(topImage, newValue.doubleValue());
            frontBackRender(frontImage, newValue.doubleValue());
            sideToSideRender(sideImage, newValue.doubleValue());
        });

        //Creating a stage and FlowPane to display the data.
        FlowPane root = new FlowPane();
        root.setVgap(8);
        root.setHgap(4);

        root.getChildren().addAll(topView, frontView, sideView, topSlider, frontSlider, sideSlider,
                                    skinOpacitySlider, sliceButton, volumeRenderButton);
        Scene scene = new Scene(root, 800, 480);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * This method reads the ctHead file.
     * @throws IOException
     */
    public void readData() throws IOException {
        File file = new File("D:\\SoftwareEngineering\\CS255\\assignment1\\CThead");

        DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));

        int i, j, k; // loop through the 3D data set

        min = Short.MAX_VALUE;
        max = Short.MIN_VALUE; // set to extreme values
        short read; // value read in
        int b1, b2; // data is wrong Endian for Java so we need to swap the bytes around.

        ctHead = new short[ct_Z_Axis][ct_Y_Axis][ct_X_Axis]; // allocate the memory
        // loop through the data reading it in
        for (k = 0; k < ct_Z_Axis; k++) {
            for (j = 0; j < ct_Y_Axis; j++) {
                for (i = 0; i < ct_X_Axis; i++) {
                    // because the Endianess is wrong, it needs to be read byte at a time and swapped
                    b1 = ((int) in.readByte()) & 0xff; // the 0xff is because Java does not have unsigned types
                    b2 = ((int) in.readByte()) & 0xff;
                    read = (short) ((b2 << 8) | b1); // and swizzle the bytes around
                    if (read < min)
                        min = read; // update the minimum
                    if (read > max)
                        max = read; // update the maximum
                    ctHead[k][j][i] = read; // put the short into memory.
                }
            }
        }
    }

    /**
     * This method shows how to carry out an operation on an image. It obtains the
     * dimensions of the image, and then loops through the image carrying out the
     * copying of a slice of data into the image.
     * @param image would be image that goes from top of head to spinal cord.
     */
    public void topDownSlices(WritableImage image, int sliceNum) {
        // Get image dimensions, and declare loop variables
        int w = (int) image.getWidth(), h = (int) image.getHeight();
        PixelWriter image_writer = image.getPixelWriter();

        double col;
        short datum;
        
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {

                datum = ctHead[sliceNum][j][i]; // uses the slider value to go through the slices.
                // calculate the colour by performing a mapping from [min,max] -> 0 to 1 (float)
                col = (((float) datum - (float) min) / ((float) (max - min)));
                image_writer.setColor(i, j, Color.color(col, col, col, 1.0));

            }
        }
    }

    /**
     * This method shows how to carry out an operation on an image. It obtains the
     * dimensions of the image, and then loops through the image carrying out the
     * copying of a slice of data into the image.
     * @param image would be image that goes from nose to back of head.
     */
    public void frontBackSlices(WritableImage image, int sliceNum) {
        // Get image dimensions, and declare loop variables
        int w = (int) image.getWidth(), h = (int) image.getHeight();
        PixelWriter image_writer = image.getPixelWriter();

        double col;
        short datum;

        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {

                datum = ctHead[j][sliceNum][i];// uses the slider value to go through the slices.
                // calculate the colour by performing a mapping from [min,max] -> 0 to 1 (float)
                col = (((float) datum - (float) min) / ((float) (max - min)));
                image_writer.setColor(i, j, Color.color(col, col, col, 1.0));
            }
        }
    }

    /**
     * This method shows how to carry out an operation on an image. It obtains the
     * dimensions of the image, and then loops through the image carrying out the
     * copying of a slice of data into the image.
     * @param image would be image that goes from ear to ear.
     */
    public void sideToSideSlices(WritableImage image, int sliceNum) {
        // Get image dimensions, and declare loop variables
        int w = (int) image.getWidth(), h = (int) image.getHeight();
        PixelWriter image_writer = image.getPixelWriter();

        double col;
        short datum;

        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {

                datum = ctHead[j][i][sliceNum]; // uses the slider value to go through the slices.
                // calculate the colour by performing a mapping from [min,max] -> 0 to 1 (float)
                col = (((float) datum - (float) min) / ((float) (max - min)));
                image_writer.setColor(i, j, Color.color(col, col, col, 1.0));
            }
        }
    }

    /**
     * This method shows how to carry an operation on the image. It goes throwugh the image in 3-D
     * For each coordinate in width and height, it goes through the depth to accumulate the colors of bone and skin
     * the accumulated data is combined into a writable image that prints to the screen.
     * @param image would be image that goes from top of head to spinal cord.
     */
    public void topDownRender(WritableImage image, double skinOpacity) {

       // Get image dimensions, and declare loop variables
        int w = (int) image.getWidth(), h = (int) image.getHeight();
        int imageDepth = ct_Z_Axis;
        PixelWriter image_writer = image.getPixelWriter();

        short datum;
        double red, green, blue, opacity, transparency;
        final double LIGHT_SOURCE = 1;

        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {

                red = green = blue = 0;
                transparency = 1;
                for (int k = 0; k < imageDepth; k++) {
                    datum = ctHead[k][j][i];

                    if (datum < -300) {
                        // opacity is 0 so image is fully transparent.
                        opacity = 0;
                        // the O at end of each color is the intensity of each color out of 1 for each slice.
                        red += transparency * LIGHT_SOURCE * opacity * 0;
                        green += transparency * LIGHT_SOURCE * opacity * 0;
                        blue += transparency * LIGHT_SOURCE * opacity * 0;
                        transparency = transparency * (1 - opacity);

                    } else if (datum >= -300 && datum <= 49) {
                        opacity = skinOpacity;
                        // the values at end of each color is the intensity of each color out of 1 for each slice.
                        red += transparency * LIGHT_SOURCE * opacity * 1;
                        green += transparency * LIGHT_SOURCE * opacity * 0.79;
                        blue += transparency * LIGHT_SOURCE * opacity * 0.6;
                        transparency = transparency * (1 - opacity);

                    } else if (datum >= 50 && datum <= 299) {
                        opacity = 0;
                        // the values at end of each color is the intensity of each color out of 1 for each slice.
                        red += transparency * LIGHT_SOURCE * opacity * 1;
                        green += transparency * LIGHT_SOURCE * opacity * 1;
                        blue += transparency * LIGHT_SOURCE * opacity * 1;
                        transparency = transparency * (1 - opacity);

                    } else if (datum >= 300 && datum <= 4096) {
                        opacity = 0.8;
                        // the values at end of each color is the intensity of each color out of 1 for each slice.
                        red += transparency * LIGHT_SOURCE * opacity * 1;
                        green += transparency * LIGHT_SOURCE * opacity * 1;
                        blue += transparency * LIGHT_SOURCE * opacity * 1;
                        transparency = transparency * (1 - opacity);

                    }
                }
                // The upper bound of a color is 1 hence anything above upper bound is limited to upper bound.
                if (red > 1)
                    red = 1;

                if (green > 1)
                    green = 1;

                if (blue > 1)
                    blue = 1;

                // Once the image has been accumulated k times we write the final image.
                image_writer.setColor(i, j, Color.color(red, green, blue, 1));
            }
        }
    }

    /**
     * This method shows how to carry an operation on the image. It goes throwugh the image in 3-D
     * For each coordinate in width and height, it goes through the depth to accumulate the colors of bone and skin
     * the accumulated data is combined into a writable image that prints to the screen.
     * @param image would be image that goes from nose to back of head.
     */
    public void frontBackRender(WritableImage image, double skinOpacity) {

        // Get image dimensions, and declare loop variables
        int w = (int) image.getWidth(), h = (int) image.getHeight();
        int imageDepth = ct_Y_Axis;
        PixelWriter image_writer = image.getPixelWriter();

        short datum;
        double red, green, blue, opacity, transparency;
        final double LIGHT_SOURCE = 1;

        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {

                red = green = blue = 0;
                transparency = 1;

                for (int k = 0; k < imageDepth; k++) {
                    datum = ctHead[j][k][i];

                    if (datum < -300) {
                        opacity = 0;
                        // the values at end of each color is the intensity of each color out of 1 for each slice.
                        red += transparency * LIGHT_SOURCE * opacity * 0;
                        green += transparency * LIGHT_SOURCE * opacity * 0;
                        blue += transparency * LIGHT_SOURCE * opacity * 0;
                        transparency = transparency * (1 - opacity);

                    } else if (datum >= -300 && datum <= 49) {
                        opacity = skinOpacity;
                        // the values at end of each color is the intensity of each color out of 1 for each slice.
                        red += transparency * LIGHT_SOURCE * opacity * 1;
                        green += transparency * LIGHT_SOURCE * opacity * 0.79;
                        blue += transparency * LIGHT_SOURCE * opacity * 0.6;
                        transparency = transparency * (1 - opacity);

                    } else if (datum >= 50 && datum <= 299) {
                        opacity = 0;
                        // the values at end of each color is the intensity of each color out of 1 for each slice.
                        red += transparency * LIGHT_SOURCE * opacity * 1;
                        green += transparency * LIGHT_SOURCE * opacity * 1;
                        blue += transparency * LIGHT_SOURCE * opacity * 1;
                        transparency = transparency * (1 - opacity);

                    } else if (datum >= 300 && datum <= 4096) {
                        opacity = 0.8;
                        // the values at end of each color is the intensity of each color out of 1 for each slice.
                        red += transparency * LIGHT_SOURCE * opacity * 1;
                        green += transparency * LIGHT_SOURCE * opacity * 1;
                        blue += transparency * LIGHT_SOURCE * opacity * 1;
                        transparency = transparency * (1 - opacity);

                    }
                }
                // The upper bound of a color is 1 hence anything above upper bound is limited to upper bound.
                if (red > 1)
                    red = 1;

                if (green > 1)
                    green = 1;

                if (blue > 1)
                    blue = 1;

                // Once the image has been accumulated k times we write the final image.
                image_writer.setColor(i, j, Color.color(red, green, blue, 1));
            }
        }
    }

    /**
     * This method shows how to carry an operation on the image. It goes throwugh the image in 3-D
     * For each coordinate in width and height, it goes through the depth to accumulate the colors of bone and skin
     * the accumulated data is combined into a writable image that prints to the screen.
     * @param image would be image that goes from ear to ear.
     */
    public void sideToSideRender(WritableImage image, double skinOpacity) {

        // Get image dimensions, and declare loop variables
        int w = (int) image.getWidth(), h = (int) image.getHeight();
        int imageDepth = ct_Y_Axis;
        PixelWriter image_writer = image.getPixelWriter();

        short datum;
        double red, green, blue, opacity, transparency;
        final double LIGHT_SOURCE = 1;

        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {

                red = green = blue = 0;
                transparency = 1;

                for (int k = 0; k < imageDepth; k++) {
                    datum = ctHead[j][i][k];

                    if (datum < -300) {
                        opacity = 0;
                        // the values at end of each color is the intensity of each color out of 1 for each slice.
                        red += transparency * LIGHT_SOURCE * opacity * 0;
                        green += transparency * LIGHT_SOURCE * opacity * 0;
                        blue += transparency * LIGHT_SOURCE * opacity * 0;
                        transparency = transparency * (1 - opacity);

                    } else if (datum >= -300 && datum <= 49) {
                        opacity = skinOpacity;
                        // the values at end of each color is the intensity of each color out of 1 for each slice.
                        red += transparency * LIGHT_SOURCE * opacity * 1;
                        green += transparency * LIGHT_SOURCE * opacity * 0.79;
                        blue += transparency * LIGHT_SOURCE * opacity * 0.6;
                        transparency = transparency * (1 - opacity);

                    } else if (datum >= 50 && datum <= 299) {
                        opacity = 0;
                        // the values at end of each color is the intensity of each color out of 1 for each slice.
                        red += transparency * LIGHT_SOURCE * opacity * 1;
                        green += transparency * LIGHT_SOURCE * opacity * 1;
                        blue += transparency * LIGHT_SOURCE * opacity * 1;
                        transparency = transparency * (1 - opacity);

                    } else if (datum >= 300 && datum <= 4096) {
                        opacity = 0.8;
                        // the values at end of each color is the intensity of each color out of 1 for each slice.
                        red += transparency * LIGHT_SOURCE * opacity * 1;
                        green += transparency * LIGHT_SOURCE * opacity * 1;
                        blue += transparency * LIGHT_SOURCE * opacity * 1;
                        transparency = transparency * (1 - opacity);

                    }
                }
                // The upper bound of a color is 1 hence anything above upper bound is limited to upper bound.
                if (red > 1)
                    red = 1;

                if (green > 1)
                    green = 1;

                if (blue > 1)
                    blue = 1;

                //Once the image has been accumulated k times we write the final image.
                image_writer.setColor(i, j, Color.color(red, green, blue, 1));
            }
        }
    }

    /**
     * This method simply launches the code.
     */
    public static void main(String[] args) {

        launch();
    }
}