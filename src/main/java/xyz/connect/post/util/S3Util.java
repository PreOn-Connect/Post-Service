package xyz.connect.post.util;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import xyz.connect.post.custom_exception.PostApiException;
import xyz.connect.post.enumeration.ErrorCode;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@Slf4j
public class S3Util {

    private final AmazonS3 amazonS3;
    private final Set<String> validMediaType;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    public S3Util(AmazonS3 amazonS3) {
        this.amazonS3 = amazonS3;
        validMediaType = new HashSet<>(List.of(
                "image/gif", "image/jfif", "image/pjpeg", "image/jpeg", "image/pjp",
                "image/jpg", "image/png", "image/bmp", "image/webp", "image/svgz", "image/svg")
        );
    }

    // MultipartFile 리스트를 받아 해당 파일들을 s3에 업로드
    public void uploadFile(MultipartFile multipartFile) throws IOException {
        if (!validMediaType.contains(multipartFile.getContentType())) {
            throw new PostApiException(ErrorCode.INVALID_MEDIA_TYPE);
        }

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(multipartFile.getSize());
        metadata.setContentType(multipartFile.getContentType());
        String fileName = UUID.randomUUID().toString();

        amazonS3.putObject(bucket, fileName, multipartFile.getInputStream(), metadata);
        log.info("File is uploaded. name: " + fileName);
    }

    //S3 에 업로드된 파일명을 찾아 Public url 을 리턴
    public String getImageUrl(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return null;
        }

        if (!amazonS3.doesObjectExist(bucket, fileName)) {
            return null;
        }

        return amazonS3.getUrl(bucket, fileName).toString();
    }

    //S3 업로드된 파일명을 받아 삭제
    public void deleteFile(String path) {
        if (path == null || path.isEmpty()) return;
        amazonS3.deleteObject(bucket, path);
    }
}
