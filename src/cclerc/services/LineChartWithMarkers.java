package cclerc.services;

import javafx.beans.InvalidationListener;
import javafx.beans.property.ObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.chart.Axis;
import javafx.scene.chart.LineChart;
import javafx.scene.shape.Line;

import java.util.Objects;
import java.util.Observable;

public class LineChartWithMarkers<X,Y> extends LineChart {

    private ObservableList<Data<X, Y>> horizontalMarkers;
    private ObservableList<Data<X, Y>> verticalMarkers;

    public LineChartWithMarkers(Axis<X> xAxis, Axis<Y> yAxis) {
        super(xAxis, yAxis);
        horizontalMarkers = FXCollections.observableArrayList(data -> new ObjectProperty[] {data.YValueProperty()});
        horizontalMarkers.addListener((InvalidationListener) observable -> layoutPlotChildren());
        verticalMarkers = FXCollections.observableArrayList(data -> new ObjectProperty[] {data.XValueProperty()});
        verticalMarkers.addListener((InvalidationListener)observable -> layoutPlotChildren());
    }

    public Data<X, Y> addHorizontalValueMarker(Data<X, Y> marker) {
        return addHorizontalValueMarker(marker, "");
    }

    public Data<X, Y> addHorizontalValueMarker(Data<X, Y> marker, String id) {
        Objects.requireNonNull(marker, "the marker must not be null");
        if (horizontalMarkers.contains(marker)) return marker;
        Line line = new Line();
        line.getStyleClass().add(id);
        marker.setNode(line );
        getPlotChildren().add(line);
        horizontalMarkers.add(marker);
        return marker;
    }

    public void removeHorizontalValueMarker(Data<X, Y> marker) {
        Objects.requireNonNull(marker, "the marker must not be null");
        if (marker.getNode() != null) {
            getPlotChildren().remove(marker.getNode());
            marker.setNode(null);
        }
        horizontalMarkers.remove(marker);
    }

    public Data<X, Y> addVerticalValueMarker(Data<X, Y> marker) {
        return addVerticalValueMarker(marker, "");
    }

    public Data<X, Y> addVerticalValueMarker(Data<X, Y> marker, String id) {
        Objects.requireNonNull(marker, "the marker must not be null");
        if (verticalMarkers.contains(marker)) return marker;
        Line line = new Line();
        line.getStyleClass().add(id);
        marker.setNode(line );
        getPlotChildren().add(line);
        verticalMarkers.add(marker);
        return marker;
    }

    public void removeVerticalValueMarker(Data<X, Y> marker) {
        Objects.requireNonNull(marker, "the marker must not be null");
        if (marker.getNode() != null) {
            getPlotChildren().remove(marker.getNode());
            marker.setNode(null);
        }
        verticalMarkers.remove(marker);
    }

    public ObservableList<Data<X, Y>> getHorizontalMarkers() {
        return horizontalMarkers;
    }

    public ObservableList<Data<X, Y>> getVerticalMarkers() {
        return verticalMarkers;
    }

    @Override
    protected void layoutPlotChildren() {
        super.layoutPlotChildren();
        for (Data<X, Y> horizontalMarker : horizontalMarkers) {
            Line line = (Line) horizontalMarker.getNode();
            line.setStartX(0);
            line.setEndX(getBoundsInLocal().getWidth());
            line.setStartY(getYAxis().getDisplayPosition(horizontalMarker.getYValue()) + 0.5); // 0.5 for crispness
            line.setEndY(line.getStartY());
            line.toFront();
        }
        for (Data<X, Y> verticalMarker : verticalMarkers) {
            Line line = (Line) verticalMarker.getNode();
            line.setStartX(getXAxis().getDisplayPosition(verticalMarker.getXValue()) + 0.5);  // 0.5 for crispness
            line.setEndX(line.getStartX());
            line.setStartY(0d);
            line.setEndY(getBoundsInLocal().getHeight());
            line.toFront();
        }
    }

}