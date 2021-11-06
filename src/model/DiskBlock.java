package model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import util.FATUtil;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;

public class DiskBlock implements Serializable {

    private static final long serialVersionUID=1L;
    private int no;
    private int index;//值
    private String type;//类型，文件，文件夹子，空
    private Object object;//文件或者文件夹对象
    private boolean begin;

    private transient StringProperty noP=new SimpleStringProperty();
    private transient StringProperty indexP=new SimpleStringProperty();
    private transient StringProperty typeP=new SimpleStringProperty();
    private transient StringProperty objectP=new SimpleStringProperty();//用于绑定属性；

    public StringProperty noPProperty() {
        return noP;
    }
    public StringProperty indexPProperty() {
        return indexP;
    }
    public StringProperty typePProperty() {
        return typeP;
    }
    public StringProperty objectPProperty() {
        return objectP;
    }

    private void setNoP(){
        this.noP.set(String.valueOf(no));
    }
    private void setIndexP() {
        this.indexP.set(String.valueOf(index));
    }
    private void setTypeP() {
        this.typeP.set(type);
    }
    private void setObjectP(){
        this.objectP.set(object==null?"":object.toString());
    }

    public DiskBlock(int no,int index,String type,Object object){
        super();
        this.no=no;
        this.index=index;
        this.type=type;
        this.object=object;
        this.begin=false;
        setNoP();
        setObjectP();
        setIndexP();
        setTypeP();
    }

    public int getNo() {
        return no;
    }

    public void setNo(int no) {
        this.no = no;
        setNoP();
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
        setIndexP();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
        setTypeP();
    }

    public Object getObject() {
        return object;
    }
    public void setObject(Object object){
        this.object=object;
        if(object instanceof File)//bind方法用于将函数体内的this绑定到某个对象，然后返回一个新函数。
        {
            this.objectP.bind(((File)object).fileNamePProperty());

        }
        else if(object instanceof Folder){
            this.objectP.bind(((Folder)object).folderNamePProperty());

        } else {
            this.objectP.unbind();
            setObjectP();
        }
        /*
        实现属性和绑定，当对象被绑定之后，一个对象的改变会自动反射到另一个对象。
        绑定用于GUI用户界面中，会将应用程序的基础数据改变同步显示出来。
        当检测到变化时自动更新性能列表
        */

        }
    public boolean isBegin(){return begin;}

    public void setBegin(boolean begin) {
        this.begin = begin;
    }

    public void allocBlock(int index ,String type ,Object object,boolean begin){
        setIndex(index);
        setType(type);
        setBegin(begin);
        setObject(object);
    }

    public void clearBlock(){
        setIndex(0);
        setType(FATUtil.EMPTY);
        setObject(null);
        setBegin(false);
    }

    public boolean isFree() {
        return index == 0;//下标是否为空，表明是否该磁盘块被使用

    }

    private void readObject(ObjectInputStream s) throws IOException,ClassNotFoundException{
        s.defaultReadObject();
        noP=new SimpleStringProperty(String.valueOf(no));
        indexP=new SimpleStringProperty(String.valueOf(index));
        typeP=new SimpleStringProperty(type);
        objectP=new SimpleStringProperty(object==null?"":object.toString());
        setObject(object);
    }
    //从objectInputStream中读取对象。defaultReadObject可以访问非静态字段。

    @Override
    public String toString(){
        Object object=getObject();
        if(object instanceof File){
            return ((File)object).toString();//如果是文件返回文件名；
        }else{
            return ((Folder)object).toString();//如果是文件夹返回文件夹名；
        }

    }
}
