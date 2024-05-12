package org.frameworkset.elasticsearch.imp.avl;
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

import org.frameworkset.elasticsearch.ElasticSearchHelper;
import org.frameworkset.tran.DataRefactor;
import org.frameworkset.tran.DataStream;
import org.frameworkset.tran.ExportResultHandler;
import org.frameworkset.tran.config.ImportBuilder;
import org.frameworkset.tran.context.Context;
import org.frameworkset.tran.ftp.FtpConfig;
import org.frameworkset.tran.ftp.RemoteFileValidate;
import org.frameworkset.tran.ftp.ValidateContext;
import org.frameworkset.tran.input.file.FileConfig;
import org.frameworkset.tran.input.file.FileFilter;
import org.frameworkset.tran.input.file.FileTaskContext;
import org.frameworkset.tran.input.file.FilterFileInfo;
import org.frameworkset.tran.plugin.es.output.ElasticsearchOutputConfig;
import org.frameworkset.tran.plugin.file.input.FileInputConfig;
import org.frameworkset.tran.schedule.CallInterceptor;
import org.frameworkset.tran.schedule.TaskContext;
import org.frameworkset.tran.task.TaskCommand;
import org.frameworkset.util.annotations.DateFormateMeta;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.util.Date;

/**
 * <p>Description: 增量扫描ftp目录中avl格式行程码数据文件，下载未采集过的日志文件，
 * 然后采集日志数据并保存到elasticsearch，采集完毕后，备份日志文件到指定的目录下面，
 * 定期清理备份目录下超过指定时间的备份日志文件
 * </p>
 * <p></p>
 * <p>Copyright (c) 2020</p>
 * @Date 2021/2/1 14:39
 * @author biaoping.yin
 * @version 1.0
 */
public class FtpAvl2ESDemo {
	private static Logger logger = LoggerFactory.getLogger(FtpAvl2ESDemo.class);
	public static void main(String[] args){

		try {
			//清除测试表,导入的时候回重建表，测试的时候加上为了看测试效果，实际线上环境不要删表
//			String repsonse = ElasticSearchHelper.getRestClientUtil().dropIndice("errorlog");
			String repsonse = ElasticSearchHelper.getRestClientUtil().dropIndice("xcm-*");
			logger.info(repsonse);
		} catch (Exception e) {
		}
		ImportBuilder importBuilder = new ImportBuilder();
		importBuilder.setBatchSize(5000)//设置批量入库的记录数
				.setFetchSize(1000);//设置按批读取文件行数
		//设置强制刷新检测空闲时间间隔，单位：毫秒，在空闲flushInterval后，还没有数据到来，强制将已经入列的数据进行存储操作，默认8秒,为0时关闭本机制
		importBuilder.setFlushInterval(10000l);

		FileInputConfig config = new FileInputConfig();
		config.setSleepAwaitTimeAfterFetch(100l);//采集完一批数据，等待一会再采集下一批数据，避免大量文件采集时，cpu占用过高
		config.setScanNewFileInterval(1*60*1000l);//每隔半1分钟扫描ftp目录下是否有最新ftp文件信息，采集完成或已经下载过的文件不会再下载采集
		/**
		 * 备份采集完成文件
		 * true 备份
		 * false 不备份
		 */
		config.setBackupSuccessFiles(true);
		/**
		 * 备份文件目录
		 */
		config.setBackupSuccessFileDir("d:/ftpavlbackup");
		/**
		 * 备份文件清理线程执行时间间隔，单位：毫秒
		 * 默认每隔10秒执行一次
		 */
		config.setBackupSuccessFileInterval(20000l);
		/**
		 * 备份文件保留时长，单位：秒
		 * 默认保留7天
		 */
		config.setBackupSuccessFileLiveTime( 100 * 60l);

		FtpConfig ftpConfig = new FtpConfig().setFtpIP("127.0.0.1").setFtpPort(21)
				.setFtpUser("ecsftp").setFtpPassword("ecsftp").setDownloadWorkThreads(3)//设置4个线程并行下载文件，可以允许最多4个文件同时下载
				.setRemoteFileDir("xcm").setRemoteFileValidate(new RemoteFileValidate() {
					/**
					 * 校验数据文件合法性和完整性接口

					 * @param validateContext 封装校验数据文件信息
					 *     dataFile 待校验零时数据文件，可以根据文件名称获取对应文件的md5签名文件名、数据量稽核文件名称等信息，
					 *     remoteFile 通过数据文件对应的ftp/sftp文件路径，计算对应的目录获取md5签名文件、数据量稽核文件所在的目录地址
					 *     ftpContext ftp配置上下文对象
					 *     然后通过remoteFileAction下载md5签名文件、数据量稽核文件，再对数据文件进行校验即可
					 *     redownload 标记校验来源是否是因校验失败重新下载文件导致的校验操作，true 为重下后 文件校验，false为第一次下载校验
					 * @return int
					 * 文件内容校验成功
					 * 	RemoteFileValidate.FILE_VALIDATE_OK = 1;
					 * 	校验失败不处理文件
					 * 	RemoteFileValidate.FILE_VALIDATE_FAILED = 2;
					 * 	文件内容校验失败并备份已下载文件
					 * 	RemoteFileValidate.FILE_VALIDATE_FAILED_BACKUP = 3;
					 * 	文件内容校验失败并删除已下载文件
					 * 	RemoteFileValidate.FILE_VALIDATE_FAILED_DELETE = 5;
					 */
					public Result validateFile(ValidateContext validateContext) {
//						if(redownload)
//							return Result.default_ok;
////						return Result.default_ok;
//						Result result = new Result();
//						result.setValidateResult(RemoteFileValidate.FILE_VALIDATE_FAILED_REDOWNLOAD);
//						result.setRedownloadCounts(3);
//						result.setMessage("MD5校验"+remoteFile+"失败，重试3次");//设置校验失败原因信息
//						//根据remoteFile的信息计算md5文件路径地址，并下载，下载务必后进行签名校验
//						//remoteFileAction.downloadFile("remoteFile.md5","dataFile.md5");
//						return result;
						return Result.default_ok;
					}
				})
				.setTransferProtocol(FtpConfig.TRANSFER_PROTOCOL_FTP) ;//采用ftp协议
		config.addConfig(new FileConfig().setFtpConfig(ftpConfig)
										.setFileFilter(new FileFilter() {//指定ftp文件筛选规则
											@Override
											public boolean accept(FilterFileInfo fileInfo, //Ftp文件名称
																  FileConfig fileConfig) {
												String name = fileInfo.getFileName();
												//判断是否采集文件数据，返回true标识采集，false 不采集
//												boolean nameMatch = name.equals("WD743D3188020220318000001.AVL");
												boolean nameMatch = name.endsWith(".AVL");

												return nameMatch;
											}
										})
										.setCharsetEncode("GBK")
										.setSourcePath("D:/ftpavl")//指定目录

						);

		config.setEnableMeta(true);
//		config.setJsondata(true);
		importBuilder.setInputConfig(config);
		//指定elasticsearch数据源名称，在application.properties文件中配置，default为默认的es数据源名称
		ElasticsearchOutputConfig elasticsearchOutputConfig = new ElasticsearchOutputConfig();
		elasticsearchOutputConfig.setTargetElasticsearch("default");
		//指定索引名称，这里采用的是elasticsearch 7以上的版本进行测试，不需要指定type
		elasticsearchOutputConfig.setIndex("xcm-{xcmDate,yyyy.MM.dd}");
		//指定索引类型，这里采用的是elasticsearch 7以上的版本进行测试，不需要指定type
		//elasticsearchOutputConfig.setIndexType("idxtype");
		importBuilder.setOutputConfig(elasticsearchOutputConfig);

		//增量配置开始
		importBuilder.setFromFirst(false);//setFromfirst(false)，如果作业停了，作业重启后从上次截止位置开始采集数据，
		//setFromfirst(true) 如果作业停了，作业重启后，重新开始采集数据
		importBuilder.setLastValueStorePath("ftpavles_import");//记录上次采集的增量字段值的文件路径，作为下次增量（或者重启后）采集数据的起点，不同的任务这个路径要不一样
		//增量配置结束

		final DateFormateMeta dateFormateMeta = DateFormateMeta.buildDateFormateMeta("yyyyMMdd");

		/**
		 * 重新设置es数据结构
		 */
		importBuilder.setDataRefactor(new DataRefactor() {
			public void refactor(Context context) throws Exception  {
				//可以根据条件定义是否丢弃当前记录

				//如果日志是普通的文本日志，非json格式，则可以自己根据规则对包含日志记录内容的message字段进行解析
				String message = context.getStringValue("@message");
				try {
					String[] fvs = message.split("\\|");//空格解析字段
//				Date optime = context.getDateValue("LOG_OPERTIME",dateFormat);
//				context.addFieldValue("logOpertime",optime);
					//20220318|743|4303030400149447|13508430017|1|0510|2|湖南湘西土家族苗族自治州|湖南湘西土家族苗族自治州、湖南怀化市|0|433100&|433100,431200&
					String xcmDate = fvs[0];
					DateFormat dateFormat = dateFormateMeta.toDateFormat();
					context.addFieldValue("xcmDate", dateFormat.parse(xcmDate));
					context.addFieldValue("xcmCiteCode", fvs[1]);
					context.addFieldValue("xcmCertNo", fvs[2]);
					context.addFieldValue("xcmMobile", fvs[3]);
					context.addFieldValue("xcmType", fvs[4]);
					context.addFieldValue("xcmToCiteCode", fvs[5]);
					context.addFieldValue("xcmToType", fvs[6]);
					context.addFieldValue("xcmFromCity", fvs[7]);
					context.addFieldValue("xcmToCity", fvs[8]);
					context.addFieldValue("xcmNo", fvs[9]);
					context.addFieldValue("xcmFromAreaCode", fvs[10]);
					context.addFieldValue("xcmToAreaCode", fvs[11]);
					context.addFieldValue("xcmCollectorTime", new Date());
					String filePath = (String) context.getMetaValue("filePath");
					context.addFieldValue("filePath", filePath);
					context.addIgnoreFieldMapping("@message");
					context.addIgnoreFieldMapping("@filemeta");
				}
				catch (Exception e){
					logger.error(message,e);
				}

			}
		});
		//映射和转换配置结束
		importBuilder.setExportResultHandler(new ExportResultHandler<String>() {
			@Override
			public void success(TaskCommand<String> taskCommand, String o) {
//				logger.info("result:"+o);
			}

			@Override
			public void error(TaskCommand<String> taskCommand, String o) {
				logger.warn("error:"+o);
			}

			@Override
			public void exception(TaskCommand<String> taskCommand, Throwable exception) {
				logger.warn("error:",exception);
			}


		});


		importBuilder.addCallInterceptor(new CallInterceptor() {
			@Override
			public void preCall(TaskContext taskContext) {

			}

			@Override
			public void afterCall(TaskContext taskContext) {
				if(taskContext != null) {
					FileTaskContext fileTaskContext = (FileTaskContext)taskContext;
					logger.info("afterCall ---- 文件{}导入情况:{}",fileTaskContext.getFileInfo().getOriginFilePath(),taskContext.getJobTaskMetrics().toString());
				}
			}

			@Override
			public void throwException(TaskContext taskContext, Throwable e) {
				if(taskContext != null) {
					taskContext.await();//等待数据异步处理完成
					FileTaskContext fileTaskContext = (FileTaskContext)taskContext;
					logger.info("文件{}导入情况:{}",fileTaskContext.getFileInfo().getOriginFilePath(),taskContext.getJobTaskMetrics().toString());
				}
			}
		});
		/**
		 * 内置线程池配置，实现多线程并行数据导入功能，作业完成退出时自动关闭该线程池
		 */
		importBuilder.setParallel(true);//设置为多线程并行批量导入,false串行
		importBuilder.setQueue(10);//设置批量导入线程池等待队列长度
		importBuilder.setThreadCount(5);//设置批量导入线程池工作线程数量
		importBuilder.setContinueOnError(true);//任务出现异常，是否继续执行作业：true（默认值）继续执行 false 中断作业执行
		importBuilder.setPrintTaskLog(true);

		/**
		 * 启动es数据导入文件并上传sftp/ftp作业
		 */
		DataStream dataStream = importBuilder.builder(true);
		dataStream.execute();//启动同步作业
		logger.info("job started.");
	}
}
