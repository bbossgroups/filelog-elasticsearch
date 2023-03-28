package org.frameworkset.elasticsearch.imp;
/**
 * Copyright 2020 bboss
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.frameworkset.elasticsearch.util.PropertiesUtil;
import org.frameworkset.spi.assemble.PropertiesContainer;
import org.frameworkset.tran.DataRefactor;
import org.frameworkset.tran.DataStream;
import org.frameworkset.tran.ExportResultHandler;
import org.frameworkset.tran.config.ImportBuilder;
import org.frameworkset.tran.context.Context;
import org.frameworkset.tran.ftp.FtpConfig;
import org.frameworkset.tran.input.file.FileConfig;
import org.frameworkset.tran.input.file.FileFilter;
import org.frameworkset.tran.input.file.FilterFileInfo;
import org.frameworkset.tran.metrics.TaskMetrics;
import org.frameworkset.tran.plugin.db.output.DBOutputConfig;
import org.frameworkset.tran.plugin.file.input.FileInputConfig;
import org.frameworkset.tran.schedule.CallInterceptor;
import org.frameworkset.tran.schedule.TaskContext;
import org.frameworkset.tran.task.TaskCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * <p>Description: 从日志文件采集日志数据并保存到Oracle12c中</p>
 * <p></p>
 * <p>Copyright (c) 2020</p>
 *
 * @author carl
 * @version 1.1
 * @Date 2021/09/28 11:25
 */
public class FtpLog2DB {

    private static Logger logger = LoggerFactory.getLogger(FtpLog2DB.class);

    /**
     * 创建目录
     *
     * @param path
     */
    public static void CreatFileDir(String path) {
        try {
            File file = new File(path);
            if (file.getParentFile().isDirectory()) {//判断上级目录是否是目录
                if (!file.exists()) {   //如果文件目录不存在
                    file.mkdirs();  //创建文件目录
                }
            } else {
                throw new Exception("传入目录非标准目录名");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        //LocalPoolDeployer.addShutdownHook = true;//在应用程序stop.sh时，关闭数据源错误提示
        //启动数据采集
        FtpLog2DB file2DB = new FtpLog2DB();
        file2DB.scheduleTimestampImportData();
    }


    public static void scheduleTimestampImportData() {
        //获取配置文件-文件目录
        //String data_dir = propertiesContainer.getSystemEnvProperty("DATA_DIR", "D:\\www\\input_data\\logs\\");  //数据获取目录


        ImportBuilder importBuilder = new ImportBuilder();
        importBuilder.setBatchSize(500)//设置批量入库的记录数
                .setFetchSize(1000);//设置按批读取文件行数
        //设置强制刷新检测空闲时间间隔，单位：毫秒，在空闲flushInterval后，还没有数据到来，强制将已经入列的数据进行存储操作，默认8秒,为0时关闭本机制
        importBuilder.setFlushInterval(10000l);
        FileInputConfig config = new FileInputConfig();

        config.setJsondata(true);//标识文本记录是json格式的数据，true 将值解析为json对象，false - 不解析，这样值将作为一个完整的message字段存放到上报数据中
        config.setRootLevel(true);//jsondata = true时，自定义的数据是否和采集的数据平级，true则直接在原先的json串中存放数据 false则定义一个json存放数据，若不是json则是message

        config.setScanNewFileInterval(1 * 60 * 1000l);//每隔半1分钟扫描ftp目录下是否有最新ftp文件信息，采集完成或已经下载过的文件不会再下载采集
        /**
         * 备份采集完成文件
         * true 备份
         * false 不备份
         */
        config.setBackupSuccessFiles(true);
        /**
         * 备份文件目录
         */
        config.setBackupSuccessFileDir("d:/ftpbackup");
        /**
         * 备份文件清理线程执行时间间隔，单位：毫秒
         * 默认每隔10秒执行一次
         */
        config.setBackupSuccessFileInterval(20000l);
        /**
         * 备份文件保留时长，单位：秒
         * 默认保留7天
         */
        config.setBackupSuccessFileLiveTime(10 * 60l);

        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
        Date _startDate = null;
        try {
            _startDate = format.parse("20191211");//下载和采集2020年12月11日以后的数据文件
        } catch (ParseException e) {
            logger.error("", e);
        }
        final Date startDate = _startDate;
        FtpConfig ftpConfig = new FtpConfig().setFtpIP("127.0.0.1").setFtpPort(222)
                .setFtpUser("test").setFtpPassword("123456")
                .setRemoteFileDir("/").setDeleteRemoteFile(true)//
                .setTransferProtocol(FtpConfig.TRANSFER_PROTOCOL_FTP); //采用ftp协议
        config.addConfig(new FileConfig().setFtpConfig(ftpConfig)
                        .setFileFilter(new FileFilter() {//指定ftp文件筛选规则
                            @Override
                            public boolean accept(FilterFileInfo fileInfo,
                                                  FileConfig fileConfig) {
                                String name = fileInfo.getFileName();
                                //判断是否采集文件数据，返回true标识采集，false 不采集
                                boolean nameMatch = name.startsWith("731_tmrt_user_login_day_");
                                if(nameMatch){
                                    String day = name.substring("731_tmrt_user_login_day_".length());
                                    SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
                                    try {
                                        Date fileDate = format.parse(day);
                                        if(fileDate.after(startDate))//下载和采集2020年12月11日以后的数据文件
                                            return true;
                                    } catch (ParseException e) {
                                        logger.error("",e);
                                    }


                                }
                                return false;
                            }
                        })
                        .addScanNewFileTimeRange("10:00-18:30")
//										.addSkipScanNewFileTimeRange("11:30-13:00")
                        .setSourcePath("D:\\ftplogs\\dbdemo")//指定目录
                //.addField("tag", "elasticsearch")//添加字段tag到记录中
                //.setCloseEOF(true)//已经结束的文件内容采集完毕后关闭文件对应的采集通道，后续不再监听对应文件的内容变化
                //.setEnableInode(false)
        );


        config.setEnableMeta(true);
        importBuilder.setInputConfig(config);
        //指定elasticsearch数据源名称，在application.properties文件中配置，default为默认的es数据源名称

        //导出到数据源配置
        DBOutputConfig dbOutputConfig = new DBOutputConfig();
        dbOutputConfig
                .setSqlFilepath("sql-dbtran.xml")
                //.setDeleteSql("deleteSql")
                .setInsertSqlName("insertFTP_LOG")//指定新增的sql语句名称，在配置文件中配置：sql-dbtran.xml
                //.setDeleteSql()
                //.setUpdateSql()
                /**
                 * 是否在批处理时，将insert、update、delete记录分组排序
                 * true：分组排序，先执行insert、在执行update、最后执行delete操作
                 * false：按照原始顺序执行db操作，默认值false
                 * @param optimize
                 * @return
                 */
                .setOptimize(true);//指定查询源库的sql语句，在配置文件中配置：sql-dbtran.xml

        //数据库相关配置参数(application.properties)
        PropertiesContainer propertiesContainer = PropertiesUtil.getPropertiesContainer();
        String dbName = propertiesContainer.getSystemEnvProperty("db.name", "oradb01");
        String dbUser = propertiesContainer.getSystemEnvProperty("db.user", "NTHSJY");
        String dbPassword = propertiesContainer.getSystemEnvProperty("db.password", "********");
        String dbDriver = propertiesContainer.getSystemEnvProperty("db.driver", "oracle.jdbc.driver.OracleDriver");
        String dbUrl = propertiesContainer.getSystemEnvProperty("db.url", "jdbc:oracle:thin:@192.168.97.100:1521:ORCLPDB1");
        String showsql = propertiesContainer.getSystemEnvProperty("db.showsql", "false");
        String validateSQL = propertiesContainer.getSystemEnvProperty(" db.validateSQL", "select 1 from dual");
        //String dbInfoEncryptClass = propertiesContainer.getSystemEnvProperty("db.dbInfoEncryptClass");
        boolean dbUsePool = propertiesContainer.getBooleanProperty("db.usePool", true);
        String dbInitSize = propertiesContainer.getSystemEnvProperty("db.initSize", "100");
        String dbMinIdleSize = propertiesContainer.getSystemEnvProperty("db.minIdleSize", "100");
        String dbMaxSize = propertiesContainer.getSystemEnvProperty("db.maxSize", "1000");
        String dbJdbcFetchSize = propertiesContainer.getSystemEnvProperty("db.jdbcFetchSize", "10000");
        boolean columnLableUpperCase = propertiesContainer.getBooleanProperty("db.columnLableUpperCase", true);

        dbOutputConfig.setDbName(dbName)//指定目标数据库，在application.properties文件中配置
                .setDbDriver(dbDriver) //数据库驱动程序，必须导入相关数据库的驱动jar包
                .setDbUrl(dbUrl) //通过useCursorFetch=true启用mysql的游标fetch机制，否则会有严重的性能隐患，useCursorFetch必须和jdbcFetchSize参数配合使用，否则不会生效
                .setDbUser(dbUser)
                .setDbPassword(dbPassword)
                .setValidateSQL(validateSQL)
                .setUsePool(dbUsePool)//是否使用连接池
        ;

        importBuilder.setOutputConfig(dbOutputConfig);
        //增量配置开始
        importBuilder.setFromFirst(false);
        //setFromfirst(false)，如果作业停了，作业重启后从上次截止位置开始采集数据，
        //setFromfirst(true) 如果作业停了，作业重启后，重新开始采集数据
        importBuilder.setLastValueStorePath("ftplog2db");//记录上次采集的增量字段值的文件路径，作为下次增量（或者重启后）采集数据的起点，不同的任务这个路径要不一样
        //增量配置结束

        //设置任务执行拦截器，可以添加多个
        importBuilder.addCallInterceptor(new CallInterceptor() {
            @Override
            public void preCall(TaskContext taskContext) {

            }

            @Override
            public void afterCall(TaskContext taskContext) {


            }


            @Override
            public void throwException(TaskContext taskContext, Throwable e) {
                System.out.println("throwException");
            }
        });


        //映射和转换配置开始
        importBuilder.setExportResultHandler(new ExportResultHandler<String, String>() {
            @Override
            public void success(TaskCommand<String, String> taskCommand, String o) {
                TaskMetrics taskMetric = taskCommand.getTaskMetrics();
                logger.info("处理耗时：" + taskCommand.getElapsed() + "毫秒");
                logger.info(taskMetric.toString());
                //logger.info("result:" + o);
            }

            @Override
            public void error(TaskCommand<String, String> taskCommand, String o) {
                logger.warn("error:" + o);
            }

            @Override
            public void exception(TaskCommand<String, String> taskCommand, Throwable exception) {
                logger.warn("处理异常error:", exception);

            }

        });
        //映射和转换配置结束
//		/**
//		 * db-es mapping 表字段名称到es 文档字段的映射：比如document_id -> docId
//		 * 可以配置mapping，也可以不配置，默认基于java 驼峰规则进行db field-es field的映射和转换
//		 */

        //importBuilder.addFieldMapping("@message", "message");
        ////
        //importBuilder.addFieldMapping("@timestamp", "optime");

        /**
         * 重新设置es数据结构
         */
        importBuilder.setDataRefactor(new DataRefactor() {
            public void refactor(Context context) throws Exception {
                //context.markRecoredInsert();//添加，默认值,如果不显示标注记录状态则默认为添加操作，对应Elasticsearch的index操作
                //
                //context.markRecoredUpdate();//修改，对应Elasticsearch的update操作
                //
                //context.markRecoredDelete();//删除，对应Elasticsearch的delete操作
                //可以根据条件定义是否丢弃当前记录
                //context.setDrop(true);return;
//				if(s.incrementAndGet() % 2 == 0) {
//					context.setDrop(true);
//					return;
//				}
//				System.out.println(data);

//				context.addFieldValue("author","duoduo");//将会覆盖全局设置的author变量
//                context.addFieldValue("author", "duoduo");
//                context.addFieldValue("title", "解放");
//                context.addFieldValue("subtitle", "小康");
//
//                context.addFieldValue("collecttime", new Date());


//				//如果日志是普通的文本日志，非json格式，则可以自己根据规则对包含日志记录内容的message字段进行解析
//				String message = (String) context.getRecord();
//                System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
//				System.out.println(context.getRecord());
//                System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
//				String[] fvs = message.split(" ");//空格解析字段
                /**
                 * //解析示意代码
                 * String[] fvs = message.split(" ");//空格解析字段
                 * //将解析后的信息添加到记录中
                 * context.addFieldValue("f1",fvs[0]);
                 * context.addFieldValue("f2",fvs[1]);
                 * context.addFieldValue("logVisitorial",fvs[2]);//包含ip信息
                 */
                //直接获取文件元信息
//				Map fileMata = (Map)context.getValue("@filemeta");
                /**
                 * 文件插件支持的元信息字段如下：
                 * hostIp：主机ip
                 * hostName：主机名称
                 * filePath： 文件路径
                 * timestamp：采集的时间戳
                 * pointer：记录对应的截止文件指针,long类型
                 * fileId：linux文件号，windows系统对应文件路径
                 */
                //String FLOW_ID = (String) context.getValue("FLOW_ID");
                //System.out.println(FLOW_ID);

                //String hostIp = (String) context.getMetaValue("hostIp");
                //String hostName = (String) context.getMetaValue("hostName");
                //String fileId = (String) context.getMetaValue("fileId");
                //long pointer = (long) context.getMetaValue("pointer");
                //context.addFieldValue("filePath", filePath);
                //context.addFieldValue("hostIp", hostIp);
                //context.addFieldValue("hostName", hostName);
                //context.addFieldValue("fileId", fileId);
                //context.addFieldValue("pointer", pointer);
                //context.addIgnoreFieldMapping("@filemeta");
                String message = (String)context.getValue("@message");
                String[] items = message.split("\t");
                context.addFieldValue("chanId", items[0]);
                context.addFieldValue("phoneNumber", items[1]);
                context.addFieldValue("eparchyName", items[2]);
                context.addFieldValue("returnCode", items[3]);
                context.addFieldValue("loginTime", items[4]);
                context.addFieldValue("loginStatus", "0000".equals(items[3]) ? "1" : "2");//1登录成功,2登录失败
                context.addIgnoreFieldMapping("@message");
            }
        });
        //映射和转换配置结束

        /**
         * 内置线程池配置，实现多线程并行数据导入功能，作业完成退出时自动关闭该线程池
         */
        //importBuilder.setUseLowcase(false);
        //importBuilder.setUseJavaName(false);
        importBuilder.setParallel(true);//设置为多线程并行批量导入,false串行
        importBuilder.setQueue(10);//设置批量导入线程池等待队列长度
        importBuilder.setThreadCount(50);//设置批量导入线程池工作线程数量
        importBuilder.setContinueOnError(true);//任务出现异常，是否继续执行作业：true（默认值）继续执行 false 中断作业执行
        importBuilder.setAsyn(false);//true 异步方式执行，不等待所有导入作业任务结束，方法快速返回；false（默认值） 同步方式执行，等待所有导入作业任务结束，所有作业结束后方法才返回
        importBuilder.setPrintTaskLog(true);

        /**
         * 启动es数据导入文件并上传sftp/ftp作业
         */
        DataStream dataStream = importBuilder.builder();
        dataStream.execute();//启动同步作业
        logger.info("job started.");
    }
}
