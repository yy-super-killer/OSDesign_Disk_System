package util;

import model.File;
import model.Folder;

import java.util.List;

public class FATUtil {
    public static final String ICO = "res/ico.png";
    public static final String FOLDER_IMG = "res/folder.png";
    public static final String FILE_IMG = "res/file.png";
    public static final String DISK_IMG = "res/disk.png";
    public static final String TREE_NODE_IMG = "res/node.png";
    public static final String FORWARD_IMG = "res/forward.png";
    public static final String BACK_IMG = "res/back.png";
    public static final String SAVE_IMG = "res/save.png";
    public static final String CLOSE_IMG = "res/close.png";
    public static final String INTRODUCTION_IMG="res/introduction.png";
    public static final String SEARCH_IMG="res/search.png";



    //磁盘状态
    public static final int FREE = 0;

    public static final int FLAGREAD=0;
    public static final int FLAGWRITE=1;

    public static final int END = 255;
    public static final int ERROR = -1;//错误代码，比如说磁盘块用完了。

    //以下是文件类型
    public static final String DISK = "磁盘";
    public static final String FOLDER="文件夹";
    public static final String FILE="文件";
    public static final String EMPTY="空";



    public static double getSize(int length){return Double.parseDouble(String.format("%.2f",length/1024.0));}

    public static double getFolderSize(Folder folder){
        List<Object>  children=folder.getChildren();
        double size=0;
        for(Object child:children){
            if(child instanceof File){
                size+=((File)child).getSize();
            }else{
                size+=getFolderSize((Folder) child);//递归求值
            }
        }
        return Double.parseDouble((String.format("%.2f", size)));
    }

    public static int blocksCount(int length){//计算blocks
        if (length <= 64){
            return 1;
        } else {
            int n = 0;
            if (length % 64 == 0){
                n = length / 64;
            } else {
                n = length / 64;
                n++;
            }
            return n;
        }
    }
}
