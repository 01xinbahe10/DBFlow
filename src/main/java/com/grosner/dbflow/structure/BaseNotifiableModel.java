package com.grosner.dbflow.structure;

import com.grosner.dbflow.config.FlowManager;
import com.grosner.dbflow.sql.SqlUtils;

/**
 * Author: andrewgrosner
 * Contributors: { }
 * Description:
 */
public class BaseNotifiableModel extends BaseModel {

    @Override
    public void save(boolean async) {
        SqlUtils.save(FlowManager.getInstance(), this, async, SqlUtils.SAVE_MODE_DEFAULT, true);
    }

    public void insert(boolean async) {
        SqlUtils.save(FlowManager.getInstance(), this, async, SqlUtils.SAVE_MODE_INSERT, false);
    }

    @Override
    public void update(boolean async) {
        SqlUtils.save(FlowManager.getInstance(), this, async, SqlUtils.SAVE_MODE_UPDATE, true);
    }

}
