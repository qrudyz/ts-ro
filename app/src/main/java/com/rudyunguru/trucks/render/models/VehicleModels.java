package com.rudyunguru.trucks.render.models;

import com.rudyunguru.trucks.render.MeshData;
import com.rudyunguru.trucks.render.math.Mat4;

/**
 * Every vehicle in the game is built here, in code. No mesh, texture or badge is downloaded or
 * converted from another game - the shapes are original procedural work.
 */
public final class VehicleModels {

    private VehicleModels() {}

    private static final float WHEEL_RADIUS = 0.52f;
    private static final float WHEEL_WIDTH = 0.32f;
    private static final float HALF_WIDTH = 1.18f;

    public enum Cab {
        LIGHT_DAY_CAB, MID_SLEEPER, ACTROS_SLEEPER, SWAY_TALL, TGX_FLAT,
        SCANIA_R_HIGH, VOLVO_FH_AERO, RENAULT_T_ROUND, SCANIA_S_FLAT_LUXURY, HEAVY_HAULER
    }

    public static Cab cabForVariant(int variant) {
        Cab[] cabs = Cab.values();
        return cabs[Math.floorMod(variant, cabs.length)];
    }

    public static MeshData truck(int variant, float[] paint, boolean detailed) {
        MeshData mesh = new MeshData();
        Cab cab = cabForVariant(variant);
        float pr = paint[0], pg = paint[1], pb = paint[2];
        float dark = 0.10f;
        float chassisY = 0.95f;

        // Chassis rails
        float chassisLength = cab == Cab.LIGHT_DAY_CAB ? 5.6f : 6.1f;
        mesh.addBox(0f, chassisY - 0.20f, 0.2f, 1.05f, 0.24f, chassisLength, 0.16f, 0.17f, 0.19f, MeshData.FLAG_MATTE);
        mesh.addBox(-1.0f, chassisY - 0.20f, 0.2f, 0.14f, 0.30f, chassisLength, 0.13f, 0.14f, 0.16f, MeshData.FLAG_MATTE);
        mesh.addBox(1.0f, chassisY - 0.20f, 0.2f, 0.14f, 0.30f, chassisLength, 0.13f, 0.14f, 0.16f, MeshData.FLAG_MATTE);

        // Cab
        float cabWidth = 2.42f;
        float cabLength = cab == Cab.LIGHT_DAY_CAB ? 2.05f : 2.45f;
        float cabHeight = switch (cab) {
            case LIGHT_DAY_CAB -> 1.55f;
            case MID_SLEEPER, ACTROS_SLEEPER -> 2.05f;
            case SWAY_TALL, TGX_FLAT, RENAULT_T_ROUND -> 2.25f;
            case SCANIA_R_HIGH, VOLVO_FH_AERO -> 2.35f;
            case SCANIA_S_FLAT_LUXURY, HEAVY_HAULER -> 2.55f;
        };
        float cabZ = cab == Cab.LIGHT_DAY_CAB ? 1.75f : 1.95f;
        float cabBase = chassisY + 0.15f;
        mesh.addBox(0f, cabBase + cabHeight / 2f, cabZ, cabWidth, cabHeight, cabLength, pr, pg, pb, MeshData.FLAG_MATTE);

        // Windows
        mesh.addBox(0f, cabBase + cabHeight * 0.72f, cabZ + cabLength / 2f + 0.01f, cabWidth * 0.9f, cabHeight * 0.34f, 0.06f, 0.08f, 0.12f, 0.16f, MeshData.FLAG_GLASS);
        mesh.addBox(cabWidth / 2f + 0.01f, cabBase + cabHeight * 0.72f, cabZ - cabLength * 0.05f, 0.06f, cabHeight * 0.28f, cabLength * 0.55f, 0.08f, 0.12f, 0.16f, MeshData.FLAG_GLASS);
        mesh.addBox(-cabWidth / 2f - 0.01f, cabBase + cabHeight * 0.72f, cabZ - cabLength * 0.05f, 0.06f, cabHeight * 0.28f, cabLength * 0.55f, 0.08f, 0.12f, 0.16f, MeshData.FLAG_GLASS);

        // Front details
        float frontZ = cabZ + cabLength / 2f;
        mesh.addBox(0f, chassisY - 0.05f, frontZ + 0.06f, cabWidth, 0.55f, 0.22f, pr * 0.85f, pg * 0.85f, pb * 0.85f, MeshData.FLAG_MATTE);
        mesh.addBox(0f, cabBase + cabHeight * 0.30f, frontZ + 0.04f, cabWidth * 0.86f, cabHeight * 0.34f, 0.10f, dark, dark, dark, MeshData.FLAG_METAL);
        mesh.addBox(cabWidth * 0.34f, chassisY + 0.05f, frontZ + 0.10f, 0.42f, 0.22f, 0.10f, 1f, 0.96f, 0.86f, MeshData.FLAG_EMISSIVE);
        mesh.addBox(-cabWidth * 0.34f, chassisY + 0.05f, frontZ + 0.10f, 0.42f, 0.22f, 0.10f, 1f, 0.96f, 0.86f, MeshData.FLAG_EMISSIVE);

        // Wheels
        addTruckWheels(mesh, cab);

        // Accessories
        mesh.addBox(0f, chassisY + 0.12f, -1.2f, 0.85f, 0.12f, 0.85f, 0.45f, 0.46f, 0.48f, MeshData.FLAG_METAL); // Fifth wheel

        // Exhaust stacks (rear)
        if (cab == Cab.ACTROS_SLEEPER || cab == Cab.RENAULT_T_ROUND || cab == Cab.TGX_FLAT) {
            float stackX = cabWidth / 2f + 0.15f;
            float stackZ = -0.4f;
            mesh.addBox(stackX, chassisY + 0.05f, stackZ, 0.06f, 0.25f, 0.06f, 0.08f, 0.08f, 0.08f, MeshData.FLAG_METAL);
            mesh.addBox(-stackX, chassisY + 0.05f, stackZ, 0.06f, 0.25f, 0.06f, 0.08f, 0.08f, 0.08f, MeshData.FLAG_METAL);
        }

        // Fuel tanks under chassis
        mesh.addBox(-cabWidth / 2f - 0.05f, chassisY - 0.15f, -0.8f, 0.20f, 0.28f, 0.6f,
                0.12f, 0.12f, 0.12f, MeshData.FLAG_METAL);
        mesh.addBox(cabWidth / 2f + 0.05f, chassisY - 0.15f, -0.8f, 0.20f, 0.28f, 0.6f,
                0.12f, 0.12f, 0.12f, MeshData.FLAG_METAL);

        // Air deflector (sides) on long-haul cabs
        if (cab == Cab.ACTROS_SLEEPER || cab == Cab.SCANIA_R_HIGH || cab == Cab.VOLVO_FH_AERO || cab == Cab.RENAULT_T_ROUND) {
            float defZ = cabZ - cabLength / 2f - 0.3f;
            float defH = cabHeight * 0.65f;
            mesh.addBox(-cabWidth / 2f - 0.12f, cabBase + cabHeight * 0.5f, defZ, 0.12f, defH, 0.2f,
                    pr * 0.8f, pg * 0.8f, pb * 0.8f, MeshData.FLAG_MATTE);
            mesh.addBox(cabWidth / 2f + 0.12f, cabBase + cabHeight * 0.5f, defZ, 0.12f, defH, 0.2f,
                    pr * 0.8f, pg * 0.8f, pb * 0.8f, MeshData.FLAG_MATTE);
        }

        return mesh;
    }

    private static void addTruckWheels(MeshData mesh, Cab cab) {
        float frontZ = 2.85f;
        float rearZ = -1.05f;
        mesh.addWheel(HALF_WIDTH, WHEEL_RADIUS, frontZ, WHEEL_RADIUS, WHEEL_WIDTH, 0.1f, 0.1f, 0.1f);
        mesh.addWheel(-HALF_WIDTH, WHEEL_RADIUS, frontZ, WHEEL_RADIUS, WHEEL_WIDTH, 0.1f, 0.1f, 0.1f);
        
        mesh.addWheel(HALF_WIDTH, WHEEL_RADIUS, rearZ, WHEEL_RADIUS, WHEEL_WIDTH, 0.1f, 0.1f, 0.1f);
        mesh.addWheel(-HALF_WIDTH, WHEEL_RADIUS, rearZ, WHEEL_RADIUS, WHEEL_WIDTH, 0.1f, 0.1f, 0.1f);
        
        if (cab == Cab.HEAVY_HAULER || cab == Cab.SCANIA_S_FLAT_LUXURY) {
            float midZ = -0.05f;
            mesh.addWheel(HALF_WIDTH, WHEEL_RADIUS, midZ, WHEEL_RADIUS, WHEEL_WIDTH, 0.1f, 0.1f, 0.1f);
            mesh.addWheel(-HALF_WIDTH, WHEEL_RADIUS, midZ, WHEEL_RADIUS, WHEEL_WIDTH, 0.1f, 0.1f, 0.1f);
        }
    }

    public static MeshData trailer(int variant) {
        MeshData mesh = new MeshData();
        float r = 0.85f, g = 0.85f, b = 0.85f;
        float length = 13.6f;
        float width = 2.55f;
        float height = 2.7f;
        float base = 1.15f;

        // Main body
        mesh.addBox(0f, base + height / 2f, -length / 2f + 1.0f, width, height, length, r, g, b, MeshData.FLAG_MATTE);
        
        // Chassis
        mesh.addBox(0f, base - 0.15f, -length / 2f + 1.0f, width * 0.95f, 0.25f, length, 0.15f, 0.15f, 0.15f, MeshData.FLAG_MATTE);

        // Axles & Wheels
        float rearZ = -length + 2.5f;
        for (int i = 0; i < 3; i++) {
            float z = rearZ + i * 1.35f;
            mesh.addWheel(HALF_WIDTH, WHEEL_RADIUS, z, WHEEL_RADIUS, WHEEL_WIDTH, 0.1f, 0.1f, 0.1f);
            mesh.addWheel(-HALF_WIDTH, WHEEL_RADIUS, z, WHEEL_RADIUS, WHEEL_WIDTH, 0.1f, 0.1f, 0.1f);
        }

        return mesh;
    }
}
