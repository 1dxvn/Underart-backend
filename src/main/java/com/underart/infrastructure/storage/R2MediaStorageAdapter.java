package com.underart.infrastructure.storage;

import com.underart.domain.exception.MediaStorageException;
import com.underart.domain.port.out.MediaStoragePort;
import jakarta.annotation.PreDestroy;
import java.io.InputStream;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.core.checksums.ResponseChecksumValidation;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Component
public class R2MediaStorageAdapter implements MediaStoragePort {

    private static final Duration UPLOAD_URL_TTL = Duration.ofMinutes(15);

    private final String accountId;
    private final String accessKeyId;
    private final String secretAccessKey;
    private final String bucket;
    private final String publicBaseUrl;
    private S3Client client;
    private S3Presigner presigner;

    public R2MediaStorageAdapter(
            @Value("${storage.r2.account-id:}") String accountId,
            @Value("${storage.r2.access-key-id:}") String accessKeyId,
            @Value("${storage.r2.secret-access-key:}") String secretAccessKey,
            @Value("${storage.r2.bucket:}") String bucket,
            @Value("${storage.r2.public-url:}") String publicBaseUrl) {
        this.accountId = accountId;
        this.accessKeyId = accessKeyId;
        this.secretAccessKey = secretAccessKey;
        this.bucket = bucket;
        this.publicBaseUrl = publicBaseUrl.endsWith("/")
                ? publicBaseUrl.substring(0, publicBaseUrl.length() - 1) : publicBaseUrl;
    }

    @Override
    public void upload(String key, InputStream content, long sizeBytes, String contentType) {
        try {
            client().putObject(PutObjectRequest.builder().bucket(bucket).key(key)
                    .contentType(contentType).contentLength(sizeBytes).build(),
                    RequestBody.fromInputStream(content, sizeBytes));
        } catch (SdkException e) {
            throw new MediaStorageException("No se pudo subir el archivo", e);
        }
    }

    @Override
    public void delete(String key) {
        try {
            client().deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (SdkException e) {
            throw new MediaStorageException("No se pudo borrar el archivo", e);
        }
    }

    @Override
    public PresignedUpload presignUpload(String key, String contentType, long sizeBytes) {
        try {
            PresignedPutObjectRequest presigned = presigner().presignPutObject(PutObjectPresignRequest.builder()
                    .signatureDuration(UPLOAD_URL_TTL)
                    .putObjectRequest(PutObjectRequest.builder().bucket(bucket).key(key)
                            .contentType(contentType).contentLength(sizeBytes).build())
                    .build());
            return new PresignedUpload(presigned.url().toString(), presigned.expiration(),
                    Map.of("Content-Type", contentType));
        } catch (SdkException e) {
            throw new MediaStorageException("No se pudo generar la URL de subida", e);
        }
    }

    @Override
    public Optional<Long> objectSize(String key) {
        try {
            return Optional.of(client().headObject(HeadObjectRequest.builder().bucket(bucket).key(key).build())
                    .contentLength());
        } catch (NoSuchKeyException e) {
            return Optional.empty();
        } catch (S3Exception e) {
            if (e.statusCode() == 404) {
                return Optional.empty();
            }
            throw new MediaStorageException("No se pudo consultar el archivo", e);
        } catch (SdkException e) {
            throw new MediaStorageException("No se pudo consultar el archivo", e);
        }
    }

    @Override
    public String publicUrl(String key) {
        requireConfigured();
        return publicBaseUrl + "/" + key;
    }

    private synchronized S3Client client() {
        requireConfigured();
        if (client == null) {
            client = S3Client.builder()
                    .endpointOverride(endpoint())
                    .region(Region.of("auto"))
                    .credentialsProvider(credentials())
                    .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                    .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                    .responseChecksumValidation(ResponseChecksumValidation.WHEN_REQUIRED)
                    .build();
        }
        return client;
    }

    private synchronized S3Presigner presigner() {
        requireConfigured();
        if (presigner == null) {
            presigner = S3Presigner.builder()
                    .endpointOverride(endpoint())
                    .region(Region.of("auto"))
                    .credentialsProvider(credentials())
                    .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
                    .build();
        }
        return presigner;
    }

    private URI endpoint() {
        return URI.create("https://" + accountId + ".r2.cloudflarestorage.com");
    }

    private StaticCredentialsProvider credentials() {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, secretAccessKey));
    }

    private void requireConfigured() {
        boolean configured = Stream.of(accountId, accessKeyId, secretAccessKey, bucket, publicBaseUrl)
                .allMatch(v -> v != null && !v.isBlank());
        if (!configured) {
            throw new MediaStorageException("El almacenamiento de archivos no está configurado (variables R2_*)");
        }
    }

    @PreDestroy
    synchronized void close() {
        if (client != null) {
            client.close();
        }
        if (presigner != null) {
            presigner.close();
        }
    }
}
