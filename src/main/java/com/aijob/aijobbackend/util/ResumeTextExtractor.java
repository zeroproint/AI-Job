package com.aijob.aijobbackend.util;

import com.aijob.aijobbackend.exception.ResumeParseException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/**
 * 简历文件文本提取工具：支持 PDF、Word（.doc/.docx）和 TXT。
 * <p>
 * 只做纯文本提取，不保留排版；提取结果直接作为 Dify 的输入。
 */
public final class ResumeTextExtractor {

    /** 中文 TXT 常见的非 UTF-8 编码 */
    private static final Charset GB18030 = Charset.forName("GB18030");

    private ResumeTextExtractor() {
    }

    /**
     * 按文件扩展名提取纯文本。
     *
     * @param bytes    文件内容
     * @param fileName 原始文件名（用于判断格式）
     * @return 规整后的文本，保证非空
     * @throws ResumeParseException 格式不支持、文件损坏或无法提取出文本时抛出
     */
    public static String extract(byte[] bytes, String fileName) {
        if (bytes == null || bytes.length == 0) {
            throw new ResumeParseException("简历文件为空");
        }

        String text;
        try {
            text = switch (extensionOf(fileName)) {
                case "pdf" -> extractPdf(bytes);
                case "docx" -> extractDocx(bytes);
                case "doc" -> extractDoc(bytes);
                case "txt" -> decodeText(bytes);
                default -> throw new ResumeParseException("仅支持 PDF、Word（.doc/.docx）和 TXT 格式的简历文件");
            };
        } catch (ResumeParseException e) {
            throw e;
        } catch (Exception e) {
            throw new ResumeParseException("简历文件解析失败，请确认文件未损坏：" + e.getMessage(), e);
        }

        String normalized = normalize(text);
        if (normalized.isEmpty()) {
            throw new ResumeParseException("未能从文件中解析出文本，可能是扫描件或空文档，建议另存为文本后重试");
        }
        return normalized;
    }

    private static String extractPdf(byte[] bytes) throws IOException {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            return new PDFTextStripper().getText(document);
        }
    }

    private static String extractDocx(byte[] bytes) throws IOException {
        try (XWPFDocument document = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            StringBuilder text = new StringBuilder();
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                text.append(paragraph.getText()).append('\n');
            }
            // 表格内容单独追加，避免简历中的表格信息丢失
            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        text.append(cell.getText()).append('\t');
                    }
                    text.append('\n');
                }
            }
            return text.toString();
        }
    }

    private static String extractDoc(byte[] bytes) throws IOException {
        try (HWPFDocument document = new HWPFDocument(new ByteArrayInputStream(bytes));
             WordExtractor extractor = new WordExtractor(document)) {
            return extractor.getText();
        }
    }

    private static String decodeText(byte[] bytes) {
        try {
            CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT);
            return decoder.decode(ByteBuffer.wrap(bytes)).toString();
        } catch (CharacterCodingException e) {
            // UTF-8 解码失败时按国标编码兜底
            return new String(bytes, GB18030);
        }
    }

    /** 统一换行、去掉 BOM/不可见字符并压缩连续空行 */
    private static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace("﻿", "")
                .replace(' ', ' ')
                .replaceAll("[\\t ]+\\n", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private static String extensionOf(String fileName) {
        if (fileName == null) {
            return "";
        }
        String name = fileName.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1);
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
