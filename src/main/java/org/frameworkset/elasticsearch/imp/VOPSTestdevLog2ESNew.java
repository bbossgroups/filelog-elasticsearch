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
import org.frameworkset.tran.context.Context;
import org.frameworkset.tran.input.file.*;
import org.frameworkset.tran.output.es.FileLog2ESImportBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * <p>Description: 从日志文件采集日志数据并保存到</p>
 * <p></p>
 * <p>Copyright (c) 2020</p>
 * @Date 2021/2/1 14:39
 * @author biaoping.yin
 * @version 1.0
 */
public class VOPSTestdevLog2ESNew {
	private static Logger logger = LoggerFactory.getLogger(VOPSTestdevLog2ESNew.class);
	public static void main(String[] args){
		PropertiesContainer propertiesContainer = PropertiesUtil.getPropertiesContainer();
		int threadCount = propertiesContainer.getIntSystemEnvProperty("log.threadCount",5);

		int threadQueue = propertiesContainer.getIntSystemEnvProperty("log.threadQueue",50);

		int batchSize = propertiesContainer.getIntSystemEnvProperty("log.batchSize",100);

		int fetchSize = propertiesContainer.getIntSystemEnvProperty("log.fetchSize",10);
		long closeOlderTime = propertiesContainer.getLongSystemEnvProperty("log.closeOlderTime",172800000);
		boolean printTaskLog = propertiesContainer.getBooleanSystemEnvProperty("log.printTaskLog",false);
		String logPath = propertiesContainer.getSystemEnvProperty("log.path","/home/log/visualops");//同时指定了默认值false
		String startLabel = propertiesContainer.getSystemEnvProperty("log.startLabel","^\\[[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}:[0-9]{3}\\]");
		String charsetEncode = propertiesContainer.getSystemEnvProperty("log.charsetEncode","GB2312");
		String filelog_import = propertiesContainer.getSystemEnvProperty("log.filelog_import","filelog_import");
		String fileNames = propertiesContainer.getSystemEnvProperty("log.fileNames","business-handler,gateway-handler,metrics-warn-job,metrics-webdetector-node,smsdata-job,eccloginlog-handler,metrics-common-job,metrics-historydata-job,metrics-webdetector-handler,metrics-web,webpage-handler,metrics-report,metrics-webdetector-job,smsdata-handler");
		String levels = propertiesContainer.getSystemEnvProperty("log.levels","ERROR,WARN,INFO");
		String[] levelArr = levels.split(",");
		for (int i = 0; i < levelArr.length; i++){
			levelArr[i] = "["+levelArr[i]+"] ";

		}
		FileLog2ESImportBuilder importBuilder = new FileLog2ESImportBuilder();
		importBuilder.setBatchSize(batchSize)//设置批量入库的记录数
				.setFetchSize(fetchSize);//设置按批读取文件行数
		//设置强制刷新检测空闲时间间隔，单位：毫秒，在空闲flushInterval后，还没有数据到来，强制将已经入列的数据进行存储操作，默认8秒,为0时关闭本机制
		importBuilder.setFlushInterval(10000l);

		/**
		 * 一次、作业创建一个内置的线程池，实现多线程并行数据导入elasticsearch功能，作业完毕后关闭线程池
		 */
		importBuilder.setParallel(true);//设置为多线程并行批量导入,false串行
		importBuilder.setQueue(threadQueue);//设置批量导入线程池等待队列长度
		importBuilder.setThreadCount(threadCount);//设置批量导入线程池工作线程数量
		importBuilder.setContinueOnError(true);//任务出现异常，是否继续执行作业：true（默认值）继续执行 false 中断作业执行
		importBuilder.setAsyn(false);//true 异步方式执行，不等待所有导入作业任务结束，方法快速返回；false（默认值） 同步方式执行，等待所有导入作业任务结束，所有作业结束后方法才返回
		importBuilder.setPrintTaskLog(printTaskLog);

		FileImportConfig config = new FileImportConfig();
		config.setCharsetEncode(charsetEncode);

		final String[] fileNameArr = fileNames.split(",");
		config.addConfig(new FileConfig().setSourcePath(logPath)//指定目录
						.setFileFilter(new FileFilter() {//根据文件名称动态判断目录下的文件是否需要被采集
							@Override
							public boolean accept(File dir, String name, FileConfig fileConfig) {
								for (int i = 0; i < fileNameArr.length; i++) {
									String fileName = fileNameArr[i];
									if(name.equals(fileName+".log"))
										return true;
								}
								return false;
							}
						})
						.setFieldBuilder(new FieldBuilder() { //根据文件信息动态为不同的日志文件添加固定的字段
							@Override
							public void buildFields(FileInfo file, FieldManager fieldManager) {
								String fileName = file.getFileName();
								String tag = null;
								for (int i = 0; i < fileNameArr.length; i++) {
									String _fileName = fileNameArr[i];
									if(fileName.startsWith(_fileName)) {
										tag = _fileName;
										break;
									}
								}
								//添加tag标记，值为文件名称的小写，作为记录的索引名称
								if(tag != null)
									fieldManager.addField("tag",tag.toLowerCase());
							}
						})
						//.addField("tag",fileName.toLowerCase())//添加字段tag到记录中
						.setFileHeadLineRegular(startLabel)//指定多行记录的开头识别标记，正则表达式
						.setCloseEOF(false)//已经结束的文件内容采集完毕后关闭文件对应的采集通道，后续不再监听对应文件的内容变化
						.setEnableInode(true)
						/**
						 *重命名文件监听路径：一些日志组件会指定将滚动日志文件放在与当前日志文件不同的目录下，需要通过renameFileSourcePath指定这个不同的目录地址，以便
						 * 可以追踪到未采集完毕的滚动日志文件，从而继续采集文件中没有采集完毕的日志
						 * 本路径只有在inode机制有效并且启用的情况下才起作用,默认与sourcePath一致
						 */
						.setRenameFileSourcePath(logPath)
						.setCloseOlderTime(closeOlderTime) //如果2天(172800000毫秒)内日志内容没变化，则不再采集对应的日志文件，重启作业也不会采集
						//如果指定了closeOlderTime，但是有些文件是特例不能不关闭，那么可以通过指定CloseOldedFileAssert来
						//检查静默时间达到closeOlderTime的文件是否需要被关闭
						.setCloseOldedFileAssert(new CloseOldedFileAssert() {
							@Override
							public boolean canClose(FileInfo fileInfo) {
								String name = fileInfo.getFileName();//正文件不能被关闭，滚动生成的文件才需要被关闭
								for (int i = 0; i < fileNameArr.length; i++) {
									String fileName = fileNameArr[i];
									if(name.equals(fileName+".log"))
										return false;
								}
								return true;
							}
						})
						.setIncludeLines(levelArr, LineMatchType.STRING_CONTAIN)
		);

		/**
		 * 默认采用异步机制保存增量同步数据状态，提升同步性能，可以通过以下机制关闭异步机制：
		 * importBuilder.setAsynFlushStatus(false);
		 */
		importBuilder.setAsynFlushStatus(true);

		/**
		 * 启用元数据信息到记录中，元数据信息以map结构方式作为@filemeta字段值添加到记录中，文件插件支持的元信息字段如下：
		 * hostIp：主机ip
		 * hostName：主机名称
		 * filePath： 文件路径
		 * timestamp：采集的时间戳
		 * pointer：记录对应的截止文件指针,long类型
		 * fileId：linux文件号，windows系统对应文件路径
		 * 例如：
		 * {
		 *   "_index": "filelog",
		 *   "_type": "_doc",
		 *   "_id": "HKErgXgBivowv_nD0Jhn",
		 *   "_version": 1,
		 *   "_score": null,
		 *   "_source": {
		 *     "title": "解放",
		 *     "subtitle": "小康",
		 *     "ipinfo": "",
		 *     "newcollecttime": "2021-03-30T03:27:04.546Z",
		 *     "author": "张无忌",
		 *     "@filemeta": {
		 *       "path": "D:\\ecslog\\error-2021-03-27-1.log",
		 *       "hostname": "",
		 *       "pointer": 3342583,
		 *       "hostip": "",
		 *       "timestamp": 1617074824542,
		 *       "fileId": "D:/ecslog/error-2021-03-27-1.log"
		 *     },
		 *     "message": "[18:04:40:161] [INFO] - org.frameworkset.tran.schedule.ScheduleService.externalTimeSchedule(ScheduleService.java:192) - Execute schedule job Take 3 ms"
		 *   }
		 * }
		 *
		 * true 开启 false 关闭
		 */
		config.setEnableMeta(true);
		importBuilder.setFileImportConfig(config);
		//指定elasticsearch数据源名称，在application.properties文件中配置，default为默认的es数据源名称
		importBuilder.setTargetElasticsearch("default");
//		//指定索引名称，这里采用的是elasticsearch 7以上的版本进行测试，不需要指定type
		importBuilder.setIndex("filelog");
//		//指定索引类型，这里采用的是elasticsearch 7以上的版本进行测试，不需要指定type
//		//importBuilder.setIndexType("idxtype");

		//增量配置开始
		importBuilder.setFromFirst(false);//setFromfirst(false)，如果作业停了，作业重启后从上次截止位置开始采集数据，
		//setFromfirst(true) 如果作业停了，作业重启后，重新开始采集数据
		importBuilder.setLastValueStorePath(filelog_import);//记录上次采集的增量字段值的文件路径，作为下次增量（或者重启后）采集数据的起点，不同的任务这个路径要不一样
		//增量配置结束
		importBuilder.addFieldMapping("@timestamp","collectTime");
		//映射和转换配置开始


		/**
		 * 重新设置es数据结构
		 */
		importBuilder.setDataRefactor(new DataRefactor() {
			public void refactor(Context context) throws Exception  {
				String tag = context.getStringValue("tag");
				if(tag != null) {
					context.setIndex("vops-dev-"+tag+"-{dateformat=yyyy.MM}");
				}
				else {
					context.setIndex("vops-dev-{dateformat=yyyy.MM}");
				}

			}
		});
		//映射和转换配置结束


		/**
		 * 启动es数据导入文件并上传sftp/ftp作业
		 */
		DataStream dataStream = importBuilder.builder();
		dataStream.execute();//启动同步作业
		logger.info("job started.");
	}
}
