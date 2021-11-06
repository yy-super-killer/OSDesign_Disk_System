package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Path implements Serializable {

    private String pathName;
    private Path parent;
    private List<Path> children;

    public Path (String name,Path parent){
        this.setPathName(name);
        this.setParent(parent);
        this.children=new ArrayList<>();//要是有什么问题，可以再尖括号里加入Path；

    }
    public String getPathName(){return pathName;}

    public void setPathName(String pathName){this.pathName=pathName;}

    public Path getParent(){return parent;}

    public void setParent(Path parent){this.parent=parent;}

    public boolean hasParent(){return (parent==null)?false:true;}

    public List<Path> getChildren(){return children;}

    public void setChildren(List<Path> children){this.children=children;}

    public void addChildren(Path child){this.children.add(child);}

    public void removeChildren(Path child){this.children.remove(child);}

    public boolean hasChild(){return children.isEmpty()?false:true;}

    @Override
    public String toString(){return "Path [pathName="+pathName+"]";}



}
