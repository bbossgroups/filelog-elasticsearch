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
import org.frameworkset.tran.context.Context;
import org.frameworkset.tran.db.DBConfigBuilder;
import org.frameworkset.tran.input.excel.ExcelFileConfig;
import org.frameworkset.tran.input.excel.ExcelFileImportConfig;
import org.frameworkset.tran.input.file.FileConfig;
import org.frameworkset.tran.input.file.FileFilter;
import org.frameworkset.tran.input.file.FilterFileInfo;
import org.frameworkset.tran.output.db.FileLog2DBImportBuilder;
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
		FileLog2DBImportBuilder importBuilder = new FileLog2DBImportBuilder();
		importBuilder.setBatchSize(500)//设置批量入库的记录数
				.setFetchSize(1000);//设置按批读取文件行数
		//设置强制刷新检测空闲时间间隔，单位：毫秒，在空闲flushInterval后，还没有数据到来，强制将已经入列的数据进行存储操作，默认8秒,为0时关闭本机制
		importBuilder.setFlushInterval(10000l);

		ExcelFileImportConfig config = new ExcelFileImportConfig();

		//shebao_org,person_no, name, cert_type,cert_no,zhs_item  ,zhs_class ,zhs_sub_class,zhs_year  , zhs_level
		//配置excel文件列与导出字段名称映射关系
		config.addConfig(new ExcelFileConfig()
						.addCellMapping(0,"shebao_org")
						.addCellMapping(1,"person_no")
						.addCellMapping(2,"name")
						.addCellMapping(3,"cert_type")

						.addCellMapping(4,"cert_no","")
						.addCellMapping(5,"zhs_item")

						.addCellMapping(6,"zhs_class")
						.addCellMapping(7,"zhs_sub_class")
						.addCellMapping(8,"zhs_year","2022")
						.addCellMapping(9,"zhs_level","1")
						.setSourcePath("D:\\workspace\\bbossesdemo\\filelog-elasticsearch\\excelfiles")//指定目录
						.setFileFilter(new FileFilter() {
							@Override
							public boolean accept(FilterFileInfo fileInfo, FileConfig fileConfig) {
								//判断是否采集文件数据，返回true标识采集，false 不采集
								return fileInfo.getFileName().equals("cityperson.xlsx");
							}
						})//指定文件过滤器
				        .setSkipHeaderLines(1)//忽略第一行
		);


		config.setEnableMeta(true);
		importBuilder.setFileImportConfig(config);
		//指定elasticsearch数据源名称，在application.properties文件中配置，default为默认的es数据源名称

//导出到数据源配置
		DBConfigBuilder dbConfigBuilder = new DBConfigBuilder();
		dbConfigBuilder
				.setSqlFilepath("sql-dbtran.xml")

				.setTargetDbName("test")//指定目标数据库，在application.properties文件中配置
//				.setTargetDbDriver("com.mysql.cj.jdbc.Driver") //数据库驱动程序，必须导入相关数据库的驱动jar包
//				.setTargetDbUrl("jdbc:mysql://localhost:3306/bboss?useCursorFetch=true") //通过useCursorFetch=true启用mysql的游标fetch机制，否则会有严重的性能隐患，useCursorFetch必须和jdbcFetchSize参数配合使用，否则不会生效
//				.setTargetDbUser("root")
//				.setTargetDbPassword("123456")
//				.setTargetValidateSQL("select 1")
//				.setTargetUsePool(true)//是否使用连接池
				.setInsertSqlName("insertcityperson")//指定新增的sql语句名称，在配置文件中配置：sql-dbtran.xml

				/**
				 * 是否在批处理时，将insert、update、delete记录分组排序
				 * true：分组排序，先执行insert、在执行update、最后执行delete操作
				 * false：按照原始顺序执行db操作，默认值false
				 * @param optimize
				 * @return
				 */
				.setOptimize(false);//指定查询源库的sql语句，在配置文件中配置：sql-dbtran.xml
		importBuilder.setOutputDBConfig(dbConfigBuilder.buildDBImportConfig());
		//增量配置开始
		importBuilder.setFromFirst(true);//setFromfirst(false)，如果作业停了，作业重启后从上次截止位置开始采集数据，
		//setFromfirst(true) 如果作业停了，作业重启后，重新开始采集数据
		importBuilder.setLastValueStorePath("excelfilelogdb_import");//记录上次采集的增量字段值的文件路径，作为下次增量（或者重启后）采集数据的起点，不同的任务这个路径要不一样
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

		/**
		 * 一次、作业创建一个内置的线程池，实现多线程并行数据导入elasticsearch功能，作业完毕后关闭线程池
		 */
		importBuilder.setParallel(true);//设置为多线程并行批量导入,false串行
		importBuilder.setQueue(10);//设置批量导入线程池等待队列长度
		importBuilder.setThreadCount(6);//设置批量导入线程池工作线程数量
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
