// ---- Minimal WebGL renderer (same approach as the Android GL engine) ----
const cv = document.getElementById('c');
const gl = cv.getContext('webgl');
const VS = `attribute vec3 p;attribute vec3 n;attribute vec3 c;
uniform mat4 vp;uniform vec3 sun;
varying vec3 vC;
void main(){gl_Position=vp*vec4(p,1.0);
 float d=max(dot(n,sun),0.0)*0.75+0.25;
 vC=c*min(d,1.0);}`;
const FS = `precision mediump float;varying vec3 vC;
void main(){gl_FragColor=vec4(vC,1.0);}`;
function sh(t,s){const o=gl.createShader(t);gl.shaderSource(o,s);gl.compileShader(o);return o}
const pr=gl.createProgram();
gl.attachShader(pr,sh(gl.VERTEX_SHADER,VS));
gl.attachShader(pr,sh(gl.FRAGMENT_SHADER,FS));
gl.linkProgram(pr);gl.useProgram(pr);
const uVP=gl.getUniformLocation(pr,'vp');
const uSun=gl.getUniformLocation(pr,'sun');
gl.enable(gl.DEPTH_TEST);
const bP=gl.createBuffer(),bN=gl.createBuffer(),bC=gl.createBuffer();
const aP=gl.getAttribLocation(pr,'p'),aN=gl.getAttribLocation(pr,'n'),aC=gl.getAttribLocation(pr,'c');
gl.uniform3f(uSun,0.4,0.9,0.2);

// ---- Mat4 helpers ----
const sub=(a,b)=>[a[0]-b[0],a[1]-b[1],a[2]-b[2]];
const dot=(a,b)=>a[0]*b[0]+a[1]*b[1]+a[2]*b[2];
const cross=(a,b)=>[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]];
const norm=a=>{const l=Math.hypot(...a)||1;return[a[0]/l,a[1]/l,a[2]/l]};
function persp(f,a,n,fr){const t=1/Math.tan(f/2);return[t/a,0,0,0, 0,t,0,0, 0,0,(fr+n)/(n-fr),-1, 0,0,2*fr*n/(n-fr),0]}
function lookAt(e,c,u){
 const z=norm(sub(e,c)),x=norm(cross(u,z)),y=cross(z,x);
 return[x[0],y[0],z[0],0, x[1],y[1],z[1],0, x[2],y[2],z[2],0,
        -dot(x,e),-dot(y,e),-dot(z,e),1]}
function mul(A,B){const o=new Array(16);for(let i=0;i<4;i++)for(let j=0;j<4;j++){let s=0;for(let k=0;k<4;k++)s+=A[k*4+j]*B[i*4+k];o[i*4+j]=s}return o}
