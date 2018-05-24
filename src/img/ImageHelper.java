package img;

import img.common.Lunar;
import img.dto.Image;
import img.dto.Pixel;
import img.dto.Request;

import javax.media.jai.*;
import java.awt.image.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class ImageHelper {

    public List<List<Pixel>> getPixelsArrayFromImage(Request request, Image image){
        RenderedOp op = JAI.create("fileload", request.getFilePath());
        image.setWidth(op.getWidth());
        image.setHeight(op.getHeight());

        final short[] pixels = ((DataBufferUShort) op.getData().getDataBuffer()).getData();

        int[][] data = new int[op.getWidth()][op.getHeight()];

        List<List<Pixel>> pixelsArray = new ArrayList<>();
        List<Pixel> columnPixels = new ArrayList<>();

        int col = 0;
        int row = 0;
        int blockSize = op.getWidth();
        Pixel pixel = null;
        int stepper = op.getColorModel().getColorSpace().isCS_sRGB() ? 3 : 1;

        for (int i = 0; i < pixels.length; i=i+stepper) {
            pixel = new Pixel();
            pixel.setPixelValue(pixels[i] < 0 ? 65536 + pixels[i] : pixels[i]); //adding 65535 to avoid
            pixel.setX(row+1); // Start the x from 1 than 0
            pixel.setY(col+1); // Start the y from 1 than 0
            data[row][col] = pixels[i];
            columnPixels.add(pixel);

//            System.out.println("("+row+","+col+") : " + data[row][col]);
            row++;
            if (row == blockSize) {
                col++;
                pixelsArray.add(columnPixels);
                columnPixels = new ArrayList<>();
                row = 0;
            }
        }
//        System.out.println(data);

        return pixelsArray;
    }

    public void writeToTiff(int[][] pixels, int width, int height, double standardDeviation, String filename) {
        BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_USHORT_GRAY);
        WritableRaster wr = bi.getRaster();

        for (int row = 0; row < height; row++){
                wr.setPixels(0, row, width, 1, pixels[row]); // writing one row at a time. Hence h is 1
        }

        SampleModel sampleModel =
                RasterFactory.createBandedSampleModel(DataBuffer.TYPE_USHORT,  width,height,1);
        // Create a compatible ColorModel.
        ColorModel colorModel = PlanarImage.createColorModel(sampleModel);

        TiledImage tiledImage = new TiledImage(0,0,width,height,0,0,
                sampleModel,colorModel);
        // Set the data of the tiled image to be the raster.
        tiledImage.setData(wr);
        // Save the image on a file.
        String fileName = filename+"_"+standardDeviation+"_"+Lunar.radius +".tiff";
        System.out.println(filename);
        JAI.create("filestore",tiledImage,fileName,"TIFF");

        System.out.println(wr);
    }
}
