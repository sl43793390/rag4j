package com.sl.rag4j.ragservice;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentParser;
import dev.langchain4j.data.document.parser.TextDocumentParser;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.parser.apache.poi.ApachePoiDocumentParser;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.parser.markdown.MarkdownDocumentParser;
import org.springframework.stereotype.Service;

import java.io.InputStream;

/**
 * 文档解析服务，根据文件扩展名选择合适的解析器将文件内容转为LangChain4j Document对象
 * 支持PDF、Office文档、Markdown及其他格式（通过Tika通用解析器）
 */
@Service
public class DocumentParsingService {

    /**
     * 解析上传的文件，根据文件扩展名自动选择对应解析器
     * @param inputStream 文件输入流
     * @param fileName 文件名（用于判断文件类型）
     * @return 解析后的LangChain4j Document对象
     */
    public Document parseFile(InputStream inputStream, String fileName) {
        String extension = getFileExtension(fileName);
        DocumentParser parser = selectParser(extension);
        return parser.parse(inputStream);
    }

    /**
     * 从文件名提取扩展名（不含点号），如"report.pdf"返回"pdf"
     */
    private String getFileExtension(String fileName) {
        if (isSupportedFileType(fileName)){
            int dotIndex = fileName.lastIndexOf('.');
            if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
                return fileName.substring(dotIndex + 1).toLowerCase();
            }
        }
        return "";
    }
    /**
     * 检查文件是否为支持的类型
     * @param fileName 文件路径
     * @return 是否支持
     */
    private boolean isSupportedFileType(String fileName) {
        return fileName.endsWith(".txt") ||
                fileName.endsWith(".html") ||
                fileName.endsWith(".pdf") ||
                fileName.endsWith(".doc") ||
                fileName.endsWith(".docx") ||
                fileName.endsWith(".ppt") ||
                fileName.endsWith(".pptx") ||
                fileName.endsWith(".xls") ||
                fileName.endsWith(".xlsx") ||
                fileName.endsWith(".md");
    }

    /**
     * 根据文件扩展名选择对应的文档解析器
     * PDF使用PdfBox，Office文档使用Apache POI，Markdown使用专用解析器，其他格式使用Tika通用解析
     */
    private DocumentParser selectParser(String extension) {
        switch (extension) {
            case "pdf":
                return new ApachePdfBoxDocumentParser();
            case "doc", "docx", "xls", "xlsx", "ppt", "pptx":
                return new ApachePoiDocumentParser();
            case "md", "markdown":
                return new MarkdownDocumentParser();
            case "txt":
                return new TextDocumentParser();
            default:
                // Tika解析器可处理TXT、HTML、RTF等多种格式
                return new ApacheTikaDocumentParser();
        }
    }
}