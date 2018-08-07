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

        long startTime = System.currentTimeMillis();
        Request request = new Request();
        request.setStandardDeviation(32);
        request.setFilePath("./img/Tiff/06.tif");
        Processor processor = new Processor();
        processor.processCorona(request);
        System.out.println("Standard Deviation "+ request.getStandardDeviation() + " took "+ ((System.currentTimeMillis() - startTime)/1000) + " seconds");

    }
}
