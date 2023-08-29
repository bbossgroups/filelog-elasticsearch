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

import org.frameworkset.tran.DataRefactor;
import org.frameworkset.tran.DataStream;
import org.frameworkset.tran.ExportResultHandler;
import org.frameworkset.tran.config.ImportBuilder;
import org.frameworkset.tran.context.Context;
import org.frameworkset.tran.input.excel.ExcelFileConfig;
import org.frameworkset.tran.input.file.FileConfig;
import org.frameworkset.tran.input.file.FileFilter;
import org.frameworkset.tran.input.file.FilterFileInfo;
import org.frameworkset.tran.metrics.TaskMetrics;
import org.frameworkset.tran.plugin.db.output.DBOutputConfig;
import org.frameworkset.tran.plugin.file.input.ExcelFileInputConfig;
import org.frameworkset.tran.schedule.CallInterceptor;
import org.frameworkset.tran.schedule.TaskContext;
import org.frameworkset.tran.task.TaskCommand;
import org.frameworkset.util.concurrent.Count;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>Description: 从日志文件采集日志数据并保存到</p>
 * <p></p>
 * <p>Copyright (c) 2020</p>
 * @Date 2021/2/1 14:39
 * @author biaoping.yin
 * @version 1.0
 */
public class ExcelFile2DBDemo {
	private static Logger logger = LoggerFactory.getLogger(ExcelFile2DBDemo.class);
	public static void main(String[] args){
//		Date[] times = getFileDates(new File("D:\\workspace\\bbossesdemo\\filelog-elasticsearch\\excelfiles\\backup\\works.xlsx") );

/**
 * 案例对应的表结构
 * CREATE TABLE
 *     filelog
 *     (
 *         id VARCHAR(100),
 *         MESSAGE text,
 *         title VARCHAR(1024),
 *         collecttime DATETIME,
 *         author VARCHAR(100),
 *         subtitle VARCHAR(450),
 *         optime DATETIME,
 *         path VARCHAR(200),
 *         hostname VARCHAR(100),
 *         pointer bigint(10),
 *         hostip VARCHAR(100),
 *         fileId VARCHAR(200),
 *         tag VARCHAR(45),
 *         PRIMARY KEY (id),
 *     )
 *     ENGINE=InnoDB DEFAULT CHARSET=utf8;
 */
		ImportBuilder importBuilder = new ImportBuilder();
		importBuilder.setBatchSize(500)//设置批量入库的记录数
				.setFetchSize(1000);//设置按批读取文件行数
		//设置强制刷新检测空闲时间间隔，单位：毫秒，在空闲flushInterval后，还没有数据到来，强制将已经入列的数据进行存储操作，默认8秒,为0时关闭本机制
		importBuilder.setFlushInterval(10000l);
		ExcelFileInputConfig config = new ExcelFileInputConfig();
		config.setBackupSuccessFiles(true);
		config.setBackupSuccessFileDir("D:\\workspace\\bbossesdemo\\filelog-elasticsearch\\excelfiles\\backup");
//		config.setBackupSuccessFileLiveTime(0);

		//shebao_org,person_no, name, cert_type,cert_no,zhs_item  ,zhs_class ,zhs_sub_class,zhs_year  , zhs_level
		//配置excel文件列与导出字段名称映射关系
		FileConfig excelFileConfig = new ExcelFileConfig();
		excelFileConfig
				.addCellMapping(0,"shebao_org")
				.addCellMapping(1,"person_no")
				.addCellMapping(2,"name")
				.addCellMapping(3,"cert_type")

				.addCellMapping(4,"cert_no","")
				.addCellMapping(5,"zhs_item")

				.addCellMapping(6,"zhs_class")
				.addCellMapping(7,"zhs_sub_class")
				.addCellMapping(8,"zhs_year","2022")
				.addCellMapping(9,"zhs_level","1");
//                .addCellMappingWithType(10,"xxx",CELL_DATE );
		excelFileConfig.setSourcePath("D:\\workspace\\bbossesdemo\\filelog-elasticsearch\\excelfiles")//指定目录
				.setFileFilter(new FileFilter() {
					@Override
					public boolean accept(FilterFileInfo fileInfo, FileConfig fileConfig) {
						//判断是否采集文件数据，返回true标识采集，false 不采集
						return fileInfo.getFileName().equals("cityperson.xlsx");
					}
				})//指定文件过滤器
				.setSkipHeaderLines(1);//忽略第一行
		config.addConfig(excelFileConfig);


		config.setEnableMeta(true);
		importBuilder.setInputConfig(config);
		//指定elasticsearch数据源名称，在application.properties文件中配置，default为默认的es数据源名称

//导出到数据源配置
		DBOutputConfig dbOutputConfig = new DBOutputConfig();
		dbOutputConfig
				.setSqlFilepath("sql-dbtran.xml")

				.setDbName("test")//指定目标数据库，在application.properties文件中配置
//				.setDbDriver("com.mysql.cj.jdbc.Driver") //数据库驱动程序，必须导入相关数据库的驱动jar包
//				.setDbUrl("jdbc:mysql://localhost:3306/bboss?useCursorFetch=true") //通过useCursorFetch=true启用mysql的游标fetch机制，否则会有严重的性能隐患，useCursorFetch必须和jdbcFetchSize参数配合使用，否则不会生效
//				.setDbUser("root")
//				.setDbPassword("123456")
//				.setValidateSQL("select 1")
//				.setUsePool(true)//是否使用连接池
				.setInsertSqlName("insertcityperson")//指定新增的sql语句名称，在配置文件中配置：sql-dbtran.xml

				/**
				 * 是否在批处理时，将insert、update、delete记录分组排序
				 * true：分组排序，先执行insert、在执行update、最后执行delete操作
				 * false：按照原始顺序执行db操作，默认值false
				 * @param optimize
				 * @return
				 */
				.setOptimize(false);//指定查询源库的sql语句，在配置文件中配置：sql-dbtran.xml
		importBuilder.setOutputConfig(dbOutputConfig);
		//增量配置开始
		importBuilder.setFromFirst(false);//setFromfirst(false)，如果作业停了，作业重启后从上次截止位置开始采集数据，
		//setFromfirst(true) 如果作业停了，作业重启后，重新开始采集数据
		importBuilder.setLastValueStorePath("excelfilemysql_import");//记录上次采集的增量字段值的文件路径，作为下次增量（或者重启后）采集数据的起点，不同的任务这个路径要不一样
        importBuilder.setLastValueStorePassword("@12341234*");
        //增量配置结束


		final Count count = new Count();
		/**
		 * 重新设置es数据结构
		 */
		importBuilder.setDataRefactor(new DataRefactor() {
			public void refactor(Context context) throws Exception  {

				//shebao_org,person_no, name, cert_type,cert_no,zhs_item  ,zhs_class ,zhs_sub_class,zhs_year  , zhs_level

				context.addFieldValue("rowNo",count.getCount());
				count.increament();

//				logger.info(SimpleStringUtil.object2json(values));
			}
		});

		//映射和转换配置结束
		importBuilder.addCallInterceptor(new CallInterceptor() {
			@Override
			public void preCall(TaskContext taskContext) {

			}

			@Override
			public void afterCall(TaskContext taskContext) {
//				if(taskContext != null)
//					logger.info(taskContext.getJobTaskMetrics().toString());
			}

			@Override
			public void throwException(TaskContext taskContext, Throwable e) {

			}
		});

		importBuilder.setExportResultHandler(new ExportResultHandler<String, String>() {
			@Override
			public void success(TaskCommand<String, String> taskCommand, String result) {
				TaskMetrics taskMetric = taskCommand.getTaskMetrics();

				//String updateSQL = "update _status_datasource " +
				//        "set totalRecords = ?," +
				//        "totalSuccessRecords = ? ," +
				//        "totalFailedRecords= ? , " +
				//        "totalIgnoreRecords = ?  " +
				//        "where ID=?";
				//try {
				//    SQLExecutor.updateWithDBName("filelog2db", updateSQL);
				//    logger.info("创建dslconfig表成功：" + updateSQL + "。");
				//} catch (SQLException e1) {
				//    logger.info("创建dslconfig表失败：" + updateSQL + "。", e1);
				//    e1.printStackTrace();
				//}
				//SQLExecutor.updateWithDBName(statusDbname, updateSQL, currentStatus.getTime(), lastValue,
				//        lastValueType, currentStatus.getFilePath(), currentStatus.getFileId(),
				//        currentStatus.getStatus(), currentStatus.getId());
				//SQLExecutor.updateBeans();_status_datasource


				logger.info(taskMetric.toString());
				//更新文件处理状态库
				//logger.info("result:" + result);
			}

			@Override
			public void error(TaskCommand<String, String> taskCommand, String result) {
				//TaskMetrics taskMetrics = taskCommand.getTaskMetrics();
				//logger.info(taskMetrics.toString());
				logger.warn("error:" + result);
			}

			@Override
			public void exception(TaskCommand<String, String> taskCommand, Throwable exception) {
				//TaskMetrics taskMetrics = taskCommand.getTaskMetrics();
				//logger.info(taskMetrics.toString());
//				logger.warn("处理异常error:", exception);

			}


		});

		/**
		 * 内置线程池配置，实现多线程并行数据导入功能，作业完成退出时自动关闭该线程池
		 */
		importBuilder.setParallel(true);//设置为多线程并行批量导入,false串行
		importBuilder.setQueue(10);//设置批量导入线程池等待队列长度
		importBuilder.setThreadCount(6);//设置批量导入线程池工作线程数量
		importBuilder.setContinueOnError(false);//任务出现异常，是否继续执行作业：true（默认值）继续执行 false 中断作业执行
		importBuilder.setPrintTaskLog(true);

		/**
		 * 启动es数据导入文件并上传sftp/ftp作业
		 */
		DataStream dataStream = importBuilder.builder();
		dataStream.execute();//启动同步作业
		logger.info("job started.");
	}
}

/**
 * pestresql
 * CREATE TABLE
 *     cityperson
 *     (
 *         shebao_org VARCHAR(200),
 *         person_no VARCHAR(200),
 *         name VARCHAR(200),
 *         cert_type VARCHAR(200),
 *         cert_no VARCHAR(200) NOT NULL,
 *         zhs_item VARCHAR(200),
 *         zhs_class VARCHAR(200),
 *         zhs_sub_class VARCHAR(200),
 *         zhs_year VARCHAR(200),
 *         zhs_level VARCHAR(200),
 *         rowNo bigint
 *     )
 *
 */
