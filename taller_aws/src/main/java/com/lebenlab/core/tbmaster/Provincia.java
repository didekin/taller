package com.lebenlab.core.tbmaster;

import com.lebenlab.Jsonable;

import org.jdbi.v3.core.mapper.RowMapper;

/**
 * User: pedro@didekin
 * Date: 06/04/2020
 * Time: 17:47
 */
public class Provincia implements Jsonable {

    public static final RowMapper<Provincia> mapper = (rs, ctx) ->
            new Provincia(rs.getInt("provincia_id"), rs.getInt("mercado_id"), rs.getString("nombre"));

    public final int provinciaId;
    public final int mercadoId;
    public final String nombre;

    public Provincia(int provincia_id, int mercado_id, String nombre)
    {
        provinciaId = provincia_id;
        mercadoId = mercado_id;
        this.nombre = nombre;
    }
}
