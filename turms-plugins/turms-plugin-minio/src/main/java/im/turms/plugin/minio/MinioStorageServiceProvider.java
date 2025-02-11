/*
 * Copyright (C) 2019 The Turms Project
 * https://github.com/turms-im/turms
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package im.turms.plugin.minio;

import im.turms.server.common.access.admin.web.MediaType;
import im.turms.server.common.access.client.dto.constant.StorageResourceType;
import im.turms.server.common.access.common.ResponseStatusCode;
import im.turms.server.common.infra.collection.CollectionUtil;
import im.turms.server.common.infra.exception.ResponseException;
import im.turms.server.common.infra.exception.ResponseExceptionPublisherPool;
import im.turms.server.common.infra.lang.StringUtil;
import im.turms.server.common.infra.logging.core.logger.Logger;
import im.turms.server.common.infra.logging.core.logger.LoggerFactory;
import im.turms.server.common.infra.plugin.TurmsExtension;
import im.turms.server.common.infra.property.TurmsPropertiesManager;
import im.turms.server.common.infra.property.env.service.business.storage.StorageItemProperties;
import im.turms.server.common.infra.property.env.service.business.storage.StorageProperties;
import im.turms.server.common.infra.reactor.PublisherUtil;
import im.turms.service.domain.group.service.GroupMemberService;
import im.turms.service.domain.message.service.MessageService;
import im.turms.service.infra.plugin.extension.StorageServiceProvider;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioAsyncClient;
import io.minio.PostPolicy;
import io.minio.RemoveObjectArgs;
import io.minio.SetBucketLifecycleArgs;
import io.minio.SetBucketPolicyArgs;
import io.minio.http.Method;
import io.minio.messages.Expiration;
import io.minio.messages.LifecycleConfiguration;
import io.minio.messages.LifecycleRule;
import io.minio.messages.RuleFilter;
import io.minio.messages.Status;
import org.springframework.context.ApplicationContext;
import reactor.core.publisher.Mono;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static im.turms.server.common.access.client.dto.constant.StorageResourceType.GROUP_PROFILE_PICTURE;
import static im.turms.server.common.access.client.dto.constant.StorageResourceType.MESSAGE_ATTACHMENT;
import static im.turms.server.common.access.client.dto.constant.StorageResourceType.USER_PROFILE_PICTURE;

/**
 * @author James Chen
 */
public class MinioStorageServiceProvider extends TurmsExtension implements StorageServiceProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(MinioStorageServiceProvider.class);

    private static final int INIT_BUCKETS_TIMEOUT_SECONDS = 60;
    private static final Map<StorageResourceType, String> RESOURCE_TYPE_TO_BUCKET_NAME;
    private static final String RESOURCE_URL = "url";

    private static final String HTTP_HEADER_CONTENT_TYPE = "Content-Type";

    private TurmsMinioAsyncClient client;
    private String baseUrl;

    private StorageProperties storageProperties;

    private MessageService messageService;
    private GroupMemberService groupMemberService;

    private boolean isServing;

    static {
        StorageResourceType[] resourceTypes = StorageResourceType.values();
        RESOURCE_TYPE_TO_BUCKET_NAME = CollectionUtil.newMapWithExpectedSize(resourceTypes.length - 1);
        for (StorageResourceType resourceType : resourceTypes) {
            if (resourceType != StorageResourceType.UNRECOGNIZED) {
                RESOURCE_TYPE_TO_BUCKET_NAME.put(resourceType,
                        resourceType.name().toLowerCase().replace("_", "-"));
            }
        }
    }

    @Override
    public void onStarted() {
        setUp();
    }

    private void setUp() {
        MinioStorageProperties properties = loadProperties(MinioStorageProperties.class);
        if (!properties.isEnabled()) {
            return;
        }
        String endpoint = properties.getEndpoint();
        URI uri;
        try {
            uri = new URI(endpoint);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("The endpoint URL is illegal: " + endpoint, e);
        }
        if (!uri.isAbsolute()) {
            throw new IllegalArgumentException("The endpoint URL must be absolute: " + endpoint);
        }
        ApplicationContext context = getContext();
        messageService = context.getBean(MessageService.class);
        groupMemberService = context.getBean(GroupMemberService.class);
        TurmsPropertiesManager propertiesManager = context.getBean(TurmsPropertiesManager.class);
        storageProperties = propertiesManager.getLocalProperties().getService().getStorage();
        baseUrl = uri.getScheme() + "://" + uri.getAuthority();
        initClient(endpoint, properties.getRegion(),
                properties.getAccessKey(), properties.getSecretKey());
        Duration timeout = Duration.ofSeconds(INIT_BUCKETS_TIMEOUT_SECONDS);
        try {
            initBuckets()
                    .block(timeout);
            isServing = true;
        } catch (Exception e) {
            MinioStorageProperties.Retry retry = properties.getRetry();
            int maxAttempts = retry.getMaxAttempts();
            if (!retry.isEnabled() || maxAttempts <= 0) {
                throw new RuntimeException("Failed to initialize the MinIO client", e);
            }
            LOGGER.error("Failed to initialize the MinIO client. Retry times: 0", e);
            try {
                Thread.sleep(retry.getInitialIntervalMillis());
            } catch (InterruptedException ex) {
                throw new RuntimeException("Failed to initialize the MinIO client", e);
            }
            for (int currentRetryTimes = 1; currentRetryTimes <= maxAttempts; currentRetryTimes++) {
                try {
                    initBuckets()
                            .block(timeout);
                } catch (Exception ex) {
                    LOGGER.error("Failed to initialize the MinIO client. Retry times: " + currentRetryTimes, ex);
                    if (currentRetryTimes == maxAttempts) {
                        throw new RuntimeException("Failed to initialize the MinIO client with retries exhausted: " + maxAttempts);
                    }
                    try {
                        Thread.sleep(retry.getIntervalMillis());
                    } catch (InterruptedException ignored) {
                        throw new RuntimeException("Failed to initialize the MinIO client", ex);
                    }
                }
            }
            isServing = true;
        }
    }

    private void initClient(String endpoint, String region, String accessKey, String secretKey) {
        MinioAsyncClient.Builder builder = MinioAsyncClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey);
        if (StringUtil.isNotBlank(region)) {
            builder.region(region);
        }
        client = new TurmsMinioAsyncClient(builder.build());
    }

    @Override
    public Mono<Void> deleteUserProfilePicture(Long requesterId,
                                               @Nullable String resourceKeyStr,
                                               @Nullable Long resourceKeyNum) {
        if (!isServing) {
            return ResponseExceptionPublisherPool.serverUnavailable();
        }
        String objectKey = requesterId.toString();
        String bucketName = getBucketName(StorageResourceType.USER_PROFILE_PICTURE);
        return PublisherUtil
                .fromFuture(() -> client.removeObject(RemoveObjectArgs.builder()
                        .bucket(bucketName)
                        .object(objectKey)
                        .build()))
                .onErrorMap(t -> new RuntimeException("Failed to remove the user profile picture [" +
                        objectKey +
                        "] in the bucket [" +
                        bucketName +
                        "]"
                        , t));
    }

    @Override
    public Mono<Map<String, String>> queryUserProfilePictureUploadInfo(Long requesterId,
                                                                       @Nullable String resourceKeyStr,
                                                                       @Nullable Long resourceKeyNum) {
        if (!isServing) {
            return ResponseExceptionPublisherPool.serverUnavailable();
        }
        String objectKey = requesterId.toString();
        StorageItemProperties itemProperties = storageProperties.getUserProfilePicture();
        Map<String, String> uploadInfo = getResourceUploadInfo(getBucketName(USER_PROFILE_PICTURE),
                objectKey,
                itemProperties.getAllowedContentType(),
                itemProperties.getMinSizeBytes(),
                itemProperties.getMaxSizeBytes(),
                itemProperties.getUploadUrlExpireAfterSeconds());
        return Mono.just(uploadInfo);
    }

    @Override
    public Mono<Map<String, String>> queryUserProfilePictureDownloadInfo(Long requesterId,
                                                                         @Nullable String resourceKeyStr,
                                                                         @Nullable Long resourceKeyNum) {
        if (!isServing) {
            return ResponseExceptionPublisherPool.serverUnavailable();
        }
        if (resourceKeyNum == null) {
            return Mono.error(ResponseException
                    .get(ResponseStatusCode.ILLEGAL_ARGUMENT, "The user ID must not be null"));
        }
        String url = baseUrl + "/" + getBucketName(StorageResourceType.USER_PROFILE_PICTURE) + "/" + resourceKeyNum;
        return Mono.just(Map.of(RESOURCE_URL, url));
    }

    @Override
    public Mono<Void> deleteGroupProfilePicture(Long requesterId,
                                                @Nullable String resourceKeyStr,
                                                @Nullable Long resourceKeyNum) {
        if (!isServing) {
            return ResponseExceptionPublisherPool.serverUnavailable();
        }
        if (resourceKeyNum == null) {
            return Mono.error(ResponseException
                    .get(ResponseStatusCode.ILLEGAL_ARGUMENT, "The group ID must not be null"));
        }
        return groupMemberService.isOwnerOrManager(requesterId, resourceKeyNum, false)
                .flatMap(hasPermission -> {
                    if (!hasPermission) {
                        return ResponseExceptionPublisherPool.unauthorized();
                    }
                    String objectKey = resourceKeyNum.toString();
                    String bucketName = getBucketName(GROUP_PROFILE_PICTURE);
                    return PublisherUtil
                            .fromFuture(() -> client.removeObject(RemoveObjectArgs.builder()
                                    .bucket(bucketName)
                                    .object(objectKey)
                                    .build()))
                            .onErrorMap(t -> new RuntimeException("Failed to remove the group profile picture [" +
                                    objectKey +
                                    "] in the bucket [" +
                                    bucketName +
                                    "]"
                                    , t));
                });
    }

    @Override
    public Mono<Map<String, String>> queryGroupProfilePictureUploadInfo(Long requesterId,
                                                                        @Nullable String resourceKeyStr,
                                                                        @Nullable Long resourceKeyNum) {
        if (!isServing) {
            return ResponseExceptionPublisherPool.serverUnavailable();
        }
        if (resourceKeyNum == null) {
            return Mono.error(ResponseException
                    .get(ResponseStatusCode.ILLEGAL_ARGUMENT, "The group ID must not be null"));
        }
        return groupMemberService.isOwnerOrManager(requesterId, resourceKeyNum, false)
                .flatMap(hasPermission -> {
                    if (!hasPermission) {
                        return ResponseExceptionPublisherPool.unauthorized();
                    }
                    StorageItemProperties itemProperties = storageProperties.getGroupProfilePicture();
                    String objectKey = resourceKeyNum.toString();
                    Map<String, String> uploadInfo = getResourceUploadInfo(getBucketName(GROUP_PROFILE_PICTURE),
                            objectKey,
                            itemProperties.getAllowedContentType(),
                            itemProperties.getMinSizeBytes(),
                            itemProperties.getMaxSizeBytes(),
                            itemProperties.getUploadUrlExpireAfterSeconds());
                    return Mono.just(uploadInfo);
                });
    }

    @Override
    public Mono<Map<String, String>> queryGroupProfilePictureDownloadInfo(Long requesterId,
                                                                          @Nullable String resourceKeyStr,
                                                                          @Nullable Long resourceKeyNum) {
        if (!isServing) {
            return ResponseExceptionPublisherPool.serverUnavailable();
        }
        if (resourceKeyNum == null) {
            return Mono.error(ResponseException
                    .get(ResponseStatusCode.ILLEGAL_ARGUMENT, "The group ID must not be null"));
        }
        String url = baseUrl + "/" + getBucketName(GROUP_PROFILE_PICTURE) + "/" + resourceKeyNum;
        return Mono.just(Map.of(RESOURCE_URL, url));
    }

    @Override
    public Mono<Void> deleteMessageAttachment(Long requesterId,
                                              @Nullable String resourceKeyStr,
                                              @Nullable Long resourceKeyNum) {
        return Mono.error(ResponseException
                .get(ResponseStatusCode.ILLEGAL_ARGUMENT, "The message attachment cannot be deleted"));
    }

    @Override
    public Mono<Map<String, String>> queryMessageAttachmentUploadInfo(Long requesterId,
                                                                      @Nullable String resourceKeyStr,
                                                                      @Nullable Long resourceKeyNum) {
        if (!isServing) {
            return ResponseExceptionPublisherPool.serverUnavailable();
        }
        if (resourceKeyNum == null) {
            return Mono.error(ResponseException
                    .get(ResponseStatusCode.ILLEGAL_ARGUMENT, "The message ID must not be null"));
        }
        String objectKey = resourceKeyStr == null
                ? resourceKeyNum.toString()
                : resourceKeyNum + "/" + resourceKeyStr;
        StorageItemProperties itemProperties = storageProperties.getMessageAttachment();
        Map<String, String> uploadInfo = getResourceUploadInfo(getBucketName(MESSAGE_ATTACHMENT),
                objectKey,
                itemProperties.getAllowedContentType(),
                itemProperties.getMinSizeBytes(),
                itemProperties.getMaxSizeBytes(),
                itemProperties.getUploadUrlExpireAfterSeconds());
        return Mono.just(uploadInfo);
    }

    @Override
    public Mono<Map<String, String>> queryMessageAttachmentDownloadInfo(Long requesterId,
                                                                        @Nullable String resourceKeyStr,
                                                                        @Nullable Long resourceKeyNum) {
        if (!isServing) {
            return ResponseExceptionPublisherPool.serverUnavailable();
        }
        if (resourceKeyNum == null) {
            return Mono.error(ResponseException
                    .get(ResponseStatusCode.ILLEGAL_ARGUMENT, "The message ID must not be null"));
        }
        return messageService.isMessageRecipientOrSender(resourceKeyNum, requesterId)
                .flatMap(hasPermission -> {
                    String key = resourceKeyStr == null
                            ? resourceKeyNum.toString()
                            : resourceKeyNum + "/" + resourceKeyStr;
                    return getPresignedDownloadUrl(getBucketName(MESSAGE_ATTACHMENT),
                            key,
                            storageProperties
                                    .getMessageAttachment()
                                    .getDownloadUrlExpireAfterSeconds())
                            .map(url -> Map.of(RESOURCE_URL, url));
                });
    }

    //region bucket
    private Mono<Void> initBuckets() {
        StorageResourceType[] resourceTypes = StorageResourceType.values();
        List<Mono<Void>> initBuckets = new ArrayList<>(resourceTypes.length - 1);
        for (StorageResourceType resourceType : resourceTypes) {
            if (resourceType == StorageResourceType.UNRECOGNIZED) {
                continue;
            }
            String bucket = getBucketName(resourceType);
            Mono<Void> initBucket = bucketExists(bucket)
                    .flatMap(exists -> {
                        if (exists) {
                            LOGGER.info("Bucket {} has already existed", bucket);
                            return Mono.empty();
                        }
                        Mono<Void> createBucket = createBucket(resourceType)
                                .then(Mono.defer(() -> {
                                    int days = switch (resourceType) {
                                        case USER_PROFILE_PICTURE -> storageProperties.getUserProfilePicture().getExpireAfterDays();
                                        case GROUP_PROFILE_PICTURE -> storageProperties.getGroupProfilePicture().getExpireAfterDays();
                                        case MESSAGE_ATTACHMENT -> storageProperties.getMessageAttachment().getExpireAfterDays();
                                        default -> throw ResponseException
                                                .get(ResponseStatusCode.ILLEGAL_ARGUMENT, "The resource type is unknown: " + resourceType);
                                    };
                                    return setBucketLifecycle(bucket, days);
                                }));
                        if (resourceType == StorageResourceType.USER_PROFILE_PICTURE ||
                                resourceType == GROUP_PROFILE_PICTURE) {
                            createBucket = createBucket
                                    .then(Mono.defer(() -> setBucketPolicy(bucket)));
                        }
                        return createBucket
                                .doOnSuccess(unused -> LOGGER.info("Bucket {} is created", bucket));
                    })
                    .onErrorMap(t -> new RuntimeException("Failed to initialize bucket", t));
            initBuckets.add(initBucket);
        }
        return Mono.whenDelayError(initBuckets);
    }

    private Mono<Void> createBucket(StorageResourceType resourceType) {
        String bucketName = getBucketName(resourceType);
        return PublisherUtil.fromFuture(() -> client.makeBucket(MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build()))
                .onErrorMap(t -> new RuntimeException("Failed to create the bucket [" + bucketName + "]", t));
    }

    private Mono<Void> setBucketPolicy(String bucket) {
        //language=JSON
        String policy = """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Action": [
                        "s3:GetObject"
                      ],
                      "Effect": "Allow",
                      "Resource": "arn:aws:s3:::%s/*",
                      "Principal": "*"
                    }
                  ]
                }""".formatted(bucket);
        return PublisherUtil.fromFuture(() -> client.setBucketPolicy(SetBucketPolicyArgs.builder()
                        .bucket(bucket)
                        .config(policy)
                        .build()))
                .onErrorMap(t -> new RuntimeException("Failed to set the bucket policy [" +
                        StringUtil.sanitizeLatin1(policy) +
                        "] to the bucket [" +
                        bucket
                        + "]", t))
                .then();
    }

    private Mono<Void> setBucketLifecycle(String bucket, int expireAfterDays) {
        if (expireAfterDays <= 0) {
            return Mono.empty();
        }
        return PublisherUtil.fromFuture(() -> client.setBucketLifecycle(SetBucketLifecycleArgs.builder()
                        .bucket(bucket)
                        .config(new LifecycleConfiguration(List.of(
                                new LifecycleRule(
                                        Status.ENABLED,
                                        null,
                                        new Expiration((ZonedDateTime) null, expireAfterDays, null),
                                        new RuleFilter(""),
                                        null,
                                        null,
                                        null,
                                        null)
                        )))
                        .build()))
                .onErrorMap(t -> new RuntimeException("Failed to set a lifecycle configuration to the bucket [" +
                        bucket
                        + "]", t))
                .then();
    }

    private Mono<Boolean> bucketExists(String bucket) {
        return PublisherUtil.fromFuture(() -> client.bucketExists(BucketExistsArgs.builder()
                        .bucket(bucket)
                        .build()))
                .onErrorMap(t -> new RuntimeException("Failed to check if the bucket [" + bucket + "] exists", t));
    }

    private String getBucketName(StorageResourceType resourceType) {
        return RESOURCE_TYPE_TO_BUCKET_NAME.get(resourceType);
    }
    //endregion

    //region presign
    private Mono<String> getPresignedDownloadUrl(@NotNull String bucket,
                                                 @NotNull String key,
                                                 int expireAfterSeconds) {
        GetPresignedObjectUrlArgs.Builder builder = GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(bucket)
                .object(key);
        if (expireAfterSeconds > 0) {
            builder.expiry(expireAfterSeconds);
        }
        return PublisherUtil.fromFuture(() -> client.getPresignedObjectUrlAsync(builder.build()))
                .onErrorMap(t -> new RuntimeException("Failed to get the presigned URL to download the resource object [" +
                        key +
                        "] in the bucket [" +
                        bucket +
                        "]", t));
    }

    private Map<String, String> getResourceUploadInfo(@NotNull String bucket,
                                                      @NotNull String objectKey,
                                                      @NotNull String allowedContentType,
                                                      int minSizeBytes,
                                                      int maxSizeBytes,
                                                      int expireAfterSeconds) {
        PostPolicy policy = new PostPolicy(bucket, ZonedDateTime.now().plusSeconds(expireAfterSeconds));
        policy.addEqualsCondition("key", objectKey);
        MediaType mediaType = MediaType.create(allowedContentType);
        if (!mediaType.isWildcardType()) {
            if (mediaType.isWildcardSubtype()) {
                policy.addStartsWithCondition(HTTP_HEADER_CONTENT_TYPE, mediaType.type() + "/");
            } else {
                policy.addEqualsCondition(HTTP_HEADER_CONTENT_TYPE, mediaType.toString());
            }
        }
        boolean hasMaxSizeLimit = maxSizeBytes > 0;
        if (minSizeBytes > 0 || hasMaxSizeLimit) {
            if (hasMaxSizeLimit && minSizeBytes > maxSizeBytes) {
                throw new IllegalArgumentException("The minimum size cannot be greater than the maximum size");
            }
            policy.addContentLengthRangeCondition(minSizeBytes, maxSizeBytes);
        }
        try {
            Map<String, String> map = client.getPresignedPostFormData(policy);
            CollectionUtil.add(map, RESOURCE_URL, baseUrl + "/" + bucket);
            return map;
        } catch (Exception e) {
            throw new RuntimeException("Failed to get the presigned post form data for the resource object [" +
                    objectKey +
                    "] in the bucket [" +
                    bucket +
                    "]"
                    , e);
        }
    }
    //endregion

}