package com.example.swd392_se1907_aives.service;

import com.fasterxml.jackson.databind.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import java.nio.*;
import java.nio.charset.*;
import java.util.*;

@Component @RequiredArgsConstructor
public class QuestionImportParser {
    private final ObjectMapper json;
    public record Row(int row,String content,String referenceAnswer,String bloomLevel,String difficulty,String rubricId){}
    public List<Row> parse(String filename,byte[] bytes){
        if(bytes.length==0||bytes.length>2*1024*1024)throw invalid("Tệp phải có nội dung và không vượt 2 MB.");
        String text;
        try{text=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString().replaceFirst("^\uFEFF","");}
        catch(CharacterCodingException e){throw invalid("Lưu tệp dưới dạng UTF-8 trước khi import.");}
        try{
            List<Row> result=new ArrayList<>();
            if(filename.toLowerCase(Locale.ROOT).endsWith(".json")){
                JsonNode items=json.readerFor(JsonNode.class).with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS).readValue(text);
                if(items==null||!items.isArray())throw invalid("JSON phải là một mảng câu hỏi.");
                if(items.size()>200)throw invalid("Mỗi lần import tối đa 200 câu hỏi.");
                for(var item:items){if(!item.isObject())throw invalid("Mỗi phần tử JSON phải là một câu hỏi.");int line=result.size()+1;result.add(new Row(line,value(item,"content"),value(item,"referenceAnswer"),value(item,"bloomLevel"),value(item,"difficulty"),value(item,"rubricId")));}
            }else if(filename.toLowerCase(Locale.ROOT).endsWith(".csv")){
                List<List<String>> records=csv(text);if(records.size()<2)throw invalid("CSV cần hàng tiêu đề và ít nhất một câu hỏi.");
                var headers=records.getFirst().stream().map(String::strip).toList();
                if(new HashSet<>(headers).size()!=headers.size()||!headers.containsAll(List.of("content","bloomLevel","difficulty")))throw invalid("CSV cần các cột content, bloomLevel, difficulty; tiêu đề không được trùng.");
                for(int i=1;i<records.size();i++){
                    var fields=records.get(i);if(fields.size()==1&&fields.getFirst().isBlank())continue;
                    if(fields.size()!=headers.size())throw invalid("Dòng "+(i+1)+": số cột không khớp tiêu đề.");
                    Map<String,String> item=new HashMap<>();for(int j=0;j<headers.size();j++)item.put(headers.get(j),fields.get(j));
                    result.add(new Row(i+1,item.getOrDefault("content",""),item.getOrDefault("referenceAnswer",""),item.getOrDefault("bloomLevel",""),item.getOrDefault("difficulty",""),item.getOrDefault("rubricId","")));
                }
            }else throw invalid("Chỉ hỗ trợ CSV UTF-8 hoặc JSON theo mẫu.");
            if(result.isEmpty()||result.size()>200)throw invalid("Mỗi lần import từ 1 đến 200 câu hỏi.");
            return result;
        }catch(ResponseStatusException e){throw e;}catch(Exception e){throw invalid("Không đọc được tệp. Kiểm tra định dạng CSV/JSON theo mẫu.");}
    }
    private String value(JsonNode item,String key){var value=item.get(key);return value==null||value.isNull()?"":value.isTextual()?value.asText():"";}
    // RFC 4180 fields, including commas, escaped quotes and newlines inside quoted content.
    private List<List<String>> csv(String text){
        List<List<String>> rows=new ArrayList<>();List<String> row=new ArrayList<>();StringBuilder field=new StringBuilder();boolean quoted=false,closed=false;
        for(int i=0;i<text.length();i++){
            char c=text.charAt(i);
            if(quoted){if(c=='"'){if(i+1<text.length()&&text.charAt(i+1)=='"'){field.append('"');i++;}else{quoted=false;closed=true;}}else field.append(c);continue;}
            if(c==','||c=='\n'||c=='\r'){
                row.add(field.toString());field.setLength(0);closed=false;
                if(c!=','){rows.add(row);row=new ArrayList<>();if(rows.size()>202)throw invalid("Mỗi lần import tối đa 200 câu hỏi.");if(c=='\r'&&i+1<text.length()&&text.charAt(i+1)=='\n')i++;}
            }else if(c=='"'){if(field.length()>0||closed)throw invalid("CSV có dấu nháy sai định dạng.");quoted=true;}
            else {if(closed)throw invalid("CSV có ký tự ngoài dấu nháy đóng.");field.append(c);}
        }
        if(quoted)throw invalid("CSV chưa đóng dấu nháy.");
        if(field.length()>0||closed||!row.isEmpty()){row.add(field.toString());rows.add(row);}return rows;
    }
    private ResponseStatusException invalid(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
}
