package com.example.swd392_se1907_aives.modules.teaching.internal.service;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.springframework.stereotype.Component;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Path;
import java.util.concurrent.Semaphore;

@Component
public class MaterialPreviewRenderer {
    private final Semaphore slots=new Semaphore(2);
    public byte[] renderPowerPoint(Path path,String mime) throws IOException {
        if(!slots.tryAcquire())throw new IOException("Đang có nhiều yêu cầu xem tài liệu. Hãy thử lại sau.");
        try(var slides=PowerPointDocuments.open(path,mime);var pdf=new PDDocument()) {
            var size=slides.getPageSize();double scale=Math.min(2d,1600d/Math.max(size.width,size.height));
            for(var slide:slides.getSlides()) {
                var image=new BufferedImage(Math.max(1,(int)Math.ceil(size.width*scale)),Math.max(1,(int)Math.ceil(size.height*scale)),BufferedImage.TYPE_INT_RGB);
                var graphics=image.createGraphics();
                try {
                    graphics.setColor(Color.WHITE);graphics.fillRect(0,0,image.getWidth(),image.getHeight());
                    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
                    graphics.setRenderingHint(RenderingHints.KEY_RENDERING,RenderingHints.VALUE_RENDER_QUALITY);
                    graphics.scale(scale,scale);slide.draw(graphics);
                } finally { graphics.dispose(); }
                var page=new PDPage(new PDRectangle(size.width,size.height));pdf.addPage(page);
                try(var stream=new PDPageContentStream(pdf,page)) {stream.drawImage(JPEGFactory.createFromImage(pdf,image,.9f),0,0,size.width,size.height);}
                image.flush();
            }
            var output=new LimitedOutput();pdf.save(output);return output.bytes.toByteArray();
        } catch(RuntimeException failure) {throw new IOException("Không tạo được bản xem trước PowerPoint.",failure);}
        finally {slots.release();}
    }
    private static final class LimitedOutput extends OutputStream {
        final ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        private void check(int added) throws IOException {if((long)bytes.size()+added>40L*1024*1024)throw new IOException("Bản xem trước vượt 40 MB.");}
        @Override public void write(int value) throws IOException {check(1);bytes.write(value);}
        @Override public void write(byte[] values,int start,int count) throws IOException {check(count);bytes.write(values,start,count);}
    }
}
