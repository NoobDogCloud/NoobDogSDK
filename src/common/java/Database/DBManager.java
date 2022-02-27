package common.java.Database;

import common.java.nLogger.nLogger;
import org.json.gsc.JSONArrayStream;
import org.json.gsc.JSONObject;
import org.json.gsc.JSONObjectStream;

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

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
        try (JSONObjectStream builder = new JSONObjectStream(outFile)) {
            for (String tableName : tableNameArr) {
                builder.putJson(tableName, jsonStream -> {
                    jsonStream.put("builder", db.tableBuildMeta(tableName));
                    db.form(tableName).scan(item -> {
                        jsonStream.<JSONObject>putJsonArray("data", jsonArrayStream -> jsonArrayStream.addAll(item));
                        return null;
                    }, 50);
                });
                /*
                JSONObject tableInfo = JSONObject.build()
                        // 构造表创建json
                        .put("builder", db.tableBuildMeta(tableName))
                        // 为每张表构造数据 json
                        .put("data", db.form(tableName).select());
                builder.put(tableName, tableInfo);
                 */
            }
        }
    }

    // 导入数据库
    public void doImport(File inFile) {
        doImport(inFile, true);
    }

    public void doImport(File inFile, boolean isOverwrite) {
        try (JSONObjectStream reader = new JSONObjectStream(inFile)) {
            reader.forEach((tableName, value) -> {
                try (var tableInfo = new JSONObjectStream(value)) {
                    // 创建表
                    try {
                        if (isOverwrite) {
                            db.removeTable(tableName);
                        }
                        db.buildTableFromMeta(tableName, tableInfo.getString("builder"));
                        nLogger.logInfo("创建表" + tableName + "成功");
                    } catch (Exception e) {
                        nLogger.errorInfo("创建表失败：" + tableName);
                    }
                    // 写入数据
                    AtomicInteger count = new AtomicInteger(0);
                    try (JSONArrayStream<JSONObject> result = tableInfo.getJsonArrayStream("data")) {
                        db.form(tableName);
                        result.forEach(item -> {
                            count.addAndGet(db.data(item).insert().size());
                        });
                    } catch (Exception e) {
                        String errorInfo = """
                                表:%s
                                写入数据->失败:%s
                                """.indent(0);
                        nLogger.errorInfo(String.format(errorInfo, tableName, e.getMessage()));
                    }
                    nLogger.logInfo("表" + tableName + "写入数据" + count.get() + "条");
                }

                /*
                JSONObject tableInfo = JSONObject.build(value.toString());
                // 创建表
                try {
                    if (isOverwrite) {
                        db.removeTable(tableName);
                    }
                    db.buildTableFromMeta(tableName, tableInfo.getString("builder"));
                    nLogger.logInfo("创建表" + tableName + "成功");
                } catch (Exception e) {
                    nLogger.errorInfo("创建表失败：" + tableName);
                }
                // 写入数据
                int count = 0;
                try {
                    JSONArray<JSONObject> result = tableInfo.getJsonArray("data");
                    db.form(tableName);
                    for (JSONObject item : result) {
                        db.data(item);
                    }
                    count += db.insert().size();
                } catch (Exception e) {
                    String errorInfo = """
                        表:%s
                        写入数据->失败:%s
                        """.indent(0);
                    nLogger.errorInfo(String.format(errorInfo, tableName, e.getMessage()));
                }
                nLogger.logInfo("表" + tableName + "写入数据" + count + "条");
                 */
            });
        }
    }
}
