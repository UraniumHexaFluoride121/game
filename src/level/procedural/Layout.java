package level.procedural;

import foundation.Main;
import level.procedural.marker.LayoutMarker;

import java.util.ArrayList;
import java.util.HashSet;

public class Layout {
    public static final boolean DEBUG_LAYOUT_RENDER = true;

    private final int sectionSize, sectionCount, maxHeight;
    //Markers stored by X and Y pos
    private final HashSet<LayoutMarker>[][] markers;
    //Markers stored by level section for faster lookup in some cases
    private final HashSet<LayoutMarker>[] markerSections;

    public Layout(int maxHeight, int sectionSize, int bufferSections) {
        this.maxHeight = maxHeight;
        this.sectionSize = sectionSize;
        markers = new HashSet[Main.BLOCKS_X][];
        sectionCount = ((int) Math.ceil((double) maxHeight / sectionSize)) + bufferSections;
        markerSections = new HashSet[sectionCount];
        for (int i = 0; i < sectionCount; i++) {
            markerSections[i] = new HashSet<>();
        }
    }

    public void generateMarkers() {
        ArrayList<LayoutMarker> markers = new ArrayList<>();
        for (HashSet<LayoutMarker> markerSection : markerSections) {
            markers.addAll(markerSection);
        }
        markers.forEach(LayoutMarker::generate);
    }

    public void addMarker(LayoutMarker marker) {
        int xPos = ((int) marker.pos.x);
        int yPos = ((int) marker.pos.y);
        if (markers[xPos] == null)
            markers[xPos] = new HashSet[maxHeight];
        if (markers[xPos][yPos] == null)
            markers[xPos][yPos] = new HashSet<>();
        markers[xPos][yPos].add(marker);
        markerSections[yPosToSection(yPos)].add(marker);
    }

    public void removeMarker(LayoutMarker marker) {
        int xPos = ((int) marker.pos.x);
        int yPos = ((int) marker.pos.y);
        if (markers[xPos] == null)
            markers[xPos] = new HashSet[maxHeight];
        if (markers[xPos][yPos] == null)
            markers[xPos][yPos] = new HashSet<>();
        markers[xPos][yPos].remove(marker);
        markerSections[yPosToSection(yPos)].remove(marker);
    }

    private int yPosToSection(float y) {
        return ((int) (y / sectionSize));
    }
}
