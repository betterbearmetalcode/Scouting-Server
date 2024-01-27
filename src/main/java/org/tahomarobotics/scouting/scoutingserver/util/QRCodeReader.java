package org.tahomarobotics.scouting.scoutingserver.util;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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
public class QRCodeReader {


    private static final Map<DecodeHintType, ErrorCorrectionLevel> decodeHintMap = new HashMap<DecodeHintType, ErrorCorrectionLevel>();
    private static final String charset = "UTF-8";

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
    public static String readQRCode(String filePath) throws FileNotFoundException, IOException, NotFoundException {

        BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(
                new BufferedImageLuminanceSource(
                        ImageIO.read(new FileInputStream(filePath)))));
        Result qrCodeResult = new MultiFormatReader().decode(binaryBitmap);
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
}