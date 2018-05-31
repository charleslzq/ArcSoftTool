package com.github.charleslzq.face.baidu.sample;

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
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.bin.david.form.core.SmartTable;
import com.bin.david.form.data.column.Column;
import com.bin.david.form.data.table.PageTableData;
import com.github.charleslzq.face.baidu.BaiduFaceEngineService;
import com.github.charleslzq.face.baidu.BaiduFaceEngineServiceBackground;
import com.github.charleslzq.face.baidu.BaiduUserApi;
import com.github.charleslzq.face.baidu.CoroutineSupport;
import com.github.charleslzq.face.baidu.data.BaiduResponse;
import com.github.charleslzq.face.baidu.data.FaceListItem;
import com.github.charleslzq.face.baidu.data.FaceListResult;
import com.github.charleslzq.face.baidu.data.GroupIdList;
import com.github.charleslzq.face.baidu.data.Image;
import com.github.charleslzq.face.baidu.data.LivenessControl;
import com.github.charleslzq.face.baidu.data.QualityControl;
import com.github.charleslzq.face.baidu.data.UserIdList;
import com.github.charleslzq.faceengine.support.ImageUtils;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import kotlinx.coroutines.experimental.Deferred;

public class MainActivity extends AppCompatActivity {
    private static final String BAIDU_ID_REGEX = "[a-zA-Z0-9_]+";
    @BindView(R.id.userInfoTable)
    SmartTable userInfoTable;
    @BindView(R.id.baiduServerUrl)
    EditText baiduServerUrl;
    @BindView(R.id.refreshButton)
    Button refreshButton;
    private BaiduFaceEngineService faceEngineService = null;
    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            faceEngineService = (BaiduFaceEngineService) iBinder;
            baiduServerUrl.setText(faceEngineService.getUrl());
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            faceEngineService = null;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        userInfoTable.getConfig().setShowXSequence(false);
        userInfoTable.getConfig().setShowYSequence(false);

        bindService(new Intent(this, BaiduFaceEngineServiceBackground.class), serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK && data != null && faceEngineService != null) {
            switch (RequestCodes.fromCode(requestCode)) {
                case FACE_CHECK:
                    String groupId = data.getExtras().getString("groupId");
                    String userId = data.getExtras().getString("userId");
                    String userInfo = data.getExtras().getString("userInfo");
                    StringBuilder sb = new StringBuilder();
                    sb.append("Found Person with Group ID ");
                    sb.append(groupId);
                    sb.append(" and User ID ");
                    sb.append(userId);
                    sb.append(", User Info ");
                    sb.append(userInfo);
                    toast(sb.toString());
                    break;
                case IMAGE_CAMERA:
                    final Bitmap image = (Bitmap) data.getExtras().get("data");
                    if (image != null) {
                        final List<String> groupIdList = getGroupIdListFromTable();
                        if (groupIdList.isEmpty()) {
                            toast("There is no group");
                        } else {
                            View dialogLayout = getLayoutInflater().inflate(R.layout.dialog_user_add, null);

                            final AutoCompleteTextView groupIdInput = dialogLayout.findViewById(R.id.groupIdText);
                            groupIdInput.setAdapter(getArrayAdapter(groupIdList));
                            groupIdInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                                @Override
                                public void onFocusChange(View v, boolean hasFocus) {
                                    if (!hasFocus && !groupIdList.contains(groupIdInput.getText().toString())) {
                                        groupIdInput.setError("Group Id Does not Exist");
                                    }
                                }
                            });

                            final AutoCompleteTextView userIdInput = dialogLayout.findViewById(R.id.userIdText);
                            userIdInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                                @Override
                                public void onFocusChange(View v, boolean hasFocus) {
                                    if (hasFocus && groupIdList.contains(groupIdInput.getText().toString())) {
                                        userIdInput.setAdapter(getArrayAdapter(getUserIdListFromTable(groupIdInput.getText().toString())));
                                    }
                                }
                            });

                            final EditText userInfoInput = dialogLayout.findViewById(R.id.userInfoText);
                            final AlertDialog dialog = new AlertDialog.Builder(this)
                                    .setView(dialogLayout)
                                    .setTitle("Input the Information")
                                    .setPositiveButton("OK", null)
                                    .setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.dismiss();
                                        }
                                    })
                                    .show();
                            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    String groupId = groupIdInput.getText().toString();
                                    String userId = userIdInput.getText().toString();
                                    String userInfo = userInfoInput.getText().toString();
                                    blockingGet(faceEngineService.addUser(groupId, new BaiduUserApi.RegisterImage(
                                            new Image(Image.Type.BASE64, ImageUtils.toEncodedBytes(image)),
                                            userId, userInfo
                                    ), QualityControl.NONE, LivenessControl.NONE));
                                    refresh();
                                    dialog.dismiss();
                                }
                            });

                        }
                    }
                    break;
            }
        }
    }

    @OnClick(R.id.addUserButton)
    public void onAddUserButtonClicked() {
        startActivityForResult(
                new Intent(MediaStore.ACTION_IMAGE_CAPTURE),
                RequestCodes.IMAGE_CAMERA.getCode()
        );
    }

    @OnClick(R.id.imageSearchButton)
    public void onImageSearchButtonClicked() {
        startActivityForResult(
                new Intent(this, FaceDetectActivity.class),
                RequestCodes.FACE_CHECK.getCode()
        );
    }

    @OnClick(R.id.refreshButton)
    public void refresh() {
        if (faceEngineService != null) {
            faceEngineService.setUrlWithCallback(baiduServerUrl.getText().toString(), new Runnable() {
                @Override
                public void run() {
                    List<TableItem> dataList = new ArrayList<>();
                    BaiduResponse<GroupIdList> groupResponse = blockingGet(faceEngineService.listGroup(0, 100));
                    if (groupResponse.getResult() != null) {
                        List<String> groupIdList = groupResponse.getResult().getGroupIdList();
                        faceEngineService.getEngine().getDefaultSearchGroups().clear();
                        faceEngineService.getEngine().getDefaultSearchGroups().addAll(groupIdList);
                        for (String groupId : groupIdList) {
                            UserIdList userIdList = blockingGet(faceEngineService.listUser(groupId, 0, 100)).getResult();
                            if (userIdList == null || userIdList.getUserIdList().isEmpty()) {
                                dataList.add(new TableItem(groupId, "", "", ""));
                            } else {
                                for (String userId : userIdList.getUserIdList()) {
                                    FaceListResult faceListResult = blockingGet(faceEngineService.listFace(groupId, userId)).getResult();
                                    if (faceListResult == null || faceListResult.getFaceList().isEmpty()) {
                                        dataList.add(new TableItem(groupId, userId, "", ""));
                                    } else {
                                        for (FaceListItem faceListItem : faceListResult.getFaceList()) {
                                            dataList.add(new TableItem(groupId, userId, faceListItem.getFaceToken(), faceListItem.getCreateTime()));
                                        }
                                    }
                                }
                            }
                        }
                    }
                    userInfoTable.setTableData(new PageTableData(
                            "Registered Faces",
                            dataList,
                            TableColumn.getColumns()
                    ));
                }
            });
        }
    }

    private ArrayAdapter<String> getArrayAdapter(List<String> idList) {
        return new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                idList
        );
    }

    private List<String> getGroupIdListFromTable() {
        List<String> result = new ArrayList<>();
        if (userInfoTable.getTableData() != null) {
            for (TableItem item : ((PageTableData<TableItem>) userInfoTable.getTableData()).getT()) {
                result.add(item.groupId);
            }
        }
        return result;
    }

    private List<String> getUserIdListFromTable(String groupId) {
        List<String> result = new ArrayList<>();
        if (userInfoTable.getTableData() != null) {
            for (TableItem item : ((PageTableData<TableItem>) userInfoTable.getTableData()).getT()) {
                if (groupId == null || item.groupId == groupId) {
                    result.add(item.userId);
                }
            }
        }
        return result;
    }

    private <T> T blockingGet(Deferred<T> result) {
        return CoroutineSupport.blockingGet(result);
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    enum TableColumn {
        GROUP_ID("group", "groupId"),
        USER_ID("userId", "userId"),
        FACE_TOKEN("faceToken", "faceToken"),
        CREATE_TIME("createTime", "createTime");

        private final String title;
        private final String field;

        TableColumn(String title, String field) {
            this.title = title;
            this.field = field;
        }

        public static List<Column<String>> getColumns() {
            List<Column<String>> columns = new ArrayList<>();
            for (TableColumn column : TableColumn.values()) {
                columns.add(column.getColumnSetting());
            }
            return columns;
        }

        public Column<String> getColumnSetting() {
            Column<String> setting = new Column<String>(title, field);
            setting.setAutoMerge(true);
            return setting;
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

    static class TableItem {
        final String groupId;
        final String userId;
        final String faceToken;
        final String createTime;

        TableItem(String groupId, String userId, String faceToken, String createTime) {
            this.groupId = groupId;
            this.userId = userId;
            this.faceToken = faceToken;
            this.createTime = createTime;
        }
    }
}
