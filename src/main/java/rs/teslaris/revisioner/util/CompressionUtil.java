package rs.teslaris.revisioner.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CompressionUtil {

    public static byte[] compress(String content) {
        try (
            var baos = new ByteArrayOutputStream();
            var gzip = new GZIPOutputStream(baos)
        ) {
            gzip.write(content.getBytes(StandardCharsets.UTF_8));
            gzip.finish();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String decompress(byte[] bytes) {
        try (
            var bais = new ByteArrayInputStream(bytes);
            var gzip = new GZIPInputStream(bais)
        ) {
            return new String(
                gzip.readAllBytes(),
                StandardCharsets.UTF_8
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
