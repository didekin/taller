package com.lebenlab.core;

import com.lebenlab.BeanBuilder;
import com.lebenlab.Jsonable;
import com.lebenlab.ProcessArgException;
import com.lebenlab.core.tbmaster.PG1;

import static com.lebenlab.ProcessArgException.error_simulation_producto;

/**
 * User: pedro@didekin
 * Date: 01/04/2020
 * Time: 11:51
 */
public class Pg1Promocion implements Jsonable {

    public final Promocion promo;
    public final int pg1;

    Pg1Promocion(Pg1PromoBuilder builder)
    {
        promo = builder.promo;
        pg1 = builder.pg1;
    }

    // ====================== Builder ===================

    public static class Pg1PromoBuilder implements BeanBuilder<Pg1Promocion> {

        private Promocion promo;
        private int pg1;


        public Pg1PromoBuilder()
        {
        }

        public Pg1PromoBuilder promo(Promocion promoIn)
        {
            promo = new Promocion.PromoBuilder().copyPromo(promoIn).build();
            return this;
        }

        public Pg1PromoBuilder pg1(int pg1In)
        {
            pg1 = PG1.fromInt(pg1In);
            return this;
        }

        public Pg1PromoBuilder copy(Pg1Promocion pg1Promocion)
        {
            pg1(pg1Promocion.pg1);
            promo(pg1Promocion.promo);
            return this;
        }

        @Override
        public Pg1Promocion build()
        {
            Pg1Promocion instance = new Pg1Promocion(this);
            if (promo == null || pg1 <= 0 || !promo.pg1s.contains(pg1)) {
                throw new ProcessArgException(error_simulation_producto);
            }
            return instance;
        }
    }
}
