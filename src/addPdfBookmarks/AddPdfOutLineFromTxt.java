package addPdfBookmarks;

 import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.IntHashtable;
import com.itextpdf.text.pdf.PdfArray;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfDictionary;
import com.itextpdf.text.pdf.PdfGState;
import com.itextpdf.text.pdf.PdfIndirectReference;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfNumber;
import com.itextpdf.text.pdf.PdfObject;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.PdfString;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.SimpleBookmark;
 public class AddPdfOutLineFromTxt {
	 
    private static Stack<OutlineInfo> parentOutlineStack = new Stack<OutlineInfo>(); 
    
    // 所有者密码
    private static final String OWNERPASSWORD = "871023";
    
    // 添加水印图片路径
    private static final String imageFilePath = "/Users/shuyunguo/Documents/sy.jpg";
    
    public static void main(String[] args){
    	try {

    	
    	 String serial_number="056";
    	 String username="睿信财富";
    	 String parentPath="/Users/shuyunguo/Documents/a--诊断/"+serial_number+username;
    	 
    	 
     	BufferedReader in= new BufferedReader(new FileReader(parentPath+"/menus.txt"));
 	 
     	String sourcePdf=parentPath+"/"+username+"-诊断报告v1.0.pdf";
     	
     	String withBookmarkPdf=parentPath+"/"+username+"-诊断报告v1.0a.pdf";
     	
     	String withWaterMarkPdf=parentPath+"/"+username+"-诊断报告v1.0d.pdf";
     	
     	String withPerssionPdf=parentPath+"/"+username+"-诊断报告v1.0c.pdf";
     	
		createPdf(withBookmarkPdf,sourcePdf, in, 1);
		
		//waterMark(withBookmarkPdf,withWaterMarkPdf,"USER: "+username);
		
		//addPdfMark(withBookmarkPdf,withWaterMarkPdf);
		
		//addPermission(withWaterMarkPdf,withPerssionPdf);
			
			
//		addPdfMark("/Users/shuyunguo/Documents/全资本/全资本分析诊断报告v0.42b.pdf",
//					"/Users/shuyunguo/Documents/全资本/全资本分析诊断报告.pdf", "/Users/shuyunguo/Documents/全资本/shuiyin.png",123);
//			
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
//		} catch (DocumentException e) {
//			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    
    
    /**
     * 增加pdf书签目录
     * @param destPdf
     * @param sourcePdf
     * @param bufRead
     * @param pattern
     * @throws IOException
     * @throws DocumentException
     */
     public static void createPdf(String destPdf, String sourcePdf,
            BufferedReader bufRead, int pattern) throws IOException,
            DocumentException { 
         if (pattern != AddBookmarkConstants.RESERVED_OLD_OUTLINE
                &&  pattern != AddBookmarkConstants.RESERVED_NONE
                && pattern != AddBookmarkConstants.RESERVED_FIRST_OUTLINE)
            return;
         
        // 读入pdf文件
        PdfReader reader = new PdfReader(sourcePdf); 
         List<HashMap<String, Object>> outlines = new ArrayList<HashMap<String, Object>>();
         
        if (pattern == AddBookmarkConstants.RESERVED_OLD_OUTLINE) {
            outlines.addAll(SimpleBookmark.getBookmark(reader));
        } else if (pattern == AddBookmarkConstants.RESERVED_FIRST_OUTLINE) {
            addFirstOutlineReservedPdf(outlines, reader);
        } 
        
         addBookmarks(bufRead, outlines, null, 0);
        // 新建stamper
        PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(destPdf)); 
         stamper.setOutlines(outlines);
         
         stamper.close();
    } 
     
     /**
      * 
      * @param bufRead 目录文件
      * @param outlines
      * @param preOutline
      * @param preLevel 
      * @throws IOException
      */
     private static void addBookmarks(BufferedReader bufRead,
            List<HashMap<String, Object>> outlines,
            HashMap<String, Object> preOutline, int preLevel)
            throws IOException {
        String contentFormatLine = null;
        bufRead.mark(1);
        
        if ((contentFormatLine = bufRead.readLine()) != null) {
            FormattedBookmark bookmark = parseFormmattedText(contentFormatLine); 
             HashMap<String, Object> map = parseBookmarkToHashMap(bookmark); 
             int level = bookmark.getLevel();
             
             System.out.println("preLevel:"+preLevel + " level:"+level+"  title:"+bookmark.getTitle());
             
            // 如果n==m, 那么是同一层的，这个时候，就加到ArrayList中,继续往下面读取
            if (level == preLevel) {
            	if(outlines!=null){
            		  outlines.add(map);
                      addBookmarks(bufRead, outlines, map, level);
            	}else{
            		System.out.println("map is null");
            	}
              
            }
            // 如果n>m,那么可以肯定，该行是上一行的孩子，, new 一个kids的arraylist,并且加入到这个arraylist中
            else if (level > preLevel) {
                List<HashMap<String, Object>> kids = new ArrayList<HashMap<String, Object>>();
                kids.add(map);
                preOutline.put("Kids", kids);
                // 记录有孩子的outline信息
                parentOutlineStack.push(new OutlineInfo(preOutline, outlines,preLevel));
                addBookmarks(bufRead, kids, map, level);
            }
            // 如果n<m , 那么就是说孩子增加完了，退回到上层，bufRead倒退一行
            else if (level < preLevel) {
                bufRead.reset();
                OutlineInfo obj = parentOutlineStack.pop();
                addBookmarks(bufRead, obj.getOutlines(), obj.getPreOutline(), obj.getPreLevel());
            } 
            
         }
    } 
     
     /**
      * 转换为map
      * @param bookmark
      * @return
      */
     private static HashMap<String, Object> parseBookmarkToHashMap(
            FormattedBookmark bookmark) {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("Title", bookmark.getTitle());
        map.put("Action", "GoTo");
        map.put("Page", bookmark.getPage() + " Fit");
        return map;
    } 
     
     private static FormattedBookmark parseFormmattedText(String contentFormatLine) {
    	 //确保单一空格
    	 Pattern p = Pattern.compile("\\s+");
    	 Matcher m = p.matcher(contentFormatLine);
    	 contentFormatLine= m.replaceAll(" ");
    	 
        FormattedBookmark bookmark = new FormattedBookmark();
        String title = "";
        String destPage = ""; 
         // 当没有页码在字符串结尾的时候，一般就是书的名字，如果格式正确的话。
        int lastSpaceIndex = contentFormatLine.lastIndexOf(" ");
        if (lastSpaceIndex == -1) {
            title = contentFormatLine;
            destPage = "1";
        } else {
            title = contentFormatLine.substring(0, lastSpaceIndex);
            destPage = contentFormatLine.substring(lastSpaceIndex + 1);
        } 
         String[] titleSplit = title.split(" ");
        int dotCount = titleSplit[0].split("\\.").length-1; //-1 
        System.out.println(title+" level:"+dotCount);
        
        bookmark.setLevel(dotCount);
        bookmark.setPage(destPage);
        bookmark.setTitle(title);
        return bookmark;
    } 
     private static void addFirstOutlineReservedPdf(
            List<HashMap<String, Object>> outlines, PdfReader reader) {
        PdfDictionary catalog = reader.getCatalog();
        PdfObject obj = PdfReader.getPdfObjectRelease(catalog
                .get(PdfName.OUTLINES));
        // 没有书签
        if (obj == null || !obj.isDictionary())
            return;
        PdfDictionary outlinesDictionary = (PdfDictionary) obj;
        // 得到第一个书签
        PdfDictionary firstOutline = (PdfDictionary) PdfReader
                .getPdfObjectRelease(outlinesDictionary.get(PdfName.FIRST)); 
         PdfString titleObj = firstOutline.getAsString((PdfName.TITLE));
        String title = titleObj.toUnicodeString(); 
         PdfArray dest = firstOutline.getAsArray(PdfName.DEST); 
         if (dest == null) {
            PdfDictionary action = (PdfDictionary) PdfReader
                    .getPdfObjectRelease(firstOutline.get(PdfName.A));
            if (action != null) {
                if (PdfName.GOTO.equals(PdfReader.getPdfObjectRelease(action
                        .get(PdfName.S)))) {
                    dest = (PdfArray) PdfReader.getPdfObjectRelease(action
                            .get(PdfName.D));
                }
            }
        }
        String destStr = parseDestString(dest, reader); 
         String[] decodeStr = destStr.split(" ");
        int num = Integer.valueOf(decodeStr[0]);
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put("Title", title);
        map.put("Action", "GoTo");
        map.put("Page", num + " Fit"); 
         outlines.add(map);
    } 
     private static String parseDestString(PdfArray dest, PdfReader reader) {
        String destStr = "";
        if (dest.isString()) {
            destStr = dest.toString();
        } else if (dest.isName()) {
            destStr = PdfName.decodeName(dest.toString());
        } else if (dest.isArray()) {
            IntHashtable pages = new IntHashtable();
            int numPages = reader.getNumberOfPages();
            for (int k = 1; k <= numPages; ++k) {
                pages.put(reader.getPageOrigRef(k).getNumber(), k);
                reader.releasePage(k);
            } 
             destStr = makeBookmarkParam((PdfArray) dest, pages);
        }
        return destStr;
    } 
     private static String makeBookmarkParam(PdfArray dest, IntHashtable pages) {
        StringBuffer s = new StringBuffer();
        PdfObject obj = dest.getPdfObject(0);
        if (obj.isNumber()) {
            s.append(((PdfNumber) obj).intValue() + 1);
        } else {
            s.append(pages.get(getNumber((PdfIndirectReference) obj)));
        }
        s.append(' ').append(dest.getPdfObject(1).toString().substring(1));
        for (int k = 2; k < dest.size(); ++k) {
            s.append(' ').append(dest.getPdfObject(k).toString());
        }
        return s.toString();
    } 
     
     private static int getNumber(PdfIndirectReference indirect) {
        PdfDictionary pdfObj = (PdfDictionary) PdfReader
                .getPdfObjectRelease(indirect);
        if (pdfObj.contains(PdfName.TYPE)
                && pdfObj.get(PdfName.TYPE).equals(PdfName.PAGES)
                && pdfObj.contains(PdfName.KIDS)) {
            PdfArray kids = (PdfArray) pdfObj.get(PdfName.KIDS);
            indirect = (PdfIndirectReference) kids.getPdfObject(0);
        }
        return indirect.getNumber();
    }
 
     
     /**
         * 给pdf文件添加水印
         * @param InPdfFile 要加水印的原pdf文件路径
         * @param outPdfFile 加了水印后要输出的路径
         * @param markImagePath 水印图片路径
         * @param pageSize 原pdf文件的总页数
         * @throws Exception
         */
	public static void addPdfMark(String InPdfFile, String outPdfFile
			 ) throws Exception {
		PdfReader reader = new PdfReader(InPdfFile, "PDF".getBytes());
		PdfStamper stamp = new PdfStamper(reader, new FileOutputStream(
				outPdfFile));
		 int total = reader.getNumberOfPages() + 1;
		Image img = Image.getInstance(imageFilePath);// 插入水印  
		img.setAbsolutePosition(150, 100);
		for (int i = 1; i <= total; i++) {
			PdfContentByte under = stamp.getUnderContent(i);
			under.addImage(img);
		}
		stamp.close();// 关闭
		File tempfile = new File(InPdfFile);
		tempfile.deleteOnExit();
	}
	
	
	
	/**
	 * 设置水印
	 * @param inputFile
	 * @param outputFile
	 * @param userPassWord
	 * @param ownerPassWord
	 * @param waterMarkName
	 * @param permission
	 */
    private static void waterMark(String inputFile, String outputFile,
            String waterMarkName) {
        try {
            PdfReader reader = new PdfReader(inputFile);
            PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(
                    outputFile));

            BaseFont base =  BaseFont.createFont("STSong-Light", "UniGB-UCS2-H",BaseFont.NOT_EMBEDDED);
            
            int total = reader.getNumberOfPages() + 1;
//            Image image = Image.getInstance(imageFilePath);
//            
//           
//            image.setAbsolutePosition(0, 0);//坐标
            
//            image.setRotation(-20);//旋转 弧度
//            image.setRotationDegrees(-45);//旋转 角度
           // image.scaleAbsolute(2480,3508);//自定义大小
            //image.scalePercent(100);//依照比例缩放
            
//            PdfContentByte over = stamper.getUnderContent(1);
//
//            over.addImage(image);
            
            PdfContentByte under;
          
            int rise = 0;
            for (int i = 1; i < total; i++) {
                rise = 100;
                int x=80;
                under = stamper.getOverContent(i);
                // 添加图片
//                PdfContentByte over = stamper.getUnderContent(i);
//                over.addImage(image);
                
                
                PdfGState gs = new PdfGState();
                gs.setFillOpacity(0.1f);// 设置透明度为0.1
                under.setGState(gs);
                under.beginText();
                
                
                under.setFontAndSize(base, 40);
                // 设置水印文字字体倾斜 开始
                    under.setTextMatrix(180, 100);
                    for (int k = 0; k < 5; k++) {
                        under.setTextMatrix(x,rise);
                        under.showText(waterMarkName);
                        rise += 150;
                        x+=50;
                    }
                    
                // 字体设置结束
                under.endText();
                
            }
            stamper.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * 权限控制
     * @param inputFile
     * @param outputFile
     * @param ownerPassWord
     * @param permission
     */
    private static void addPermission(String inputFile, String outputFile
            ) {
    	 PdfReader reader;
		try {
			reader = new PdfReader(inputFile);
			PdfStamper stamper1 = new PdfStamper(reader, new FileOutputStream(
	                 outputFile));
			// 设置密码
			stamper1.setEncryption(null, OWNERPASSWORD.getBytes(),
            		PdfWriter.ALLOW_SCREENREADERS, PdfWriter.STANDARD_ENCRYPTION_128);//PdfWriter.ENCRYPTION_AES_128
			stamper1.close();
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
         
    	
    }

}
 