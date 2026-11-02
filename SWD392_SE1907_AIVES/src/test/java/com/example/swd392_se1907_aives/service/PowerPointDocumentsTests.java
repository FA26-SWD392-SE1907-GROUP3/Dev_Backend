package com.example.swd392_se1907_aives.service;

import com.example.swd392_se1907_aives.modules.teaching.internal.service.*;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.io.*;
import java.awt.geom.Rectangle2D;
import java.util.zip.*;
import static org.assertj.core.api.Assertions.*;

class PowerPointDocumentsTests {
    @TempDir Path directory;
    @Test void pptAndPptxTextCanBeExtractedAndRenderedToPdf()throws Exception{
        for(boolean modern:new boolean[]{false,true}){
            Path input=directory.resolve(modern?"lecture.pptx":"lecture.ppt");
            if(modern)try(var show=new XMLSlideShow()){
                var text=show.createSlide().createTextBox();text.setAnchor(new Rectangle2D.Double(40,40,600,120));text.setText("Tài liệu kiến trúc phần mềm");
                var next=show.createSlide().createTextBox();next.setAnchor(new Rectangle2D.Double(40,40,600,120));next.setText("Second slide: responsibilities");
                try(var out=Files.newOutputStream(input)){show.write(out);}
            }else try(var show=new HSLFSlideShow()){
                var text=show.createSlide().createTextBox();text.setAnchor(new Rectangle2D.Double(40,40,600,120));text.setText("Tài liệu kiến trúc phần mềm");
                var next=show.createSlide().createTextBox();next.setAnchor(new Rectangle2D.Double(40,40,600,120));next.setText("Second slide: responsibilities");
                try(var out=Files.newOutputStream(input)){show.write(out);}
            }
            String mime=modern?MaterialService.PPTX:MaterialService.PPT;
            assertThat(new MaterialTextExtractor().extract(input,mime)).contains("Tài liệu kiến trúc phần mềm","Second slide: responsibilities");
            byte[] preview=new MaterialPreviewRenderer().renderPowerPoint(input,mime);
            try(var document=Loader.loadPDF(preview)){
                assertThat(document.getNumberOfPages()).isEqualTo(2);
                assertThat(document.getPage(0).getResources().getXObjectNames().iterator().hasNext()).isTrue();
                var image=new PDFRenderer(document).renderImageWithDPI(0,90);
                int titlePixels=0,displacedPixels=0;
                for(int y=0;y<image.getHeight();y++)for(int x=0;x<image.getWidth();x++){
                    int color=image.getRGB(x,y)&0xffffff;
                    if(((color>>16)&255)<120&&((color>>8)&255)<120&&(color&255)<120){
                        if(y<120&&x<500)titlePixels++;else displacedPixels++;
                    }
                }
                assertThat(titlePixels).as("Title must be rendered at its original top-left anchor").isGreaterThan(100);
                assertThat(displacedPixels).as("No mirrored or rotated title elsewhere on the slide").isLessThan(20);
                if(modern){Files.createDirectories(Path.of("target"));javax.imageio.ImageIO.write(image,"png",Path.of("target","powerpoint-preview-proof.png").toFile());}
            }
        }
    }
    @Test void presentationWithoutTextCanBeUploadedAndViewedButNotIndexedForAi()throws Exception{
        Path input=directory.resolve("visual.pptx");
        try(var show=new XMLSlideShow()){show.createSlide();try(var out=Files.newOutputStream(input)){show.write(out);}}
        var extractor=new MaterialTextExtractor();extractor.validate(input,MaterialService.PPTX);
        assertThat(new MaterialPreviewRenderer().renderPowerPoint(input,MaterialService.PPTX)).isNotEmpty();
        assertThatThrownBy(()->extractor.extract(input,MaterialService.PPTX)).isInstanceOf(IOException.class);
    }
    @Test void unrelatedZipAndOversizedPresentationsAreRejected()throws Exception{
        Path unrelated=directory.resolve("not-slides.pptx");
        try(var zip=new ZipOutputStream(Files.newOutputStream(unrelated))){zip.putNextEntry(new ZipEntry("word/document.xml"));zip.write("text".getBytes());zip.closeEntry();}
        assertThatThrownBy(()->PowerPointDocuments.validate(unrelated,MaterialService.PPTX)).isInstanceOf(IOException.class);
        Path many=directory.resolve("too-many.pptx");
        try(var show=new XMLSlideShow()){for(int i=0;i<151;i++)show.createSlide();try(var out=Files.newOutputStream(many)){show.write(out);}}
        assertThatThrownBy(()->PowerPointDocuments.validate(many,MaterialService.PPTX)).isInstanceOf(IOException.class).hasMessageContaining("150");
    }
}
