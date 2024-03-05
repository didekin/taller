package com.lebenlab.core.simulacion;

/**
 * User: pedro@didekin.es
 * Date: 10/02/2020
 * Time: 14:13
 * <p>
 * 3. Calculation of model.
 * 4. Serialization and archive of model in disk or memory.
 */
public class ModelForSimulationDao implements ModelForSimulationDaoIf {

    public static final ModelForSimulationDao modelSimulateDao = new ModelForSimulationDao();

    private ModelForSimulationDao()
    {
    }
}
