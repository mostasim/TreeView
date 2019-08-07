package me.texy.treeviewdemo;

import android.view.View;

import me.texy.treeview.base.BaseNodeViewBinder;
import me.texy.treeview.base.BaseNodeViewFactory;


/**
 * Created by zxy on 17/4/23.
 */

public class MyNodeViewFactory extends BaseNodeViewFactory {


    @Override
    public BaseNodeViewBinder getNodeViewBinder(View view, int level) {

        levelTypes.add(0);
        levelTypes.add(1);
        levelTypes.add(2);
        levelTypes.add(3);

        switch (level) {
            case 0:
                return new FirstLevelNodeViewBinder(view);
            case 1:
                return new SecondLevelNodeViewBinder(view);
            case 2:
                return new SecondLevel2NodeViewBinder(view);
            case 3:
                return new ThirdLevelNodeViewBinder(view);
            default:
                return null;
        }
    }
}
