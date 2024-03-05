package com.lebenlab.core.tbmaster;

import com.lebenlab.Jsonable;

import java.util.List;

import static com.lebenlab.core.tbmaster.TbMaestrasDao.maestrasDao;

/**
 * User: pedro@didekin.es
 * Date: 14/02/2020
 * Time: 11:08
 */
public enum TbMaestraAction {
    conceptos {
        @Override
        public List<? extends Jsonable> getTableValues() {
            return maestrasDao.getConceptos();
        }
    },
    incentivos {
        @Override
        public List<? extends Jsonable> getTableValues() {
            return maestrasDao.getIncentivos();
        }
    },
    medios_comunicacion {
        @Override
        public List<? extends Jsonable> getTableValues()
        {
            return maestrasDao.getMediosComunicacion();
        }
    },
    mercados {
        @Override
        public List<? extends Jsonable> getTableValues() {
            return maestrasDao.getMercados();
        }
    },
    pg1s {
        @Override
        public List<? extends Jsonable> getTableValues() {
            return maestrasDao.getPg1s();
        }
    },
    provincias {
        @Override
        public List<? extends Jsonable> getTableValues() {
            return maestrasDao.getProvincias();
        }
    };

    public abstract List<? extends Jsonable> getTableValues();
}
