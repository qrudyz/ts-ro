// ---- camera + input ----
let camYaw=0.6,camDist=34,camH=13,drag=false,lx=0;
cv.onpointerdown=e=>{drag=true;lx=e.clientX};
cv.onpointermove=e=>{if(drag){camYaw+=(e.clientX-lx)*0.01;lx=e.clientX}};
cv.onpointerup=()=>drag=false;
const keys={};
onkeydown=e=>keys[e.key.toLowerCase()]=true;
onkeyup=e=>keys[e.key.toLowerCase()]=false;

function resize(){cv.width=innerWidth*devicePixelRatio;cv.height=innerHeight*devicePixelRatio;
 gl.viewport(0,0,cv.width,cv.height)}
onresize=resize;resize();

let last=performance.now();
function frame(now){
 const dt=Math.min((now-last)/1000,0.05);last=now;
 if(keys['w'])TRUCK.v=Math.min(TRUCK.v+40*dt,90);
 else TRUCK.v=Math.max(TRUCK.v-30*dt,0);
 if(keys['s'])TRUCK.v=Math.max(TRUCK.v-60*dt,0);
 const dirx=Math.sin(TRUCK.yaw),dirz=Math.cos(TRUCK.yaw);
 TRUCK.x+=dirx*TRUCK.v*dt*0.25;
 TRUCK.z+=dirz*TRUCK.v*dt*0.25;
 if(keys['a'])TRUCK.yaw-=0.7*dt*Math.min(TRUCK.v/20,1);
 if(keys['d'])TRUCK.yaw+=0.7*dt*Math.min(TRUCK.v/20,1);
 const eye=[TRUCK.x-dirx*camDist*Math.cos(camYaw), camH, TRUCK.z-dirz*camDist*Math.cos(camYaw)];
 const ctr=[TRUCK.x, 2, TRUCK.z];
 const vp=mul(persp(1.1,cv.width/cv.height,1,3000),lookAt(eye,ctr,[0,1,0]));
 gl.clearColor(0.45,0.65,0.9,1);
 gl.clear(gl.COLOR_BUFFER_BIT|gl.DEPTH_BUFFER_BIT);
 gl.uniformMatrix4fv(uVP,false,new Float32Array(vp));
 gl.bindBuffer(gl.ARRAY_BUFFER,bP);gl.enableVertexAttribArray(aP);gl.vertexAttribPointer(aP,3,gl.FLOAT,false,0,0);
 gl.bindBuffer(gl.ARRAY_BUFFER,bN);gl.enableVertexAttribArray(aN);gl.vertexAttribPointer(aN,3,gl.FLOAT,false,0,0);
 gl.bindBuffer(gl.ARRAY_BUFFER,bC);gl.enableVertexAttribArray(aC);gl.vertexAttribPointer(aC,3,gl.FLOAT,false,0,0);
 gl.drawArrays(gl.TRIANGLES,0,NV);
 document.getElementById('speed').innerHTML=Math.round(TRUCK.v)+' <span>km/h</span>';
 document.getElementById('gear').textContent=TRUCK.v>1?'D'+Math.min(Math.ceil(TRUCK.v/15),12):'N';
 requestAnimationFrame(frame);
}
requestAnimationFrame(frame);
