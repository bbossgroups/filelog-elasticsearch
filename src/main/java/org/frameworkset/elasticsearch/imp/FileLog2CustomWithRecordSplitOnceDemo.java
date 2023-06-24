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

import org.frameworkset.elasticsearch.entity.KeyMap;
import org.frameworkset.tran.*;
import org.frameworkset.tran.config.ImportBuilder;
import org.frameworkset.tran.context.Context;
import org.frameworkset.tran.input.file.FileConfig;
import org.frameworkset.tran.input.file.FileFilter;
import org.frameworkset.tran.input.file.FilterFileInfo;
import org.frameworkset.tran.plugin.custom.output.CustomOutPut;
import org.frameworkset.tran.plugin.custom.output.CustomOutputConfig;
import org.frameworkset.tran.plugin.file.input.FileInputConfig;
import org.frameworkset.tran.record.SplitHandler;
import org.frameworkset.tran.schedule.TaskContext;
import org.frameworkset.tran.task.TaskCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
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
public class FileLog2CustomWithRecordSplitOnceDemo {
	private static Logger logger = LoggerFactory.getLogger(FileLog2CustomWithRecordSplitOnceDemo.class);
	public static void main(String[] args){

		ImportBuilder importBuilder = new ImportBuilder();
		importBuilder.setBatchSize(500)//设置批量入库的记录数
				.setFetchSize(1000);//设置按批读取文件行数
		//设置强制刷新检测空闲时间间隔，单位：毫秒，在空闲flushInterval后，还没有数据到来，强制将已经入列的数据进行存储操作，默认8秒,为0时关闭本机制
		importBuilder.setFlushInterval(10000l);
		importBuilder.setSplitFieldName("@message");
		importBuilder.setSplitHandler(new SplitHandler() {
			@Override
			public List<KeyMap> splitField(TaskContext taskContext,
														   Record record, Object splitValue) {
//				Map<String,Object > data = (Map<String, Object>) record.getData();
				List<KeyMap> splitDatas = new ArrayList<>();
				//模拟将数据切割为10条记录
				for(int i = 0 ; i < 10; i ++){
					KeyMap d = new KeyMap();
					d.put("message",i+"-"+splitValue);
//					d.setKey(SimpleStringUtil.getUUID());//如果是往kafka推送数据，可以设置推送的key
					splitDatas.add(d);
				}
				return splitDatas;
			}
		});
		importBuilder.addFieldMapping("@message","message");
		FileInputConfig config = new FileInputConfig();
		config.setCharsetEncode("GB2312");
        /**
         * 一次性文件数据采集，禁用新文件扫描机制，控制文件采集的一次性全量处理，这样就不会进行增量监听了，和其他插件的一次性采集设置保持一致
         */
        config.setDisableScanNewFiles(true);
        /**
         * 一次性文件全量采集的处理，是否禁止记录文件采集状态，false 不禁止，true 禁止，不禁止
         * 情况下作业重启，已经采集过的文件不会再采集，未采集完的文件，从上次采集截止的位置开始采集
         * 默认 true 禁止
         */
        config.setDisableScanNewFilesCheckpoint(false);
        config.setMaxFilesThreshold(10);
		config.addConfig(new FileConfig().setSourcePath("D:\\oncelogs")//指定目录
										.setFileHeadLineRegular("^\\[[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}:[0-9]{3}\\]")//指定多行记录的开头识别标记，正则表达式
										.setFileFilter(new FileFilter() {
											@Override
											public boolean accept(FilterFileInfo fileInfo, FileConfig fileConfig) {
												//判断是否采集文件数据，返回true标识采集，false 不采集
												boolean r = fileInfo.getFileName().startsWith("metrics-report");
												return r;
											}
										})//指定文件过滤器
                                        .setDeleteEOFFile(true)//删除采集完毕的文件
//                                        .setCloseOlderTime(10000L)
                                        .setCloseEOF(true)//采集完毕后，关闭文件采集通道
										.addField("tag","elasticsearch")//添加字段tag到记录中
						);

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
		importBuilder.setInputConfig(config);
        CustomOutputConfig customOutputConfig = new CustomOutputConfig();
        customOutputConfig.setCustomOutPut(new CustomOutPut() {
            @Override
            public void handleData(TaskContext taskContext, List<CommonRecord> datas) {

                //You can do any thing here for datas
                for(CommonRecord record:datas){
                    Map<String,Object> data = record.getDatas();
//                    logger.info(SimpleStringUtil.object2json(data));
                }
            }
        });
        importBuilder.setOutputConfig(customOutputConfig);
		/**
		 * 重新设置es数据结构
		 */
		importBuilder.setDataRefactor(new DataRefactor() {
			public void refactor(Context context) throws Exception  {
				//可以根据条件定义是否丢弃当前记录
				//context.setDrop(true);return;
//				if(s.incrementAndGet() % 2 == 0) {
//					context.setDrop(true);
//					return;
//				}
//				System.out.println(data);

//				context.addFieldValue("author","duoduo");//将会覆盖全局设置的author变量
				context.addFieldValue("title","解放");
				context.addFieldValue("subtitle","小康");
				

			}
		});
		//映射和转换配置结束
		importBuilder.setExportResultHandler(new ExportResultHandler() {
			@Override
			public void success(TaskCommand taskCommand, Object o) {
				logger.info("result:"+o);
			}

			@Override
			public void error(TaskCommand taskCommand, Object o) {
				logger.warn("error:"+o);
			}

			@Override
			public void exception(TaskCommand taskCommand, Throwable exception) {
				logger.warn("error:",exception);
			}


		});
		/**
		 * 内置线程池配置，实现多线程并行数据导入功能，作业完成退出时自动关闭该线程池
		 */
		importBuilder.setParallel(true);//设置为多线程并行批量导入,false串行
		importBuilder.setQueue(10);//设置批量导入线程池等待队列长度
		importBuilder.setThreadCount(50);//设置批量导入线程池工作线程数量
		importBuilder.setContinueOnError(true);//任务出现异常，是否继续执行作业：true（默认值）继续执行 false 中断作业执行
		importBuilder.setPrintTaskLog(true);
//        importBuilder.setJobClosedListener(new AsynJobClosedListener() {
//            @Override
//            public void jobClosed(ImportContext importContext, Throwable throwable) {
//                File files = new File("D:\\oncelogs");
//                if(files.exists()){
//                    do {
//                        File[] fs = files.listFiles(new FilenameFilter() {
//                            @Override
//                            public boolean accept(File dir, String name) {
//                                boolean r = name.startsWith("metrics-report");
//                                return r;
//                            }
//                        });
//                      if(fs == null || fs.length == 0){
//                          break;
//                      }
//                        try {
//                            sleep(1000L);
//                        } catch (InterruptedException e) {
//
//                        }
//                    }while(true);
//
//
//
//                }
//            }
//        });
		/**
		 * 启动es数据导入文件并上传sftp/ftp作业
		 */
		DataStream dataStream = importBuilder.builder();
		dataStream.execute();//启动同步作业
	}
}
