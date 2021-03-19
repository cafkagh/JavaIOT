package com.spike.mercury.util;

import java.sql.*;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MysqlJdo {

    Lock lock = new ReentrantLock();

    private static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";
    private static String DB_URL;
    private static String USER;
    private static String PASS;

    private static Connection conn = null;
    private static Statement stmt = null;

    private static String table = "";
    private static String field = "*";
    private static String where = " 1 = 1 ";
    private static String order = "";
    private static String limit = "";

    public MysqlJdo(String host, int port, String dbName, String username, String password){
        DB_URL = "jdbc:mysql://" + host + ":" + Integer.toString(port)+ "/" + dbName + "?useUnicode=true&characterEncoding=utf-8&useSSL=false&autoReconnect=true";
        USER = username;
        PASS = password;
        try{
            Class.forName(JDBC_DRIVER);
            conn = DriverManager.getConnection(DB_URL,USER,PASS);
            stmt = conn.createStatement();
        } catch(Exception se){
            se.printStackTrace();
        }
    }

    public void reset(){
        table = "";
        where = " 1 = 1 ";
        field = "*";
        limit = "";
        order = "";
    }

    public MysqlJdo table(String t){
        lock.lock();
        this.table = t;
        return this;
    }

    public MysqlJdo field(String f){
        this.field = f;
        return this;
    }

    public MysqlJdo where(String w){
        this.where = w;
        return this;
    }

    public MysqlJdo order(String o){
        this.where = " order by " + o;
        return this;
    }

    public MysqlJdo limit(int len){
        this.field = " limit 0,"+Integer.toString(len);
        return this;
    }
    public MysqlJdo limit(int start, int len){
        this.field = " limit " + Integer.toString(start) + "," + Integer.toString(len);
        return this;
    }

    public void Renewal(){
        lock.lock();
        try {
            conn.prepareStatement("SELECT 1").executeQuery();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        lock.unlock();
    }

    public int count(){
        int count = 0;
        ResultSet rs = null;
        String sql = "SELECT count(*) as count FROM " + this.table + " where " + this.where;
        System.out.println("**** " + sql + " ****");
        try {
            Statement stmt = this.conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			rs = stmt.executeQuery(sql);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        reset();
        try {
            rs.last();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        try {
            count = rs.getInt("count");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        lock.unlock();
        return count;
    }

    public ResultSet find(){
        ResultSet rs = null;
        String sql = "SELECT " + this.field + " FROM " + this.table + " where " + this.where + " limit 0,1";
        System.out.println("**** " + sql + " ****");
        try {
            Statement stmt = this.conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
			rs = stmt.executeQuery(sql);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        reset();
        try {
            rs.last();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        lock.unlock();
        return rs;
    }

    public ResultSet select(){
        ResultSet rs = null;
        String sql = "SELECT " + this.field + " FROM " + this.table + " where " + this.where  + this.order + this.limit;
        System.out.println("**** " + sql + " ****");
        try {
            rs = this.stmt.executeQuery(sql);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        reset();
        lock.unlock();
        return rs;
    }

    public static void main(String[] args){}
}
