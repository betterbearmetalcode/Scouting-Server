package org.tahomarobotics.scouting.scoutingserver.util;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

import javax.imageio.ImageIO;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
public class QRCodeUtil {
    //qr codes images are cached in this folder. Filenames are the timestamp they were created and file extensions are .bmp
    public static final String iamgeDataFilepath = "src/main/resources/org/tahomarobotics/scouting/scoutingserver/images/";
    private static final Map<DecodeHintType, ErrorCorrectionLevel> decodeHintMap = new HashMap<DecodeHintType, ErrorCorrectionLevel>();
    private static final String charset = "UTF-8";

    public static ArrayList<String> qrData = new ArrayList<>();

//untested
    public static void createQRCode(String qrCodeData, String filePath, Map hintMap, int qrCodeheight, int qrCodewidth)
            throws WriterException, IOException {
        BitMatrix matrix = new MultiFormatWriter().encode(
                new String(qrCodeData.getBytes(charset), charset),
                BarcodeFormat.QR_CODE, qrCodewidth, qrCodeheight);
        MatrixToImageWriter.writeToFile(matrix, filePath.substring(filePath
                .lastIndexOf('.') + 1), new File(filePath));
    }

    /**
     * @param filePath
     * @return Qr Code value
     * @throws FileNotFoundException
     * @throws IOException
     * @throws NotFoundException
     */
    public static String readQRCode(String filePath) throws IOException, NotFoundException {
        FileInputStream stream = null;
        Result qrCodeResult = null;
        try {
            stream = new FileInputStream(filePath);
            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(
                    new BufferedImageLuminanceSource(
                            ImageIO.read(stream))));
            qrCodeResult = new MultiFormatReader().decode(binaryBitmap);

        }finally {
            stream.close();
        }

        return qrCodeResult.getText();
    }



    public static String decodeText(String input) throws DataFormatException {
        String compressedText  = input.split("&")[0];
        //decompress
        Inflater decompressor = new Inflater();
        decompressor.setInput(input.getBytes(StandardCharsets.UTF_8), 0, compressedText.length());
        byte[] result = new byte[100];
        int resultLength = decompressor.inflate(result);
        decompressor.end();

        return new String(result, 0, resultLength, StandardCharsets.UTF_8);
    }


    public static ArrayList<String> getCachedQRData() {
        ArrayList<String> output = new ArrayList<>();
        File dir = new File(iamgeDataFilepath);
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
            for (File child : directoryListing) {
                try {
                    output.add(QRCodeUtil.readQRCode(child.getCanonicalPath()));
                } catch (IOException  e) {

                    e.printStackTrace();
                    System.err.println("Failed to read some random cached qr code...");
                } catch (NotFoundException e) {
                    //was unable to read data from this file. Therefore it is useless and will be deleted
                    if (!child.delete()) {
                        child.deleteOnExit();
                    }
                }
            }
            return output;
        } else {
            return output;
        }

    }

    public record MatchData() {

    }

}