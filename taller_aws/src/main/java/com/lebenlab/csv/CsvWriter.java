package com.lebenlab.csv;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.charset.Charset;

import com.lebenlab.ProcessArgException;
import smile.data.DataFrame;
import smile.data.Tuple;

import static com.lebenlab.csv.CsvConstant.delimiter;
import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * User: pedro@didekin
 * Date: 02/04/2020
 * Time: 15:48
 */
public final class CsvWriter {

    public final Charset charset;
    public final String errorMsg;
    public final String header;

    public CsvWriter(String errorMsg, String header)
    {
        this(UTF_8, errorMsg, header);
    }

    public CsvWriter(Charset charset, String errorMsg, String header)
    {
        this.charset = charset;
        this.errorMsg = errorMsg;
        this.header = header;
    }

    public InputStream writeDfToStream(DataFrame df)
    {
        try (ByteArrayOutputStream byteArrOut = new ByteArrayOutputStream(1024);
             PrintWriter writer = new PrintWriter(byteArrOut, true, charset)
        ) {
            writer.println(header); // Cabecera.
            final var numDfFields = df.schema().length();
            Tuple row;
            StringBuilder rowStr;
            for (int i = 0; i < df.size(); ++i) {
                row = df.get(i); // get row i.
                rowStr = new StringBuilder();
                for (int j = 0; j < numDfFields; ++j) {
                    rowStr.append(row.getString(j)).append(delimiter.toString());  // append field j in row i.
                }
                rowStr.deleteCharAt(rowStr.length() - 1);
                writer.println(rowStr);
            }
            return new ByteArrayInputStream(byteArrOut.toByteArray());
        } catch (IOException e) {
            throw new ProcessArgException(errorMsg + e.getMessage());
        }
    }
}
