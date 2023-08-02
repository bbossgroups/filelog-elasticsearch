package org.frameworkset.elasticsearch.imp.pdf;
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

import com.frameworkset.util.SimpleStringUtil;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.frameworkset.tran.CommonRecord;
import org.frameworkset.tran.DataRefactor;
import org.frameworkset.tran.DataStream;
import org.frameworkset.tran.ExportResultHandler;
import org.frameworkset.tran.config.ImportBuilder;
import org.frameworkset.tran.context.Context;
import org.frameworkset.tran.input.file.FileConfig;
import org.frameworkset.tran.input.file.FileFilter;
import org.frameworkset.tran.input.file.FilterFileInfo;
import org.frameworkset.tran.input.file.RecordExtractor;
import org.frameworkset.tran.input.pdf.PDFExtractor;
import org.frameworkset.tran.input.pdf.PDFFileConfig;
import org.frameworkset.tran.metrics.TaskMetrics;
import org.frameworkset.tran.plugin.custom.output.CustomOutPut;
import org.frameworkset.tran.plugin.custom.output.CustomOutputConfig;
import org.frameworkset.tran.plugin.file.input.PDFFileInputConfig;
import org.frameworkset.tran.schedule.CallInterceptor;
import org.frameworkset.tran.schedule.TaskContext;
import org.frameworkset.tran.task.TaskCommand;
import org.frameworkset.util.concurrent.Count;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>Description: 从日志文件采集日志数据并保存到</p>
 * <p></p>
 * <p>Copyright (c) 2020</p>
 * @Date 2021/2/1 14:39
 * @author biaoping.yin
 * @version 1.0
 */
public class PDFFile2CustomDemoOnce {
	private static Logger logger = LoggerFactory.getLogger(PDFFile2CustomDemoOnce.class);
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
        importBuilder.setJobId("PDFFile2CustomDemoOnce");//作业给一个唯一标识，避免和其他同类作业任务冲突
		importBuilder.setBatchSize(500)//设置批量入库的记录数
				.setFetchSize(1000);//设置按批读取文件行数
		//设置强制刷新检测空闲时间间隔，单位：毫秒，在空闲flushInterval后，还没有数据到来，强制将已经入列的数据进行存储操作，默认8秒,为0时关闭本机制
		importBuilder.setFlushInterval(10000l);
		PDFFileInputConfig wordFileInputConfig = new PDFFileInputConfig();
        wordFileInputConfig.setDisableScanNewFiles(true);
        wordFileInputConfig.setDisableScanNewFilesCheckpoint(false);
		//shebao_org,person_no, name, cert_type,cert_no,zhs_item  ,zhs_class ,zhs_sub_class,zhs_year  , zhs_level
		//配置excel文件列与导出字段名称映射关系
        PDFFileConfig pdfFileConfig = new PDFFileConfig();
        /**
         * 如果不设置setPdfExtractor，默认将文件内容放置到pdfContent字段中
         */
        pdfFileConfig.setPdfExtractor(new PDFExtractor() {
            @Override
            public void extractor(RecordExtractor<PDDocument> recordExtractor) throws Exception {
                Map record = new LinkedHashMap();
                if(recordExtractor.getDataObject() != null) {
                    PDFTextStripper pdfStripper = new PDFTextStripper();


                    //Retrieving text from PDF document

                    String text = pdfStripper.getText(recordExtractor.getDataObject());
                    record.put("wordContent", text);
                }
                else{


                    record.put("wordContent", "");
                }
                recordExtractor.addRecord(record);
            }




        });
		pdfFileConfig.setSourcePath("D:\\workspace\\bbossesdemo\\filelog-elasticsearch\\pdffiles")//指定目录
				.setFileFilter(new FileFilter() {
					@Override
					public boolean accept(FilterFileInfo fileInfo, FileConfig fileConfig) {
						//判断是否采集文件数据，返回true标识采集，false 不采集
						return true;
					}
				});//指定文件过滤器
		wordFileInputConfig.addConfig(pdfFileConfig);


		wordFileInputConfig.setEnableMeta(true);
		importBuilder.setInputConfig(wordFileInputConfig);
		//指定elasticsearch数据源名称，在application.properties文件中配置，default为默认的es数据源名称

//自己处理数据
        CustomOutputConfig customOutputConfig = new CustomOutputConfig();
        customOutputConfig.setCustomOutPut(new CustomOutPut() {
            @Override
            public void handleData(TaskContext taskContext, List<CommonRecord> datas) {

                //You can do any thing here for datas
                for(CommonRecord record:datas){
                    Map<String,Object> data = record.getDatas();
                    logger.info(SimpleStringUtil.object2json(data));
                }
            }
        });
        importBuilder.setOutputConfig(customOutputConfig);



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
//        dataStream.destroy(true);

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
