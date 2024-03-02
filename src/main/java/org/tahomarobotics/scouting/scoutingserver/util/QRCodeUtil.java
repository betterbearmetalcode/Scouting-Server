package org.tahomarobotics.scouting.scoutingserver.util;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

@SuppressWarnings("deprecation")
public class QRCodeUtil {

    private static final String charset = "UTF-8";


    public static void createQRCode(String qrCodeData, String filePath, int qrCodeheight, int qrCodewidth)
            throws WriterException, IOException {
        BitMatrix matrix = new MultiFormatWriter().encode(
                new String(qrCodeData.getBytes(charset), charset),
                BarcodeFormat.QR_CODE, qrCodewidth, qrCodeheight);
        MatrixToImageWriter.writeToFile(matrix, filePath.substring(filePath
                .lastIndexOf('.') + 1), new File(filePath));
    }

    public static String readQRCode(String filePath) throws IOException, NotFoundException {
        FileInputStream stream = null;
        Result qrCodeResult = null;
        try {
            File file = new File(filePath);
            if (!file.exists()) {
                Logging.logError(new FileNotFoundException(filePath), "Could not read image: " + filePath);
            }else {
                stream = new FileInputStream(file);
                BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(
                        new BufferedImageLuminanceSource(
                                ImageIO.read(stream))));
                qrCodeResult = new MultiFormatReader().decode(binaryBitmap);
            }

        } finally {
            if (stream != null) {
                stream.close();
            }
        }
        if (qrCodeResult != null) {
            return qrCodeResult.getText();
        }else throw new IOException("Could not read QR code");

    }


}