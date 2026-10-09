package com.proyecto.servicios.validation;

/**
 * Expresiones regulares y mensajes de validacion del onboarding. Son las mismas reglas que los
 * CHECK de la tabla onboarding.clientes, para que la base de datos nunca reciba un dato invalido.
 */
public final class Patrones {

    /**
     * Letras (incluye acentos y ene) separadas por un solo espacio, sin espacios al inicio o al final.
     * El lookahead (?=.{1,50}$) descarta de inmediato textos de mas de 50 caracteres: sin el, el grupo
     * repetido ( [letras]+)* hace que el motor de regex de Java desborde la pila con textos enormes.
     */
    public static final String NOMBRE = "^(?=.{1,50}$)[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+( [A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+)*$";
    public static final String MENSAJE_NOMBRE = "solo admite letras y espacios";
    public static final String MENSAJE_LONGITUD_NOMBRE = "debe tener entre 2 y 50 caracteres";
    /** Para campos opcionales en actualizaciones: vacio ("") elimina el valor. */
    public static final String NOMBRE_O_VACIO = "^$|^(?=.{2,50}$)[A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+( [A-Za-zÁÉÍÓÚÜÑáéíóúüñ]+)*$";
    public static final String MENSAJE_NOMBRE_O_VACIO = "debe tener entre 2 y 50 letras, o enviarse vacio para eliminarlo";

    public static final String CURP = "^[A-Z][AEIOUX][A-Z]{2}[0-9]{2}(0[1-9]|1[0-2])(0[1-9]|[12][0-9]|3[01])[HM]"
            + "(AS|BC|BS|CC|CL|CM|CS|CH|DF|DG|GT|GR|HG|JC|MC|MN|MS|NT|NL|OC|PL|QT|QR|SP|SL|SR|TC|TS|TL|VZ|YN|ZS|NE)"
            + "[B-DF-HJ-NP-TV-Z]{3}[0-9A-Z][0-9]$";
    public static final String MENSAJE_CURP = "La CURP no tiene un formato valido";
    public static final String MENSAJE_LONGITUD_CURP = "La CURP debe contener 18 caracteres";

    /** RFC de persona fisica: 4 letras, fecha AAMMDD y homoclave de 3 caracteres. */
    public static final String RFC = "^[A-ZÑ&]{4}[0-9]{2}(0[1-9]|1[0-2])(0[1-9]|[12][0-9]|3[01])[A-Z0-9]{2}[0-9A]$";
    public static final String MENSAJE_RFC = "El RFC no tiene un formato valido";
    public static final String MENSAJE_LONGITUD_RFC = "El RFC debe contener 13 caracteres";

    public static final String CORREO = "^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,}$";
    public static final String MENSAJE_CORREO = "El correo electronico no tiene un formato valido";
    public static final String MENSAJE_LONGITUD_CORREO = "El correo electronico admite maximo 100 caracteres";

    public static final String TELEFONO = "^[0-9]{10}$";
    public static final String MENSAJE_TELEFONO = "debe contener exactamente 10 digitos";
    public static final String TELEFONO_O_VACIO = "^$|^[0-9]{10}$";

    /**
     * Texto libre (ocupacion, empresa, calle, numeros, colonia): letras, numeros, espacios y
     * puntuacion comun. Excluye < > " ` { } ; \ y similares, para que no se puedan guardar etiquetas
     * HTML o scripts que un front podria ejecutar al mostrar el dato (XSS almacenado).
     */
    public static final String TEXTO_SEGURO = "^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ0-9 .,#/&()'°ºª-]*$";
    public static final String MENSAJE_TEXTO_SEGURO =
            "contiene caracteres no permitidos (solo letras, numeros, espacios y . , # / & ( ) ' ° -)";

    /** Texto con al menos un caracter que no sea espacio. */
    public static final String NO_VACIO = "^(?s).*\\S.*$";

    public static final String CODIGO_POSTAL = "^[0-9]{5}$";
    public static final String MENSAJE_CODIGO_POSTAL = "El codigo postal debe contener exactamente 5 digitos";

    /** Codigo ISO 3166-1 alfa-3, por ejemplo MEX. */
    public static final String NACIONALIDAD = "^[A-Z]{3}$";
    public static final String MENSAJE_NACIONALIDAD = "La nacionalidad debe ser un codigo ISO de 3 letras, por ejemplo MEX";

    private Patrones() {
    }
}
