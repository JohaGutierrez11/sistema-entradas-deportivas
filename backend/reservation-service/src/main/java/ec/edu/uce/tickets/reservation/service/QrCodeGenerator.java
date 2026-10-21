package ec.edu.uce.tickets.reservation.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.HexFormat;

@Component
public class QrCodeGenerator {

    private static final int IMAGE_SIZE = 300;
    private final SecureRandom random = new SecureRandom();

    /** 32 bytes aleatorios = 64 caracteres hexadecimales. Imposible de adivinar. */
    public String newCode() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    /** Dibuja el código como imagen PNG. */
    public byte[] toPng(String code) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(code, BarcodeFormat.QR_CODE, IMAGE_SIZE, IMAGE_SIZE);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (WriterException | IOException e) {
            throw new IllegalStateException("No se pudo generar la imagen del QR", e);
        }
    }
}