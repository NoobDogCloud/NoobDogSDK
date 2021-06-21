package common.java.Database;

import com.zaxxer.hikari.HikariDataSource;
import common.java.String.StringHelper;
import common.java.Time.TimeHelper;
import common.java.nLogger.nLogger;
import org.json.gsc.JSONArray;
import org.json.gsc.JSONObject;

import java.io.BufferedReader;
import java.io.Reader;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Oracle extends Sql {
    private List<List<Object>> conditionJSON_backup;    //条件

    public Oracle(String configString) {
        super(configString);
    }

    protected void reinit() {
        conditionJSON_backup = new ArrayList<>();
        super.reinit();
    }

    protected void initsql() {
        JSONObject obj;
        String user;
        String password;
        String sid;
        int minIdle;
        int maxWait;
        int maxActive;
        String className;

        try {
            obj = JSONObject.toJSON(_configString);
            className = obj.getString("class");
            user = obj.getString("user");
            password = obj.getString("password");
            sid = obj.getString("sid");
            String url = "oracle:thin:@" + obj.getString("host") + ":" + sid;
            minIdle = obj.getInt("minidle");
            maxWait = obj.getInt("maxwait");
            maxActive = obj.getInt("maxactive");

            HikariDataSource ds = DataSource.getOrDefault(_configString, null);
            if (ds == null || ds.isClosed() || !ds.isRunning()) {
                ds = new HikariDataSource();
                if (!user.equals("")) {
                    ds.setUsername(user);
                    ds.setPassword(password);
                    ds.setConnectionTestQuery("select 1");
                    ds.setMaximumPoolSize(maxActive);
                    ds.setKeepaliveTime(7200);
                    ds.setMaxLifetime(1800000);
                    ds.setMinimumIdle(minIdle);
                    ds.setMaxLifetime(maxWait);
                    ds.setJdbcUrl("jdbc:" + url);
                    ds.setLoginTimeout(5);
                    if (!StringHelper.isInvalided(className)) {
                        ds.setDriverClassName(className);
                    }
                }
                DataSource.put(_configString, ds);
            }
            //nLogger.logInfo("[SQL]nowCnt:" + ds.getActiveCount() + " closedCnt:" + ds.getCloseCount() + " connectCnt:" + ds.getConnectCount() + " connectErrorCnt:" + ds.getConnectErrorCount());
            dataSource = ds;
        } catch (Exception e) {
            nLogger.logInfo(e, "Sql server Config node error!");
            nLogger.logInfo("Config:" + _configString);
        }
        tableFields = new ArrayList<>();
    }

    private void appendOracleConds() {
        if (conditionJSON_backup.size() > 0) {
            conditionJSON.clear();
            conditionJSON.addAll(conditionJSON_backup);
        } else {
            conditionJSON_backup.addAll(conditionJSON);
        }
        // 发现 skipNo 补充限制条件
        if (skipNo > 0) {
            gt((limitNo > 0 ? "rn" : "rownum "), skipNo);
        } else if (limitNo > 0) {
            lte("rownum ", limitNo);
        }
    }

    protected String whereSQL() {//不支持自由条件，必须严格区分and和or2个组
        StringBuilder rString = new StringBuilder();
        appendOracleConds();
        if (conditionJSON.size() > 0) {
            int cnt = 0;
            for (List<Object> item : conditionJSON) {
                rString.append(whereSQL(item, cnt == 0));
                cnt++;
            }

            rString.insert(0, " where ");
        }
        return rString.toString();
    }

    private String getfullformSQL() {
        // 包含需要跳过的数据行
        if (limitNo > 0 && skipNo > 0) {
            return "(select rownum rn,a.* from " + getfullform() + " a where rownum <= " + limitNo + ")";
        } else {
            return getfullform();
        }
    }

    protected Object _findex(boolean isall) {
        //ResultSet rs;
        Object rs;
        Connection conn = getNewConnection();
        try {
            Statement smt = conn.createStatement();
            if (!isall) {
                if (limitNo == 0)
                    limitNo = 1;
            }
            String sql = TransactSQLInjection("select " + fieldSQL() + " from " + getfullformSQL() + whereSQL() + sortSQL());
            TransactSQLInjection(sql);
            rs = col2jsonArray(smt.executeQuery(sql));
        } catch (Exception e) {
            rs = null;
            nLogger.logInfo(e);
        }
        //这里关闭连接，结果集就没有了
        finally {
            _Close(conn);
        }
        return rs;
    }

    private String ClobToString(Clob clob) {
        try {
            Reader is = clob.getCharacterStream();
            BufferedReader br = new BufferedReader(is);
            String s = br.readLine();
            StringBuilder sb = new StringBuilder();
            while (s != null) {// 执行循环将字符串全部取出付值给StringBuffer由StringBuffer转成STRING
                sb.append(s);
                s = br.readLine();
            }
            return sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    private Object ConvertObj(Object o) {
        if (o instanceof Clob) {
            return ClobToString((Clob) o);
        }
        return o;
    }

    protected JSONArray col2jsonArray(ResultSet rst) {
        JSONArray rsj = new JSONArray();
        ResultSetMetaData m;
        int columns;

        //String aString;
        try {
            m = rst.getMetaData();//获取 列信息
            columns = m.getColumnCount();
            while (rst.next()) {
                JSONObject obj = new JSONObject();
                for (int i = 1; i <= columns; i++) {
                    Object tobj = null;
                    try {
                        tobj = ConvertObj(rst.getObject(i));
                    } catch (SQLException sqle) {
                        if (sqle.getSQLState().equals("S1009")) {
                            tobj = 0;
                        }
                        //System.out.println("errorNO:" + sqle.getSQLState());
                    }
                    if (tobj instanceof Timestamp) {
                        tobj = TimeHelper.build().dateTimeToTimestamp(tobj.toString());
                    }
                    obj.put(m.getColumnName(i), tobj);
                }
                rsj.add(obj);
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block

            rsj = null;
            nLogger.logInfo(e);
        }
        return rsj;
    }

    protected String sqlvalue(Object _value) {//未考虑json字符串
        String rValue;
        String value;
        try {
            value = _value.toString();
            String[] _values = value.split(":");
            if (_values[0].equalsIgnoreCase("func")) {
                rValue = StringHelper.join(_values, ":", 1, -1);
            } else {
                rValue = StringHelper.typeString(_value, "'");
            }
            JSONObject jsontest = JSONObject.toJSON(rValue);
            if (jsontest != null) {
                rValue = "'" + rValue + "'";
            }
        } catch (Exception e) {
            rValue = StringHelper.typeString(_value, "'");
        }
        return rValue;
    }

}
