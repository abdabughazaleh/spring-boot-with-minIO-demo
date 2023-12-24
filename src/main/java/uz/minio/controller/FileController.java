package uz.minio.controller;

import io.minio.*;
import io.minio.errors.MinioException;
import io.minio.http.Method;
import io.minio.messages.Bucket;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/app-minio")
public class FileController {

    @Value("${minio.bucketName}")
    private String bucketName;

    private final MinioClient minioClient;

    public FileController(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            InputStream inputStream = file.getInputStream();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(file.getOriginalFilename())
                    .stream(inputStream, inputStream.available(), -1)
                    .build());
            return ResponseEntity.ok("File uploaded successfully.");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload file.");
        }
    }

    @GetMapping("/download/{fileName}")
    public ResponseEntity<InputStreamResource> downloadFile(@PathVariable("fileName") String fileName) {
        try {
            InputStream stream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build()
            );
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName);

            return ResponseEntity.ok()
                    .headers(headers)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(new InputStreamResource(stream));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/temporary-url/{fileName}")
    public ResponseEntity<String> getTemporaryFileUrl(@PathVariable("fileName") String fileName) {
        try {
            String url = getTempUrl(bucketName, fileName);
            return ResponseEntity.ok(url);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to generate temporary URL for file.");
        }
    }

    @GetMapping("/get-buckets")
    public ResponseEntity<List<String>> getList() {
        try {
            List<Bucket> bucketList =
                    minioClient.listBuckets();
            for (Bucket bucket : bucketList) {
                System.out.println(bucket.creationDate() + ", " + bucket.name());
            }
            return ResponseEntity.status(HttpStatus.OK)
                    .body(bucketList.stream()
                    .map(Bucket::name)
                    .collect(Collectors.toList()));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
/*    @GetMapping("/object-is-exist")
    public ResponseEntity<List<String>> isObjectExist(@PathVariable("objectName") String objectName) {
        try {
            List<Bucket> bucketList =
                    minioClient.getObject().bucket();

            return ResponseEntity.status(HttpStatus.OK)
                    .body(bucketList.stream()
                    .map(Bucket::name)
                    .collect(Collectors.toList()));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }*/

    @PostMapping("/add-folder/{folderName}")
    public ResponseEntity<Void> addFolder(@PathVariable("folderName") String folderName) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder().bucket(bucketName)
                            .object("spring-boot-3/")
                            .stream(new ByteArrayInputStream(new byte[] {}), 0, -1)
                            .build());
            return ResponseEntity.status(HttpStatus.OK).body(null);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getTempUrl(String bucketName, String objectName) throws IOException, NoSuchAlgorithmException, InvalidKeyException, MinioException {
        Map<String, String> reqParams = new HashMap<>();
        //     reqParams.put("response-content-type", "application/json");
        StatObjectResponse statObjectResponse = minioClient.statObject(StatObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                .build());
        System.out.println(statObjectResponse);
        String url = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(bucketName)
                        .object(objectName)
                        .expiry(5, TimeUnit.SECONDS)
                        .extraQueryParams(reqParams)
                        .build());
        System.out.println(url);
        return url;
    }
}
