package com.inspien.cepaas.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.inspien.cepaas.enums.ErrorCode;
import com.inspien.cepaas.exception.OftpException;
import com.inspien.cepaas.msgbox.api.*;
import com.inspien.cepaas.util.io.IOUtil;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.UnsupportedCharsetException;
import java.util.List;
import java.util.Map;

public class MessageBoxClient implements AutoCloseable {
    private final CloseableHttpClient client;
    private final String endpoint;
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final String GET_RECORD_PATH = "/msgbox/ngin/rest/record/get/%s/%s";
    private static final String CREATE_MESSAGE_PATH = "/msgbox/ngin/rest/message/post/";
    private static final String CREATE_RECORD_PATH = "/msgbox/ngin/rest/message/post/";
    private static final String UPDATE_ATTACHMENT_PATH = "/msgbox/ngin/rest/attachment/put";
    private static final String GET_PENDING_RECORDS_PATH = "/msgbox/ngin/rest/record/get/query";
    private static final String GET_PAYLOAD_PATH = "/msgbox/ngin/rest/payload/get";
    private static final String UPDATE_MESSAGE_STATUS_PATH = "/msgbox/ngin/rest/record/put/";


    public MessageBoxClient(String endpoint) {
        this.endpoint = endpoint;
        this.client = HttpClientBuilder.create().build();
    }

    public MessageRecordWithAttachment getRecord(String slotId, String messageId) {
        String path = String.format(GET_RECORD_PATH, slotId, messageId);
        return executeGetRequest(path, MessageRecordWithAttachment.class);
    }

    public void createMessage(MessagePostRequest request) {
        String path = CREATE_MESSAGE_PATH + request.getRecord().getMessage_id();
        executePostRequest(path, request);
    }

    public void createRecord(MessageRecordWithAttachment request) {
        String path = CREATE_RECORD_PATH + request.getMessage_id();
        executePostRequest(path, request);
    }

    public void updateAttachment(MessageAttachmentPushRequest request) {
        executePostRequest(UPDATE_ATTACHMENT_PATH, request);
    }

    public List<MessageRecordWithAttachment> getPendingRecords(PendingMessageRecordQuery query) {
        return executePostRequest(GET_PENDING_RECORDS_PATH, query, new TypeReference<List<MessageRecordWithAttachment>>() {});
    }

    public byte[] getPayload(String msgboxId, String slotId, String msgId) {
        MessagePayloadPullRequest req = new MessagePayloadPullRequest(msgboxId, slotId, msgId);
        return executePostRequest(GET_PAYLOAD_PATH, req, byte[].class);
    }

    public void updateMessageStatus(MessageRecordWithAttachment request) {
        String path = UPDATE_MESSAGE_STATUS_PATH + request.getMessage_id();
        executePostRequest(path, request);
    }

    @Override
    public void close() throws IOException {
        client.close();
    }

    @SuppressWarnings("resource")
    private <T> T executeGetRequest(String path, Class<T> responseType) {
        HttpGet get;
        try {
            get = new HttpGet(getUri(endpoint, path));
            HttpResponse response = client.execute(get);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                return mapper.readValue(response.getEntity().getContent(), responseType);
            } else {
                throw new OftpException(ErrorCode.RESPONSE_NOT_SUCCESS, IOUtil.readFromInputStream(response.getEntity().getContent()));
            }
        } catch (URISyntaxException | IOException e) {
            throw new OftpException(ErrorCode.REQUEST_FAILED, "Request failed: {}" + e.getMessage());
        }
    }

    private void executePostRequest(String path, Object request) {
        executePostRequest(path, request, Void.class);
    }

    @SuppressWarnings("resource")
    private <T> T executePostRequest(String path, Object request, Class<T> responseType) {
        HttpPost post;
        try {
            post = new HttpPost(getUri(endpoint, path));
            post.setEntity(new StringEntity(mapper.writeValueAsString(request), ContentType.APPLICATION_JSON));
            HttpResponse response = client.execute(post);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200 && responseType != Void.class) {
                return mapper.readValue(response.getEntity().getContent(), responseType);
            } else if (statusCode == 200) {
                return null;
            } else {
                throw new OftpException(ErrorCode.RESPONSE_NOT_SUCCESS, IOUtil.readFromInputStream(response.getEntity().getContent()));
            }
        } catch (URISyntaxException | UnsupportedCharsetException | IOException e) {
            throw new OftpException(ErrorCode.REQUEST_FAILED, "Request failed: " + e.getMessage());
        }
        
    }

    @SuppressWarnings("resource")
    private <T> T executePostRequest(String path, Object request, TypeReference<T> responseType) {
        HttpPost post;
        try {
            post = new HttpPost(getUri(endpoint, path));
            post.setEntity(new StringEntity(mapper.writeValueAsString(request), ContentType.APPLICATION_JSON));
            HttpResponse response = client.execute(post);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                return mapper.readValue(response.getEntity().getContent(), responseType);
            } else {
                throw new OftpException(ErrorCode.RESPONSE_NOT_SUCCESS, IOUtil.readFromInputStream(response.getEntity().getContent()));
            }
        } catch (URISyntaxException | UnsupportedCharsetException | IOException e) {
            throw new OftpException(ErrorCode.REQUEST_FAILED, "Request failed: " + e.getMessage());
        }
        
    }

    private static URI getUri(String endpoint, String path) throws URISyntaxException {
        return getUri(endpoint, path, null);
    }

    private static URI getUri(String endpoint, String path, Map<String, String> params) throws URISyntaxException {
        URIBuilder uri = new URIBuilder(endpoint);
        uri.setPath(path);
        if (params != null) params.forEach(uri::addParameter);
        return uri.build();
    }
}