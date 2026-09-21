package com.rudyunguru.trucks.render;

import com.rudyunguru.trucks.core.model.WorldGeometry;
import com.rudyunguru.trucks.core.model.RoadStrip;
import com.rudyunguru.trucks.core.model.BuildingBox;
import com.rudyunguru.trucks.core.model.PropInstance;
import com.rudyunguru.trucks.core.model.GroundPatch;

/**
 * Turns WorldGeometry into a displayable MeshData.
 */
public final class WorldMeshFactory {

    public static MeshData build(WorldGeometry geo) {
        MeshData mesh = new MeshData();

        // Terrain / Ground
        for (GroundPatch gp : geo.getGround()) {
            float[] rgb = MeshData.rgb(gp.getColorHex().isEmpty() ? "#2D5A27" : gp.getColorHex(), new float[3]);
            mesh.addBoxRotY((float)gp.getX(), 0f, (float)gp.getZ(), (float)gp.getWidth(), 0.1f, (float)gp.getDepth(),
                    (float)gp.getRotationRad(), rgb[0], rgb[1], rgb[2], MeshData.FLAG_MATTE);
        }

        // Roads
        for (RoadStrip rs : geo.getRoads()) {
            mesh.addRoadStrip(rs.getPoints(), rs.pointCount(), (float)rs.getWidthMetres(), 0.05f, 
                    0.2f, 0.2f, 0.22f, MeshData.FLAG_MATTE);
            // Center line
            mesh.addRoadMarkings(rs.getPoints(), rs.pointCount(), 0f, 0.15f, 0.06f, 3f, 3f, 1f, 1f, 1f);
        }

        // Buildings
        for (BuildingBox bb : geo.getBuildings()) {
            float[] rgb = MeshData.rgb(bb.getColorHex(), new float[3]);
            mesh.addBoxRotY((float)bb.getX(), (float)bb.getHeight() / 2f, (float)bb.getZ(), 
                    (float)bb.getWidth(), (float)bb.getHeight(), (float)bb.getDepth(), 
                    (float)bb.getRotationRad(), rgb[0], rgb[1], rgb[2], MeshData.FLAG_MATTE);
        }

        // Props
        for (PropInstance p : geo.getProps()) {
            float[] rgb = MeshData.rgb(p.getColorHex(), new float[3]);
            mesh.addCylinderY((float)p.getX(), (float)p.getY(), (float)p.getZ(), (float)p.getScale() * 0.5f, 
                    (float)p.getScale() * 2f, 8, rgb[0], rgb[1], rgb[2], MeshData.FLAG_MATTE);
        }

        return mesh;
    }
}
