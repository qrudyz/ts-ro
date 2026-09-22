// ---- Romania world: real motorways (A1, A2, A3) + cities (same coords as game) ----
const A1=[[520,1150],[505,980],[480,800],[430,650],[360,540],[290,470],[200,420],[60,415]];
const A2=[[540,1140],[640,1200],[740,1230],[850,1240]];
const A3=[[530,1130],[540,1000],[545,900]];
const cities={Bucuresti:[530,1140],Pitesti:[480,950],Sibiu:[360,540],Deva:[240,470],
 Timisoara:[110,420],Arad:[60,415],Brasov:[545,900],Ploiesti:[545,1030],
 Constanta:[860,1240],Cluj:[230,320],Craiova:[300,900]};
// ground
flat(450,0,750,2600,1700,[0.16,0.30,0.14]);
// Carpathian arc (stacked shrinking boxes = simple mountains)
for(let i=0;i<40;i++){
 const t=i/40, x=650-Math.sin(t*2.6)*180, z=900-t*640;
 const h=30+Math.sin(i*3.7)*14;
 for(let s=0;s<h;s+=8) box(x-s*0.35,s,z-s*0.35,(h-s)*0.9,8,(h-s)*0.9,[0.34+s/h*0.2,0.33+s/h*0.2,0.31+s/h*0.18]);
}
function road(pts,w,col){
 for(let i=0;i<pts.length-1;i++){
  const[ax,az]=pts[i],[bx,bz]=pts[i+1];
  const dx=bx-ax,dz=bz-az,len=Math.hypot(dx,dz),n=Math.ceil(len/22);
  for(let s=0;s<n;s++){
   const t0=s/n,t1=(s+1)/n;
   const x0=ax+dx*t0,z0=az+dz*t0,x1=ax+dx*t1,z1=az+dz*t1;
   const yaw=Math.atan2(dz,dx),cx=(x0+x1)/2,cz=(z0+z1)/2,sl=Math.hypot(x1-x0,z1-z0)+2;
   flat(cx,0.05,cz,sl,w,col,yaw);
  }
 }}
road(A1,16,[0.12,0.12,0.13]); road(A2,16,[0.12,0.12,0.13]);
road(A3,13,[0.14,0.14,0.15]);
// lane markings on A1
for(let i=0;i<A1.length-1;i++){
 const[ax,az]=A1[i],[bx,bz]=A1[i+1];
 const dx=bx-ax,dz=bz-az,len=Math.hypot(dx,dz);
 for(let s=0;s<len;s+=30){
  const t=s/len;
  flat(ax+dx*t,0.09,az+dz*t,7,1.1,[0.95,0.9,0.2],Math.atan2(dz,dx));
 }}
// cities: blocks (tall in center, low at edge, Romanian-concrete palette)
const palette=[[0.75,0.68,0.55],[0.6,0.62,0.66],[0.8,0.6,0.5],[0.55,0.58,0.52]];
for(const[name,[cx,cz]]of Object.entries(cities)){
 for(let b=0;b<26;b++){
  const ox=(Math.random()-0.5)*130,oz=(Math.random()-0.5)*130;
  const h=8+Math.random()*30*(b<8?1.6:0.7);
  box(cx+ox,0,cz+oz,10+Math.random()*8,h,10+Math.random()*8,palette[b%4]);
 }}
// trees
for(let i=0;i<300;i++){
 const x=Math.random()*1600,z=Math.random()*1400;
 if(Math.hypot(x-530,z-1140)<160)continue;
 box(x,0,z,2.5,4+Math.random()*4,2.5,[0.1+Math.random()*0.1,0.3+Math.random()*0.15,0.08]);
}
