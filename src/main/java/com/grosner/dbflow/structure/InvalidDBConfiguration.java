package com.grosner.dbflow.structure;

/**
 * Author: andrewgrosner
 * Contributors: { }
 * Description:
 */
public class InvalidDBConfiguration extends RuntimeException {

    public InvalidDBConfiguration(String databaseName) {
        super("The database " + databaseName + " in a multidatabase setup needs to manually define the model classes");
    }

    public InvalidDBConfiguration(String dummy, String modelName) {
        super("The manager in a multidatabase setup did not include " + modelName + " in the DBConfiguration");
    }
}
