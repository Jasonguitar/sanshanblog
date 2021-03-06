package xyz.sanshan.main.web.api.ueditor.upload;

import xyz.sanshan.main.web.api.ueditor.define.AppInfo;
import xyz.sanshan.main.web.api.ueditor.define.BaseState;
import xyz.sanshan.main.web.api.ueditor.define.State;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import xyz.sanshan.main.service.editor.UEditorFileService;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.io.*;

public class StorageManager {
    private static final Logger log = LoggerFactory.getLogger(StorageManager.class);

    public static final int BUFFER_SIZE = 8192;
//    private static Mongo mongo = new Mongo("localhost", 27017);
//    private static DB db = mongo.getDB("sanshanblog");
//    private static DBCollection collection = db.getCollection("MyImage");

    @Autowired
    private UEditorFileService UEditorFileService;

    private static UEditorFileService staticueditorFileService;

    /**
     * 通过PostConstruct方法间接静态注入UEditorFileService
     */
    @PostConstruct
    void  init(){
     staticueditorFileService=this.UEditorFileService;
    }

    public StorageManager() {

    }

    public static State saveBinaryFile(byte[] data, String path) {
        File file = new File(path);

        State state = valid(file);

        if (!state.isSuccess()) {
            return state;
        }

        try {
            BufferedOutputStream bos = new BufferedOutputStream(
                    new FileOutputStream(file));
            bos.write(data);
            bos.flush();
            bos.close();
        } catch (IOException ioe) {
            return new BaseState(false, AppInfo.IO_ERROR);
        }

        state = new BaseState(true, file.getAbsolutePath());
        state.putInfo("size", data.length);
        state.putInfo("title", file.getName());
        return state;
    }


    /**
     * 存到七牛云中
     *
     * @param is
     * @param path
     * @param maxSize
     * @return
     */
    public static State saveFileByInputStream(InputStream is, String path, String suffix,
                                              long maxSize) {
        State state = null;

        File tmpFile = getTmpFile();

        byte[] dataBuf = new byte[2048];
        BufferedInputStream bis = new BufferedInputStream(is, StorageManager.BUFFER_SIZE);

        try {
            BufferedOutputStream bos = new BufferedOutputStream(
                    new FileOutputStream(tmpFile), StorageManager.BUFFER_SIZE);

            int count = 0;
            while ((count = bis.read(dataBuf)) != -1) {
                bos.write(dataBuf, 0, count);
            }
            bos.flush();
            bos.close();

            if (tmpFile.length() > maxSize) {
                tmpFile.delete();
                return new BaseState(false, AppInfo.MAX_SIZE);
            }


            //原本的方式
//			state = saveTmpFile(tmpFile, path);

            DBObject metedata = new BasicDBObject();
            //对应的博客ID
            //metedata.put("blog_id",)
            try {
                staticueditorFileService.saveFile(new FileInputStream(tmpFile), path, suffix, metedata);
            } catch (Exception e) {
                log.error(e.getMessage());
                return new BaseState(false, AppInfo.IO_ERROR);
            }

            state = new BaseState(true);
            state.putInfo("size", tmpFile.length());
            state.putInfo("title", tmpFile.getName());

            if (!state.isSuccess()) {
                tmpFile.delete();
            }


            return state;

        } catch (IOException e) {
        }
        return new BaseState(false, AppInfo.IO_ERROR);
    }


    public static State saveFileByInputStream(InputStream is, String path, String suffix) {
        State state = null;

        File tmpFile = getTmpFile();

        byte[] dataBuf = new byte[2048];
        BufferedInputStream bis = new BufferedInputStream(is, StorageManager.BUFFER_SIZE);

        try {
            BufferedOutputStream bos = new BufferedOutputStream(
                    new FileOutputStream(tmpFile), StorageManager.BUFFER_SIZE);

            int count = 0;
            while ((count = bis.read(dataBuf)) != -1) {
                bos.write(dataBuf, 0, count);
            }
            bos.flush();
            bos.close();

            //原本的代码
            //			state = saveTmpFile(tmpFile, path);

            //存入云中
            DBObject metedata = new BasicDBObject();
            //元数据
            try {
                staticueditorFileService.saveFile(new FileInputStream(tmpFile), path, suffix, metedata);
            } catch (Exception e) {
                log.error(e.getMessage());
                return new BaseState(false, AppInfo.IO_ERROR);
            }

            state = new BaseState(true);
            state.putInfo("size", tmpFile.length());
            state.putInfo("title", tmpFile.getName());

            if (!state.isSuccess()) {
                tmpFile.delete();
            }

            return state;
        } catch (IOException e) {
        }
        return new BaseState(false, AppInfo.IO_ERROR);
    }

    private static File getTmpFile() {
        File tmpDir = FileUtils.getTempDirectory();
        String tmpFileName = (Math.random() * 10000 + "").replace(".", "");
        return new File(tmpDir, tmpFileName);
    }


    private static State valid(File file) {
        File parentPath = file.getParentFile();

        if ((!parentPath.exists()) && (!parentPath.mkdirs())) {
            return new BaseState(false, AppInfo.FAILED_CREATE_FILE);
        }

        if (!parentPath.canWrite()) {
            return new BaseState(false, AppInfo.PERMISSION_DENIED);
        }

        return new BaseState(true);
    }

    /**
     * 个人实现的另一种方法
     * @param content
     * @param filename
     * @param type
     * @param metedata
     */
//    private static void saveFile(InputStream content, String filename, String type, DBObject metedata) {
//        GridFS gfsPhoto = new GridFS(db, "fs");
//        GridFSInputFile gfsFile = gfsPhoto.createFile(content);
//        gfsFile.setFilename(filename);
//        gfsFile.setContentType(type);
//        gfsFile.setMetaData(metedata);
//        gfsFile.save();
//        mongo.close();
//    }
}
