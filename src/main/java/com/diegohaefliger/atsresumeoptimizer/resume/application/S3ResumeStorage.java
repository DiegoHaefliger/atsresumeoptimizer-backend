package com.diegohaefliger.atsresumeoptimizer.resume.application;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Component
class S3ResumeStorage implements ResumeStorage {

	private final S3Client s3Client;
	private final StorageProperties properties;

	S3ResumeStorage(StorageProperties properties) {
		this.properties = properties;
		this.s3Client = S3Client.builder()
				.endpointOverride(URI.create(properties.endpoint()))
				.region(Region.US_EAST_1)
				.credentialsProvider(
						StaticCredentialsProvider.create(AwsBasicCredentials.create(properties.accessKey(), properties.secretKey())))
				.forcePathStyle(true)
				.build();
	}

	@PostConstruct
	void ensureBucketExists() {
		try {
			s3Client.headBucket(HeadBucketRequest.builder().bucket(properties.bucket()).build());
		} catch (NoSuchBucketException exception) {
			s3Client.createBucket(CreateBucketRequest.builder().bucket(properties.bucket()).build());
		}
	}

	@Override
	public void upload(String key, byte[] content, String contentType) {
		s3Client.putObject(
				PutObjectRequest.builder().bucket(properties.bucket()).key(key).contentType(contentType).build(),
				RequestBody.fromBytes(content));
	}

	@Override
	public byte[] download(String key) {
		return s3Client.getObjectAsBytes(GetObjectRequest.builder().bucket(properties.bucket()).key(key).build()).asByteArray();
	}

	@Override
	public void delete(String key) {
		s3Client.deleteObject(DeleteObjectRequest.builder().bucket(properties.bucket()).key(key).build());
	}
}
