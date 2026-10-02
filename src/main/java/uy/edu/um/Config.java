package uy.edu.um;

public class Config {
    public static final int T = 20000; // Milisegundos de simulacion
    public static final int M = 3; // Mesas
    public static final int P = 2; // Personas
    public static final int Z = 2; // Mozos
    public static final int C = 2; // Cocineros
    public static final int Y = 1; // Cajero
    public static final int TPmin = 300, TPmax = 800; // Tiempo mínimo y máximo que puede llegar un cliente alrestaurante.
    public static final int TMmin = 500, TMmax = 1500; // Tiempo mínimo y máximo que demoran los clientes en seleccionar el menú
    public static final int TQmin = 1000, TQmax = 2000; // Tiempo mínimo y máximo que demoran los clientes en comer.
    public static final int TZmin = 300, TZmax = 800; // Tiempo mínimo y máximo que demoran los mozos en anotar el pedido.
    public static final int TRmin = 300, TRmax = 800; // Tiempo mínimo y máximo que demoran los mozos en retirar los platos de la cocina y servirlos en la mesa de los clientes.
    public static final int TLmin = 200, TLmax = 500; // Tiempo mínimo y máximo que demoran los mozos en levantar los platos y limpiar la mesa.
    public static final int TCmin = 200, TCmax = 500; // tiempo mínimo y máximo que demora un menú en cocinarse.
    public static final int TYmin = 300, TYmax = 700; // Tiempo mínimo y máximo que demoran los cajeros en cobrar a los clientes.
}
