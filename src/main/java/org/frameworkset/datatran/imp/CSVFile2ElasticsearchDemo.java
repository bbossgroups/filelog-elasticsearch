package org.frameworkset.datatran.imp;
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
import org.frameworkset.tran.input.file.FileConfig;
import org.frameworkset.tran.input.file.FileFilter;
import org.frameworkset.tran.input.file.FilterFileInfo;
import org.frameworkset.tran.plugin.es.output.ElasticsearchOutputConfig;
import org.frameworkset.tran.plugin.file.input.FileInputConfig;
import org.frameworkset.tran.task.TaskCommand;
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
public class CSVFile2ElasticsearchDemo {

	private static final Logger logger = LoggerFactory.getLogger(CSVFile2ElasticsearchDemo.class);
	public static void main(String[] args){

		ImportBuilder importBuilder = new ImportBuilder();
		importBuilder.setBatchSize(500)//设置批量入库的记录数
				.setFetchSize(1000);//设置按批读取文件行数
		//设置强制刷新检测空闲时间间隔，单位：毫秒，在空闲flushInterval后，还没有数据到来，强制将已经入列的数据进行存储操作，默认8秒,为0时关闭本机制
		importBuilder.setFlushInterval(10000l);

//":null,"jdbcFetchSize":-2147483648,"dbDriver":"com.mysql.cj.jdbc.Driver","dbUrl":"jdbc:mysql://192.168.137.1:3306/bboss?useUnicode=true&characterEncoding=utf-8&useSSL=false","dbUser":"root","dbPassword":"123456","initSize":100,"minIdleSize":100,"maxSize":100,"showSql":true,"usePool":true,"dbtype":null,"dbAdaptor":null,"columnLableUpperCase":false,"enableDBTransaction":false,"validateSQL":"select 1","dbName":"test"},"statusDbname":null,"statusTableDML":null,"fetchSize":10,"flushInterval":0,"ignoreNullValueField":false,"Elasticsearch":"default","sourceElasticsearch":"default","clientOptions":null,"geoipConfig":null,"sortLastValue":true,"useBatchContextIndexName":false,"discardBulkResponse":true,"debugResponse":false,"scheduleConfig":{"scheduleDate":null,"deyLay":1000,"period":10000,"fixedRate":false,"externalTimer":false},"importIncreamentConfig":{"lastValueColumn":"logOpertime","lastValue":null,"lastValueType":1,"lastValueStorePath":"es2dbdemo_import","lastValueStoreTableName":null,"lastValueDateType":true,"fromFirst":true,"statusTableId":null},"externalTimer":false,"printTaskLog":true,"applicationPropertiesFile":null,"configs":null,"batchSize":2,"parallel":true,"threadCount":50,"queue":10,"asyn":false,"continueOnError":true,"asynResultPollTimeOut":1000,"useLowcase":null,"scheduleBatchSize":null,"index":null,"indexType":null,"useJavaName":null,"exportResultHandlerClass":null,"locale":null,"timeZone":null,"esIdGeneratorClass":"org.frameworkset.tran.DefaultEsIdGenerator","dataRefactorClass":"org.frameworkset.datatran.imp.ES2DBScrollTimestampDemo$3","pagine":false,"scrollLiveTime":"10m","queryUrl":"dbdemo/_search","dsl2ndSqlFile":"dsl2ndSqlFile.xml","dslName":"scrollQuery","sliceQuery":false,"sliceSize":0,"Index":null,"IndexType":null}
		FileInputConfig config = new FileInputConfig();
		config.setJsondata(false);

		config.addConfig(new FileConfig().setSourcePath("C:\\data\\cvs")//指定目录
						.setFileFilter(new FileFilter() {
							@Override
							public boolean accept(FilterFileInfo fileInfo, FileConfig fileConfig) {
								//判断是否采集文件数据，返回true标识采集，false 不采集
								return fileInfo.getFileName().equals("luoyong.csv");
							}
						})//指定文件过滤器
						.setCloseEOF(true)//已经结束的文件内容采集完毕后关闭文件对应的采集通道，后续不再监听对应文件的内容变化
						.setEnableInode(false)
				.setSkipHeaderLines(1)
		);

		config.setEnableMeta(false);
		config.setDisableScanNewFiles(true);
		config.setDisableScanNewFilesCheckpoint(true);

		importBuilder.setInputConfig(config);
		//指定elasticsearch数据源名称，在application.properties文件中配置，default为默认的es数据源名称

		//导出到数据源配置
		ElasticsearchOutputConfig elasticsearchOutputConfig = new ElasticsearchOutputConfig();
		elasticsearchOutputConfig
				.addTargetElasticsearch("elasticsearch.serverNames","default")
				.addElasticsearchProperty("default.elasticsearch.rest.hostNames","10.13.6.7:9200")
				.addElasticsearchProperty("default.elasticsearch.showTemplate","true")
				.addElasticsearchProperty("default.elasticUser","elastic")
				.addElasticsearchProperty("default.elasticPassword","changeme")
				.addElasticsearchProperty("default.elasticsearch.failAllContinue","true")
				.addElasticsearchProperty("default.http.timeoutSocket","60000")
				.addElasticsearchProperty("default.http.timeoutConnection","40000")
				.addElasticsearchProperty("default.http.connectionRequestTimeout","70000")
				.addElasticsearchProperty("default.http.maxTotal","200")
				.addElasticsearchProperty("default.http.defaultMaxPerRoute","100")
				.setIndex("ngxlog")
				.setDiscardBulkResponse(true);//设置是否需要批量处理的响应报文，不需要设置为false，true为需要，默认false

		importBuilder.setOutputConfig(elasticsearchOutputConfig);

	
		/**
		 * 重新设置es数据结构
		 */
		importBuilder.setDataRefactor(new DataRefactor() {
			public void refactor(Context context) throws Exception  {
				String message = context.getStringValue("@message");
				String[] values = message.split("\\,");
				//@timestamp,_id _index	_score	_type	body	document_type	enNum	input.type	ip	ngUid	ntime	referer	remoteaddr	req	restime	serveraddr	status	time	uid	upstream
				// useragent
				int num = values.length;
				if (num < 22) {
					context.setDrop(true);
					return;
				}
				context.addFieldValue("@timestamp",values[0]);
				context.addFieldValue("_id",values[1]);
				context.addFieldValue("_index",values[2]);
				context.addFieldValue("_score",values[3]);
				context.addFieldValue("_type",values[4]);
				context.addFieldValue("body",values[5]);
				context.addFieldValue("document_type",values[6]);
				context.addFieldValue("enNum",values[7]);
				context.addFieldValue("input.type",values[8]);
				context.addFieldValue("ip",values[9]);
				context.addFieldValue("ngUid",values[10]);
				context.addFieldValue("ntime",values[11]);
				context.addFieldValue("referer",values[12]);
				context.addFieldValue("remoteaddr",values[13]);
				context.addFieldValue("req",values[14]);
				context.addFieldValue("restime",values[15]);
				context.addFieldValue("serveraddr",values[16]);
				context.addFieldValue("status",values[17]);
				context.addFieldValue("time",values[18]);
				context.addFieldValue("uid",values[19]);
				context.addFieldValue("upstream",values[20]);
				context.addFieldValue("useragent",values[21]);

			}
		});

        importBuilder.setExportResultHandler(new ExportResultHandler() {
            @Override
            public void success(TaskCommand taskCommand, Object o) {
                
            }

            @Override
            public void error(TaskCommand taskCommand, Object o) {
                logger.warn(""+o);
            }

            @Override
            public void exception(TaskCommand taskCommand, Throwable exception) {
                logger.error("",exception);
            }
        });
		//映射和转换配置结束

		/**
		 * 内置线程池配置，实现多线程并行数据导入功能，作业完成退出时自动关闭该线程池
		 */
		importBuilder.setParallel(true);//设置为多线程并行批量导入,false串行
		importBuilder.setQueue(10);//设置批量导入线程池等待队列长度
		importBuilder.setThreadCount(6);//设置批量导入线程池工作线程数量
		importBuilder.setContinueOnError(true);//任务出现异常，是否继续执行作业：true（默认值）继续执行 false 中断作业执行
		importBuilder.setPrintTaskLog(true);

		/**
		 * 启动es数据导入文件并上传sftp/ftp作业
		 */
		DataStream dataStream = importBuilder.builder();
		dataStream.execute();//启动同步作业
		logger.info("job started.");
	}
}
