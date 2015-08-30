package com.raizlabs.android.dbflow.processor.definition.column;

import com.raizlabs.android.dbflow.annotation.Column;
import com.raizlabs.android.dbflow.annotation.ForeignKeyReference;
import com.raizlabs.android.dbflow.processor.utils.StringUtils;
import com.squareup.javapoet.TypeName;

/**
 * Description:
 */
public class PrivateColumnAccess extends BaseColumnAccess {

    private String getterName;
    private String setterName;

    public PrivateColumnAccess(Column column) {
        getterName = column.getterName();
        setterName = column.setterName();
    }

    public PrivateColumnAccess(ForeignKeyReference reference) {
        getterName = reference.referencedGetterName();
        setterName = reference.referencedSetterName();
    }

    @Override
    String getColumnAccessString(TypeName fieldType, String elementName, boolean isModelContainerAdapter, String variableNameString) {
        if (StringUtils.isNullOrEmpty(getterName)) {
            return String.format("%1s.get%1s()", variableNameString, capitalize(elementName));
        } else {
            return String.format("%1s.%1s()", variableNameString, getterName);
        }
    }

    @Override
    String getShortAccessString(String elementName, boolean isModelContainerAdapter) {
        if (StringUtils.isNullOrEmpty(getterName)) {
            return String.format("get%1s()", capitalize(elementName));
        } else {
            return String.format("%1s()", getterName);
        }
    }

    @Override
    String setColumnAccessString(TypeName fieldType, String elementName, String formattedAccess, boolean isModelContainerAdapter, String variableNameString) {
        if (StringUtils.isNullOrEmpty(setterName)) {
            return String.format("%1s.set%1s(%1s)", variableNameString, capitalize(elementName), formattedAccess);
        } else {
            return String.format("%1s.%1s(%1s)", variableNameString, setterName, formattedAccess);
        }
    }

    private static String capitalize(String str) {
        if (str == null || str.trim().length() == 0) {
            return str;
        }

        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
