package com.lebenlab;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.regex.Pattern;

import static java.lang.Character.digit;
import static java.lang.Character.toUpperCase;
import static java.util.Locale.FRENCH;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.UNICODE_CASE;
import static java.util.regex.Pattern.compile;
import static java.util.stream.IntStream.range;

/**
 * User: pedro@didekin.es
 * Date: 10/10/19
 * Time: 10:13
 */

@SuppressWarnings("unused")
public enum DataPatterns {

    // ============== EXPERIMENTO ==============
    EXPERIMENTO("[0-9a-zA-Z_ñÑ]{5,25}"),
    EXPERIMENTO_ID("[0-9]+"){
        @Override
        public boolean isPatternOk(String experimentoId)
        {
            return super.isPatternOk(experimentoId) && Long.parseLong(experimentoId) > 0;
        }
    },

    // ============== PROMOCIÓN ==============
    COD_PROMO("[0-9a-zA-Z_ñÑ]{5,25}"),
    NOMBRE_PROMO("[0-9a-zA-ZñÑáéíóúüÜ,[\\s]]{4,100}"),

    // ============== TOKEN ==============
    /**
     * Pattern: <header>.<encrypted key>.<initialization vector>.<ciphertext>.<authentication tag>, where encrypted key
     * is the empty String when the key management algorithm is DIRECT.
     */
    jwt_with_direct_key(".+\\.{2}.+\\..+"),

    // ============== USUARIO ==============

    EMAIL("[\\w\\._\\-@+]{1,64}@[\\w\\-_]{1,255}\\.[\\w&&[^0-9]]{1,10}"),
    TFNO_MOVIL("^34(?:6[0-9]|7[1-9])[0-9]{7}$"),
    NIF("[0-9]{7,8}[a-zA-Z]") {
        @Override
        public boolean isPatternOk(String nifString)
        {
            if (!super.isPatternOk(nifString)) {
                return false;
            }
            return isNifOk(nifString);
        }
    },
    /**
     * Los NIE's de extranjeros residentes en España tienen una letra (X, Y, Z), 7 números y dígito de control.
     */
    NIE("[X-Zx-z][0-9]{7}[a-zA-Z]") {
        @Override
        public boolean isPatternOk(String nieString)
        {
            if (!super.isPatternOk(nieString)) {
                return false;
            }
            return isNifOk(
                    nieString.replace(
                            nieString.charAt(0),
                            Character.forDigit(nie_first_letter_conversion.indexOf(toUpperCase(nieString.charAt(0))), 10))
            );
        }
    },
    /**
     * H. Comunidades de propietarios en régimen de propiedad horizontal.
     */
    CIF_COMUNIDAD("H[0-9]{7}[0-9]") {
        @Override
        public boolean isPatternOk(String cifComunidad)
        {
            if (!super.isPatternOk(cifComunidad)) {
                return false;
            }
            return digit(cifComunidad.charAt(cifComunidad.length() - 1), 10) == getCifDigitCtrl(cifComunidad.substring(1, cifComunidad.length() - 1));
        }
    },
    /**
     * A. Sociedades anónimas.
     * B. Sociedades de responsabilidad limitada.
     * C. Sociedades colectivas.
     * D. Sociedades comanditarias.
     * E. Comunidades de bienes.
     * F. Sociedades cooperativas.
     * G. Asociaciones y fundaciones.
     * H. Comunidades de propietarios en régimen de propiedad horizontal.
     * J. Sociedades civiles.
     * N. Entidades no residentes.
     * P. Corporaciones locales.
     * Q. Organismos autónomos, estatales o no, y asimilados, y congregaciones e instituciones religiosas.
     * R. Congregaciones e instituciones religiosas (desde 2008, ORDEN EHA/451/2008)
     * S. Órganos de la Administración del Estado y comunidades autónomas
     * U. Uniones Temporales de Empresas.
     * V. Sociedad Agraria de Transformación.
     * W. Establecimientos permanentes de entidades no residentes en España
     * <p>
     * El dígito de control será:
     * - Una LETRA si la clave de entidad es P, Q, R, S, W.
     * - Un NÚMERO si la entidad es A, B, E o H.
     * - Para otras claves de entidad: el dígito podrá ser tanto número como letra.
     * Para la conversión a letra:
     * J = 0, A = 1, B = 2, C= 3, D = 4, E = 5, F = 6, G = 7, H = 8, I = 9
     */
    CIF("[[A-H][J-N][P-S]UVW][0-9]{7}[0-9A-J]") {
        @Override
        public boolean isPatternOk(String cifString)
        {
            if (!super.isPatternOk(cifString)) {
                return false;
            }
            int digitControl = getCifDigitCtrl(cifString.substring(1, cifString.length() - 1));

            final boolean b1 = digit(cifString.charAt(cifString.length() - 1), 10) == digitControl;
            if (cif_cod_entidad_solo_numero.indexOf(toUpperCase(cifString.charAt(0))) >= 0) {
                return b1;
            }
            final boolean b = cif_conversion_index_to_letra.charAt(digitControl) == toUpperCase(cifString.charAt(cifString.length() - 1));
            if (cif_cod_entidad_solo_letra.indexOf(toUpperCase(cifString.charAt(0))) >= 0) {
                return b;
            }
            return b1 || b;
        }
    },
    ;

    private static final String cif_cod_entidad_solo_numero = "ABEH";
    private static final String cif_cod_entidad_solo_letra = "PQRSW";
    private static final String cif_conversion_index_to_letra = "JABCDEFGHI";
    private static final String nie_first_letter_conversion = "XYZ";
    private static final String nif_conversion_lastChar = "TRWAGMYFPDXBNJZSQVHLCKE";

    static boolean isNifOk(String nifString)
    {
        int numberNif = Integer.valueOf(nifString.substring(0, nifString.length() - 1), 10);
        char letterNif = toUpperCase(nifString.charAt(nifString.length() - 1));
        return nif_conversion_lastChar.charAt(numberNif % 23) == letterNif;
    }

    static int getCifDigitCtrl(String central7digitsStr)
    {
        int digitControl = range(0, central7digitsStr.length())
                .map(indexIn -> {
                    if (indexIn % 2 == 1) {
                        return digit(central7digitsStr.charAt(indexIn), 10);
                    } else {
                        int result = digit(central7digitsStr.charAt(indexIn), 10) * 2;
                        return (result % 10) + (result / 10);
                    }
                })
                .sum() % 10;

        return digitControl > 0 ? (10 - digitControl) : 0;
    }

    public static final String error_userName = "Wrong initialization value in userName";
    public static final String error_appId = "Wrong initialization value in appID";

    private final Pattern pattern;
    private final String regexp;

    DataPatterns(String patternString)
    {
        pattern = compile(patternString, UNICODE_CASE | CASE_INSENSITIVE);
        regexp = patternString;
    }

    public boolean isPatternOk(String fieldToCheck)
    {
        return pattern.matcher(fieldToCheck).matches();
    }

    public String getRegexp()
    {
        return regexp;
    }

    // ======================  Static utilities ========================

    public static String getDecimalNumberStr(double doubleNum, int numDecimals){
        NumberFormat decFormat = DecimalFormat.getInstance(FRENCH);
        decFormat.setMaximumFractionDigits(numDecimals);
        return decFormat.format(doubleNum);
    }
}
