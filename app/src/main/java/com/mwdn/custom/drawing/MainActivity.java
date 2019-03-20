package com.mwdn.custom.drawing;

import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatSpinner;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;

import com.mwdn.custom.drawing.adapter.LayersAdapter;
import com.mwdn.custom.drawing.adapter.LayersAdapter.LayersAdapterCallback;
import com.mwdn.custom.drawing.view.DrawingView;
import com.mwdn.custom.drawing.view.DrawingView.FillingType;
import com.mwdn.custom.drawing.view.DrawingView.Layer;
import com.mwdn.custom.drawing.view.DrawingView.MatrixType;
import com.mwdn.custom.drawing.view.DrawingView.TransformType;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

public class MainActivity extends AppCompatActivity implements LayersAdapterCallback,
        OnItemSelectedListener, OnCheckedChangeListener {

    @BindView(R.id.am_layers)
    RecyclerView layersList;
    @BindView(R.id.am_drawing_view)
    DrawingView drawingView;
    @BindView(R.id.am_matrix_group)
    RadioGroup matrixGroup;
    @BindView(R.id.am_transform_group)
    RadioGroup transformGroup;
    @BindView(R.id.am_paint_style)
    RadioGroup styleGroup;
    @BindView(R.id.am_layer_mode)
    AppCompatSpinner modeSpinner;

    private Unbinder unbinder;
    private LayersAdapter adapter;
    private List<String> layerModes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        unbinder = ButterKnife.bind(this);
        initList();
        initSpinner();
        initRadioGroups();
        refreshLayersList();
    }

    @Override
    protected void onDestroy() {
        unbinder.unbind();
        super.onDestroy();
    }

    @Override
    public void onLayerChosen(Layer layer) {
        drawingView.setCurrentLayer(layer);
        switch (layer.getTransformType()) {
            case TransformType.ROTATE:
                transformGroup.check(R.id.am_transform_rotate);
                break;
            case TransformType.SKEW:
                transformGroup.check(R.id.am_transform_skew);
                break;
            case TransformType.TRANSLATE:
                transformGroup.check(R.id.am_transform_translate);
                break;
        }
        switch (layer.getMatrixType()) {
            case MatrixType.LAYER:
                matrixGroup.check(R.id.am_view_matrix);
                break;
            case MatrixType.PAINT:
                matrixGroup.check(R.id.am_paint_matrix);
                break;
            case MatrixType.PATH:
                matrixGroup.check(R.id.am_path_matrix);
                break;
            case MatrixType.SHADER:
                matrixGroup.check(R.id.am_shader_matrix);
                break;
        }

        if (layer.getPaintStyle() == Paint.Style.STROKE) {
            styleGroup.check(R.id.am_style_stroke);
        } else {
            styleGroup.check(R.id.am_style_fill);
        }

        PorterDuff.Mode mode = layer.getLayerMode();
        if (mode == null) {
            modeSpinner.setSelection(0);
        } else {
            modeSpinner.setSelection(mode.ordinal() + 1);
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        String item = (String) modeSpinner.getItemAtPosition(position);
        if ("None".equals(item)) {
            drawingView.removeLayerMode();
            refreshLayersList();
        } else {
            PorterDuff.Mode mode = PorterDuff.Mode.valueOf(item);
            drawingView.setLayerMode(mode);
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        switch (group.getId()) {
            case R.id.am_transform_group:
                setTransformType(checkedId);
                break;
            case R.id.am_matrix_group:
                setMatrixType(checkedId);
                break;
            case R.id.am_paint_style:
                drawingView.setPaintStyle(checkedId == R.id.am_style_stroke ? Paint.Style.STROKE : Paint.Style.FILL);
                break;
        }
    }

    @OnClick(R.id.am_add_layer)
    void onAddLayerClicked() {
        Layer newLayer = drawingView.addNewLayer();
        adapter.addLayer(newLayer);
    }

    @OnClick(R.id.am_remove_layer)
    void onRemoveLayerClicked() {
        Layer layer = adapter.removeSelectedLayer();
        drawingView.removeLayer(layer);
    }

    @OnClick({R.id.am_stroke_add, R.id.am_stroke_minus})
    void onStrokeWidthUpdated(View view) {
        if (view.getId() == R.id.am_stroke_add) {
            drawingView.increaseStroke();
        } else {
            drawingView.decreaseStroke();
        }
    }

    @OnClick({R.id.am_paint_color, R.id.am_paint_shader, R.id.am_paint_image})
    void onPaintTypeUpdated(View view) {
        @FillingType int type;
        switch (view.getId()) {
            case R.id.am_paint_shader:
                type = FillingType.SHADER;
                break;
            case R.id.am_paint_image:
                type = FillingType.IMAGE;
                break;
            case R.id.am_paint_color:
            default:
                type = FillingType.COLOR;
                break;
        }

        drawingView.setFillingType(type);
    }

    private void setTransformType(int checkedId) {
        @TransformType int type;
        switch (checkedId) {
            case R.id.am_transform_skew:
                type = TransformType.SKEW;
                break;
            case R.id.am_transform_rotate:
                type = TransformType.ROTATE;
                break;
            case R.id.am_transform_translate:
            default:
                type = TransformType.TRANSLATE;
                break;
        }
        drawingView.setMatrixTransform(type);
    }

    private void setMatrixType(int checkedId) {
        @MatrixType int matrixType;
        switch (checkedId) {
            case R.id.am_shader_matrix:
                matrixType = MatrixType.SHADER;
                break;
            case R.id.am_path_matrix:
                matrixType = MatrixType.PATH;
                break;
            case R.id.am_paint_matrix:
                matrixType = MatrixType.PAINT;
                break;
            case R.id.am_view_matrix:
            default:
                matrixType = MatrixType.LAYER;
                break;
        }
        drawingView.setMatrixType(matrixType);
    }

    private void refreshLayersList() {
        adapter.setLayers(drawingView.getLayers());
    }

    private void initSpinner() {
        layerModes.add("None");
        for (PorterDuff.Mode mode : PorterDuff.Mode.values()) {
            layerModes.add(mode.toString());
        }


        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, layerModes);
        modeSpinner.setAdapter(spinnerAdapter);
        modeSpinner.setOnItemSelectedListener(this);
    }

    private void initList() {
        if (adapter == null) {
            adapter = new LayersAdapter();
        }

        adapter.setCallback(this);
        layersList.setLayoutManager(new LinearLayoutManager(this));
        layersList.setAdapter(adapter);
    }

    private void initRadioGroups() {
        matrixGroup.setOnCheckedChangeListener(this);
        styleGroup.setOnCheckedChangeListener(this);
        transformGroup.setOnCheckedChangeListener(this);
    }
}
