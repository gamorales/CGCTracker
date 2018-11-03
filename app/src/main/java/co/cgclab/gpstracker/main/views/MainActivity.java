package co.cgclab.gpstracker.main.views;

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.ActivityCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.text.Html;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.w3c.dom.Text;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import co.cgclab.gpstracker.R;
import co.cgclab.gpstracker.carros.models.CarrosModel;
import co.cgclab.gpstracker.carros.views.CarrosActivity;
import co.cgclab.gpstracker.main.models.CoordenadasModel;
import co.cgclab.gpstracker.usuarios.views.ClaveActivity;
import co.cgclab.gpstracker.web.controllers.IComandosVehiculo;
import co.cgclab.gpstracker.usuarios.views.LoginActivity;
import co.cgclab.gpstracker.web.models.CommandResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private ImageView btnEncendidoOn, btnEncendidoOff, btnMovimientoOn, btnMovimientoOff;
    private ImageView btnBloquear, btnDesbloquear, btnVelocidadOn, btnVelocidadOff;
    private ImageView btnEscuchar, btnTracker, btnAbout, btnStatus;
    private TextView tvMainMenuEmail;
    private Spinner spVehiculos;
    private NavigationView navigationView;
    private View headerView;

    // Dialog State
    TextView estadoPlaca, estadoTelefono, estadoStatus, estadoSpeed, estadoGPS;

    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private FirebaseUser firebaseUser;
    private Retrofit retrofit;

    String login, userID, imei, telefono;
    String[] placa;
    private List<String> lVehiculos;
    private IComandosVehiculo iComandosVehiculo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        initRetrofit();

        Intent intent = getIntent();
        login = intent.getStringExtra("email");

        // Se obtienen los datos del usuario logueado
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        userID = firebaseUser.getUid();

        spVehiculos = findViewById(R.id.spVehiculos);
        cargarSpinner();

        navigationView = findViewById(R.id.nav_view);
        headerView = navigationView.getHeaderView(0);

        if (login != null) {
            try {
                /*
                 * Como el TextView está dentro de un NavigationView,
                 * el setText se hace de esta forma
                 */
                tvMainMenuEmail = headerView.findViewById(R.id.tvMainMenuEmail);
                tvMainMenuEmail.setText(firebaseUser.getEmail());
            } catch (Exception ex) {
                Log.e("MainActivity", "" + ex.getMessage());
            }
        }

        // Menú lateral
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
            this,
            drawer,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        );
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        btnEscuchar = findViewById(R.id.btnEscuchar);
        btnTracker = findViewById(R.id.btnTracker);
        btnStatus = findViewById(R.id.btnStatus);
        btnAbout = findViewById(R.id.btnAbout);
        btnBloquear = findViewById(R.id.btnBloquear);
        btnDesbloquear = findViewById(R.id.btnDesbloquear);
        btnVelocidadOn = findViewById(R.id.btnVelocidadOn);
        btnVelocidadOff = findViewById(R.id.btnVelocidadOff);
        btnEncendidoOn = findViewById(R.id.btnEncendidoOn);
        btnEncendidoOff = findViewById(R.id.btnEncendidoOff);
        btnMovimientoOn = findViewById(R.id.btnMovimientoOn);
        btnMovimientoOff = findViewById(R.id.btnMovimientoOff);

        // Se hará la llamada, cuando cuelgue, vuelve al aplicativo
        PhoneCallListener phoneListener = new PhoneCallListener();
        TelephonyManager telephonyManager = (TelephonyManager) this
                .getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(phoneListener, PhoneStateListener.LISTEN_CALL_STATE);
        btnEscuchar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                placa = spVehiculos.getSelectedItem().toString().split(" - ");

                if (placa.length==2) {
                    mostrarMensaje(
                        getResources().getString(R.string.calling_car)+" "+placa[1],
                        1
                    );
                    consultarVehiculo(placa[0], true);
                } else {
                    mostrarMensaje(getResources().getString(R.string.select_car), 1);
                }
            }
        });
        btnTracker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                startActivity(intent);
            }
        });
        btnStatus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            //mostrarMensaje("check", 1);
            //enviarComandos("status");
                showAboutDialog(1);
            }
        });
        btnAbout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAboutDialog(2);
            }
        });

        /******************************* Comandos *******************************/
        btnBloquear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enviarComandos(getResources().getString(R.string.cut_off_oil),
                               "cut_off_oil", "0", "0", "0", "0");
            }
        });
        btnDesbloquear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enviarComandos(getResources().getString(R.string.resume_oil),
                               "resume_oil", "0", "0", "0", "0");
            }
        });
        btnVelocidadOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showOptionsDialog("velocidad");
            }
        });
        btnVelocidadOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enviarComandos(getResources().getString(R.string.msg_speed_off),
                              "speed_limit_off", "0", "0", "0", "0");
            }
        });
        btnEncendidoOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enviarComandos(getResources().getString(R.string.msg_start_on),
                              "move_alarm", "0", "0", "0", "0");
            }
        });
        btnEncendidoOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enviarComandos(getResources().getString(R.string.msg_start_off),
                              "move_cancel", "0", "0", "0", "0");
            }
        });
        btnMovimientoOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showOptionsDialog("tiempo");
            }
        });
        btnMovimientoOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enviarComandos(getResources().getString(R.string.msg_track_off),
                               "cancel_track", "0", "0", "0", "0");
            }
        });
    }

    private void showOptionsDialog(final String tipo) {
        final TextView tvSpeedLabel;
        final Spinner spTiempos;
        final TextInputEditText txtVelocidad, txtDistancia;
        Button btnAceptar, btnCancelar;

        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.activity_conf_speed);

        tvSpeedLabel = dialog.findViewById(R.id.tvSpeedLabel);
        spTiempos = dialog.findViewById(R.id.spTiempos);
        txtVelocidad = dialog.findViewById(R.id.txtVelocidad);
        txtDistancia = dialog.findViewById(R.id.txtDistancia);
        btnAceptar = dialog.findViewById(R.id.btnAceptar);
        btnCancelar = dialog.findViewById(R.id.btnCancelar);

        spTiempos.setVisibility(View.GONE);
        txtDistancia.setVisibility(View.GONE);
        txtVelocidad.setVisibility(View.GONE);

        switch (tipo) {
            case "velocidad":
                tvSpeedLabel.setText(getResources().getString(R.string.speed_lbl_velocidad));
                txtVelocidad.setVisibility(View.VISIBLE);
                break;
            case "distancia":
                tvSpeedLabel.setText(getResources().getString(R.string.speed_lbl_distancia));
                txtDistancia.setVisibility(View.VISIBLE);
                break;
            case "tiempo":
                tvSpeedLabel.setText(getResources().getString(R.string.speed_lbl_tiempo));
                spTiempos.setVisibility(View.VISIBLE);
                break;
        }

        btnAceptar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String valor = "";
                switch (tipo) {
                    case "velocidad":
                        valor = txtVelocidad.getText().toString();
                        if (valor.trim().equals("")) {
                            mostrarMensaje(getResources().getString(R.string.msg_no_speed), 1);
                        } else {
                            enviarComandos(getResources().getString(R.string.msg_speed),
                                    "speed_limit", valor, "0", "0", "0");
                            dialog.dismiss();
                        }
                        break;
                    case "distancia":
                        valor = (Integer.parseInt(txtDistancia.getText().toString())<1000?"0"+txtDistancia.getText().toString():txtDistancia.getText().toString());
                        if (valor.trim().equals("")) {
                            mostrarMensaje(getResources().getString(R.string.msg_no_distance), 1);
                        } else {
                            enviarComandos(getResources().getString(R.string.msg_distance),
                                    "track_distance", valor, "0", "0", "0");
                        }
                        dialog.dismiss();
                        break;
                    case "tiempo":
                        valor = spTiempos.getSelectedItem().toString();
                        if (valor.trim().equals("")) {
                            mostrarMensaje(getResources().getString(R.string.msg_no_time), 1);
                        } else {
                            enviarComandos(getResources().getString(R.string.msg_time),
                                    "track_interval", valor, "0", "0", "0");
                        }
                        dialog.dismiss();
                        break;
                }
            }
        });

        btnCancelar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    public void showAboutDialog(int dialogForm) {
        final Dialog dialog = new Dialog(this);

        switch (dialogForm) {
            case 1:
                if (placa.length==2) {
                    dialog.setContentView(R.layout.activity_estado);

                    TextView estado_lbl_cerrar = dialog.findViewById(R.id.estado_lbl_cerrar);
                    estadoPlaca = dialog.findViewById(R.id.estado_placa);
                    estadoTelefono = dialog.findViewById(R.id.estado_telefono);
                    estadoStatus = dialog.findViewById(R.id.estado_estado);
                    estadoSpeed = dialog.findViewById(R.id.estado_speed);
                    estadoGPS = dialog.findViewById(R.id.estado_gps);
                    estado_lbl_cerrar.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            dialog.dismiss();
                        }
                    });

                    ultimoEstado(imei, placa[0]);
                } else {
                    mostrarMensaje(getResources().getString(R.string.select_car), 1);
                }
                break;
            case 2:
                dialog.setContentView(R.layout.activity_about);

                TextView cgclab_url = dialog.findViewById(R.id.cgclab_url);
                cgclab_url.setText(Html.fromHtml(getResources().getString(R.string.base_url_click)));
                cgclab_url.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent openBrowser = new Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(getResources().getString(R.string.base_url))
                        );
                        startActivity(openBrowser);
                    }
                });

                ImageView dialog_logo_cerrar = dialog.findViewById(R.id.dialog_logo_cerrar);
                dialog_logo_cerrar.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        dialog.dismiss();
                    }
                });

                break;
        }

        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    /**
     * Se mostrará en el Dialog el último estado del vehículo, extrayendo la info de la última
     * hora guardada en Firebase
     *
     * @param imei
     * @param placa
     */
    public void ultimoEstado(String imei, String placa) {
        DateFormat dateFormat = new SimpleDateFormat("yyMMdd");
        Date date = new Date();

        estadoPlaca.setText(placa);
        estadoTelefono.setText(telefono);

        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        DatabaseReference databaseReference = firebaseDatabase.getReference("Coordenadas/"+userID+"/"+imei);
        Query query = firebaseDatabase.getReference(
            "Coordenadas/"+userID+"/"+imei+"/"+dateFormat.format(date)
        ).orderByChild("fecha").limitToLast(1);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snap: dataSnapshot.getChildren()) {
                    final CoordenadasModel coordenadasModel = snap.getValue(CoordenadasModel.class);

                    estadoGPS.setText(coordenadasModel.getLatitud()+","+coordenadasModel.getLongitud());
                    estadoGPS.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Intent intent = new Intent(
                                    MainActivity.this,
                                    StreetViewActivity.class
                            );
                            intent.putExtra("latitud", coordenadasModel.getLatitud()+"");
                            intent.putExtra("longitud", coordenadasModel.getLongitud()+"");
                            startActivity(intent);
                        }
                    });

                    estadoStatus.setText(coordenadasModel.getTipo());
                    estadoSpeed.setText(coordenadasModel.getVelocidad()+" Km/h");
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    private void enviarComandos(final String mensaje, final String comando, final String value,
                                final String longitud01, final String latitud02,
                                final String longitud02) {
        placa = spVehiculos.getSelectedItem().toString().split(" - ");
        try {
            if (placa.length==2) {
                FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
                final DatabaseReference databaseReference = firebaseDatabase.getReference("Vehiculos/"+placa[0]);
                databaseReference.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            CarrosModel vehiculo = dataSnapshot.getValue(CarrosModel.class);
                            String imeiGPS = vehiculo.getImei();

                            Call<CommandResponse> commandResponseCall = iComandosVehiculo.comandoVehiculo(
                                imeiGPS,
                                comando,
                                value,
                                longitud01,
                                latitud02,
                                longitud02
                            );

                            commandResponseCall.enqueue(new Callback<CommandResponse>() {
                                @Override
                                public void onResponse(Call<CommandResponse> call, Response<CommandResponse> response) {
                                    try {
                                        Log.i("MainActivity", response.body()+"");
                                        CommandResponse comandos = response.body();
                                        if (comandos.getSuccess()==1) {
                                            mostrarMensaje(comandos.getData(),1);
                                            mostrarMensaje(mensaje, 1);
                                        } else {
                                            mostrarMensaje(
                                                    getResources().getString(R.string.check_data),
                                                    1
                                            );
                                        }
                                    } catch (Exception ex) {
                                        Log.e("MainActivity", "ERROR 2: "+ex.getMessage());
                                    }
                                }

                                @Override
                                public void onFailure(Call<CommandResponse> call, Throwable t) {
                                    Log.e("MainActivity", "onFailure: "+t.getMessage());
                                }
                            });
                        }

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });
            } else {
                mostrarMensaje(getResources().getString(R.string.select_car), 1);
            }
        } catch (Exception e) {
            Log.e("MainActivity", "lock On "+e.getMessage());
        }
    }

    public void initRetrofit() {
        Gson gson = new GsonBuilder().setLenient().create();
        retrofit = new Retrofit.Builder()
                .baseUrl(getApplicationContext().getString(R.string.server_url))
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        iComandosVehiculo = retrofit.create(IComandosVehiculo.class);
    }

    /**
     * Método encargado de hacer las llamadas al dispositivo, además de guardar el valor en la
     * variable 'imei'
     *
     * @param placa
     * @param llamar Boolean true -> Hace la llamada al celular
     */
    private void consultarVehiculo(final String placa, final boolean llamar) {
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        DatabaseReference databaseReference = firebaseDatabase.getReference("Vehiculos");
        Query query = databaseReference.orderByChild("idUsuario").equalTo(userID);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    try {
                        for (DataSnapshot elemento : dataSnapshot.getChildren()) {
                            CarrosModel vehiculo = elemento.getValue(CarrosModel.class);
                            if (vehiculo.getPlaca().equals(placa)) {
                                imei = vehiculo.getImei();
                                telefono = vehiculo.getTelefono();

                                if (llamar) {
                                    Intent callIntent = new Intent(Intent.ACTION_CALL);
                                    callIntent.setData(Uri.parse("tel:" + vehiculo.getTelefono().toString()));
                                    if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                                        // TODO: Consider calling
                                        //    ActivityCompat#requestPermissions
                                        // here to request the missing permissions, and then overriding
                                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                        //                                          int[] grantResults)
                                        // to handle the case where the user grants the permission. See the documentation
                                        // for ActivityCompat#requestPermissions for more details.
                                        return;
                                    }
                                    startActivity(callIntent);
                                }
                            }
                        }
                    } catch (Exception ex) {
                        Log.e("CarroActivity", "111) ERROR: " + ex.getMessage());
                    }

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void cargarSpinner() {
        lVehiculos = new ArrayList<String>();
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        DatabaseReference databaseReference = firebaseDatabase.getReference("Vehiculos");

        /*
         * Con Query se hace consulta por un campo específico dentro de la tabla.
         * Si hubiera un child sería,
         * databaseReference.child("nombreChild").orderByChild("idUsuario").equalTo(userID);
         */
        Query query = databaseReference.orderByChild("idUsuario").equalTo(userID);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    lVehiculos.clear();
                    lVehiculos.add(getResources().getString(R.string.choose_car));
                    try {
                        for (DataSnapshot elemento: dataSnapshot.getChildren()) {
                            CarrosModel vehiculo = elemento.getValue(CarrosModel.class);

                            if (vehiculo.getActivo().equals("1")) {
                                lVehiculos.add(vehiculo.getPlaca() + " - " + vehiculo.getNombre());
                            }
                        }
                        mostrarMensaje(
                            getResources().getString(R.string.update_car_list),
                            1
                        );
                    } catch (Exception ex) {
                        Log.e("MainActivity", "402) ERROR: "+ex.getMessage());
                    }

                    ArrayAdapter<String> adapterVehiculo = new ArrayAdapter<String>(
                        MainActivity.this,
                        android.R.layout.simple_spinner_item,
                        lVehiculos
                    );
                    adapterVehiculo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                    spVehiculos.setAdapter(adapterVehiculo);

                    // Cuando se seleccione un elemento, quede mostrándose en el spinner
                    spVehiculos.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                            spVehiculos.setSelection(i);
                            placa = spVehiculos.getSelectedItem().toString().split(" - ");
                            consultarVehiculo(placa[0], false);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> adapterView) {

                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    private void cargarSpinner2() {
        lVehiculos = new ArrayList<String>();
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        DatabaseReference databaseReference = firebaseDatabase.getReference("Vehiculos");

        /*
         * Con Query se hace consulta por un campo específico dentro de la tabla.
         * Si hubiera un child sería,
         * databaseReference.child("nombreChild").orderByChild("idUsuario").equalTo(userID);
         */
        Query query = databaseReference.orderByChild("idUsuario").equalTo(userID);
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()) {
                    mostrarMensaje(
                        getResources().getString(R.string.update_car_list),
                        1
                    );
                    lVehiculos.clear();
                    lVehiculos.add(getResources().getString(R.string.all_cars));
                    try {
                        for (DataSnapshot elemento: dataSnapshot.getChildren()) {
                            CarrosModel vehiculo = elemento.getValue(CarrosModel.class);
                            if (vehiculo.getActivo().equals("1")) {
                                lVehiculos.add(vehiculo.getPlaca() + " - " + vehiculo.getNombre());
                            }
                        }
                    } catch (Exception ex) {
                        Log.e("MainActivity", "214) ERROR: "+ex.getMessage());
                    }

                    ArrayAdapter<String> adapterVehiculo = new ArrayAdapter<String>(
                        MainActivity.this,
                        android.R.layout.simple_spinner_item,
                        lVehiculos
                    );
                    adapterVehiculo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

                    spVehiculos.setAdapter(adapterVehiculo);

                    // Cuando se seleccione un elemento, quede mostrándose en el spinner
                    spVehiculos.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                            spVehiculos.setSelection(i);
                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> adapterView) {

                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    @Override
    public void onBackPressed() {
        // En caso de ir hacía atrás, se esconderá el menú lateral
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void mostrarMensaje(String mensaje, Integer largo) {
        Toast.makeText(
                MainActivity.this,
                mensaje,
                largo==1?Toast.LENGTH_LONG:Toast.LENGTH_SHORT
        ).show();
    }

    public void iniciarConexion() {
        firebaseAuth = FirebaseAuth.getInstance();
        authStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
                if (firebaseUser==null) {
                    mostrarMensaje(getResources().getString(R.string.user_no_connected), 0);
                }
            }
        };
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();
        Intent intent;
        switch (id) {
            case R.id.mnu_add_car:
                intent = new Intent(MainActivity.this, CarrosActivity.class);
                intent.putExtra("mail", login);
                startActivity(intent);
                break;
            case R.id.mnu_modificar:
                intent = new Intent(MainActivity.this, ClaveActivity.class);
                intent.putExtra("login", login);
                intent.putExtra("activity", "main");
                startActivity(intent);
                break;
            case R.id.mnu_logout:
                firebaseAuth.signOut();
                intent = new Intent(MainActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();
                break;
            case R.id.mnu_acercade:
                showAboutDialog(2);
                break;
            case R.id.mnu_ayuda:
                mostrarMensaje("Ayuda", 0);
                break;
        }

        /*if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }*/

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        iniciarConexion();
        firebaseAuth.addAuthStateListener(authStateListener);
    }

    @Override
    protected void onStop() {
        super.onStop();
        firebaseAuth.removeAuthStateListener(authStateListener);
    }

    //monitor phone call activities
    private class PhoneCallListener extends PhoneStateListener {

        private boolean isPhoneCalling = false;

        String LOG_TAG = "LOGGING 123";

        @Override
        public void onCallStateChanged(int state, String incomingNumber) {

            if (TelephonyManager.CALL_STATE_RINGING == state) {
                // phone ringing
                Log.i(LOG_TAG, "RINGING, number: " + incomingNumber);
            }

            if (TelephonyManager.CALL_STATE_OFFHOOK == state) {
                // active
                Log.i(LOG_TAG, "OFFHOOK");

                isPhoneCalling = true;
            }

            if (TelephonyManager.CALL_STATE_IDLE == state) {
                // run when class initial and phone call ended,
                // need detect flag from CALL_STATE_OFFHOOK
                Log.i(LOG_TAG, "IDLE");

                if (isPhoneCalling) {

                    Log.i(LOG_TAG, "restart app");

                    // restart app
                    Intent i = getBaseContext().getPackageManager()
                            .getLaunchIntentForPackage(
                                    getBaseContext().getPackageName());
                    i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(i);

                    isPhoneCalling = false;
                }

            }
        }
    }
}
