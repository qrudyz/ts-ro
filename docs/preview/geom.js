// ---- Geometry builder (mirrors WorldMeshFactory.addBox) ----
const V=[],N=[],C=[];
function quad(p0,p1,p2,p3,col){
 const n=norm(cross(sub(p1,p0),sub(p3,p0)));
 [p0,p1,p2,p0,p2,p3].forEach(p=>{V.push(...p);N.push(...n);C.push(...col)})}
function box(x,y,z,w,h,d,col,yaw=0){
 const c=Math.cos(yaw),s=Math.sin(yaw);
 const R=(px,pz)=>[x+px*c-pz*s, y, z+px*s+pz*c];
 const hw=w/2,hd=d/2;
 const a=R(-hw,-hd),b=R(hw,-hd),e=R(hw,hd),f=R(-hw,hd);
 quad([a[0],y,a[2]],[b[0],y,b[2]],[b[0],y+h,b[2]],[a[0],y+h,a[2]],col);
 quad([e[0],y,e[2]],[f[0],y,f[2]],[f[0],y+h,f[2]],[e[0],y+h,e[2]],col);
 quad([b[0],y,b[2]],[e[0],y,e[2]],[e[0],y+h,e[2]],[b[0],y+h,b[2]],col);
 quad([f[0],y,f[2]],[a[0],y,a[2]],[a[0],y+h,a[2]],[f[0],y+h,f[2]],col);
 quad([a[0],y+h,a[2]],[b[0],y+h,b[2]],[e[0],y+h,e[2]],[f[0],y+h,f[2]],col);
}
function flat(cx,y,cz,w,d,col,yaw=0){
 const c=Math.cos(yaw),s=Math.sin(yaw),hw=w/2,hd=d/2;
 const R=(px,pz)=>[cx+px*c-pz*s, y, cz+px*s+pz*c];
 const a=R(-hw,-hd),b=R(hw,-hd),e=R(hw,hd),f=R(-hw,hd);
 quad(a,b,e,f,col);
 // back face so it's visible from both sides
 const n0=V.length;
 quad(a,f,e,b,col);
}
