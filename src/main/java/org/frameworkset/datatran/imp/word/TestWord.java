package org.frameworkset.datatran.imp.word;
/**
 * Copyright 2024 bboss
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


import org.apache.poi.ooxml.POIXMLProperties;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xwpf.usermodel.*;
import org.apache.xmlbeans.XmlException;
import org.openxmlformats.schemas.officeDocument.x2006.customProperties.CTProperty;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTSimpleField;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTStyles;


import java.io.*;
import java.util.List;

public class TestWord {
    
    public static String readWord(String filePath) throws IOException {
        StringBuilder builder = new StringBuilder();
        OPCPackage opcPackage = null;
        try {
            opcPackage = OPCPackage.open(new File(filePath));
            XWPFDocument document = new XWPFDocument(opcPackage);
//            org.openxmlformats.schemas.officeDocument.x2006.docPropsVTypes.
            // 读取文档属性
            POIXMLProperties.CustomProperties customProperties = document.getProperties().getCustomProperties();
            List<CTProperty> properties = customProperties.getUnderlyingProperties().getPropertyList();
            for (CTProperty property : properties) {
                String propertyName = property.getName();
                if (propertyName == null) {
                    continue;
                }
                builder.append(property.getName() + "-------"+property.getLpwstr()).append("\r\n");
            }

            // 读取所有段落
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            for (XWPFParagraph para : paragraphs) {
                // 获取CTP对象
                CTP ctp = para.getCTP();
                if(ctp.getFldSimpleList().size() > 0){
                    CTSimpleField fldSimple = ctp.getFldSimpleList().get(0);
                    builder.append(para.getText() + "-------隐藏的属性-------" +fldSimple.getInstr()).append("\r\n");
                }
            }

            // 遍历段落并读取XML片段
            for (XWPFParagraph para : paragraphs) {
                String text = para.getText(); // 获取段落文本
                String xmlFragment = para.getCTP().xmlText(); // 获取对应的 XML 文本片段
                builder.append(text).append("\r\n");
                // 在这里处理你的 XML 片段
            }
            return builder.toString();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            opcPackage.close();
        }
        return "";
    }
    public static void main(String[] args) throws IOException {

       System.out.println( readWord("c:/workdir/user_world_1722564194343.docx"));

    }
    
    public static void writeWord() throws IOException, XmlException {

        // 新建的word文档对象
        XWPFDocument doc = new XWPFDocument();
// word整体样式
// 读取模板文档
        XWPFDocument template = new XWPFDocument(new FileInputStream("D:\\test\\poi\\word\\format.docx"));
// 获得模板文档的整体样式
        CTStyles wordStyles = template.getStyle();
// 获取新建文档对象的样式
        XWPFStyles newStyles = doc.createStyles();
// 关键行// 修改设置文档样式为静态块中读取到的样式
        newStyles.setStyles(wordStyles);
// 标题1，1级大纲
        XWPFParagraph para1 = doc.createParagraph();
// 关键行// 1级大纲
        para1.setStyle("1");
        XWPFRun run1 = para1.createRun();
// 标题内容
        run1.setText("标题1");

// 标题2，2级大纲
        XWPFParagraph para2 = doc.createParagraph();
// 关键行// 2级大纲
        para2.setStyle("2");
        XWPFRun run2 = para2.createRun();
// 标题内容
        run2.setText("标题2");

// 正文
        XWPFParagraph paraX = doc.createParagraph();
        XWPFRun runX = paraX.createRun();
        for(int i=0;i<100;i++) {
            // 正文内容
            runX.setText("正文\r\n");
        }

// word写入到文件
        FileOutputStream fos;
        try {
            fos = new FileOutputStream("D:\\test\\poi\\word\\test.docx");
            doc.write(fos);
            fos.close();
        } catch (Exception e) {
            // TODO 自动生成的 catch 块
            e.printStackTrace();
        }

     
    }
}





