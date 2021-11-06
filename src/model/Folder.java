package model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import util.FATUtil;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Folder implements Serializable {

    private static final long serialVersionUID = 1L;

    //有个时间控制的变量；
    private String folderName;//文件夹名
    private String type;//文件夹类型
    private int diskNum;//起始盘块号

    private String location;//位置
    private double size;//大小
    private String space;//占用空间
    private Date createTime;//创建时间

    private Folder parent;//父文件夹
    private List<Object> children;//子文件夹
    private Path path;//路径对象

    private transient StringProperty folderNameP=new SimpleStringProperty();
    public StringProperty folderNamePProperty(){return folderNameP;}//UI获取property;
    private void setFolderNameP(){
        this.folderNameP.set(folderName);
    }//设置property;


    public Folder(String folderName){
        this.folderName=folderName;

    }

    public Folder(String folderName,String location,int diskNum,Folder parent){
        this.folderName=folderName;
        this.location=location;
        this.size=0;
        this.space=size+"KB";
        this.createTime=new Date();

        this.diskNum=diskNum;
        this.type= FATUtil.FOLDER;

        this.parent=parent;
        this.setChildren(new ArrayList<>());
        setFolderNameP();
    }

    public String getFolderName(){return folderName;}

    public void setFolderName(String folderName){
        this.folderName=folderName;
        setFolderNameP();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getDiskNum() {
        return diskNum;
    }

    public void setDiskNum(int diskNum) {
        this.diskNum = diskNum;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public double getSize(){return size;}

    public void setSize(double KBcount){
        this.size=KBcount;
        this.setSpace(size+"KB");
    }
    public String getSpace() {
        return space;
    }

    public void setSpace(String space) {
        this.space = space;
    }

    public String getCreateTime(){
        SimpleDateFormat format=new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss");
        return format.format(createTime);
    }

    public Folder getParent() {
        return parent;
    }

    public void setParent(Folder parent) {
        this.parent = parent;
    }

    public boolean hasParent(){return (parent==null)?false:true;}

    public List<Object> getChildren() {
        return children;
    }

    public void setChildren(List<Object> children) {
        this.children = children;
    }

    public void addChildren(Object child){this.children.add(child);}

    public void removeChildren(Object child){this.children.remove(child);}

    public boolean hasChild(){return children.isEmpty()?false:true;}

    public Path getPath(){return path;}

    public void setPath(Path path ){this.path=path;}

    private void readObject(ObjectInputStream s)throws IOException,ClassNotFoundException{
        s.defaultReadObject();
        folderNameP=new SimpleStringProperty(folderName);


    }//可以理解；

    @Override
    public  String toString(){return folderName;}




}
