package com.github.charleslzq.samplejava;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.EditText;
import android.widget.Toast;

import com.bin.david.form.core.SmartTable;
import com.bin.david.form.data.column.Column;
import com.bin.david.form.data.format.IFormat;
import com.bin.david.form.data.table.PageTableData;
import com.github.charleslzq.arcsofttools.kotlin.ArcSoftFaceOfflineEngine;
import com.github.charleslzq.arcsofttools.kotlin.Face;
import com.github.charleslzq.arcsofttools.kotlin.Person;
import com.github.charleslzq.arcsofttools.kotlin.WebSocketArcSoftService;
import com.github.charleslzq.arcsofttools.kotlin.support.Nv21ImageUtils;
import com.github.charleslzq.faceengine.core.TrackedFace;
import com.github.charleslzq.faceengine.support.ServiceCaller;
import com.github.charleslzq.faceengine.support.ServiceConnectionProvider;
import com.github.charleslzq.faceengine.support.ServiceInvoker;
import com.github.charleslzq.facestore.FaceStoreChangeListener;
import com.github.charleslzq.facestore.Meta;
import com.github.charleslzq.facestore.websocket.WebSocketCompositeFaceStore;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss.SSS");
    private static final List<Column> COLUMNS = buildColumnSetting();
    private final AtomicBoolean loaded = new AtomicBoolean(false);
    @BindView(R.id.faceStoreTable)
    SmartTable faceStoreTable;
    @BindView(R.id.tableFilterText)
    EditText tableFilterText;
    private Predicate<Person> defaultFilter = new Predicate<Person>() {
        @Override
        public boolean test(Person person) {
            return true;
        }
    };
    private Predicate<Person> tableFilter = defaultFilter;
    private final FaceStoreChangeListener<Person, Face> storeListener = new FaceStoreChangeListener<Person, Face>() {
        @Override
        public void onFaceDelete(String s, String s1) {
            reload();
        }

        @Override
        public void onPersonDelete(String s) {
            reload();
        }

        @Override
        public void onFaceUpdate(String s, Face face) {
            reload();
        }

        @Override
        public void onPersonUpdate(Person person) {
            reload();
        }
    };
    private ServiceConnectionProvider<ArcSoftFaceOfflineEngine<WebSocketCompositeFaceStore<Person, Face>>> connection
            = WebSocketArcSoftService.Companion.getBuilder()
            .afterConnected(new ServiceInvoker<ArcSoftFaceOfflineEngine<WebSocketCompositeFaceStore<Person, Face>>>() {
                @Override
                public void invoke(ArcSoftFaceOfflineEngine<WebSocketCompositeFaceStore<Person, Face>> service) {
                    service.getStore().getListeners().add(storeListener);
                    service.getStore().refresh();
                }
            })
            .beforeDisconnect(new ServiceInvoker<ArcSoftFaceOfflineEngine<WebSocketCompositeFaceStore<Person, Face>>>() {
                @Override
                public void invoke(ArcSoftFaceOfflineEngine<WebSocketCompositeFaceStore<Person, Face>> service) {
                    service.getStore().getListeners().remove(storeListener);
                }
            })
            .build();

    private static List<Column> buildColumnSetting() {
        List<Column> result = new ArrayList<>();

        Column<String> personIdColumn = new Column<>("id", "person.id");
        Column<String> personNameColumn = new Column<>("id", "person.name");
        Column personColumn = new Column("Person", personIdColumn, personNameColumn);
        result.add(personColumn);

        Column<List<Face>> faceCountColumn = new Column<>("faceCount", "faces", new IFormat<List<Face>>() {
            @Override
            public String format(List<Face> faces) {
                return String.valueOf(faces.size());
            }
        });
        result.add(faceCountColumn);

        Column<List<Face>> lastUpdateColumn = new Column<>("Last Update", "faces", new IFormat<List<Face>>() {
            @Override
            public String format(List<Face> faces) {
                if (!faces.isEmpty()) {
                    Face lastUpdateFace = Collections.max(faces, new Comparator<Face>() {
                        @Override
                        public int compare(Face face1, Face face2) {
                            return face1.getUpdateTime().compareTo(face2.getUpdateTime());
                        }
                    });
                    return lastUpdateFace == null ? "UNKNOWN" : DATE_TIME_FORMATTER.print(lastUpdateFace.getUpdateTime());
                } else {
                    return "UNKNOWN";
                }
            }
        });
        result.add(lastUpdateColumn);

        return result;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        faceStoreTable.getConfig().setShowXSequence(false);
        faceStoreTable.getConfig().setShowYSequence(false);
        tableFilterText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s == null || s.toString().trim().isEmpty()) {
                    tableFilter = defaultFilter;
                } else {
                    final String text = s.toString().trim();
                    tableFilter = new Predicate<Person>() {
                        @Override
                        public boolean test(Person person) {
                            return person.getId().contains(text) || person.getName().contains(text);
                        }
                    };
                }
                if (!reload()) {
                    toast("Unable to find corresponding person with id or name contains the text");
                }
            }
        });

        connection.bind(this, WebSocketArcSoftService.class);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (RequestCodes.fromCode(requestCode)) {
            case FACE_CHECK:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String personName = data.getExtras().getString("personName");
                    toast("Found Person " + personName);
                } else {
                    toast("Fail to identify face");
                }
                break;
            case IMAGE_CAMERA:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    registerImage(data);
                }
                break;
        }
    }

    private boolean reload() {
        Boolean result = connection.whenConnected(new ServiceCaller<ArcSoftFaceOfflineEngine<? extends WebSocketCompositeFaceStore<Person, Face>>, Boolean>() {
            @Override
            public Boolean call(ArcSoftFaceOfflineEngine<? extends WebSocketCompositeFaceStore<Person, Face>> service) {
                List<FaceData<Person, Face>> faceDataList = new ArrayList<>();
                for (String id : service.getStore().getPersonIds()) {
                    Person person = service.getStore().getPerson(id);
                    if (tableFilter.test(person)) {
                        List<String> faceIdList = service.getStore().getFaceIdList(id);
                        List<Face> faces = new ArrayList<>();
                        for (String faceId : faceIdList) {
                            Face face = service.getStore().getFace(id, faceId);
                            if (face != null) {
                                faces.add(face);
                            }
                        }
                        faceDataList.add(new FaceData<>(person, faces));
                    }
                }
                if (!faceDataList.isEmpty()) {
                    PageTableData<FaceData<Person, Face>> pageTableData = new PageTableData<FaceData<Person, Face>>("Registered Persons And Faces",
                            faceDataList,
                            COLUMNS
                    );
                    if (!loaded.compareAndSet(false, true)) {
                        pageTableData.setPageSize(100);
                    }
                    faceStoreTable.setTableData(pageTableData);
                    return true;
                } else {
                    return false;
                }
            }
        });
        return result != null && result;
    }

    private void registerImage(final Intent data) {
        connection.whenConnected(new ServiceInvoker<ArcSoftFaceOfflineEngine<? extends WebSocketCompositeFaceStore<Person, Face>>>() {
            @Override
            public void invoke(final ArcSoftFaceOfflineEngine<? extends WebSocketCompositeFaceStore<Person, Face>> service) {
                final Map<TrackedFace, Face> faces = service.detect(Nv21ImageUtils.toFrame(BitmapFileHelper.load(data.getExtras().getString("picPath"))));
                if (!faces.isEmpty() && faces.size() == 1) {
                    final AtomicReference<SimplePerson> selectedPerson = new AtomicReference<>(null);
                    View dialogLayout = getLayoutInflater().inflate(R.layout.dialog_register, null);
                    final AutoCompleteTextView autoCompleteTextView = dialogLayout.findViewById(R.id.personRegister);
                    autoCompleteTextView.setAdapter(new ArrayAdapter<>(
                            MainActivity.this,
                            android.R.layout.simple_dropdown_item_1line,
                            constructAutoCompleteData()
                    ));
                    autoCompleteTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                        @Override
                        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                            selectedPerson.set(((ArrayAdapter<SimplePerson>) parent.getAdapter()).getItem(position));
                        }
                    });
                    autoCompleteTextView.addTextChangedListener(new TextWatcher() {
                        @Override
                        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                        }

                        @Override
                        public void onTextChanged(CharSequence s, int start, int before, int count) {

                        }

                        @Override
                        public void afterTextChanged(Editable s) {
                            selectedPerson.set(null);
                        }
                    });
                    (new AlertDialog.Builder(MainActivity.this))
                            .setView(dialogLayout)
                            .setTitle("Your ID or Name")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String personId = selectedPerson.get() != null ? selectedPerson.get().getId() : UUID.randomUUID().toString();
                                    String personName = selectedPerson.get() != null ? selectedPerson.get().getName() : autoCompleteTextView.getText().toString();
                                    if (selectedPerson.get() == null) {
                                        service.getStore().savePerson(new Person(personId, personName));
                                    }
                                    service.getStore().saveFace(personId, faces.get(0));
                                }
                            })
                            .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            })
                            .show();
                    String testPersonId = "test";
                    service.getStore().savePerson(new Person(testPersonId, "test_name"));
                    for (Face face : faces.values()) {
                        service.getStore().saveFace(testPersonId, face);
                    }
                }
            }
        });
    }

    @OnClick(R.id.captureImageButton)
    public void onCaptureImageButtonClicked() {
        startActivityForResult(
                new Intent(MediaStore.ACTION_IMAGE_CAPTURE),
                RequestCodes.IMAGE_CAMERA.getCode()
        );
    }

    @OnClick(R.id.checkFaceButton)
    public void onFaceCheckButtonClicked() {
        startActivityForResult(
                new Intent(this, FaceDetectActivity.class),
                RequestCodes.FACE_CHECK.getCode()
        );
    }

    @OnClick(R.id.refreshButton)
    public void onRefreshButtonClicked() {
        connection.whenConnected(new ServiceInvoker<ArcSoftFaceOfflineEngine<? extends WebSocketCompositeFaceStore<Person, Face>>>() {
            @Override
            public void invoke(ArcSoftFaceOfflineEngine<? extends WebSocketCompositeFaceStore<Person, Face>> service) {
                service.getStore().refresh();
            }
        });
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        unbindService(connection);
        super.onDestroy();
    }

    private List<SimplePerson> constructAutoCompleteData() {
        List<SimplePerson> result = connection.whenConnected(new ServiceCaller<ArcSoftFaceOfflineEngine<? extends WebSocketCompositeFaceStore<Person, Face>>, List<SimplePerson>>() {
            @Override
            public List<SimplePerson> call(ArcSoftFaceOfflineEngine<? extends WebSocketCompositeFaceStore<Person, Face>> service) {
                List<SimplePerson> result = new ArrayList<>();
                for (String id : service.getStore().getPersonIds()) {
                    Person person = service.getStore().getPerson(id);
                    if (person != null) {
                        result.add(SimplePerson.fromPerson(person));
                    }
                }
                return result;
            }
        });
        if (result == null) {
            return Collections.emptyList();
        } else {
            return result;
        }
    }

    enum RequestCodes {
        IMAGE_CAMERA,
        FACE_CHECK;

        public static RequestCodes fromCode(int code) {
            return RequestCodes.values()[code - 1];
        }

        public int getCode() {
            return ordinal() + 1;
        }
    }

    interface Predicate<T> {
        boolean test(T t);
    }

    public static class FaceData<P extends Meta, F extends Meta> {
        private final P person;
        private final List<F> faces;

        public FaceData(P person, List<F> faces) {
            this.person = person;
            this.faces = faces;
        }

        public P getPerson() {
            return person;
        }

        public List<F> getFaces() {
            return faces;
        }
    }

    public static class SimplePerson {
        private final String id;
        private final String name;

        public SimplePerson(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public static SimplePerson fromPerson(Person person) {
            return new SimplePerson(person.getId(), person.getName());
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }
}
