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
package com.gluonhq.maps.demo;

import com.gluonhq.charm.down.common.JavaFXPlatform;
import com.gluonhq.charm.down.common.PlatformFactory;
import com.gluonhq.charm.down.common.Position;
import com.gluonhq.charm.down.common.PositionService;
import com.gluonhq.maps.MapLayer;
import com.gluonhq.maps.MapPoint;
import com.gluonhq.maps.MapView;
import javafx.application.Application;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

/**
 *
 * Demo class showing a simple map app
 */
public class DemoMap extends Application {

    private PoiLayer poiLayer;
    static {
        try {
            LogManager.getLogManager().readConfiguration( DemoMap.class.getResourceAsStream("/logging.properties") );
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private MapView view;
    @Override
    public void start(Stage stage) throws Exception {
        BorderPane bp = new BorderPane();
        view = new MapView();
        view.addLayer(positionLayer());
        poiLayer = myDemoLayer();
        view.addLayer(poiLayer);
        view.setZoom(14); 
        bp.setCenter(view);
        bp.setTop(new Label ("Gluon Maps Demo"));
        Scene scene;
        if (JavaFXPlatform.isDesktop()) {
            scene = new Scene(bp, 600, 700);
        } else {
            Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
            scene = new Scene (bp, bounds.getWidth(), bounds.getHeight());
        }
        stage.setScene(scene);
        stage.show();
        moveMe();
//        MapPoint moscone = new MapPoint(37.7841772,-122.403751);
//        MapPoint sun = new MapPoint(37.396256,-121.953847);
//        view.setCenter(moscone);
//        view.flyTo(2., sun, 2.);
    }
    
    MapPoint moving = new MapPoint(50.8458,4.724); 
    private PoiLayer myDemoLayer () {
        PoiLayer answer = new PoiLayer();
        InputStream is1 = DemoMap.class.getResourceAsStream("/waypoint3.png");
    //  Node icon1 = new Circle(7, Color.BLUE);
        Image im1 = new Image (is1);
        ImageView icon1 = new ImageView(im1);
        answer.addPoint(new MapPoint(50.8458,4.724), icon1);
        Node icon2 = new Circle(7, Color.GREEN);
        InputStream is2 = DemoMap.class.getResourceAsStream("/airplane3.png");
        answer.addPoint(moving, new ImageView(new Image(is2)));
        return answer;
    }

    private void moveMe() {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    double d = moving.getLatitude();
                    double lon = moving.getLongitude();
                    d = d + Math.random() * .00005;
                    lon = lon + Math.random() * .00005;
                    moving.update(d, lon);
                    poiLayer.markDirty();
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(DemoMap.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        };
        Thread t = new Thread(r);
        t.start();

    }
    
    private MapLayer positionLayer() {
        PoiLayer answer = new PoiLayer();
        PositionService positionService = PlatformFactory.getPlatform().getPositionService();
        System.out.println("POSSERVICE = "+positionService);
        if (positionService != null) {
            ReadOnlyObjectProperty<Position> positionProperty = positionService.positionProperty();
            Position position = positionProperty.get();
            if (position == null) {position = new Position(50.85,4.73);}
            final MapPoint mapPoint = new MapPoint(position.getLatitude(), position.getLongitude());
            InputStream is = DemoMap.class.getResourceAsStream("/compass3.png");
            
            answer.addPoint(mapPoint, new ImageView(new Image(is)));
            view.setCenter(mapPoint);

            positionProperty.addListener(e -> {
                Position pos = positionProperty.get();
                                System.out.println("[JVDBG] NEW POSITION "+pos);
                mapPoint.update(pos.getLatitude(), pos.getLongitude());
            });
        }
        return answer;
    }
}
