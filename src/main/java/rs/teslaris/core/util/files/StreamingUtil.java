package rs.teslaris.core.util.files;

import io.minio.GetObjectResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

public class StreamingUtil {

    private static final int BUFFER_SIZE = 8192;


    private StreamingUtil() {
    }

    public static StreamingResponseBody createStreamingBody(InputStream inputStream,
                                                            int bufferSize, Runnable onComplete) {
        return outputStream -> {
            try (inputStream) {
                byte[] buffer = new byte[bufferSize];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    outputStream.flush();
                }

                if (Objects.nonNull(onComplete)) {
                    onComplete.run();
                }
            }
        };
    }

    public static StreamingResponseBody createStreamingBody(InputStream inputStream) {
        return createStreamingBody(inputStream, BUFFER_SIZE, null);
    }

    public static StreamingResponseBody createStreamingBody(InputStream inputStream,
                                                            Runnable runnable) {
        return createStreamingBody(inputStream, BUFFER_SIZE, runnable);
    }

    public static StreamingResponseBody createStreamingBodyFromS3Response(GetObjectResponse file,
                                                                          int bufferSize) {
        return outputStream -> {
            try (var inputStream = file) {
                byte[] buffer = new byte[bufferSize];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    outputStream.flush();
                }
            }
        };
    }

    public static StreamingResponseBody createStreamingBodyFromS3Response(GetObjectResponse file) {
        return createStreamingBodyFromS3Response(file, BUFFER_SIZE);
    }

    public static void streamData(InputStream inputStream, OutputStream outputStream,
                                  int bufferSize) throws IOException {
        try (inputStream) {
            byte[] buffer = new byte[bufferSize];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                outputStream.flush();
            }
        }
    }

    public static void streamData(InputStream inputStream, OutputStream outputStream)
        throws IOException {
        streamData(inputStream, outputStream, BUFFER_SIZE);
    }
}
