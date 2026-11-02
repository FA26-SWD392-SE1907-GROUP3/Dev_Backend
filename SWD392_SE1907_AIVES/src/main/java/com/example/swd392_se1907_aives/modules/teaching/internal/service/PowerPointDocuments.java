package com.example.swd392_se1907_aives.modules.teaching.internal.service;

import org.apache.poi.sl.usermodel.*;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipFile;

/** Parses documents locally; never executes macros or opens external document links. */
public final class PowerPointDocuments {
    public static final int MAX_SLIDES=150;
    private PowerPointDocuments() {}
    static { ZipSecureFile.setMaxEntrySize(32L*1024*1024); }

    public static SlideShow<?,?> open(Path path,String mime) throws IOException {
        if(MaterialService.PPTX.equals(mime)) {
            try(var zip=new ZipFile(path.toFile())) {
                if(zip.getEntry("ppt/presentation.xml")==null || zip.getEntry("[Content_Types].xml")==null)
                    throw new IOException("Tệp không phải PPTX hợp lệ.");
            }
        }
        SlideShow<?,?> show;
        try { show=SlideShowFactory.create(path.toFile()); }
        catch(RuntimeException failure) { throw new IOException("Không đọc được PowerPoint; tệp có thể bị khóa hoặc bị lỗi.",failure); }
        if(show.getSlides().isEmpty()||show.getSlides().size()>MAX_SLIDES) {
            show.close();throw new IOException("PowerPoint cần có từ 1 đến "+MAX_SLIDES+" trang chiếu.");
        }
        var size=show.getPageSize();
        if(size.width<=0||size.height<=0||size.width>10000||size.height>10000) {
            show.close();throw new IOException("Kích thước trang chiếu không hợp lệ.");
        }
        return show;
    }
    public static void validate(Path path,String mime) throws IOException { try(var show=open(path,mime)) { /* Parsing is the validation. */ } }
    public static String text(Path path,String mime) throws IOException {
        try(var show=open(path,mime)) {
            var text=new StringBuilder();int number=0;
            for(var slide:show.getSlides()) {
                var page=new StringBuilder();appendShapes(slide.getShapes(),page);
                number++;
                if(!page.toString().isBlank()) text.append("Trang chiếu ").append(number).append('\n').append(page).append('\n');
                if(text.length()>1_000_000)throw new IOException("PowerPoint có quá nhiều văn bản.");
            }
            return text.toString();
        } catch(RuntimeException failure) { throw new IOException("Không trích xuất được văn bản PowerPoint.",failure); }
    }
    private static void appendShapes(List<? extends Shape<?,?>> shapes,StringBuilder text) throws IOException {
        for(var shape:shapes) {
            if(shape instanceof TextShape<?,?> value) text.append(value.getText()).append('\n');
            else if(shape instanceof TableShape<?,?> table) {
                for(int row=0;row<table.getNumberOfRows();row++)for(int col=0;col<table.getNumberOfColumns();col++) {
                    var cell=table.getCell(row,col);if(cell!=null)text.append(cell.getText()).append('\n');
                }
            } else if(shape instanceof ShapeContainer<?,?> group) appendShapes(group.getShapes(),text);
            if(text.length()>1_000_000)throw new IOException("PowerPoint có quá nhiều văn bản.");
        }
    }
}
