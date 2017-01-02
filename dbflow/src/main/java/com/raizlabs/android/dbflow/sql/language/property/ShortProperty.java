package com.raizlabs.android.dbflow.sql.language.property;

import com.raizlabs.android.dbflow.sql.language.BaseModelQueriable;
import com.raizlabs.android.dbflow.sql.language.Condition;
import com.raizlabs.android.dbflow.sql.language.ITypeConditional;
import com.raizlabs.android.dbflow.sql.language.NameAlias;

import static com.raizlabs.android.dbflow.sql.language.Condition.column;

/**
 * Description: Basic {@link short} property. Accepts only short, {@link BaseModelQueriable}, and
 * {@link ITypeConditional} objects.
 */
public class ShortProperty extends PrimitiveProperty<ShortProperty> {

    public ShortProperty(Class<?> table, NameAlias nameAlias) {
        super(table, nameAlias);
    }

    public ShortProperty(Class<?> table, String columnName) {
        this(table, new NameAlias.Builder(columnName).build());
    }

    public ShortProperty(Class<?> table, String columnName, String aliasName) {
        this(table, new NameAlias.Builder(columnName).as(aliasName).build());
    }

    @Override
    protected ShortProperty newPropertyInstance(Class<?> table, NameAlias nameAlias) {
        return new ShortProperty(table, nameAlias);
    }

    public Condition is(short value) {
        return column(nameAlias).is(value);
    }

    public Condition eq(short value) {
        return column(nameAlias).eq(value);
    }

    public Condition isNot(short value) {
        return column(nameAlias).isNot(value);
    }

    public Condition notEq(short value) {
        return column(nameAlias).notEq(value);
    }

    public Condition like(short value) {
        return column(nameAlias).like(String.valueOf(value));
    }

    public Condition notLike(short value) {
        return column(nameAlias).notLike(String.valueOf(value));
    }

    public Condition glob(short value) {
        return column(nameAlias).glob(String.valueOf(value));
    }

    public Condition greaterThan(short value) {
        return column(nameAlias).greaterThan(value);
    }

    public Condition greaterThanOrEq(short value) {
        return column(nameAlias).greaterThanOrEq(value);
    }

    public Condition lessThan(short value) {
        return column(nameAlias).lessThan(value);
    }

    public Condition lessThanOrEq(short value) {
        return column(nameAlias).lessThanOrEq(value);
    }

    public Condition.Between between(short value) {
        return column(nameAlias).between(value);
    }

    public Condition.In in(short firstValue, short... values) {
        Condition.In in = column(nameAlias).in(firstValue);
        for (short value : values) {
            in.and(value);
        }
        return in;
    }

    public Condition.In notIn(short firstValue, short... values) {
        Condition.In in = column(nameAlias).notIn(firstValue);
        for (short value : values) {
            in.and(value);
        }
        return in;
    }

    public Condition concatenate(short value) {
        return column(nameAlias).concatenate(value);
    }

}
