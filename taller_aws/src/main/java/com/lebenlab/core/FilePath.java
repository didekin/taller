package com.lebenlab.core;

import com.lebenlab.ProcessArgException;

import java.nio.file.Path;

import static com.lebenlab.ProcessArgException.error_nonexistent_dir;
import static java.nio.file.Files.isDirectory;
import static java.nio.file.Paths.get;

/**
 * User: pedro@didekin
 * Date: 30/03/2020
 * Time: 16:12
 */
public interface FilePath {

    Path appfilesPath = get(System.getenv("BOSCH_HOME")).resolve("app_files");
    Path modelsDir = appfilesPath.resolve("models");

    static void existDir(Path pathIn)
    {
        if (!isDirectory(pathIn)) {
            throw new ProcessArgException(error_nonexistent_dir + pathIn.toString());
        }
    }

    Path path();
}
