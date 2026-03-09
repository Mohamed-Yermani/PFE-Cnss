package com.projet.cnss.services;

import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucketName;

    // Initialiser le bucket au démarrage
    public void initBucket() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(bucketName).build()
            );
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(bucketName).build()
                );
                log.info("Bucket '{}' créé avec succès", bucketName);
            }
        } catch (Exception e) {
            log.error("Erreur lors de l'init du bucket : {}", e.getMessage());
        }
    }

    // Upload un fichier
    public String uploadDocument(String userEmail, MultipartFile file) throws Exception {
        initBucket();
        String objectName = userEmail.replace("@", "_").replace(".", "_")
                + "/" + System.currentTimeMillis() + "_" + file.getOriginalFilename();

        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(file.getContentType())
                        .build()
        );
        log.info("Fichier uploadé : {}", objectName);
        return objectName;
    }

    // Télécharger un fichier
    public byte[] downloadDocument(String objectName) throws Exception {
        InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        );
        return stream.readAllBytes();
    }

    // Supprimer un fichier
    public void deleteDocument(String objectName) throws Exception {
        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .build()
        );
        log.info("Fichier supprimé : {}", objectName);
    }

    // Obtenir l'URL de téléchargement
    public String getDownloadUrl(String objectName) throws Exception {
        return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .bucket(bucketName)
                        .object(objectName)
                        .method(Method.GET)
                        .build()
        );
    }
}