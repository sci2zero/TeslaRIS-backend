package rs.teslaris.core.util;

import java.io.IOException;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
public class ResourceMultipartFile implements MultipartFile {

    private final String name;

    private final String originalFilename;

    private final String contentType;

    private final ByteArrayResource byteArrayResource;


    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public String getOriginalFilename() {
        return this.originalFilename;
    }

    @Override
    public String getContentType() {
        return this.contentType;
    }

    @Override
    public boolean isEmpty() {
        return byteArrayResource.contentLength() == 0;
    }

    @Override
    public long getSize() {
        return byteArrayResource.contentLength();
    }

    @Override
    public byte[] getBytes() throws IOException {
        return byteArrayResource.getByteArray();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return byteArrayResource.getInputStream();
    }

    @Override
    public void transferTo(java.io.File dest) throws IllegalStateException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Resource getResource() {
        return byteArrayResource;
    }
}
