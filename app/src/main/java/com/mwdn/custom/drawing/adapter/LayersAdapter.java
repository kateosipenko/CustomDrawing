package com.mwdn.custom.drawing.adapter;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mwdn.custom.drawing.R;
import com.mwdn.custom.drawing.view.DrawingView.Layer;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class LayersAdapter extends RecyclerView.Adapter<LayersAdapter.LayerHolder> {

    private int selectedPosition = 0;
    private List<Layer> layers = new ArrayList<>();

    private LayersAdapterCallback callback;

    public void setCallback(LayersAdapterCallback callback) {
        this.callback = callback;
    }

    public void setLayers(List<Layer> layers) {
        this.layers.clear();
        if (layers != null) {
            this.layers.addAll(layers);
        }

        notifyDataSetChanged();
    }

    public void addLayer(Layer newLayer) {
        this.layers.add(newLayer);
        notifyItemInserted(this.layers.size() - 1);
        setSelectedPosition(this.layers.size() - 1);
    }

    public Layer removeSelectedLayer() {
        Layer selected = getItemByPosition(selectedPosition);
        if (selectedPosition >= 0 && selectedPosition < this.layers.size()) {
            this.layers.remove(selectedPosition);
            notifyItemRemoved(selectedPosition);

        }

        return selected;
    }

    @NonNull
    @Override
    public LayerHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.row_layer, viewGroup, false);
        return new LayerHolder(view, this);
    }

    @Override
    public void onBindViewHolder(@NonNull LayerHolder layerHolder, int i) {
        layerHolder.update(getItemByPosition(i));
    }

    @Override
    public int getItemCount() {
        return layers.size();
    }

    private Layer getItemByPosition(int position) {
        return position >= 0 && position < layers.size() ? layers.get(position) : null;
    }

    private void setSelectedPosition(int position) {
        int old = selectedPosition;
        selectedPosition = position;
        notifyItemChanged(old);
        notifyItemChanged(selectedPosition);
    }

    static class LayerHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.rl_title)
        TextView titleView;

        private LayersAdapter adapter;

        LayerHolder(@NonNull View itemView, LayersAdapter adapter) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            this.adapter = adapter;
        }

        @OnClick(R.id.rl_title)
        void onLayerClicked() {
            Layer layer = adapter.getItemByPosition(getAdapterPosition());
            if (layer != null && adapter.callback != null) {
                adapter.setSelectedPosition(getAdapterPosition());
                adapter.callback.onLayerChosen(layer);
            }
        }

        private void update(@Nullable Layer layer) {
            if (layer == null) {
                return;
            }

            titleView.setText(String.format("Layer %1$s", layer.getLayerId()));
            titleView.setActivated(getAdapterPosition() == adapter.selectedPosition);
        }
    }

    public interface LayersAdapterCallback {
        void onLayerChosen(Layer layer);
    }
}
