package com.aijob.aijobbackend.service.impl;

import com.aijob.aijobbackend.exception.DifyException;
import com.aijob.aijobbackend.service.DifyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Dify CHATFLOW 应用调用实现。
 * <p>
 * 请求走 blocking（非流式）模式，Dify 会一次性返回完整结果，答案在 answer 字段。
 * 响应体统一用 Map 接收，避免绑定具体 DTO 时受 Dify 返回字段变动影响。
 */
@Slf4j
@Service
public class DifyServiceImpl implements DifyService {

    private static final String RESPONSE_MODE_BLOCKING = "blocking";
    /** 日志中错误响应体的最大截取长度，避免刷屏 */
    private static final int LOG_BODY_MAX_LENGTH = 500;
    /** block() 等待时间比 WebClient 的响应超时略长，保证由 WebClient 先触发超时 */
    private static final long BLOCK_EXTRA_SECONDS = 10L;

    private final WebClient webClient;
    private final String apiUrl;
    private final String apiKey;
    private final String user;
    private final long timeoutSeconds;

    public DifyServiceImpl(@Qualifier("difyWebClient") WebClient webClient,
                           @Value("${dify.api-url}") String apiUrl,
                           @Value("${dify.api-key}") String apiKey,
                           @Value("${dify.user:ai-job-backend}") String user,
                           @Value("${dify.timeout-seconds:120}") long timeoutSeconds) {
        this.webClient = webClient;
        this.apiUrl = apiUrl;
        this.apiKey = apiKey;
        this.user = user;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    public String chat(String query) {
        if (!StringUtils.hasText(query)) {
            throw new IllegalArgumentException("Dify 调用内容不能为空");
        }

        Map<String, Object> requestBody = new HashMap<>(5);
        // 若 Dify 应用定义了自定义输入变量，需在此填入；当前应用未定义，传空对象
        requestBody.put("inputs", Map.of());
        requestBody.put("query", query);
        requestBody.put("response_mode", RESPONSE_MODE_BLOCKING);
        requestBody.put("conversation_id", "");
        requestBody.put("user", user);

        long start = System.currentTimeMillis();
        log.info("调用 Dify 开始，queryLength={}", query.length());

        try {
            Map<String, Object> response = webClient.post()
                    .uri(apiUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .onStatus(status -> status.isError(), clientResponse -> clientResponse
                            .bodyToMono(String.class)
                            .defaultIfEmpty("")
                            .flatMap(body -> Mono.error(new DifyException(
                                    "Dify 接口返回错误，HTTP " + clientResponse.statusCode().value()
                                            + "，body=" + abbreviate(body)))))
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .block(Duration.ofSeconds(timeoutSeconds + BLOCK_EXTRA_SECONDS));

            long cost = System.currentTimeMillis() - start;
            if (response == null) {
                throw new DifyException("Dify 返回内容为空");
            }

            Object answer = response.get("answer");
            if (answer == null || !StringUtils.hasText(answer.toString())) {
                throw new DifyException("Dify 未返回 answer，event=" + response.get("event")
                        + "，message=" + response.get("message"));
            }

            String answerText = answer.toString();
            log.info("调用 Dify 成功，cost={}ms，answerLength={}", cost, answerText.length());
            return answerText;

        } catch (DifyException e) {
            log.error("调用 Dify 失败，cost={}ms，原因：{}", System.currentTimeMillis() - start, e.getMessage(), e);
            throw e;
        } catch (WebClientRequestException e) {
            // 连接被拒绝、网络中断、读响应超时等
            log.error("调用 Dify 网络异常（连接失败或超时），cost={}ms", System.currentTimeMillis() - start, e);
            throw new DifyException("调用 Dify 网络异常：" + e.getMessage(), e);
        } catch (IllegalStateException e) {
            // Reactor block() 等待超时
            log.error("调用 Dify 阻塞等待超时，cost={}ms", System.currentTimeMillis() - start, e);
            throw new DifyException("调用 Dify 超时（超过 " + timeoutSeconds + " 秒）", e);
        } catch (Exception e) {
            log.error("调用 Dify 发生未知异常，cost={}ms", System.currentTimeMillis() - start, e);
            throw new DifyException("调用 Dify 失败：" + e.getMessage(), e);
        }
    }

    private static String abbreviate(String text) {
        if (text == null) {
            return "";
        }
        return text.length() <= LOG_BODY_MAX_LENGTH ? text : text.substring(0, LOG_BODY_MAX_LENGTH) + "...";
    }
}
