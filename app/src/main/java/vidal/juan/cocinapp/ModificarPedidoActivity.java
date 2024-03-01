package vidal.juan.cocinapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModificarPedidoActivity extends AppCompatActivity {

    private Button volverDetallesPedidosButton,confirmModPedidoButton,seleccionarFechaEntregaButton;
    private ListView listaDetalleMod = null;
    private TextView fechaPedidoDetalleTextMod,fechaEntregaModTextview,cometariosDetalleTextMod,totalDetalleTextMod,idPedidoModTextView;
    private String idPedido;
    private double precioTotalPedido = 0;

    private String nuevaFechaEntrega, fechaPedido;
    // Formatear la fecha al formato deseado: aaaa-MM-dd
    SimpleDateFormat formato = new SimpleDateFormat("yyyy-MM-dd");
    //Formatear para guardar hora minutos y segundos
    SimpleDateFormat formatoHoraMinSeg = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    static final int apartirDiasRecoger = 4;//Dias a partir de los cuales se puede recoger Dias definidos por esther ? TODO sacar este dato de BBDD?



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modificar_pedido);
        //referencia a items xml
        volverDetallesPedidosButton = findViewById(R.id.volverDetallesPedidosButton);
        confirmModPedidoButton = findViewById(R.id.confirmModPedidoButton);
        seleccionarFechaEntregaButton = findViewById(R.id.seleccionarFechaEntregaButton);
        listaDetalleMod = findViewById(R.id.listaDetalleMod);
        fechaPedidoDetalleTextMod = findViewById(R.id.fechaPedidoDetalleTextMod);
        fechaEntregaModTextview = findViewById(R.id.fechaEntregaModTextview);
        cometariosDetalleTextMod = findViewById(R.id.cometariosDetalleTextMod);
        totalDetalleTextMod = findViewById(R.id.totalDetalleTextMod);
        idPedidoModTextView  = findViewById(R.id.idPedidoModTextView);

        //Funcionalidad botones
        //Volver a la pantalla principal
        volverDetallesPedidosButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                volverDetallePedido();
            }
        });

        //Seleccionar nueva fecha pedido
        seleccionarFechaEntregaButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                obtenerFechaDatepicker();
            }
        });

        // Recuperar id pedido del pedido seleccionado en la anterior actividad
        idPedido = getIntent().getStringExtra("idPedido");
        //Log
        Log.d("idPedidoRecib", "Pedido: " + idPedido);
        // Buscar y mostrar los detalles del pedido a partir de id
        buscarPedido(idPedido);

    }

    /**
     * Buscar el pedido por el id de la ativity anterior
     * @param idPedido id del pedido obtenido en la activity anterior
     */
    private void buscarPedido(String idPedido) {
        // Referencia a la base de datos mediante el id pedido
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("pedidos").child(idPedido);
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshotPedido) {
                //Obj Pedido encontrado a partir del Idpedido
                Pedido pedido = dataSnapshotPedido.getValue(Pedido.class);
                if(pedido != null){
                    Log.d("ExecPedidoObj", "pedido orig."+ pedido.toString());
                    //Valores del pedido a la vista
                    fechaPedidoDetalleTextMod.setText(pedido.getFecha_pedido().toString());
                    fechaEntregaModTextview.setText( pedido.getFecha_entrega().toString());
                    cometariosDetalleTextMod.setText(pedido.getComentarios().toString());
                    totalDetalleTextMod.setText(String.valueOf(pedido.getPrecio_total()) + "\u20AC" );
                    idPedidoModTextView.setText(getString(R.string.idPedidoString) + idPedido.substring(3,7));
                    //Agregar al objeto pedido el stock,cantidadMax, y precio de la racion en cada detalle del pedido
                    //Buscar los datos de la racion a partir del nombre de la racion
                    final int[] racionesRecuperadas = {0};
                    for (DetallePedidoNoParcel detalle : pedido.getDetalles()) {
                        //Encontrar la racion en la BBDD para cada detalle
                        DatabaseReference racionRef = FirebaseDatabase.getInstance().getReference().child("raciones").child(detalle.getRacion());
                        ValueEventListener valueEventListenerRacion;
                        valueEventListenerRacion= new ValueEventListener() {
                            @Override
                            public void onDataChange( DataSnapshot dataSnapshotRacion) {
                                //Crear objeto racion a partir de datos de BBDD
                                Racion racion = dataSnapshotRacion.getValue(Racion.class);
                                //Pasar el stock,precioRacion,cantidadMaximaRacion al detalle del pedido
                                detalle.setStockRacion(racion.getStock());
                                detalle.setPrecioRacion(racion.getPrecio());
                                detalle.setPedidoMaxRacion(racion.getPedido_max());
                                detalle.setStockOriginal(racion.getStock());
                                Log.d("AddRacionADetalle", "ADD cant,stock y preico orig."+ detalle.toString());
                                racionesRecuperadas[0]++;
                                // Verificar si se han recuperado todas las raciones
                                if (racionesRecuperadas[0] == pedido.getDetalles().size()) {
                                    llenarLista(pedido);
                                    //Boton Modificar el pedido

                                }

                            }
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                            //Litener de confirmar
                        };
                        racionRef.addValueEventListener(valueEventListenerRacion);

                        Log.d("Conex", "Conex"+ racionRef.toString());
                    }
                    confirmModPedidoButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            confirmModificarPedido(pedido,dataSnapshotPedido);
                            Log.d("PedidoMod", "El pedido modficado"+ pedido.toString());
                            Log.d("PedidoMod", "El DDetalles del pedido"+ pedido.getDetalles().toString());

                        }
                    });
                }
            }//Fin on datachange Pediddos
            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Manejar errores de la base de datos
                Log.e("Error BBDD", "Error al buscar el pedido: " + databaseError.getMessage());
            }
        });

    }

    /**
     * Llenar la lista con los detalles del pedido
     * @param pedido objeto pedido del que se obtienen los detalles
     */
    private void llenarLista(Pedido pedido ){
        Log.d("ExecLlenarLista", "Llenar lista ejecutado: " );
        listaDetalleMod.setAdapter(new AdaptadorDetallesNoparcel(this, R.layout.detealle_pedido_mod_vista, pedido.getDetalles()) {
            @Override
            public void onEntrada(DetallePedidoNoParcel detallePedido, View view) {
                if (detallePedido != null) {
                    //Componentes de la vista que muestra el detalle del pediddo
                    Log.d("ExecDetalleAddLista", "DetalleAñadido " + detallePedido.toString() );
                    TextView nombreRacionDetalle = view.findViewById(R.id.nombreRacionDetalle);
                    TextView cantidadRacionDetalleVistaDetalle = view.findViewById(R.id.cantidadRacionDetalleVistaDetalle);
                    TextView precioRacionDetalleVistaDetalle = view.findViewById(R.id.precioRacionDetalleVistaDetalle);
                    Button modDetalleBotonQuitar,modDetalleBotonAnadir;
                    modDetalleBotonQuitar = view.findViewById(R.id.modDetalleBotonQuitar);
                    modDetalleBotonAnadir = view.findViewById(R.id.modDetalleBotonAnadir);
                    //Logs
                    /*Log.d("RacionNombre", "Datos racion buscada : " + detallePedido.getRacion());
                    Log.d("detallesAlaLista", "Datos introducidos a la lista : " + pedido.getDetalles().toString());
                    Log.d("detallesAlaLista", "Tamaño de la lista : " + pedido.getDetalles().size());*/
                    //Valores inciales de la vista de la lista
                    nombreRacionDetalle.setText(detallePedido.getRacion());
                    cantidadRacionDetalleVistaDetalle.setText(String.valueOf(detallePedido.getCantidad()));
                    precioRacionDetalleVistaDetalle.setText(String.valueOf (detallePedido.getPrecio()) + "\u20AC");
                    //Listeners para los botones
                    //Contador
                    final int[] modCantidad = {0};
                    final int[] stockOrig ={0};
                    //Stock original del detalle
                    stockOrig[0] = Integer.parseInt(detallePedido.getStockOriginal());
                    //Boton añadir
                    modDetalleBotonAnadir.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Log.d("ExecRacionEnStock", "Stock al hacer + " + detallePedido.getRacion() + ": " + detallePedido.getStockRacion());
                            //El precio de una racion para añadir o quitar al total
                            double precioUnaracion = Double.parseDouble(detallePedido.getPrecioRacion());
                            int cantidadActual = detallePedido.getCantidad();
                            int cantidadMaxima = detallePedido.getPedidoMaxRacion();
                            //int stock = Integer.parseInt(racion.getStock());

                            Log.d("ExecRaStockAntes+", "Stock antes de hacer click en + " + detallePedido.getRacion() + ": " + detallePedido.getStockRacion());
                            Log.d("ValorModCantidadA", "Valor de mod cantidad Antes de if: " + modCantidad[0] );
                            if (modCantidad[0] < Math.min(cantidadMaxima, stockOrig[0])) {
                                //Aumentar las veces que se dio a +
                                modCantidad[0] += 1;
                                //Modificar el stock
                                int stock = Integer.parseInt(detallePedido.getStockRacion());
                                //Disminuir 1 de sotck
                                detallePedido.setStockRacion(String.valueOf(stock- 1));
                                //Aumentar la cantidad en 1 del pedido
                                detallePedido.setCantidad(cantidadActual + 1);
                                Log.d("ValorModCantidadB", "Valor de mod cantidad depues de if: " + modCantidad[0] );
                                Log.d("ExecRaStockdesp+", "Stock despues de hacer click en + " + detallePedido.getRacion() + ": " + detallePedido.getStockRacion());
                                //Mostrar la actu en la view
                                cantidadRacionDetalleVistaDetalle.setText(String.valueOf(detallePedido.getCantidad()));
                                double nuevoPrecio = Double.parseDouble(detallePedido.getPrecioRacion()) * (detallePedido.getCantidad());
                                //Actualizar el precio del pedido
                                detallePedido.setPrecio(nuevoPrecio);
                                //precioRacionActu[0] = precioUnaracionMas;
                                //Mostrar actu del precio en la view
                                precioRacionDetalleVistaDetalle.setText(String.valueOf(detallePedido.getPrecio()) + "\u20AC");
                                //Precio total aterior
                                double precioAnt;
                                //String del precio total actual en la vista general, hayq que quitar el simbolo $ con un substring
                                String totalStringAnt = totalDetalleTextMod.getText().toString();
                                precioAnt = Double.parseDouble(totalStringAnt.substring(0, totalStringAnt.length() - 1));
                                totalDetalleTextMod.setText(String.format("%.2f", precioAnt + precioUnaracion) + "\u20AC");
                                //todo quitar Log.e("stockdetalles", "Pulsar boton detalles mod."+ pedido.toString());

                            } else {
                                if (modCantidad[0] == cantidadMaxima) {
                                    Toast.makeText(ModificarPedidoActivity.this, "Se ha alcanzado el máximo de productos de este tipo por pedido", Toast.LENGTH_SHORT).show();

                                } else {
                                    Toast.makeText(ModificarPedidoActivity.this, "Se ha alcanzado el límite de productos disponibles en stock", Toast.LENGTH_SHORT).show();
                                }
                            }
                        }

                    });
                    // Boton quitar
                    modDetalleBotonQuitar.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            //El precio de una racion para añadir o quitar al total
                            double precioUnaracion = Double.parseDouble(detallePedido.getPrecioRacion());
                            if (detallePedido.getCantidad() > 0) {
                                //Disminuir las veces que se dio a boton
                                modCantidad[0] -= 1;
                                int stock = Integer.parseInt(detallePedido.getStockRacion());
                                detallePedido.setStockRacion(String.valueOf(stock + 1));
                                Log.d("StockActual-", "Stock del producto : " + detallePedido.getRacion() + " " + (detallePedido.getStockRacion()));
                                //Disminuir cantidad mostrada
                                detallePedido.setCantidad(detallePedido.getCantidad() - 1);
                                if (detallePedido.getCantidad() == 0) {
                                    Toast.makeText(ModificarPedidoActivity.this, detallePedido.getRacion() + " se eliminará del pedido", Toast.LENGTH_SHORT).show();
                                }
                                //Mostrar la actu en la view
                                cantidadRacionDetalleVistaDetalle.setText(String.valueOf(detallePedido.getCantidad()));
                                //Actualizar el precio del pedido
                                detallePedido.setPrecio(Double.parseDouble(detallePedido.getPrecioRacion()) * (detallePedido.getCantidad()));
                                precioRacionDetalleVistaDetalle.setText(String.valueOf(detallePedido.getPrecio()) + "\u20AC");//Mostrar actu del precio en la view
                                //Precio total anterior original
                                double precioAnt;
                                //String del precio total actual en la vista geneeral, hayq que quitar el simbolo $ con un substring
                                String totalStringAnt = totalDetalleTextMod.getText().toString();
                                precioAnt = Double.parseDouble(totalStringAnt.substring(0, totalStringAnt.length() - 1));

                                totalDetalleTextMod.setText(String.format("%.2f", precioAnt - precioUnaracion) + "\u20AC");


                            }
                        }
                    });
                }
            }
        });//Fin llenar lista
        Log.d("ExecTamLista", "tamaño de la lista: " + listaDetalleMod.getAdapter().getCount());
        ((BaseAdapter) listaDetalleMod.getAdapter()).notifyDataSetChanged();
    }

    private void updateStock(Racion racion, DataSnapshot dataSnapshotRacion) {
        dataSnapshotRacion.getRef().setValue(racion).addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        // DATO MODIFICADO EN BBDD
                        Toast.makeText(ModificarPedidoActivity.this,"Stock modificado "  , Toast.LENGTH_LONG).show();
                        Log.d("StockAtualizado", "Stock actulizado:  "+ racion.toString());
                        //volver a l
                        volverDetallePedido();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(ModificarPedidoActivity.this,"ERROR no se registraron los cambios "  , Toast.LENGTH_LONG).show();
                    }
                });
    }


    /**
     * Volver Detalles del pedido
     */
    private  void volverDetallePedido() {
        Intent intent = new Intent(ModificarPedidoActivity.this, VerDetallesPedidoActivity.class);
        intent.putExtra("idPedido", idPedido);//Pasar el id del detalle del peddido para volver a donde estaba
        startActivity(intent);

        finish();
    }

    /**
     * Metodo para obener la fecha seleccionada en el datepicker; Tiene restricion mediante la CONSTANTE a paritr dias recoge
     * Se muestran los dias siempre a partir de esta constante.
     */
    private void obtenerFechaDatepicker() {

        //Obtener referencias al dia de hoy
        final Calendar calendar =  Calendar.getInstance();
        final int ano = calendar.get(Calendar.YEAR);
        final int mes = calendar.get(Calendar.MONTH);
        final int dia = calendar.get(Calendar.DAY_OF_MONTH);
        //Formatear la fecha a dia de hoy para pasarla al insert
        fechaPedido = formatoHoraMinSeg.format(calendar.getTime());
        DatePickerDialog datePickerDialog = new DatePickerDialog(this , new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker datePicker, int year, int month, int day) {
                //Setear calendar con la fecha seleccionada
                calendar.set(Calendar.YEAR, year);
                calendar.set(Calendar.MONTH, month);
                calendar.set(Calendar.DAY_OF_MONTH, day);
                // Validar si la fecha seleccionada es igual al día de hoy
                Calendar fechaSeleccionada = Calendar.getInstance();
                fechaSeleccionada.set(year, month, day);
                //Que la fecha seleccionada no sea el dia de hoy
                if (fechaSeleccionada.get(Calendar.YEAR) == ano &&
                        fechaSeleccionada.get(Calendar.MONTH) == mes &&
                        fechaSeleccionada.get(Calendar.DAY_OF_MONTH) == dia) {
                    Toast.makeText(ModificarPedidoActivity.this, "Seleccione una fecha diferente al día de hoy", Toast.LENGTH_SHORT).show();
                }else {
                    String fechaFormateada = formato.format(calendar.getTime());
                    //Que hacer cuando se seleccione la fecha
                    Toast.makeText(ModificarPedidoActivity.this, "Nueva fecha de entrega: " + fechaFormateada, Toast.LENGTH_LONG).show();
                    nuevaFechaEntrega = fechaFormateada;
                    Log.d("FechaSel", "FechaSel: " + nuevaFechaEntrega);
                    Log.d("FechaPedido", "FechaPedido: " + fechaPedido);
                    fechaEntregaModTextview.setText(nuevaFechaEntrega);
                }
            }
        }, ano, mes, dia);
        // Restricción para que no se pueda seleccionar el día de hoy
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        //Restriccion de fines de semana
        datePickerDialog.getDatePicker().init(ano, mes, dia, new DatePicker.OnDateChangedListener() {
            @Override
            public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                // Verificar si la fecha seleccionada es fin de semana
                calendar.set(year, monthOfYear, dayOfMonth);
                int finde = calendar.get(Calendar.DAY_OF_WEEK);

                // Verificar si la fecha seleccionada es sabado o domingo
                if (finde == Calendar.SATURDAY || finde == Calendar.SUNDAY) {
                    // Si es un fin de semana, error
                    Toast.makeText(getApplicationContext(), "No se pueden seleccionar fines de semana", Toast.LENGTH_SHORT).show();

                    // Restablecer la fecha seleccionada al dia a partir del cual se puede hacer el pedido
                    datePickerDialog.getDatePicker().updateDate(ano, mes, dia);
                }
            }
        });

        //Restriccion de fecha
        Calendar calendarioMin = Calendar.getInstance();
        //Apartir de los dias que diga ESTHER se puede recoger el pedido todo esto se hace con una constante local; habria que hacerlo de BBDD para que sea mas sencillo de configurar
        calendarioMin.add(Calendar.DAY_OF_MONTH, + apartirDiasRecoger);
        //Setear el dia minimo
        datePickerDialog.getDatePicker().setMinDate(calendarioMin.getTimeInMillis() - 1000);
        //TODO hace falta setear dia máximo?
        datePickerDialog.show();


    }
    private void confirmModificarPedido( Pedido pedido,DataSnapshot dataSnapshotPedido) {
        //Actulizar el stock con los nuevos datos

        //Contador para actualizar pedido una vez se hallan modificado todas las raciones del pedido
        final int[] contadorActuRaciones = {0};
        //Recorrer los detalles
        for (DetallePedidoNoParcel detalleActu: pedido.getDetalles())
        {
            //Obtener datos de la racion por nombre de la racion
            DatabaseReference datarefActuRacion;
            datarefActuRacion = FirebaseDatabase.getInstance().getReference().child("raciones").child(detalleActu.getRacion());
            datarefActuRacion.child("stock").setValue(detalleActu.getStockRacion()).addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void unused) {
                    Log.d("ActuRacion", "Racion " + detalleActu.getRacion() + "actualizada");
                    contadorActuRaciones[0]++;//Aumentar el contador cuando se actulice la racoin en la BBDD
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e("ActuRacion", "Error al escribir en la base de datos: " + e.getMessage());
                    Toast.makeText(ModificarPedidoActivity.this, "Error, peiddo no modificado ", Toast.LENGTH_LONG).show();
                    volverDetallePedido();
                }
            });
        }
        if (contadorActuRaciones[0] == pedido.getDetalles().size()) {
            //Actualizar el pedido
            //Datos modificados en la vista, pasarlos al pedido
            String totalString = totalDetalleTextMod.getText().toString();
            pedido.setPrecio_total(Double.parseDouble(totalString.substring(0, totalString.length() - 1)));
            pedido.setComentarios(cometariosDetalleTextMod.getText().toString());
            pedido.setFecha_entrega(fechaEntregaModTextview.getText().toString());
            //List para detalles nuevos sin datos del stock
            List<DetallePedidoNoParcel> detallesSinDatosStock = new ArrayList<>();
            for (DetallePedidoNoParcel detalleConDatosStock : pedido.getDetalles()) {
                detallesSinDatosStock.add(new DetallePedidoNoParcel(detalleConDatosStock.getRacion(), detalleConDatosStock.getCantidad(), detalleConDatosStock.getPrecio()));
            }
            //Pasar la lista al pedido
            pedido.setDetalles(detallesSinDatosStock);
            //Crear un Map con lo que se tiene que actulizar del pedido
            Map<String, Object> actuPedido = new HashMap<>();
            actuPedido.put("comentarios", pedido.getComentarios());
            actuPedido.put("precio_total", pedido.getPrecio_total());
            actuPedido.put("fecha_entrega", pedido.getFecha_entrega());
            actuPedido.put("detalles", pedido.getDetalles());
            // Actualizar los campos en la base de datos
            dataSnapshotPedido.getRef().updateChildren(actuPedido).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {

                    if (task.isSuccessful()) {
                        // La actualización se realizó correctamente
                        Log.d("ACtuPedido", "Actualización exitosa");
                        Toast.makeText(ModificarPedidoActivity.this, "Pedido modificado ", Toast.LENGTH_LONG).show();
                        volverDetallePedido();
                    } else {
                        // La actualización falló
                        Exception e = task.getException();
                        if (e != null) {
                            Toast.makeText(ModificarPedidoActivity.this, "Error, peiddo no modificado ", Toast.LENGTH_LONG).show();
                            Log.e("ACtuPedido", "Error al actualizar: " + e.getMessage());
                            volverDetallePedido();
                        }
                    }
                }
            });
        }


    }

    /**
     * Método para actulizar el stock una vez modificado el pedido
     * @param pedido
     */
    private void actulizarStock(DatabaseReference databaseReference , Pedido pedido) {
        // Obtener detalles originales del pedido
        databaseReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Pedido pedidoOriginal = dataSnapshot.getValue(Pedido.class);
                if (pedidoOriginal != null) {
                    // Detalles originales del pedido

                    List<DetallePedidoNoParcel> detallesOriginales = pedidoOriginal.getDetalles();
                    // Detalles modificados
                    List<DetallePedidoNoParcel> detallesModificados = pedido.getDetalles();
                    //Ver detalles en log

                    // Actualizar el stock en la base de datos según las diferencias entre las cantidades originales y modificadas
                    for (int i = 0; i < detallesOriginales.size(); i++) {
                        DetallePedidoNoParcel detalleOriginal = detallesOriginales.get(i);
                        DetallePedidoNoParcel detalleModificado = detallesModificados.get(i);

                        // Calcular la diferencia entre la cantidad original y la cantidad modificada
                        int diferencia = detalleModificado.getCantidad() - detalleOriginal.getCantidad();
                        Log.e("stockPrueba", "Diferencia Stock ." + diferencia);

                        // Obtener la referencia al  de la ración en la base de datos
                        DatabaseReference racionRef = FirebaseDatabase.getInstance().getReference().child("raciones").child(detalleOriginal.getRacion());

                        // Actualizar el stock, es la suma del stock atual con la diferencia
                        racionRef.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(DataSnapshot dataSnapshot) {
                                Racion racion = dataSnapshot.getValue(Racion.class);

                                if (racion != null) {
                                    int stockActual = Integer.parseInt(racion.getStock());
                                    Log.e("stockPrueba", "Stock actual." + racion.getStock());
                                    int nuevoStock = stockActual + diferencia;
                                    Log.e("stockPrueba", "Nuevo stock." + nuevoStock);
                                    if (nuevoStock >= 0) {
                                        // Actualizar el stock en la base de datos
                                        racion.setStock(String.valueOf(nuevoStock));
                                        racionRef.setValue(racion);
                                        Log.d("StockActualizado", "Stock de la ración " + detalleOriginal.getRacion() + " actualizado a: " + nuevoStock);
                                    } else {
                                        // Log prueba error negativo
                                        Log.e("ErrorStock", "El nuevo stock es negativo.");
                                    }
                                } else {
                                    Log.e("ErrorStock", "No se encontró la ración en la base de datos.");
                                }
                            }

                            @Override
                            public void onCancelled(DatabaseError databaseError) {
                                // Errores de la base de datos
                                Log.e("Error BBDD", "Error al obtener la ración: " + databaseError.getMessage());
                            }
                        });
                    }
                } else {
                    Log.e("ErrorPedido", "No se encontró el pedido en la base de datos.");
                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // Manejar errores de la base de datos
                Log.e("Error BBDD", "Error al obtener el pedido: " + databaseError.getMessage());
            }
        });
    }
}