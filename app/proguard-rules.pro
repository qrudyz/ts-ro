# Keep the simulation/renderer entry points reachable through reflection-free XML wiring.
-keep class com.rudyunguru.trucks.ui.** { *; }
-keep class com.rudyunguru.trucks.gl.** { *; }
-keepattributes *Annotation*
