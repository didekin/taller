package com.lebenlab;

import io.javalin.http.ExceptionHandler;

/**
 * User: pedro@didekin.es
 * Date: 22/01/2020
 * Time: 14:55
 */
public class ProcessArgException extends RuntimeException {

    public static final String error_build_promocion = "Definición errónea de los parámetros de una promoción: ";
    public static final String error_build_variante = "Definición errónea de los parámetros de la variante de una promoción: ";
    public static final String error_build_promomedcom = "Definición errónea de los parámetros de un medio de comunicación: ";
    public static final String error_build_textcomunicacion = "Texto de la comunicación con más de 500 caracteres o vacío.";
    public static final String error_build_text_clasificacion = "Promoción sin comunicación con texto asociado";
    public static final String error_conceptos = "Selección errónea de conceptos: ";
    public static final String error_dataframe_modelo_wrong = "Error en los datos para construcción del modelo";
    public static final String error_incentivo = "Selección errónea de incentivo: ";
    public static final String error_num_muestras_promociones = "Número de muestras de participantes no coincide con número de promociones";
    public static final String error_in_dictionnary = "No hay en dicccionario una probabilidad para cada estado word-textClass";
    public static final String error_jdbi_statement = "Error ejecutando operación en base de datos: ";
    public static final String error_medio = "Selección errónea de medio de comunicación: ";
    public static final String error_mercados = "Selección errónea de mercados: ";
    public static final String error_in_participante_samples = "Muestras sin participantes";
    public static final String error_nonexistent_dir = "No existe el directorio: ";
    public static final String error_pg1s = "Selección errónea de productos ";
    public static final String error_promos_in_exp = "El experiemento no contiene dos promociones";
    public static final String error_promo_medio_sms = "Promoción con medio de comunicación distinto a SMS";
    public static final String error_quarter = "Trimestre de la promoción erróneo: ";
    public static final String error_sinch_parameter_map = "Mapa de parámetros con menos entradas que receptores de SMS";
    public static final String error_sinch_retrieving_sms_report = "Error en la solicitud de un informe de envíos SMS";
    public static final String error_sinch_sending_sms = "Error enviando SMS con Sinch";
    public static final String error_simulation_producto = "Datos erróneos en la simulación de resultados de un producto";
    public static final String error_simulation_plot_ordinal_wrong = "El tipo de gráfico ha de ser 1 ó 2";
    public static final String error_reading_serialized_model = "Error escribiendo en fichero modelo serializado: ";
    public static final String error_writing_serialized_model = "Error leyendo modelo serializado en fichero: ";
    public static final String experimento_overlapping = "Hay otro experimento solapado en el tiempo con: ";
    public static final String experimento_wrongly_initialized = "Experimento mal inicializado ";
    public static final String file_error_reading_msg = "Error leyendo el fichero";
    public static final String num_promos_by_pg1_wrong = "Número promos con el mismo producto != 2";
    public static final String result_experiment_wrongly_initialized = "Resultado de experimento mal incializado o sin resultados : ";
    public static final String result_experiment_more_3_pg1s = "Hay más de 3 PG1s con resultados";
    public static final String result_experiment_no_pg1s = "No hay productos con resultados";
    public static final String result_experiment_wrong_participantes = "Número de participantes difiere entre productos en promoción";
    public static final String tTest_error_generic = "Error en la ejecución de t-test: ";
    public static final String upload_file_error_msg = "Fichero vacío o con formato erróneo";

    // Mensajes sin lanzar excepción.
    public static final String no_data_for_prediction = "No hay historial suficiente para hacer estimaciones ";

    private final String message;

    public ProcessArgException(String messageIn)
    {
        message = messageIn;
    }

    @Override
    public String getMessage()
    {
        return message;
    }

    public static final ExceptionHandler<ProcessArgException> handleProcessException = (e, ctx) -> {
        ctx.status(500);
        ctx.result(e.message);
    };
}
