package com.aijob.aijobbackend.service;

/**
 * Dify 应用调用服务
 */
public interface DifyService {

    /**
     * 调用 Dify CHATFLOW 应用（blocking 模式），同步等待并返回 answer 文本。
     *
     * @param query 用户输入内容（简历分析场景下为简历文本）
     * @return Dify 返回的 answer 字符串
     * @throws com.aijob.aijobbackend.exception.DifyException 调用失败、超时或返回内容为空时抛出
     */
    String chat(String query);
}
