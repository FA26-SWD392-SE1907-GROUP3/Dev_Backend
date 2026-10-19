package com.example.swd392_se1907_aives.service;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.zip.*;
import static org.assertj.core.api.Assertions.*;

class MaterialTextExtractorTests {
    @TempDir Path directory;
    private final MaterialTextExtractor extractor=new MaterialTextExtractor();
    @Test void extractsTextFromARealPdf() throws Exception {
        Path file=directory.resolve("lesson.pdf");
        try(var document=new PDDocument()){
            var page=new PDPage();document.addPage(page);
            try(var stream=new PDPageContentStream(document,page)){
                stream.beginText();stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA),12);
                stream.newLineAtOffset(50,700);stream.showText("Software architecture separates responsibilities.");stream.endText();
            }
            document.save(file.toFile());
        }
        assertThat(extractor.extract(file,"application/pdf")).contains("Software architecture separates responsibilities.");
    }
    @Test void rejectsPdfWithoutText() throws Exception {
        Path file=directory.resolve("scan.pdf");
        try(var document=new PDDocument()){document.addPage(new PDPage());document.save(file.toFile());}
        assertThatThrownBy(()->extractor.extract(file,"application/pdf")).isInstanceOf(IOException.class).hasMessageContaining("OCR");
    }
    @Test void docxExternalEntitiesAreNotExpanded() throws Exception {
        Path file=directory.resolve("external.docx");
        try(var zip=new ZipOutputStream(Files.newOutputStream(file))){
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write("<!DOCTYPE doc [<!ENTITY external SYSTEM 'file:///should-never-be-read'>]><doc><t>&external;</t></doc>".getBytes(StandardCharsets.UTF_8));zip.closeEntry();
        }
        assertThatThrownBy(()->extractor.extract(file,MaterialService.DOCX)).isInstanceOf(IOException.class);
    }
}
