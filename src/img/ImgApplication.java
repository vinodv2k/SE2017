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
        request.setStandardDeviation(1);
        request.setFilePath("./img/1_250_PC.tif");

        Lunar.radius = 30;

        Processor processor = new Processor();
        processor.processCorona(request);
    }
}
