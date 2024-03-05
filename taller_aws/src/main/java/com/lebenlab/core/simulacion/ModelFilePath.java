package com.lebenlab.core.simulacion;

import com.lebenlab.core.FilePath;
import com.lebenlab.core.tbmaster.PG1;

import java.nio.file.Path;

import static com.lebenlab.core.FilePath.existDir;

/**
 * User: pedro@didekin.es
 * Date: 29/12/2019
 * Time: 14:09
 */
public class ModelFilePath implements FilePath {

    static final String rndName = "file_rndforest_";

    private final Path filePath;

    public ModelFilePath(PG1 pg1Model)
    {
        existDir(modelsDir);
        filePath = modelsDir.resolve(rndName + pg1Model.name());
    }

    @Override
    public Path path()
    {
        return filePath;
    }
}
