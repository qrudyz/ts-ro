// ---- Truck: red painted cab + chrome grille + trailer (procedural, like VehicleModels) ----
const TRUCK={x:A1[0][0],z:A1[0][1],yaw:0,v:0};
function buildTruck(){
 const paint=[0.85,0.12,0.12];
 box(TRUCK.x,0,TRUCK.z, 3.2,3.4,8.5, paint, TRUCK.yaw);          // cabin
 box(TRUCK.x,2.6,TRUCK.z+2.6, 2.6,1.2,0.4,[0.3,0.55,0.85],TRUCK.yaw); // windshield
 box(TRUCK.x,0,TRUCK.z-6.5, 2.6,1.2,10,[0.15,0.15,0.18],TRUCK.yaw);   // chassis
 box(TRUCK.x,0,TRUCK.z-18, 3.4,4.2,13,[0.92,0.92,0.95],TRUCK.yaw);    // trailer
 for(const o of[[-1.5,4],[1.5,4],[-1.5,-1],[1.5,-1],[-1.5,-24],[1.5,-24],[-1.5,-27],[1.5,-27]])
  box(TRUCK.x+o[0],0,TRUCK.z+o[1],0.9,1.8,1.8,[0.08,0.08,0.08],TRUCK.yaw); // wheels
}
buildTruck();
gl.bindBuffer(gl.ARRAY_BUFFER,bP);gl.bufferData(gl.ARRAY_BUFFER,new Float32Array(V),gl.STATIC_DRAW);
gl.bindBuffer(gl.ARRAY_BUFFER,bN);gl.bufferData(gl.ARRAY_BUFFER,new Float32Array(N),gl.STATIC_DRAW);
gl.bindBuffer(gl.ARRAY_BUFFER,bC);gl.bufferData(gl.ARRAY_BUFFER,new Float32Array(C),gl.STATIC_DRAW);
const NV=V.length/3;
