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

    private static final Map<DecodeHintType, ErrorCorrectionLevel> decodeHintMap = new HashMap<DecodeHintType, ErrorCorrectionLevel>();
    private static final String charset = "UTF-8";




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







}