package common.java.Database;

import common.java.File.FileText;
import common.java.nLogger.nLogger;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.io.File;

public class DBManager {
    private final DBLayer db;

    private DBManager(DBLayer dbLayer) {
        this.db = dbLayer;
    }

    public static DBManager getInstance(DBLayer dbLayer) {
        return new DBManager(dbLayer);
    }

    // 导出数据库
    public void doExport(File outFile) {
        doExport(db.getAllTables(), outFile);
    }

    public void doExport(String[] tableNameArr, File outFile) {
        JSONObject builder = new JSONObject();
        for (String tableName : tableNameArr) {
            JSONObject tableInfo = JSONObject.build()
                    // 构造表创建json
                    .put("builder", db.tableBuildMeta(tableName))
                    // 为每张表构造数据 json
                    .put("data", db.form(tableName).select());
            builder.put(tableName, tableInfo);
        }
        if (!JSONObject.isInvalided(builder)) {
            FileText.build(outFile).write(builder.toString());
        }
    }

    // 导入数据库
    public void doImport(File inFile) {
        JSONObject json = JSONObject.toJSON(FileText.build(inFile).readString());
        for (String tableName : json.keySet()) {
            JSONObject tableInfo = json.getJson(tableName);
            // 创建表
            try {
                db.buildTableFromMeta(tableName, tableInfo.getString("builder"));
            } catch (Exception e) {
                nLogger.errorInfo("创建表失败：" + tableName);
            }

            // 写入数据
            try {
                JSONArray<JSONObject> result = tableInfo.getJsonArray("data");
                db.form(tableName);
                for (JSONObject item : result) {
                    db.data(item);
                }
                db.insert();
            } catch (Exception e) {
                String errorInfo = """
                        表:%s
                        写入数据->失败:%s
                        """.indent(0);
                nLogger.errorInfo(String.format(errorInfo, tableName, e.getMessage()));
            }
        }
    }
}
