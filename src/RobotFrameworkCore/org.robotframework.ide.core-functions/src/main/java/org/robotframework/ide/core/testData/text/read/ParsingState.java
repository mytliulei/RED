package org.robotframework.ide.core.testData.text.read;

public enum ParsingState {
    /**
     * 
     */
    UNKNOWN,
    /**
     * 
     */
    TRASH,
    /**
     * 
     */
    TABLE_HEADER_COLUMN,
    /**
     * 
     */
    COMMENT,
    /**
     * 
     */
    SETTING_TABLE_HEADER,
    /**
     * 
     */
    SETTING_TABLE_INSIDE,
    /**
     * 
     */
    VARIABLE_TABLE_HEADER,
    /**
     * 
     */
    VARIABLE_TABLE_INSIDE,
    /**
     * 
     */
    TEST_CASE_TABLE_HEADER,
    /**
     * 
     */
    TEST_CASE_TABLE_INSIDE,
    /**
     * 
     */
    KEYWORD_TABLE_HEADER,
    /**
     * 
     */
    KEYWORD_TABLE_INSIDE,
    /**
     * 
     */
    SETTING_LIBRARY_IMPORT,
    /**
     * 
     */
    SETTING_LIBRARY_NAME_OR_PATH,
    /**
     * 
     */
    SETTING_LIBRARY_ARGUMENTS,
    /**
     * 
     */
    SETTING_LIBRARY_IMPORT_ALIAS,
    /**
     * 
     */
    SETTING_LIBRARY_IMPORT_ALIAS_VALUE,
    /**
     * 
     */
    SETTING_VARIABLE_IMPORT,
    /**
     * 
     */
    SETTING_VARIABLE_IMPORT_PATH,
    /**
     * 
     */
    SETTING_VARIABLE_ARGUMENTS,
    /**
     * 
     */
    SETTING_RESOURCE_IMPORT,
    /**
     * 
     */
    SETTING_RESOURCE_IMPORT_PATH,
    /**
     * 
     */
    SETTING_RESOURCE_UNWANTED_ARGUMENTS,
    /**
     * 
     */
    SETTING_DOCUMENTATION,
    /**
     * 
     */
    SETTING_DOCUMENTATION_TEXT,
    /**
     * 
     */
    SETTING_METADATA,
    /**
     * 
     */
    SETTING_METADATA_KEY,
    /**
     * 
     */
    SETTING_METADATA_VALUE,
    /**
     * 
     */
    SETTING_SUITE_SETUP,
    /**
     * 
     */
    SETTING_SUITE_SETUP_KEYWORD,
    /**
     *  
     */
    SETTING_SUITE_SETUP_KEYWORD_ARGUMENT,
    /**
     * 
     */
    SETTING_SUITE_TEARDOWN,
    /**
     * 
     */
    SETTING_SUITE_TEARDOWN_KEYWORD,
    /**
     *  
     */
    SETTING_SUITE_TEARDOWN_KEYWORD_ARGUMENT;
}
