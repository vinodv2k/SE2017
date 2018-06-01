package img;

import img.common.Lunar;
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
        request.setStandardDeviation(4);
        request.setFilePath("./img/1_250_PC.tif");

        Lunar.radius = 20;

        Processor processor = new Processor();
        processor.processCorona(request);
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
}
