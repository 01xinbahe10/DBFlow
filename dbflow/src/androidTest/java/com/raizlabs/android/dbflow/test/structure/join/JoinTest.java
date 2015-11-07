package com.raizlabs.android.dbflow.test.structure.join;

import com.raizlabs.android.dbflow.sql.language.From;
import com.raizlabs.android.dbflow.sql.language.Join;
import com.raizlabs.android.dbflow.sql.language.SQLite;
import com.raizlabs.android.dbflow.test.FlowTestCase;

import java.util.List;

/**
 * Description:
 */
public class JoinTest extends FlowTestCase {


    public void testJoins() {
        From<Company> joinQuery =
                SQLite.select(Department_Table.emp_id, Company_Table.name, Department_Table.dept)
                        .from(Company.class)
                        .join(Department.class, Join.JoinType.INNER)
                        .on(Company_Table.id.withTable().eq(Department_Table.emp_id.withTable()));
        String query = joinQuery.getQuery();

        assertEquals("SELECT `emp_id`,`name`,`dept` FROM `Company` INNER JOIN `Department` " +
                "ON `Company`.`id`=`Department`.`emp_id`", query.trim());

        List<CompanyDepartmentJoin> companyDepartmentJoins = joinQuery.queryCustomList(CompanyDepartmentJoin.class);
        
    }
}
