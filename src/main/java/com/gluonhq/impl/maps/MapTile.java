/*
 * Copyright (c) 2016, Gluon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL GLUON BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.gluonhq.impl.maps;

import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.WeakInvalidationListener;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.transform.Scale;

import java.util.LinkedList;
import java.util.List;
import java.util.logging.Logger;

import static java.lang.Math.floor;
import java.util.Objects;

/**
 *
 */
class MapTile extends Region {

    private static final Logger logger = Logger.getLogger( MapTile.class.getName() );

    final int myZoom;
    final long i, j, key;
    String host = "http://tile.openstreetmap.org/";
    final BaseMap baseMap;
    // a list of tiles that this tile is covering. In case the covered tiles are 
    // not yet loaded, this tile will be rendered.
    final List<MapTile> coveredTiles = new LinkedList();
    /**
     * In most cases, a tile will be shown scaled. The value for the scale
     * factor depends on the active zoom and the tile-specific myZoom
     */
    final Scale scale = new Scale();
    private boolean dbg = false;

    public boolean isCovering() {
     if (dbg)   System.out.println("[JVDBG] tile "+this+" is covering for "+coveredTiles);
        return coveredTiles.size() > 0;
    }

    private final InvalidationListener zl;
    private final InvalidationListener progressListener;
    private ReadOnlyDoubleProperty progress;

    // final Image image;
    MapTile(BaseMap baseMap, int nearestZoom, long i, long j) {
       if (dbg) System.out.println("[JVDBG] create maptile, address = "+super.toString());
        this.baseMap = baseMap;
        this.myZoom = nearestZoom;
        this.i = i;
        this.j = j;
        int i_max = 1 << nearestZoom;
        this.key = i * i_max + j;

        scale.setPivotX(0);
        scale.setPivotY(0);
        getTransforms().add(scale);
        debug("[JVDBG] load image [" + myZoom + "], i = " + i + ", j = " + j);

        ImageView iv = new ImageView();
        iv.setMouseTransparent(true);
        this.progress = ImageRetriever.fillImage(iv, myZoom, i, j);

        Label l = new Label("Tile [" + myZoom + "], i = " + i + ", j = " + j);
        getChildren().addAll(iv,l);
        zl = recalculate();
        this.progressListener = o -> {
            if (this.progress.get() == 1.) {
                debug("[JVDBG] got image  [" + myZoom + "], i = " + i + ", j = " + j);
                baseMap.storeInCache(myZoom, key, this);
                this.setNeedsLayout(true);
            }
        };
        this.progress.addListener(progressListener);
        baseMap.zoom().addListener(new WeakInvalidationListener(zl));
        baseMap.translateXProperty().addListener(new WeakInvalidationListener(zl));
        baseMap.translateYProperty().addListener(new WeakInvalidationListener(zl));
        calculatePosition();
        this.setMouseTransparent(true);
        debug("[JVDBG] created maptile "+this);
    }

    public boolean loading() {
        return !(progress.greaterThanOrEqualTo(1.)).get();
    }

    /**
     * The immutable zoomlevel for this tile.
     *
     * @return
     */
    int getZoomLevel() {
        return myZoom;
    }

    private InvalidationListener recalculate() {
        return o -> calculatePosition();
    }

    private void calculatePosition() {
        double currentZoom = baseMap.zoom().get();
        int visibleWindow = (int) floor(currentZoom + BaseMap.TIPPING);
        if ((visibleWindow == myZoom) || isCovering() || ((visibleWindow >= BaseMap.MAX_ZOOM) && (myZoom == BaseMap.MAX_ZOOM - 1))) {
            this.setVisible(true);

        } else {
            this.setVisible(false);
        }
        logger.fine("visible tile " + this + "? " + this.isVisible() + (this.isVisible() ? " covering? " + isCovering() : ""));
        double sf = Math.pow(2, currentZoom - myZoom);
        scale.setX(sf);
        scale.setY(sf);
        setTranslateX(256 * i * sf);
        setTranslateY(256 * j * sf);
    }

    @Override
    public String toString() {
        return "tile with z = " + myZoom + " [" + i + "," + j + "]"+ " and address "+super.toString();
    }

    /**
     * This tile is covering for the child tile that is still being loaded.
     *
     * @param child
     */
    void addCovering(MapTile child) {
        coveredTiles.add(child);
        InvalidationListener il = createProgressListener(child);
        debug("[JVDBG] Tile "+this+"has to cover for "+child);
        child.progress.addListener(il);
        calculatePosition();
    }

    InvalidationListener createProgressListener(MapTile child) {
        return new InvalidationListener() {
            @Override
            public void invalidated(Observable o) {
                if (child.progress.get() >= 1.0d) {
                    MapTile.this.coveredTiles.remove(child);
                    debug("[JVDBG] Tile "+child+" is now loaded, remove from coveredTiles for "+MapTile.this);
                    child.progress.removeListener(this);
                    MapTile.this.baseMap.markDirty();
                }
            }
        };
    }

    public void debug(String s) {
if (dbg)        logger.fine("LOG " + System.currentTimeMillis() % 10000 + ": " + s);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + this.myZoom;
        hash = 97 * hash + (int) (this.i ^ (this.i >>> 32));
        hash = 97 * hash + (int) (this.j ^ (this.j >>> 32));
        hash = 97 * hash + Objects.hashCode(this.host);
        hash = 97 * hash + Objects.hashCode(this.baseMap);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MapTile other = (MapTile) obj;
        if (this.myZoom != other.myZoom) {
            return false;
        }
        if (this.i != other.i) {
            return false;
        }
        if (this.j != other.j) {
            return false;
        }
        if (!Objects.equals(this.host, other.host)) {
            return false;
        }
        if (!Objects.equals(this.baseMap, other.baseMap)) {
            return false;
        }
        return true;
    }

    void scheduleRemoval() {
        this.progress.removeListener(progressListener);
    }
    
}
