package img;

import img.common.SolarCenter;
import img.dto.Pixel;
import img.dto.Request;
import img.logic.Processor;
import img.utils.CoordinateUtil;

import java.util.ArrayList;
import java.util.List;

public class ImgApplication {
    public static void main(String[] args) throws Exception {
        Request request = new Request();
        request.setStandardDeviation(1);
        request.setFilePath("./img/sample.tif");

        SolarCenter.solarCenterX = 66;
        SolarCenter.solarCenterY = 66;

        Processor processor = new Processor();
        processor.processCorona(request);
//        testOnlyOnePixel();

//        testAngles();
    }

    public static void testAngles(){
        Pixel pixel = new Pixel();
        pixel.setxOffset(45);
        pixel.setyOffset(45);
        pixel.setQuadrant(1);
        CoordinateUtil.updateDegrees(pixel);
        System.out.println(pixel.getRadius());
        System.out.println(pixel.getAngle());
    }

    public static void testOnlyOnePixel() {
        SolarCenter.solarCenterX = 66;
        SolarCenter.solarCenterY = 66;

        Request request = new Request();
        request.setStandardDeviation(1);
        request.setFilePath("./img/sample.tif");

        List<List<Pixel>> pixels = new ArrayList<>();
        List<Pixel> innerPixels = new ArrayList<>();
        Pixel pixel = new Pixel();
        pixel.setX(20);
        pixel.setY(20);
        innerPixels.add(pixel);
        pixels.add(innerPixels);

        Processor processor = new Processor();
        processor.achf(pixels);
    }
}
