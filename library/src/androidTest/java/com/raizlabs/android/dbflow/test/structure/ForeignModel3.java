package com.raizlabs.android.dbflow.test.structure;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.ContainerAdapter;
import com.raizlabs.android.dbflow.annotation.ForeignKeyReference;
import com.raizlabs.android.dbflow.annotation.Table;
import com.raizlabs.android.dbflow.test.TestDatabase;

/**
 * Description:
 */
@ContainerAdapter
@Table(databaseName = TestDatabase.NAME)
public class ForeignModel3 extends TestModel1 {

    @Column(columnType = Column.FOREIGN_KEY,
            references = {@ForeignKeyReference(columnName = "testmodel_id",
                                               columnType = Long.class, foreignColumnName = "id"
            )})
    Long testModelAI;
}
