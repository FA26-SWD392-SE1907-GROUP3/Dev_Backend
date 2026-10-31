package com.example.swd392_se1907_aives.modules.teaching.internal.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Path;
import java.util.zip.ZipFile;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

@Component
public class MaterialTextExtractor {
    private static final int MAX_TEXT = 1_000_000;
    private static final int MAX_XML = 10_000_000;

    public String extract(Path path, String mimeType) throws IOException {
        String text = "application/pdf".equals(mimeType) ? pdf(path) : docx(path);
        text = text.replace('\u0000', ' ').replaceAll("[\\t ]+", " ").strip();
        if (text.isBlank())
            throw new IOException(
                    "Tài liệu không có văn bản. PDF ảnh cần được OCR trước khi tải lên.");
        if (text.length() > MAX_TEXT)
            throw new IOException("Tài liệu có quá nhiều văn bản (tối đa 1.000.000 ký tự).");
        return text;
    }

    private String pdf(Path path) throws IOException {
        try (var document = Loader.loadPDF(path.toFile())) {
            if (document.isEncrypted()
                    || !document.getCurrentAccessPermission().canExtractContent())
                throw new IOException("PDF bị khóa, không thể trích xuất văn bản.");
            if (document.getNumberOfPages() > 300)
                throw new IOException("PDF chỉ được tối đa 300 trang.");
            StringBuilder text = new StringBuilder();
            Writer writer =
                    new Writer() {
                        public void write(char[] chars, int offset, int length) throws IOException {
                            if (text.length() + length > MAX_TEXT)
                                throw new IOException("PDF có quá nhiều văn bản.");
                            text.append(chars, offset, length);
                        }

                        public void flush() {}

                        public void close() {}
                    };
            new PDFTextStripper().writeText(document, writer);
            return text.toString();
        }
    }

    private String docx(Path path) throws IOException {
        try (var zip = new ZipFile(path.toFile())) {
            var entry = zip.getEntry("word/document.xml");
            if (entry == null) throw new IOException("Tệp không phải DOCX hợp lệ.");
            byte[] bytes;
            try (var input = zip.getInputStream(entry)) {
                bytes = input.readNBytes(MAX_XML + 1);
            }
            if (bytes.length > MAX_XML) throw new IOException("Nội dung DOCX vượt giới hạn xử lý.");
            XMLInputFactory factory = XMLInputFactory.newFactory();
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            factory.setProperty("javax.xml.stream.isSupportingExternalEntities", false);
            var reader = factory.createXMLStreamReader(new ByteArrayInputStream(bytes));
            StringBuilder text = new StringBuilder();
            try {
                while (reader.hasNext()) {
                    int event = reader.next();
                    if (event == XMLStreamConstants.DTD)
                        throw new IOException("DOCX chứa XML không hợp lệ.");
                    if (event == XMLStreamConstants.START_ELEMENT
                            && "t".equals(reader.getLocalName()))
                        text.append(reader.getElementText());
                    else if (event == XMLStreamConstants.END_ELEMENT
                            && "p".equals(reader.getLocalName())) text.append('\n');
                    if (text.length() > MAX_TEXT)
                        throw new IOException("DOCX có quá nhiều văn bản.");
                }
            } finally {
                reader.close();
            }
            return text.toString();
        } catch (XMLStreamException e) {
            throw new IOException("Không đọc được nội dung DOCX.", e);
        }
    }
}
