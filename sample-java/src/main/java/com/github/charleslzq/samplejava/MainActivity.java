package com.github.charleslzq.samplejava;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.IBinder;
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
import com.github.charleslzq.arcsofttools.kotlin.ArcSoftFaceEngineService;
import com.github.charleslzq.arcsofttools.kotlin.Face;
import com.github.charleslzq.arcsofttools.kotlin.Person;
import com.github.charleslzq.arcsofttools.kotlin.WebSocketArcSoftEngineService;
import com.github.charleslzq.arcsofttools.kotlin.support.Nv21ImageUtils;
import com.github.charleslzq.facestore.FaceData;
import com.github.charleslzq.facestore.FaceStoreChangeListener;
import com.github.charleslzq.facestore.websocket.WebSocketCompositeFaceStore;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
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
    private ArcSoftFaceEngineService<WebSocketCompositeFaceStore<Person, Face>> faceEngineService = null;
    private Predicate<FaceData<Person, Face>> defaultFilter = new Predicate<FaceData<Person, Face>>() {
        @Override
        public boolean test(FaceData<Person, Face> faceData) {
            return true;
        }
    };
    private Predicate<FaceData<Person, Face>> tableFilter = defaultFilter;
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            faceEngineService = (ArcSoftFaceEngineService<WebSocketCompositeFaceStore<Person, Face>>) iBinder;
            faceEngineService.getStore().getListeners().add(storeListener);
            faceEngineService.getStore().refresh();
            reload();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            faceEngineService.getStore().getListeners().remove(storeListener);
            faceEngineService = null;
        }
    };
    private final FaceStoreChangeListener<Person, Face> storeListener = new FaceStoreChangeListener<Person, Face>() {
        @Override
        public void onPersonFaceClear(String s) {
            reload();
        }

        @Override
        public void onFaceDelete(String s, String s1) {
            reload();
        }

        @Override
        public void onFaceDataDelete(String s) {
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
                Face lastUpdateFace = Collections.max(faces, new Comparator<Face>() {
                    @Override
                    public int compare(Face face1, Face face2) {
                        return face1.getUpdateTime().compareTo(face2.getUpdateTime());
                    }
                });
                return lastUpdateFace == null ? "UNKNOWN" : DATE_TIME_FORMATTER.print(lastUpdateFace.getUpdateTime());
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
                    tableFilter = new Predicate<FaceData<Person, Face>>() {
                        @Override
                        public boolean test(FaceData<Person, Face> personFaceFaceData) {
                            return personFaceFaceData.getPerson().getId().contains(text) || personFaceFaceData.getPerson().getName().contains(text);
                        }
                    };
                }
                if (!reload()) {
                    toast("Unable to find corresponding person with id or name contains the text");
                }
            }
        });

        bindService(new Intent(this, WebSocketArcSoftEngineService.class), serviceConnection, Context.BIND_AUTO_CREATE);
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
                if (resultCode == Activity.RESULT_OK && data != null && faceEngineService != null) {
                    registerImage(data);
                }
                break;
        }
    }

    private boolean reload() {
        if (faceEngineService != null) {
            List<FaceData<Person, Face>> faceDataList = new ArrayList<>();
            for (String id : faceEngineService.getStore().getPersonIds()) {
                FaceData<Person, Face> faceData = faceEngineService.getStore().getFaceData(id);
                if (tableFilter.test(faceData)) {
                    faceDataList.add(faceData);
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
        } else {
            return false;
        }
    }

    private void registerImage(Intent data) {
        final List<Face> faceList = faceEngineService.detect(Nv21ImageUtils.toFrame((Bitmap) data.getExtras().get("data")));
        if (!faceList.isEmpty() && faceList.size() == 1) {
            final AtomicReference<SimplePerson> selectedPerson = new AtomicReference<>(null);
            View dialogLayout = getLayoutInflater().inflate(R.layout.dialog_register, null);
            final AutoCompleteTextView autoCompleteTextView = dialogLayout.findViewById(R.id.personRegister);
            autoCompleteTextView.setAdapter(new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_dropdown_item_1line,
                    constructAutoCompleteData())
            );
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
            (new AlertDialog.Builder(this))
                    .setView(dialogLayout)
                    .setTitle("Your ID or Name")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String personId = selectedPerson.get() != null ? selectedPerson.get().getId() : UUID.randomUUID().toString();
                            String personName = selectedPerson.get() != null ? selectedPerson.get().getName() : autoCompleteTextView.getText().toString();
                            if (selectedPerson.get() == null) {
                                faceEngineService.getStore().savePerson(new Person(personId, personName));
                            }
                            faceEngineService.getStore().saveFace(personId, faceList.get(0));
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
            faceEngineService.getStore().savePerson(new Person(testPersonId, "test_name"));
            for (Face face : faceList) {
                faceEngineService.getStore().saveFace(testPersonId, face);
            }
        }
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
        if (faceEngineService != null) {
            faceEngineService.getStore().refresh();
        }
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onDestroy() {
        unbindService(serviceConnection);
        super.onDestroy();
    }

    private List<SimplePerson> constructAutoCompleteData() {
        if (faceEngineService != null) {
            List<SimplePerson> result = new ArrayList<>();
            for (String id : faceEngineService.getStore().getPersonIds()) {
                Person person = faceEngineService.getStore().getPerson(id);
                if (person != null) {
                    result.add(SimplePerson.fromPerson(person));
                }
            }
            return result;
        } else {
            return Collections.emptyList();
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
