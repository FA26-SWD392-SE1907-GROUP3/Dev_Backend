import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.net.URI;
import java.security.MessageDigest;
import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

/** Explicit data-maintenance utility. Never starts the application or changes its JPA code. */
public class NeonDataReset {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Set<String> KEEP = Set.of("users", "courses");
    private static String q(String value) { return "\"" + value.replace("\"", "\"\"") + "\""; }
    private static String table(String name) { return "\"public\"." + q(name); }
    private static Properties env(Path path) throws Exception {
        var values = new Properties();
        for (String line : Files.readAllLines(path)) {
            if (line.isBlank() || line.stripLeading().startsWith("#")) continue;
            int equals = line.indexOf('='); if (equals < 1) continue;
            String value = line.substring(equals + 1).trim();
            if (value.length() > 1 && ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'")))) value = value.substring(1,value.length()-1);
            values.setProperty(line.substring(0,equals).replaceFirst("^\\uFEFF", "").trim(), value);
        }
        return values;
    }
    private static String host(Properties env) {
        String url=env.getProperty("DB_URL","");
        if(!url.startsWith("jdbc:postgresql://"))throw new IllegalArgumentException("DB_URL must use jdbc:postgresql://; keep credentials in DB_USERNAME and DB_PASSWORD");
        try {
            URI uri=URI.create(url.substring(5));
            if(uri.getHost()==null||uri.getUserInfo()!=null)throw new IllegalArgumentException();
            return uri.getHost()+(uri.getPort()<0?"":":"+uri.getPort());
        } catch(IllegalArgumentException error) { throw new IllegalArgumentException("Invalid JDBC database URL; keep credentials out of DB_URL"); }
    }
    private static Connection connect(Properties env) throws Exception {
        host(env);
        if(env.getProperty("DB_USERNAME","").isBlank()||env.getProperty("DB_PASSWORD","").isBlank())throw new IllegalArgumentException("DB_USERNAME and DB_PASSWORD are required in the local .env");
        Properties properties = new Properties();
        properties.setProperty("user",env.getProperty("DB_USERNAME")); properties.setProperty("password",env.getProperty("DB_PASSWORD"));
        properties.setProperty("connectTimeout","15"); properties.setProperty("socketTimeout","180");
        properties.setProperty("ApplicationName","AIVES-keep-users-courses-reset");
        var connection = DriverManager.getConnection(env.getProperty("DB_URL"),properties);
        return connection;
    }
    private static void execute(Connection connection, String sql) throws SQLException { try (var statement=connection.createStatement()) { statement.execute(sql); } }
    private static void setup(Connection connection, boolean readOnly) throws SQLException {
        connection.setTransactionIsolation(readOnly ? Connection.TRANSACTION_REPEATABLE_READ : Connection.TRANSACTION_READ_COMMITTED);
        connection.setReadOnly(readOnly); connection.setAutoCommit(false);
        execute(connection,"SET LOCAL TIME ZONE 'UTC'");
        execute(connection,"SET LOCAL DateStyle='ISO, MDY'");
        execute(connection,"SET LOCAL lock_timeout='10s'");
        execute(connection,"SET LOCAL statement_timeout='120s'");
    }
    private static List<String> tables(Connection connection) throws SQLException {
        var names=new ArrayList<String>();
        try(var statement=connection.createStatement();var rows=statement.executeQuery("select tablename from pg_tables where schemaname='public' order by tablename")) { while(rows.next()) names.add(rows.getString(1)); }
        if (!names.containsAll(KEEP)) throw new IllegalStateException("users/courses tables not found");
        return names;
    }
    private static List<String> rows(Connection connection, String name) throws SQLException {
        var values=new ArrayList<String>();
        try(var statement=connection.createStatement();var result=statement.executeQuery("select row_to_json(t)::text from "+table(name)+" t order by row_to_json(t)::text collate \"C\"")) { while(result.next()) values.add(result.getString(1)); }
        return values;
    }
    private static long count(Connection connection,String name) throws SQLException {
        try(var statement=connection.createStatement();var result=statement.executeQuery("select count(*) from "+table(name))) { result.next();return result.getLong(1); }
    }
    private static String digest(byte[] bytes) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
    private static byte[] jsonLines(List<String> rows) { return (rows.isEmpty()?"":String.join("\n",rows)+"\n").getBytes(StandardCharsets.UTF_8); }
    private static String protectedMd5(Connection connection,String name) throws SQLException {
        try(var statement=connection.createStatement();var result=statement.executeQuery("select md5(coalesce(string_agg(row_to_json(t)::text,E'\\n' order by row_to_json(t)::text collate \"C\"),'')) from "+table(name)+" t")) {result.next();return result.getString(1);}
    }
    private static List<String> writableColumns(Connection connection,String name) throws SQLException {
        var columns=new ArrayList<String>();
        try(var statement=connection.prepareStatement("select a.attname from pg_attribute a join pg_class c on c.oid=a.attrelid join pg_namespace n on n.oid=c.relnamespace where n.nspname='public' and c.relname=? and a.attnum>0 and not a.attisdropped and a.attgenerated='' order by a.attnum")) {
            statement.setString(1,name);try(var result=statement.executeQuery()){while(result.next())columns.add(result.getString(1));}
        }
        return columns;
    }
    private static List<Map<String,Object>> foreignKeys(Connection connection) throws SQLException {
        var list=new ArrayList<Map<String,Object>>();
        try(var statement=connection.createStatement();var result=statement.executeQuery("select ns.nspname,child.relname,f.conname,parent_ns.nspname,parent.relname,f.condeferrable,f.condeferred from pg_constraint f join pg_class child on child.oid=f.conrelid join pg_namespace ns on ns.oid=child.relnamespace join pg_class parent on parent.oid=f.confrelid join pg_namespace parent_ns on parent_ns.oid=parent.relnamespace where f.contype='f' and (ns.nspname='public' or parent_ns.nspname='public') order by ns.nspname,child.relname,f.conname")) {
            while(result.next()) {
                String schema=result.getString(1),name=result.getString(2),parentSchema=result.getString(4),parent=result.getString(5);
                if(!schema.equals("public")||!parentSchema.equals("public"))throw new IllegalStateException("Cross-schema foreign key requires review; no reset performed");
                if(KEEP.contains(name)&&!KEEP.contains(parent))throw new IllegalStateException("A protected table references a table to clear; no reset performed");
                list.add(Map.of("table",name,"constraint",result.getString(3),"parent",parent,"deferrable",result.getBoolean(6),"initiallyDeferred",result.getBoolean(7)));
            }
        }
        return list;
    }
    private static void guardTriggers(Connection connection) throws SQLException {
        try(var statement=connection.createStatement();var result=statement.executeQuery("select count(*) from pg_trigger t join pg_class c on c.oid=t.tgrelid join pg_namespace n on n.oid=c.relnamespace where n.nspname='public' and not t.tgisinternal and t.tgenabled<>'D' and (t.tgtype::integer & 32)<>0")) {
            result.next();if(result.getLong(1)>0)throw new IllegalStateException("TRUNCATE triggers require review; no reset performed");
        }
    }
    private static String joinedTables(List<String> names) { return String.join(", ",names.stream().map(NeonDataReset::table).toList()); }
    private static void preview(Properties env) throws Exception {
        try(Connection connection=connect(env)) {
            setup(connection,true);
            var names=tables(connection);foreignKeys(connection);guardTriggers(connection);
            long clearRows=0;int clearTables=0;
            System.out.println("HOST="+host(env));System.out.println("DATABASE="+connection.getCatalog());
            for(String name:names) {
                long rows=count(connection,name);
                System.out.println((KEEP.contains(name)?"KEEP ":"CLEAR ")+"public."+name+" rows="+rows);
                if(!KEEP.contains(name)){clearTables++;clearRows+=rows;}
            }
            System.out.println("TABLES_TO_CLEAR="+clearTables);System.out.println("ROWS_TO_CLEAR="+clearRows);
            System.out.println("PREVIEW_ONLY_NO_DATA_CHANGED");connection.rollback();
        }
    }
    private static void backup(Properties env,Path parent) throws Exception {
        Files.createDirectories(parent);
        Path destination=parent.resolve("before-reset-"+LocalDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))+"-"+UUID.randomUUID().toString().substring(0,8));
        Files.createDirectory(destination);
        try(Connection connection=connect(env)) {
            setup(connection,true);
            var names=tables(connection);var foreignKeys=foreignKeys(connection);guardTriggers(connection);
            var entries=new ArrayList<Map<String,Object>>();
            var restore=new StringBuilder("-- Restore cleared AIVES application data only. Users/courses must match this backup.\n-- Run manually only if restoration is desired. Contains private application data.\nBEGIN;\nSET LOCAL TIME ZONE 'UTC';\nSET LOCAL DateStyle='ISO, MDY';\nSET LOCAL lock_timeout='10s';\nLOCK TABLE "+joinedTables(names)+" IN ACCESS EXCLUSIVE MODE;\n");
            for(String name:KEEP) {
                String fingerprint=protectedMd5(connection,name);
                restore.append("DO $protected$ BEGIN IF (SELECT md5(coalesce(string_agg(row_to_json(t)::text,E'\\n' ORDER BY row_to_json(t)::text COLLATE \"C\"),'')) FROM ").append(table(name)).append(" t) <> '").append(fingerprint).append("' THEN RAISE EXCEPTION 'Protected ").append(name).append(" changed; restoration stopped'; END IF; END $protected$;\n");
            }
            for(var foreignKey:foreignKeys) if(!KEEP.contains((String)foreignKey.get("table"))) restore.append("ALTER TABLE ").append(table((String)foreignKey.get("table"))).append(" ALTER CONSTRAINT ").append(q((String)foreignKey.get("constraint"))).append(" DEFERRABLE INITIALLY DEFERRED;\n");
            restore.append("SET CONSTRAINTS ALL DEFERRED;\n");
            var cleared=names.stream().filter(name->!KEEP.contains(name)).toList();
            restore.append("TRUNCATE TABLE ").append(joinedTables(cleared)).append(" CONTINUE IDENTITY RESTRICT;\n");
            for(String name:names) {
                var data=rows(connection,name);byte[] bytes=jsonLines(data);String file=name+".jsonl";
                Files.write(destination.resolve(file),bytes,StandardOpenOption.CREATE_NEW);
                var columns=writableColumns(connection,name);
                entries.add(Map.of("table",name,"keep",KEEP.contains(name),"rows",data.size(),"file",file,"sha256",digest(bytes),"columns",columns));
                if(!KEEP.contains(name)&&!data.isEmpty()) {
                    String delimiter="$backup_"+UUID.randomUUID().toString().replace("-","")+"$";
                    String payload="[\n"+String.join(",\n",data)+"\n]";
                    if(payload.contains(delimiter))throw new IllegalStateException("Unexpected SQL delimiter collision");
                    String selected=String.join(", ",columns.stream().map(NeonDataReset::q).toList());
                    restore.append("INSERT INTO ").append(table(name)).append(" (").append(selected).append(") OVERRIDING SYSTEM VALUE SELECT ").append(selected).append(" FROM json_populate_recordset(NULL::").append(table(name)).append(", ").append(delimiter).append(payload).append(delimiter).append("::json);\n");
                }
            }
            restore.append("SET CONSTRAINTS ALL IMMEDIATE;\n");
            for(var foreignKey:foreignKeys) if(!KEEP.contains((String)foreignKey.get("table"))) {
                restore.append("ALTER TABLE ").append(table((String)foreignKey.get("table"))).append(" ALTER CONSTRAINT ").append(q((String)foreignKey.get("constraint")));
                restore.append(Boolean.TRUE.equals(foreignKey.get("deferrable"))?(Boolean.TRUE.equals(foreignKey.get("initiallyDeferred"))?" DEFERRABLE INITIALLY DEFERRED":" DEFERRABLE INITIALLY IMMEDIATE"):" NOT DEFERRABLE").append(";\n");
            }
            restore.append("COMMIT;\n");
            Path restorePath=destination.resolve("restore-cleared-data.sql");Files.writeString(restorePath,restore.toString(),StandardOpenOption.CREATE_NEW);
            var manifest=Map.of("formatVersion",1,"host",host(env),"database",connection.getCatalog(),"createdAt",Instant.now().toString(),"schema","public","tables",entries,"foreignKeys",foreignKeys,"restoreSqlSha256",digest(Files.readAllBytes(restorePath)));
            JSON.writerWithDefaultPrettyPrinter().writeValue(destination.resolve("manifest.json").toFile(),manifest);
            connection.rollback();
            Files.writeString(parent.resolve("latest-backup-path.txt"),destination.toAbsolutePath().toString());
            System.out.println("BACKUP_COMPLETE="+destination.toAbsolutePath());
            System.out.println("APPLICATION_TABLES="+entries.size());
            System.out.println("TABLES_TO_CLEAR="+cleared.size());
            System.out.println("ROWS_TO_CLEAR="+entries.stream().filter(entry->!Boolean.TRUE.equals(entry.get("keep"))).mapToLong(entry->((Number)entry.get("rows")).longValue()).sum());
            for(var entry:entries)if(Boolean.TRUE.equals(entry.get("keep")))System.out.println("KEEP "+entry.get("table")+" rows="+entry.get("rows"));
        }
    }
    private static void reset(Properties env,Path backup) throws Exception {
        var manifest=JSON.readTree(backup.resolve("manifest.json").toFile());
        if(manifest.path("formatVersion").asInt()!=1||!host(env).equals(manifest.path("host").asText())||!"public".equals(manifest.path("schema").asText()))throw new IllegalStateException("Backup target does not match");
        var entries=new TreeMap<String,JsonNode>();
        for(var entry:manifest.path("tables")) {
            String name=entry.path("table").asText();
            if(!name.matches("[A-Za-z_][A-Za-z0-9_]*")||!entry.path("file").asText().equals(name+".jsonl"))throw new IllegalStateException("Invalid backup path");
            byte[] bytes=Files.readAllBytes(backup.resolve(entry.path("file").asText()));
            if(!digest(bytes).equals(entry.path("sha256").asText()))throw new IllegalStateException("Backup checksum mismatch: "+name);
            long count=0;for(String line:new String(bytes,StandardCharsets.UTF_8).split("\n"))if(!line.isEmpty()){if(!JSON.readTree(line).isObject())throw new IllegalStateException("Invalid backup row");count++;}
            if(count!=entry.path("rows").asLong()||entry.path("keep").asBoolean()!=KEEP.contains(name))throw new IllegalStateException("Invalid backup manifest");
            entries.put(name,entry);
        }
        if(!digest(Files.readAllBytes(backup.resolve("restore-cleared-data.sql"))).equals(manifest.path("restoreSqlSha256").asText()))throw new IllegalStateException("Restore script checksum mismatch");
        boolean committed=false;
        try(Connection connection=connect(env)) {
            try {
                setup(connection,false);
                if(!connection.getCatalog().equals(manifest.path("database").asText()))throw new IllegalStateException("Backup database does not match the current connection");
                var names=tables(connection);
                if(!names.equals(new ArrayList<>(entries.keySet())))throw new IllegalStateException("Application table list changed since backup");
                execute(connection,"LOCK TABLE "+joinedTables(names)+" IN ACCESS EXCLUSIVE MODE");
                foreignKeys(connection);guardTriggers(connection);
                for(String name:names)if(!digest(jsonLines(rows(connection,name))).equals(entries.get(name).path("sha256").asText()))throw new IllegalStateException("Data changed since backup; reset cancelled: "+name);
                var cleared=names.stream().filter(name->!KEEP.contains(name)).toList();
                if(cleared.isEmpty())throw new IllegalStateException("No tables to clear");
                execute(connection,"TRUNCATE TABLE "+joinedTables(cleared)+" CONTINUE IDENTITY RESTRICT");
                var finalCounts=new TreeMap<String,Long>();
                for(String name:names) {
                    long count=count(connection,name);finalCounts.put(name,count);
                    if(KEEP.contains(name)) {
                        if(!digest(jsonLines(rows(connection,name))).equals(entries.get(name).path("sha256").asText()))throw new IllegalStateException("Protected data changed; rolling back");
                    } else if(count!=0)throw new IllegalStateException("Table not empty; rolling back: "+name);
                }
                connection.commit();committed=true;
                System.out.println("RESET_COMMITTED");
                System.out.println("KEPT_USERS="+finalCounts.get("users"));System.out.println("KEPT_COURSES="+finalCounts.get("courses"));
                System.out.println("CLEARED_TABLES="+cleared.size());System.out.println("PROTECTED_DATA_HASHES_UNCHANGED=true");
                var result=Map.of("committedAt",Instant.now().toString(),"host",host(env),"database",connection.getCatalog(),"counts",finalCounts,"clearedTables",cleared,"protectedHashesUnchanged",true,"backup",backup.toAbsolutePath().toString());
                JSON.writerWithDefaultPrettyPrinter().writeValue(backup.resolve("reset-result.json").toFile(),result);
            } catch(Exception failure) {
                if(!committed)connection.rollback();
                throw failure;
            }
        }
    }
    public static void main(String[] args) {
        Properties credentials=new Properties();
        try {
            if(args.length<2)throw new IllegalArgumentException("Expected mode and local environment file");
            credentials=env(Path.of(args[1]));
            if(args[0].equals("preview")&&args.length==2)preview(credentials);
            else if(args[0].equals("backup")&&args.length==3)backup(credentials,Path.of(args[2]));
            else if(args[0].equals("reset")&&args.length==4&&args[3].equals("--confirm-keep-users-courses"))reset(credentials,Path.of(args[2]));
            else throw new IllegalArgumentException("Reset requires a completed backup and explicit confirmation flag");
        } catch(Exception failure) {
            Throwable cause=failure;while(cause.getCause()!=null&&cause.getCause()!=cause)cause=cause.getCause();
            String message=String.valueOf(cause.getMessage());
            for(String name:credentials.stringPropertyNames()) {
                String value=credentials.getProperty(name);
                if(value!=null&&value.length()>=4)message=message.replace(value,"<redacted>");
            }
            System.err.println("RESET_TOOL_FAILED "+cause.getClass().getSimpleName()+": "+message);
            System.exit(1);
        }
    }
}
