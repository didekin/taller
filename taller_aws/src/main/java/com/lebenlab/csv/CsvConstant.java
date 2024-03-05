package com.lebenlab.csv;

/**
 * User: pedro@didekin.es
 * Date: 22/01/2020
 * Time: 16:13
 */
public enum CsvConstant {

    delimiter(";"),
    newLine("\r\n"),
    point_csv(".csv"),
    point_zip(".zip"),
    multipart_form_file_param("upload_file"),
    ;

    private final String constantValue;

    CsvConstant(String constantValueIn)
    {
        constantValue = constantValueIn;
    }

    @Override
    public String toString()
    {
        return constantValue;
    }
}
